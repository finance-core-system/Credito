# Service Token Flow

## Background

mTLS는 연결을 맺은 workload의 인증서 신원을 확인한다. 하지만 호출자가 어떤 내부 API를 어떤 권한으로 호출할 수 있는지까지 표현하지 않는다.

내부 서비스 호출에는 별도의 application-level 권한 표현이 필요하다. 이번 구현은 Keycloak `system-realm`의 `client_credentials` 흐름으로 service-token을 발급하고, 수신 서비스에서 `issuer`, `audience`, `client_id`, `scope`, `expiry`를 검증하는 구조를 둔다.

## Decision

내부 서비스 계정은 서비스별 Keycloak client로 분리한다.

| Service client | Requestable scopes |
| --- | --- |
| `gateway-service` | `customers.read`, `accounts.read`, `lendings.read`, `batch.run` |
| `customer-service` | none |
| `account-service` | `customers.read` |
| `lending-service` | `customers.read`, `accounts.read` |
| `batch-service` | `accounts.read`, `lendings.read` |

발급 토큰의 audience는 `credito-internal-api`로 고정한다. 수신 서비스는 사용자/운영자 JWT와 내부 service-token을 같은 resource server 기반에서 검증하되, 내부 API path에는 추가 service-token rule을 적용한다.

## Implementation

Keycloak import 파일 `infra/keycloak/import/system-realm.json`에 서비스별 confidential client와 내부 scope를 정의했다.

공통 모듈에는 다음 컴포넌트를 둔다.

- `ServiceTokenClient`: token endpoint에 `client_credentials` 요청을 보내 access token을 발급받는다.
- `ServiceTokenValidator`: JWT의 issuer, audience, client id, expiry, scope를 검증한다.
- `ServiceTokenAuthorizationFilter`: 설정된 내부 API path에 대해 service-token 정책을 강제한다.
- `CreditoResourceServerProperties.service-token`: 수신 서비스별 내부 API 허용 정책을 설정한다.

수신 정책은 현재 다음과 같다.

| Receiving service | Protected path | Allowed clients | Required scope |
| --- | --- | --- | --- |
| `customer-service` | `/api/customers/**` | `gateway-service`, `account-service`, `lending-service` | `customers.read` |
| `account-service` | `/api/accounts/**` | `gateway-service`, `lending-service`, `batch-service` | `accounts.read` |
| `lending-service` | `/api/lendings/**` | `gateway-service`, `batch-service` | `lendings.read` |
| `batch-service` | `/api/batch/**` | `gateway-service` | `batch.run` |

`gateway-service`는 외부 요청을 받는 진입점이므로 이번 변경에서 service-token 수신 rule을 두지 않았다. 내부 서비스로 호출할 때 사용할 service-token 발급 컴포넌트만 공통 모듈에 제공한다.

## Consequences

- service-token은 `system-realm` issuer와 `credito-internal-api` audience를 가져야 한다.
- 내부 API path는 허용된 service client와 필수 scope를 모두 만족해야 한다.
- 만료 시각이 없거나 만료된 토큰은 거부한다.
- mTLS client certificate가 없는 요청은 내부 API rule에서 거부한다.
- 현재 HTTP client bean 또는 Gateway route filter에 service-token 자동 첨부는 구현하지 않았다.
