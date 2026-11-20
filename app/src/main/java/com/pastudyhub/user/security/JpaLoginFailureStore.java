package com.pastudyhub.user.security;

import com.pastudyhub.user.repository.LoginFailureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * The login_failures table.
 *
 * <p>Every write runs in its own transaction (REQUIRES_NEW). A failed login ends
 * with an exception, which rolls back the login's transaction; without this the
 * failure would be rolled back with it and never counted.
 */
@Component
@RequiredArgsConstructor
public class JpaLoginFailureStore implements LoginFailureStore {

    private final LoginFailureRepository repository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<LoginFailure> find(String email) {
        return repository.findById(email);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(LoginFailure failure) {
        repository.save(failure);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void delete(String email) {
        repository.deleteById(email);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteEndedBefore(LocalDateTime time) {
        repository.deleteEndedBefore(time);
    }

    // Rows of windows that are over only take up space
    // (calls the repository directly: a call to this class's own method would skip
    // the transaction, and a delete query needs one)
    @Scheduled(fixedDelay = 3_600_000, initialDelay = 300_000)
    @Transactional
    public void removeOldRows() {
        repository.deleteEndedBefore(LocalDateTime.now());
    }
}
