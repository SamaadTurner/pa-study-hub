package com.pastudyhub.user.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI configuration.
 * Swagger UI available at: http://localhost:8084/swagger-ui.html
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "PA Study Hub — User Service API",
        version = "1.0.0",
        description = "User registration, authentication (JWT), and profile management for PA Study Hub.",
        contact = @Contact(name = "Samaad Turner", url = "https://github.com/SamaadTurner/pa-study-hub")
    ),
    servers = {
        @Server(url = "http://localhost:8084", description = "Local User Service"),
        @Server(url = "http://localhost:8080", description = "Local via API Gateway")
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
