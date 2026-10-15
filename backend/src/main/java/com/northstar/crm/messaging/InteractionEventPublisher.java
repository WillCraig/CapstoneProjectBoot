package com.northstar.crm.messaging;

import com.northstar.crm.domain.InteractionEntity;
import org.springframework.stereotype.Component;

/**
 * Publishes CustomerInteractionRecordedV1 to topic crm.customer.interactions.v1.
 * Carried forward from Lab 49 (adapted to the Lab 50 JPA entity).
 */
@Component
public class InteractionEventPublisher {

  public static final String TOPIC = "crm.customer.interactions.v1";

  /**
   * Starter stub: logs only. Full path: inject KafkaTemplate and send to TOPIC (see ADR-002).
   */
  public void publish(InteractionEntity interaction) {
    // TODO (Lab 49): build CustomerInteractionRecordedV1 and publish to Kafka using KafkaTemplate,
    //   keyed by customer public id, with the correlationId carried in the event.
    System.out.println(
        "TODO publish event for customer="
            + interaction.getCustomer().getPublicId()
            + " correlation="
            + interaction.getCorrelationId());
  }
}
