package com.pastudyhub.user.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a PA Study Hub user account.
 *
 * <p>Passwords are stored as BCrypt hashes (strength 12) — never plaintext.
 * The userId is propagated as a JWT claim and forwarded in the X-User-Id header
 * by the API Gateway to all downstream services.
 */
@Entity
@Table(name = "users",
    indexes = {
        @Index(name = "idx_users_email", columnList = "email", unique = true)
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Primary login credential and unique identifier. Normalized to lowercase before storage.
     */
    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;

    /**
     * BCrypt hash (strength 12). The raw password is NEVER stored or logged.
     */
    @Column(name = "password_hash", nullable = false, length = 72)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.STUDENT;

    /** Optional — e.g. "University of Washington MEDEX". */
    @Column(name = "pa_school_name", length = 200)
    private String paSchoolName;

    /** Optional — expected graduation year, e.g. 2028. */
    @Column(name = "graduation_year")
    private Integer graduationYear;

    /**
     * When true, the app may send push/email reminders at preferredStudyTime.
     */
    @Column(name = "study_reminders_enabled", nullable = false)
    @Builder.Default
    private boolean studyRemindersEnabled = true;

    /**
     * 24-hour time string for study reminders, e.g. "08:00" or "20:30".
     */
    @Column(name = "preferred_study_time", length = 5)
    private String preferredStudyTime;

    /** URL to a profile avatar image (CDN or S3 URL). */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /**
     * Soft-delete flag. Deactivated users cannot log in.
     * Data is retained for audit purposes.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
}
