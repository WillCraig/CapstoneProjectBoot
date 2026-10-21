package com.northstar.crm.interaction;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InteractionResponse(
        UUID interactionId,
        String customerId,
        String channel,
        String summary,
        OffsetDateTime createdAt,
        Long version
) {
    public static InteractionResponse from(CustomerInteraction interaction) {
        return new InteractionResponse(
                interaction.getInteractionId(),
                interaction.getCustomer().getCustomerId(),
                interaction.getChannel(),
                interaction.getSummary(),
                interaction.getCreatedAt(),
                interaction.getVersion()
        );
    }
}
