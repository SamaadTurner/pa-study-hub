package com.pastudyhub.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Slf4j
@Configuration
public class MailConfig {

    // Brevo when a key is set, else only the log. Switching provider later means
    // another MailSender here and a setting, nothing else.
    @Bean
    public MailSender mailSender(MailProperties properties, RestClient.Builder builder) {
        if (properties.sendsForReal()) {
            log.info("Emails are sent through Brevo");
            return new BrevoMailSender(properties, builder);
        }
        log.info("No Brevo key set: emails are written to the log and not sent");
        return new LoggingMailSender();
    }
}
