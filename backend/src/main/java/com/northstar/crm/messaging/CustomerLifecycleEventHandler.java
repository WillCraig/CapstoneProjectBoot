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
 * Audit consumer for {@link CustomerStatusChangedV1}: same rules as interactions — reject an
 * unknown version (non-retryable, goes to the DLT), and apply each eventId once using the
 * shared {@code processed_event} table, so at-least-once redelivery is harmless.
 */
@Service
public class CustomerLifecycleEventHandler {

  private static final Logger log = LoggerFactory.getLogger(CustomerLifecycleEventHandler.class);

  private final ProcessedEventRepository processedEvents;

  public CustomerLifecycleEventHandler(ProcessedEventRepository processedEvents) {
    this.processedEvents = processedEvents;
  }

  /** @return true if the event was applied, false if it was a duplicate delivery */
  @Transactional
  public boolean handle(CustomerStatusChangedV1 event) {
    if (event.correlationId() != null) {
      MDC.put(CorrelationId.MDC_KEY, event.correlationId());
    }
    try {
      if (event.eventVersion() != CustomerStatusChangedV1.VERSION) {
        throw new UnsupportedEventVersionException(event.eventVersion());
      }
      if (processedEvents.existsById(event.eventId())) {
        log.info(
            "Duplicate event skipped eventId={} customerId={}", event.eventId(), event.customerId());
        return false;
      }
      processedEvents.save(
          new ProcessedEventEntity(
              event.eventId(), event.eventType(), event.customerId(), event.correlationId()));
      log.info(
          "Audited {} v{} eventId={} customerId={} {}->{} actor={}",
          event.eventType(),
          event.eventVersion(),
          event.eventId(),
          event.customerId(),
          event.fromStatus(),
          event.toStatus(),
          event.actor());
      return true;
    } finally {
      MDC.remove(CorrelationId.MDC_KEY);
    }
  }
}
