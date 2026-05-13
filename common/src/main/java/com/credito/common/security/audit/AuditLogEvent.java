package com.credito.common.security.audit;

import java.time.Instant;
import java.util.Map;

/**
 * 보안상 추적해야 하는 행위를 서비스 공통 감사 이벤트로 표현하는 값 객체입니다.
 *
 * <p>이벤트 종류, 행위자, 서비스, trace id, 대상 리소스, 처리 결과, 추가 속성을
 * 하나의 불변 record로 묶습니다.</p>
 *
 * <p>주요 책임</p>
 * <ul>
 *     <li>감사 이벤트 공통 필드 정의</li>
 *     <li>eventType 필수 검증</li>
 *     <li>outcome, occurredAt, attributes 기본값 보정</li>
 *     <li>성공 이벤트 생성 factory 제공</li>
 * </ul>
 */
public record AuditLogEvent(
    String eventType,
    String actorId,
    String serviceName,
    String traceId,
    String resourceType,
    String resourceId,
    Outcome outcome,
    Instant occurredAt,
    Map<String, Object> attributes
) {

    public AuditLogEvent {
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("감사 로그 eventType은 비어 있을 수 없습니다.");
        }
        outcome = outcome == null ? Outcome.UNKNOWN : outcome;
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public static AuditLogEvent success(String eventType, String actorId, String serviceName, String traceId) {
        return success(eventType, actorId, serviceName, traceId, null, null);
    }

    public static AuditLogEvent success(
        String eventType,
        String actorId,
        String serviceName,
        String traceId,
        String resourceType,
        String resourceId
    ) {
        return new AuditLogEvent(
            eventType,
            actorId,
            serviceName,
            traceId,
            resourceType,
            resourceId,
            Outcome.SUCCESS,
            Instant.now(),
            Map.of());
    }

    /**
     * 감사 대상 행위가 어떤 결과로 끝났는지 표현합니다.
     *
     * <p>성공, 실패, 거부, 알 수 없음 상태를 감사 이벤트에 기록하기 위한 값입니다.</p>
     *
     * <p>주요 책임</p>
     * <ul>
     *     <li>감사 이벤트 처리 결과 표현</li>
     *     <li>null outcome의 기본값 제공</li>
     * </ul>
     */
    public enum Outcome {
        SUCCESS,
        FAILURE,
        DENIED,
        UNKNOWN
    }
}
