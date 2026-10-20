package com.northstar.crm.messaging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Bounded consumer failure handling: transient failures retry twice with a fixed 500 ms
 * backoff, then the record goes to {@code <topic>.DLT} (same key/value plus failure headers)
 * instead of retrying forever. {@link UnsupportedEventVersionException} can never succeed on
 * retry, so it skips straight to the DLT.
 */
@Configuration
public class KafkaConsumerConfig {

  @Bean
  public DefaultErrorHandler kafkaErrorHandler(
      KafkaTemplate<String, CustomerInteractionRecordedV1> kafkaTemplate) {
    DefaultErrorHandler errorHandler =
        new DefaultErrorHandler(
            new DeadLetterPublishingRecoverer(kafkaTemplate), new FixedBackOff(500L, 2L));
    errorHandler.addNotRetryableExceptions(UnsupportedEventVersionException.class);
    return errorHandler;
  }
}
