package com.northstar.crm.messaging;

import com.northstar.crm.domain.InteractionEntity;
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
 * Publishes {@link CustomerInteractionRecordedV1} to {@code crm.customer.interactions.v1},
 * keyed by customer public id so one customer's events stay in partition order.
 *
 * <p>Consistency (Lab 48 / docs/architecture.md): PostgreSQL is the system of record. When a
 * transaction is active the send is deferred until after commit, so consumers never see an
 * event for an interaction that rolled back. With no active transaction (e.g. unit tests) the
 * event is sent immediately. A full outbox is out of scope for Lab 49 and noted in the demo doc.
 */
@Component
public class InteractionEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(InteractionEventPublisher.class);

  public static final String TOPIC = "crm.customer.interactions.v1";

  /** Placeholder until Lab 51 supplies the authenticated JWT principal. */
  public static final String DEFAULT_ACTOR = "demo-agent";

  private final KafkaTemplate<String, CustomerInteractionRecordedV1> kafkaTemplate;
  private final String topic;

  public InteractionEventPublisher(
      KafkaTemplate<String, CustomerInteractionRecordedV1> kafkaTemplate,
      @Value("${crm.kafka.topic:" + TOPIC + "}") String topic) {
    this.kafkaTemplate = kafkaTemplate;
    this.topic = topic;
  }

  public void publish(InteractionEntity interaction) {
    CustomerInteractionRecordedV1 event = toEvent(interaction, DEFAULT_ACTOR);
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

  static CustomerInteractionRecordedV1 toEvent(InteractionEntity interaction, String actor) {
    return new CustomerInteractionRecordedV1(
        UUID.randomUUID(),
        CustomerInteractionRecordedV1.TYPE,
        CustomerInteractionRecordedV1.VERSION,
        Instant.now(),
        interaction.getCorrelationId(),
        actor,
        interaction.getCustomer().getPublicId(),
        interaction.getId().toString(),
        interaction.getInteractionType());
  }

  private void send(CustomerInteractionRecordedV1 event) {
    // Identifiers and correlation only — never log the interaction summary.
    log.info(
        "Publishing {} v{} eventId={} customerId={} interactionId={} correlationId={}",
        event.eventType(),
        event.eventVersion(),
        event.eventId(),
        event.customerId(),
        event.interactionId(),
        event.correlationId());
    kafkaTemplate.send(topic, event.customerId(), event);
  }
}
