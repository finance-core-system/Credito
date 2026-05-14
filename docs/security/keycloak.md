# Keycloak 인증 구성 기록

## 문제 상황

Credito에는 성격이 다른 세 종류의 주체가 있다. 고객은 외부 사용자로서 고객 API를 사용하고, 운영자는 더 넓은 데이터와 관리 기능에 접근하며, 내부 서비스는 사용자 없이 서비스 간 호출을 수행한다.

이 세 흐름을 각 업무 서비스가 직접 처리하면 로그인, 토큰 발급, 비밀번호 정책, MFA, 역할 매핑이 서비스마다 흩어진다. 인증 정책이 흩어지면 변경 비용이 커지고, 서비스별로 보안 기준이 달라질 가능성도 높아진다.

따라서 이번 단계에서는 인증과 토큰 발급을 업무 서비스에서 분리하고, Keycloak을 인증 서버로 두는 구조를 잡았다. 업무 서비스는 사용자를 직접 로그인시키지 않고, 이후 Resource Server로서 Keycloak이 발급한 토큰을 검증하는 방향을 따른다.

## 목표

이번 구성의 목표는 운영 환경 완성이 아니라 개발 환경에서 재현 가능한 인증 기본선을 만드는 것이다.

- 고객, 운영자, 내부 서비스 계정을 분리한다.
- 브라우저 기반 client와 서비스 간 client의 인증 흐름을 다르게 둔다.
- 운영자 계정에는 MFA와 업무 role 모델을 둔다.
- 서비스가 검증할 issuer, audience, role claim 기준을 문서화한다.
- 같은 설정을 Docker Compose에서 반복적으로 가져올 수 있게 realm import 파일로 관리한다.

## 전체 구조

Keycloak은 Docker Compose의 `keycloak` 서비스로 실행된다. realm 설정은 `infra/keycloak/import` 아래 JSON 파일로 관리한다.

컨테이너 시작 시 Compose는 각 realm 파일을 명시적으로 import한 뒤 Keycloak을 시작한다.

```text
customer-realm.json -> admin-realm.json -> system-realm.json -> start-dev
```

import는 `--override false`로 실행한다. 이미 MySQL volume에 같은 realm이 있으면 덮어쓰지 않는다. 실수로 로컬 realm을 덮어쓰는 위험은 줄어들지만, import 파일 변경 사항을 기존 로컬 DB에 자동 반영하지는 않는다. 최신 import 파일 기준으로 다시 만들려면 Keycloak 데이터가 들어 있는 MySQL volume을 초기화해야 한다.

## Realm 분리

realm은 사용자 영역, issuer, 정책 경계를 나누는 단위로 사용했다.

| Realm | 목적 | 외부 issuer | 내부 issuer |
| --- | --- | --- | --- |
| `customer-realm` | 고객 로그인과 고객 API 접근 | `http://localhost:8085/realms/customer-realm` | `http://keycloak:8080/realms/customer-realm` |
| `admin-realm` | 운영자 로그인과 운영 API 접근 | `http://localhost:8085/realms/admin-realm` | `http://keycloak:8080/realms/admin-realm` |
| `system-realm` | 내부 서비스 간 토큰 발급 | `http://localhost:8085/realms/system-realm` | `http://keycloak:8080/realms/system-realm` |

하나의 realm에 모든 client와 role을 넣는 방식도 가능하다. 하지만 고객 계정, 운영자 계정, 내부 서비스 계정은 보안 정책과 토큰 수신자가 다르다. realm을 분리하면 issuer가 명확해지고, 각 영역의 로그인 정책과 role 모델을 독립적으로 다룰 수 있다.

반대로 realm이 늘어나면 import 파일과 설정 중복도 늘어난다. 현재는 분리로 얻는 경계 명확성이 더 중요하다고 판단했다.

## Client 구성

| Realm | Client | Client 타입 | 인증 흐름 | Audience |
| --- | --- | --- | --- | --- |
| `customer-realm` | `credito-customer-web` | public | Authorization Code + PKCE | `credito-api` |
| `admin-realm` | `credito-admin-console` | public | Authorization Code + PKCE | `credito-admin-api` |
| `system-realm` | `gateway-service` | confidential | Client Credentials | `credito-internal-api` |
| `system-realm` | `customer-service` | confidential | Client Credentials | `credito-internal-api` |
| `system-realm` | `account-service` | confidential | Client Credentials | `credito-internal-api` |
| `system-realm` | `lending-service` | confidential | Client Credentials | `credito-internal-api` |
| `system-realm` | `batch-service` | confidential | Client Credentials | `credito-internal-api` |

