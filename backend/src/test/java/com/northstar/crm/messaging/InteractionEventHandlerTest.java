package com.northstar.crm.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.northstar.crm.domain.ProcessedEventEntity;
import com.northstar.crm.repo.ProcessedEventRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class InteractionEventHandlerTest {

  private ProcessedEventRepository processedEvents;
  private InteractionEventHandler handler;

  @BeforeEach
  void setUp() {
    processedEvents = mock(ProcessedEventRepository.class);
    handler = new InteractionEventHandler(processedEvents);
  }

  @Test
  void handle_newEvent_appliesAndRecordsDedupeRow() {
    CustomerInteractionRecordedV1 event = event(1, "lab-request-001");
    when(processedEvents.existsById(event.eventId())).thenReturn(false);

    assertTrue(handler.handle(event));

    ArgumentCaptor<ProcessedEventEntity> saved =
        ArgumentCaptor.forClass(ProcessedEventEntity.class);
    verify(processedEvents).save(saved.capture());
    assertEquals(event.eventId(), saved.getValue().getEventId());
    assertEquals("CustomerInteractionRecorded", saved.getValue().getEventType());
    assertEquals("CUS-1001", saved.getValue().getCustomerId());
    assertEquals("lab-request-001", saved.getValue().getCorrelationId());
  }

  @Test
  void handle_duplicateEventId_isNoOp() {
    CustomerInteractionRecordedV1 event = event(1, "lab-request-001");
    when(processedEvents.existsById(event.eventId())).thenReturn(true);

    assertFalse(handler.handle(event));

    verify(processedEvents, never()).save(any());
  }

  @Test
  void handle_unsupportedVersion_throwsAndRecordsNothing() {
    CustomerInteractionRecordedV1 event = event(99, "lab-request-001");

    assertThrows(UnsupportedEventVersionException.class, () -> handler.handle(event));

    verify(processedEvents, never()).save(any());
  }

  private static CustomerInteractionRecordedV1 event(int version, String correlationId) {
    return new CustomerInteractionRecordedV1(
        UUID.randomUUID(),
        CustomerInteractionRecordedV1.TYPE,
        version,
        Instant.now(),
        correlationId,
        InteractionEventPublisher.DEFAULT_ACTOR,
        "CUS-1001",
        UUID.randomUUID().toString(),
        "NOTE");
  }
}
