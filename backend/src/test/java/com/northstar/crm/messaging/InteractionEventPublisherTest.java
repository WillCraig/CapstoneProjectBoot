package com.northstar.crm.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.northstar.crm.domain.CustomerEntity;
import com.northstar.crm.domain.InteractionEntity;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class InteractionEventPublisherTest {

  private KafkaTemplate<String, CustomerInteractionRecordedV1> kafkaTemplate;
  private InteractionEventPublisher publisher;
  private InteractionEntity interaction;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    kafkaTemplate = mock(KafkaTemplate.class);
    publisher = new InteractionEventPublisher(kafkaTemplate, InteractionEventPublisher.TOPIC);

    CustomerEntity amina = mock(CustomerEntity.class);
    when(amina.getPublicId()).thenReturn("CUS-1001");

    interaction = new InteractionEntity();
    interaction.setId(UUID.randomUUID());
    interaction.setCustomer(amina);
    interaction.setInteractionType("NOTE");
    interaction.setSummary("Requested address update");
    interaction.setCorrelationId("lab-request-001");
    interaction.setCreatedAt(Instant.now());
  }

  @AfterEach
  void clearSynchronization() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void publish_withoutTransaction_sendsV1EventKeyedByCustomerId() {
    publisher.publish(interaction);

    ArgumentCaptor<CustomerInteractionRecordedV1> sent =
        ArgumentCaptor.forClass(CustomerInteractionRecordedV1.class);
    verify(kafkaTemplate).send(eq("crm.customer.interactions.v1"), eq("CUS-1001"), sent.capture());

    CustomerInteractionRecordedV1 event = sent.getValue();
    assertNotNull(event.eventId());
    assertEquals("CustomerInteractionRecorded", event.eventType());
    assertEquals(1, event.eventVersion());
    assertNotNull(event.occurredAt());
    assertEquals("lab-request-001", event.correlationId());
    assertEquals(InteractionEventPublisher.DEFAULT_ACTOR, event.actor());
    assertEquals("CUS-1001", event.customerId());
    assertEquals(interaction.getId().toString(), event.interactionId());
    assertEquals("NOTE", event.interactionType());
  }

  @Test
  void publish_insideTransaction_waitsForAfterCommit() {
    TransactionSynchronizationManager.initSynchronization();

    publisher.publish(interaction);
    verify(kafkaTemplate, never()).send(any(), any(), any());

    TransactionSynchronizationManager.getSynchronizations()
        .forEach(sync -> sync.afterCommit());
    verify(kafkaTemplate).send(eq("crm.customer.interactions.v1"), eq("CUS-1001"), any());
  }
}
