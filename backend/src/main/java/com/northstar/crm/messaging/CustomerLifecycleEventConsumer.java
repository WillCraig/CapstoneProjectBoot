package com.northstar.crm.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code crm.customer.lifecycle.v1}. Bounded retry and the {@code <topic>.DLT} route
 * come from the shared {@link KafkaConsumerConfig} error handler.
 */
@Component
public class CustomerLifecycleEventConsumer {

  private final CustomerLifecycleEventHandler handler;

  public CustomerLifecycleEventConsumer(CustomerLifecycleEventHandler handler) {
    this.handler = handler;
  }

  @KafkaListener(
      topics = "${crm.kafka.lifecycle-topic}",
      groupId = "${crm.kafka.lifecycle-consumer-group}")
  public void onEvent(CustomerStatusChangedV1 event) {
    handler.handle(event);
  }
}
