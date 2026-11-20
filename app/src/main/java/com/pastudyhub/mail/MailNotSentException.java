package com.pastudyhub.mail;

/** The email provider refused the email or could not be reached. */
public class MailNotSentException extends RuntimeException {
    public MailNotSentException(String message, Throwable cause) {
        super(message, cause);
    }
}
