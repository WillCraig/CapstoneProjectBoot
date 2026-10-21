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

class CustomerLifecycleEventHandlerTest {

  private ProcessedEventRepository processedEvents;
  private CustomerLifecycleEventHandler handler;

  @BeforeEach
  void setUp() {
    processedEvents = mock(ProcessedEventRepository.class);
    handler = new CustomerLifecycleEventHandler(processedEvents);
  }

  @Test
  void newEvent_isAudited() {
    CustomerStatusChangedV1 event = event(1);
    when(processedEvents.existsById(event.eventId())).thenReturn(false);

    assertTrue(handler.handle(event));

    ArgumentCaptor<ProcessedEventEntity> saved = ArgumentCaptor.forClass(ProcessedEventEntity.class);
    verify(processedEvents).save(saved.capture());
    assertEquals(event.eventId(), saved.getValue().getEventId());
    assertEquals("CustomerStatusChanged", saved.getValue().getEventType());
    assertEquals("CUS-1002", saved.getValue().getCustomerId());
  }

  @Test
  void duplicate_isSkipped() {
    CustomerStatusChangedV1 event = event(1);
    when(processedEvents.existsById(event.eventId())).thenReturn(true);

    assertFalse(handler.handle(event));
    verify(processedEvents, never()).save(any());
  }

  @Test
  void unknownVersion_isRejected() {
    assertThrows(UnsupportedEventVersionException.class, () -> handler.handle(event(2)));
    verify(processedEvents, never()).save(any());
  }

  private static CustomerStatusChangedV1 event(int version) {
    return new CustomerStatusChangedV1(
        UUID.randomUUID(), CustomerStatusChangedV1.TYPE, version, Instant.now(),
        "lab-request-001", "agent", "CUS-1002", "PROSPECT", "ACTIVE");
  }
}
