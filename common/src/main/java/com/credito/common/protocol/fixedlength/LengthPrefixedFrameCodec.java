package com.credito.common.protocol.fixedlength;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public final class LengthPrefixedFrameCodec {

    private final int prefixLength;

    public LengthPrefixedFrameCodec(int prefixLength) {
        if (prefixLength <= 0) {
            throw new IllegalArgumentException("길이 prefix 자릿수는 1 이상이어야 합니다.");
        }
        this.prefixLength = prefixLength;
    }

    public byte[] encode(byte[] payload) {
        Objects.requireNonNull(payload, "payload는 null일 수 없습니다.");
        String length = Integer.toString(payload.length);
        if (length.length() > prefixLength) {
            throw new FixedLengthMessageException("payload 길이가 prefix 자릿수를 초과했습니다.");
        }

        byte[] prefix = String.format("%0" + prefixLength + "d", payload.length)
            .getBytes(StandardCharsets.US_ASCII);
        byte[] frame = Arrays.copyOf(prefix, prefix.length + payload.length);
        System.arraycopy(payload, 0, frame, prefix.length, payload.length);

        return frame;
    }

    public byte[] decode(byte[] frame) {
        Objects.requireNonNull(frame, "frame은 null일 수 없습니다.");
        if (frame.length < prefixLength) {
            throw new FixedLengthMessageException("frame 길이가 prefix보다 짧습니다.");
        }

        int payloadLength = parsePayloadLength(frame);
        int actualPayloadLength = frame.length - prefixLength;
        if (actualPayloadLength != payloadLength) {
            throw new FixedLengthMessageException(
                "payload 길이가 prefix와 다릅니다. expected=" + payloadLength + ", actual=" + actualPayloadLength);
        }

        return Arrays.copyOfRange(frame, prefixLength, frame.length);
    }

    private int parsePayloadLength(byte[] frame) {
        String prefix = new String(frame, 0, prefixLength, StandardCharsets.US_ASCII);
        try {
            return Integer.parseInt(prefix);
        } catch (NumberFormatException exception) {
            throw new FixedLengthMessageException("길이 prefix는 숫자여야 합니다.");
        }
    }
}
