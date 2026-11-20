package com.pastudyhub.user.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/** A link sent by email: reset a password, or confirm the email. Only the token's hash is stored. */
@Entity
@Table(name = "email_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uq_email_tokens_token_hash", columnNames = "token_hash"),
        indexes = @Index(name = "idx_email_tokens_user_purpose", columnList = "user_id, purpose, created_at"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailToken {

    public enum Purpose {
        RESET_PASSWORD,
        CONFIRM_EMAIL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Purpose purpose;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
