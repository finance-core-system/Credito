FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle ./
COPY gradle gradle
COPY common common
COPY account-service account-service

RUN chmod +x gradlew && \
    ./gradlew :account-service:bootJar --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN groupadd --system credito && useradd --system --gid credito --create-home --home-dir /app credito

COPY --from=build --chown=credito:credito /workspace/account-service/build/libs/*.jar app.jar

EXPOSE 8082

USER credito

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
