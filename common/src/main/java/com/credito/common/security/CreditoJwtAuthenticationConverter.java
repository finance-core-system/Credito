package com.credito.common.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

public final class CreditoJwtAuthenticationConverter {

    private CreditoJwtAuthenticationConverter() {
    }

    public static Converter<Jwt, JwtAuthenticationToken> create() {
        JwtGrantedAuthoritiesConverter scopeAuthorities = new JwtGrantedAuthoritiesConverter();

        return jwt -> {
            Set<GrantedAuthority> authorities = new LinkedHashSet<>(scopeAuthorities.convert(jwt));
            addRoleAuthorities(authorities, jwt.getClaimAsStringList("roles"));
            addRealmRoleAuthorities(authorities, jwt.getClaim("realm_access"));
            return new JwtAuthenticationToken(jwt, authorities, principalName(jwt));
        };
    }

    private static void addRealmRoleAuthorities(Set<GrantedAuthority> authorities, Object realmAccessClaim) {
        if (!(realmAccessClaim instanceof Map<?, ?> realmAccess)) {
            return;
        }

        Object roles = realmAccess.get("roles");
        if (roles instanceof Collection<?> roleCollection) {
            addRoleAuthorities(authorities, roleCollection);
        }
    }

    private static void addRoleAuthorities(Set<GrantedAuthority> authorities, Collection<?> roles) {
        if (roles == null) {
            return;
        }

        roles.stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .map(String::trim)
            .filter(role -> !role.isEmpty())
            .map(CreditoJwtAuthenticationConverter::roleAuthority)
            .map(SimpleGrantedAuthority::new)
            .forEach(authorities::add);
    }

    private static String roleAuthority(String role) {
        if (role.startsWith("ROLE_")) {
            return role;
        }
        return "ROLE_" + role;
    }

    private static String principalName(Jwt jwt) {
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        if (preferredUsername != null && !preferredUsername.isBlank()) {
            return preferredUsername;
        }
        return jwt.getSubject();
    }
}
