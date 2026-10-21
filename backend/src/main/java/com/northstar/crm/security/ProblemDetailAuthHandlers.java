package com.northstar.crm.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.northstar.crm.observability.CorrelationId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * 401 and 403 in the same RFC 9457 shape as every other API error (plus {@code correlationId}),
 * so the Angular client has one error contract. Details stay generic: the response never says
 * why a token failed beyond "invalid or expired".
 */
public class ProblemDetailAuthHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

  private static final Logger log = LoggerFactory.getLogger(ProblemDetailAuthHandlers.class);
  private static final ObjectMapper JSON = new ObjectMapper();

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
      throws IOException {
    boolean badToken = ex instanceof InvalidBearerTokenException;
    String detail =
        badToken ? "Bearer token is invalid or expired." : "A bearer token is required.";
    log.info("401 {} {} ({})", request.getMethod(), request.getRequestURI(), detail);
    response.setHeader(
        HttpHeaders.WWW_AUTHENTICATE, badToken ? "Bearer error=\"invalid_token\"" : "Bearer");
    write(response, request, HttpStatus.UNAUTHORIZED, detail);
  }

  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
      throws IOException {
    log.warn(
        "403 {} {} user={}",
        request.getMethod(),
        request.getRequestURI(),
        CurrentActor.name());
    write(
        response, request, HttpStatus.FORBIDDEN, "Your role does not allow this action.");
  }

  private static void write(
      HttpServletResponse response, HttpServletRequest request, HttpStatus status, String detail)
      throws IOException {
    Map<String, Object> problem = new LinkedHashMap<>();
    problem.put("type", "about:blank");
    problem.put("title", status.getReasonPhrase());
    problem.put("status", status.value());
    problem.put("detail", detail);
    problem.put("instance", request.getRequestURI());
    String correlationId = CorrelationId.current();
    if (correlationId != null) {
      problem.put("correlationId", correlationId);
    }
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    JSON.writeValue(response.getOutputStream(), problem);
  }
}
