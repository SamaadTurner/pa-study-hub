package com.pastudyhub.user.repository;

import com.pastudyhub.user.security.LoginFailure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface LoginFailureRepository extends JpaRepository<LoginFailure, String> {

    @Modifying
    @Query("DELETE FROM LoginFailure f WHERE f.windowEnd < :time")
    int deleteEndedBefore(@Param("time") LocalDateTime time);
}
