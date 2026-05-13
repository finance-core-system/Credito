package com.credito.common.security.trace;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Trace ID 전파 필터를 Spring servlet filter로 등록하는 설정 클래스입니다.
 *
 * <p>Credito 서비스가 common 모듈을 component scan할 때 {@link TraceIdFilter}를 servlet filter로 등록하고,
 * 보안 및 로깅 필터보다 먼저 실행되도록 가장 높은 우선순위를 부여합니다.</p>
 *
 * <p>주요 책임</p>
 * <ul>
 *     <li>TraceIdFilter servlet filter 등록</li>
 *     <li>필터 실행 순서를 최우선으로 지정</li>
 *     <li>서비스별 trace id 처리 방식 통일</li>
 * </ul>
 */
@Configuration
public class TraceIdConfiguration {

    @Bean
    FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration() {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>(new TraceIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setName("traceIdFilter");
        registration.addUrlPatterns("/*");

        return registration;
    }
}
