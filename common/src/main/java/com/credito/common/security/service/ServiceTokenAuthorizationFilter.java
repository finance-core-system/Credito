package com.credito.common.security.service;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.List;

import com.credito.common.security.CreditoResourceServerProperties;
import com.credito.common.security.service.ServiceTokenValidator.ServiceTokenValidationResult;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 내부 API 요청에 대해 service-token 정책을 강제하는 Servlet filter입니다.
 *
 * <p>설정된 path rule에 해당하는 요청만 검사하며, mTLS client certificate 존재 여부와
 * service-token의 issuer, audience, client id, scope를 함께 확인합니다.</p>
 *
 * <p>주요 책임</p>
 * <ul>
 *     <li>요청 path에 매칭되는 내부 API rule 탐색</li>
 *     <li>mTLS client certificate 존재 여부 확인</li>
 *     <li>JWT 기반 service-token 세부 정책 검증</li>
 *     <li>정책 위반 요청 차단</li>
 * </ul>
 */
public class ServiceTokenAuthorizationFilter extends OncePerRequestFilter {

    private static final String X509_CERTIFICATE_ATTRIBUTE = "jakarta.servlet.request.X509Certificate";

    private final CreditoResourceServerProperties.ServiceToken properties;

    public ServiceTokenAuthorizationFilter(CreditoResourceServerProperties.ServiceToken properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        if (properties == null || !properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        CreditoResourceServerProperties.Rule rule = matchingRule(request);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "service-token이 필요합니다.");
            return;
        }

        if (properties.isRequireClientCertificate() && !hasClientCertificate(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "mTLS client certificate가 필요합니다.");
            return;
        }

        Jwt jwt = token.getToken();
        ServiceTokenValidationResult result = ServiceTokenValidator.validate(
            jwt,
            properties.getAllowedIssuers(),
            properties.getAllowedAudiences(),
            rule.getAllowedClientIds(),
            rule.getRequiredScopes());

        if (!result.valid()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, result.reason());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private CreditoResourceServerProperties.Rule matchingRule(HttpServletRequest request) {
        List<CreditoResourceServerProperties.Rule> rules = properties.getRules();
        if (rules == null || rules.isEmpty()) {
            return null;
        }
        String requestUri = request.getRequestURI();
        return rules.stream()
            .filter(rule -> matches(rule.getPathPattern(), requestUri))
            .findFirst()
            .orElse(null);
    }

    private static boolean matches(String pattern, String requestUri) {
        if (pattern == null || pattern.isBlank()) {
            return false;
        }
        if (pattern.endsWith("/**")) {
            String basePath = pattern.substring(0, pattern.length() - 3);
            return requestUri.equals(basePath) || requestUri.startsWith(basePath + "/");
        }
        return requestUri.equals(pattern);
    }

    private static boolean hasClientCertificate(HttpServletRequest request) {
        Object certificateAttribute = request.getAttribute(X509_CERTIFICATE_ATTRIBUTE);
        return certificateAttribute instanceof X509Certificate[] certificates && certificates.length > 0;
    }
}