`credito-customer-web`과 `credito-admin-console`은 브라우저 기반 UI를 전제로 한다. 브라우저 앱은 client secret을 안전하게 보관할 수 없으므로 public client로 두고 PKCE를 사용한다.

`system-realm`의 서비스별 client는 사용자 브라우저가 아니라 서버 간 호출에 사용한다. 각 client는 secret을 안전하게 보관할 수 있는 내부 서비스가 사용하는 것을 전제로 하므로 confidential client와 client credentials 흐름을 사용한다.

서비스별 client scope는 호출 가능한 내부 API를 표현한다.

| Client | 요청 가능한 scope |
| --- | --- |
| `gateway-service` | `customers.read`, `accounts.read`, `lendings.read`, `batch.run` |
| `customer-service` | 없음 |
| `account-service` | `customers.read` |
| `lending-service` | `customers.read`, `accounts.read` |
| `batch-service` | `accounts.read`, `lendings.read` |

수신 서비스는 `system-realm` issuer, `credito-internal-api` audience, service client id, scope를 함께 검증한다. 내부 API별 수신 정책은 다음 기준으로 둔다.

| 수신 서비스 | 보호 path | 허용 client | 필수 scope |
| --- | --- | --- | --- |
| `customer-service` | `/api/customers/**` | `gateway-service`, `account-service`, `lending-service` | `customers.read` |
| `account-service` | `/api/accounts/**` | `gateway-service`, `lending-service`, `batch-service` | `accounts.read` |
| `lending-service` | `/api/lendings/**` | `gateway-service`, `batch-service` | `lendings.read` |
| `batch-service` | `/api/batch/**` | `gateway-service` | `batch.run` |

## 주요 설정값

### 공통 realm 설정

`enabled: true`는 realm을 활성화한다. import 파일에 realm이 있어도 비활성화되어 있으면 로그인과 토큰 발급에 사용할 수 없다.

`sslRequired: external`은 외부 요청에는 HTTPS를 요구하되, 로컬 개발이나 내부 통신에서는 HTTP를 허용하는 Keycloak 개발 친화 설정이다. 운영 환경에서는 TLS 종료 방식에 맞춰 더 엄격하게 조정해야 한다.

`registrationAllowed: false`는 사용자가 직접 가입하는 흐름을 막는다. 현재 프로젝트에서는 고객/운영자 계정 생성 정책을 아직 정의하지 않았고, 임의 가입을 허용할 단계가 아니다.

`loginWithEmailAllowed: true`는 이메일 기반 로그인을 허용한다. 사용자 식별자는 이후 도메인 정책에 맞춰 조정할 수 있지만, 개발 단계에서는 이메일 로그인이 가장 단순하다.

`duplicateEmailsAllowed: false`는 같은 이메일을 여러 계정이 공유하지 못하게 한다. 운영자 계정에서는 특히 감사와 추적을 위해 이메일 중복을 피하는 편이 낫다.

`resetPasswordAllowed: true`는 비밀번호 재설정 흐름을 허용한다. 실제 메일 발송, 템플릿, 정책은 운영 환경 구성에서 별도로 다뤄야 한다.

`bruteForceProtected: true`는 반복 로그인 실패에 대한 Keycloak의 기본 방어 기능을 켠다. 세부 임계값은 아직 조정하지 않았고, 기본선을 켜두는 수준이다.

### PKCE 설정

브라우저 기반 client에는 다음 설정을 둔다.

```json
"attributes": {
  "pkce.code.challenge.method": "S256"
}
```

PKCE는 authorization code가 탈취되더라도 공격자가 토큰으로 교환하기 어렵게 만든다. `S256`은 plain challenge보다 안전한 방식이므로 public client에는 기본값으로 둔다.

### Redirect URI

운영자 client의 redirect URI는 다음처럼 구체적인 callback 경로만 허용한다.

```json
"redirectUris": [
  "http://localhost:8080/admin/callback",
  "http://localhost:3000/admin/callback"
]
```

처음에는 `/admin/*`처럼 넓은 와일드카드를 둘 수 있지만, 리다이렉트 허용 범위가 커진다. 현재는 실제 로그인 callback으로 쓸 경로만 허용해 의도하지 않은 리다이렉트 가능성을 줄였다.

logout redirect도 별도 callback으로 제한했다.

```json
"post.logout.redirect.uris": "http://localhost:8080/admin/logout/callback##http://localhost:3000/admin/logout/callback"
```

