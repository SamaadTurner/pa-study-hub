package com.pastudyhub.flashcard.integration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.pastudyhub.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Fill-in-the-blank (cloze) cards")
class ClozeCardsIntegrationTest extends BaseIntegrationTest {

    private static final String TEXT =
            "{{c1::Metoprolol}} is a {{c2::beta blocker::drug class}} that lowers {{c1::heart rate}}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("One text makes one card per blank number, in order, at the end of the deck")
    void createsOneCardPerNumber() throws Exception {
        UUID user = UUID.randomUUID();
        String deckId = createDeck(user, false);
        createBasicCard(user, deckId, "Basic first");

        createCloze(user, deckId, TEXT, "Cardioselective")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].cardType").value("CLOZE"))
                .andExpect(jsonPath("$[0].clozeIndex").value(1))
                .andExpect(jsonPath("$[1].clozeIndex").value(2))
                .andExpect(jsonPath("$[0].front").value(TEXT))
                .andExpect(jsonPath("$[0].back").value("Cardioselective"));

        // Listed after the basic card, in number order, and both are due to study
        JsonNode cards = getJson(user, "/api/v1/decks/" + deckId + "/cards").get("content");
        assertThat(cards).hasSize(3);
        assertThat(cards.get(0).get("cardType").asString()).isEqualTo("BASIC");
        assertThat(cards.get(1).get("clozeIndex").asInt()).isEqualTo(1);
        assertThat(cards.get(2).get("clozeIndex").asInt()).isEqualTo(2);
        JsonNode queue = getJson(user, "/api/v1/decks/" + deckId + "/review");
        assertThat(queue).hasSize(3);
    }

    @Test
    @DisplayName("Text without blanks is refused, and extra notes are optional")
    void validation() throws Exception {
        UUID user = UUID.randomUUID();
        String deckId = createDeck(user, false);

        createCloze(user, deckId, "No blanks at all", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Mark at least one blank like this: {{c1::the answer}}"));
        createCloze(user, deckId, "{{c1::Only}} one", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].back").value(""));
        // Someone else's deck
        createCloze(UUID.randomUUID(), deckId, "{{c1::Not yours}}", null).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Editing a cloze card must keep its own blank; a basic card still needs an answer")
    void editing() throws Exception {
        UUID user = UUID.randomUUID();
        String deckId = createDeck(user, false);
        String secondCard = parse(createCloze(user, deckId, TEXT, null)).get(1).get("id").asString();

        Map<String, Object> withoutC2 = Map.of("front", "{{c1::Metoprolol}} lowers heart rate", "back", "");
        update(user, secondCard, withoutC2)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("This card asks for blank c2, so the text needs a {{c2::...}} blank"));
        update(user, secondCard, Map.of("front", "{{c1::Metoprolol}} is a {{c2::beta-1 blocker}}", "back", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.front").value("{{c1::Metoprolol}} is a {{c2::beta-1 blocker}}"));

        String basic = createBasicCard(user, deckId, "Question");
        update(user, basic, Map.of("front", "Question", "back", "  ")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Card back (answer) is required"));
        mockMvc.perform(post("/api/v1/decks/{id}/cards", deckId).header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("front", "No answer", "tags", List.of()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Copying a public deck keeps the cards' type and blank number")
    void cloneKeepsCloze() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        String deckId = createDeck(owner, true);
        createCloze(owner, deckId, TEXT, null).andExpect(status().isCreated());

        String copyId = parse(mockMvc.perform(post("/api/v1/decks/{id}/clone", deckId)
                        .header("Authorization", bearerToken(other)))
                .andExpect(status().isCreated())).get("id").asString();
        JsonNode copied = getJson(other, "/api/v1/decks/" + copyId + "/cards").get("content");
        assertThat(copied).hasSize(2);
        assertThat(copied.get(0).get("cardType").asString()).isEqualTo("CLOZE");
        assertThat(copied.get(1).get("clozeIndex").asInt()).isEqualTo(2);
    }

    // ---- helpers -------------------------------------------------------------

    private String createDeck(UUID user, boolean isPublic) throws Exception {
        return parse(mockMvc.perform(post("/api/v1/decks").header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Cloze deck", "description", "",
                                "category", "PHARMACOLOGY", "isPublic", isPublic))))
                .andExpect(status().isCreated())).get("id").asString();
    }

    private String createBasicCard(UUID user, String deckId, String front) throws Exception {
        return parse(mockMvc.perform(post("/api/v1/decks/{id}/cards", deckId).header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("front", front, "back", "Answer", "tags", List.of()))))
                .andExpect(status().isCreated())).get("id").asString();
    }

    private ResultActions createCloze(UUID user, String deckId, String text, String extra) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("text", text);
        if (extra != null) {
            body.put("extra", extra);
        }
        return mockMvc.perform(post("/api/v1/decks/{id}/cloze-cards", deckId).header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions update(UUID user, String cardId, Map<String, Object> body) throws Exception {
        return mockMvc.perform(put("/api/v1/cards/{id}", cardId).header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private JsonNode getJson(UUID user, String url) throws Exception {
        return objectMapper.readTree(mockMvc.perform(get(url).header("Authorization", bearerToken(user)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private JsonNode parse(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
