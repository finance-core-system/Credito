package com.credito.common.security.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.credito.common.security.CreditoResourceServerProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServiceTokenAuthorizationFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsRequestWithMatchingClientAndScope() throws Exception {
        ServiceTokenAuthorizationFilter filter = new ServiceTokenAuthorizationFilter(properties(false));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt("gateway-service", "accounts.read")));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/accounts");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CountingFilterChain chain = new CountingFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(1, chain.count);
        assertEquals(200, response.getStatus());
    }

    @Test
    void rejectsRequestWithoutRequiredScope() throws Exception {
        ServiceTokenAuthorizationFilter filter = new ServiceTokenAuthorizationFilter(properties(false));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt("gateway-service", "customers.read")));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/accounts/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new CountingFilterChain());

        assertEquals(403, response.getStatus());
    }

    @Test
    void skipsRequestOutsideConfiguredRules() throws Exception {
        ServiceTokenAuthorizationFilter filter = new ServiceTokenAuthorizationFilter(properties(true));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CountingFilterChain chain = new CountingFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(1, chain.count);
        assertEquals(200, response.getStatus());
    }

    private static CreditoResourceServerProperties.ServiceToken properties(boolean requireClientCertificate) {
        CreditoResourceServerProperties.Rule rule = new CreditoResourceServerProperties.Rule();
        rule.setPathPattern("/api/accounts/**");
        rule.setAllowedClientIds(Set.of("gateway-service"));
        rule.setRequiredScopes(Set.of("accounts.read"));

        CreditoResourceServerProperties.ServiceToken properties = new CreditoResourceServerProperties.ServiceToken();
        properties.setEnabled(true);
        properties.setRequireClientCertificate(requireClientCertificate);
        properties.setAllowedIssuers(Set.of("https://auth.credito.local/realms/system-realm"));
        properties.setAllowedAudiences(Set.of("credito-internal-api"));
        properties.setRules(List.of(rule));
        return properties;
    }

    private static Jwt jwt(String clientId, String scope) {
        Instant now = Instant.now();
        return new Jwt(
            "token",
            now,
            now.plusSeconds(60),
            Map.of("alg", "none"),
            Map.of(
                "iss", "https://auth.credito.local/realms/system-realm",
                "aud", List.of("credito-internal-api"),
                "azp", clientId,
                "scope", scope));
    }

    private static class CountingFilterChain implements FilterChain {

        private int count;

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
            count++;
        }
    }
}
