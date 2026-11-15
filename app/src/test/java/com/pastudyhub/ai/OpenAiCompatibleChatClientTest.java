package com.pastudyhub.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * The client against a fake provider. MockRestServiceServer answers the HTTP calls,
 * so nothing leaves the machine and no API key is needed.
 */
@DisplayName("OpenAI-compatible chat client")
class OpenAiCompatibleChatClientTest {

    private static final String BASE_URL = "https://ai.example.test/openai/v1";

    private AiProperties properties;
    private MockRestServiceServer server;
    private OpenAiCompatibleChatClient client;

    @BeforeEach
    void setUp() {
        properties = new AiProperties();
        properties.setBaseUrl(BASE_URL);
        properties.setApiKey("test-key");
        properties.setModel("test-model");

        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OpenAiCompatibleChatClient(properties, builder.build());
    }

    private static String replyWithContent(String content) {
        return "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":" + content + "}}]}";
    }

    @Test
    @DisplayName("Sends the model, both messages and the key, and returns the reply text")
    void sendsRequestAndReturnsContent() {
        server.expect(requestTo(BASE_URL + "/chat/completions"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("test-model"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[0].content").value("be helpful"))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andExpect(jsonPath("$.messages[1].content").value("hello"))
                .andExpect(jsonPath("$.response_format.type").value("json_object"))
                .andRespond(withSuccess(replyWithContent("\"{\\\"ok\\\":true}\""), MediaType.APPLICATION_JSON));

        String reply = client.complete("be helpful", "hello");

        assertThat(reply).isEqualTo("{\"ok\":true}");
        server.verify();
    }

    @Test
    @DisplayName("Without a key it is off and makes no call at all")
    void disabledWithoutKey() {
        properties.setApiKey("");

        assertThat(client.isEnabled()).isFalse();
        assertThatThrownBy(() -> client.complete("system", "user"))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("turned off");
        server.verify(); // nothing was expected, so nothing may have been sent
    }

    @Test
    @DisplayName("Reports the model it is set to use")
    void reportsModel() {
        assertThat(client.model()).isEqualTo("test-model");
    }

    @Test
    @DisplayName("A rate limit answer (429) means try again later")
    void rateLimited() {
        server.expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client.complete("system", "user"))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("busy");
    }

    @Test
    @DisplayName("A refused key (401) is reported as a provider error, without the key")
    void badKey() {
        server.expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.complete("system", "user"))
                .isInstanceOf(AiResponseException.class)
                .hasMessageContaining("API key")
                .hasMessageNotContaining("test-key");
    }

    @Test
    @DisplayName("Any other error status is a provider error with the status in it")
    void serverError() {
        server.expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.complete("system", "user"))
                .isInstanceOf(AiResponseException.class)
                .hasMessageContaining("500");
    }

    @Test
    @DisplayName("No answer in time means the service is not reachable")
    void timeout() {
        server.expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> client.complete("system", "user"))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("not answering");
    }

    @Test
    @DisplayName("A reply without any text is an error")
    void emptyReply() {
        server.expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.complete("system", "user"))
                .isInstanceOf(AiResponseException.class)
                .hasMessageContaining("empty");
    }
}
