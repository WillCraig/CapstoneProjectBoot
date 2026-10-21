package com.northstar.crm.observability;

import org.slf4j.MDC;

/** Where the current request's correlation id lives (SLF4J MDC), shared by logs and code. */
public final class CorrelationId {

  public static final String HEADER = "X-Correlation-Id";
  public static final String MDC_KEY = "correlationId";

  private CorrelationId() {}

  /** @return the correlation id for the current thread, or {@code null} outside a request */
  public static String current() {
    return MDC.get(MDC_KEY);
  }
}
