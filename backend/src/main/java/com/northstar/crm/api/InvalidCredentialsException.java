package com.northstar.crm.api;

/** Wrong username or password; deliberately does not say which. */
public class InvalidCredentialsException extends RuntimeException {
  public InvalidCredentialsException() {
    super("Invalid username or password.");
  }
}
