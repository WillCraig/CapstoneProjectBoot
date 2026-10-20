package com.northstar.crm.messaging;

/**
 * An event arrived with an eventVersion this consumer does not understand. Retrying can never
 * fix that, so the Kafka error handler treats it as non-retryable and routes it to the DLT.
 */
public class UnsupportedEventVersionException extends RuntimeException {
  public UnsupportedEventVersionException(int eventVersion) {
    super("Unsupported eventVersion: " + eventVersion);
  }
}
