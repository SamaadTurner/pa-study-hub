package com.pastudyhub.user.repository;

import com.pastudyhub.user.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access layer for {@link RefreshToken} entities.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Find a refresh token by its opaque token string.
     * Used during token refresh and logout flows.
     *
     * <p>Safe: Spring Data JPA generates a parameterized query — no SQL injection risk.
     *
     * @param token the opaque token string to look up
     * @return Optional containing the token record if found
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Revoke all active refresh tokens for a user (used during logout-all-devices).
     *
     * <p>This JPQL query is parameterized (via :userId) — no SQL injection risk.
     * Uses JPQL named parameter binding which Hibernate compiles to a prepared statement.
     *
     * @param userId the user's UUID
     */
    @Modifying
    @Query("UPDATE RefreshToken r SET r.isRevoked = true WHERE r.user.id = :userId AND r.isRevoked = false")
    void revokeAllForUser(@Param("userId") UUID userId);

    /**
     * Delete expired and revoked tokens older than the cutoff to keep the table clean.
     * Safe: parameterized JPQL query.
     *
     * @param cutoff tokens with expiresAt before this timestamp will be deleted
     */
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :cutoff OR r.isRevoked = true")
    void deleteExpiredAndRevoked(@Param("cutoff") LocalDateTime cutoff);
}
