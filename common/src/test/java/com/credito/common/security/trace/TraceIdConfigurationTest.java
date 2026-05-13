package com.credito.common.security.trace;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class TraceIdConfigurationTest {

    @Test
    void registersTraceIdFilterWithHighestPrecedence() {
        FilterRegistrationBean<TraceIdFilter> registration =
            new TraceIdConfiguration().traceIdFilterRegistration();

        assertInstanceOf(TraceIdFilter.class, registration.getFilter());
        assertEquals(Ordered.HIGHEST_PRECEDENCE, registration.getOrder());
        assertEquals("traceIdFilter", registration.getFilterName());
    }
}
