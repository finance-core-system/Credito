package com.credito.common.security.trace;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Inbound HTTP 요청의 trace id를 애플리케이션 처리 흐름에 연결하는 servlet filter입니다.
 *
 * <p>요청 헤더의 trace id를 읽거나 새로 생성한 뒤 request attribute, response header,
 * SLF4J MDC에 저장해 로그와 응답에서 같은 trace id를 사용할 수 있게 합니다.</p>
 *
 * <p>주요 책임</p>
 * <ul>
 *     <li>X-Trace-Id 요청 헤더 조회</li>
 *     <li>trace id 미존재 시 UUID 생성</li>
 *     <li>request attribute와 response header에 trace id 설정</li>
 *     <li>요청 처리 동안 MDC traceId 설정 및 정리</li>
 * </ul>
 */
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
