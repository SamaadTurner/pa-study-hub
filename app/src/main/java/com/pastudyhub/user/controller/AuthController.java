package com.pastudyhub.user.controller;

import com.pastudyhub.user.dto.*;
import com.pastudyhub.user.security.RefreshTokenCookie;
import com.pastudyhub.user.service.AccountEmailService;
import com.pastudyhub.user.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private final RefreshTokenCookie refreshTokenCookie;
    private final AccountEmailService accountEmailService;

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
        AuthResponse response = authenticationService.register(request);
        // After the account is saved, so a failed email can't undo the sign-up
        accountEmailService.sendConfirmationAfterSignUp(response.getUser().getId());
        return response;
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
    public AuthResponse refresh(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String cookieToken,
            @RequestBody(required = false) RefreshTokenRequest body) {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(tokenFrom(cookieToken, body));
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
    @Operation(
        summary = "Logout — revoke refresh token",
        responses = {
            @ApiResponse(responseCode = "204", description = "Logged out successfully")
        }
    )
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String cookieToken,
            @RequestBody(required = false) RefreshTokenRequest body) {
        String token = tokenFrom(cookieToken, body);
        if (token != null) {
            authenticationService.logout(token);
        }
        // The cookie goes either way, so a sign-out always signs out this browser
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.clear().toString())
                .build();
    }

    // The cookie, or for a tab opened before the cookie existed, the token in the body
    private static String tokenFrom(String cookieToken, RefreshTokenRequest body) {
        if (cookieToken != null && !cookieToken.isBlank()) {
            return cookieToken;
        }
        String fromBody = body == null ? null : body.getRefreshToken();
        return fromBody == null || fromBody.isBlank() ? null : fromBody;
    }

    @GetMapping("/google/status")
    @Operation(summary = "Whether Google sign-in is on, with the client ID for the button")
    public GoogleStatusResponse googleStatus() {
        return authenticationService.googleStatus();
    }

    @PostMapping("/google")
    @Operation(
        summary = "Sign in with a Google ID token (makes an account the first time)",
        responses = {
            @ApiResponse(responseCode = "200", description = "Signed in"),
            @ApiResponse(responseCode = "401", description = "The Google token is invalid or the email is not verified"),
            @ApiResponse(responseCode = "409", description = "A password account has this email; link it first")
        }
    )
    public AuthResponse signInWithGoogle(@Valid @RequestBody GoogleSignInRequest request) {
        return authenticationService.signInWithGoogle(request);
    }

    @PostMapping("/google/link")
    @Operation(summary = "Add Google sign-in to an existing account, proven with its password")
    public AuthResponse linkGoogle(@Valid @RequestBody GoogleLinkRequest request) {
        return authenticationService.linkGoogle(request);
    }

    @PostMapping("/password-reset/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Forgot password: email a reset link (same answer whether or not the email has an account)")
    public MessageResponse requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        accountEmailService.requestPasswordReset(request.getEmail());
        return new MessageResponse("If an account uses that email, a reset link is on its way. "
                + "It works for one hour. Check your spam folder too.");
    }

    @PostMapping("/password-reset/confirm")
    @Operation(summary = "Set a new password with the token from a reset link, and sign in")
    public AuthResponse resetPassword(@Valid @RequestBody NewPasswordRequest request) {
        return authenticationService.resetPassword(request);
    }

    @PostMapping("/confirm-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Confirm the email with the token from the link sent after signing up")
    public void confirmEmail(@Valid @RequestBody EmailLinkRequest request) {
        authenticationService.confirmEmail(request);
    }
}
