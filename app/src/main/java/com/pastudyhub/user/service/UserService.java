package com.pastudyhub.user.service;

import com.pastudyhub.user.dto.ChangePasswordRequest;
import com.pastudyhub.user.dto.DeleteAccountRequest;
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
     * Change a user's password, or set the first one for an account made with Google.
     * The current password is checked when the account has one. Signs out every device.
     *
     * @param userId  the user's UUID
     * @param request current and new password
     */
    void changePassword(UUID userId, ChangePasswordRequest request);

    /** Adds Google sign-in to the logged-in user's account. */
    UserResponse linkGoogle(UUID userId, String credential);

    /** Removes Google sign-in, only if the account has a password to sign in with. */
    UserResponse unlinkGoogle(UUID userId);

    /** Deletes the account and all of its data, after checking the confirmation. */
    void deleteAccount(UUID userId, DeleteAccountRequest request);
}
