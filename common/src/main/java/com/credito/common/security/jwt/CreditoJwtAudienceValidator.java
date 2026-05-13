package com.credito.common.security.jwt;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * JWT audience claim이 허용된 audience 목록과 일치하는지 검증하는 validator입니다.
 *
 * <p>Spring Security JWT 검증 흐름에서 사용되며, 토큰의 audience 중 하나라도
 * 서비스가 허용한 audience와 일치하면 성공으로 판단합니다.</p>
 *
 * <p>주요 책임</p>
 * <ul>
 *     <li>허용 audience 목록 보관</li>
 *     <li>토큰 audience claim 존재 여부 확인</li>
 *     <li>토큰 audience와 허용 audience 매칭</li>
 *     <li>검증 실패 시 OAuth2 error 반환</li>
 * </ul>
 */
public final class CreditoJwtAudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final Set<String> audiences;

    public CreditoJwtAudienceValidator(Collection<String> audiences) {
        Objects.requireNonNull(audiences, "허용 audience 목록은 null일 수 없습니다.");
        this.audiences = Set.copyOf(new LinkedHashSet<>(audiences));
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (audiences.isEmpty()) {
            return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "필수 audience 설정이 비어 있습니다.", null));
        }

        Collection<String> tokenAudiences = token.getAudience();
        if (tokenAudiences == null || tokenAudiences.isEmpty()) {
            return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "토큰 audience가 비어 있습니다.", null));
        }

        boolean accepted = tokenAudiences.stream().anyMatch(audiences::contains);
        if (accepted) {
            return OAuth2TokenValidatorResult.success();
        }

        return OAuth2TokenValidatorResult.failure(
            new OAuth2Error("invalid_token", "허용되지 않은 토큰 audience입니다.", null));
    }
}
