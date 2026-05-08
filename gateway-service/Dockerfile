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

RUN groupadd --system credito && \
    useradd --system --gid credito --create-home --home-dir /app credito && \
    chown -R credito:credito /app

COPY --from=build --chown=credito:credito /workspace/gateway-service/build/libs/*.jar app.jar

EXPOSE 8080 9080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/${MANAGEMENT_SERVER_PORT} && printf 'GET /actuator/health HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n' >&3 && grep '200 OK' <&3 >/dev/null"]

USER credito

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
