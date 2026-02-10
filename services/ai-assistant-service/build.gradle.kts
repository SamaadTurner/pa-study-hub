plugins {
    // Inherited from root: java, spring boot, dependency-management, jacoco
}

description = "PA Study Hub — AI Assistant Service: Claude API integration for study AI features"

dependencies {
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    implementation("org.springframework.boot:spring-boot-starter-security")

    // Anthropic Java SDK for Claude API calls
    // Official SDK: https://github.com/anthropics/anthropic-sdk-java
    implementation("com.anthropic:anthropic-sdk-java:1.+")

    // WebClient for calling study-progress-service to get analytics context
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Jackson for JSON parsing of Claude's structured responses
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    testImplementation("org.testcontainers:junit-jupiter:1.19.4")
    testImplementation("org.testcontainers:postgresql:1.19.4")
    testImplementation("org.wiremock:wiremock-standalone:3.3.1")
}

springBoot {
    mainClass.set("com.pastudyhub.ai.AiAssistantServiceApplication")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("ai-assistant-service.jar")
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    violationRules {
        rule {
            excludes = listOf(
                "com.pastudyhub.ai.model.*",
                "com.pastudyhub.ai.dto.*",
                "com.pastudyhub.ai.AiAssistantServiceApplication"
            )
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}
