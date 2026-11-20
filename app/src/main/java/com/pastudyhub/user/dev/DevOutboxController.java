package com.pastudyhub.user.dev;

import com.pastudyhub.mail.LoggingMailSender;
import com.pastudyhub.mail.MailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * The emails the app would have sent, for the end-to-end tests: they read the reset
 * and confirmation links from here instead of a real inbox.
 *
 * <p>Only exists with the dev profile, and only works while emails are logged
 * rather than sent (no Brevo key). Anyone could read it, so it must never run on a
 * real deployment.
 */
@RestController
@Profile("dev")
@RequestMapping("/api/v1/dev/outbox")
@RequiredArgsConstructor
public class DevOutboxController {

    private final MailSender mailSender;

    @GetMapping
    public List<LoggingMailSender.SentMail> sentTo(@RequestParam String to) {
        if (!(mailSender instanceof LoggingMailSender logging)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Emails are being sent for real");
        }
        return logging.sentTo(to);
    }
}
