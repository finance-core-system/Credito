package com.credito.common.security;

import java.util.Collection;
import java.util.Objects;

import com.credito.common.security.context.SecurityContext;

public final class ServicePermissionChecker {

    private ServicePermissionChecker() {
    }

    public static boolean hasAuthority(SecurityContext context, String authority) {
        return context != null && context.hasAuthority(authority);
    }

    public static boolean hasAnyAuthority(SecurityContext context, Collection<String> authorities) {
        Objects.requireNonNull(authorities, "검사할 authority 목록은 null일 수 없습니다.");
        return context != null && authorities.stream().anyMatch(context::hasAuthority);
    }

    public static boolean hasScope(SecurityContext context, String scope) {
        return context != null && context.hasScope(scope);
    }

    public static boolean hasAnyScope(SecurityContext context, Collection<String> scopes) {
        Objects.requireNonNull(scopes, "검사할 scope 목록은 null일 수 없습니다.");
        return context != null && scopes.stream().anyMatch(context::hasScope);
    }

    public static boolean hasRole(SecurityContext context, String role) {
        return context != null && context.hasRole(role);
    }

    public static void requireAuthority(SecurityContext context, String authority) {
        if (!hasAuthority(context, authority)) {
            throw new SecurityException("필수 authority가 없습니다: " + authority);
        }
    }

    public static void requireScope(SecurityContext context, String scope) {
        if (!hasScope(context, scope)) {
            throw new SecurityException("필수 scope가 없습니다: " + scope);
        }
    }
}
