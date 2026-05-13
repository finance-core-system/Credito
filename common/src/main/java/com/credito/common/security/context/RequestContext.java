package com.credito.common.security.context;

import java.time.Instant;
import java.util.Map;

public record RequestContext(
    String traceId,
    String idempotencyKey,
    String sourceIp,
    String userAgent,
    Instant requestedAt,
    Map<String, String> attributes
) {

    public RequestContext {
        requestedAt = requestedAt == null ? Instant.now() : requestedAt;
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public static RequestContext of(String traceId, String sourceIp, String userAgent) {
        return new RequestContext(traceId, null, sourceIp, userAgent, Instant.now(), Map.of());
    }

    public RequestContext withIdempotencyKey(String idempotencyKey) {
        return new RequestContext(traceId, idempotencyKey, sourceIp, userAgent, requestedAt, attributes);
    }
}
