# ============================================================
# PA Study Hub - Monolith Dockerfile
# Multi-stage build for Spring Boot application
# ============================================================

# Stage 1: Build the application
FROM gradle:8.5-jdk17-alpine AS builder

WORKDIR /build

# Copy gradle configuration
COPY build.gradle.kts settings.gradle.kts lombok.config gradlew ./
COPY gradle ./gradle

# Copy source code
COPY app ./app

# Build the application (skip tests for Docker build)
RUN ./gradlew :app:build -x test --no-daemon

# Unpack the jar so the app runs from a plain classpath. Class data sharing (below)
# can only store classes from plain jars, not from the jars nested inside a Spring
# Boot jar or from a folder, so the app's own classes go into application.jar.
RUN mkdir /build/unpacked && cd /build/unpacked \
    && jar xf /build/app/build/libs/pa-study-hub.jar \
    && jar cf application.jar -C BOOT-INF/classes .

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# The app's classes and its libraries, unpacked
COPY --from=builder /build/unpacked/application.jar application.jar
COPY --from=builder /build/unpacked/BOOT-INF/lib lib

# Class data sharing: start the app once while building the image, record every
# class it loads into app.jsa, and let each real start map that file instead of
# loading and checking thousands of classes again. On Render's free plan every
# wake-up is a full start on a small share of one CPU, so this is what visitors
# wait for. Measured locally on one core: ready in 3.5 s instead of 6.2 s.
#
# The training run stops as soon as the app has started (spring.context.exit).
# There is no database while the image is built, so it runs without Flyway and
# tells Hibernate not to ask the database anything. Same JVM flags as on Render
# (JAVA_TOOL_OPTIONS in render.yaml), so the archive fits the real start. If it
# ever does not fit, the JVM just starts without it.
RUN JWT_SECRET=image-build-training-run-only-not-a-real-secret java \
    -XX:ArchiveClassesAtExit=app.jsa \
    -XX:+UseSerialGC -XX:TieredStopAtLevel=1 \
    -Duser.timezone=UTC \
    -Dspring.context.exit=onRefresh \
    -Dspring.flyway.enabled=false \
    -Dspring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false \
    -Dspring.datasource.url=jdbc:postgresql://localhost:1/none \
    -Dspring.datasource.hikari.initialization-fail-timeout=-1 \
    -Dspring.datasource.hikari.connection-timeout=250 \
    -cp "application.jar:lib/*" com.pastudyhub.PaStudyHubApplication

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application from the unpacked classpath, with the class archive
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:SharedArchiveFile=app.jsa", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Duser.timezone=UTC", \
    "-cp", "application.jar:lib/*", \
    "com.pastudyhub.PaStudyHubApplication"]
