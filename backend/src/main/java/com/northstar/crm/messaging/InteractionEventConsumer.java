package com.northstar.crm.messaging;

import com.northstar.crm.interaction.CustomerInteractionRecordedV1;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InteractionEventConsumer {

    private final InteractionEventProcessingService processingService;

    public InteractionEventConsumer(
            InteractionEventProcessingService processingService
    ) {
        this.processingService = processingService;
    }

    @KafkaListener(
            topics = CustomerInteractionRecordedV1.TOPIC,
            groupId = "${spring.kafka.consumer.group-id:northstar-crm}"
    )
    public void consume(CustomerInteractionRecordedV1 event) {
        processingService.processOnce(event);
    }
}
