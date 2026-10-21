package com.northstar.crm.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.northstar.crm.domain.CustomerEntity;
import com.northstar.crm.repo.CustomerRepository;
import com.northstar.crm.support.AbstractIntegrationTest;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Full-stack IT for customer management: HTTP -> service -> PostgreSQL (Flyway seed), via
 * Testcontainers. The containers are shared with the other ITs, so each test restores the
 * seeded customers (Amina ACTIVE, Ravi PROSPECT) and removes customers it created.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CustomerApiIT extends AbstractIntegrationTest {

  private static final String BEARER =
      String.join(" ", "Bearer", DemoBearerFilter.DEMO_TOKEN);
  private static final Set<String> SEEDED = Set.of("CUS-1001", "CUS-1002");

  @Autowired MockMvc mockMvc;

  @Autowired CustomerRepository customerRepository;

  @Autowired ObjectMapper objectMapper;

  @AfterEach
  void restoreSeeds() {
    for (CustomerEntity customer : customerRepository.findAll()) {
      if (!SEEDED.contains(customer.getPublicId())) {
        customerRepository.delete(customer);
      }
    }
    resetCustomer("CUS-1001", "ACTIVE");
    resetCustomer("CUS-1002", "PROSPECT");
  }

  @Test
  void search_findsSeededAminaByName() throws Exception {
    mockMvc
        .perform(get("/api/v1/customers").param("q", "amina").header("Authorization", BEARER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].customerId").value("CUS-1001"))
        .andExpect(jsonPath("$[0].fullName").value("Amina Khan"))
        .andExpect(jsonPath("$[0].status").value("ACTIVE"));
  }

  @Test
  void create_persistsRowWithGeneratedPublicId() throws Exception {
    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", BEARER)
                    .content(
                        """
                        {"fullName":"Priya Nair","status":"PROSPECT"}
                        """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.fullName").value("Priya Nair"))
            .andReturn();

    String customerId =
        objectMapper.readTree(created.getResponse().getContentAsString()).get("customerId").asText();
    assertTrue(
        customerId.startsWith("CUS-"), "generated ID keeps the CUS-#### shape: " + customerId);
    String location = created.getResponse().getHeader("Location");
    assertTrue(location != null && location.endsWith("/" + customerId));

    // Persistence proof: the row is really in PostgreSQL, readable by public ID.
    CustomerEntity stored = customerRepository.findByPublicId(customerId).orElseThrow();
    assertEquals("Priya Nair", stored.getFullName());
    assertEquals("PROSPECT", stored.getStatus());
  }

  @Test
  void update_movesRaviFromProspectToActiveInDatabase() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/customers/{id}", "CUS-1002")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", BEARER)
                .content(
                    """
                    {"fullName":"Ravi Singh","status":"ACTIVE"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));

    assertEquals("ACTIVE", customerRepository.findByPublicId("CUS-1002").orElseThrow().getStatus());
  }

  @Test
  void delete_closesCustomerButRowSurvives() throws Exception {
    mockMvc
        .perform(delete("/api/v1/customers/{id}", "CUS-1002").header("Authorization", BEARER))
        .andExpect(status().isNoContent());

    CustomerEntity stored = customerRepository.findByPublicId("CUS-1002").orElseThrow();
    assertEquals("CLOSED", stored.getStatus());
  }

  @Test
  void get_unknownCustomer_notFound() throws Exception {
    mockMvc
        .perform(get("/api/v1/customers/{id}", "CUS-9999").header("Authorization", BEARER))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.detail").value("Unknown customer: CUS-9999"));
  }

  @Test
  void create_invalidStatus_badRequest_andNothingPersisted() throws Exception {
    long before = customerRepository.count();

    mockMvc
        .perform(
            post("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", BEARER)
                .content(
                    """
                    {"fullName":"Priya Nair","status":"VIP"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Validation failed"));

    assertEquals(before, customerRepository.count());
  }

  @Test
  void search_withoutBearer_unauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/customers")).andExpect(status().isUnauthorized());
  }

  private void resetCustomer(String publicId, String status) {
    customerRepository
        .findByPublicId(publicId)
        .ifPresent(
            customer -> {
              customer.setStatus(status);
              customerRepository.save(customer);
            });
  }
}
