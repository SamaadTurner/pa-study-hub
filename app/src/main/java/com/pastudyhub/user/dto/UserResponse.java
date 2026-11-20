package com.pastudyhub.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Public-safe representation of a User.
 * Never includes passwordHash or other sensitive internal fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String paSchoolName;
    private Integer graduationYear;
    /** The time zone streaks and goals use (from the user's browser); null until known. */
    private String timeZone;
    private boolean studyRemindersEnabled;
    private String preferredStudyTime;
    private String avatarUrl;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    /** False for an account made with Google that has not set a password. */
    private boolean hasPassword;
    private boolean googleLinked;
    private boolean emailVerified;
}
