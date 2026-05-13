package com.credito.common.security.service;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import org.springframework.security.oauth2.jwt.Jwt;

public final class ServiceTokenValidator {

    private ServiceTokenValidator() {
    }

    public static ServiceTokenValidationResult validate(
        Jwt jwt,
        Collection<String> allowedIssuers,
        Collection<String> allowedAudiences,
        Collection<String> allowedClientIds
    ) {
        Objects.requireNonNull(jwt, "서비스 토큰은 null일 수 없습니다.");

        String issuer = jwt.getIssuer() == null ? null : jwt.getIssuer().toString();
        if (!contains(allowedIssuers, issuer)) {
            return ServiceTokenValidationResult.invalid("허용되지 않은 issuer입니다.");
        }

        boolean audienceAccepted = jwt.getAudience().stream()
            .anyMatch(audience -> contains(allowedAudiences, audience));
        if (!audienceAccepted) {
            return ServiceTokenValidationResult.invalid("허용되지 않은 audience입니다.");
        }

        String clientId = clientId(jwt);
        if (!contains(allowedClientIds, clientId)) {
            return ServiceTokenValidationResult.invalid("허용되지 않은 client_id입니다.");
        }

        return ServiceTokenValidationResult.valid(issuer, clientId, Set.copyOf(jwt.getAudience()));
    }

    private static String clientId(Jwt jwt) {
        String azp = jwt.getClaimAsString("azp");
        if (azp != null && !azp.isBlank()) {
            return azp;
        }
        return jwt.getClaimAsString("client_id");
    }

    private static boolean contains(Collection<String> values, String value) {
        return value != null && values != null && values.contains(value);
    }

    public record ServiceTokenValidationResult(
        boolean valid,
        String issuer,
        String clientId,
        Set<String> audiences,
        String reason
    ) {

        private static ServiceTokenValidationResult valid(String issuer, String clientId, Set<String> audiences) {
            return new ServiceTokenValidationResult(true, issuer, clientId, audiences, null);
        }

        private static ServiceTokenValidationResult invalid(String reason) {
            return new ServiceTokenValidationResult(false, null, null, Set.of(), reason);
        }
    }
}
