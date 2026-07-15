package com.pastudyhub.user.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PasswordPolicy}.
 *
 * <p>Tests all policy rules in isolation — no Spring context needed.
 * Uses AssertJ for readable assertions.
 */
@DisplayName("PasswordPolicy Unit Tests")
class PasswordPolicyTest {

    private PasswordPolicy passwordPolicy;

    @BeforeEach
    void setUp() {
        passwordPolicy = new PasswordPolicy();
    }

    @Test
    @DisplayName("Valid password should pass all rules")
    void validPassword_shouldPass() {
        PasswordPolicy.ValidationResult result = passwordPolicy.validate("PAStudent2026!");

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("Another valid password with different special char should pass")
    void anotherValidPassword_shouldPass() {
        PasswordPolicy.ValidationResult result = passwordPolicy.validate("Cardiology@99");

        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("Password too short (7 chars) should fail")
    void passwordTooShort_shouldFail() {
        PasswordPolicy.ValidationResult result = passwordPolicy.validate("Short1!");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors())
                .anyMatch(err -> err.contains("at least 8 characters"));
    }

    @Test
    @DisplayName("Password missing uppercase should fail")
    void passwordMissingUppercase_shouldFail() {
        PasswordPolicy.ValidationResult result = passwordPolicy.validate("lowercase123!");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors())
                .anyMatch(err -> err.contains("uppercase"));
    }

    @Test
    @DisplayName("Password missing lowercase should fail")
    void passwordMissingLowercase_shouldFail() {
        PasswordPolicy.ValidationResult result = passwordPolicy.validate("UPPERCASE123!");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors())
                .anyMatch(err -> err.contains("lowercase"));
    }

    @Test
    @DisplayName("Password missing number should fail")
    void passwordMissingNumber_shouldFail() {
        PasswordPolicy.ValidationResult result = passwordPolicy.validate("NoNumbers!");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors())
                .anyMatch(err -> err.contains("number"));
    }

    @Test
    @DisplayName("Password missing special character should fail")
    void passwordMissingSpecialChar_shouldFail() {
        PasswordPolicy.ValidationResult result = passwordPolicy.validate("NoSpecials123");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors())
                .anyMatch(err -> err.contains("special character"));
    }

    @Test
    @DisplayName("Null password should fail with specific error")
    void nullPassword_shouldFail() {
        PasswordPolicy.ValidationResult result = passwordPolicy.validate(null);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    @DisplayName("Empty string password should fail multiple rules")
    void emptyPassword_shouldFailMultipleRules() {
        PasswordPolicy.ValidationResult result = passwordPolicy.validate("");

        assertThat(result.isValid()).isFalse();
        // Should fail length, uppercase, lowercase, digit, and special char
        assertThat(result.getErrors()).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Password with multiple failures should include all error messages")
    void multipleFailures_allErrorsReturned() {
        PasswordPolicy.ValidationResult result = passwordPolicy.validate("short");

        assertThat(result.isValid()).isFalse();
        // Fails: length, uppercase, digit, special char
        assertThat(result.getErrors()).hasSizeGreaterThanOrEqualTo(3);
        assertThat(result.getErrorMessage()).contains(";");
    }

    @Test
    @DisplayName("Exactly 8 characters with all requirements should pass")
    void exactly8CharsWithAllRules_shouldPass() {
        // Exactly 8: uppercase, lowercase, digit, special
        PasswordPolicy.ValidationResult result = passwordPolicy.validate("Abc1!def");

        assertThat(result.isValid()).isTrue();
    }
}
