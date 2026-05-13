# Fixed-Length Message Protocol

## Background

Credito 서비스 간 통신은 기본적으로 HTTP JSON을 사용할 수 있다. JSON은 개발과 디버깅이 쉽지만, 금융권 시스템에서는 고정 길이 전문 방식도 자주 쓰인다.

고정 길이 전문은 필드 순서와 byte 길이를 사전에 합의하고, 값을 정해진 길이에 맞춰 padding해서 전송하는 방식이다.

이 문서에서 다루는 현재 구현은 다음 형태를 기준으로 한다.

```text
[length prefix][fixed-length message body]
```

- `length prefix`: body의 byte 길이를 ASCII 숫자로 표현한다.
- `message body`: 고정 길이 필드들을 순서대로 이어 붙인 bytes다.
- 필드 길이는 문자 수가 아니라 charset 기준 byte 수다.

현재 구현 위치:

- `com.credito.common.protocol.fixedlength`

## Alternatives

선택지는 네 가지였다.

1. JSON을 계속 사용한다.

   개발 생산성은 높지만, field ordering, byte length, legacy 금융 시스템과의 형태를 맞추기 어렵다.

2. ISO 8583 전체를 구현한다.

   금융 메시지 표준에 가깝지만 MTI, bitmap, data element, variable length field까지 포함해야 한다. 현재 서비스 간 내부 통신에는 범위가 크다.

3. 외부 ISO 8583 라이브러리를 도입한다.

   빠르게 표준 메시지 처리를 붙일 수 있지만, 우리 내부 계약이 ISO 8583 전체를 요구하지 않으면 불필요한 개념이 많이 들어온다.

4. common에 작은 fixed-length codec을 둔다.

   byte 길이, padding, field order, length prefix만 공통화한다. 전문별 업무 필드와 검증은 각 서비스가 정의한다.

현재 구현은 네 번째 선택지다.

## Decision

`common.protocol.fixedlength` 패키지에 fixed-length body codec과 length-prefixed frame codec을 두었다.

이렇게 한 이유는 다음과 같다.

- 현재 필요한 것은 ISO 8583 호환이 아니라 내부 wire format 고정이다.
- 전문별 필드 정의는 서비스마다 다를 수 있다.
- codec은 업무 의미를 몰라도 된다.
- 고정 길이 처리에서 가장 중요한 byte length, padding, frame length 검증을 common에서 통일할 수 있다.
- 보안 기능이 아니라 wire protocol이므로 `security`가 아니라 `protocol` 아래에 둔다.

## Implementation

### Package

```text
com.credito.common.protocol.fixedlength
```

포함 클래스:

- `FieldAlignment`
- `FieldPadding`
- `FixedLengthFieldSpec`
- `FixedLengthMessageSpec`
- `FixedLengthMessageCodec`
- `FixedLengthMessageException`
- `LengthPrefixedFrameCodec`

### FieldAlignment

```java
public enum FieldAlignment {
    LEFT,
    RIGHT
}
```

동작:

- `LEFT`: 값을 왼쪽에 두고 오른쪽을 padding한다.
- `RIGHT`: 값을 오른쪽에 두고 왼쪽을 padding한다.

### FieldPadding

```java
public enum FieldPadding {
    SPACE(' '),
    ZERO('0')
}
```

동작:

- `SPACE`: 빈 영역을 space 문자로 채운다.
- `ZERO`: 빈 영역을 `0` 문자로 채운다.

padding 문자는 지정 charset에서 1 byte로 인코딩되어야 한다. 그렇지 않으면 encoding 중 `FixedLengthMessageException`을 던진다.

### FixedLengthFieldSpec

```java
public record FixedLengthFieldSpec(
    String name,
    int length,
    FieldAlignment alignment,
    FieldPadding padding
)
```

생성 규칙:

- name은 null이거나 blank일 수 없다.
- length는 1 이상이어야 한다.
- alignment는 null일 수 없다.
- padding은 null일 수 없다.

편의 factory:

```java
FixedLengthFieldSpec.alpha("memo", 10)
FixedLengthFieldSpec.numeric("amount", 10)
```

동작:

- `alpha`: `LEFT + SPACE`
- `numeric`: `RIGHT + ZERO`

encoding 동작:

- null 값은 빈 문자열로 처리한다.
- 값을 charset 기준 bytes로 변환한다.
- value bytes가 필드 length보다 크면 `FixedLengthMessageException`을 던진다.
- 정렬 방향에 맞춰 padding bytes와 value bytes를 합친다.

decoding 동작:

- message bytes에서 offset과 length만큼 잘라 문자열로 변환한다.
- padding 제거는 하지 않는다.

### FixedLengthMessageSpec

```java
public record FixedLengthMessageSpec(
    Charset charset,
    List<FixedLengthFieldSpec> fields
)
```

생성 규칙:

- charset은 null일 수 없다.
- fields는 null일 수 없다.
- fields는 비어 있을 수 없다.
- field name은 중복될 수 없다.
- fields는 `List.copyOf`로 복사한다.

