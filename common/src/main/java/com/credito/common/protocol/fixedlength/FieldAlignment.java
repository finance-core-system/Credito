package com.credito.common.protocol.fixedlength;

/**
 * Fixed-length 필드 값을 정해진 byte 영역 안에서 어느 방향으로 정렬할지 표현합니다.
 *
 * <p>필드 값이 스펙 길이보다 짧을 때 남는 영역을 왼쪽 또는 오른쪽에 둘지 결정합니다.</p>
 *
 * <p>주요 책임</p>
 * <ul>
 *     <li>왼쪽 정렬 필드 표현</li>
 *     <li>오른쪽 정렬 필드 표현</li>
 * </ul>
 */
public enum FieldAlignment {
    LEFT,
    RIGHT
}
