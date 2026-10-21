
package com.northstar.crm.interaction;

import com.northstar.crm.customer.Customer;
import com.northstar.crm.customer.CustomerRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InteractionService {

    private final CustomerRepository customerRepository;
    private final CustomerInteractionRepository interactionRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public InteractionService(
            CustomerRepository customerRepository,
            CustomerInteractionRepository interactionRepository,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.customerRepository = customerRepository;
        this.interactionRepository = interactionRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public InteractionResponse create(
            String customerId,
            CreateInteractionRequest request,
            String correlationId,
            String actor
    ) {
        Customer customer = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        CustomerInteraction interaction = new CustomerInteraction(
                customer,
                request.channel().name(),
                request.summary().trim()
        );

        CustomerInteraction saved = interactionRepository.saveAndFlush(interaction);

        CustomerInteractionRecordedV1 event =
                CustomerInteractionRecordedV1.create(
                        customer.getEventCustomerId(),
                        saved.getInteractionId(),
                        saved.getChannel(),
                        correlationId,
                        actor
                );

        applicationEventPublisher.publishEvent(event);

        return InteractionResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<InteractionResponse> list(String customerId) {
        if (!customerRepository.existsByCustomerId(customerId)) {
            throw new CustomerNotFoundException(customerId);
        }

        return interactionRepository
                .findByCustomer_CustomerIdAndDeletedFalseOrderByCreatedAtDesc(
                        customerId
                )
                .stream()
                .map(InteractionResponse::from)
                .toList();
    }

    @Transactional
    public InteractionResponse update(
            String customerId,
            UUID interactionId,
            UpdateInteractionRequest request
    ) {
        CustomerInteraction interaction = interactionRepository
                .findByInteractionIdAndCustomer_CustomerIdAndDeletedFalse(
                        interactionId,
                        customerId
                )
                .orElseThrow(() ->
                        new InteractionNotFoundException(interactionId.toString()));

        interaction.update(
                request.channel().name(),
                request.summary().trim()
        );

        return InteractionResponse.from(interaction);
    }

    @Transactional
    public void delete(String customerId, UUID interactionId) {
        CustomerInteraction interaction = interactionRepository
                .findByInteractionIdAndCustomer_CustomerIdAndDeletedFalse(
                        interactionId,
                        customerId
                )
                .orElseThrow(() ->
                        new InteractionNotFoundException(interactionId.toString()));

        interaction.softDelete();
    }
}
