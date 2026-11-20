package com.pastudyhub.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Sends email through Brevo's transactional email API
 * (POST https://api.brevo.com/v3/smtp/email, key in the "api-key" header).
 *
 * <p>Brevo only accepts calls from IP addresses on the account's allow-list
 * (Settings, Security, Authorised IPs); a call from anywhere else gets a 401.
 */
@Slf4j
public class BrevoMailSender implements MailSender {

    private final RestClient restClient;
    private final MailProperties properties;

    public BrevoMailSender(MailProperties properties, RestClient.Builder builder) {
        this(properties, builder.baseUrl("https://api.brevo.com/v3").build());
    }

    // The tests pass in a RestClient that talks to a fake server
    BrevoMailSender(MailProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public void send(String to, String subject, String text, String html) {
        Map<String, Object> body = Map.of(
                "sender", Map.of("name", properties.getFromName(), "email", properties.getFromAddress()),
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "textContent", text,
                "htmlContent", html);
        try {
            restClient.post()
                    .uri("/smtp/email")
                    .header("api-key", properties.getBrevoApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Email sent through Brevo: subject=\"{}\"", subject);
        } catch (RestClientException ex) {
            // The message can name the problem (for example an IP not on the allow-list).
            // The recipient's address is not logged.
            throw new MailNotSentException("Brevo did not accept the email: " + ex.getMessage(), ex);
        }
    }
}
