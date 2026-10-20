package com.northstar.crm.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SaveCustomerRequest(
    @NotBlank @Size(max = 200) String fullName,
    @NotBlank @Pattern(regexp = "PROSPECT|ACTIVE|CLOSED") String status) {}
