package com.pastudyhub.user.security;

import com.pastudyhub.user.exception.TooManySignupsException;
import com.pastudyhub.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Sign-up limit")
class SignupLimiterTest {

    private final UserRepository users = mock(UserRepository.class);
    private final SignupLimiter limiter = new SignupLimiter(users, 30, 200);

    private void accountsMade(long lastHour, long lastDay) {
        when(users.countByCreatedAtAfter(argThat(t -> t != null && t.isAfter(LocalDateTime.now().minusMinutes(61))))).thenReturn(lastHour);
        when(users.countByCreatedAtAfter(argThat(t -> t != null && t.isBefore(LocalDateTime.now().minusHours(2))))).thenReturn(lastDay);
    }

    @Test
    @DisplayName("Under both limits a new account is fine")
    void underTheLimits() {
        accountsMade(29, 199);
        assertThatCode(limiter::checkRoomForOneMore).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("The 31st account in an hour, or the 201st in a day, waits")
    void overALimit() {
        accountsMade(30, 30);
        assertThatThrownBy(limiter::checkRoomForOneMore).isInstanceOf(TooManySignupsException.class);

        accountsMade(5, 200);
        assertThatThrownBy(limiter::checkRoomForOneMore).isInstanceOf(TooManySignupsException.class);
    }
}
