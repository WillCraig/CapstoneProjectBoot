package com.northstar.crm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Dedupe store row: one per applied Kafka event. Internal bookkeeping only — identifiers and
 * correlation, never interaction content. Never exposed via the REST API.
 */
@Entity
@Table(name = "processed_event")
public class ProcessedEventEntity {

  @Id
  @Column(name = "event_id")
  private UUID eventId;

  @Column(name = "event_type", nullable = false)
  private String eventType;

  @Column(name = "customer_id", nullable = false)
  private String customerId;

  @Column(name = "correlation_id", nullable = false)
  private String correlationId;

  @Column(name = "processed_at", nullable = false)
  private Instant processedAt = Instant.now();

  public ProcessedEventEntity() {}

  public ProcessedEventEntity(UUID eventId, String eventType, String customerId, String correlationId) {
    this.eventId = eventId;
    this.eventType = eventType;
    this.customerId = customerId;
    this.correlationId = correlationId;
  }

  public UUID getEventId() {
    return eventId;
  }

  public String getEventType() {
    return eventType;
  }

  public String getCustomerId() {
    return customerId;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public Instant getProcessedAt() {
    return processedAt;
  }
}
