package com.northstar.crm.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Editable fields for an existing interaction; the owning customer never changes. */
public record UpdateInteractionRequest(
    @NotBlank
        @Pattern(
            regexp = "CALL|EMAIL|NOTE|MEETING",
            message = "interactionType must be CALL, EMAIL, NOTE, or MEETING")
        String interactionType,
    @NotBlank @Size(max = 1024) String summary) {}