### Audience mapper

각 client에는 audience mapper를 둔다.

```json
"protocolMapper": "oidc-audience-mapper",
"included.custom.audience": "credito-admin-api",
"access.token.claim": "true",
"id.token.claim": "false"
```

`aud`는 토큰이 어떤 API를 대상으로 발급됐는지 나타낸다. 서비스는 자신이 기대하는 audience가 없는 토큰을 거부해야 한다.

`access.token.claim: true`는 API 호출에 쓰이는 access token에 audience를 넣겠다는 뜻이다. `id.token.claim: false`는 로그인한 사용자의 신원 정보를 담는 ID token에는 API audience를 넣지 않겠다는 뜻이다.

### 운영자 MFA 설정

`admin-realm`에는 TOTP 정책과 `CONFIGURE_TOTP` required action을 추가했다.

```json
"otpPolicyType": "totp",
"otpPolicyAlgorithm": "HmacSHA1",
"otpPolicyDigits": 6,
"otpPolicyLookAheadWindow": 1,
"otpPolicyPeriod": 30
```

`otpPolicyType: totp`는 시간 기반 일회용 비밀번호를 사용한다는 뜻이다. 일반적인 인증 앱과 호환된다.

`otpPolicyAlgorithm: HmacSHA1`, `otpPolicyDigits: 6`, `otpPolicyPeriod: 30`은 대부분의 OTP 앱에서 사용하는 기본 조합이다. 보안 강도를 더 높일 수는 있지만, 초기 개발 환경에서는 호환성과 재현성을 우선했다.

`otpPolicyLookAheadWindow: 1`은 시간 오차를 어느 정도 허용한다. 사용자의 기기 시간과 서버 시간이 약간 어긋나도 인증 실패가 과도하게 발생하지 않도록 한다.

required action은 다음 의미를 갖는다.

```json
{
  "alias": "CONFIGURE_TOTP",
  "enabled": true,
  "defaultAction": true
}
```

`enabled: true`는 OTP 등록 액션을 사용할 수 있게 한다. `defaultAction: true`는 새 운영자 계정이 로그인할 때 OTP 등록을 요구하도록 한다. 이 방식은 별도 custom browser flow를 만드는 것보다 단순하고 import로 재현하기 쉽다.

다만 이 설정은 세부 조건부 MFA 정책까지 표현하지 않는다. 예를 들어 특정 네트워크에서는 MFA를 완화하거나, 고위험 작업에서만 재인증을 요구하는 정책은 후속 작업에서 별도로 설계해야 한다.

## 운영자 Role 모델

운영자 권한은 realm role로 정의했다.

| Role | 의미 |
| --- | --- |
| `ROLE_OPERATOR` | 운영자 콘솔 접근과 기본 운영 업무 |
| `ROLE_LOAN_REVIEWER` | 대출 심사 업무 |
| `ROLE_MANAGER` | 관리자 승인과 상위 운영 판단 |
| `ROLE_BATCH_ADMIN` | 배치 작업 운영과 관리 |

role은 직접 사용자에게 붙이기보다 group을 통해 부여하는 방식을 기본으로 한다.

| Group | 부여 role | 책임 |
| --- | --- | --- |
| `operators` | `ROLE_OPERATOR` | 기본 운영자 접근 |
| `loan-reviewers` | `ROLE_OPERATOR`, `ROLE_LOAN_REVIEWER` | 대출 심사 업무 |
| `managers` | `ROLE_OPERATOR`, `ROLE_MANAGER` | 관리자 승인 업무 |
| `batch-admins` | `ROLE_OPERATOR`, `ROLE_BATCH_ADMIN` | 배치 운영 업무 |

realm role을 선택한 이유는 여러 서비스가 같은 운영자 role을 읽어야 하기 때문이다. client role을 쓰면 특정 client에 권한이 더 잘 묶이지만, 서비스 전반에서 공통 운영자 권한을 해석해야 하는 현재 구조에서는 realm role이 더 단순하다.

대신 realm role은 범위가 넓다. 실제 API별 허용 여부는 각 서비스의 보안 설정에서 별도로 판단해야 한다.

## Role claim 매핑

Keycloak 기본 토큰에는 realm role이 보통 `realm_access.roles` 아래에 들어간다. 이 구조는 Keycloak에 특화되어 있다.

서비스 구현을 단순하게 하기 위해 `credito-admin-console`에는 별도 mapper를 추가했다.

