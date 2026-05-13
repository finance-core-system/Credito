package com.credito.common.security.context;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * 인증된 주체의 보안 정보를 서비스 로직에서 쓰기 쉽게 담는 불변 컨텍스트입니다.
 *
 * <p>Spring Security {@link Authentication}에서 principal, subject, service, authority 정보를 추출하고,
 * role과 scope 조회를 위한 helper 메서드를 제공합니다.</p>
 *
 * <p>주요 책임</p>
 * <ul>
 *     <li>인증 주체 식별자 보관</li>
 *     <li>서비스 식별자 보관</li>
 *     <li>authority 집합 정규화</li>
 *     <li>role, scope, authority 보유 여부 확인</li>
 * </ul>
 */
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
        return new SecurityContext(principalId, principalName, serviceName, immutableSortedSet(authorities), true, Map.of());
    }

    public static SecurityContext from(Authentication authentication) {
        if (authentication == null
            || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {
            return anonymous();
        }

        Set<String> authorities = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
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
