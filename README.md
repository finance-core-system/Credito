# credito

Java 17, Spring Boot 3.5.x 기반 멀티모듈 프로젝트입니다.

## Modules

- `common`: 서비스 간 공유 코드
- `customer-service`: 고객 서비스
- `account-service`: 계좌 서비스
- `lending-service`: 대출 서비스
- `batch-service`: 배치 서비스

## Commands

```bash
./gradlew clean build
./gradlew :customer-service:bootRun
./gradlew :account-service:bootRun
./gradlew :lending-service:bootRun
./gradlew :batch-service:bootRun
```

## Runtime Ports

- `customer-service`: `8081`
- `account-service`: `8082`
- `lending-service`: `8083`
- `batch-service`: `8084`
