package com.pastudyhub.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response body for login, register, and refresh token endpoints. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /** Short-lived JWT access token (15 minutes). Include in Authorization: Bearer <token> header. */
    private String accessToken;

    /** Long-lived refresh token (7 days). Store securely, use to obtain new access tokens. */
    // Moved into the HttpOnly cookie by RefreshTokenCookieAdvice, so it never
    // reaches the page's JavaScript; left out of the JSON when empty
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String refreshToken;

    /** The authenticated user's profile data. */
    private UserResponse user;
}
