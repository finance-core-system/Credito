package com.credito.common.security.idempotency;

import java.util.regex.Pattern;

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

    public record ValidationResult(boolean valid, String reason) {

        private static ValidationResult ok() {
            return new ValidationResult(true, null);
        }

        private static ValidationResult invalid(String reason) {
            return new ValidationResult(false, reason);
        }
    }
}
