plugins {
    // Inherited from root: java, spring boot, dependency-management, jacoco
}

description = "PA Study Hub — Flashcard Service: decks, cards, SM-2 spaced repetition"

dependencies {
    // JWT for validating tokens forwarded from gateway
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // Spring Security
    implementation("org.springframework.boot:spring-boot-starter-security")

    // WebClient for calling study-progress-service
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Testcontainers for PostgreSQL integration tests
    testImplementation("org.testcontainers:junit-jupiter:1.19.4")
    testImplementation("org.testcontainers:postgresql:1.19.4")

    // WireMock to mock study-progress-service calls
    testImplementation("org.wiremock:wiremock-standalone:3.3.1")
}

springBoot {
    mainClass.set("com.pastudyhub.flashcard.FlashcardServiceApplication")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("flashcard-service.jar")
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    violationRules {
        rule {
            excludes = listOf(
                "com.pastudyhub.flashcard.model.*",
                "com.pastudyhub.flashcard.dto.*",
                "com.pastudyhub.flashcard.FlashcardServiceApplication",
                "com.pastudyhub.flashcard.seed.*"
            )
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}
