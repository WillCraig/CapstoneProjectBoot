package com.northstar.crm.observability;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.northstar.crm.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** The k3s probes and the smoke script depend on these exact paths answering UP. */
@SpringBootTest
@AutoConfigureMockMvc
class HealthEndpointsIT extends AbstractIntegrationTest {

  @Autowired MockMvc mockMvc;

  @Test
  void readiness_isUp_withDatabaseReachable() throws Exception {
    mockMvc
        .perform(get("/actuator/health/readiness"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void liveness_isUp() throws Exception {
    mockMvc
        .perform(get("/actuator/health/liveness"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void correlationId_isEchoedOnResponses() throws Exception {
    mockMvc
        .perform(get("/actuator/health/liveness").header(CorrelationId.HEADER, "lab-request-001"))
        .andExpect(header().string(CorrelationId.HEADER, "lab-request-001"));
  }
}
