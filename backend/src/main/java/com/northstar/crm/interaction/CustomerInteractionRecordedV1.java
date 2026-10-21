package com.northstar.crm.interaction;

import java.time.Instant;
import java.util.UUID;

public record CustomerInteractionRecordedV1(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String correlationId,
        String actor,
        UUID customerId,
        UUID interactionId,
        String channel
) {
    public static final String TOPIC = "crm.customer.interactions.v1";

    public static CustomerInteractionRecordedV1 create(
            UUID customerId,
            UUID interactionId,
            String channel,
            String correlationId,
            String actor
    ) {
        return new CustomerInteractionRecordedV1(
                UUID.randomUUID(),
                "CustomerInteractionRecorded",
                1,
                Instant.now(),
                correlationId,
                actor,
                customerId,
                interactionId,
                channel
        );
    }
}
