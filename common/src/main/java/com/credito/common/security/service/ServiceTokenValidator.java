package com.credito.common.security.service;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * 서비스 간 호출에 사용되는 JWT의 발급자, 대상, 호출 client를 검증하는 클래스입니다.
 *
 * <p>토큰의 issuer, audience, client id가 호출 대상 서비스에서 허용한 목록에 포함되는지 확인하고,
 * 검증 결과와 실패 사유를 값 객체로 반환합니다.</p>
 *
 * <p>주요 책임</p>
 * <ul>
 *     <li>JWT issuer 허용 여부 검증</li>
 *     <li>JWT audience 허용 여부 검증</li>
 *     <li>azp 또는 client_id claim 허용 여부 검증</li>
 *     <li>서비스 토큰 검증 결과 반환</li>
 * </ul>
 */
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

        Collection<String> audiences = jwt.getAudience();
        if (audiences == null || audiences.isEmpty()) {
            return ServiceTokenValidationResult.invalid("토큰 audience가 비어 있습니다.");
        }

        boolean audienceAccepted = audiences.stream()
            .anyMatch(audience -> contains(allowedAudiences, audience));
        if (!audienceAccepted) {
            return ServiceTokenValidationResult.invalid("허용되지 않은 audience입니다.");
        }

        String clientId = clientId(jwt);
        if (!contains(allowedClientIds, clientId)) {
            return ServiceTokenValidationResult.invalid("허용되지 않은 client_id입니다.");
        }

        return ServiceTokenValidationResult.valid(issuer, clientId, Set.copyOf(audiences));
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

    /**
     * 서비스 토큰 검증 결과를 표현하는 값 객체입니다.
     *
     * <p>검증 성공 여부와 함께 검증된 issuer, client id, audience 목록 또는 실패 사유를 반환합니다.</p>
     *
     * <p>주요 책임</p>
     * <ul>
     *     <li>서비스 토큰 검증 성공 여부 보관</li>
     *     <li>검증된 issuer, client id, audience 보관</li>
     *     <li>검증 실패 사유 보관</li>
     * </ul>
     */
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
