package com.pastudyhub.user.dev;

import com.pastudyhub.user.model.User;
import com.pastudyhub.user.model.UserRole;
import com.pastudyhub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates a demo account at startup so there is something to log in with on a
 * developer's machine.
 *
 * <p>This only exists with the "dev" profile. The password is written right here
 * in a public repository, so the account must never exist in a real deployment.
 * Without the profile this class is not even created.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DemoAccountLoader implements ApplicationRunner {

    public static final String EMAIL = "demo@example.com";
    public static final String PASSWORD = "DemoStudent1!";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        // The app is restarted many times against the same database
        if (userRepository.existsByEmail(EMAIL)) {
            return;
        }

        User demoUser = User.builder()
                .email(EMAIL)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .firstName("Demo")
                .lastName("Student")
                .role(UserRole.STUDENT)
                .build();

        userRepository.save(demoUser);
        log.info("Created the demo account {} (dev profile only)", EMAIL);
    }
}
