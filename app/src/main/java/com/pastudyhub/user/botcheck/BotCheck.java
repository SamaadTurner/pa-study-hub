package com.pastudyhub.user.botcheck;

import com.pastudyhub.user.exception.BotCheckFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * The check on the sign-up form that a person, not a script, is making the account
 * (Cloudflare Turnstile).
 *
 * <p>The sign-up cap (SignupLimiter) protects the free email quota, but a script
 * could use the cap up and pause sign-up for everyone. With this check, a script
 * has to get past Turnstile for every account. The browser shows the widget and
 * gets a one-time token; the server asks Cloudflare whether the token is good.
 * Off when the keys are not set (local runs, tests, docker compose).
 */
@Slf4j
@Component
public class BotCheck {

    static final String CLOUDFLARE_URL = "https://challenges.cloudflare.com/turnstile/v0";

    private final RestClient restClient;
    private final String siteKey;
    private final String secretKey;

    @Autowired
    public BotCheck(RestClient.Builder builder,
                    @Value("${bot-check.turnstile.site-key:}") String siteKey,
                    @Value("${bot-check.turnstile.secret-key:}") String secretKey) {
        this(builder.baseUrl(CLOUDFLARE_URL).build(), siteKey, secretKey);
    }

    // The tests pass in a RestClient that talks to a fake Cloudflare
    BotCheck(RestClient restClient, String siteKey, String secretKey) {
        this.restClient = restClient;
        this.siteKey = siteKey;
        this.secretKey = secretKey;
    }

    public boolean isEnabled() {
        return !siteKey.isBlank() && !secretKey.isBlank();
    }

    /** The public key the browser needs for the widget, or null when the check is off. */
    public String siteKey() {
        return isEnabled() ? siteKey : null;
    }

    /** Throws BotCheckFailedException unless the check is off or Cloudflare accepts the token. */
    public void verify(String token) {
        if (!isEnabled()) {
            return;
        }
        if (token == null || token.isBlank() || token.length() > 2048) {
            throw new BotCheckFailedException();
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", secretKey);
        form.add("response", token);
        Map<?, ?> answer;
        try {
            answer = restClient.post()
                    .uri("/siteverify")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException ex) {
            // Cloudflare can't be reached: sign-up waits rather than letting scripts through
            log.warn("Turnstile check could not be done: {}", ex.getMessage());
            throw new BotCheckFailedException("The check that you're not a robot could not be done right now. Please try again.");
        }
        if (answer == null || !Boolean.TRUE.equals(answer.get("success"))) {
            Object codes = answer == null ? List.of() : answer.get("error-codes");
            log.info("Turnstile refused a sign-up: {}", codes);
            throw new BotCheckFailedException();
        }
    }
}
