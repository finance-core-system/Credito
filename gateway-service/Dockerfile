FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle ./
COPY gradle gradle
COPY common common
COPY gateway-service gateway-service

RUN chmod +x gradlew
RUN ./gradlew :gateway-service:bootJar --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /workspace/gateway-service/build/libs/*.jar app.jar

EXPOSE 8080 9080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
