package com.northstar.crm.interaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerInteractionRepository
        extends JpaRepository<CustomerInteraction, UUID> {

    List<CustomerInteraction> findByCustomer_CustomerIdAndDeletedFalseOrderByCreatedAtDesc(
            String customerId
    );

    Optional<CustomerInteraction> findByInteractionIdAndCustomer_CustomerIdAndDeletedFalse(
            UUID interactionId,
            String customerId
    );
}