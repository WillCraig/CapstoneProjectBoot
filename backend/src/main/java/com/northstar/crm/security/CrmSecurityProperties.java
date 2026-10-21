package com.northstar.crm.security;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Security settings under {@code crm.security}. Secret values arrive from the environment
 * ({@code CRM_JWT_SECRET}, {@code CRM_AGENT_PASSWORD}, {@code CRM_ADMIN_PASSWORD}); only the
 * {@code local} and {@code test} profiles carry labelled throwaway defaults.
 */
@ConfigurationProperties(prefix = "crm.security")
public record CrmSecurityProperties(
    Jwt jwt, List<String> corsAllowedOrigins, Map<String, DemoUser> users) {

  /**
   * @param secret HMAC-SHA256 signing key, at least 32 bytes
   * @param issuer {@code iss} claim written on login and required on every request
   * @param ttl access-token lifetime; short because there is no refresh or revocation
   */
  public record Jwt(String secret, String issuer, Duration ttl) {}

  /**
   * Synthetic demo account. {@code password} is either a BCrypt hash ({@code $2a$...}, the form
   * to use in deployed environments) or a plain dev value that is BCrypt-hashed at startup.
   */
  public record DemoUser(String password, List<String> roles) {}
}
