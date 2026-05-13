package com.credito.common.protocol.fixedlength;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FixedLengthMessageCodecTest {

    @Test
    void encodesFieldsByFixedByteLength() {
        FixedLengthMessageCodec codec = new FixedLengthMessageCodec(spec());

        byte[] encoded = codec.encode(Map.of(
            "messageType", "0200",
            "serviceCode", "ACCT",
            "amount", "1000",
            "memo", "OK"));

        assertEquals("0200ACCT0000001000OK        ", new String(encoded, StandardCharsets.UTF_8));
    }

    @Test
    void decodesFieldsWithoutTrimmingPadding() {
        FixedLengthMessageCodec codec = new FixedLengthMessageCodec(spec());

        Map<String, String> decoded = codec.decode("0200ACCT0000001000OK        ".getBytes(StandardCharsets.UTF_8));

        assertEquals("0200", decoded.get("messageType"));
        assertEquals("ACCT", decoded.get("serviceCode"));
        assertEquals("0000001000", decoded.get("amount"));
        assertEquals("OK        ", decoded.get("memo"));
    }

    @Test
    void rejectsValueLongerThanConfiguredByteLength() {
        FixedLengthMessageCodec codec = new FixedLengthMessageCodec(spec());

        assertThrows(FixedLengthMessageException.class, () -> codec.encode(Map.of(
            "messageType", "0200",
            "serviceCode", "ACCOUNT",
            "amount", "1000",
            "memo", "OK")));
    }

    @Test
    void rejectsMissingFieldValue() {
        FixedLengthMessageCodec codec = new FixedLengthMessageCodec(spec());

        assertThrows(FixedLengthMessageException.class, () -> codec.encode(Map.of(
            "messageType", "0200",
            "serviceCode", "ACCT",
            "amount", "1000")));
    }

    @Test
    void rejectsMessageWithUnexpectedLength() {
        FixedLengthMessageCodec codec = new FixedLengthMessageCodec(spec());

        assertThrows(FixedLengthMessageException.class, () -> codec.decode("0200".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void encodesUsingByteLengthNotCharacterCount() {
        FixedLengthMessageCodec codec = new FixedLengthMessageCodec(new FixedLengthMessageSpec(
            StandardCharsets.UTF_8,
            List.of(FixedLengthFieldSpec.alpha("name", 6))));

        byte[] encoded = codec.encode(Map.of("name", "가"));

        assertArrayEquals("가   ".getBytes(StandardCharsets.UTF_8), encoded);
    }

    private static FixedLengthMessageSpec spec() {
        return new FixedLengthMessageSpec(
            StandardCharsets.UTF_8,
            List.of(
                FixedLengthFieldSpec.numeric("messageType", 4),
                FixedLengthFieldSpec.alpha("serviceCode", 4),
                FixedLengthFieldSpec.numeric("amount", 10),
                FixedLengthFieldSpec.alpha("memo", 10)));
    }
}
