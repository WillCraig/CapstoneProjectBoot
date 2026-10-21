package com.northstar.crm.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.northstar.crm.repo.CustomerRepository;
import com.northstar.crm.support.AbstractIntegrationTest;
import com.northstar.crm.support.TestTokens;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The API, not Angular, is the security boundary. Negative paths first: no token, expired
 * token, forged token, and the wrong role must all be refused with a Problem Details body.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestTokens.class)
class SecurityIT extends AbstractIntegrationTest {

  @Autowired MockMvc mockMvc;
  @Autowired TestTokens tokens;
  @Autowired CustomerRepository customerRepository;
  @Autowired ObjectMapper objectMapper;

  @AfterEach
  void reopenRavi() {
    customerRepository
        .findByPublicId("CUS-1002")
        .ifPresent(
            ravi -> {
              ravi.setStatus("PROSPECT");
              customerRepository.save(ravi);
            });
  }

  @Test
  void noToken_is401ProblemDetail_withCorrelationId() throws Exception {
    mockMvc
        .perform(get("/api/v1/customers/CUS-1001").header("X-Correlation-Id", "sec-it-001"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("WWW-Authenticate", "Bearer"))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.title").value("Unauthorized"))
        .andExpect(jsonPath("$.instance").value("/api/v1/customers/CUS-1001"))
        .andExpect(jsonPath("$.correlationId").value("sec-it-001"));
  }

  @Test
  void expiredToken_is401() throws Exception {
    mockMvc
        .perform(get("/api/v1/customers/CUS-1001").header("Authorization", tokens.expiredAgentBearer()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.detail").value("Bearer token is invalid or expired."));
  }

  @Test
  void tokenSignedWithAnotherKey_is401_evenIfItClaimsAdmin() throws Exception {
    mockMvc
        .perform(delete("/api/v1/customers/CUS-1002").header("Authorization", tokens.forgedAdminBearer()))
        .andExpect(status().isUnauthorized());
    assertEquals("PROSPECT", customerRepository.findByPublicId("CUS-1002").orElseThrow().getStatus());
  }

  @Test
  void garbageToken_is401() throws Exception {
    mockMvc
        .perform(get("/api/v1/customers").header("Authorization", "Bearer lab-demo-token"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void agentClosingCustomer_is403ProblemDetail_andNothingChanges() throws Exception {
    mockMvc
        .perform(
            delete("/api/v1/customers/CUS-1002")
                .header("Authorization", tokens.agentBearer())
                .header("X-Correlation-Id", "sec-it-403"))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.correlationId").value("sec-it-403"));
    assertEquals("PROSPECT", customerRepository.findByPublicId("CUS-1002").orElseThrow().getStatus());
  }

  @Test
  void adminClosingCustomer_succeeds() throws Exception {
    mockMvc
        .perform(delete("/api/v1/customers/CUS-1002").header("Authorization", tokens.adminBearer()))
        .andExpect(status().isNoContent());
    assertEquals("CLOSED", customerRepository.findByPublicId("CUS-1002").orElseThrow().getStatus());
  }

  @Test
  void agentCanReadAndSearch() throws Exception {
    mockMvc
        .perform(get("/api/v1/customers").param("q", "amina").header("Authorization", tokens.agentBearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].customerId").value("CUS-1001"));
  }

  @Test
  void login_thenUseIssuedToken() throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"agent\",\"password\":\"agent-test-password\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.username").value("agent"))
            .andExpect(jsonPath("$.roles[0]").value("AGENT"))
            .andExpect(jsonPath("$.expiresIn").value(300))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String token = objectMapper.readTree(body).get("accessToken").asText();

    mockMvc
        .perform(get("/api/v1/customers/CUS-1001").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fullName").value("Amina Khan"));
    // ...but the AGENT token still cannot close a customer.
    mockMvc
        .perform(delete("/api/v1/customers/CUS-1002").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminLogin_carriesAdminRole() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"admin-test-password\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roles[1]").value("ADMIN"));
  }

  @Test
  void login_wrongPassword_is401_withoutSayingWhichPartWasWrong() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"agent\",\"password\":\"wrong\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.detail").value("Invalid username or password."));
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"nobody\",\"password\":\"wrong\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.detail").value("Invalid username or password."));
  }

  @Test
  void login_blankFields_is400() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"\",\"password\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void healthIsPublic_butMetricsAreAdminOnly() throws Exception {
    mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
    mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(get("/actuator/metrics").header("Authorization", tokens.agentBearer()))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get("/actuator/metrics").header("Authorization", tokens.adminBearer()))
        .andExpect(status().isOk());
  }

  @Test
  void corsPreflight_allowsLocalAngularOrigin_only() throws Exception {
    mockMvc
        .perform(
            options("/api/v1/customers")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "authorization"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    mockMvc
        .perform(
            options("/api/v1/customers")
                .header("Origin", "https://evil.example")
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isForbidden());
  }
}
