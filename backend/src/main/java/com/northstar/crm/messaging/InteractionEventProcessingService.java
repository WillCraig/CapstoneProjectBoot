package com.northstar.crm.messaging;

import com.northstar.crm.interaction.CustomerInteractionRecordedV1;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InteractionEventProcessingService {

    private final ProcessedEventRepository repository;

    public InteractionEventProcessingService(
            ProcessedEventRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional
    public void processOnce(CustomerInteractionRecordedV1 event) {
        if (repository.existsById(event.eventId())) {
            return;
        }

        try {
            repository.saveAndFlush(new ProcessedEvent(event.eventId()));
        } catch (DataIntegrityViolationException duplicate) {
            // A duplicate event must not be processed again.
            return;
        }

        // Add downstream business processing here when required.
        // Keep the processing and deduplication record in this transaction.
    }
}
