package com.pastudyhub.user.controller;

import com.pastudyhub.user.dto.*;
import com.pastudyhub.user.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints.
 *
 * <p>All endpoints under /api/v1/auth/** are PUBLIC — no JWT required.
 * The API Gateway routes these without running the JWT validation filter.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, refresh tokens, and logout")
public class AuthController {

    private final AuthenticationService authenticationService;

    /**
     * Register a new PA school student account.
     *
     * @param request user registration data
     * @return 201 Created with JWT access token + refresh token + user profile
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Register a new user account",
        description = "Creates a new STUDENT account. Returns JWT access token (15 min) and refresh token (7 days).",
        responses = {
            @ApiResponse(responseCode = "201", description = "Account created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or weak password"),
            @ApiResponse(responseCode = "409", description = "Email already registered")
        }
    )
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authenticationService.register(request);
    }

    /**
     * Authenticate with email and password.
     *
     * @param request login credentials
     * @return 200 OK with JWT access token + refresh token + user profile
     */
    @PostMapping("/login")
    @Operation(
        summary = "Login with email and password",
        responses = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid email or password"),
            @ApiResponse(responseCode = "403", description = "Account is deactivated")
        }
    )
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authenticationService.login(request);
    }

    /**
     * Exchange a valid refresh token for a new access token.
     * Rotates the refresh token — the old one is immediately invalidated.
     *
     * @param request containing the refresh token
     * @return 200 OK with new access token + new refresh token
     */
    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh access token",
        description = "Exchanges a refresh token for a new access token. The refresh token is rotated (old one revoked).",
        responses = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Refresh token is invalid, expired, or revoked")
        }
    )
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authenticationService.refresh(request);
    }

    /**
     * Logout by revoking the refresh token.
     * The client should also discard the access token (it will expire naturally).
     *
     * @param request containing the refresh token to revoke
     * @return 204 No Content
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Logout — revoke refresh token",
        responses = {
            @ApiResponse(responseCode = "204", description = "Logged out successfully")
        }
    )
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        authenticationService.logout(request.getRefreshToken());
    }
}
