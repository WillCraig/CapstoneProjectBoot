package com.northstar.crm.interaction;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class InteractionAfterCommitListener {

    private final InteractionEventPublisher publisher;

    public InteractionAfterCommitListener(InteractionEventPublisher publisher) {
        this.publisher = publisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterCommit(CustomerInteractionRecordedV1 event) {
        publisher.publish(event);
    }
}
