package com.pastudyhub.user.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request body for POST /api/v1/auth/register.
 * All fields are validated server-side regardless of client-side validation.
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    /**
     * Minimum 8 chars with uppercase, lowercase, digit, and special character.
     * Additional validation is performed by {@link com.pastudyhub.user.security.PasswordPolicy}.
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    private String password;

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @Size(max = 200, message = "PA school name must not exceed 200 characters")
    private String paSchoolName;

    @Min(value = 2020, message = "Graduation year must be 2020 or later")
    @Max(value = 2040, message = "Graduation year must be 2040 or earlier")
    private Integer graduationYear;
}
