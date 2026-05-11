package com.credito.common.security;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreditoJwtAuthenticationConverterTest {

    @Test
    void convertsTopLevelAndRealmRolesToAuthorities() {
        Jwt jwt = new Jwt(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(60),
            Map.of("alg", "none"),
            Map.of(
                "sub", "operator-1",
                "preferred_username", "operator",
                "roles", List.of("ROLE_OPERATOR"),
                "realm_access", Map.of("roles", List.of("ROLE_MANAGER"))));

        var authentication = CreditoJwtAuthenticationConverter.create().convert(jwt);

        assertEquals("operator", authentication.getName());
        assertTrue(authorities(authentication).contains("ROLE_OPERATOR"));
        assertTrue(authorities(authentication).contains("ROLE_MANAGER"));
    }

    private static Set<String> authorities(org.springframework.security.core.Authentication authentication) {
        return authentication.getAuthorities().stream()
            .map(Object::toString)
            .collect(Collectors.toSet());
    }
}
