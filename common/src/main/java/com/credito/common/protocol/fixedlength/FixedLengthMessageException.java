package com.credito.common.protocol.fixedlength;

/**
 * Fixed-length 전문 처리 중 스펙 위반이 발생했을 때 사용하는 예외입니다.
 *
 * <p>필드 길이 초과, 전문 전체 길이 불일치, frame 길이 불일치처럼
 * wire format 계약을 만족하지 못한 경우에 발생합니다.</p>
 *
 * <p>주요 책임</p>
 * <ul>
 *     <li>전문 인코딩 실패 표현</li>
 *     <li>전문 디코딩 실패 표현</li>
 *     <li>length-prefixed frame 검증 실패 표현</li>
 * </ul>
 */
public class FixedLengthMessageException extends RuntimeException {

    public FixedLengthMessageException(String message) {
        super(message);
    }
}
