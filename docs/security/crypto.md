# Crypto And Masking

## Background

Credito 서비스는 고객 정보, 계좌 정보, 서비스 간 요청 식별자, 전문 payload 등 보안상 민감한 값을 다룬다.

이 값들은 상황에 따라 서로 다른 처리가 필요하다.

- 원문을 저장하지 않고도 같은 값인지 비교해야 한다.
- 서비스 간 메시지가 중간에 변조되지 않았는지 확인해야 한다.
- token, signature 같은 값을 비교할 때 timing 차이를 줄여야 한다.
- 로그와 감사 이벤트에는 민감 정보 원문을 남기지 않아야 한다.
- 테스트용 난수가 아니라 운영에서 사용할 수 있는 난수가 필요하다.

그래서 `common` 모듈에 작은 crypto/masking 유틸리티를 두었다.

현재 구현 위치:

- `com.credito.common.security.crypto.CryptoUtil`
- `com.credito.common.security.crypto.MaskingUtil`

## Alternatives

선택지는 크게 세 가지였다.

1. 각 서비스에서 직접 구현한다.

   서비스마다 `MessageDigest`, `Mac`, masking 로직을 직접 작성하는 방식이다. 빠르게 시작할 수 있지만, hashing charset, HMAC 출력 포맷, masking 규칙이 서비스마다 달라질 수 있다.

2. 외부 보안 라이브러리를 감싼다.

   Apache Commons Codec, Spring Security crypto, Bouncy Castle 같은 라이브러리를 사용할 수 있다. 기능은 많지만 현재 필요한 기능은 SHA-256, HMAC-SHA256, secure random, constant-time compare, masking 정도라서 의존성을 늘릴 이유가 크지 않았다.

3. JDK 표준 API 위에 얇은 공통 유틸리티를 둔다.

   `MessageDigest`, `Mac`, `SecureRandom`, `MessageDigest.isEqual`만 사용해서 현재 필요한 동작을 고정한다. 기능 범위는 작지만 서비스 간 출력 포맷과 실패 방식을 맞출 수 있다.

현재 구현은 세 번째 선택지다.

## Decision

`CryptoUtil`은 JDK 표준 crypto API만 사용한다.

이렇게 한 이유는 다음과 같다.

- 현재 필요한 알고리즘이 JDK 기본 제공 범위 안에 있다.
- `common` 모듈의 보안 유틸이 무거운 crypto abstraction이 되는 것을 피할 수 있다.
- 서비스 간 HMAC 결과 포맷을 Base64로 고정할 수 있다.
- hash 입력 charset을 UTF-8로 고정할 수 있다.
- signature 비교 방식을 `constantTimeEquals`로 모아둘 수 있다.

`MaskingUtil`은 암호화가 아니라 표시용 masking 유틸로 분리했다.

이렇게 한 이유는 다음과 같다.

- masking은 보안 저장 방식이 아니라 로그와 감사 이벤트 표시 방식이다.
- 이메일, 전화번호, 계좌번호의 masking 규칙을 서비스마다 다르게 두지 않기 위해서다.
- 원문 복원이 불가능한 형태로 로그 노출 범위를 줄이는 것이 목적이다.

## Implementation

### SHA-256 Hex

```java
String digest = CryptoUtil.sha256Hex(value);
```

구현 내용:

- 입력 문자열을 UTF-8 bytes로 변환한다.
- `MessageDigest.getInstance("SHA-256")`로 digest를 계산한다.
- `HexFormat`으로 lowercase hex 문자열을 만든다.
- 알고리즘을 사용할 수 없으면 `IllegalStateException`으로 감싼다.

결과:

- 같은 입력은 항상 같은 hex 값을 만든다.
- 원문은 복원할 수 없다.
- salt는 사용하지 않는다.

현재 구현은 password hashing 용도가 아니다. 비밀번호 저장 로직은 이 유틸로 구현되어 있지 않다.

