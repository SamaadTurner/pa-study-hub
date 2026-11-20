package com.pastudyhub.user.security;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Where LoginAttemptLimiter keeps its counts. The real one is the database table;
 * the unit tests use a map.
 */
public interface LoginFailureStore {

    Optional<LoginFailure> find(String email);

    void save(LoginFailure failure);

    void delete(String email);

    /** Removes the rows whose window ended before the given time. */
    void deleteEndedBefore(LocalDateTime time);
}
