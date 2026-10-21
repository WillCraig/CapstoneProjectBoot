package com.northstar.crm.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private OffsetDateTime processedAt;

    protected ProcessedEvent() {
        // Required by JPA.
    }

    public ProcessedEvent(UUID eventId) {
        this.eventId = eventId;
        this.processedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getEventId() {
        return eventId;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }
}
