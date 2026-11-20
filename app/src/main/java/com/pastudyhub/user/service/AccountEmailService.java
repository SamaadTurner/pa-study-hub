package com.pastudyhub.user.service;

import com.pastudyhub.mail.MailNotSentException;
import com.pastudyhub.user.exception.EmailNotSentException;
import com.pastudyhub.user.exception.TooManyEmailsException;
import com.pastudyhub.user.exception.UserNotFoundException;
import com.pastudyhub.user.model.EmailToken;
import com.pastudyhub.user.model.User;
import com.pastudyhub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

/**
 * When the app emails a user: the confirmation after signing up, a new confirmation
 * on request, and "forgot password".
 *
 * <p>These methods are not transactional on purpose. Each email goes out in
 * EmailLinkService's own transaction, so a failed send rolls back only its link,
 * never the sign-up that came before it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountEmailService {

    private final UserRepository userRepository;
    private final EmailLinkService emailLinkService;

    /** Right after signing up. A failure is logged; the account is made either way. */
    public void sendConfirmationAfterSignUp(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            try {
                emailLinkService.sendEmailConfirmation(user);
            } catch (MailNotSentException ex) {
                log.warn("Confirmation email after sign-up not sent: userId={}, {}", userId, ex.getMessage());
            }
        });
    }

    /** "Send the link again" from the app. */
    public void resendConfirmation(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (user.isEmailVerified()) {
            return;
        }
        if (!emailLinkService.maySend(user, EmailToken.Purpose.CONFIRM_EMAIL)) {
            throw new TooManyEmailsException("We already sent a few links in the last hour. Check your inbox and spam "
                    + "folder, or try again later.");
        }
        try {
            emailLinkService.sendEmailConfirmation(user);
        } catch (MailNotSentException ex) {
            log.error("Confirmation email not sent: userId={}, {}", userId, ex.getMessage());
            throw new EmailNotSentException("The email could not be sent right now. Please try again later.");
        }
    }

    /**
     * "Forgot password". The caller always answers the same way, whether or not an
     * account has this email, so the form can't be used to find out who has one.
     */
    public void requestPasswordReset(String email) {
        String normalized = email.toLowerCase(Locale.ROOT).trim();
        User user = userRepository.findByEmail(normalized).orElse(null);
        if (user == null || !user.isActive()) {
            log.info("Password reset asked for an email without an active account");
            return;
        }
        if (!emailLinkService.maySend(user, EmailToken.Purpose.RESET_PASSWORD)) {
            log.info("Password reset not sent, hourly limit reached: userId={}", user.getId());
            return;
        }
        try {
            emailLinkService.sendPasswordReset(user);
        } catch (MailNotSentException ex) {
            log.error("Password reset email not sent: userId={}, {}", user.getId(), ex.getMessage());
        }
    }
}
