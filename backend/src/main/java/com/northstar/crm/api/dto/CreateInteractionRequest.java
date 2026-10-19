package com.northstar.crm.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * TODO: Align field names with Lab 48 contracts. Session block: add Bean Validation
 * ({@code @NotBlank}, {@code @Size}) on customerId / summary / interactionType.
 */
public record CreateInteractionRequest(
    // TODO: @NotBlank on customerId
    @NotBlank
        String customerId,
    // TODO: constrain interactionType (e.g. CALL, EMAIL, NOTE)
    @NotBlank
        @Pattern(
            regexp = "CALL|EMAIL|NOTE|MEETING", message = "interactionType must be CALL, EMAIL, NOTE, or MEETING")
    String interactionType,
    // TODO: @NotBlank @Size(max = ...)
    @NotBlank
        @Size(max = 1024)
            String summary,
    String correlationId) {}
