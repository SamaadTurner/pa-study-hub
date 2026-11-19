package com.pastudyhub.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Adds Google sign-in to an existing account, proven with that account's password. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleLinkRequest {

    @NotBlank(message = "The Google credential is required")
    private String credential;

    @NotBlank(message = "Password is required")
    private String password;
}
