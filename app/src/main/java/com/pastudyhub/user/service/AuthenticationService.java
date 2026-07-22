package com.pastudyhub.user.service;

import com.pastudyhub.user.dto.*;

/**
 * Authentication service interface — dependency inversion principle.
 *
 * <p>All auth flows go through this interface. The implementation
 * ({@link AuthenticationServiceImpl}) is the only concrete class, but
 * the interface makes it easy to mock in tests.
 */
public interface AuthenticationService {

    /**
     * Register a new user account.
     *
     * @param request registration data with email, password, name
     * @return JWT access token + refresh token + user profile
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticate with email and password.
     *
     * @param request login credentials
     * @return JWT access token + refresh token + user profile
     */
    AuthResponse login(LoginRequest request);

    /**
     * Exchange a valid refresh token for a new access token.
     * Rotates the refresh token (revokes old, issues new).
     *
     * @param request containing the refresh token string
     * @return new JWT access token + new refresh token + user profile
     */
    AuthResponse refresh(RefreshTokenRequest request);

    /**
     * Revoke a refresh token (logout).
     *
     * @param refreshToken the refresh token to revoke
     */
    void logout(String refreshToken);
}
