# ---- Build stage -----------------------------------------------------------
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY settings.gradle.kts .
COPY build.gradle.kts .
COPY services/ai-assistant-service/build.gradle.kts services/ai-assistant-service/

RUN ./gradlew :services:ai-assistant-service:dependencies --no-daemon -q || true

COPY services/ai-assistant-service/src services/ai-assistant-service/src
RUN ./gradlew :services:ai-assistant-service:bootJar --no-daemon -x test

# ---- Runtime stage ---------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app

COPY --from=build /workspace/services/ai-assistant-service/build/libs/ai-assistant-service.jar app.jar

USER appuser
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=docker", "app.jar"]
