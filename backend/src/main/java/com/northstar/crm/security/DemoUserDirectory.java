package com.northstar.crm.security;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * The synthetic accounts allowed to log in ({@code agent}: AGENT; {@code admin}: AGENT+ADMIN).
 * A stand-in for a real identity provider: the rest of the app only ever sees a JWT, so swapping
 * this for an external IdP later does not touch the controllers. Passwords are held only as
 * BCrypt hashes; an account with no configured password is disabled, never given a default.
 */
@Component
public class DemoUserDirectory {

  private static final Logger log = LoggerFactory.getLogger(DemoUserDirectory.class);

  public record Account(String username, String passwordHash, List<String> roles) {}

  private final PasswordEncoder passwordEncoder;
  private final Map<String, Account> accounts = new LinkedHashMap<>();
  private final String timingDummyHash;

  public DemoUserDirectory(CrmSecurityProperties properties, PasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
    this.timingDummyHash = passwordEncoder.encode("timing-equaliser-not-a-password");
    Map<String, CrmSecurityProperties.DemoUser> users =
        properties.users() == null ? Map.of() : properties.users();
    users.forEach(
        (username, user) -> {
          if (user.password() == null || user.password().isBlank()) {
            log.warn("Demo user '{}' is disabled: no password configured", username);
            return;
          }
          String hash =
              isBcryptHash(user.password())
                  ? user.password()
                  : passwordEncoder.encode(user.password());
          accounts.put(username, new Account(username, hash, List.copyOf(user.roles())));
        });
  }

  public Optional<Account> authenticate(String username, String rawPassword) {
    Account account = accounts.get(username);
    if (account == null) {
      // Same BCrypt cost for unknown users so response time does not reveal valid usernames.
      passwordEncoder.matches(rawPassword, timingDummyHash);
      return Optional.empty();
    }
    return passwordEncoder.matches(rawPassword, account.passwordHash())
        ? Optional.of(account)
        : Optional.empty();
  }

  private static boolean isBcryptHash(String value) {
    return value.matches("^\\$2[aby]\\$\\d{2}\\$.{53}$");
  }
}
