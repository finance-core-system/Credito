package com.credito.common.protocol.fixedlength;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LengthPrefixedFrameCodecTest {

    @Test
    void encodesFrameWithAsciiLengthPrefix() {
        LengthPrefixedFrameCodec codec = new LengthPrefixedFrameCodec(4);

        byte[] frame = codec.encode("PING".getBytes(StandardCharsets.UTF_8));

        assertEquals("0004PING", new String(frame, StandardCharsets.UTF_8));
    }

    @Test
    void decodesFramePayload() {
        LengthPrefixedFrameCodec codec = new LengthPrefixedFrameCodec(4);

        byte[] payload = codec.decode("0004PING".getBytes(StandardCharsets.UTF_8));

        assertArrayEquals("PING".getBytes(StandardCharsets.UTF_8), payload);
    }

    @Test
    void rejectsFrameWhenPrefixLengthDoesNotMatchPayload() {
        LengthPrefixedFrameCodec codec = new LengthPrefixedFrameCodec(4);

        assertThrows(FixedLengthMessageException.class, () -> codec.decode("0005PING".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void rejectsNonNumericLengthPrefix() {
        LengthPrefixedFrameCodec codec = new LengthPrefixedFrameCodec(4);

        FixedLengthMessageException exception = assertThrows(
            FixedLengthMessageException.class,
            () -> codec.decode("00A4PING".getBytes(StandardCharsets.UTF_8)));

        assertInstanceOf(NumberFormatException.class, exception.getCause());
    }
}
