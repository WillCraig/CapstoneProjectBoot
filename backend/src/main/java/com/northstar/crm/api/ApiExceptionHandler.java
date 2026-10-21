package com.northstar.crm.api;

import com.northstar.crm.observability.CorrelationId;
import com.northstar.crm.service.InteractionNotFoundException;
import com.northstar.crm.service.UnknownCustomerException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps failures to RFC 9457 Problem Details ({@code application/problem+json}) instead of
 * stack traces or ad-hoc error shapes, so Lab 50's Angular client sees one stable error
 * contract: {@code type, title, status, detail, instance}, plus {@code correlationId} so a
 * support ticket can be matched to the server log line.
 *
 * <p>Extends {@link ResponseEntityExceptionHandler} so framework MVC errors (validation,
 * missing parameters, unreadable bodies) render in the same format, with validation keeping
 * our {@code "Validation failed"} detail plus a per-field {@code errors} list.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(UnknownCustomerException.class)
  public ResponseEntity<ProblemDetail> unknownCustomer(
      UnknownCustomerException ex, HttpServletRequest request) {
    return notFound(ex.getMessage(), request);
  }

  @ExceptionHandler(InteractionNotFoundException.class)
  public ResponseEntity<ProblemDetail> unknownInteraction(
      InteractionNotFoundException ex, HttpServletRequest request) {
    return notFound(ex.getMessage(), request);
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ProblemDetail> invalidCredentials(
      InvalidCredentialsException ex, HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    problem.setTitle("Unauthorized");
    problem.setInstance(URI.create(request.getRequestURI()));
    return problemResponse(problem);
  }

  private static ResponseEntity<ProblemDetail> notFound(String detail, HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, detail);
    problem.setTitle("Not Found");
    problem.setInstance(URI.create(request.getRequestURI()));
    return problemResponse(problem);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<String> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .toList();
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
    problem.setTitle("Bad Request");
    if (request instanceof ServletWebRequest servletRequest) {
      problem.setInstance(URI.create(servletRequest.getRequest().getRequestURI()));
    }
    problem.setProperty("errors", errors);
    withCorrelation(problem);
    return ResponseEntity.status(problem.getStatus())
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  /** Framework MVC errors (missing param, unreadable body, ...) also carry the correlation id. */
  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
    if (body instanceof ProblemDetail problem) {
      withCorrelation(problem);
    }
    return super.handleExceptionInternal(ex, body, headers, statusCode, request);
  }

  static ProblemDetail withCorrelation(ProblemDetail problem) {
    String correlationId = CorrelationId.current();
    if (correlationId != null) {
      problem.setProperty("correlationId", correlationId);
    }
    return problem;
  }

  private static ResponseEntity<ProblemDetail> problemResponse(ProblemDetail problem) {
    withCorrelation(problem);
    return ResponseEntity.status(problem.getStatus())
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }
}
