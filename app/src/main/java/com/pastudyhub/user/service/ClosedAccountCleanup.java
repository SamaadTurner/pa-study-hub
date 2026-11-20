package com.pastudyhub.user.service;

import com.pastudyhub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Before 2026-11-19, closing an account only switched it off (is_active = false)
 * and kept the data. The privacy page now says closing deletes the data, so at
 * startup this deletes the accounts that were closed the old way, the same way
 * closing works today. Nothing switches accounts off any more, so after the first
 * run it finds nothing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClosedAccountCleanup implements ApplicationRunner {

    private final UserRepository userRepository;
    private final AccountDeletionService accountDeletionService;

    @Override
    public void run(ApplicationArguments args) {
        List<UUID> closed = userRepository.findIdsOfInactiveUsers();
        for (UUID userId : closed) {
            accountDeletionService.deleteEverything(userId);
        }
        if (!closed.isEmpty()) {
            log.info("Deleted {} account(s) that were closed before closing deleted data", closed.size());
        }
    }
}
