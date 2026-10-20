package com.northstar.crm.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.northstar.crm.repo.ProcessedEventRepository;
import com.northstar.crm.support.AbstractIntegrationTest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Consumer resilience against the real broker: apply once, duplicate delivery is a no-op,
 * and an unsupported eventVersion lands on the dead-letter topic instead of being applied.
 */
@SpringBootTest
class InteractionEventConsumerIT extends AbstractIntegrationTest {

  private static final String TOPIC = InteractionEventPublisher.TOPIC;
  private static final String DLT = TOPIC + ".DLT";

  @Autowired KafkaTemplate<String, CustomerInteractionRecordedV1> kafkaTemplate;

  @Autowired ProcessedEventRepository processedEvents;

  @BeforeEach
  void cleanProcessed() {
    processedEvents.deleteAll();
  }

  @Test
  void consumesEvent_andRecordsProcessedRow() throws Exception {
    CustomerInteractionRecordedV1 event = event(UUID.randomUUID(), 1);

    send(event);

    awaitProcessed(event.eventId());
    var row = processedEvents.findById(event.eventId()).orElseThrow();
    assertEquals("CUS-1001", row.getCustomerId());
    assertEquals("it-consumer-001", row.getCorrelationId());
  }

  @Test
  void duplicateDelivery_appliesOnlyOnce() throws Exception {
    CustomerInteractionRecordedV1 event = event(UUID.randomUUID(), 1);
    CustomerInteractionRecordedV1 sentinel = event(UUID.randomUUID(), 1);

    send(event);
    awaitProcessed(event.eventId());
    send(event); // redelivery of the very same eventId
    send(sentinel); // same partition key: handled after the duplicate
    awaitProcessed(sentinel.eventId());

    long timesApplied =
        processedEvents.findAll().stream().filter(r -> r.getEventId().equals(event.eventId())).count();
    assertEquals(1, timesApplied);
    assertEquals(2, processedEvents.count());
  }

  @Test
  void unsupportedVersion_goesToDeadLetterTopic_andIsNotApplied() throws Exception {
    CustomerInteractionRecordedV1 poison = event(UUID.randomUUID(), 99);

    try (KafkaConsumer<String, String> dltConsumer = newDltConsumer()) {
      dltConsumer.subscribe(List.of(DLT));
      send(poison);

      ConsumerRecord<String, String> deadLettered =
          awaitRecordContaining(dltConsumer, poison.eventId().toString());
      assertEquals("CUS-1001", deadLettered.key());
    }
    assertFalse(processedEvents.existsById(poison.eventId()));
  }

  private void send(CustomerInteractionRecordedV1 event) throws Exception {
    kafkaTemplate.send(TOPIC, event.customerId(), event).get(10, TimeUnit.SECONDS);
  }

  private void awaitProcessed(UUID eventId) {
    long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
    while (System.nanoTime() < deadline) {
      if (processedEvents.existsById(eventId)) {
        return;
      }
      sleep(250);
    }
    throw new AssertionError("Event " + eventId + " was not processed within 30s");
  }

  private ConsumerRecord<String, String> awaitRecordContaining(
      KafkaConsumer<String, String> consumer, String needle) {
    long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
    while (System.nanoTime() < deadline) {
      for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
        if (record.value().contains(needle)) {
          return record;
        }
      }
    }
    throw new AssertionError("No DLT record containing " + needle + " within 30s");
  }

  private static KafkaConsumer<String, String> newDltConsumer() {
    Properties props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers());
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "consumer-it-dlt-" + UUID.randomUUID());
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

  private static CustomerInteractionRecordedV1 event(UUID eventId, int version) {
    return new CustomerInteractionRecordedV1(
        eventId,
        CustomerInteractionRecordedV1.TYPE,
        version,
        Instant.now(),
        "it-consumer-001",
        InteractionEventPublisher.DEFAULT_ACTOR,
        "CUS-1001",
        UUID.randomUUID().toString(),
        "NOTE");
  }
}
