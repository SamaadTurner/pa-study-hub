package com.pastudyhub.exam.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pastudyhub.BaseIntegrationTest;
import com.pastudyhub.exam.model.AnswerOption;
import com.pastudyhub.exam.model.DifficultyLevel;
import com.pastudyhub.exam.model.Question;
import com.pastudyhub.exam.model.QuestionCategory;
import com.pastudyhub.exam.repository.AnswerOptionRepository;
import com.pastudyhub.exam.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Takes practice exams through the real endpoints, the way the exam page does.
 *
 * <p>Each test starts its own exam with its own user, so the tests don't depend on
 * each other or on the order they run in. The exams only use dermatology questions,
 * which this class adds itself, so it knows how many there are on both H2 and
 * PostgreSQL (the migrations load questions for other categories).
 */
@DisplayName("Taking a practice exam")
class ExamFlowIntegrationTest extends BaseIntegrationTest {

    private static final int QUESTIONS_AVAILABLE = 4;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AnswerOptionRepository answerOptionRepository;

    @BeforeEach
    void addDermatologyQuestions() {
        if (!questionRepository.findByCategoryAndIsActiveTrue(QuestionCategory.DERMATOLOGY).isEmpty()) {
            return;
        }
        for (int i = 1; i <= QUESTIONS_AVAILABLE; i++) {
            Question question = Question.builder()
                    .stem("Dermatology test question " + i)
                    .category(QuestionCategory.DERMATOLOGY)
                    .difficulty(DifficultyLevel.MEDIUM)
                    .explanation("Explanation for question " + i)
                    .isActive(true)
                    .build();

            // The first option is the right one
            for (int option = 0; option < 4; option++) {
                question.getAnswerOptions().add(AnswerOption.builder()
                        .question(question)
                        .text("Question " + i + " option " + option)
                        .isCorrect(option == 0)
                        .orderIndex(option)
                        .build());
            }
            questionRepository.save(question);
        }
    }

    // ---- starting and loading an exam --------------------------------------

    @Test
    @DisplayName("A new exam comes back with its questions and no answers given away")
    void startExam_returnsQuestions() throws Exception {
        UUID user = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/exams")
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startJson(3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.startedAt").isString())
                .andExpect(jsonPath("$.questionCount").value(3))
                .andExpect(jsonPath("$.answeredCount").value(0))
                .andExpect(jsonPath("$.questions", hasSize(3)))
                .andExpect(jsonPath("$.questions[0].answerOptions", hasSize(4)))
                .andExpect(jsonPath("$.questions[0].selectedOptionId").doesNotExist())
                // nothing that tells the user which option is right
                .andExpect(jsonPath("$.questions[0].answerOptions[0].isCorrect").doesNotExist())
                .andExpect(jsonPath("$.questions[0].explanation").doesNotExist());
    }

    @Test
    @DisplayName("Loading the exam again gives the same questions in the same order")
    void loadExam_hasSameQuestions() throws Exception {
        UUID user = UUID.randomUUID();
        JsonNode started = startExam(user, 4);

        JsonNode loaded = loadExam(user, started.get("id").asText());

        // This used to come back empty, because the picked questions were never saved
        assertThat(questionIds(loaded)).hasSize(4);
        assertThat(questionIds(loaded)).isEqualTo(questionIds(started));
        assertThat(loaded.get("answeredCount").asInt()).isZero();
    }

    @Test
    @DisplayName("After one answer the exam still has every question, with the pick on the answered one")
    void answerOne_keepsAllQuestions() throws Exception {
        UUID user = UUID.randomUUID();
        JsonNode started = startExam(user, 3);
        String sessionId = started.get("id").asText();
        JsonNode second = started.get("questions").get(1);
        String pickedOption = second.get("answerOptions").get(2).get("id").asText();

        // The answer response is the whole exam again, not just the answered question
        answer(user, sessionId, second.get("id").asText(), pickedOption)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions", hasSize(3)))
                .andExpect(jsonPath("$.answeredCount").value(1));

        JsonNode loaded = loadExam(user, sessionId);
        assertThat(questionIds(loaded)).isEqualTo(questionIds(started));
        assertThat(loaded.get("answeredCount").asInt()).isEqualTo(1);
        assertThat(loaded.get("questions").get(1).get("selectedOptionId").asText()).isEqualTo(pickedOption);
        assertThat(loaded.get("questions").get(0).hasNonNull("selectedOptionId")).isFalse();
        assertThat(loaded.get("questions").get(2).hasNonNull("selectedOptionId")).isFalse();
    }

    @Test
    @DisplayName("Changing an answer replaces the old pick instead of counting twice")
    void changeAnswer_replacesThePick() throws Exception {
        UUID user = UUID.randomUUID();
        JsonNode started = startExam(user, 2);
        String sessionId = started.get("id").asText();
        JsonNode first = started.get("questions").get(0);
        String firstPick = first.get("answerOptions").get(1).get("id").asText();
        String secondPick = first.get("answerOptions").get(3).get("id").asText();

        answer(user, sessionId, first.get("id").asText(), firstPick).andExpect(status().isOk());
        answer(user, sessionId, first.get("id").asText(), secondPick).andExpect(status().isOk());

        JsonNode loaded = loadExam(user, sessionId);
        assertThat(loaded.get("answeredCount").asInt()).isEqualTo(1);
        assertThat(loaded.get("questions").get(0).get("selectedOptionId").asText()).isEqualTo(secondPick);
    }

    @Test
    @DisplayName("Answering does not reveal whether the pick was right")
    void answering_doesNotRevealTheRightAnswer() throws Exception {
        UUID user = UUID.randomUUID();
        JsonNode started = startExam(user, 1);
        JsonNode question = started.get("questions").get(0);

        answer(user, started.get("id").asText(), question.get("id").asText(), correctOptionOf(question))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].selectedOptionId").isString())
                .andExpect(jsonPath("$.questions[0].answerOptions[0].isCorrect").doesNotExist())
                .andExpect(jsonPath("$.questions[0].explanation").doesNotExist());
    }

