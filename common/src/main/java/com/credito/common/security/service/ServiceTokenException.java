package com.credito.common.security.service;

/**
 * service-token 발급 또는 처리 실패를 표현하는 runtime exception입니다.
 *
 * <p>내부 token endpoint 호출 실패, 응답 파싱 실패, 요청 중단 같은 service-token 흐름의
 * 기술적 실패를 호출자에게 전달합니다.</p>
 *
 * <p>주요 책임</p>
 * <ul>
 *     <li>service-token 발급 실패 표현</li>
 *     <li>원인 예외 보존</li>
 * </ul>
 */
public class ServiceTokenException extends RuntimeException {

    public ServiceTokenException(String message) {
        super(message);
    }

    public ServiceTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
