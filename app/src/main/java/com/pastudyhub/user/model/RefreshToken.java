package com.pastudyhub.user.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a long-lived refresh token issued to a user.
 *
 * <p>Refresh tokens allow clients to obtain a new access token without requiring
 * the user to log in again. They expire after {@code JWT_REFRESH_TOKEN_EXPIRY_DAYS}
 * days (default: 7) and are rotated on every refresh (old token revoked, new issued).
 *
 * <p>Token rotation prevents replay attacks: if an attacker steals a refresh token,
 * using it will immediately invalidate the legitimate user's session (because the
 * legitimate client will then hold a revoked token and receive a 401).
 */
@Entity
@Table(name = "refresh_tokens",
    indexes = {
        @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"),
        @Index(name = "idx_refresh_tokens_token", columnList = "token", unique = true)
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The opaque token string (a UUID) stored in the client's secure storage.
     * Not a JWT — just a random UUID looked up in this table.
     */
    @Column(name = "token", unique = true, nullable = false, length = 36)
    private String token;

    /**
     * When this refresh token expires. Checked on every refresh request.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * True if this token has been revoked (logout or rotation).
     * Revoked tokens are kept in the DB for audit purposes.
     */
    @Column(name = "is_revoked", nullable = false)
    @Builder.Default
    private boolean isRevoked = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
