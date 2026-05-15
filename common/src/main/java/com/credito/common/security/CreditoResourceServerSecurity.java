package com.credito.common.security;

import com.credito.common.security.jwt.CreditoJwtAudienceValidator;
import com.credito.common.security.jwt.CreditoJwtAuthenticationConverter;
import com.credito.common.security.service.ServiceTokenAuthorizationFilter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Credito 서비스의 공통 resource server 보안 구성을 생성하는 유틸리티 클래스입니다.
 *
 * <p>{@link CreditoResourceServerProperties}에 바인딩된 issuer, JWK set URI, audience 설정을 기반으로
 * JWT decoder와 authentication manager resolver를 구성한 뒤 Spring Security filter chain을 생성합니다.</p>
 *
 * <p>주요 책임</p>
 * <ul>
 *     <li>public path와 인증 필요 path 구분</li>
 *     <li>issuer별 JWT decoder 구성</li>
 *     <li>issuer와 audience 검증 연결</li>
 *     <li>JWT claim을 Spring Security authority로 변환</li>
 *     <li>내부 API service-token 정책 filter 연결</li>
 * </ul>
 */
public final class CreditoResourceServerSecurity {

    private CreditoResourceServerSecurity() {
    }

    public static SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        CreditoResourceServerProperties properties
    ) throws Exception {
        validate(properties);

        return http
            .authorizeHttpRequests(authorize -> {
                properties.getPublicPaths()
                    .forEach(path -> authorize.requestMatchers(path).permitAll());
                authorize.anyRequest().authenticated();
            })
            .oauth2ResourceServer(oauth2 -> oauth2
                .authenticationManagerResolver(authenticationManagerResolver(properties)))
            .addFilterAfter(
                new ServiceTokenAuthorizationFilter(properties.getServiceToken()),
                BearerTokenAuthenticationFilter.class)
            .csrf(AbstractHttpConfigurer::disable)
            .build();
    }

    private static JwtIssuerAuthenticationManagerResolver authenticationManagerResolver(
        CreditoResourceServerProperties properties
    ) {
        Map<String, AuthenticationManager> authenticationManagers = new LinkedHashMap<>();
        properties.getIssuers().forEach(issuer -> authenticationManagers.put(
            issuer.getIssuerUri(),
            authenticationManager(jwtDecoder(issuer, properties))));

        return new JwtIssuerAuthenticationManagerResolver(issuer -> {
            AuthenticationManager authenticationManager = authenticationManagers.get(issuer);
            if (authenticationManager == null) {
                throw new InvalidBearerTokenException("신뢰할 수 없는 토큰 issuer입니다: " + issuer);
            }
            return authenticationManager;
        });
    }

    private static AuthenticationManager authenticationManager(JwtDecoder jwtDecoder) {
        JwtAuthenticationProvider authenticationProvider = new JwtAuthenticationProvider(jwtDecoder);
        authenticationProvider.setJwtAuthenticationConverter(CreditoJwtAuthenticationConverter.create());
        return authenticationProvider::authenticate;
    }

    private static NimbusJwtDecoder jwtDecoder(
        CreditoResourceServerProperties.Issuer issuer,
        CreditoResourceServerProperties properties
    ) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(issuer.getJwkSetUri()).build();
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<Jwt>(
            JwtValidators.createDefaultWithIssuer(issuer.getIssuerUri()),
            new CreditoJwtAudienceValidator(properties.getAudiences())));
        return jwtDecoder;
    }

    private static void validate(CreditoResourceServerProperties properties) {
        if (properties.getIssuers().isEmpty()) {
            throw new IllegalStateException("신뢰할 JWT issuer를 하나 이상 설정해야 합니다.");
        }
        if (properties.getAudiences().isEmpty()) {
            throw new IllegalStateException("허용할 JWT audience를 하나 이상 설정해야 합니다.");
        }
        properties.getIssuers().forEach(CreditoResourceServerSecurity::validateIssuer);
        validateServiceToken(properties.getServiceToken());
    }

    private static void validateIssuer(CreditoResourceServerProperties.Issuer issuer) {
        if (issuer.getIssuerUri() == null || issuer.getIssuerUri().isBlank()) {
            throw new IllegalStateException("JWT issuer-uri는 비어 있을 수 없습니다.");
        }
        if (issuer.getJwkSetUri() == null || issuer.getJwkSetUri().isBlank()) {
            throw new IllegalStateException("JWT jwk-set-uri는 비어 있을 수 없습니다.");
        }
    }

    private static void validateServiceToken(CreditoResourceServerProperties.ServiceToken serviceToken) {
        if (serviceToken == null || !serviceToken.isEnabled()) {
            return;
        }
        if (serviceToken.getRules() == null || serviceToken.getRules().isEmpty()) {
            throw new IllegalStateException("service-token 규칙을 하나 이상 설정하거나 enabled=false로 비활성화해야 합니다.");
        }
        if (serviceToken.getAllowedIssuers().isEmpty()) {
            throw new IllegalStateException("service-token issuer를 하나 이상 설정해야 합니다.");
        }
        if (serviceToken.getAllowedAudiences().isEmpty()) {
            throw new IllegalStateException("service-token audience를 하나 이상 설정해야 합니다.");
        }
        serviceToken.getRules().forEach(CreditoResourceServerSecurity::validateServiceTokenRule);
    }

    private static void validateServiceTokenRule(CreditoResourceServerProperties.Rule rule) {
        if (rule.getPathPattern() == null || rule.getPathPattern().isBlank()) {
            throw new IllegalStateException("service-token path-pattern은 비어 있을 수 없습니다.");
        }
        if (rule.getAllowedClientIds().isEmpty()) {
            throw new IllegalStateException("service-token 허용 client id를 하나 이상 설정해야 합니다.");
        }
        if (rule.getRequiredScopes().isEmpty()) {
            throw new IllegalStateException("service-token 필수 scope를 하나 이상 설정해야 합니다.");
        }
    }
}
