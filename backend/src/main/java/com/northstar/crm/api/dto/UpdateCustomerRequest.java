package com.northstar.crm.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Editable fields for an existing customer; the public ID never changes. Status carries the
 * lifecycle transitions (for example Ravi {@code PROSPECT -> ACTIVE} on onboarding).
 */
public record UpdateCustomerRequest(
    @NotBlank @Size(max = 200) String fullName,
    @NotBlank
        @Pattern(
            regexp = "PROSPECT|ACTIVE|CLOSED",
            message = "status must be PROSPECT, ACTIVE, or CLOSED")
        String status) {}
