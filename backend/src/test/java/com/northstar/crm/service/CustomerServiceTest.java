package com.northstar.crm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.northstar.crm.api.dto.CreateCustomerRequest;
import com.northstar.crm.api.dto.CustomerResponse;
import com.northstar.crm.api.dto.UpdateCustomerRequest;
import com.northstar.crm.domain.CustomerEntity;
import com.northstar.crm.messaging.CustomerLifecycleEventPublisher;
import com.northstar.crm.repo.CustomerRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests for the customer management slice: the JPA repository is mocked. */
class CustomerServiceTest {

  private CustomerRepository customerRepository;
  private CustomerLifecycleEventPublisher lifecycleEvents;
  private CustomerService service;

  @BeforeEach
  void setUp() {
    customerRepository = mock(CustomerRepository.class);
    lifecycleEvents = mock(CustomerLifecycleEventPublisher.class);
    service = new CustomerService(customerRepository, lifecycleEvents);
    when(customerRepository.findByPublicId("CUS-1001"))
        .thenReturn(Optional.of(customer(1L, "CUS-1001", "Amina Khan", "ACTIVE")));
    when(customerRepository.findByPublicId("CUS-1002"))
        .thenReturn(Optional.of(customer(2L, "CUS-1002", "Ravi Singh", "PROSPECT")));
    when(customerRepository.findByPublicId("CUS-9999")).thenReturn(Optional.empty());
    when(customerRepository.save(any(CustomerEntity.class)))
        .thenAnswer(
            invocation -> {
              CustomerEntity entity = invocation.getArgument(0);
              // Stand in for the IDENTITY column: the first save assigns the database ID.
              if (entity.getId() == null) {
                ReflectionTestUtils.setField(entity, "id", 42L);
              }
              return entity;
            });
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void create_generatesPublicIdFromDatabaseId() {
    CustomerResponse response =
        service.create(new CreateCustomerRequest("  Priya Nair  ", "PROSPECT"));

    assertEquals("CUS-1042", response.customerId());
    assertEquals("Priya Nair", response.fullName());
    assertEquals("PROSPECT", response.status());
    assertNotNull(response.createdAt());
  }

  @Test
  void get_forAmina_returnsProfile() {
    CustomerResponse response = service.get("CUS-1001");

    assertEquals("CUS-1001", response.customerId());
    assertEquals("Amina Khan", response.fullName());
    assertEquals("ACTIVE", response.status());
  }

  @Test
  void get_unknownCustomer_fails() {
    assertThrows(UnknownCustomerException.class, () -> service.get("CUS-9999"));
  }

  @Test
  void search_withoutQuery_listsAllByName() {
    when(customerRepository.findAllByOrderByFullNameAsc())
        .thenReturn(
            List.of(
                customer(1L, "CUS-1001", "Amina Khan", "ACTIVE"),
                customer(2L, "CUS-1002", "Ravi Singh", "PROSPECT")));

    List<CustomerResponse> results = service.search("  ");

    assertEquals(2, results.size());
    assertEquals("Amina Khan", results.get(0).fullName());
    assertEquals("Ravi Singh", results.get(1).fullName());
  }

  @Test
  void search_withQuery_matchesIdOrNameTerm() {
    when(customerRepository
            .findByPublicIdContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrderByFullNameAsc(
                "amina", "amina"))
        .thenReturn(List.of(customer(1L, "CUS-1001", "Amina Khan", "ACTIVE")));

    List<CustomerResponse> results = service.search(" amina ");

    assertEquals(1, results.size());
    assertEquals("CUS-1001", results.get(0).customerId());
  }

  @Test
  void update_movesRaviFromProspectToActive() {
    CustomerResponse response =
        service.update("CUS-1002", new UpdateCustomerRequest("Ravi Singh", "ACTIVE"));

    assertEquals("CUS-1002", response.customerId());
    assertEquals("ACTIVE", response.status());
    verify(lifecycleEvents).publishStatusChange("CUS-1002", "PROSPECT", "ACTIVE");
  }

  @Test
  void update_nameOnly_publishesNoLifecycleEvent() {
    service.update("CUS-1001", new UpdateCustomerRequest("Amina K. Khan", "ACTIVE"));

    verify(lifecycleEvents, never()).publishStatusChange(any(), any(), any());
  }

  @Test
  void update_unknownCustomer_neverSaves() {
    assertThrows(
        UnknownCustomerException.class,
        () -> service.update("CUS-9999", new UpdateCustomerRequest("Ghost", "ACTIVE")));

    verify(customerRepository, never()).save(any());
  }

  @Test
  void update_agentSettingClosed_isDenied_andNothingIsSavedOrPublished() {
    signInAs("agent", "ROLE_AGENT");

    assertThrows(
        AccessDeniedException.class,
        () -> service.update("CUS-1001", new UpdateCustomerRequest("Amina Khan", "CLOSED")));

    verify(customerRepository, never()).save(any());
    verify(lifecycleEvents, never()).publishStatusChange(any(), any(), any());
  }

  @Test
  void update_agentReopeningClosedCustomer_isDenied() {
    CustomerEntity closed = customer(3L, "CUS-1003", "Closed Customer", "CLOSED");
    when(customerRepository.findByPublicId("CUS-1003")).thenReturn(Optional.of(closed));
    signInAs("agent", "ROLE_AGENT");

    assertThrows(
        AccessDeniedException.class,
        () -> service.update("CUS-1003", new UpdateCustomerRequest("Closed Customer", "ACTIVE")));

    assertEquals("CLOSED", closed.getStatus());
    verify(customerRepository, never()).save(any());
  }

  @Test
  void update_adminSettingClosed_closesAndPublishes() {
    signInAs("admin", "ROLE_AGENT", "ROLE_ADMIN");

    CustomerResponse response =
        service.update("CUS-1001", new UpdateCustomerRequest("Amina Khan", "CLOSED"));

    assertEquals("CLOSED", response.status());
    verify(lifecycleEvents).publishStatusChange("CUS-1001", "ACTIVE", "CLOSED");
  }

  @Test
  void delete_closesCustomerButKeepsRow() {
    CustomerEntity amina = customer(1L, "CUS-1001", "Amina Khan", "ACTIVE");
    when(customerRepository.findByPublicId("CUS-1001")).thenReturn(Optional.of(amina));

    service.delete("CUS-1001");

    assertEquals("CLOSED", amina.getStatus());
    assertTrue(amina.getId() != null);
    verify(customerRepository).save(amina);
    verify(customerRepository, never()).delete(any(CustomerEntity.class));
    verify(lifecycleEvents).publishStatusChange("CUS-1001", "ACTIVE", "CLOSED");
  }

  @Test
  void delete_alreadyClosed_isIdempotent_andPublishesNothing() {
    CustomerEntity closed = customer(3L, "CUS-1003", "Closed Customer", "CLOSED");
    when(customerRepository.findByPublicId("CUS-1003")).thenReturn(Optional.of(closed));

    service.delete("CUS-1003");

    verify(customerRepository, never()).save(any());
    verify(lifecycleEvents, never()).publishStatusChange(any(), any(), any());
  }

  @Test
  void delete_unknownCustomer_fails() {
    assertThrows(UnknownCustomerException.class, () -> service.delete("CUS-9999"));
    verify(customerRepository, never()).save(any());
    verify(lifecycleEvents, never()).publishStatusChange(any(), any(), any());
  }

  private static void signInAs(String username, String... authorities) {
    TestingAuthenticationToken auth = new TestingAuthenticationToken(username, null, authorities);
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  private static CustomerEntity customer(Long id, String publicId, String fullName, String status) {
    CustomerEntity entity = new CustomerEntity();
    ReflectionTestUtils.setField(entity, "id", id);
    entity.setPublicId(publicId);
    entity.setFullName(fullName);
    entity.setStatus(status);
    entity.setCreatedAt(Instant.parse("2026-10-20T12:00:00Z"));
    return entity;
  }
}
