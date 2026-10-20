package com.northstar.crm.service;

import com.northstar.crm.api.dto.CreateInteractionRequest;
import com.northstar.crm.api.dto.InteractionResponse;
import com.northstar.crm.domain.CustomerEntity;
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
    if (!customerRepository.existsByPublicId(customerId)) {
      throw new UnknownCustomerException("Unknown customer: " + customerId);
    }

    return interactionRepository.findByCustomer_PublicIdOrderByCreatedAtDesc(customerId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public InteractionResponse create(CreateInteractionRequest request, String correlationHeader) {
    CustomerEntity customer =
        customerRepository
            .findByPublicId(request.customerId())
            .orElseThrow(
                () -> new UnknownCustomerException("Unknown customer: " + request.customerId()));

    String correlationId = resolveCorrelationId(correlationHeader, request.correlationId());

    InteractionEntity interaction = new InteractionEntity();
    interaction.setId(UUID.randomUUID());
    interaction.setCustomer(customer);
    interaction.setInteractionType(request.interactionType());
    interaction.setSummary(request.summary());
    interaction.setCorrelationId(correlationId);
    interaction.setCreatedAt(Instant.now());

    InteractionEntity saved = interactionRepository.save(interaction);
    eventPublisher.publish(saved);
    return toResponse(saved);
  }

  private String resolveCorrelationId(String correlationHeader, String requestCorrelationId) {
    if (correlationHeader != null && !correlationHeader.isBlank()) {
      return correlationHeader;
    }
    if (requestCorrelationId != null && !requestCorrelationId.isBlank()) {
      return requestCorrelationId;
    }
    return DEFAULT_CORRELATION;
  }

  private InteractionResponse toResponse(InteractionEntity interaction) {
    return new InteractionResponse(
        interaction.getId(),
        interaction.getCustomer().getPublicId(),
        interaction.getInteractionType(),
        interaction.getSummary(),
        interaction.getCorrelationId(),
        interaction.getCreatedAt());
  }
}
