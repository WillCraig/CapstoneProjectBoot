package com.northstar.crm.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.northstar.crm.domain.ProcessedEventEntity;
import com.northstar.crm.repo.CustomerRepository;
import com.northstar.crm.repo.ProcessedEventRepository;
import com.northstar.crm.support.AbstractIntegrationTest;
import com.northstar.crm.support.TestTokens;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Customer lifecycle events end to end on the real broker: an API status change is published
 * after commit and audited once by the consumer; redelivery is a no-op; an unknown version is
 * dead-lettered.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestTokens.class)
class CustomerLifecycleEventConsumerIT extends AbstractIntegrationTest {

  private static final String TOPIC = CustomerLifecycleEventPublisher.TOPIC;
  private static final String DLT = TOPIC + ".DLT";

  @Autowired MockMvc mockMvc;
  @Autowired TestTokens tokens;
  @Autowired ObjectMapper objectMapper;
  @Autowired CustomerRepository customerRepository;
  @Autowired ProcessedEventRepository processedEvents;
  @Autowired KafkaTemplate<String, CustomerStatusChangedV1> kafkaTemplate;

  @AfterEach
  void restoreRavi() {
    customerRepository
        .findByPublicId("CUS-1002")
        .ifPresent(
            ravi -> {
              ravi.setStatus("PROSPECT");
              customerRepository.save(ravi);
            });
  }

  @Test
  void raviActivatedThroughApi_publishesV1Event_andIsAudited() throws Exception {
    String correlationId = "it-lifecycle-" + UUID.randomUUID().toString().substring(0, 8);
    try (KafkaConsumer<String, String> topicReader = newStringConsumer()) {
      topicReader.subscribe(List.of(TOPIC));

      mockMvc
          .perform(
              put("/api/v1/customers/{id}", "CUS-1002")
                  .contentType(MediaType.APPLICATION_JSON)
                  .header("Authorization", tokens.agentBearer())
                  .header("X-Correlation-Id", correlationId)
                  .content("{\"fullName\":\"Ravi Singh\",\"status\":\"ACTIVE\"}"))
          .andExpect(status().isOk());

      ConsumerRecord<String, String> record = awaitRecordContaining(topicReader, correlationId);
      assertEquals("CUS-1002", record.key(), "partition key is the customer public id");
      JsonNode event = objectMapper.readTree(record.value());
      assertEquals("CustomerStatusChanged", event.get("eventType").asText());
      assertEquals(1, event.get("eventVersion").asInt());
      assertEquals("agent", event.get("actor").asText());
      assertEquals("PROSPECT", event.get("fromStatus").asText());
      assertEquals("ACTIVE", event.get("toStatus").asText());
      assertFalse(record.value().contains("Ravi Singh"), "lifecycle events carry no customer name");

      ProcessedEventEntity audited = awaitAuditRow(UUID.fromString(event.get("eventId").asText()));
      assertEquals("CUS-1002", audited.getCustomerId());
      assertEquals(correlationId, audited.getCorrelationId());
    }
  }

  @Test
  void duplicateDelivery_isAuditedOnce() throws Exception {
    CustomerStatusChangedV1 event = event(UUID.randomUUID(), 1);
    CustomerStatusChangedV1 sentinel = event(UUID.randomUUID(), 1);

    send(event);
    awaitAuditRow(event.eventId());
    send(event);
    send(sentinel); // same key, so it is consumed after the duplicate
    awaitAuditRow(sentinel.eventId());

    assertEquals(
        1,
        processedEvents.findAll().stream().filter(r -> r.getEventId().equals(event.eventId())).count());
  }

  @Test
  void unsupportedVersion_goesToDeadLetterTopic() throws Exception {
    CustomerStatusChangedV1 poison = event(UUID.randomUUID(), 99);
    try (KafkaConsumer<String, String> dltReader = newStringConsumer()) {
      dltReader.subscribe(List.of(DLT));
      send(poison);

      ConsumerRecord<String, String> deadLettered =
          awaitRecordContaining(dltReader, poison.eventId().toString());
      assertEquals("CUS-1002", deadLettered.key());
    }
    assertFalse(processedEvents.existsById(poison.eventId()));
  }

  private void send(CustomerStatusChangedV1 event) throws Exception {
    kafkaTemplate.send(TOPIC, event.customerId(), event).get(10, TimeUnit.SECONDS);
  }

  private ProcessedEventEntity awaitAuditRow(UUID eventId) {
    long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
    while (System.nanoTime() < deadline) {
      var row = processedEvents.findById(eventId);
      if (row.isPresent()) {
        return row.get();
      }
      sleep(250);
    }
    throw new AssertionError("Event " + eventId + " was not audited within 30s");
  }

  private static ConsumerRecord<String, String> awaitRecordContaining(
      KafkaConsumer<String, String> consumer, String needle) {
    long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
    while (System.nanoTime() < deadline) {
      for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
        if (record.value().contains(needle)) {
          return record;
        }
      }
    }
    throw new AssertionError("No record containing " + needle + " within 30s");
  }

  private static KafkaConsumer<String, String> newStringConsumer() {
    Properties props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers());
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "lifecycle-it-" + UUID.randomUUID());
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    return new KafkaConsumer<>(props);
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Interrupted while waiting", e);
    }
  }

  private static CustomerStatusChangedV1 event(UUID eventId, int version) {
    return new CustomerStatusChangedV1(
        eventId, CustomerStatusChangedV1.TYPE, version, Instant.now(),
        "it-lifecycle-direct", "agent", "CUS-1002", "PROSPECT", "ACTIVE");
  }
}
