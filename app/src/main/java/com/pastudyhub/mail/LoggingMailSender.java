package com.pastudyhub.mail;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Used when no Brevo key is set (tests, local runs). Writes the subject to the log
 * and keeps the last emails in memory, so tests can open the links in them. The
 * dev profile shows them through DevOutboxController.
 */
@Slf4j
public class LoggingMailSender implements MailSender {

    /** One email as it would have been sent. */
    public record SentMail(String to, String subject, String text) {
    }

    private static final int KEEP = 200;

    private final List<SentMail> outbox = new ArrayList<>();

    @Override
    public synchronized void send(String to, String subject, String text, String html) {
        log.info("Email not sent (no Brevo key set): subject=\"{}\"", subject);
        outbox.add(new SentMail(to, subject, text));
        if (outbox.size() > KEEP) {
            outbox.remove(0);
        }
    }

    /** The emails to one address, newest first. */
    public synchronized List<SentMail> sentTo(String to) {
        List<SentMail> result = new ArrayList<>();
        for (int i = outbox.size() - 1; i >= 0; i--) {
            if (outbox.get(i).to().equalsIgnoreCase(to)) {
                result.add(outbox.get(i));
            }
        }
        return result;
    }
}
