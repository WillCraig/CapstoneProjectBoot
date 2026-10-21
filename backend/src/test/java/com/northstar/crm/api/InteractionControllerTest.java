package com.northstar.crm.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.northstar.crm.api.dto.CreateInteractionRequest;
import com.northstar.crm.api.dto.InteractionResponse;
import com.northstar.crm.api.dto.UpdateInteractionRequest;
import com.northstar.crm.service.InteractionNotFoundException;
import com.northstar.crm.service.InteractionService;
import com.northstar.crm.service.UnknownCustomerException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** HTTP contract for /api/v1/interactions with the service mocked (no DB, no Kafka). */
@WebMvcTest(InteractionController.class)
class InteractionControllerTest {

  // Demo token from the filter itself; Lab 51 replaces this scheme with JWT.
  private static final String BEARER = "Bearer " + DemoBearerFilter.DEMO_TOKEN;

  @Autowired MockMvc mockMvc;

  @MockBean InteractionService interactionService;

  @Test
  void create_forAmina_returns201() throws Exception {
    UUID id = UUID.randomUUID();
    when(interactionService.create(any(), any()))
        .thenReturn(
            new InteractionResponse(
                id, "CUS-1001", "NOTE", "Requested address update", "lab-request-001",
                Instant.parse("2026-10-20T12:00:00Z")));

    mockMvc
        .perform(
            post("/api/v1/interactions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", BEARER)
                .header("X-Correlation-ID", "lab-request-001")
                .content(
                    """
                    {"customerId":"CUS-1001","interactionType":"NOTE","summary":"Requested address update","correlationId":"lab-request-001"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.customerId").value("CUS-1001"))
        .andExpect(jsonPath("$.interactionType").value("NOTE"))
        .andExpect(jsonPath("$.correlationId").value("lab-request-001"));

    // The X-Correlation-ID header must reach the service unchanged.
    ArgumentCaptor<String> correlation = ArgumentCaptor.forClass(String.class);
    verify(interactionService)
        .create(any(CreateInteractionRequest.class), correlation.capture());
    org.junit.jupiter.api.Assertions.assertEquals("lab-request-001", correlation.getValue());
  }

  @Test
  void create_invalidInteractionType_returns400ProblemDetail() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/interactions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", BEARER)
                .header("X-Correlation-ID", "lab-request-001")
                .content(
                    """
                    {"customerId":"CUS-1001","interactionType":"TEXT","summary":"Bad type"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("about:blank"))
        .andExpect(jsonPath("$.title").value("Bad Request"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value("Validation failed"))
        .andExpect(jsonPath("$.instance").value("/api/v1/interactions"))
        .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.startsWith("interactionType:")));

    verify(interactionService, never()).create(any(), any());
  }

  @Test
  void create_blankSummary_returns400ProblemDetail() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/interactions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", BEARER)
                .content(
                    """
                    {"customerId":"CUS-1001","interactionType":"NOTE","summary":"   "}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value("Validation failed"));

    verify(interactionService, never()).create(any(), any());
  }

  @Test
  void create_unknownCustomer_returns404ProblemDetail() throws Exception {
    when(interactionService.create(any(), any()))
        .thenThrow(new UnknownCustomerException("CUS-9999"));

    mockMvc
        .perform(
            post("/api/v1/interactions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", BEARER)
                .header("X-Correlation-ID", "lab-request-001")
                .content(
                    """
                    {"customerId":"CUS-9999","interactionType":"NOTE","summary":"Should fail"}
                    """))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Not Found"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.detail").value("Unknown customer: CUS-9999"))
        .andExpect(jsonPath("$.instance").value("/api/v1/interactions"))
        .andExpect(jsonPath("$.correlationId").value("lab-request-001"));
  }

  @Test
  void list_forAmina_returns200Timeline() throws Exception {
    when(interactionService.list("CUS-1001"))
        .thenReturn(
            List.of(
                new InteractionResponse(
                    UUID.randomUUID(), "CUS-1001", "NOTE", "Follow-up", "lab-request-001",
                    Instant.parse("2026-10-20T12:00:00Z"))));

    mockMvc
        .perform(
            get("/api/v1/interactions")
                .param("customerId", "CUS-1001")
                .header("Authorization", BEARER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].customerId").value("CUS-1001"))
        .andExpect(jsonPath("$[0].summary").value("Follow-up"));
  }

  @Test
  void list_unknownCustomer_returns404ProblemDetail() throws Exception {
    when(interactionService.list("CUS-9999"))
        .thenThrow(new UnknownCustomerException("CUS-9999"));

    mockMvc
        .perform(
            get("/api/v1/interactions")
                .param("customerId", "CUS-9999")
                .header("Authorization", BEARER))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  void detailUpdateDelete_forInteraction() throws Exception {
    UUID id = UUID.randomUUID();
    InteractionResponse original =
        new InteractionResponse(
            id, "CUS-1001", "NOTE", "Original", "lab-request-001",
            Instant.parse("2026-10-20T12:00:00Z"));
    InteractionResponse updated =
        new InteractionResponse(
            id, "CUS-1001", "EMAIL", "Corrected summary", "lab-request-001",
            Instant.parse("2026-10-20T12:00:00Z"));
    when(interactionService.get(id)).thenReturn(original);
    when(interactionService.update(any(), any())).thenReturn(updated);

    mockMvc
        .perform(get("/api/v1/interactions/{id}", id).header("Authorization", BEARER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.summary").value("Original"));

    mockMvc
        .perform(
            put("/api/v1/interactions/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", BEARER)
                .content(
                    """
                    {"interactionType":"EMAIL","summary":"Corrected summary"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.interactionType").value("EMAIL"))
        .andExpect(jsonPath("$.summary").value("Corrected summary"));
    verify(interactionService)
        .update(id, new UpdateInteractionRequest("EMAIL", "Corrected summary"));

    mockMvc
        .perform(delete("/api/v1/interactions/{id}", id).header("Authorization", BEARER))
        .andExpect(status().isNoContent());
    verify(interactionService).delete(id);
  }

  @Test
  void detail_unknownInteraction_returns404ProblemDetail() throws Exception {
    UUID id = UUID.randomUUID();
    when(interactionService.get(id)).thenThrow(new InteractionNotFoundException(id));

    mockMvc
        .perform(get("/api/v1/interactions/{id}", id).header("Authorization", BEARER))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail").value("Unknown interaction: " + id));
  }

  @Test
  void update_invalidType_returns400ProblemDetail() throws Exception {
    UUID id = UUID.randomUUID();

    mockMvc
        .perform(
            put("/api/v1/interactions/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", BEARER)
                .content(
                    """
                    {"interactionType":"FAX","summary":"Bad update"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Validation failed"));

    verify(interactionService, never()).update(any(), any());
  }

  @Test
  void create_withoutBearer_returns401() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/interactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"customerId":"CUS-1001","interactionType":"NOTE","summary":"No token"}
                    """))
        .andExpect(status().isUnauthorized());

    verify(interactionService, never()).create(any(), any());
  }
}
