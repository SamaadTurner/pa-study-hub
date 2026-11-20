package com.pastudyhub.user.repository;

import com.pastudyhub.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access layer for {@link User} entities.
 *
 * <p>All queries use Spring Data JPA parameterized query derivation — no raw SQL,
 * eliminating SQL injection risk at the data access layer.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /** Accounts made since the given time (SignupLimiter). */
    long countByCreatedAtAfter(java.time.LocalDateTime since);

    /**
     * Find a user by their email address (case-insensitive via LOWER() in DB).
     * Email is normalized to lowercase before storage, so this lookup is consistent.
     *
     * @param email the email address to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if an email address is already registered.
     * Used during registration to detect duplicates without loading the full entity.
     *
     * @param email the email to check
     * @return true if a user with this email exists
     */
    boolean existsByEmail(String email);

    Optional<User> findByGoogleSubject(String googleSubject);

    // Accounts closed before closing deleted the data (see ClosedAccountCleanup)
    @Query("SELECT u.id FROM User u WHERE u.isActive = false")
    List<UUID> findIdsOfInactiveUsers();
}
