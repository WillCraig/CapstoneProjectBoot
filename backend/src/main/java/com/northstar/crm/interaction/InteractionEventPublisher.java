package com.northstar.crm.interaction;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class InteractionEventPublisher {

    private final KafkaTemplate<String, CustomerInteractionRecordedV1> kafkaTemplate;

    public InteractionEventPublisher(
            KafkaTemplate<String, CustomerInteractionRecordedV1> kafkaTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(CustomerInteractionRecordedV1 event) {
        kafkaTemplate.send(
                CustomerInteractionRecordedV1.TOPIC,
                event.customerId().toString(),
                event
        );
    }
}
