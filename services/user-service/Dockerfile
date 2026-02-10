# PA Study Hub — User Service Dockerfile
# Multi-stage build: reduces final image size by ~60% (JDK → JRE)

# ==========================================
# Stage 1: Build
# ==========================================
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

# Copy Gradle wrapper and build files first (leverages Docker layer caching)
# Dependencies are downloaded before source code is copied, so they're cached
# unless build files change.
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

RUN chmod +x gradlew

# Download dependencies (cached layer — only invalidated when build files change)
RUN ./gradlew :services:user-service:dependencies --no-daemon 2>/dev/null || true

# Copy source code
COPY services/user-service/src ./services/user-service/src
COPY services/user-service/build.gradle.kts ./services/user-service/

# Build the fat JAR (skip tests — tests run in CI, not in Docker image build)
RUN ./gradlew :services:user-service:bootJar --no-daemon -x test

# ==========================================
# Stage 2: Runtime (smaller JRE-only image)
# ==========================================
FROM eclipse-temurin:17-jre-alpine

# Create non-root user for security — running as root in containers is a security risk
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy only the built JAR from Stage 1
COPY --from=build /app/services/user-service/build/libs/user-service.jar app.jar

# Switch to non-root user
USER appuser

EXPOSE 8084

# Docker healthcheck — used by docker-compose depends_on and load balancers
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8084/actuator/health || exit 1

# JVM flags: G1GC for containerized workloads, container memory awareness
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
