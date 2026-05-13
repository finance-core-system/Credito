package com.credito.common.security.context;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void treatsAnonymousAuthenticationAsAnonymousContext() {
        AnonymousAuthenticationToken authentication = new AnonymousAuthenticationToken(
            "key",
            "anonymousUser",
            List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

        SecurityContext context = SecurityContext.from(authentication);

        assertFalse(context.authenticated());
        assertTrue(context.authorities().isEmpty());
    }

    @Test
    void extractsAuthoritiesUsingGrantedAuthorityContract() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
            "principal",
            "credentials",
            List.of(() -> "ROLE_CUSTOMER"));
        authentication.setAuthenticated(true);

        SecurityContext context = SecurityContext.from(authentication);

        assertEquals("principal", context.principalId());
        assertTrue(context.hasAuthority("ROLE_CUSTOMER"));
    }
}
