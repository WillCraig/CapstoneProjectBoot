package com.northstar.crm.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

  private final CorrelationIdFilter filter = new CorrelationIdFilter();

  @Test
  void incomingId_isPutInMdcDuringRequest_echoed_andClearedAfter() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/customers");
    request.addHeader(CorrelationId.HEADER, "lab-request-001");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> seenInChain = new AtomicReference<>();

    filter.doFilter(request, response, (req, res) -> seenInChain.set(CorrelationId.current()));

    assertEquals("lab-request-001", seenInChain.get());
    assertEquals("lab-request-001", response.getHeader(CorrelationId.HEADER));
    assertNull(MDC.get(CorrelationId.MDC_KEY), "MDC must not leak to the next request on this thread");
  }

  @Test
  void missingId_isGenerated() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/customers"), response, (req, res) -> {});

    String generated = response.getHeader(CorrelationId.HEADER);
    assertNotNull(generated);
    UUID.fromString(generated);
  }

  @Test
  void unsafeId_isReplaced_notLogged() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/customers");
    request.addHeader(CorrelationId.HEADER, "abc\nFAKE LOG LINE");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (req, res) -> {});

    String used = response.getHeader(CorrelationId.HEADER);
    assertNotEquals("abc\nFAKE LOG LINE", used);
    UUID.fromString(used);
  }
}
