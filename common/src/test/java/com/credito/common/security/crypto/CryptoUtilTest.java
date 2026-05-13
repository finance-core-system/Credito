package com.credito.common.security.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CryptoUtilTest {

    @Test
    void calculatesSha256Hex() {
        assertEquals(
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
            CryptoUtil.sha256Hex("hello"));
    }

    @Test
    void comparesValuesInConstantTimeApi() {
        assertTrue(CryptoUtil.constantTimeEquals("same", "same"));
        assertFalse(CryptoUtil.constantTimeEquals("same", "different"));
    }
}
