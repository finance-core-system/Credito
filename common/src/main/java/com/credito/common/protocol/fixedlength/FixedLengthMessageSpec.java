package com.credito.common.protocol.fixedlength;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record FixedLengthMessageSpec(
    Charset charset,
    List<FixedLengthFieldSpec> fields
) {

    public FixedLengthMessageSpec {
        Objects.requireNonNull(charset, "전문 charset은 null일 수 없습니다.");
        Objects.requireNonNull(fields, "전문 필드 목록은 null일 수 없습니다.");
        fields = List.copyOf(fields);

        if (fields.isEmpty()) {
            throw new IllegalArgumentException("전문 필드는 하나 이상이어야 합니다.");
        }

        Set<String> uniqueNames = fields.stream()
            .map(FixedLengthFieldSpec::name)
            .collect(Collectors.toSet());

        if (uniqueNames.size() != fields.size()) {
            throw new IllegalArgumentException("전문 필드 이름은 중복될 수 없습니다.");
        }
    }

    public static FixedLengthMessageSpec utf8(List<FixedLengthFieldSpec> fields) {
        return new FixedLengthMessageSpec(StandardCharsets.UTF_8, fields);
    }

    public int totalLength() {
        return fields.stream()
            .mapToInt(FixedLengthFieldSpec::length)
            .sum();
    }
}
