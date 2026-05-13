package com.credito.common.security.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceTokenValidatorTest {

    @Test
    void acceptsTokenWithAllowedIssuerAudienceAndClient() {
        var result = ServiceTokenValidator.validate(
            jwt("https://auth.credito.local/realms/credito", List.of("account-service"), "gateway-service"),
            List.of("https://auth.credito.local/realms/credito"),
            List.of("account-service"),
            List.of("gateway-service"));

        assertTrue(result.valid());
    }

    @Test
    void rejectsTokenWithUnexpectedClient() {
        var result = ServiceTokenValidator.validate(
            jwt("https://auth.credito.local/realms/credito", List.of("account-service"), "unknown-service"),
            List.of("https://auth.credito.local/realms/credito"),
            List.of("account-service"),
            List.of("gateway-service"));

        assertFalse(result.valid());
    }

    @Test
    void rejectsTokenWithoutAudienceClaim() {
        var result = ServiceTokenValidator.validate(
            jwtWithoutAudience("https://auth.credito.local/realms/credito", "gateway-service"),
            List.of("https://auth.credito.local/realms/credito"),
            List.of("account-service"),
            List.of("gateway-service"));

        assertFalse(result.valid());
    }

    @Test
    void rejectsTokenWithEmptyAudienceClaim() {
        var result = ServiceTokenValidator.validate(
            jwt("https://auth.credito.local/realms/credito", List.of(), "gateway-service"),
            List.of("https://auth.credito.local/realms/credito"),
            List.of("account-service"),
            List.of("gateway-service"));

        assertFalse(result.valid());
    }

    private static Jwt jwt(String issuer, List<String> audiences, String clientId) {
        return new Jwt(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(60),
            Map.of("alg", "none"),
            Map.of(
                "iss", issuer,
                "aud", audiences,
                "azp", clientId));
    }

    private static Jwt jwtWithoutAudience(String issuer, String clientId) {
        return new Jwt(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(60),
            Map.of("alg", "none"),
            Map.of(
                "iss", issuer,
                "azp", clientId));
    }
}
