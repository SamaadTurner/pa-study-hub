plugins {
    // Inherited from root: java, spring boot, dependency-management, jacoco
}

description = "PA Study Hub — Study Progress Service: streaks, goals, activity logging, analytics"

dependencies {
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    implementation("org.springframework.boot:spring-boot-starter-security")

    testImplementation("org.testcontainers:junit-jupiter:1.19.4")
    testImplementation("org.testcontainers:postgresql:1.19.4")
    testImplementation("org.wiremock:wiremock-standalone:3.3.1")
}

springBoot {
    mainClass.set("com.pastudyhub.progress.StudyProgressServiceApplication")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("study-progress-service.jar")
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    violationRules {
        rule {
            excludes = listOf(
                "com.pastudyhub.progress.model.*",
                "com.pastudyhub.progress.dto.*",
                "com.pastudyhub.progress.StudyProgressServiceApplication"
            )
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}
