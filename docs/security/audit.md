# Audit Logging

## Background

Credito 서비스는 인증, 인가, 고객 정보 접근, 계좌 정보 접근, 서비스 간 요청 같은 행위를 추적해야 한다. 일반 애플리케이션 로그만으로는 누가, 어느 서비스에서, 어떤 리소스에 대해, 어떤 결과를 만들었는지 일관되게 표현하기 어렵다.

그래서 common 모듈에 감사 로그 이벤트의 공통 데이터 구조를 두었다.

현재 구현 위치:

- `com.credito.common.security.audit.AuditLogEvent`

현재 구현은 감사 로그 저장소, publisher, appender를 포함하지 않는다. 이벤트를 표현하는 record만 제공한다.

## Alternatives

선택지는 세 가지였다.

1. 각 서비스가 필요한 필드를 직접 로그 문자열로 남긴다.

   구현은 빠르지만 서비스마다 필드 이름과 누락 기준이 달라진다. 나중에 검색하거나 집계하기도 어렵다.

2. 감사 로그 전용 저장소와 publisher까지 common에서 바로 제공한다.

   기능은 완성도 있지만 저장소, 전송 방식, 장애 처리 정책을 먼저 확정해야 한다. 현재 단계에서는 과하다.

3. common에는 감사 이벤트 모델만 둔다.

   이벤트의 필드와 기본 보정 규칙만 통일한다. 저장과 전송은 각 서비스 또는 별도 로깅 구성에서 붙인다.

현재 구현은 세 번째 선택지다.

## Decision

`AuditLogEvent`는 Java record로 구현했다.

이렇게 한 이유는 다음과 같다.

- 감사 이벤트는 값 객체에 가깝다.
- 생성 후 필드가 바뀌지 않는 구조가 맞다.
- 서비스가 동일한 필드 이름으로 이벤트를 만들 수 있다.
- null 기본값 보정은 canonical constructor에서 처리할 수 있다.

## Implementation

### Fields

```java
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
)
```

필드 의미:

- `eventType`: 이벤트 종류
- `actorId`: 행위 주체
- `serviceName`: 이벤트를 만든 서비스
- `traceId`: 요청 추적 ID
- `resourceType`: 대상 리소스 타입
- `resourceId`: 대상 리소스 식별자
- `outcome`: 처리 결과
- `occurredAt`: 이벤트 발생 시각
- `attributes`: 추가 속성

### Constructor Rules

생성 시 적용되는 규칙:

- `eventType`이 null이거나 blank이면 `IllegalArgumentException`을 던진다.
- `outcome`이 null이면 `Outcome.UNKNOWN`으로 바꾼다.
- `occurredAt`이 null이면 `Instant.now()`로 바꾼다.
- `attributes`가 null이면 `Map.of()`로 바꾼다.
- `attributes`가 있으면 `Map.copyOf(attributes)`로 복사한다.

결과:

- event type 없는 감사 이벤트는 만들 수 없다.
- outcome과 occurredAt은 null로 남지 않는다.
- attributes는 record 생성 후 외부 map 변경에 영향을 받지 않는다.

### Outcome

```java
public enum Outcome {
    SUCCESS,
    FAILURE,
    DENIED,
    UNKNOWN
}
```

현재 outcome은 네 가지 값만 제공한다.

### Factory Method

```java
AuditLogEvent event = AuditLogEvent.success(
    "ACCOUNT_VIEWED",
    actorId,
    "account-service",
    traceId,
    "account",
    accountId);
```

`success` factory는 `Outcome.SUCCESS`, 현재 시각, 빈 attributes로 이벤트를 만든다.

오버로드:

- `success(eventType, actorId, serviceName, traceId)`
- `success(eventType, actorId, serviceName, traceId, resourceType, resourceId)`

## Consequences

좋아진 점:

- 감사 이벤트 필드가 common 타입으로 고정됐다.
- event type 누락을 생성 시점에 막는다.
- outcome, occurredAt, attributes의 null 처리가 통일됐다.
- 성공 이벤트를 짧게 만들 수 있는 factory가 생겼다.

남아 있는 점:

- 이벤트 저장소는 구현하지 않았다.
- 이벤트 전송 publisher는 구현하지 않았다.
- event type enum은 없다. 현재는 문자열이다.
- 민감 필드 masking을 자동 적용하지 않는다.
- trace id를 자동으로 가져오지 않는다.
- 실패, 거부 이벤트용 factory는 아직 없다.

현재 구현의 범위는 “감사 로그 이벤트를 일관된 값 객체로 표현하는 것”까지다.
