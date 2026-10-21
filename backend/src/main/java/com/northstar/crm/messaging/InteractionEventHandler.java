package com.northstar.crm.messaging;

import com.northstar.crm.domain.ProcessedEventEntity;
import com.northstar.crm.observability.CorrelationId;
import com.northstar.crm.repo.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies a {@link CustomerInteractionRecordedV1} exactly once in effect: the processed-event
 * row is both the audit record and the dedupe key, so an at-least-once redelivery is a no-op.
 * In a fuller system the side effect (notification, projection update) would happen in this
 * same transaction as the dedupe insert.
 */
@Service
public class InteractionEventHandler {

  private static final Logger log = LoggerFactory.getLogger(InteractionEventHandler.class);

  private final ProcessedEventRepository processedEvents;

  public InteractionEventHandler(ProcessedEventRepository processedEvents) {
    this.processedEvents = processedEvents;
  }

  /** @return true if the event was applied, false if it was a duplicate delivery */
  @Transactional
  public boolean handle(CustomerInteractionRecordedV1 event) {
    // Listener threads have no HTTP request; carry the event's correlation id into the logs.
    if (event.correlationId() != null) {
      MDC.put(CorrelationId.MDC_KEY, event.correlationId());
    }
    try {
      return apply(event);
    } finally {
      MDC.remove(CorrelationId.MDC_KEY);
    }
  }

  private boolean apply(CustomerInteractionRecordedV1 event) {
    if (event.eventVersion() != CustomerInteractionRecordedV1.VERSION) {
      throw new UnsupportedEventVersionException(event.eventVersion());
    }
    // Identifiers and correlation only — the event carries no summary, and none is logged here.
    if (processedEvents.existsById(event.eventId())) {
      log.info(
          "Duplicate event skipped eventId={} customerId={} correlationId={}",
          event.eventId(),
          event.customerId(),
          event.correlationId());
      return false;
    }
    processedEvents.save(
        new ProcessedEventEntity(
            event.eventId(), event.eventType(), event.customerId(), event.correlationId()));
    log.info(
        "Applied {} v{} eventId={} customerId={} interactionId={} correlationId={}",
        event.eventType(),
        event.eventVersion(),
        event.eventId(),
        event.customerId(),
        event.interactionId(),
        event.correlationId());
    return true;
  }
}
