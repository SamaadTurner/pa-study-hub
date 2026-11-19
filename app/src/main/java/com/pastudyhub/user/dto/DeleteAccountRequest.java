package com.pastudyhub.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Confirmation for closing an account. An account with a password sends the
 * password; an account that only uses Google types its email instead.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteAccountRequest {
    private String password;
    private String confirmEmail;
}
