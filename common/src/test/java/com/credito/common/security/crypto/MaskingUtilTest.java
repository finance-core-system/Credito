package com.credito.common.security.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaskingUtilTest {

    @Test
    void masksEmailAndKeepsDomain() {
        assertEquals("u****@credito.com", MaskingUtil.maskEmail("user@credito.com"));
    }

    @Test
    void masksAccountNumberKeepingLastFour() {
        assertEquals("****1234", MaskingUtil.maskAccountNumber("1000200030001234"));
    }
}
