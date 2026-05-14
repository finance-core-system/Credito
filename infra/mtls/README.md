# Dev mTLS Certificates

이 폴더는 dev/compose 환경에서 사용할 자체 CA와 서비스별 인증서를 생성하는 스크립트를 보관한다.

## Generate

```bash
./infra/mtls/generate-dev-certs.sh
```

기본 출력 위치:

```text
infra/mtls/generated
```

기본 store password:

```text
changeit
```

환경변수로 바꿀 수 있다.

```bash
MTLS_STORE_PASSWORD=local-secret ./infra/mtls/generate-dev-certs.sh
```

## Output

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

`generated/` 아래 산출물은 Git에 커밋하지 않는다.
