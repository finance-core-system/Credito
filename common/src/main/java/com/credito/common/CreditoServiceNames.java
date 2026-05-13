package com.credito.common;

/**
 * Credito 내부 서비스 이름을 상수로 제공하는 클래스입니다.
 *
 * <p>서비스 간 토큰 검증, 감사 로그, trace 속성처럼 서비스 식별자가 필요한 곳에서
 * 문자열 오타를 줄이기 위한 공통 기준 값을 제공합니다.</p>
 *
 * <p>주요 책임</p>
 * <ul>
 *     <li>내부 서비스 이름 상수 제공</li>
 *     <li>서비스 식별자 문자열의 중복 선언 방지</li>
 * </ul>
 */
public final class CreditoServiceNames {

    public static final String GATEWAY_SERVICE = "gateway-service";
    public static final String CUSTOMER_SERVICE = "customer-service";
    public static final String ACCOUNT_SERVICE = "account-service";
    public static final String LENDING_SERVICE = "lending-service";
    public static final String BATCH_SERVICE = "batch-service";

    private CreditoServiceNames() {
    }
}
