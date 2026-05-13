package com.credito.common.protocol.fixedlength;

/**
 * Fixed-length 필드의 남는 byte 영역을 채울 padding 문자를 정의합니다.
 *
 * <p>문자 필드와 숫자 필드가 서로 다른 padding 규칙을 사용할 수 있도록
 * space와 zero padding을 명시적으로 구분합니다.</p>
 *
 * <p>주요 책임</p>
 * <ul>
 *     <li>space padding 문자 제공</li>
 *     <li>zero padding 문자 제공</li>
 * </ul>
 */
public enum FieldPadding {
    SPACE(' '),
    ZERO('0');

    private final char value;

    FieldPadding(char value) {
        this.value = value;
    }

    char value() {
        return value;
    }
}
