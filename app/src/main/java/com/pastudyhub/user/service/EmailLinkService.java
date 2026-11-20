package com.pastudyhub.user.service;

import com.pastudyhub.mail.MailNotSentException;
import com.pastudyhub.mail.MailProperties;
import com.pastudyhub.mail.MailSender;
import com.pastudyhub.user.model.EmailToken;
import com.pastudyhub.user.model.User;
import com.pastudyhub.user.repository.EmailTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * The links the app sends by email: reset a password (works for 1 hour) and
 * confirm the email (24 hours). Each works once, and a new link of the same kind
 * makes the older ones stop working.
 *
 * <p>The token in a link is 32 random bytes. The database only keeps its SHA-256
 * hash, so someone reading the table still can't use a link.
 *
 * <p>A user gets at most {@link #MAX_PER_HOUR} links of one kind an hour. The free
 * Brevo plan sends 300 emails a day for the whole site, so this also keeps one
 * person from using them up.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailLinkService {

    public static final Duration RESET_VALID_FOR = Duration.ofHours(1);
    public static final Duration CONFIRM_VALID_FOR = Duration.ofHours(24);
    public static final int MAX_PER_HOUR = 3;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailTokenRepository emailTokenRepository;
    private final MailSender mailSender;
    private final MailProperties mailProperties;

    /** True when the user may be sent another link of this kind now. */
    @Transactional(readOnly = true)
    public boolean maySend(User user, EmailToken.Purpose purpose) {
        LocalDateTime anHourAgo = LocalDateTime.now().minusHours(1);
        return emailTokenRepository.countByUserIdAndPurposeAndCreatedAtAfter(user.getId(), purpose, anHourAgo)
                < MAX_PER_HOUR;
    }

    /**
     * Sends a password reset link.
     *
     * @throws MailNotSentException when the email provider refused it
     */
    @Transactional
    public void sendPasswordReset(User user) {
        String token = newToken(user, EmailToken.Purpose.RESET_PASSWORD, RESET_VALID_FOR);
        String link = mailProperties.getAppBaseUrl() + "/reset-password?token=" + token;
        String text = "Hi " + user.getFirstName() + ",\n\n"
                + "Someone asked to reset the password of your PA Study Hub account. "
                + "To choose a new password, open this link within an hour:\n\n" + link + "\n\n"
                + "If it wasn't you, ignore this email. Your password stays the same.\n";
        mailSender.send(user.getEmail(), "Reset your PA Study Hub password", text, toHtml(text, link));
    }

    /**
     * Sends a link that confirms the user's email.
     *
     * @throws MailNotSentException when the email provider refused it
     */
    @Transactional
    public void sendEmailConfirmation(User user) {
        String token = newToken(user, EmailToken.Purpose.CONFIRM_EMAIL, CONFIRM_VALID_FOR);
        String link = mailProperties.getAppBaseUrl() + "/confirm-email?token=" + token;
        String text = "Hi " + user.getFirstName() + ",\n\n"
                + "Please confirm the email of your PA Study Hub account by opening this link:\n\n" + link + "\n\n"
                + "The link works for 24 hours. If you didn't make this account, ignore this email.\n";
        mailSender.send(user.getEmail(), "Confirm your email for PA Study Hub", text, toHtml(text, link));
    }

    /**
     * Uses a link: returns its user if the token is right, of this kind, unused and
     * not expired, and marks it used. Otherwise empty.
     */
    @Transactional
    public Optional<User> useLink(String token, EmailToken.Purpose purpose) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Optional<EmailToken> found = emailTokenRepository.findByTokenHashAndPurpose(hash(token.trim()), purpose);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        EmailToken emailToken = found.get();
        LocalDateTime now = LocalDateTime.now();
        if (emailToken.getUsedAt() != null || emailToken.getExpiresAt().isBefore(now)) {
            return Optional.empty();
        }
        emailToken.setUsedAt(now);
        emailTokenRepository.save(emailToken);
        return Optional.of(emailToken.getUser());
    }

    private String newToken(User user, EmailToken.Purpose purpose, Duration validFor) {
        LocalDateTime now = LocalDateTime.now();
        emailTokenRepository.retireUnused(user.getId(), purpose, now);

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        emailTokenRepository.save(EmailToken.builder()
                .user(user)
                .purpose(purpose)
                .tokenHash(hash(token))
                .expiresAt(now.plus(validFor))
                .build());
        return token;
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            // Every Java runtime has SHA-256
            throw new IllegalStateException(ex);
        }
    }

    // The same words as the text version, with the link clickable
    private String toHtml(String text, String link) {
        String escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        String withLink = escaped.replace(link.replace("&", "&amp;"),
                "<a href=\"" + link + "\">" + link + "</a>");
        return "<p>" + withLink.replace("\n\n", "</p><p>").replace("\n", "<br>") + "</p>";
    }
}
