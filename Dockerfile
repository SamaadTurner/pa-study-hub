# ============================================================
# PA Study Hub - Monolith Dockerfile
# Multi-stage build for Spring Boot application
# ============================================================

# Stage 1: Build the application
FROM gradle:8.5-jdk17-alpine AS builder

WORKDIR /build

# Copy gradle configuration
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle ./gradle

# Copy source code
COPY app ./app

# Build the application (skip tests for Docker build)
RUN ./gradlew :app:build -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy built JAR from builder stage
COPY --from=builder /build/app/build/libs/*.jar app.jar

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]
