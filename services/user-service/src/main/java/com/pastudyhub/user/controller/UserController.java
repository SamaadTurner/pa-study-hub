package com.pastudyhub.user.controller;

import com.pastudyhub.user.dto.ChangePasswordRequest;
import com.pastudyhub.user.dto.UpdateProfileRequest;
import com.pastudyhub.user.dto.UserResponse;
import com.pastudyhub.user.security.JwtTokenProvider;
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
@Tag(name = "User Profile", description = "View and update user profile, change password, deactivate account")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

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
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Change password")
    public void changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        UUID userId = extractUserId(authentication);
        userService.changePassword(userId, request);
    }

    /**
     * Soft-delete the current user's account.
     * The user will no longer be able to log in.
     */
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate account (soft delete)")
    public void deactivateAccount(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        userService.deactivateAccount(userId);
    }

    /**
     * Extracts the user's UUID from the JWT claims stored in the Authentication principal.
     * The principal is set by JwtAuthenticationFilter as the userId string.
     */
    private UUID extractUserId(Authentication authentication) {
        // Principal is the userId string set in JwtAuthenticationFilter
        return UUID.fromString((String) authentication.getPrincipal());
    }
}
