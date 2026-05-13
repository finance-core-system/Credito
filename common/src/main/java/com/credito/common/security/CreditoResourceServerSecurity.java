package com.credito.common.security;

import com.credito.common.security.jwt.CreditoJwtAudienceValidator;
import com.credito.common.security.jwt.CreditoJwtAuthenticationConverter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;

import java.util.LinkedHashMap;
import java.util.Map;

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
    }

    private static void validateIssuer(CreditoResourceServerProperties.Issuer issuer) {
        if (issuer.getIssuerUri() == null || issuer.getIssuerUri().isBlank()) {
            throw new IllegalStateException("JWT issuer-uri는 비어 있을 수 없습니다.");
        }
        if (issuer.getJwkSetUri() == null || issuer.getJwkSetUri().isBlank()) {
            throw new IllegalStateException("JWT jwk-set-uri는 비어 있을 수 없습니다.");
        }
    }
}
