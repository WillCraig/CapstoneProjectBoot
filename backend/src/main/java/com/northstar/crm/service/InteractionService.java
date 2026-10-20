package com.northstar.crm.service;

import com.northstar.crm.api.dto.CreateInteractionRequest;
import com.northstar.crm.api.dto.InteractionResponse;
import com.northstar.crm.domain.InteractionEntity;
import com.northstar.crm.messaging.InteractionEventPublisher;
import com.northstar.crm.repo.CustomerRepository;
import com.northstar.crm.repo.InteractionRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
    customerRepository
        .findByPublicId(customerId)
        .orElseThrow(() -> new UnknownCustomerException(customerId));
    return interactionRepository.findByCustomer_PublicIdOrderByCreatedAtDesc(customerId).stream()
        .map(InteractionService::toResponse)
        .toList();
  }

  @Transactional
  public InteractionResponse create(CreateInteractionRequest request, String correlationHeader) {
    String correlationId = resolveCorrelationId(correlationHeader, request.correlationId());
    var customer =
        customerRepository
            .findByPublicId(request.customerId())
            .orElseThrow(() -> new UnknownCustomerException(request.customerId()));

    InteractionEntity entity = new InteractionEntity();
    entity.setId(UUID.randomUUID());
    entity.setCustomer(customer);
    entity.setInteractionType(request.interactionType());
    entity.setSummary(request.summary());
    entity.setCorrelationId(correlationId);
    entity.setCreatedAt(Instant.now());

    InteractionEntity saved = interactionRepository.save(entity);
    // Publisher defers the Kafka send until this transaction commits (see docs/architecture.md).
    eventPublisher.publish(saved);
    return toResponse(saved);
  }

  static String resolveCorrelationId(String correlationHeader, String requestCorrelationId) {
    if (correlationHeader != null && !correlationHeader.isBlank()) {
      return correlationHeader;
    }
    if (requestCorrelationId != null && !requestCorrelationId.isBlank()) {
      return requestCorrelationId;
    }
    return DEFAULT_CORRELATION;
  }

  private static InteractionResponse toResponse(InteractionEntity entity) {
    return new InteractionResponse(
        entity.getId(),
        entity.getCustomer().getPublicId(),
        entity.getInteractionType(),
        entity.getSummary(),
        entity.getCorrelationId(),
        entity.getCreatedAt());
  }
}
