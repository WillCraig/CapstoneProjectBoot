package com.northstar.crm.messaging;

import com.northstar.crm.domain.InteractionEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes CustomerInteractionRecordedV1 to topic crm.customer.interactions.v1.
 * Carried forward from Lab 49 (adapted to the Lab 50 JPA entity).
 */
@Component
public class InteractionEventPublisher {

  public static final String TOPIC = "crm.customer.interactions.v1";

  private final KafkaTemplate<Object, Object> kafkaTemplate;

  public InteractionEventPublisher(KafkaTemplate<Object, Object> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  public void publish(InteractionEntity interaction) {
    String customerId = interaction.getCustomer().getPublicId();
    CustomerInteractionRecordedV1 event =
        new CustomerInteractionRecordedV1(
            CustomerInteractionRecordedV1.TYPE,
            CustomerInteractionRecordedV1.VERSION,
            interaction.getId(),
            customerId,
            interaction.getInteractionType(),
            interaction.getCorrelationId(),
            interaction.getCreatedAt());
    kafkaTemplate.send(TOPIC, customerId, event);
  }
}
