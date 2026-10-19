package com.northstar.crm.service;

import com.northstar.crm.api.dto.CreateInteractionRequest;
import com.northstar.crm.api.dto.InteractionResponse;
import com.northstar.crm.domain.Interaction;
import com.northstar.crm.messaging.InteractionEventPublisher;
import com.northstar.crm.repo.InteractionRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class InteractionService {

  private final InteractionRepository interactionRepository;
  private final InteractionEventPublisher eventPublisher;

  public InteractionService(
      InteractionRepository interactionRepository, InteractionEventPublisher eventPublisher) {
    this.interactionRepository = interactionRepository;
    this.eventPublisher = eventPublisher;
  }

  /**
   * Session block: implement create flow. Full path: @Transactional + Kafka + Problem Details.
   */
  public InteractionResponse create(CreateInteractionRequest request, String correlationHeader) {
    // TODO 1: resolve correlationId (header > request > "lab-request-001")
    String resolvedCorrelationID;
    if(correlationHeader != null && !correlationHeader.isBlank()) {
      resolvedCorrelationID = correlationHeader;
    } else if(request.correlationID() != null && !request.correlationID().isBlank()) {
      resolvedCorrelationID = request.correlationID();
    } else {
      resolvedCorrelationID = "lab-request-001";
    }
    // TODO 2: reject unknown customer (CUS-9999 / missing) — throw domain exception
    if(request.customerId() == null || request.customerId().isBlank() || !interactionRepository.customerExists(request.customerId())) {
      throw new UnknownCustomerException("Unknown customer: " + (request.customerId() == null ? "<missing>" : request.customerId()));
    }
    // TODO 3: map request → Interaction, set createdAt = Instant.now()
    Interaction interaction = new Interaction();
    interaction.setCustomerId(request.customerId());
    interaction.setInteractionType(request.interactionType());
    interaction.setSummary(request.summary());
    interaction.setCorrelationID(resolvedCorrelationID);
    interaction.setCreatedAt(Instant.now());

    // TODO 4: save via repository
    Interaction savedInteraction = interactionRepository.save(interaction);

    // TODO 5: publish event
    eventPublisher.publish(savedInteraction);

    // TODO 6: return InteractionResponse
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
