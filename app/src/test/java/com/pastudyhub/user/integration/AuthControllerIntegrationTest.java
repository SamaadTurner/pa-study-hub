package com.pastudyhub.user.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pastudyhub.BaseIntegrationTest;
import com.pastudyhub.user.dto.LoginRequest;
import com.pastudyhub.user.dto.RegisterRequest;
import com.pastudyhub.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController.
 */
@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.pastudyhub.user.repository.RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        // Clean database before each test for isolation
        // Delete refresh tokens first due to foreign key constraint
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== REGISTER TESTS ====================

    @Test
    @DisplayName("POST /register with valid data should return 201 with tokens")
    void register_validRequest_shouldReturn201WithTokens() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("jocelyn@test.com");
        request.setPassword("PAStudent2026!");
        request.setFirstName("Jocelyn");
        request.setLastName("Turner");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("jocelyn@test.com"))
                .andExpect(jsonPath("$.user.firstName").value("Jocelyn"))
                .andExpect(jsonPath("$.user.role").value("STUDENT"))
                // SECURITY: password hash must NEVER appear in the response
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("POST /register with duplicate email should return 409 Conflict")
    void register_duplicateEmail_shouldReturn409() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("duplicate@test.com");
        request.setPassword("PAStudent2026!");
        request.setFirstName("First");
        request.setLastName("User");

        // Register first time
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Try to register again with same email
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                // SECURITY: stack trace must NOT appear
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("POST /register with invalid email should return 400 with field errors")
    void register_invalidEmail_shouldReturn400WithFieldErrors() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("not-an-email");
        request.setPassword("PAStudent2026!");
        request.setFirstName("Jocelyn");
        request.setLastName("Turner");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    @DisplayName("POST /register with missing required fields should return 400")
    void register_missingRequiredFields_shouldReturn400() throws Exception {
        // Empty request
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    @DisplayName("POST /register with weak password should return 400")
    void register_weakPassword_shouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("jocelyn@test.com");
        request.setPassword("weakpassword"); // no uppercase, digit, or special char
        request.setFirstName("Jocelyn");
        request.setLastName("Turner");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @DisplayName("POST /login with valid credentials should return 200 with tokens")
    void login_validCredentials_shouldReturn200WithTokens() throws Exception {
        // First register
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("logintest@test.com");
        registerRequest.setPassword("PAStudent2026!");
        registerRequest.setFirstName("Login");
        registerRequest.setLastName("Test");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Then login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("logintest@test.com");
        loginRequest.setPassword("PAStudent2026!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("logintest@test.com"));
    }

    @Test
    @DisplayName("POST /login with wrong password should return 401")
    void login_wrongPassword_shouldReturn401() throws Exception {
        // Register first
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("wrongpass@test.com");
        registerRequest.setPassword("PAStudent2026!");
        registerRequest.setFirstName("Test");
        registerRequest.setLastName("User");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Try wrong password
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("wrongpass@test.com");
        loginRequest.setPassword("WrongPassword123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                // SECURITY: error message should not hint whether email or password was wrong
                .andExpect(jsonPath("$.detail").value(containsString("Invalid email or password")));
    }

    @Test
    @DisplayName("POST /login with non-existent email should return 401 (not 404)")
    void login_nonExistentEmail_shouldReturn401NotFound() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("doesnotexist@test.com");
        loginRequest.setPassword("SomePassword123!");

        // SECURITY: must return 401, not 404 — prevents user enumeration
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    // Tests for endpoints that need a token are in ProtectedEndpointsIntegrationTest
}
