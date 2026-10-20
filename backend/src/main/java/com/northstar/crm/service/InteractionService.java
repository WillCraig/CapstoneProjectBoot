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
    if (!customerRepository.existsByPublicId(customerId) ) {
      throw new UnknownCustomerException("Unknown customer: " + customerId);
    }
    // TODO 2: load interactions for the public id, newest first
    //         (interactionRepository.findByCustomer_PublicIdOrderByCreatedAtDesc)
    List<InteractionResponse> interactions = interactionRepository
        .findByCustomer_PublicIdOrderByCreatedAtDesc(customerId)
        .stream()
        .map(interaction -> new InteractionResponse(
            interaction.getId(),
            interaction.getCustomer().getPublicId(),
            interaction.getInteractionType(),
            interaction.getSummary(),
            interaction.getCorrelationId(),
            interaction.getCreatedAt()
        ))
        .toList();
    // TODO 3: map each entity to InteractionResponse (never return the entity)
    return interactions;
    //throw new UnsupportedOperationException("TODO: implement InteractionService.list");
  }

  @Transactional
  public InteractionResponse create(CreateInteractionRequest request, String correlationHeader) {
    // TODO 1: resolve correlationId (header > request > DEFAULT_CORRELATION "lab-request-001")
    String resolvedCorrelationID;
    if(correlationHeader != null && !correlationHeader.isBlank()) {
      resolvedCorrelationID = correlationHeader;
    } else if(request.correlationID() != null && !request.correlationID().isBlank()) {
      resolvedCorrelationID = request.correlationID();
    } else {
      resolvedCorrelationID = "lab-request-001";
    }
    // TODO 2: look up the customer by public id (e.g. CUS-1001); unknown (CUS-9999 / missing)
    //         -> throw UnknownCustomerException
    if(request.customerId() == null || request.customerId().isBlank() || !interactionRepository.customerExists(request.customerId())) {
      throw new UnknownCustomerException("Unknown customer: " + (request.customerId() == null ? "<missing>" : request.customerId()));
    }
    // TODO 3: map request -> InteractionEntity (new UUID id, customer, type, summary,
    //         correlationId, createdAt = Instant.now())
    Interaction interaction = new Interaction();
    interaction.setCustomerId(request.customerId());
    interaction.setInteractionType(request.interactionType());
    interaction.setSummary(request.summary());
    interaction.setCorrelationID(resolvedCorrelationID);
    interaction.setCreatedAt(Instant.now());
    // TODO 4: save via interactionRepository
    Interaction savedInteraction = interactionRepository.save(interaction);
    // TODO 5: publish CustomerInteractionRecordedV1 via eventPublisher.publish(saved)
    eventPublisher.publish(savedInteraction);
    // TODO 6: return InteractionResponse (the DTO, not the entity)
    return new InteractionResponse(
        savedInteraction.getId(),
        savedInteraction.getCustomerId(),
        savedInteraction.getInteractionType(),
        savedInteraction.getSummary(),
        savedInteraction.getCorrelationID(),
        savedInteraction.getCreatedAt()
    );
    //throw new UnsupportedOperationException("TODO: implement InteractionService.create");
  }
}
