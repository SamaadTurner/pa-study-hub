package com.pastudyhub.mail;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Settings for sending email, from the "mail" section of application.yml.
 *
 * <p>With a Brevo API key the app sends through Brevo's web API (Render's free
 * plan blocks the SMTP ports, so plain SMTP is not an option there). Without a key
 * emails are only written to the log, which is what tests and local runs use.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mail")
public class MailProperties {

    /** Brevo API key. Empty = emails are logged, not sent. */
    private String brevoApiKey = "";

    /** The sender address verified in Brevo. */
    private String fromAddress = "";

    private String fromName = "PA Study Hub";

    /** The site's address, for the links in emails (no slash at the end). */
    private String appBaseUrl = "https://pa-study-hub.onrender.com";

    public boolean sendsForReal() {
        return !brevoApiKey.isBlank();
    }
}
