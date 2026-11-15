package com.pastudyhub.user.dev;

import com.pastudyhub.user.model.User;
import com.pastudyhub.user.model.UserRole;
import com.pastudyhub.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Demo account (dev profile)")
class DemoAccountLoaderTest {

    // Strength 4 is the lowest BCrypt allows. It keeps the test fast.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private UserRepository userRepository;
    private DemoAccountLoader loader;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        loader = new DemoAccountLoader(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Creates the account when it is not there yet")
    void createsTheAccount() {
        when(userRepository.existsByEmail(DemoAccountLoader.EMAIL)).thenReturn(false);

        loader.run(null);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());

        User demoUser = saved.getValue();
        assertThat(demoUser.getEmail()).isEqualTo("demo@example.com");
        assertThat(demoUser.getFirstName()).isEqualTo("Demo");
        assertThat(demoUser.getRole()).isEqualTo(UserRole.STUDENT);
        assertThat(demoUser.isActive()).isTrue();
    }

    @Test
    @DisplayName("Stores a hash that matches the demo password, never the password itself")
    void storesAHashOfThePassword() {
        when(userRepository.existsByEmail(DemoAccountLoader.EMAIL)).thenReturn(false);

        loader.run(null);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());

        String storedHash = saved.getValue().getPasswordHash();
        assertThat(storedHash).isNotEqualTo(DemoAccountLoader.PASSWORD);
        assertThat(passwordEncoder.matches(DemoAccountLoader.PASSWORD, storedHash)).isTrue();
    }

    @Test
    @DisplayName("Does nothing when the account already exists")
    void leavesAnExistingAccountAlone() {
        when(userRepository.existsByEmail(DemoAccountLoader.EMAIL)).thenReturn(true);

        loader.run(null);

        verify(userRepository, never()).save(any());
    }
}
