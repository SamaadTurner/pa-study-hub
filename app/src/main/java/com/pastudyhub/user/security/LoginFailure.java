package com.pastudyhub.user.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Failed logins for one email in the current window (see LoginAttemptLimiter). */
@Entity
@Table(name = "login_failures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginFailure {

    @Id
    @Column(length = 255)
    private String email;

    @Column(name = "failure_count", nullable = false)
    private int failureCount;

    @Column(name = "window_end", nullable = false)
    private LocalDateTime windowEnd;
}
