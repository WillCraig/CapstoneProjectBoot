package com.northstar.crm.messaging;

import com.northstar.crm.domain.Interaction;
import org.springframework.stereotype.Component;

@Component
public class InteractionEventPublisher {

  /**
   * Session stub: log or no-op. Full path: KafkaTemplate + topic from ADR-002.
   */
  public void publish(Interaction interaction) {
    // TODO: build CustomerInteractionRecordedV1 and publish to Kafka
    CustomerInteractionRecordedV1 event =
        new CustomerInteractionRecordedV1(
            CustomerInteractionRecordedV1.TYPE,
            CustomerInteractionRecordedV1.VERSION,
            interaction.getId(),
            interaction.getCustomerId(),
            interaction.getInteractionType(),
            interaction.getCorrelationId(),
            interaction.getCreatedAt());
    kafkaTemplate.send("crm.customer.interactions.v1", interaction.getCustomerId(), event);

    //System.out.println("TODO publish event for customer=" + interaction.getCustomerId() + " correlation=" + interaction.getCorrelationId());
  }
}
