package com.northstar.crm.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/v1/interactions}.
 *
 * <p>Reminder: the {@code @...} labels are Bean Validation annotations — rules attached to a
 * field instead of hand-written checks. {@code @NotBlank} = not null/empty/spaces,
 * {@code @Size} = max length, {@code @Pattern} = must match the given pattern. They are
 * enforced before the service runs because the controller marks this parameter
 * {@code @Valid}; a failed rule rejects the request with 400. A field with no annotation
 * ({@code correlationId}) is optional.
 */
public record CreateInteractionRequest(
    @NotBlank String customerId,
    @NotBlank
        @Pattern(
            regexp = "CALL|EMAIL|NOTE|MEETING",
            message = "interactionType must be CALL, EMAIL, NOTE, or MEETING")
        String interactionType,
    @NotBlank @Size(max = 1024) String summary,
    String correlationId) {}
