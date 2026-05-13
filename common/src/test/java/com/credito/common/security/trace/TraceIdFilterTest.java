package com.credito.common.security.trace;

import jakarta.servlet.ServletException;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TraceIdFilterTest {

    @Test
    void propagatesIncomingTraceId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> forwardedTraceId = new AtomicReference<>();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "trace-123");

        new TraceIdFilter().doFilter(request, response, (servletRequest, servletResponse) ->
            forwardedTraceId.set(((jakarta.servlet.http.HttpServletRequest) servletRequest)
                .getHeader(TraceIdFilter.TRACE_ID_HEADER)));

        assertEquals("trace-123", request.getAttribute(TraceIdFilter.REQUEST_ATTRIBUTE));
        assertEquals("trace-123", response.getHeader(TraceIdFilter.TRACE_ID_HEADER));
        assertEquals("trace-123", forwardedTraceId.get());
    }

    @Test
    void createsTraceIdWhenHeaderIsMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> forwardedTraceId = new AtomicReference<>();

        new TraceIdFilter().doFilter(request, response, (servletRequest, servletResponse) ->
            forwardedTraceId.set(((jakarta.servlet.http.HttpServletRequest) servletRequest)
                .getHeader(TraceIdFilter.TRACE_ID_HEADER)));

        assertNotNull(request.getAttribute(TraceIdFilter.REQUEST_ATTRIBUTE));
        assertNotNull(response.getHeader(TraceIdFilter.TRACE_ID_HEADER));
        assertEquals(response.getHeader(TraceIdFilter.TRACE_ID_HEADER), forwardedTraceId.get());
    }

    @Test
    void createsTraceIdWhenHeaderContainsInvalidCharacters() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "trace\r\npolluted");

        new TraceIdFilter().doFilter(request, response, new MockFilterChain());

        assertNotEquals("trace\r\npolluted", request.getAttribute(TraceIdFilter.REQUEST_ATTRIBUTE));
        assertNotEquals("trace\r\npolluted", response.getHeader(TraceIdFilter.TRACE_ID_HEADER));
        assertNotNull(request.getAttribute(TraceIdFilter.REQUEST_ATTRIBUTE));
    }

    @Test
    void restoresPreviousMdcTraceId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "trace-123");
        MDC.put(TraceIdFilter.MDC_TRACE_ID_KEY, "previous-trace");
        assumeTrue("previous-trace".equals(MDC.get(TraceIdFilter.MDC_TRACE_ID_KEY)));

        try {
            new TraceIdFilter().doFilter(request, response, (servletRequest, servletResponse) ->
                assertEquals("trace-123", MDC.get(TraceIdFilter.MDC_TRACE_ID_KEY)));

            assertEquals("previous-trace", MDC.get(TraceIdFilter.MDC_TRACE_ID_KEY));
        } finally {
            MDC.remove(TraceIdFilter.MDC_TRACE_ID_KEY);
        }
    }
}
