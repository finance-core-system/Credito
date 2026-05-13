package com.credito.common.security.trace;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Trace ID 전파 필터를 Spring servlet filter로 등록하는 설정 클래스입니다.
 *
 * <p>Credito 서비스가 common 모듈을 component scan할 때 {@link TraceIdFilter}를 bean으로 등록해
 * 모든 inbound HTTP 요청에 같은 trace id 처리 규칙을 적용합니다.</p>
 *
 * <p>주요 책임</p>
 * <ul>
 *     <li>TraceIdFilter bean 등록</li>
 *     <li>서비스별 trace id 처리 방식 통일</li>
 * </ul>
 */
@Configuration
public class TraceIdConfiguration {

    @Bean
    TraceIdFilter traceIdFilter() {
        return new TraceIdFilter();
    }
}
