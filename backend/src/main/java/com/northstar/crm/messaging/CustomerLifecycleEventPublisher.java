package com.northstar.crm.messaging;

import com.northstar.crm.observability.CorrelationId;
import com.northstar.crm.security.CurrentActor;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Publishes {@link CustomerStatusChangedV1} with the same consistency rule as interactions:
 * the event is built on the request thread (so {@code actor} and {@code correlationId} are the
 * caller's) but only sent after the PostgreSQL transaction commits. Known gap, shared with the
 * interaction publisher: a crash between commit and send loses the event (no outbox yet).
 */
@Component
public class CustomerLifecycleEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(CustomerLifecycleEventPublisher.class);

  public static final String TOPIC = "crm.customer.lifecycle.v1";

  private final KafkaTemplate<String, CustomerStatusChangedV1> kafkaTemplate;
  private final String topic;

  public CustomerLifecycleEventPublisher(
      KafkaTemplate<String, CustomerStatusChangedV1> kafkaTemplate,
      @Value("${crm.kafka.lifecycle-topic:" + TOPIC + "}") String topic) {
    this.kafkaTemplate = kafkaTemplate;
    this.topic = topic;
  }

  public void publishStatusChange(String customerId, String fromStatus, String toStatus) {
    CustomerStatusChangedV1 event = toEvent(customerId, fromStatus, toStatus);
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              send(event);
            }
          });
    } else {
      send(event);
    }
  }

  static CustomerStatusChangedV1 toEvent(String customerId, String fromStatus, String toStatus) {
    String correlationId = CorrelationId.current();
    return new CustomerStatusChangedV1(
        UUID.randomUUID(),
        CustomerStatusChangedV1.TYPE,
        CustomerStatusChangedV1.VERSION,
        Instant.now(),
        correlationId != null ? correlationId : UUID.randomUUID().toString(),
        CurrentActor.name(),
        customerId,
        fromStatus,
        toStatus);
  }

  private void send(CustomerStatusChangedV1 event) {
    log.info(
        "Publishing {} v{} eventId={} customerId={} {}->{} actor={}",
        event.eventType(),
        event.eventVersion(),
        event.eventId(),
        event.customerId(),
        event.fromStatus(),
        event.toStatus(),
        event.actor());
    kafkaTemplate
        .send(topic, event.customerId(), event)
        .whenComplete(
            (result, ex) -> {
              if (ex != null) {
                log.error(
                    "Failed to publish {} eventId={} customerId={}",
                    event.eventType(),
                    event.eventId(),
                    event.customerId(),
                    ex);
              }
            });
  }
}
