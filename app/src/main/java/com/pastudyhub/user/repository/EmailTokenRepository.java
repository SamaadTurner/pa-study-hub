package com.pastudyhub.user.repository;

import com.pastudyhub.user.model.EmailToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface EmailTokenRepository extends JpaRepository<EmailToken, UUID> {

    Optional<EmailToken> findByTokenHashAndPurpose(String tokenHash, EmailToken.Purpose purpose);

    // How many links of this kind the user was sent since a moment (to limit sending)
    long countByUserIdAndPurposeAndCreatedAtAfter(UUID userId, EmailToken.Purpose purpose, LocalDateTime since);

    // A new link makes the user's older unused links of the same kind stop working
    @Modifying
    @Query("UPDATE EmailToken t SET t.usedAt = :now WHERE t.user.id = :userId "
            + "AND t.purpose = :purpose AND t.usedAt IS NULL")
    void retireUnused(@Param("userId") UUID userId, @Param("purpose") EmailToken.Purpose purpose,
                      @Param("now") LocalDateTime now);
}
