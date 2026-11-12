package com.pastudyhub.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI configuration for PA Study Hub monolith.
 * Swagger UI available at: http://localhost:8080/swagger-ui.html
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "PA Study Hub API",
        version = "1.0.0",
        description = "Unified API for PA Study Hub: flashcards, exams, progress tracking, and user management.",
        contact = @Contact(name = "Samaad Turner", url = "https://github.com/SamaadTurner/pa-study-hub")
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Local Development"),
        @Server(url = "https://pa-study-hub.onrender.com", description = "Production")
    }
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer"
)
public class OpenApiConfig {
    // Configuration via annotations — no additional beans needed
}
