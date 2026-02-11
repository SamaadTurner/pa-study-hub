# ---- Build stage ----
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copy Gradle wrapper and build files first (layer caching)
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle.kts build.gradle.kts
COPY settings.gradle.kts settings.gradle.kts

# Copy only the flashcard service source
COPY services/flashcard-service/build.gradle.kts services/flashcard-service/build.gradle.kts
COPY services/flashcard-service/src services/flashcard-service/src

# Make wrapper executable and build (skip tests — CI runs them separately)
RUN chmod +x gradlew && ./gradlew :services:flashcard-service:bootJar -x test --no-daemon

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/services/flashcard-service/build/libs/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseG1GC", \
  "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:docker}", \
  "-jar", "app.jar"]
