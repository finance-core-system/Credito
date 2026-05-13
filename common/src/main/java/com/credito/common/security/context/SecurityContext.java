package com.credito.common.security.context;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;

public record SecurityContext(
    String principalId,
    String principalName,
    String serviceName,
    Set<String> authorities,
    boolean authenticated,
    Map<String, Object> attributes
) {

    public SecurityContext {
        authorities = immutableSortedSet(authorities);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public static SecurityContext anonymous() {
        return new SecurityContext(null, null, null, Set.of(), false, Map.of());
    }

    public static SecurityContext of(
        String principalId,
        String principalName,
        String serviceName,
        Collection<String> authorities
    ) {
        return new SecurityContext(principalId, principalName, serviceName, Set.copyOf(authorities), true, Map.of());
    }

    public static SecurityContext from(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return anonymous();
        }

        Set<String> authorities = authentication.getAuthorities().stream()
            .map(Object::toString)
            .collect(Collectors.toSet());

        return new SecurityContext(
            authentication.getName(),
            authentication.getName(),
            null,
            authorities,
            true,
            Map.of());
    }

    public boolean hasAuthority(String authority) {
        return authorities.contains(authority);
    }

    public boolean hasRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;

        return hasAuthority(authority);
    }

    public boolean hasScope(String scope) {
        if (scope == null || scope.isBlank()) {
            return false;
        }
        return hasAuthority("SCOPE_" + scope);
    }

    private static Set<String> immutableSortedSet(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }

        return values.stream()
            .filter(value -> value != null && !value.isBlank())
            .collect(Collectors.collectingAndThen(
                Collectors.toCollection(TreeSet::new),
                Set::copyOf));
    }
}