    @Test
    @DisplayName("Another user can't load someone else's exam")
    void otherUser_cantLoadExam() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        JsonNode started = startExam(owner, 2);

        mockMvc.perform(get("/api/v1/exams/{sessionId}", started.get("id").asText())
                        .header("Authorization", bearerToken(otherUser)))
                .andExpect(status().isNotFound());
    }

    // ---- answers that don't fit the exam ------------------------------------

    @Test
    @DisplayName("A question that is not in the exam can't be answered")
    void answerQuestionFromOutsideTheExam_returns400() throws Exception {
        UUID user = UUID.randomUUID();
        JsonNode started = startExam(user, 2);
        String sessionId = started.get("id").asText();

        // Two of the four dermatology questions were picked, so find one that was not
        List<String> picked = questionIds(started);
        Question outside = questionRepository.findByCategoryAndIsActiveTrue(QuestionCategory.DERMATOLOGY).stream()
                .filter(question -> !picked.contains(question.getId().toString()))
                .findFirst()
                .orElseThrow();
        String itsRightOption = answerOptionRepository.findByQuestionId(outside.getId()).stream()
                .filter(AnswerOption::isCorrect)
                .findFirst()
                .orElseThrow()
                .getId()
                .toString();

        answer(user, sessionId, outside.getId().toString(), itsRightOption)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("That question is not part of this exam"));

        assertThat(loadExam(user, sessionId).get("answeredCount").asInt()).isZero();
    }

    @Test
    @DisplayName("An option from a different question is rejected")
    void answerWithOptionOfAnotherQuestion_returns400() throws Exception {
        UUID user = UUID.randomUUID();
        JsonNode started = startExam(user, 2);
        String sessionId = started.get("id").asText();
        JsonNode first = started.get("questions").get(0);
        JsonNode second = started.get("questions").get(1);

        // The right option of question two, sent as the answer to question one.
        // This used to be saved and counted as a correct answer.
        answer(user, sessionId, first.get("id").asText(), correctOptionOf(second))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("That answer option does not belong to this question"));

        assertThat(loadExam(user, sessionId).get("answeredCount").asInt()).isZero();
    }

    @Test
    @DisplayName("An option that doesn't exist is rejected")
    void answerWithUnknownOption_returns400() throws Exception {
        UUID user = UUID.randomUUID();
        JsonNode started = startExam(user, 1);
        String questionId = started.get("questions").get(0).get("id").asText();

        answer(user, started.get("id").asText(), questionId, UUID.randomUUID().toString())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("An answer without an option is rejected")
    void answerWithoutOption_returns400() throws Exception {
        UUID user = UUID.randomUUID();
        JsonNode started = startExam(user, 1);
        String questionId = started.get("questions").get(0).get("id").asText();

        mockMvc.perform(post("/api/v1/exams/{sessionId}/questions/{questionId}/answer",
                        started.get("id").asText(), questionId)
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"timeSpentSeconds\": 5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Another user can't answer in someone else's exam")
    void otherUser_cantAnswer() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        JsonNode started = startExam(owner, 1);
        JsonNode question = started.get("questions").get(0);

        answer(otherUser, started.get("id").asText(), question.get("id").asText(), correctOptionOf(question))
                .andExpect(status().isNotFound());

        assertThat(loadExam(owner, started.get("id").asText()).get("answeredCount").asInt()).isZero();
    }

    // ---- helpers -----------------------------------------------------------

    // The same body the exam setup page sends, limited to the questions added above
    private String startJson(int questionCount) {
        return "{\"questionCount\": " + questionCount
                + ", \"categoryFilter\": \"DERMATOLOGY\", \"timeLimitMinutes\": 0}";
    }

    private JsonNode startExam(UUID user, int questionCount) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/exams")
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startJson(questionCount)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode loadExam(UUID user, String sessionId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/exams/{sessionId}", sessionId)
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private ResultActions answer(UUID user, String sessionId, String questionId, String optionId) throws Exception {
        String body = "{\"selectedOptionId\": \"" + optionId + "\", \"timeSpentSeconds\": 20}";

        return mockMvc.perform(post("/api/v1/exams/{sessionId}/questions/{questionId}/answer",
                        sessionId, questionId)
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private List<String> questionIds(JsonNode exam) {
        List<String> ids = new ArrayList<>();
        for (JsonNode question : exam.get("questions")) {
            ids.add(question.get("id").asText());
        }
        return ids;
    }

    // The exam never says which option is right, so the tests look it up in the database
    private String correctOptionOf(JsonNode question) {
        UUID questionId = UUID.fromString(question.get("id").asText());
        return answerOptionRepository.findByQuestionId(questionId).stream()
                .filter(AnswerOption::isCorrect)
                .findFirst()
                .orElseThrow()
                .getId()
                .toString();
    }

    private String wrongOptionOf(JsonNode question) {
        UUID questionId = UUID.fromString(question.get("id").asText());
        return answerOptionRepository.findByQuestionId(questionId).stream()
                .filter(option -> !option.isCorrect())
                .findFirst()
                .orElseThrow()
                .getId()
                .toString();
    }
}
