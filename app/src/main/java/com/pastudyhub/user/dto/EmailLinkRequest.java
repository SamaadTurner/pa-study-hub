package com.pastudyhub.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** The token from a link sent by email. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailLinkRequest {

    @NotBlank(message = "The link is missing its code")
    private String token;
}
