package com.credito.common.security.context;

import java.time.Instant;
import java.util.Map;

/**
 * 요청 처리에 필요한 추적 및 주체 정보를 담는 불변 컨텍스트입니다.
 *
 * <p>trace id, actor id, client id, 요청 시각, 추가 속성을 하나로 묶어
 * 서비스 내부 흐름에서 동일한 요청 정보를 전달할 수 있게 합니다.</p>
 *
 * <p>주요 책임</p>
 * <ul>
 *     <li>요청 trace id 보관</li>
 *     <li>요청 주체와 client 식별자 보관</li>
 *     <li>요청 생성 시각 보관</li>
 *     <li>요청 부가 속성 보관</li>
 * </ul>
 */
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
