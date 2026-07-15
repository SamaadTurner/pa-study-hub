package com.pastudyhub.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PA Study Hub — User Service
 *
 * <p>Responsible for user registration, login, JWT issuance, token refresh/rotation,
 * and user profile management. This service is the authentication authority —
 * all JWTs are signed here with the shared HS256 secret.
 *
 * <p>Port: 8084
 */
@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
