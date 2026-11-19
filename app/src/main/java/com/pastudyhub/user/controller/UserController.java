package com.pastudyhub.user.controller;

import com.pastudyhub.user.dto.AuthResponse;
import com.pastudyhub.user.dto.ChangePasswordRequest;
import com.pastudyhub.user.dto.DeleteAccountRequest;
import com.pastudyhub.user.dto.GoogleSignInRequest;
import com.pastudyhub.user.dto.UpdateProfileRequest;
import com.pastudyhub.user.dto.UserResponse;
import com.pastudyhub.user.security.JwtTokenProvider;
import com.pastudyhub.user.service.AuthenticationService;
import com.pastudyhub.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for user profile management.
 *
 * <p>All endpoints require a valid JWT in the Authorization: Bearer header.
 * The userId is extracted from the JWT claims set by JwtAuthenticationFilter.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "View and update your profile, password, Google sign-in, and close your account")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final AuthenticationService authenticationService;

    /**
     * Get the current user's profile (derived from JWT).
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user's profile")
    public UserResponse getMyProfile(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        return userService.getProfile(userId);
    }

    /**
     * Update the current user's profile fields.
     * Only non-null fields in the request body are updated.
     */
    @PutMapping("/me")
    @Operation(summary = "Update user profile")
    public UserResponse updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = extractUserId(authentication);
        return userService.updateProfile(userId, request);
    }

    /**
     * Change the current user's password.
     * Requires the current password for verification.
     */
    @PutMapping("/me/password")
    @Operation(summary = "Change the password, or set the first one for an account made with Google",
            description = "Signs out every device and returns new tokens for this one.")
    public AuthResponse changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        UUID userId = extractUserId(authentication);
        userService.changePassword(userId, request);
        return authenticationService.issueTokensFor(userId);
    }

    @PostMapping("/me/google")
    @Operation(summary = "Add Google sign-in to your account")
    public UserResponse linkGoogle(
            Authentication authentication,
            @Valid @RequestBody GoogleSignInRequest request) {
        return userService.linkGoogle(extractUserId(authentication), request.getCredential());
    }

    @DeleteMapping("/me/google")
    @Operation(summary = "Remove Google sign-in (only when the account has a password)")
    public UserResponse unlinkGoogle(Authentication authentication) {
        return userService.unlinkGoogle(extractUserId(authentication));
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Close the account and delete all of its data",
            description = "Needs the password, or the email typed out for an account without a password.")
    public void deleteAccount(
            Authentication authentication,
            @RequestBody DeleteAccountRequest request) {
        userService.deleteAccount(extractUserId(authentication), request);
    }

    private UUID extractUserId(Authentication authentication) {
        // Principal is the userId string set in JwtAuthenticationFilter
        return UUID.fromString((String) authentication.getPrincipal());
    }
}
