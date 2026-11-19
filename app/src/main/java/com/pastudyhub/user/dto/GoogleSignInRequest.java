package com.pastudyhub.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** The ID token ("credential") that Google's sign-in button gives the browser. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleSignInRequest {

    @NotBlank(message = "The Google credential is required")
    private String credential;
}
