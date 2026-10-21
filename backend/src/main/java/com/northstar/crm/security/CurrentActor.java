package com.northstar.crm.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** The authenticated username (JWT {@code sub}) for audit fields such as an event's actor. */
public final class CurrentActor {

  /** Used when no user is authenticated on this thread, e.g. a background job. */
  public static final String SYSTEM = "system";

  private CurrentActor() {}

  public static String name() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
      return SYSTEM;
    }
    return auth.getName();
  }
}
