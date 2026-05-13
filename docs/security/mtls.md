# Mutual TLS

## Background

Credito 서비스 간 통신은 JWT resource server 설정을 통해 issuer, audience, authority를 검증한다. 하지만 JWT는 애플리케이션 계층의 token 검증이고, 네트워크 연결 자체가 어떤 서비스에서 왔는지 확인하는 기능은 아니다.

mTLS는 TLS 연결 단계에서 client와 server가 서로 인증서를 제시하고 검증하는 방식이다.

일반 TLS:

```text
client -> server certificate 검증
```

mTLS:

```text
client -> server certificate 검증
server -> client certificate 검증
```

현재 common 모듈에는 mTLS 연결 전체를 구성하는 auto-configuration은 없다. 대신 인증서와 keystore를 로딩하는 작은 컴포넌트만 있다.

현재 구현 위치:

- `com.credito.common.security.mtls.MtlsCertificateLoader`

## Alternatives

선택지는 세 가지였다.

1. 서비스별 설정에서 직접 인증서 파일을 읽는다.

   각 서비스가 `KeyStore`, `CertificateFactory`, 파일 IO를 직접 다룬다. 서비스별로 예외 처리와 type 기본값이 달라질 수 있다.

2. Spring Boot SSL bundle 설정만 사용한다.

   Spring Boot의 SSL 설정으로 keystore/truststore를 직접 연결하는 방식이다. 런타임 연결 구성에는 적합하지만, 인증서 파일 자체를 읽어서 검사하거나 테스트하는 공통 코드로 쓰기는 제한적이다.

3. common에 인증서 로딩 유틸만 둔다.

   keystore와 X.509 certificate bundle을 로딩하는 기능만 제공한다. 실제 mTLS server/client 구성은 각 서비스 또는 infra 설정에서 담당한다.

현재 구현은 세 번째 선택지다.

## Decision

`MtlsCertificateLoader`는 두 가지 일만 한다.

- keystore 파일을 `KeyStore`로 로딩한다.
- 인증서 파일을 `List<X509Certificate>`로 로딩한다.

이렇게 한 이유는 다음과 같다.

- mTLS 구성 방식은 runtime, container, Spring 설정에 따라 달라질 수 있다.
- common이 HTTP client/server bean 생성까지 맡으면 서비스별 설정 자유도가 줄어든다.
- 인증서 로딩은 테스트와 운영 점검에서 공통으로 재사용할 수 있다.
- 실패 시 예외 타입을 `IllegalStateException`으로 통일할 수 있다.

## Implementation

### KeyStore Loading

```java
KeyStore keyStore = MtlsCertificateLoader.loadKeyStore(
    Path.of("certs/service.p12"),
    password,
    "PKCS12");
```

구현 내용:

- `Files.newInputStream(path)`로 파일을 연다.
- type이 null이거나 blank이면 `KeyStore.getDefaultType()`을 사용한다.
- type이 있으면 해당 type으로 `KeyStore.getInstance(type)`을 호출한다.
- `keyStore.load(inputStream, password)`로 keystore를 로딩한다.
- IO 또는 security 예외가 발생하면 `IllegalStateException`으로 감싼다.

결과:

- 호출자는 로딩된 `KeyStore`를 받는다.
- 파일이 없거나 password가 틀리거나 type이 맞지 않으면 실패한다.
- password 보관 방식은 이 클래스가 다루지 않는다.

### X.509 Certificate Loading

```java
List<X509Certificate> certificates =
    MtlsCertificateLoader.loadX509Certificates(Path.of("certs/ca.pem"));
```

구현 내용:

- `Files.newInputStream(path)`로 파일을 연다.
- `CertificateFactory.getInstance("X.509")`를 사용한다.
- `generateCertificates(inputStream)`으로 파일 안의 certificate들을 읽는다.
- 결과 중 `X509Certificate` 타입만 필터링한다.
- 실패하면 `IllegalStateException`으로 감싼다.

결과:

- PEM 또는 DER 형태의 X.509 인증서를 읽을 수 있다.
- 여러 인증서가 들어 있는 bundle도 collection으로 처리한다.
- X.509 certificate가 아닌 값은 결과에서 제외된다.

## Consequences

좋아진 점:

- 인증서와 keystore 로딩 코드가 서비스마다 흩어지지 않는다.
- 실패 메시지가 `KeyStore 로딩에 실패했습니다`, `X.509 인증서 로딩에 실패했습니다` 형태로 통일된다.
- 테스트에서 인증서 로딩 실패 케이스를 common 기준으로 검증할 수 있다.

남아 있는 점:

- mTLS 연결을 자동으로 구성하지 않는다.
- hostname 검증, SAN 검증, certificate chain 정책은 이 클래스에 없다.
- 인증서 만료 검사 API는 아직 없다.
- keystore password 로딩 방식은 구현하지 않았다.
- Spring Boot SSL bundle과 직접 연결하는 설정 클래스는 없다.

현재 구현의 범위는 “mTLS에 필요한 인증서 자료를 읽는 것”까지다.
