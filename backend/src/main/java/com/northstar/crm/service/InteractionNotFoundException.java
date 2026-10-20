package com.northstar.crm.service;

import java.util.UUID;

/** Raised when an interaction id does not exist or has been soft-deleted. */
public class InteractionNotFoundException extends RuntimeException {
  public InteractionNotFoundException(UUID interactionId) {
    super("Unknown interaction: " + interactionId);
  }
}
