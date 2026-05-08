FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle ./
COPY gradle gradle
COPY common common
COPY gateway-service gateway-service

RUN chmod +x gradlew && \
    ./gradlew :gateway-service:bootJar --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN groupadd --system credito && useradd --system --gid credito --create-home --home-dir /app credito

COPY --from=build --chown=credito:credito /workspace/gateway-service/build/libs/*.jar app.jar

EXPOSE 8080 9080

USER credito

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
