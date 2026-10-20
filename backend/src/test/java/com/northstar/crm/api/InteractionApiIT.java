package com.northstar.crm.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InteractionApiIT {

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:5432/crm");
    registry.add("spring.datasource.username", () -> "crm");
    registry.add("spring.datasource.password", () -> "change-me");
  }

  @Autowired MockMvc mockMvc;

  private String bearerToken() {
    return "Bearer " + DemoBearerFilter.DEMO_TOKEN;
  }

  @Test
  void list_withoutBearer_unauthorized() throws Exception {
    mockMvc
        .perform(get("/api/v1/interactions").param("customerId", "CUS-1001"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void listCustomers_returnsTrackedCustomersInIdOrder() throws Exception {
    mockMvc
        .perform(get("/api/v1/customers").header("Authorization", bearerToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].customerId").value("CUS-1001"))
        .andExpect(jsonPath("$[0].fullName").value("Amina Khan"))
        .andExpect(jsonPath("$[0].status").value("ACTIVE"))
        .andExpect(jsonPath("$[1].customerId").value("CUS-1002"));
  }

  @Test
  void createCustomer_generatesPublicIdAndSavesDetails() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/customers")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fullName":"Jordan Lee","status":"PROSPECT"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.customerId").isNotEmpty())
        .andExpect(jsonPath("$.fullName").value("Jordan Lee"))
        .andExpect(jsonPath("$.status").value("PROSPECT"));
  }

  @Test
  void updateCustomer_changesNameAndStatusWithoutChangingPublicId() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/customers/CUS-1001")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fullName":"Amina Khan-Smith","status":"CLOSED"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.customerId").value("CUS-1001"))
        .andExpect(jsonPath("$.fullName").value("Amina Khan-Smith"))
        .andExpect(jsonPath("$.status").value("CLOSED"));
  }

  @Test
  void createAndList_forAmina() throws Exception {
    String body =
        """
        {"customerId":"CUS-1001","interactionType":"NOTE","summary":"Follow-up on billing question","correlationId":"lab-request-001"}
        """;

    mockMvc
        .perform(
            post("/api/v1/interactions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", bearerToken())
                .header("X-Correlation-ID", "lab-request-001")
                .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.customerId").value("CUS-1001"))
        .andExpect(jsonPath("$.correlationId").value("lab-request-001"));

    mockMvc
        .perform(
            get("/api/v1/interactions")
                .param("customerId", "CUS-1001")
                .header("Authorization", bearerToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].customerId").value("CUS-1001"));
  }

  @Test
  void create_unknownCustomer_notFound() throws Exception {
    String body =
        """
        {"customerId":"CUS-9999","interactionType":"NOTE","summary":"Should fail"}
        """;

    mockMvc
        .perform(
            post("/api/v1/interactions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", bearerToken())
                .content(body))
        .andExpect(status().isNotFound());
  }
}
