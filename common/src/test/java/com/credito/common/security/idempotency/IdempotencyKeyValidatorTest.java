package com.credito.common.security.idempotency;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdempotencyKeyValidatorTest {

    @Test
    void acceptsReasonableKey() {
        assertTrue(IdempotencyKeyValidator.isValid("payment-request:20260513-0001"));
    }

    @Test
    void rejectsShortOrUnsafeKeys() {
        assertFalse(IdempotencyKeyValidator.isValid("short"));
        assertFalse(IdempotencyKeyValidator.isValid("payment request 0001"));
    }
}
