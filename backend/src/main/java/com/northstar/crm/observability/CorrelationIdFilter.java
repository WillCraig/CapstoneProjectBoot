package com.northstar.crm.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads {@code X-Correlation-Id} (or creates one), puts it in the MDC so every log line for the
 * request carries it, and echoes it on the response so a caller can quote it in a bug report.
 *
 * <p>Runs before Spring Security so 401/403 responses and their logs are correlated too.
 * Incoming values are only trusted if they are short and log-safe (no spaces or newlines, max
 * 64 chars to match the {@code correlation_id} column); anything else is replaced.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

  private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9._:-]{1,64}");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String incoming = request.getHeader(CorrelationId.HEADER);
    String correlationId =
        incoming != null && SAFE.matcher(incoming).matches()
            ? incoming
            : UUID.randomUUID().toString();
    MDC.put(CorrelationId.MDC_KEY, correlationId);
    response.setHeader(CorrelationId.HEADER, correlationId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(CorrelationId.MDC_KEY);
    }
  }
}
