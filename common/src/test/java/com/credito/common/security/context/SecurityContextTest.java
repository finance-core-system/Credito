package com.credito.common.security.context;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityContextTest {

    @Test
    void checksAuthoritiesRolesAndScopes() {
        SecurityContext context = SecurityContext.of(
            "principal-1",
            "principal",
            "account-service",
            List.of("ROLE_ADMIN", "SCOPE_accounts.read"));

        assertTrue(context.hasAuthority("ROLE_ADMIN"));
        assertTrue(context.hasRole("ADMIN"));
        assertTrue(context.hasScope("accounts.read"));
        assertFalse(context.hasScope("accounts.write"));
    }

    @Test
    void anonymousContextIsNotAuthenticated() {
        SecurityContext context = SecurityContext.anonymous();

        assertFalse(context.authenticated());
        assertFalse(context.hasRole("ADMIN"));
    }

    @Test
    void createsContextWithEmptyAuthoritiesWhenAuthoritiesAreNull() {
        SecurityContext context = SecurityContext.of("principal-1", "principal", "account-service", null);

        assertTrue(context.authenticated());
        assertTrue(context.authorities().isEmpty());
    }
}
