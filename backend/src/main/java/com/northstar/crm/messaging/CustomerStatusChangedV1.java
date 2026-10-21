package com.northstar.crm.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Versioned customer lifecycle event V1, published to {@code crm.customer.lifecycle.v1} keyed
 * by customer public id whenever a customer's status actually changes (e.g. Ravi
 * {@code PROSPECT -> ACTIVE}, or any status {@code -> CLOSED}). Identifiers and statuses only:
 * no names or other personal data, so the audit trail never copies customer PII.
 */
public record CustomerStatusChangedV1(
    UUID eventId,
    String eventType,
    int eventVersion,
    Instant occurredAt,
    String correlationId,
    String actor,
    String customerId,
    String fromStatus,
    String toStatus) {

  public static final String TYPE = "CustomerStatusChanged";
  public static final int VERSION = 1;
}
