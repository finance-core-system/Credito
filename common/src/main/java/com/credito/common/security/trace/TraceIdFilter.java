package com.credito.common.security.trace;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Inbound HTTP 요청의 trace id를 애플리케이션 처리 흐름에 연결하는 servlet filter입니다.
 *
 * <p>요청 헤더의 trace id를 읽거나 새로 생성한 뒤 request header, request attribute,
 * response header, SLF4J MDC에 저장해 로그와 downstream 요청에서 같은 trace id를 사용할 수 있게 합니다.</p>
 *
 * <p>주요 책임</p>
 * <ul>
 *     <li>X-Trace-Id 요청 헤더 조회</li>
 *     <li>trace id 미존재 시 UUID 생성</li>
 *     <li>request header, request attribute, response header에 trace id 설정</li>
 *     <li>요청 처리 동안 MDC traceId 설정 및 정리</li>
 * </ul>
 */
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String MDC_TRACE_ID_KEY = "traceId";
    public static final String REQUEST_ATTRIBUTE = TraceIdFilter.class.getName() + ".TRACE_ID";
    private static final int MAX_TRACE_ID_LENGTH = 128;
    private static final Pattern ALLOWED_TRACE_ID = Pattern.compile("^[A-Za-z0-9._-]+$");

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = traceId(request);
        request.setAttribute(REQUEST_ATTRIBUTE, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        String previousTraceId = MDC.get(MDC_TRACE_ID_KEY);
        MDC.put(MDC_TRACE_ID_KEY, traceId);
        try {
            filterChain.doFilter(new TraceIdRequestWrapper(request, traceId), response);
        } finally {
            if (previousTraceId == null) {
                MDC.remove(MDC_TRACE_ID_KEY);
            } else {
                MDC.put(MDC_TRACE_ID_KEY, previousTraceId);
            }
        }
    }

    private static String traceId(HttpServletRequest request) {
        String header = request.getHeader(TRACE_ID_HEADER);
        if (header == null) {
            return UUID.randomUUID().toString();
        }

        String traceId = header.trim();
        if (!traceId.isBlank() && traceId.length() <= MAX_TRACE_ID_LENGTH && ALLOWED_TRACE_ID.matcher(traceId).matches()) {
            return traceId;
        }
        return UUID.randomUUID().toString();
    }

    private static final class TraceIdRequestWrapper extends HttpServletRequestWrapper {

        private final String traceId;

        private TraceIdRequestWrapper(HttpServletRequest request, String traceId) {
            super(request);
            this.traceId = traceId;
        }

        @Override
        public String getHeader(String name) {
            if (TRACE_ID_HEADER.equalsIgnoreCase(name)) {
                return traceId;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (TRACE_ID_HEADER.equalsIgnoreCase(name)) {
                return Collections.enumeration(List.of(traceId));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Enumeration<String> originalHeaderNames = super.getHeaderNames();
            if (originalHeaderNames == null) {
                return Collections.enumeration(List.of(TRACE_ID_HEADER));
            }

            List<String> headerNames = Collections.list(originalHeaderNames);
            boolean hasTraceIdHeader = headerNames.stream()
                .anyMatch(name -> TRACE_ID_HEADER.equalsIgnoreCase(name));
            if (!hasTraceIdHeader) {
                headerNames.add(TRACE_ID_HEADER);
            }
            return Collections.enumeration(headerNames);
        }
    }
}
