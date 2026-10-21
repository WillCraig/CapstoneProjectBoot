package com.northstar.crm.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.northstar.crm.observability.CorrelationId;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class CustomerLifecycleEventPublisherTest {

  private KafkaTemplate<String, CustomerStatusChangedV1> kafkaTemplate;
  private CustomerLifecycleEventPublisher publisher;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    kafkaTemplate = mock(KafkaTemplate.class);
    when(kafkaTemplate.send(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
    publisher =
        new CustomerLifecycleEventPublisher(kafkaTemplate, CustomerLifecycleEventPublisher.TOPIC);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    MDC.put(CorrelationId.MDC_KEY, "lab-request-001");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    MDC.clear();
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void statusChange_sendsV1EventKeyedByCustomer_withActorAndCorrelation() {
    publisher.publishStatusChange("CUS-1002", "PROSPECT", "ACTIVE");

    ArgumentCaptor<CustomerStatusChangedV1> sent =
        ArgumentCaptor.forClass(CustomerStatusChangedV1.class);
    verify(kafkaTemplate).send(eq("crm.customer.lifecycle.v1"), eq("CUS-1002"), sent.capture());
    CustomerStatusChangedV1 event = sent.getValue();
    assertNotNull(event.eventId());
    assertEquals("CustomerStatusChanged", event.eventType());
    assertEquals(1, event.eventVersion());
    assertNotNull(event.occurredAt());
    assertEquals("lab-request-001", event.correlationId());
    assertEquals("admin", event.actor());
    assertEquals("CUS-1002", event.customerId());
    assertEquals("PROSPECT", event.fromStatus());
    assertEquals("ACTIVE", event.toStatus());
  }

  @Test
  void insideTransaction_waitsForAfterCommit() {
    TransactionSynchronizationManager.initSynchronization();

    publisher.publishStatusChange("CUS-1001", "ACTIVE", "CLOSED");
    verify(kafkaTemplate, never()).send(any(), any(), any());

    TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCommit());
    verify(kafkaTemplate).send(eq("crm.customer.lifecycle.v1"), eq("CUS-1001"), any());
  }

  @Test
  void rolledBackTransaction_neverPublishes() {
    TransactionSynchronizationManager.initSynchronization();

    publisher.publishStatusChange("CUS-1001", "ACTIVE", "CLOSED");
    TransactionSynchronizationManager.getSynchronizations()
        .forEach(s -> s.afterCompletion(org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK));

    verify(kafkaTemplate, never()).send(any(), any(), any());
  }
}