동작:

- `totalLength()`는 모든 field length 합계를 반환한다.
- `utf8(fields)`는 UTF-8 charset을 사용하는 spec을 만든다.

### FixedLengthMessageCodec

```java
FixedLengthMessageCodec codec = new FixedLengthMessageCodec(spec);
```

encode:

```java
byte[] body = codec.encode(Map.of(
    "messageType", "0200",
    "serviceCode", "ACCT",
    "amount", "1000",
    "memo", "OK"));
```

동작:

- spec의 field 순서대로 값을 읽는다.
- map에 없는 필드는 누락으로 보고 `FixedLengthMessageException`을 던진다.
- 명시적으로 null 값이 들어온 필드는 빈 값으로 padding된다.
- 각 field spec이 값을 bytes로 encode한다.
- 결과 bytes를 순서대로 이어 붙인다.

decode:

```java
Map<String, String> values = codec.decode(body);
```

동작:

- 입력 message 길이가 spec total length와 다르면 `FixedLengthMessageException`을 던진다.
- spec의 field 순서대로 bytes를 잘라 문자열로 변환한다.
- 결과는 `LinkedHashMap`에 담긴다.
- padding은 제거하지 않는다.

예시:

```java
FixedLengthMessageSpec spec = new FixedLengthMessageSpec(
    StandardCharsets.UTF_8,
    List.of(
        FixedLengthFieldSpec.numeric("messageType", 4),
        FixedLengthFieldSpec.alpha("serviceCode", 4),
        FixedLengthFieldSpec.numeric("amount", 10),
        FixedLengthFieldSpec.alpha("memo", 10)));
```

위 spec에 다음 값을 넣으면:

```java
Map.of(
    "messageType", "0200",
    "serviceCode", "ACCT",
    "amount", "1000",
    "memo", "OK")
```

body 문자열은 다음 형태가 된다.

```text
0200ACCT0000001000OK
```

마지막 `memo` 필드는 10 bytes라서 `OK` 뒤에 space padding 8 bytes가 붙는다.

### LengthPrefixedFrameCodec

```java
LengthPrefixedFrameCodec frameCodec = new LengthPrefixedFrameCodec(4);
```

생성 규칙:

- prefixLength는 1 이상이어야 한다.

encode:

```java
byte[] frame = frameCodec.encode(body);
```

동작:

- payload length를 decimal 문자열로 만든다.
- prefixLength보다 길면 `FixedLengthMessageException`을 던진다.
- prefix는 ASCII zero-padding 숫자로 만든다.
- prefix 뒤에 payload bytes를 붙인다.

예:

```text
payload = PING
frame   = 0004PING
```

decode:

```java
byte[] payload = frameCodec.decode(frame);
```

동작:

- frame 길이가 prefixLength보다 짧으면 `FixedLengthMessageException`을 던진다.
- prefix를 ASCII 문자열로 읽고 정수로 파싱한다.
- prefix가 숫자가 아니면 `FixedLengthMessageException`을 던진다.
- prefix가 말한 payload 길이와 실제 payload 길이가 다르면 `FixedLengthMessageException`을 던진다.
- payload bytes만 반환한다.

## Consequences

좋아진 점:

- fixed-length 전문의 byte length 처리 기준이 common으로 모였다.
- 문자 필드와 숫자 필드의 기본 padding 방식이 생겼다.
- decode 시 전체 message 길이를 스펙과 비교한다.
- TCP식 length-prefixed frame을 encode/decode할 수 있다.
- protocol 코드가 security 패키지와 분리됐다.

남아 있는 점:

- ISO 8583 MTI/bitmap/data element는 구현하지 않았다.
- variable-length field는 구현하지 않았다.
- 전문 header 모델은 아직 없다.
- message type registry는 아직 없다.
- 업무 필수값 검증은 codec에 없다.
- 숫자 필드에 숫자만 들어왔는지 검증하지 않는다.
- HMAC/MAC 필드는 codec에 내장하지 않았다.
- streaming socket reader는 아직 없다.

현재 구현의 범위는 “이미 받은 bytes 또는 보낼 values를 고정 길이 전문 body/frame으로 변환하는 것”까지다.

## Tests

현재 테스트 위치:

- `common/src/test/java/com/credito/common/protocol/fixedlength/FixedLengthMessageCodecTest.java`
- `common/src/test/java/com/credito/common/protocol/fixedlength/LengthPrefixedFrameCodecTest.java`

검증하는 내용:

- 고정 byte 길이로 encode한다.
- decode 시 padding을 제거하지 않는다.
- 필드 값이 byte length를 초과하면 실패한다.
- 전체 전문 길이가 스펙과 다르면 실패한다.
- 한글처럼 다중 byte 문자를 byte length 기준으로 처리한다.
- length prefix를 ASCII 숫자로 붙인다.
- prefix와 payload 길이가 다르면 실패한다.
- prefix가 숫자가 아니면 실패한다.
