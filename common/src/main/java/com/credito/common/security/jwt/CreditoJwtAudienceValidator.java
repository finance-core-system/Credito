package com.credito.common.security.jwt;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

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
