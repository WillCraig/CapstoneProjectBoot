package com.northstar.crm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.northstar.crm.api.dto.CreateInteractionRequest;
import com.northstar.crm.api.dto.InteractionResponse;
import com.northstar.crm.domain.CustomerEntity;
import com.northstar.crm.domain.InteractionEntity;
import com.northstar.crm.messaging.InteractionEventPublisher;
import com.northstar.crm.repo.CustomerRepository;
import com.northstar.crm.repo.InteractionRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit test carried forward from Lab 49, adapted to the Lab 50 JPA repositories (mocked).
 *
 * <p>Uses mocked repositories to cover the interaction creation flow.
 */
class InteractionServiceTest {

  private CustomerRepository customerRepository;
  private InteractionRepository interactionRepository;
  private InteractionService service;

  @BeforeEach
  void setUp() {
    customerRepository = mock(CustomerRepository.class);
    interactionRepository = mock(InteractionRepository.class);
    service =
        new InteractionService(
            customerRepository, interactionRepository, mock(InteractionEventPublisher.class));

    CustomerEntity amina = mock(CustomerEntity.class);
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
    assertEquals("lab-request-001", response.correlationId());
  }

  @Test
  void create_unknownCustomer_fails() {
    CreateInteractionRequest request =
        new CreateInteractionRequest("CUS-9999", "NOTE", "Should fail", "lab-request-001");

    assertThrows(UnknownCustomerException.class, () -> service.create(request, null));
  }
}
