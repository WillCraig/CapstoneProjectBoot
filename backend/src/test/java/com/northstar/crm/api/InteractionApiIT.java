package com.northstar.crm.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.northstar.crm.messaging.InteractionEventPublisher;
import com.northstar.crm.repo.InteractionRepository;
import com.northstar.crm.support.AbstractIntegrationTest;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import com.northstar.crm.support.TestTokens;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Full-stack IT: HTTP -> service -> PostgreSQL (Flyway) -> Kafka, via Testcontainers.
 *
 * <p>Deliberately not {@code @Transactional}: the publisher sends only after the service
 * transaction commits, and a test-managed transaction would never commit — the event would
 * never be published. Tests clean up interactions themselves instead (customers stay seeded).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestTokens.class)
class InteractionApiIT extends AbstractIntegrationTest {

  @Autowired TestTokens tokens;

  /** Real signed JWT for the AGENT demo user (was the hardcoded demo token). */
  private String bearer;

  @BeforeEach
  void issueAgentToken() {
    bearer = tokens.agentBearer();
  }

  @Autowired MockMvc mockMvc;

  @Autowired InteractionRepository interactionRepository;

  @Autowired ObjectMapper objectMapper;

  @BeforeEach
  void cleanInteractions() {
    interactionRepository.deleteAll();
  }

  @Test
  void list_withoutBearer_unauthorized() throws Exception {
    mockMvc
        .perform(get("/api/v1/interactions").param("customerId", "CUS-1001"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void createAndList_forAmina_persistsRow() throws Exception {
    String body =
        """
        {"customerId":"CUS-1001","interactionType":"NOTE","summary":"Follow-up on billing question","correlationId":"lab-request-001"}
        """;

    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/interactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", bearer)
                    .header("X-Correlation-ID", "lab-request-001")
                    .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.customerId").value("CUS-1001"))
            .andExpect(jsonPath("$.correlationId").value("lab-request-001"))
            .andReturn();

    String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
    // Persistence proof: the row is really in PostgreSQL, readable by id.
    assertTrue(interactionRepository.findById(UUID.fromString(id)).isPresent());

    mockMvc
        .perform(
            get("/api/v1/interactions")
                .param("customerId", "CUS-1001")
                .header("Authorization", bearer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].customerId").value("CUS-1001"))
        .andExpect(jsonPath("$[0].id").value(id));
  }

  @Test
  void create_unknownCustomer_notFound_andNothingPersisted() throws Exception {
    String body =
        """
        {"customerId":"CUS-9999","interactionType":"NOTE","summary":"Should fail"}
        """;

    mockMvc
        .perform(
            post("/api/v1/interactions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", bearer)
                .content(body))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));

    assertEquals(0, interactionRepository.count());
  }

  @Test
  void create_invalidType_badRequest_andNothingPersisted() throws Exception {
    String body =
        """
        {"customerId":"CUS-1001","interactionType":"TEXT","summary":"Bad type"}
        """;

    mockMvc
        .perform(
            post("/api/v1/interactions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", bearer)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Validation failed"));

    assertEquals(0, interactionRepository.count());
  }

  @Test
  void create_forAmina_publishesV1Event() throws Exception {
    String correlationId = "it-kafka-001";
    try (KafkaConsumer<String, String> consumer = newConsumer()) {
      consumer.subscribe(List.of(InteractionEventPublisher.TOPIC));

      MvcResult created =
          mockMvc
              .perform(
                  post("/api/v1/interactions")
                      .contentType(MediaType.APPLICATION_JSON)
                      .header("Authorization", bearer)
                      .header("X-Correlation-ID", correlationId)
                      .content(
                          """
                          {"customerId":"CUS-1001","interactionType":"NOTE","summary":"Requested address update"}
                          """))
              .andExpect(status().isCreated())
              .andReturn();

      String interactionId =
          objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

      ConsumerRecord<String, String> record = awaitRecordWithCorrelation(consumer, correlationId);
      assertEquals("CUS-1001", record.key(), "partition key must be the customer public id");

      JsonNode event = objectMapper.readTree(record.value());
      assertFalse(event.get("eventId").asText().isBlank());
      assertEquals("CustomerInteractionRecorded", event.get("eventType").asText());
      assertEquals(1, event.get("eventVersion").asInt());
      assertNotNull(event.get("occurredAt"));
      assertEquals(correlationId, event.get("correlationId").asText());
      assertEquals("agent", event.get("actor").asText(), "actor is the JWT subject");
      assertEquals("CUS-1001", event.get("customerId").asText());
      assertEquals(interactionId, event.get("interactionId").asText());
      assertEquals("NOTE", event.get("interactionType").asText());
      assertFalse(
          record.value().contains("Requested address update"),
          "event must never carry the interaction summary");
    }
  }

  @Test
  void updateAndSoftDelete_forAmina_throughApi() throws Exception {
    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/interactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", bearer)
                    .header("X-Correlation-ID", "lab-request-001")
                    .content(
                        """
                        {"customerId":"CUS-1001","interactionType":"NOTE","summary":"Initial note"}
                        """))
            .andExpect(status().isCreated())
            .andReturn();
    String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

    mockMvc
        .perform(
            put("/api/v1/interactions/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", bearer)
                .content(
                    """
                    {"interactionType":"EMAIL","summary":"Corrected by agent"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.interactionType").value("EMAIL"))
        .andExpect(jsonPath("$.summary").value("Corrected by agent"));

    assertEquals(
        "Corrected by agent",
        interactionRepository.findById(UUID.fromString(id)).orElseThrow().getSummary());

    mockMvc
        .perform(delete("/api/v1/interactions/{id}", id).header("Authorization", bearer))
        .andExpect(status().isNoContent());
    assertTrue(interactionRepository.findById(UUID.fromString(id)).isEmpty());

    mockMvc
        .perform(get("/api/v1/interactions/{id}", id).header("Authorization", bearer))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Unknown interaction: " + id));
  }

  private static KafkaConsumer<String, String> newConsumer() {
    Properties props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers());
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "interaction-api-it-" + UUID.randomUUID());
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    return new KafkaConsumer<>(props);
  }

  private ConsumerRecord<String, String> awaitRecordWithCorrelation(
      KafkaConsumer<String, String> consumer, String correlationId) {
    long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
    while (System.nanoTime() < deadline) {
      for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
        if (record.value().contains(correlationId)) {
          return record;
        }
      }
    }
    throw new AssertionError("No Kafka record with correlationId " + correlationId + " within 30s");
  }
}
