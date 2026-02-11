package com.pastudyhub.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Programmatic route definitions for all five downstream microservices.
 *
 * Routes are also defined in application.yml — this class demonstrates
 * the Java DSL approach and overrides any YAML duplicates at runtime.
 */
@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()

            // ---- User Service (port 8080) ----------------------------------
            .route("user-service", r -> r
                .path("/api/v1/auth/**", "/api/v1/users/**")
                .filters(f -> f.stripPrefix(0))
                .uri("${services.user-service.url:http://user-service:8080}"))

            // ---- Flashcard Service (port 8082) -----------------------------
            .route("flashcard-service", r -> r
                .path("/api/v1/decks/**", "/api/v1/cards/**", "/api/v1/review/**")
                .filters(f -> f.stripPrefix(0))
                .uri("${services.flashcard-service.url:http://flashcard-service:8082}"))

            // ---- Exam Service (port 8083) ----------------------------------
            .route("exam-service", r -> r
                .path("/api/v1/exams/**", "/api/v1/questions/**")
                .filters(f -> f.stripPrefix(0))
                .uri("${services.exam-service.url:http://exam-service:8083}"))

            // ---- Study Progress Service (port 8084 — internal use also) ----
            .route("progress-service", r -> r
                .path("/api/v1/progress/**")
                .filters(f -> f.stripPrefix(0))
                .uri("${services.progress-service.url:http://study-progress-service:8083}"))

            // ---- AI Assistant Service (port 8085) --------------------------
            .route("ai-assistant-service", r -> r
                .path("/api/v1/ai/**")
                .filters(f -> f.stripPrefix(0))
                .uri("${services.ai-assistant-service.url:http://ai-assistant-service:8084}"))

            .build();
    }
}
