package com.credito.common.security.audit;

import java.time.Instant;
import java.util.Map;

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

    public enum Outcome {
        SUCCESS,
        FAILURE,
        DENIED,
        UNKNOWN
    }
}
