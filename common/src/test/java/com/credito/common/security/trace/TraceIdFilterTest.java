package com.credito.common.security.trace;

import jakarta.servlet.ServletException;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TraceIdFilterTest {

    @Test
    void propagatesIncomingTraceId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "trace-123");

        new TraceIdFilter().doFilter(request, response, new MockFilterChain());

        assertEquals("trace-123", request.getAttribute(TraceIdFilter.REQUEST_ATTRIBUTE));
        assertEquals("trace-123", response.getHeader(TraceIdFilter.TRACE_ID_HEADER));
    }

    @Test
    void createsTraceIdWhenHeaderIsMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        new TraceIdFilter().doFilter(request, response, new MockFilterChain());

        assertNotNull(request.getAttribute(TraceIdFilter.REQUEST_ATTRIBUTE));
        assertNotNull(response.getHeader(TraceIdFilter.TRACE_ID_HEADER));
    }
}
