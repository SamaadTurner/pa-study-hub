package com.pastudyhub.ai;

import tools.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Calls POST {base-url}/chat/completions in the OpenAI format. Groq, Gemini and
 * Ollama all accept this, so switching provider is a settings change.
 */
@Slf4j
@Component
public class OpenAiCompatibleChatClient implements ChatClient {

    private final AiProperties properties;
    private final RestClient restClient;

    @Autowired
    public OpenAiCompatibleChatClient(AiProperties properties, RestClient.Builder builder) {
        this(properties, builder
                .requestFactory(timeouts(properties))
                .baseUrl(properties.getBaseUrl())
                .build());
    }

    // The tests pass in a RestClient that talks to a fake server
    OpenAiCompatibleChatClient(AiProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    private static SimpleClientHttpRequestFactory timeouts(AiProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()));
        return factory;
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    @Override
    public String model() {
        return properties.getModel();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        if (!isEnabled()) {
            throw new AiUnavailableException("AI features are turned off on this server.");
        }

        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                // Low temperature: we want the facts from the notes, not creativity
                "temperature", 0.3,
                // Asks for a reply that is valid JSON. The prompt also says so,
                // because some providers need both.
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));

        JsonNode reply;
        try {
            reply = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            throw errorFor(e);
        } catch (ResourceAccessException e) {
            // Could not connect, or no answer within the timeout
            log.warn("AI provider not reachable: {}", e.getMessage());
            throw new AiUnavailableException("The AI service is not answering. Try again later.", e);
        }

        JsonNode content = reply == null ? null : reply.path("choices").path(0).path("message").path("content");
        if (content == null || !content.isString() || content.asString().isBlank()) {
            throw new AiResponseException("The AI service sent an empty answer.");
        }
        return content.asString();
    }

    // The key is never logged, only the status code
    private RuntimeException errorFor(RestClientResponseException e) {
        int status = e.getStatusCode().value();
        log.warn("AI provider answered with status {}", status);

        if (status == HttpStatus.TOO_MANY_REQUESTS.value()) {
            // The free tier allows a limited number of requests per minute and per day
            return new AiUnavailableException(
                    "The AI service is busy right now (free usage limit reached). Try again in a minute.", e);
        }
        if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value()) {
            return new AiResponseException("The AI service refused the server's API key.", e);
        }
        return new AiResponseException("The AI service answered with an error (status " + status + ").", e);
    }
}
