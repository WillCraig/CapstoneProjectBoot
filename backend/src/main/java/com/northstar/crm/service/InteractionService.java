package com.northstar.crm.service;

import com.northstar.crm.api.dto.CreateInteractionRequest;
import com.northstar.crm.api.dto.InteractionResponse;
import com.northstar.crm.messaging.InteractionEventPublisher;
import com.northstar.crm.repo.CustomerRepository;
import com.northstar.crm.repo.InteractionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InteractionService {

  public static final String DEFAULT_CORRELATION = "lab-request-001";

  private final CustomerRepository customerRepository;
  private final InteractionRepository interactionRepository;
  private final InteractionEventPublisher eventPublisher;

  public InteractionService(
      CustomerRepository customerRepository,
      InteractionRepository interactionRepository,
      InteractionEventPublisher eventPublisher) {
    this.customerRepository = customerRepository;
    this.interactionRepository = interactionRepository;
    this.eventPublisher = eventPublisher;
  }

  @Transactional(readOnly = true)
  public List<InteractionResponse> list(String customerId) {
    // TODO 1: reject unknown customer (CUS-9999 / missing) - throw UnknownCustomerException
    // TODO 2: load interactions for the public id, newest first
    //         (interactionRepository.findByCustomer_PublicIdOrderByCreatedAtDesc)
    // TODO 3: map each entity to InteractionResponse (never return the entity)
    throw new UnsupportedOperationException("TODO: implement InteractionService.list");
  }

  @Transactional
  public InteractionResponse create(CreateInteractionRequest request, String correlationHeader) {
    // TODO 1: resolve correlationId (header > request > DEFAULT_CORRELATION "lab-request-001")
    // TODO 2: look up the customer by public id (e.g. CUS-1001); unknown (CUS-9999 / missing)
    //         -> throw UnknownCustomerException
    // TODO 3: map request -> InteractionEntity (new UUID id, customer, type, summary,
    //         correlationId, createdAt = Instant.now())
    // TODO 4: save via interactionRepository
    // TODO 5: publish CustomerInteractionRecordedV1 via eventPublisher.publish(saved)
    // TODO 6: return InteractionResponse (the DTO, not the entity)
    throw new UnsupportedOperationException("TODO: implement InteractionService.create");
  }
}
