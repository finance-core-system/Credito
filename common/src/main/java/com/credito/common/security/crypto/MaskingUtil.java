package com.credito.common.security.crypto;

/**
 * 민감 정보의 로그 노출을 줄이기 위한 표시용 masking 유틸리티 클래스입니다.
 *
 * <p>이메일, 전화번호, 계좌번호처럼 로그와 감사 이벤트에 자주 등장하는 값을
 * 정해진 규칙으로 masking합니다.</p>
 *
 * <p>주요 책임</p>
 * <ul>
 *     <li>이메일 masking</li>
 *     <li>전화번호 masking</li>
 *     <li>계좌번호 masking</li>
 *     <li>뒤쪽 일부 문자만 남기는 공통 masking</li>
 * </ul>
 */
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