```json
"protocolMapper": "oidc-usermodel-realm-role-mapper",
"claim.name": "roles",
"multivalued": "true",
"access.token.claim": "true",
"id.token.claim": "false",
"userinfo.token.claim": "true"
```

이 설정은 access token에 top-level `roles` claim을 문자열 배열로 넣는다.

```json
{
  "roles": ["ROLE_OPERATOR", "ROLE_MANAGER"]
}
```

서비스는 이 claim을 Spring Security authority로 변환하면 된다. Keycloak 기본 claim인 `realm_access.roles`도 유지되므로, 후속 구현에서 둘 중 어떤 claim을 표준으로 삼을지 결정할 수 있다.

top-level `roles`는 구현을 단순하게 하지만 표준 claim 이름은 아니다. 따라서 프로젝트 내부 계약으로 문서화하고, 서비스 구현에서 일관되게 사용해야 한다.

## 선택 과정과 트레이드오프

### Realm 분리

realm을 고객, 운영자, 내부 서비스로 분리했다. 하나의 realm에 role과 client를 모두 넣는 방식보다 설정 파일이 늘어나지만, 사용자 영역과 토큰 issuer가 명확해진다. 고객 계정과 운영자 계정의 보안 정책을 다르게 가져갈 수 있고, 내부 서비스 계정도 사용자 로그인 영역과 분리된다.

### Admin client 타입

운영자 client는 confidential client가 아니라 public client + PKCE로 구성했다. admin console이라는 이름과 redirect URI가 브라우저 기반 UI를 전제로 하고 있고, 현재 코드베이스에는 client secret을 안전하게 보관할 BFF 컴포넌트가 없다. BFF를 도입하면 confidential client가 더 적절할 수 있지만, 지금 단계에서는 없는 서버 컴포넌트를 가정하지 않는 쪽을 선택했다.

### MFA 적용 방식

운영자 MFA는 복잡한 custom authentication flow 대신 TOTP required action으로 시작했다. 이 방식은 단순하고 import로 재현하기 쉽다. 다만 세부 조건부 MFA, 조직별 예외 정책, 강제 재등록 정책 같은 운영 정책은 아직 표현하지 않는다.

### Role 모델

role은 realm role과 group 매핑으로 정의했다. client role을 쓰면 특정 client에 권한이 더 강하게 묶이지만, 여러 서비스가 같은 운영자 role을 읽어야 하는 구조에서는 realm role이 더 단순하다. 대신 role 이름이 넓은 범위를 갖기 때문에 서비스별 세부 권한 판단은 각 서비스 보안 이슈에서 별도로 정리해야 한다.

### Role claim

토큰에는 top-level `roles` claim을 추가했다. Keycloak 기본 구조에만 의존하면 서비스 코드가 `realm_access.roles` 같은 Keycloak 특화 claim 구조를 알아야 한다. top-level claim은 서비스 구현을 단순하게 하지만, 표준 claim 이름은 아니므로 문서화된 내부 계약으로 관리해야 한다.

### Realm import

Compose import는 `--override false`를 사용한다. 로컬에서 이미 생성된 realm을 실수로 덮어쓰지 않는 장점이 있다. 반대로 import 파일 변경 사항은 기존 MySQL volume에 자동 반영되지 않는다. 로컬 환경을 최신 import 파일 기준으로 다시 만들려면 Keycloak 데이터가 들어 있는 MySQL volume을 초기화해야 한다.

## 현재 결과

현재 Keycloak bootstrap은 다음 상태다.

- Compose에서 Keycloak과 MySQL이 함께 기동된다.
- 세 realm이 import 파일로 재현 가능하다.
- 고객/운영자 브라우저 client는 Authorization Code + PKCE를 사용한다.
- 내부 서비스 client는 Client Credentials를 사용한다.
- 내부 서비스 client는 서비스별 client id와 scope로 분리되어 있다.
- 운영자 realm에는 TOTP required action과 role/group 모델이 있다.
- 운영자 access token에는 서비스가 읽기 쉬운 `roles` claim이 포함된다.
- issuer와 audience 기준은 `infra/keycloak/README.md`와 이 문서에 정리되어 있다.
- customer/account/lending/batch 서비스에는 service-token 수신 정책이 연결되어 있다.

## 남은 일

이번 작업 이후에도 남은 보안 처리는 다음과 같다.

- 운영자 role 기반 API 권한 체크
- 운영 환경용 secret 관리
- 운영 환경 TLS와 hostname 정책
- 운영자 MFA 세부 정책
- Gateway route에서 service-token을 자동 발급해 내부 호출에 첨부하는 filter 구성
