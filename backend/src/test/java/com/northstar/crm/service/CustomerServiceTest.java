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
import com.northstar.crm.repo.CustomerRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests for the customer management slice: the JPA repository is mocked. */
class CustomerServiceTest {

  private CustomerRepository customerRepository;
  private CustomerService service;

  @BeforeEach
  void setUp() {
    customerRepository = mock(CustomerRepository.class);
    service = new CustomerService(customerRepository);
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
  }

  @Test
  void update_unknownCustomer_neverSaves() {
    assertThrows(
        UnknownCustomerException.class,
        () -> service.update("CUS-9999", new UpdateCustomerRequest("Ghost", "ACTIVE")));

    verify(customerRepository, never()).save(any());
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
  }

  @Test
  void delete_unknownCustomer_fails() {
    assertThrows(UnknownCustomerException.class, () -> service.delete("CUS-9999"));
    verify(customerRepository, never()).save(any());
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
