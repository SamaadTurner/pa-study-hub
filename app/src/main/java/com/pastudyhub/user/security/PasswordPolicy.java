package com.pastudyhub.user.security;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates password validation rules for PA Study Hub.
 *
 * <p>Password requirements:
 * <ul>
 *   <li>Minimum 8 characters</li>
 *   <li>At least one uppercase letter (A-Z)</li>
 *   <li>At least one lowercase letter (a-z)</li>
 *   <li>At least one digit (0-9)</li>
 *   <li>At least one special character (!@#$%^&amp;*()_+-=[]|;':",&lt;&gt;?/)</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 *   PasswordPolicy policy = new PasswordPolicy();
 *   ValidationResult result = policy.validate("WeakPass");
 *   if (!result.isValid()) {
 *       // result.getErrors() contains specific failure messages
 *   }
 * }</pre>
 *
 * <p>This class is a pure domain object — no Spring dependency, fully unit-testable
 * in isolation without any Spring context.
 */
public class PasswordPolicy {

    private static final int MIN_LENGTH = 8;
    private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]|;':\",./<>?";

    /**
     * Validates a plaintext password against all policy rules.
     *
     * @param password the plaintext password to validate (must not be null)
     * @return {@link ValidationResult} indicating success or specific failures
     */
    public ValidationResult validate(String password) {
        if (password == null) {
            return ValidationResult.failure(List.of("Password must not be null"));
        }

        List<String> errors = new ArrayList<>();

        if (password.length() < MIN_LENGTH) {
            errors.add("Password must be at least " + MIN_LENGTH + " characters long");
        }

        if (!containsUppercase(password)) {
            errors.add("Password must contain at least one uppercase letter");
        }

        if (!containsLowercase(password)) {
            errors.add("Password must contain at least one lowercase letter");
        }

        if (!containsDigit(password)) {
            errors.add("Password must contain at least one number");
        }

        if (!containsSpecialChar(password)) {
            errors.add("Password must contain at least one special character (" + SPECIAL_CHARS + ")");
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    private boolean containsUppercase(String s) {
        for (char c : s.toCharArray()) {
            if (Character.isUpperCase(c)) return true;
        }
        return false;
    }

    private boolean containsLowercase(String s) {
        for (char c : s.toCharArray()) {
            if (Character.isLowerCase(c)) return true;
        }
        return false;
    }

    private boolean containsDigit(String s) {
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) return true;
        }
        return false;
    }

    private boolean containsSpecialChar(String s) {
        for (char c : s.toCharArray()) {
            if (SPECIAL_CHARS.indexOf(c) >= 0) return true;
        }
        return false;
    }

    /**
     * Result object returned by {@link PasswordPolicy#validate(String)}.
     */
    @Getter
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        private ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, List.of());
        }

        public static ValidationResult failure(List<String> errors) {
            return new ValidationResult(false, errors);
        }

        /**
         * Convenience method for getting a single combined error message.
         *
         * @return errors joined by "; "
         */
        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
}
