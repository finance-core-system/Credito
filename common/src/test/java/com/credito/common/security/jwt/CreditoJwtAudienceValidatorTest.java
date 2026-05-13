package com.credito.common.security.jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreditoJwtAudienceValidatorTest {

    @Test
    void rejectsNullConfiguredAudiences() {
        NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> new CreditoJwtAudienceValidator(null));

        assertEquals("허용 audience 목록은 null일 수 없습니다.", exception.getMessage());
    }

    @Test
    void acceptsExpectedAudience() {
        CreditoJwtAudienceValidator validator = new CreditoJwtAudienceValidator(List.of("credito-api"));

        OAuth2TokenValidatorResult result = validator.validate(jwtWithAudience(List.of("credito-api")));

        assertFalse(result.hasErrors());
    }

    @Test
    void acceptsTokenWhenOneOfMultipleAudiencesMatches() {
        CreditoJwtAudienceValidator validator = new CreditoJwtAudienceValidator(List.of("credito-api"));

        OAuth2TokenValidatorResult result = validator.validate(jwtWithAudience(List.of(
            "account-service",
            "credito-api"
        )));

        assertFalse(result.hasErrors());
    }

    @Test
    void acceptsTokenWhenOneOfMultipleConfiguredAudiencesMatches() {
        CreditoJwtAudienceValidator validator = new CreditoJwtAudienceValidator(List.of(
            "credito-api",
            "credito-admin-api"
        ));

        OAuth2TokenValidatorResult result = validator.validate(jwtWithAudience(List.of("credito-admin-api")));

        assertFalse(result.hasErrors());
    }

    @Test
    void rejectsUnexpectedAudience() {
        CreditoJwtAudienceValidator validator = new CreditoJwtAudienceValidator(List.of("credito-admin-api"));

        OAuth2TokenValidatorResult result = validator.validate(jwtWithAudience(List.of("credito-api")));

        assertTrue(result.hasErrors());
    }

    @Test
    void rejectsWhenConfiguredAudiencesAreEmpty() {
        CreditoJwtAudienceValidator validator = new CreditoJwtAudienceValidator(List.of());

        OAuth2TokenValidatorResult result = validator.validate(jwtWithAudience(List.of("credito-api")));

        assertTrue(result.hasErrors());
    }

    @Test
    void rejectsTokenWithoutAudienceClaim() {
        CreditoJwtAudienceValidator validator = new CreditoJwtAudienceValidator(List.of("credito-api"));

        OAuth2TokenValidatorResult result = validator.validate(jwtWithoutAudience());

        assertTrue(result.hasErrors());
    }

    private static Jwt jwtWithAudience(List<String> audiences) {
        return new Jwt(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(60),
            Map.of("alg", "none"),
            Map.of("aud", audiences));
    }

    private static Jwt jwtWithoutAudience() {
        return new Jwt(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(60),
            Map.of("alg", "none"),
            Map.of("sub", "customer-1"));
    }
}
