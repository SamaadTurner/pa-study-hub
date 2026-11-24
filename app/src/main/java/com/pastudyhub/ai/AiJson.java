package com.pastudyhub.ai;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Reads the JSON object out of a model's reply. */
final class AiJson {

    private AiJson() {
    }

    static JsonNode readObject(ObjectMapper objectMapper, String reply) {
        // Some models put the JSON in a ```json block or add a sentence around it,
        // so read from the first { to the last }
        int start = reply.indexOf('{');
        int end = reply.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new AiResponseException("The AI answer was not in the expected format.");
        }

        try {
            return objectMapper.readTree(reply.substring(start, end + 1));
        } catch (JacksonException e) {
            throw new AiResponseException("The AI answer was not in the expected format.", e);
        }
    }
}
