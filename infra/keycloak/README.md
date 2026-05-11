# Keycloak Bootstrap

This directory contains the development Keycloak realm import used by Docker Compose.

## Compose import

`docker-compose.yml` mounts `infra/keycloak/import` into `/opt/keycloak/data/import`, imports each realm file explicitly, and then starts Keycloak.

Realm import is intended for local and CI development bootstrap only. Client secrets in the import files are fixed development values and must not be reused in production.

Compose imports use `--override false`, so existing realms in the MySQL volume are not overwritten on restart. Reset the `mysql-data` volume when a local environment needs to be re-bootstrapped from the current import files.

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

## Admin MFA and roles

`admin-realm` enables TOTP as the default required action for operator accounts. New operator users must configure OTP during login, and the default browser authentication flow will require OTP after it is configured.

Admin role assignment should be group-based:

| Group | Realm roles | Responsibility |
| --- | --- | --- |
| `operators` | `ROLE_OPERATOR` | Default admin console access and read-only operational work |
| `loan-reviewers` | `ROLE_OPERATOR`, `ROLE_LOAN_REVIEWER` | Loan application review and underwriting workflow access |
| `managers` | `ROLE_OPERATOR`, `ROLE_MANAGER` | Manager approval and elevated operational decisions |
| `batch-admins` | `ROLE_OPERATOR`, `ROLE_BATCH_ADMIN` | Batch job operation and administrative maintenance |

`credito-admin-console` includes an `admin-realm-roles` mapper. Admin access tokens include realm roles in a top-level `roles` claim as a string array so Spring services can map them to authorities without depending on Keycloak-specific nested claims. Keycloak's standard `realm_access.roles` claim is still available through the default `roles` client scope.
