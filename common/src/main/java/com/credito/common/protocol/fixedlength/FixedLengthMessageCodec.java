package com.credito.common.protocol.fixedlength;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Fixed-length 전문 body를 byte 배열로 인코딩하고 디코딩하는 codec입니다.
 *
 * <p>{@link FixedLengthMessageSpec}에 정의된 필드 순서와 길이에 따라 값을 이어 붙이고,
 * 수신한 byte 배열을 다시 필드별 문자열 값으로 분해합니다.</p>
 *
 * <p>주요 책임</p>
 * <ul>
 *     <li>필드 스펙 순서에 따른 전문 body 인코딩</li>
 *     <li>전체 전문 길이 검증</li>
 *     <li>전문 body를 필드별 값으로 디코딩</li>
 *     <li>padding을 제거하지 않은 원문 필드 값 반환</li>
 * </ul>
 */
public final class FixedLengthMessageCodec {

    private final FixedLengthMessageSpec spec;

    public FixedLengthMessageCodec(FixedLengthMessageSpec spec) {
        this.spec = Objects.requireNonNull(spec, "전문 스펙은 null일 수 없습니다.");
    }

    public byte[] encode(Map<String, ?> values) {
        Objects.requireNonNull(values, "전문 값은 null일 수 없습니다.");

        ByteArrayOutputStream output = new ByteArrayOutputStream(spec.totalLength());
        for (FixedLengthFieldSpec field : spec.fields()) {
            if (!values.containsKey(field.name())) {
                throw new FixedLengthMessageException("전문 필드 값이 누락되었습니다: " + field.name());
            }
            Object value = values.get(field.name());
            output.writeBytes(field.encode(value == null ? null : value.toString(), spec.charset()));
        }
        return output.toByteArray();
    }

    public Map<String, String> decode(byte[] message) {
        Objects.requireNonNull(message, "전문 bytes는 null일 수 없습니다.");
        if (message.length != spec.totalLength()) {
            throw new FixedLengthMessageException(
                "전문 길이가 스펙과 다릅니다. expected=" + spec.totalLength() + ", actual=" + message.length);
        }

        Map<String, String> values = new LinkedHashMap<>();
        int offset = 0;
        for (FixedLengthFieldSpec field : spec.fields()) {
            values.put(field.name(), field.decode(message, offset, spec.charset()));
            offset += field.length();
        }
        return values;
    }
}
