package com.credito.common.security.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuditLogEventTest {

    @Test
    void createsSuccessEvent() {
        AuditLogEvent event = AuditLogEvent.success(
            "account.view",
            "customer-1",
            "account-service",
            "trace-1");

        assertEquals(AuditLogEvent.Outcome.SUCCESS, event.outcome());
        assertEquals("trace-1", event.traceId());
    }

    @Test
    void createsSuccessEventWithResource() {
        AuditLogEvent event = AuditLogEvent.success(
            "account.view",
            "customer-1",
            "account-service",
            "trace-1",
            "account",
            "account-123");

        assertEquals("account", event.resourceType());
        assertEquals("account-123", event.resourceId());
    }

    @Test
    void rejectsBlankEventType() {
        assertThrows(IllegalArgumentException.class, () -> AuditLogEvent.success(" ", "actor", "service", "trace"));
    }
}
