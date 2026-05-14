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
- `infra/mtls/generate-dev-certs.sh`
- 각 서비스 `application.yml`의 `server.ssl.*` 설정
- `docker-compose.yml`의 파일 단위 mTLS store 마운트와 TLS 환경변수

## Alternatives

선택지는 세 가지였다.

1. 서비스별 설정에서 직접 인증서 파일을 읽는다.

   각 서비스가 `KeyStore`, `CertificateFactory`, 파일 IO를 직접 다룬다. 서비스별로 예외 처리와 type 기본값이 달라질 수 있다.

2. Spring Boot SSL bundle 설정만 사용한다.

   Spring Boot의 SSL 설정으로 keystore/truststore를 직접 연결하는 방식이다. 런타임 연결 구성에는 적합하지만, 인증서 파일 자체를 읽어서 검사하거나 테스트하는 공통 코드로 쓰기는 제한적이다.

3. common에 인증서 로딩 유틸만 둔다.

   keystore와 X.509 certificate bundle을 로딩하는 기능만 제공한다. 실제 mTLS server/client 구성은 각 서비스 또는 infra 설정에서 담당한다.

현재 구현은 세 번째 선택지다.

dev/compose 인증서 발급은 운영 PKI와 분리된 자체 CA 방식으로 둔다. 운영용 인증서 발급과 rotation은 이 문서와 현재 구현 범위에 포함하지 않는다.

## Decision

`MtlsCertificateLoader`는 두 가지 일만 한다.

- keystore 파일을 `KeyStore`로 로딩한다.
- 인증서 파일을 `List<X509Certificate>`로 로딩한다.

dev/compose 환경은 다음 방식으로 준비한다.

- 자체 root CA를 생성한다.
- 서비스별 인증서를 생성한다.
- 서비스별 인증서는 serverAuth와 clientAuth 용도를 모두 가진다.
- 서비스별 인증서와 개인키를 PKCS12 keystore로 묶는다.
- root CA를 PKCS12 truststore로 가져온다.
- Spring Boot `server.ssl.*` 설정은 환경변수로 켜고 끌 수 있게 둔다.

이렇게 한 이유는 다음과 같다.

- mTLS 구성 방식은 runtime, container, Spring 설정에 따라 달라질 수 있다.
- common이 HTTP client/server bean 생성까지 맡으면 서비스별 설정 자유도가 줄어든다.
- 인증서 로딩은 테스트와 운영 점검에서 공통으로 재사용할 수 있다.
- 실패 시 예외 타입을 `IllegalStateException`으로 통일할 수 있다.
- compose와 테스트에서 `MTLS_ENABLED=false`를 명시 주입해 기존 HTTP healthcheck와 로컬 테스트를 유지한다.
- mTLS 검증이 필요할 때만 `MTLS_ENABLED=true`, `MTLS_CLIENT_AUTH=need`, `INTERNAL_SERVICE_SCHEME=https`를 명시한다.

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

### Dev Certificate Bootstrap

dev/compose 인증서 생성 스크립트는 다음 위치에 있다.

```text
infra/mtls/generate-dev-certs.sh
```

실행:

```bash
./infra/mtls/generate-dev-certs.sh
```

기본 password:

```text
changeit
```

환경변수로 password와 유효기간을 바꿀 수 있다.

```bash
MTLS_STORE_PASSWORD=local-secret MTLS_CERT_VALID_DAYS=30 ./infra/mtls/generate-dev-certs.sh
```

생성 결과:

```text
infra/mtls/generated
├── credito-dev-ca.crt
├── credito-dev-ca.key
├── truststore.p12
├── gateway-service/gateway-service.p12
├── customer-service/customer-service.p12
├── account-service/account-service.p12
├── lending-service/lending-service.p12
└── batch-service/batch-service.p12
```

`infra/mtls/generated/`는 Git 추적 대상에서 제외한다. 이 디렉터리에는 dev CA 개인키도 생성되지만, 런타임 서비스 컨테이너에는 디렉터리 전체를 마운트하지 않는다.

### Compose File Layout

compose 컨테이너는 서비스 실행에 필요한 파일만 read-only로 마운트한다. 모든 서비스는 공통 truststore 파일을 받고, 각 서비스는 자기 서비스의 PKCS12 keystore 파일만 받는다.

```text
./infra/mtls/generated/truststore.p12
./infra/mtls/generated/{service}/{service}.p12
```

`credito-dev-ca.key`, CSR, 개별 PEM key 파일은 런타임 컨테이너에 마운트하지 않는다.

각 서비스의 keystore 경로:

