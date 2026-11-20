package com.pastudyhub.user.dto;

import lombok.Data;

/** Request body for POST /api/v1/auth/refresh and /logout (both optional now). */
@Data
public class RefreshTokenRequest {

    // Optional: the token now comes in the HttpOnly cookie. A browser tab opened
    // before that change still sends it here once (see AuthController.refresh).
    private String refreshToken;
}
