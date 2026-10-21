package com.northstar.crm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.northstar.crm.api.dto.CreateInteractionRequest;
import com.northstar.crm.api.dto.InteractionResponse;
import com.northstar.crm.api.dto.UpdateInteractionRequest;
import com.northstar.crm.domain.CustomerEntity;
import com.northstar.crm.domain.InteractionEntity;
import com.northstar.crm.messaging.InteractionEventPublisher;
import com.northstar.crm.repo.CustomerRepository;
import com.northstar.crm.repo.InteractionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/** Unit tests for the Lab 49 interaction slice: JPA repositories and the publisher are mocked. */
class InteractionServiceTest {

  private CustomerRepository customerRepository;
  private InteractionRepository interactionRepository;
  private InteractionEventPublisher eventPublisher;
  private InteractionService service;
  private CustomerEntity amina;

  @BeforeEach
  void setUp() {
    customerRepository = mock(CustomerRepository.class);
    interactionRepository = mock(InteractionRepository.class);
    eventPublisher = mock(InteractionEventPublisher.class);
    service = new InteractionService(customerRepository, interactionRepository, eventPublisher);

    amina = mock(CustomerEntity.class);
    when(amina.getPublicId()).thenReturn("CUS-1001");
    when(amina.getFullName()).thenReturn("Amina Khan");
    when(customerRepository.findByPublicId("CUS-1001")).thenReturn(Optional.of(amina));
    when(customerRepository.findByPublicId("CUS-9999")).thenReturn(Optional.empty());
    when(interactionRepository.save(any(InteractionEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void create_forAmina_returnsCreatedShape() {
    CreateInteractionRequest request =
        new CreateInteractionRequest(
            "CUS-1001", "NOTE", "Follow-up on billing question", "lab-request-001");

    InteractionResponse response = service.create(request, "lab-request-001");

    assertNotNull(response.id());
    assertEquals("CUS-1001", response.customerId());
    assertEquals("NOTE", response.interactionType());
    assertEquals("Follow-up on billing question", response.summary());
    assertEquals("lab-request-001", response.correlationId());
    assertNotNull(response.createdAt());
  }

  @Test
  void create_forAmina_persistsThenPublishesSavedInteraction() {
    CreateInteractionRequest request =
        new CreateInteractionRequest(
            "CUS-1001", "NOTE", "Follow-up on billing question", "lab-request-001");

    service.create(request, "lab-request-001");

    ArgumentCaptor<InteractionEntity> saved = ArgumentCaptor.forClass(InteractionEntity.class);
    InOrder order = inOrder(interactionRepository, eventPublisher);
    order.verify(interactionRepository).save(saved.capture());
    order.verify(eventPublisher).publish(saved.getValue());
    assertEquals("lab-request-001", saved.getValue().getCorrelationId());
    assertEquals(amina, saved.getValue().getCustomer());
  }

  @Test
  void create_unknownCustomer_fails() {
    CreateInteractionRequest request =
        new CreateInteractionRequest("CUS-9999", "NOTE", "Should fail", "lab-request-001");

    assertThrows(UnknownCustomerException.class, () -> service.create(request, null));
  }

  @Test
  void create_unknownCustomer_neverPersistsOrPublishes() {
    CreateInteractionRequest request =
        new CreateInteractionRequest("CUS-9999", "NOTE", "Should fail", "lab-request-001");

    assertThrows(UnknownCustomerException.class, () -> service.create(request, null));

    verify(interactionRepository, never()).save(any());
    verify(eventPublisher, never()).publish(any());
  }

  @Test
  void create_correlationPrecedence_headerThenRequestThenDefault() {
    InteractionResponse headerWins =
        service.create(
            new CreateInteractionRequest("CUS-1001", "CALL", "Header wins", "from-body"),
            "from-header");
    assertEquals("from-header", headerWins.correlationId());

    InteractionResponse bodyFallback =
        service.create(
            new CreateInteractionRequest("CUS-1001", "CALL", "Body fallback", "from-body"), "  ");
    assertEquals("from-body", bodyFallback.correlationId());

    InteractionResponse defaulted =
        service.create(new CreateInteractionRequest("CUS-1001", "CALL", "Defaulted", null), null);
    assertEquals(InteractionService.DEFAULT_CORRELATION, defaulted.correlationId());
    assertEquals("lab-request-001", defaulted.correlationId());
  }

  @Test
  void list_forAmina_mapsEntitiesToResponsesInRepositoryOrder() {
    InteractionEntity newest = interaction("EMAIL", "Second contact", "lab-request-001");
    InteractionEntity oldest = interaction("NOTE", "First contact", "lab-request-001");
    when(interactionRepository.findByCustomer_PublicIdOrderByCreatedAtDesc("CUS-1001"))
        .thenReturn(List.of(newest, oldest));

    List<InteractionResponse> responses = service.list("CUS-1001");

    assertEquals(2, responses.size());
    assertEquals(newest.getId(), responses.get(0).id());
    assertEquals("CUS-1001", responses.get(0).customerId());
    assertEquals("EMAIL", responses.get(0).interactionType());
    assertEquals(oldest.getId(), responses.get(1).id());
  }

  @Test
  void list_unknownCustomer_fails() {
    assertThrows(UnknownCustomerException.class, () -> service.list("CUS-9999"));
    verify(interactionRepository, never()).findByCustomer_PublicIdOrderByCreatedAtDesc(any());
  }

  @Test
  void getUpdateAndSoftDelete_visibleInteraction() {
    InteractionEntity entity = interaction("NOTE", "Original summary", "lab-request-001");
    when(interactionRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

    assertEquals(entity.getId(), service.get(entity.getId()).id());

    InteractionResponse updated =
        service.update(entity.getId(), new UpdateInteractionRequest("EMAIL", "Corrected summary"));
    assertEquals("EMAIL", updated.interactionType());
    assertEquals("Corrected summary", updated.summary());
    assertNull(entity.getDeletedAt());

    service.delete(entity.getId());
    assertNotNull(entity.getDeletedAt());
    verify(eventPublisher, never()).publish(any());
  }

  @Test
  void getUpdateAndDelete_unknownInteraction_neverSaveOrPublish() {
    UUID missingId = UUID.randomUUID();
    when(interactionRepository.findById(missingId)).thenReturn(Optional.empty());

    assertThrows(InteractionNotFoundException.class, () -> service.get(missingId));
    assertThrows(
        InteractionNotFoundException.class,
        () -> service.update(missingId, new UpdateInteractionRequest("NOTE", "Nope")));
    assertThrows(InteractionNotFoundException.class, () -> service.delete(missingId));

    verify(interactionRepository, never()).save(any());
    verify(eventPublisher, never()).publish(any());
  }

  private InteractionEntity interaction(String type, String summary, String correlationId) {
    InteractionEntity entity = new InteractionEntity();
    entity.setId(UUID.randomUUID());
    entity.setCustomer(amina);
    entity.setInteractionType(type);
    entity.setSummary(summary);
    entity.setCorrelationId(correlationId);
    entity.setCreatedAt(Instant.now());
    return entity;
  }
}
