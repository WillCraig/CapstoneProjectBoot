package com.northstar.crm.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfiguration {

    public static final String DEAD_LETTER_TOPIC =
            "crm.customer.interactions.v1.DLT";

    @Bean
    public NewTopic customerInteractionsTopic() {
        return TopicBuilder.name("crm.customer.interactions.v1")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic customerInteractionsDeadLetterTopic() {
        return TopicBuilder.name(DEAD_LETTER_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            KafkaTemplate<Object, Object> kafkaTemplate
    ) {
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, exception) ->
                                new org.apache.kafka.common.TopicPartition(
                                        DEAD_LETTER_TOPIC,
                                        record.partition()
                                )
                );

        // Three retries, with a one-second delay between attempts.
        return new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(1000L, 3L)
        );
    }
}
