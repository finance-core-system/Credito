package com.credito.common.protocol.fixedlength;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class FixedLengthMessageCodec {

    private final FixedLengthMessageSpec spec;

    public FixedLengthMessageCodec(FixedLengthMessageSpec spec) {
        this.spec = Objects.requireNonNull(spec, "전문 스펙은 null일 수 없습니다.");
    }

    public byte[] encode(Map<String, ?> values) {
        Objects.requireNonNull(values, "전문 값은 null일 수 없습니다.");

        ByteArrayOutputStream output = new ByteArrayOutputStream(spec.totalLength());
        for (FixedLengthFieldSpec field : spec.fields()) {
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
