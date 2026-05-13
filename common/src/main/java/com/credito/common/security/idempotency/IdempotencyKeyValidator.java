package com.credito.common.security.idempotency;

import java.util.regex.Pattern;

/**
 * 중복 요청 방지에 사용하는 idempotency key의 기본 형식과 길이를 검증하는 클래스입니다.
 *
 * <p>요청 idempotency key가 비어 있지 않은지, 허용 길이 안에 있는지,
 * 허용된 문자만 사용하는지 확인합니다.</p>
 *
 * <p>주요 책임</p>
 * <ul>
 *     <li>idempotency key 존재 여부 검증</li>
 *     <li>idempotency key 길이 검증</li>
 *     <li>idempotency key 허용 문자 검증</li>
 *     <li>검증 실패 사유 반환</li>
 * </ul>
 */
public final class IdempotencyKeyValidator {

    private static final int MIN_LENGTH = 16;
    private static final int MAX_LENGTH = 128;
    private static final Pattern ALLOWED = Pattern.compile("^[A-Za-z0-9._:-]+$");

    private IdempotencyKeyValidator() {
    }

    public static boolean isValid(String key) {
        return validate(key).valid();
    }

    public static ValidationResult validate(String key) {
        if (key == null || key.isBlank()) {
            return ValidationResult.invalid("Idempotency-Key는 비어 있을 수 없습니다.");
        }
        if (key.length() < MIN_LENGTH || key.length() > MAX_LENGTH) {
            return ValidationResult.invalid("Idempotency-Key 길이는 16자 이상 128자 이하여야 합니다.");
        }
        if (!ALLOWED.matcher(key).matches()) {
            return ValidationResult.invalid("Idempotency-Key에 허용되지 않은 문자가 포함되어 있습니다.");
        }
        return ValidationResult.ok();
    }

    /**
     * Idempotency key 검증 결과를 표현하는 값 객체입니다.
     *
     * <p>검증 성공 여부와 실패 사유를 함께 반환해 호출자가 거부 이유를 응답이나 로그에 활용할 수 있게 합니다.</p>
     *
     * <p>주요 책임</p>
     * <ul>
     *     <li>검증 성공 여부 보관</li>
     *     <li>검증 실패 사유 보관</li>
     * </ul>
     */
    public record ValidationResult(boolean valid, String reason) {

        private static ValidationResult ok() {
            return new ValidationResult(true, null);
        }

        private static ValidationResult invalid(String reason) {
            return new ValidationResult(false, reason);
        }
    }
}
