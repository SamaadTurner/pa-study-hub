plugins {
    // Inherited from root: java, spring boot, dependency-management, jacoco
}

description = "PA Study Hub — Exam Service: practice exams, questions, scoring, PANCE prep"

dependencies {
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    implementation("org.springframework.boot:spring-boot-starter-security")

    // WebClient for calling study-progress-service
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Jackson for reading seed JSON
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    testImplementation("org.testcontainers:junit-jupiter:1.19.4")
    testImplementation("org.testcontainers:postgresql:1.19.4")
    testImplementation("org.wiremock:wiremock-standalone:3.3.1")
}

springBoot {
    mainClass.set("com.pastudyhub.exam.ExamServiceApplication")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("exam-service.jar")
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    violationRules {
        rule {
            excludes = listOf(
                "com.pastudyhub.exam.model.*",
                "com.pastudyhub.exam.dto.*",
                "com.pastudyhub.exam.ExamServiceApplication",
                "com.pastudyhub.exam.seed.*"
            )
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}
