package com.pastudyhub.mail;

/**
 * Sends one email. An interface so the provider can be swapped by configuration
 * (Brevo today; a bought domain with another provider later) and tests can use
 * the logging one.
 */
public interface MailSender {

    /**
     * @throws MailNotSentException when the provider refused or could not be reached
     */
    void send(String to, String subject, String text, String html);
}
