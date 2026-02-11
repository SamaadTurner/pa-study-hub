# ---- Build stage -----------------------------------------------------------
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY settings.gradle.kts .
COPY services/api-gateway/build.gradle.kts services/api-gateway/

RUN ./gradlew :services:api-gateway:dependencies --no-daemon -q || true

COPY services/api-gateway/src services/api-gateway/src
RUN ./gradlew :services:api-gateway:bootJar --no-daemon -x test

# ---- Runtime stage ---------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app

COPY --from=build /workspace/services/api-gateway/build/libs/api-gateway.jar app.jar

USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
