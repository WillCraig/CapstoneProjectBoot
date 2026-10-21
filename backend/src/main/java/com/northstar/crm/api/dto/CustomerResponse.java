package com.northstar.crm.api.dto;

import java.time.Instant;

/** Customer as the API exposes it; {@code customerId} is the public ID (e.g. {@code CUS-1001}). */
public record CustomerResponse(
    String customerId, String fullName, String status, Instant createdAt) {}
