plugins {
    java
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.7"
    id("jacoco")
}

group = "com.pastudyhub"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    // Checks Google ID tokens ("Continue with Google") against Google's public keys
    implementation("org.springframework.security:spring-security-oauth2-jose")

    // Database
    implementation("org.flywaydb:flyway-core")
    // Flyway 10 and later keep each database's support in its own module
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // JWT for authentication
    implementation("io.jsonwebtoken:jjwt-api:0.12.7")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.7")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.7")

    // OpenAPI / Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.17")

    // Jackson for JSON
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.h2database:h2")
}

// The scheduler evaluation (session 20) is a command-line tool, not part of the app,
// so it has its own source folder (src/evaluation) and is never in the app's jar.
sourceSets {
    create("evaluation") {
        compileClasspath += sourceSets.main.get().output + configurations.compileClasspath.get()
        runtimeClasspath += sourceSets.main.get().output + configurations.runtimeClasspath.get()
    }
    test {
        // The unit tests for the evaluation live with the other tests
        compileClasspath += sourceSets["evaluation"].output
        runtimeClasspath += sourceSets["evaluation"].output
    }
}

// ./gradlew :app:evaluateSchedulers --args="path/to/reviews.csv"
tasks.register<JavaExec>("evaluateSchedulers") {
    description = "Scores SM-2 and FSRS on a file of review histories"
    group = "verification"
    classpath = sourceSets["evaluation"].runtimeClasspath
    mainClass.set("com.pastudyhub.evaluation.SchedulerEvaluation")
}

tasks.withType<Test> {
    useJUnitPlatform()
    // Same clock as the server (UTC), whatever the machine's own time zone is
    systemProperty("user.timezone", "UTC")
    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

// The build fails if coverage drops below these numbers. They sit a little under
// what the tests measured on 2026-11-14 (96.6% of lines, 80.5% of branches), so a
// small change does not break the build but a real drop does.
//
// Code that Lombok generates (getters, builders, equals) is not counted. See
// lombok.config in the root folder.
tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.named("test"))
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.95".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.78".toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

springBoot {
    mainClass.set("com.pastudyhub.PaStudyHubApplication")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("pa-study-hub.jar")
}
