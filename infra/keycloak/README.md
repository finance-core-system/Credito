# Keycloak Bootstrap

This directory contains the development Keycloak realm import used by Docker Compose.

## Compose import

`docker-compose.yml` mounts `infra/keycloak/import` into `/opt/keycloak/data/import`, imports each realm file explicitly, and then starts Keycloak.

Realm import is intended for local and CI development bootstrap only. Client secrets in the import files are fixed development values and must not be reused in production.

## Realms

| Realm | Issuer | Purpose |
| --- | --- | --- |
| `customer-realm` | `http://localhost:8085/realms/customer-realm` | External customer login and customer-facing API access |
| `admin-realm` | `http://localhost:8085/realms/admin-realm` | Operator login and admin API access |
| `system-realm` | `http://localhost:8085/realms/system-realm` | Internal service account token issuance |

Inside Compose networks, services should use the container address issuer base:

| Realm | Internal issuer |
| --- | --- |
| `customer-realm` | `http://keycloak:8080/realms/customer-realm` |
| `admin-realm` | `http://keycloak:8080/realms/admin-realm` |
| `system-realm` | `http://keycloak:8080/realms/system-realm` |

## Clients and audiences

| Realm | Client ID | Access token audience | Flow |
| --- | --- | --- | --- |
| `customer-realm` | `credito-customer-web` | `credito-api` | Authorization Code + PKCE |
| `admin-realm` | `credito-admin-console` | `credito-admin-api` | Authorization Code |
| `system-realm` | `credito-service-client` | `credito-internal-api` | Client Credentials |

Resource servers should validate:

- `iss`: one of the issuer URLs for the accepted realm.
- `aud`: the expected API audience for the endpoint group.
- `azp` or `client_id`: the expected Keycloak client identity.
- `exp`: token expiry.

The service-specific JWT validation and service-token authorization rules are intentionally handled in later security issues.
