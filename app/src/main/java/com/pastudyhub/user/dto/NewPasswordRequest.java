package com.pastudyhub.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** The token from a reset link and the new password. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewPasswordRequest {

    @NotBlank(message = "The reset link is missing its code")
    private String token;

    @NotBlank(message = "New password is required")
    private String newPassword;
}
