package com.northstar.crm.interaction;

public class InteractionNotFoundException extends RuntimeException {
    public InteractionNotFoundException(String interactionId) {
        super("Interaction not found: " + interactionId);
    }
}