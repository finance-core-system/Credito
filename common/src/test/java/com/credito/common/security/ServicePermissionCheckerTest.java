package com.credito.common.security;

import java.util.List;

import com.credito.common.security.context.SecurityContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServicePermissionCheckerTest {

    @Test
    void checksAnyScope() {
        SecurityContext context = SecurityContext.of(
            "principal-1",
            "principal",
            "customer-service",
            List.of("SCOPE_customers.read"));

        assertTrue(ServicePermissionChecker.hasAnyScope(context, List.of("accounts.read", "customers.read")));
    }

    @Test
    void throwsWhenRequiredAuthorityIsMissing() {
        SecurityContext context = SecurityContext.of(
            "principal-1",
            "principal",
            "customer-service",
            List.of("SCOPE_customers.read"));

        assertThrows(SecurityException.class, () -> ServicePermissionChecker.requireAuthority(context, "ROLE_ADMIN"));
    }
}