### HMAC-SHA256 Base64

```java
String signature = CryptoUtil.hmacSha256Base64(secret, value);
```

구현 내용:

- secret과 value를 UTF-8 bytes로 변환한다.
- `Mac.getInstance("HmacSHA256")`을 사용한다.
- `SecretKeySpec`으로 HMAC key를 초기화한다.
- HMAC 결과 bytes를 Base64 문자열로 반환한다.
- 실패하면 `IllegalStateException`으로 감싼다.

결과:

- 같은 secret과 같은 value는 같은 Base64 signature를 만든다.
- secret이 다르면 같은 value여도 다른 signature가 나온다.
- 출력 포맷은 hex가 아니라 Base64다.

fixed-length 전문에 HMAC을 붙일 경우, 현재 유틸은 문자열 입력을 받는다. payload bytes를 어떤 charset으로 문자열화할지는 호출자가 결정한다.

### Secure Random Bytes

```java
byte[] bytes = CryptoUtil.secureRandomBytes(32);
```

구현 내용:

- 클래스 내부에 static `SecureRandom` 인스턴스를 둔다.
- 요청 길이가 0 이하이면 `IllegalArgumentException`을 던진다.
- 지정한 길이의 byte 배열을 만들고 `nextBytes`로 채운다.

결과:

- 호출자는 운영용 난수 bytes를 받을 수 있다.
- encoding은 호출자가 결정한다.
- token 문자열 생성까지는 이 유틸이 담당하지 않는다.

### Constant-Time Equals

```java
boolean matched = CryptoUtil.constantTimeEquals(left, right);
```

구현 내용:

- 둘 중 하나라도 null이면 false를 반환한다.
- 두 문자열을 UTF-8 bytes로 변환한다.
- `MessageDigest.isEqual`로 비교한다.

결과:

- 일반 `String.equals` 대신 signature 비교에 사용할 수 있다.
- 입력 문자열 길이 차이에 대한 처리는 JDK `MessageDigest.isEqual` 동작을 따른다.
- null-safe하게 false를 반환한다.

### Masking

```java
MaskingUtil.maskEmail("user@example.com")      // u****@example.com
MaskingUtil.maskPhone("01012345678")          // ****5678
MaskingUtil.maskAccountNumber("1234567890")   // ****7890
MaskingUtil.keepLast("abcdef", 2)             // ****ef
```

구현 내용:

- `maskEmail`은 첫 글자와 `@domain`만 남긴다.
- `maskPhone`은 마지막 4자리만 남긴다.
- `maskAccountNumber`는 마지막 4자리만 남긴다.
- `keepLast`는 지정한 길이만큼 뒤쪽 문자를 남긴다.
- null이거나 blank이면 입력값을 그대로 반환한다.
- 노출 길이가 0 이하이거나 원문 길이가 노출 길이보다 짧거나 같으면 `****`를 반환한다.

결과:

- 로그와 감사 이벤트에서 원문 노출을 줄일 수 있다.
- masking 결과는 원문 복원용 값이 아니다.
- 전화번호나 계좌번호의 구분자 제거는 하지 않는다. 호출자가 정규화한 값을 넘겨야 한다.

## Consequences

현재 crypto 구현은 의도적으로 작다.

좋아진 점:

- hash charset이 UTF-8로 고정됐다.
- HMAC 알고리즘과 출력 포맷이 `HmacSHA256 + Base64`로 고정됐다.
- signature 비교용 constant-time 비교 진입점이 생겼다.
- 서비스마다 masking 규칙을 다시 만들 필요가 없어졌다.

남아 있는 점:

- password hashing은 구현하지 않았다.
- AES 같은 양방향 암호화는 구현하지 않았다.
- key rotation이나 secret storage는 구현하지 않았다.
- HMAC 입력을 byte[]로 직접 받는 API는 아직 없다.
- masking 전 정규화는 호출자가 처리한다.
