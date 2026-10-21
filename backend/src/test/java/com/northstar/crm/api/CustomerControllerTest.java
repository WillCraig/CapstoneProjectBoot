package com.northstar.crm.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.northstar.crm.api.dto.CustomerResponse;
import com.northstar.crm.api.dto.UpdateCustomerRequest;
import com.northstar.crm.service.CustomerService;
import com.northstar.crm.service.UnknownCustomerException;
import java.time.Instant;
import java.util.List;
import com.northstar.crm.support.TestTokens;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.annotation.Import;
import com.northstar.crm.security.SecurityConfig;
import com.northstar.crm.security.TokenService;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** HTTP contract for /api/v1/customers with the service mocked (no DB, no Kafka). */
@WebMvcTest(CustomerController.class)
@Import({SecurityConfig.class, TokenService.class, TestTokens.class})
@ActiveProfiles("test")
class CustomerControllerTest {

  @Autowired TestTokens tokens;

  /** Real signed JWT for the AGENT demo user (was the hardcoded demo token). */
  private String bearer;

  @BeforeEach
  void issueAgentToken() {
    bearer = tokens.agentBearer();
  }
  private static final Instant CREATED = Instant.parse("2026-10-20T12:00:00Z");

  @Autowired MockMvc mockMvc;

  @MockBean CustomerService customerService;

  @Test
  void create_returns201WithLocation() throws Exception {
    when(customerService.create(any()))
        .thenReturn(new CustomerResponse("CUS-1003", "Priya Nair", "PROSPECT", CREATED));

    mockMvc
        .perform(
            post("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", bearer)
                .content(
                    """
                    {"fullName":"Priya Nair","status":"PROSPECT"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/customers/CUS-1003"))
        .andExpect(jsonPath("$.customerId").value("CUS-1003"))
        .andExpect(jsonPath("$.fullName").value("Priya Nair"))
        .andExpect(jsonPath("$.status").value("PROSPECT"));
  }

  @Test
  void create_closedStatus_returns400ProblemDetail() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", bearer)
                .content(
                    """
                    {"fullName":"Priya Nair","status":"CLOSED"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail").value("Validation failed"))
        .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.startsWith("status:")));

    verify(customerService, never()).create(any());
  }

  @Test
  void create_blankName_returns400ProblemDetail() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", bearer)
                .content(
                    """
                    {"fullName":"   ","status":"ACTIVE"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Validation failed"));

    verify(customerService, never()).create(any());
  }

  @Test
  void search_returnsMatchingCustomers() throws Exception {
    when(customerService.search("amina"))
        .thenReturn(List.of(new CustomerResponse("CUS-1001", "Amina Khan", "ACTIVE", CREATED)));

    mockMvc
        .perform(get("/api/v1/customers").param("q", "amina").header("Authorization", bearer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].customerId").value("CUS-1001"))
        .andExpect(jsonPath("$[0].fullName").value("Amina Khan"));
  }

  @Test
  void get_forAmina_returnsProfile() throws Exception {
    when(customerService.get("CUS-1001"))
        .thenReturn(new CustomerResponse("CUS-1001", "Amina Khan", "ACTIVE", CREATED));

    mockMvc
        .perform(get("/api/v1/customers/{id}", "CUS-1001").header("Authorization", bearer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fullName").value("Amina Khan"))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  void get_unknownCustomer_returns404ProblemDetail() throws Exception {
    when(customerService.get("CUS-9999")).thenThrow(new UnknownCustomerException("CUS-9999"));

    mockMvc
        .perform(get("/api/v1/customers/{id}", "CUS-9999").header("Authorization", bearer))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail").value("Unknown customer: CUS-9999"))
        .andExpect(jsonPath("$.instance").value("/api/v1/customers/CUS-9999"));
  }

  @Test
  void updateAndDelete_forRavi() throws Exception {
    when(customerService.update(any(), any()))
        .thenReturn(new CustomerResponse("CUS-1002", "Ravi Singh", "ACTIVE", CREATED));

    mockMvc
        .perform(
            put("/api/v1/customers/{id}", "CUS-1002")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", bearer)
                .content(
                    """
                    {"fullName":"Ravi Singh","status":"ACTIVE"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));
    verify(customerService).update("CUS-1002", new UpdateCustomerRequest("Ravi Singh", "ACTIVE"));

    mockMvc
        .perform(
            delete("/api/v1/customers/{id}", "CUS-1002")
                .header("Authorization", tokens.adminBearer())) // closing is ADMIN-only
        .andExpect(status().isNoContent());
    verify(customerService).delete("CUS-1002");
  }

  @Test
  void update_invalidStatus_returns400ProblemDetail() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/customers/{id}", "CUS-1002")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", bearer)
                .content(
                    """
                    {"fullName":"Ravi Singh","status":"VIP"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Validation failed"));

    verify(customerService, never()).update(eq("CUS-1002"), any());
  }

  @Test
  void create_withoutBearer_returns401() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"fullName":"Priya Nair","status":"PROSPECT"}
                    """))
        .andExpect(status().isUnauthorized());

    verify(customerService, never()).create(any());
  }
}
