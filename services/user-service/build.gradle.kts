plugins {
    // Inherited from root: java, spring boot, dependency-management, jacoco
}

description = "PA Study Hub — User Service: authentication, JWT, user profiles"

dependencies {
    // JWT — jjwt for HS256 token signing and validation
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // Spring Security (web security, BCrypt password hashing)
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Testcontainers for real PostgreSQL in integration tests
    testImplementation("org.testcontainers:junit-jupiter:1.19.4")
    testImplementation("org.testcontainers:postgresql:1.19.4")

    // WireMock for mocking downstream services in integration tests
    testImplementation("org.wiremock:wiremock-standalone:3.3.1")
}

springBoot {
    mainClass.set("com.pastudyhub.user.UserServiceApplication")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("user-service.jar")
}

// Exclude auto-generated classes from JaCoCo coverage
tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    violationRules {
        rule {
            excludes = listOf(
                "com.pastudyhub.user.model.*",
                "com.pastudyhub.user.dto.*",
                "com.pastudyhub.user.UserServiceApplication"
            )
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}
