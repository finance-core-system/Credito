package com.credito.common.security.crypto;

public final class MaskingUtil {

    private static final String MASK = "****";

    private MaskingUtil() {
    }

    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }

        int at = email.indexOf('@');
        if (at <= 1) {
            return MASK;
        }

        return email.charAt(0) + MASK + email.substring(at);
    }

    public static String maskPhone(String phone) {
        return keepLast(phone, 4);
    }

    public static String maskAccountNumber(String accountNumber) {
        return keepLast(accountNumber, 4);
    }

    public static String keepLast(String value, int visibleLength) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (visibleLength <= 0 || value.length() <= visibleLength) {
            return MASK;
        }
        return MASK + value.substring(value.length() - visibleLength);
    }
}
