# ---- Build stage ----
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle.kts build.gradle.kts
COPY settings.gradle.kts settings.gradle.kts

COPY services/study-progress-service/build.gradle.kts services/study-progress-service/build.gradle.kts
COPY services/study-progress-service/src services/study-progress-service/src

RUN chmod +x gradlew && ./gradlew :services:study-progress-service:bootJar -x test --no-daemon

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/services/study-progress-service/build/libs/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseG1GC", \
  "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:docker}", \
  "-jar", "app.jar"]
