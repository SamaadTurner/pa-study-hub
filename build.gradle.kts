// Root build file — shared configuration applied to all subprojects

plugins {
    id("java") apply false
    id("org.springframework.boot") version "3.2.2" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
    id("jacoco")
}

// Versions catalog
object Versions {
    const val springBoot = "3.2.2"
    const val springCloud = "2023.0.0"
    const val jjwt = "0.12.3"
    const val testcontainers = "1.19.4"
    const val openApi = "2.3.0"
    const val anthropic = "1.+"
    const val mapstruct = "1.5.5.Final"
    const val wiremock = "3.3.1"
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "jacoco")

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
        // Spring Boot starters — common across all services
        "implementation"("org.springframework.boot:spring-boot-starter-web")
        "implementation"("org.springframework.boot:spring-boot-starter-data-jpa")
        "implementation"("org.springframework.boot:spring-boot-starter-validation")
        "implementation"("org.springframework.boot:spring-boot-starter-actuator")
        "implementation"("org.springframework.boot:spring-boot-starter-security")

        // OpenAPI / Swagger
        "implementation"("org.springdoc:springdoc-openapi-starter-webmvc-ui:${Versions.openApi}")

        // PostgreSQL driver
        "runtimeOnly"("org.postgresql:postgresql")

        // Lombok
        "compileOnly"("org.projectlombok:lombok")
        "annotationProcessor"("org.projectlombok:lombok")
        "testCompileOnly"("org.projectlombok:lombok")
        "testAnnotationProcessor"("org.projectlombok:lombok")

        // Testing
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testImplementation"("org.springframework.security:spring-security-test")
        "testImplementation"("org.testcontainers:junit-jupiter:${Versions.testcontainers}")
        "testImplementation"("org.testcontainers:postgresql:${Versions.testcontainers}")
        "testImplementation"("org.testcontainers:testcontainers:${Versions.testcontainers}")
    }

    configurations.all {
        resolutionStrategy {
            // Force consistent versions across all subprojects
            force("org.yaml:snakeyaml:2.2")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        finalizedBy(tasks.named("jacocoTestReport"))
    }

    tasks.named<JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.named("test"))
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    // Enforce minimum 80% line coverage
    tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
        violationRules {
            rule {
                limit {
                    counter = "LINE"
                    value = "COVEREDRATIO"
                    minimum = "0.80".toBigDecimal()
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
}
