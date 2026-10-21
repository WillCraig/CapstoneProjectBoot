package com.northstar.crm.support;

import com.northstar.crm.security.TokenService;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Real signed JWTs for tests (via the app's own {@link TokenService}), so tests exercise the
 * actual validation path instead of mocking the security context. Import with
 * {@code @Import(TestTokens.class)}.
 */
@TestComponent
public class TestTokens {

  private final TokenService tokens;

  public TestTokens(TokenService tokens) {
    this.tokens = tokens;
  }

  /** {@code agent}: role AGENT. */
  public String agentBearer() {
    return "Bearer " + tokens.issue("agent", List.of("AGENT")).accessToken();
  }

  /** {@code admin}: roles AGENT + ADMIN. */
  public String adminBearer() {
    return "Bearer " + tokens.issue("admin", List.of("AGENT", "ADMIN")).accessToken();
  }

  /** Correctly signed but expired 10 minutes ago (beyond the 60 s clock skew). */
  public String expiredAgentBearer() {
    Instant issued = Instant.now().minus(Duration.ofMinutes(30));
    return "Bearer "
        + tokens.issue("agent", List.of("AGENT"), issued, Duration.ofMinutes(20)).accessToken();
  }

  /** Claims ADMIN but is signed with a key the server does not know. */
  public String forgedAdminBearer() {
    byte[] key = "attacker-key-attacker-key-attacker-key!!".getBytes(StandardCharsets.UTF_8);
    NimbusJwtEncoder forger =
        new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(key, "HmacSHA256")));
    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer("northstar-crm")
            .subject("mallory")
            .issuedAt(now)
            .expiresAt(now.plus(Duration.ofMinutes(5)))
            .claim("roles", List.of("AGENT", "ADMIN"))
            .build();
    return "Bearer "
        + forger
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
  }
}
