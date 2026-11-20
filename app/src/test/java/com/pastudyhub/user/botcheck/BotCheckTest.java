package com.pastudyhub.user.botcheck;

import com.pastudyhub.user.exception.BotCheckFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** The Turnstile check against a fake Cloudflare. */
@DisplayName("Bot check (Turnstile)")
class BotCheckTest {

    private static final String BASE_URL = "https://challenges.cloudflare.test/turnstile/v0";

    private MockRestServiceServer server;
    private BotCheck botCheck;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        botCheck = new BotCheck(builder.build(), "site-key", "secret-key");
    }

    @Test
    @DisplayName("Off without keys: nothing is checked and no site key is given out")
    void offWithoutKeys() {
        BotCheck off = new BotCheck(RestClient.create(), "", "");
        assertThat(off.isEnabled()).isFalse();
        assertThat(off.siteKey()).isNull();
        assertThatCode(() -> off.verify(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Sends the secret and the token to Cloudflare and accepts a success")
    void acceptsSuccess() {
        server.expect(requestTo(BASE_URL + "/siteverify"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formData(org.springframework.util.CollectionUtils.toMultiValueMap(
                        java.util.Map.of("secret", java.util.List.of("secret-key"), "response", java.util.List.of("token-1")))))
                .andRespond(withSuccess("{\"success\": true, \"hostname\": \"pa-study-hub.onrender.com\"}", MediaType.APPLICATION_JSON));

        assertThat(botCheck.siteKey()).isEqualTo("site-key");
        assertThatCode(() -> botCheck.verify("token-1")).doesNotThrowAnyException();
        server.verify();
    }

    @Test
    @DisplayName("A missing token, a refusal, or Cloudflare being down all stop the sign-up")
    void refusals() {
        // Two answers from the fake Cloudflare, in this order: a refusal, then an error
        server.expect(requestTo(BASE_URL + "/siteverify"))
                .andRespond(withSuccess("{\"success\": false, \"error-codes\": [\"timeout-or-duplicate\"]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/siteverify")).andRespond(withServerError());

        // No token: Cloudflare is not even asked
        assertThatThrownBy(() -> botCheck.verify(" ")).isInstanceOf(BotCheckFailedException.class);
        assertThatThrownBy(() -> botCheck.verify("used-token"))
                .isInstanceOf(BotCheckFailedException.class)
                .hasMessage("Please complete the check that you're not a robot, then try again.");
        assertThatThrownBy(() -> botCheck.verify("token"))
                .isInstanceOf(BotCheckFailedException.class)
                .hasMessageContaining("could not be done right now");
        server.verify();
    }
}
