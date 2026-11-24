package com.pastudyhub.user.integration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.pastudyhub.BaseIntegrationTest;
import com.pastudyhub.user.time.UserClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Time zones: today is the user's own day")
class TimeZoneIntegrationTest extends BaseIntegrationTest {

    // 25 hours apart, so their dates always differ, whenever the test runs
    private static final String KIRITIMATI = "Pacific/Kiritimati";   // UTC+14
    private static final String PAGO_PAGO = "Pacific/Pago_Pago";     // UTC-11

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("A review counts on the reviewer's own calendar day: streak, goal, chart and due date")
    void reviewCountsOnTheUsersDay() throws Exception {
        for (String zone : new String[] {KIRITIMATI, PAGO_PAGO}) {
            UUID user = UUID.randomUUID();
            LocalDate today = LocalDate.now(ZoneId.of(zone));
            String cardId = createCard(user, zone);

            JsonNode review = json(mockMvc.perform(post("/api/v1/cards/{id}/review", cardId)
                            .header("Authorization", bearerToken(user))
                            .header(UserClock.HEADER, zone)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"quality\": 4}"))
                    .andExpect(status().isOk()));
            int interval = review.get("interval").asInt();
            assertThat(review.get("nextReviewDate").asString()).isEqualTo(today.plusDays(interval).toString());

            // A later request without the header still uses the zone the browser sent
            JsonNode dashboard = json(mockMvc.perform(get("/api/v1/progress/dashboard")
                            .header("Authorization", bearerToken(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentStreak").value(1))
                    .andExpect(jsonPath("$.todayGoal.cardsReviewedToday").value(1)));
            JsonNode days = dashboard.get("last30DaysActivity");
            JsonNode last = days.get(days.size() - 1);
            assertThat(last.get("date").asString()).as(zone).isEqualTo(today.toString());
            assertThat(last.get("cardsReviewed").asInt()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("The zone is saved on the account and shown on the profile; a made-up zone is ignored")
    void zoneIsSaved() throws Exception {
        TestAccounts.Account account = TestAccounts.register(mockMvc, objectMapper);

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", account.authorization())
                        .header(UserClock.HEADER, "America/Los_Angeles"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", account.authorization())
                        .header(UserClock.HEADER, "Mars/Olympus_Mons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeZone").value("America/Los_Angeles"));

        String saved = jdbcTemplate.queryForObject("SELECT time_zone FROM users WHERE email = ?", String.class,
                account.email());
        assertThat(saved).isEqualTo("America/Los_Angeles");
        // A fresh clock (as after a restart) reads it from the database
        UUID userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", UUID.class, account.email());
        assertThat(new UserClock(jdbcTemplate).zoneOf(userId)).isEqualTo(ZoneId.of("America/Los_Angeles"));
    }

    private String createCard(UUID user, String zone) throws Exception {
        String deckId = json(mockMvc.perform(post("/api/v1/decks").header("Authorization", bearerToken(user))
                        .header(UserClock.HEADER, zone)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Zones", "description", "",
                                "category", "CARDIOLOGY", "isPublic", false))))
                .andExpect(status().isCreated())).get("id").asString();
        return json(mockMvc.perform(post("/api/v1/decks/{id}/cards", deckId).header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("front", "Q", "back", "A"))))
                .andExpect(status().isCreated())).get("id").asString();
    }

    private JsonNode json(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
