package com.credito.common.protocol.fixedlength;

import java.nio.charset.Charset;
import java.util.Objects;

/**
 * Fixed-length 전문 body를 구성하는 단일 필드의 스펙입니다.
 *
 * <p>필드 이름, byte 길이, 정렬 방식, padding 방식을 보관하고,
 * 해당 스펙에 맞춰 필드 값을 인코딩하거나 디코딩합니다.</p>
 *
 * <p>주요 책임</p>
 * <ul>
 *     <li>필드 이름과 byte 길이 정의</li>
 *     <li>문자/숫자 필드 기본 스펙 생성</li>
 *     <li>필드 값 byte 길이 검증</li>
 *     <li>필드 단위 인코딩 및 디코딩</li>
 * </ul>
 */
public record FixedLengthFieldSpec(
    String name,
    int length,
    FieldAlignment alignment,
    FieldPadding padding
) {

    public FixedLengthFieldSpec {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("필드 이름은 비어 있을 수 없습니다.");
        }
        if (length <= 0) {
            throw new IllegalArgumentException("필드 길이는 1 이상이어야 합니다.");
        }
        Objects.requireNonNull(alignment, "필드 정렬 방식은 null일 수 없습니다.");
        Objects.requireNonNull(padding, "필드 padding 방식은 null일 수 없습니다.");
    }

    public static FixedLengthFieldSpec alpha(String name, int length) {
        return new FixedLengthFieldSpec(name, length, FieldAlignment.LEFT, FieldPadding.SPACE);
    }

    public static FixedLengthFieldSpec numeric(String name, int length) {
        return new FixedLengthFieldSpec(name, length, FieldAlignment.RIGHT, FieldPadding.ZERO);
    }

    byte[] encode(String value, Charset charset) {
        String safeValue = value == null ? "" : value;
        byte[] valueBytes = safeValue.getBytes(charset);
        if (valueBytes.length > length) {
            throw new FixedLengthMessageException("필드 길이를 초과했습니다: " + name);
        }

        byte[] encoded = repeatedPaddingBytes(charset);
        int start = alignment == FieldAlignment.RIGHT ? length - valueBytes.length : 0;
        System.arraycopy(valueBytes, 0, encoded, start, valueBytes.length);
        return encoded;
    }

    String decode(byte[] message, int offset, Charset charset) {
        return new String(message, offset, length, charset);
    }

    private byte[] repeatedPaddingBytes(Charset charset) {
        byte[] paddingBytes = String.valueOf(padding.value()).getBytes(charset);
        if (paddingBytes.length != 1) {
            throw new FixedLengthMessageException("padding 문자는 1 byte로 인코딩되어야 합니다.");
        }

        byte[] bytes = new byte[length];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = paddingBytes[0];
        }
        return bytes;
    }
}
