package com.northstar.crm.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Issues the short-lived HS256 access token that {@link SecurityConfig} validates. */
@Service
public class TokenService {

  public record IssuedToken(String accessToken, long expiresInSeconds, List<String> roles) {}

  private final JwtEncoder encoder;
  private final String issuer;
  private final Duration ttl;
  private final Clock clock;

  @Autowired
  public TokenService(JwtEncoder encoder, CrmSecurityProperties properties) {
    this(encoder, properties, Clock.systemUTC());
  }

  TokenService(JwtEncoder encoder, CrmSecurityProperties properties, Clock clock) {
    this.encoder = encoder;
    this.issuer = properties.jwt().issuer();
    this.ttl = properties.jwt().ttl() == null ? Duration.ofMinutes(15) : properties.jwt().ttl();
    this.clock = clock;
  }

  public IssuedToken issue(String username, List<String> roles) {
    return issue(username, roles, clock.instant(), ttl);
  }

  /** Visible for tests that need an already-expired token. */
  public IssuedToken issue(String username, List<String> roles, Instant issuedAt, Duration lifetime) {
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(issuer)
            .subject(username)
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plus(lifetime))
            .id(UUID.randomUUID().toString())
            .claim(SecurityConfig.ROLES_CLAIM, roles)
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    return new IssuedToken(token, lifetime.toSeconds(), List.copyOf(roles));
  }
}
