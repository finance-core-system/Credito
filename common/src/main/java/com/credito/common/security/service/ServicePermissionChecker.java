package com.credito.common.security.service;

import java.util.Collection;
import java.util.Objects;

import com.credito.common.security.context.SecurityContext;

/**
 * 서비스 로직에서 인증 주체의 권한 보유 여부를 확인하는 helper 클래스입니다.
 *
 * <p>{@link SecurityContext}에 담긴 authority, scope, role 정보를 조회하고,
 * 필수 권한이 없을 때 예외를 던지는 메서드를 제공합니다.</p>
 *
 * <p>주요 책임</p>
 * <ul>
 *     <li>단일 authority 보유 여부 확인</li>
 *     <li>복수 authority 중 하나 이상 보유 여부 확인</li>
 *     <li>scope와 role 보유 여부 확인</li>
 *     <li>필수 authority 또는 scope 누락 시 예외 발생</li>
 * </ul>
 */
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