```text
/etc/credito/mtls/gateway-service/gateway-service.p12
/etc/credito/mtls/customer-service/customer-service.p12
/etc/credito/mtls/account-service/account-service.p12
/etc/credito/mtls/lending-service/lending-service.p12
/etc/credito/mtls/batch-service/batch-service.p12
```

공통 truststore 경로:

```text
/etc/credito/mtls/truststore.p12
```

### Spring SSL Environment

각 Spring 서비스는 다음 환경변수로 inbound HTTPS와 client certificate 요구 여부를 설정한다.

```text
MTLS_ENABLED
MTLS_KEY_STORE
MTLS_KEY_STORE_PASSWORD
MTLS_KEY_STORE_TYPE
MTLS_TRUST_STORE
MTLS_TRUST_STORE_PASSWORD
MTLS_TRUST_STORE_TYPE
MTLS_CLIENT_AUTH
```

로컬 HTTP 실행에 사용하는 명시 값:

```text
MTLS_ENABLED=false
MTLS_KEY_STORE=/etc/credito/mtls/{service}/{service}.p12
MTLS_KEY_STORE_PASSWORD=changeit
MTLS_KEY_STORE_TYPE=PKCS12
MTLS_TRUST_STORE=/etc/credito/mtls/truststore.p12
MTLS_TRUST_STORE_PASSWORD=changeit
MTLS_TRUST_STORE_TYPE=PKCS12
MTLS_CLIENT_AUTH=none
INTERNAL_SERVICE_SCHEME=http
```

mTLS handshake를 확인하려면 dev 인증서를 생성한 뒤 다음 값을 사용한다.

```bash
MTLS_ENABLED=true
MTLS_CLIENT_AUTH=need
MTLS_STORE_PASSWORD=changeit
INTERNAL_SERVICE_SCHEME=https
```

### Outbound Client Certificate

기본 `docker-compose.yml`은 `JAVA_TOOL_OPTIONS`로 JVM trustStore를 지정하지 않는다. 이 상태에서는 JVM이 기본 `cacerts` 신뢰 체인을 사용하므로, 공개 CA 기반 외부 HTTPS 호출 경로를 유지한다.

mTLS outbound client certificate와 내부 CA trustStore가 필요한 실행은 `docker-compose.mtls.yml` overlay를 함께 사용한다. overlay는 각 Java 프로세스에 다음 JVM SSL system property를 주입한다.

```text
javax.net.ssl.keyStore
javax.net.ssl.keyStorePassword
javax.net.ssl.trustStore
javax.net.ssl.trustStorePassword
```

```bash
MTLS_ENABLED=true \
MTLS_CLIENT_AUTH=need \
MTLS_STORE_PASSWORD=changeit \
INTERNAL_SERVICE_SCHEME=https \
docker compose -f docker-compose.yml -f docker-compose.mtls.yml up
```

현재 프로젝트에는 내부 서비스 호출 전용 HTTP client bean이 아직 없다. 따라서 이 설정은 JVM 기본 SSL context를 사용하는 client가 생겼을 때 같은 인증서 파일을 참조하도록 준비하는 단계다.

## Consequences

좋아진 점:

- 인증서와 keystore 로딩 코드가 서비스마다 흩어지지 않는다.
- 실패 메시지가 `KeyStore 로딩에 실패했습니다`, `X.509 인증서 로딩에 실패했습니다` 형태로 통일된다.
- 테스트에서 인증서 로딩 실패 케이스를 common 기준으로 검증할 수 있다.
- dev/compose 자체 CA와 서비스별 인증서 생성 방식이 스크립트로 재현 가능해졌다.
- 각 Spring 서비스가 keystore/truststore를 환경변수로 참조할 수 있다.
- Gateway route는 `INTERNAL_SERVICE_SCHEME`으로 HTTP/HTTPS 전환이 가능하다.
- mTLS overlay를 붙이지 않은 기본 compose 실행은 JVM 기본 `cacerts`를 유지한다.

남아 있는 점:

- compose 실행 시 mTLS 관련 환경변수는 명시적으로 주입해야 한다.
- hostname 검증, SAN 검증, certificate chain 정책은 이 클래스에 없다.
- 인증서 만료 검사 API는 아직 없다.
- 운영용 PKI 연동과 인증서 rotation은 구현하지 않았다.
- 내부 서비스 호출 전용 HTTP client bean은 아직 없다.

현재 구현의 범위는 “dev/compose에서 mTLS 인증서와 store 파일을 재현 가능하게 만들고, Spring 서비스가 해당 파일을 참조할 수 있게 준비하는 것”까지다.
