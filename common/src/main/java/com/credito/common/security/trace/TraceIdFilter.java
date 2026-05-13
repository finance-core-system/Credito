package com.credito.common.security.trace;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String MDC_TRACE_ID_KEY = "traceId";
    public static final String REQUEST_ATTRIBUTE = TraceIdFilter.class.getName() + ".TRACE_ID";

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = traceId(request);
        request.setAttribute(REQUEST_ATTRIBUTE, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        MDC.put(MDC_TRACE_ID_KEY, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_ID_KEY);
        }
    }

    private static String traceId(HttpServletRequest request) {
        String header = request.getHeader(TRACE_ID_HEADER);
        if (header != null && !header.isBlank() && header.length() <= 128) {
            return header.trim();
        }
        return UUID.randomUUID().toString();
    }
}
