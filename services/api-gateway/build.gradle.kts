plugins {
    id("java")
    id("org.springframework.boot") version "3.2.2"
    id("io.spring.dependency-management") version "1.1.4"
    id("jacoco")
}

// The API Gateway uses Spring Cloud Gateway (reactive, WebFlux-based)
// It does NOT use spring-boot-starter-web — those two conflict.
// Exclude the standard web dependencies added by the root build.

description = "PA Study Hub — API Gateway: single entry point, JWT validation, routing, rate limiting"

group = "com.pastudyhub"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

// Spring Cloud BOM for matching gateway version
dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.0")
    }
}

dependencies {
    // Spring Cloud Gateway (reactive — built on WebFlux)
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")

    // Actuator for health checks
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // JWT validation
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // Rate limiting uses the built-in RequestRateLimiter with Redis,
    // but for simplicity we use an in-memory implementation
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
}

springBoot {
    mainClass.set("com.pastudyhub.gateway.ApiGatewayApplication")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("api-gateway.jar")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
