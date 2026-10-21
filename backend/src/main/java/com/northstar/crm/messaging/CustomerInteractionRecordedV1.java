package com.northstar.crm.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Versioned event contract V1, published to {@code crm.customer.interactions.v1}.
 *
 * <p>Field names are frozen for Lab 49/50. Carry identifiers and metadata only — never the
 * interaction summary or other note content. {@code interactionId} is the interaction UUID
 * rendered as a string so non-Java consumers are not coupled to a UUID type.
 */
public record CustomerInteractionRecordedV1(
    UUID eventId,
    String eventType,
    int eventVersion,
    Instant occurredAt,
    String correlationId,
    String actor,
    String customerId,
    String interactionId,
    String interactionType) {

  public static final String TYPE = "CustomerInteractionRecorded";
  public static final int VERSION = 1;
}
