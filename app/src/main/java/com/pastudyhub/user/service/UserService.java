package com.pastudyhub.user.service;

import com.pastudyhub.user.dto.ChangePasswordRequest;
import com.pastudyhub.user.dto.UpdateProfileRequest;
import com.pastudyhub.user.dto.UserResponse;

import java.util.UUID;

/**
 * User profile management interface.
 */
public interface UserService {

    /**
     * Get a user's profile by their UUID.
     *
     * @param userId the user's UUID
     * @return UserResponse DTO
     */
    UserResponse getProfile(UUID userId);

    /**
     * Update a user's profile fields (partial update — null fields are ignored).
     *
     * @param userId  the user's UUID
     * @param request fields to update
     * @return updated UserResponse DTO
     */
    UserResponse updateProfile(UUID userId, UpdateProfileRequest request);

    /**
     * Change a user's password. Validates current password before updating.
     *
     * @param userId  the user's UUID
     * @param request current and new password
     */
    void changePassword(UUID userId, ChangePasswordRequest request);

    /**
     * Soft-delete a user account (sets isActive = false).
     * The user can no longer log in after deactivation.
     *
     * @param userId the user's UUID
     */
    void deactivateAccount(UUID userId);
}
