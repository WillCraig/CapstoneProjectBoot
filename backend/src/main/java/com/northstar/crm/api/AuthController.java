package com.northstar.crm.api;

import com.northstar.crm.api.dto.LoginRequest;
import com.northstar.crm.api.dto.LoginResponse;
import com.northstar.crm.security.DemoUserDirectory;
import com.northstar.crm.security.TokenService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exchanges demo credentials for a short-lived JWT. The password is never logged or echoed. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private static final Logger log = LoggerFactory.getLogger(AuthController.class);

  private final DemoUserDirectory users;
  private final TokenService tokens;

  public AuthController(DemoUserDirectory users, TokenService tokens) {
    this.users = users;
    this.tokens = tokens;
  }

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    DemoUserDirectory.Account account =
        users
            .authenticate(request.username(), request.password())
            .orElseThrow(
                () -> {
                  log.info("Login failed for username={}", request.username());
                  return new InvalidCredentialsException();
                });
    TokenService.IssuedToken issued = tokens.issue(account.username(), account.roles());
    log.info("Login succeeded for username={} roles={}", account.username(), account.roles());
    return new LoginResponse(
        issued.accessToken(), "Bearer", issued.expiresInSeconds(), account.username(), issued.roles());
  }
}
