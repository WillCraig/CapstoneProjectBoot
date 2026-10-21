package com.northstar.crm.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code crm.customer.interactions.v1}. All business rules (version check, dedupe)
 * live in {@link InteractionEventHandler} so they can be unit-tested without a broker;
 * delivery concerns (bounded retries, DLT) live in {@link KafkaConsumerConfig}.
 */
@Component
public class InteractionEventConsumer {

  private final InteractionEventHandler handler;

  public InteractionEventConsumer(InteractionEventHandler handler) {
    this.handler = handler;
  }

  @KafkaListener(topics = "${crm.kafka.topic}", groupId = "${crm.kafka.consumer-group}")
  public void onEvent(CustomerInteractionRecordedV1 event) {
    handler.handle(event);
  }
}
