package com.pastudyhub.exam.integration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.pastudyhub.BaseIntegrationTest;
import com.pastudyhub.exam.model.AnswerOption;
import com.pastudyhub.exam.model.DifficultyLevel;
import com.pastudyhub.exam.model.Question;
import com.pastudyhub.exam.model.QuestionCategory;
import com.pastudyhub.exam.model.TaskArea;
import com.pastudyhub.exam.repository.QuestionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Questions that users write themselves: the /questions endpoints, and which
 * questions an exam may pick.
 *
 * <p>Questions written here are in musculoskeletal, except the exam pool test,
 * which has gastroenterology to itself. No migration or other test adds questions
 * in those two categories, so the pool sizes are known.
 */
@DisplayName("Questions written by users")
class WrittenQuestionsIntegrationTest extends BaseIntegrationTest {

    private static final String CATEGORY = "MUSCULOSKELETAL";
    private static final String POOL_CATEGORY = "GASTROENTEROLOGY";
    private static final String RIGHT = "Right answer";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QuestionRepository questionRepository;

    // ---- writing and reading -------------------------------------------------

    @Test
    @DisplayName("A written question is saved with its options in order and comes back with the right answer")
    void create_savesQuestion() throws Exception {
        UUID author = UUID.randomUUID();
        Map<String, Object> body = questionBody("Which test confirms a scaphoid fracture?", CATEGORY, false);
        body.put("clinicalVignette", "  A 20-year-old fell on an outstretched hand.  ");

        mockMvc.perform(post("/api/v1/questions")
                        .header("Authorization", bearerToken(author))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.stem").value("Which test confirms a scaphoid fracture?"))
                .andExpect(jsonPath("$.clinicalVignette").value("A 20-year-old fell on an outstretched hand."))
                .andExpect(jsonPath("$.taskArea").value("DIAGNOSTIC_STUDIES"))
                .andExpect(jsonPath("$.difficulty").value("MEDIUM"))
                .andExpect(jsonPath("$.isPublic").value(false))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.answerOptions", hasSize(4)))
                .andExpect(jsonPath("$.answerOptions[0].text").value("Wrong answer 1"))
                .andExpect(jsonPath("$.answerOptions[2].text").value(RIGHT))
                .andExpect(jsonPath("$.answerOptions[2].isCorrect").value(true))
                .andExpect(jsonPath("$.answerOptions[0].isCorrect").value(false));
    }

    @Test
    @DisplayName("The list has only the user's own questions, last changed first")
    void listMine_onlyOwnQuestions() throws Exception {
        UUID author = UUID.randomUUID();
        createQuestion(author, questionBody("First question", CATEGORY, false));
        createQuestion(author, questionBody("Second question", CATEGORY, true));
        createQuestion(UUID.randomUUID(), questionBody("Someone else's question", CATEGORY, true));

        MvcResult result = mockMvc.perform(get("/api/v1/questions/mine")
                        .header("Authorization", bearerToken(author)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andReturn();

        // Saved in the same clock tick the two dates can be equal, so compare them
        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString()).get("content");
        String firstUpdated = content.get(0).get("updatedAt").asString();
        String secondUpdated = content.get(1).get("updatedAt").asString();
        assertThat(firstUpdated.compareTo(secondUpdated)).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Invalid questions are refused with a reason")
    void create_invalidQuestions() throws Exception {
        UUID author = UUID.randomUUID();

        Map<String, Object> noRightAnswer = questionBody("No right answer", CATEGORY, false);
        noRightAnswer.put("answerOptions", List.of(option("A", false), option("B", false)));
        expectBadRequest(author, noRightAnswer, "Mark exactly one answer option as right");

        Map<String, Object> twoRightAnswers = questionBody("Two right answers", CATEGORY, false);
        twoRightAnswers.put("answerOptions", List.of(option("A", true), option("B", true)));
        expectBadRequest(author, twoRightAnswers, "Mark exactly one answer option as right");

        Map<String, Object> sameText = questionBody("Same text twice", CATEGORY, false);
        sameText.put("answerOptions", List.of(option("Aspirin", true), option(" aspirin ", false)));
        expectBadRequest(author, sameText, "Two answer options have the same text");

        Map<String, Object> oneOption = questionBody("Only one option", CATEGORY, false);
        oneOption.put("answerOptions", List.of(option("A", true)));
        mockMvc.perform(post("/api/v1/questions")
                        .header("Authorization", bearerToken(author))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oneOption)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.answerOptions").value("Pick between 2 and 5 answer options"));

        Map<String, Object> blankOption = questionBody("Blank option", CATEGORY, false);
        blankOption.put("answerOptions", List.of(option("A", true), option("  ", false)));
        mockMvc.perform(post("/api/v1/questions")
                        .header("Authorization", bearerToken(author))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankOption)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors['answerOptions[1].text']").value("Every answer option needs text"));

        Map<String, Object> noTaskArea = questionBody("No task area", CATEGORY, false);
        noTaskArea.remove("taskArea");
        noTaskArea.put("explanation", "");
        mockMvc.perform(post("/api/v1/questions")
                        .header("Authorization", bearerToken(author))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noTaskArea)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.taskArea").value("Task area is required"))
                .andExpect(jsonPath("$.fieldErrors.explanation").value("The explanation is required"));

        mockMvc.perform(get("/api/v1/questions/mine")
                        .header("Authorization", bearerToken(author)))
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("Only the author can open, edit or delete a question, and nobody can change a starter question")
    void otherUsers_areRefused() throws Exception {
        UUID author = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        // Shared, so a stranger may use it in exams, but still not open or change it
        String questionId = createQuestion(author, questionBody("Shared question", CATEGORY, true));
        String body = objectMapper.writeValueAsString(questionBody("Changed by a stranger", CATEGORY, true));

        mockMvc.perform(get("/api/v1/questions/{id}", questionId)
                        .header("Authorization", bearerToken(stranger)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("You can only open and change questions you wrote"));
        mockMvc.perform(put("/api/v1/questions/{id}", questionId)
                        .header("Authorization", bearerToken(stranger))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/questions/{id}", questionId)
                        .header("Authorization", bearerToken(stranger)))
                .andExpect(status().isForbidden());

        // A starter question has no author
        Question starter = saveStarterQuestion();
        mockMvc.perform(put("/api/v1/questions/{id}", starter.getId())
                        .header("Authorization", bearerToken(author))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/questions/{id}", UUID.randomUUID())
                        .header("Authorization", bearerToken(author)))
                .andExpect(status().isNotFound());

        // The author's question is unchanged
        mockMvc.perform(get("/api/v1/questions/{id}", questionId)
                        .header("Authorization", bearerToken(author)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stem").value("Shared question"));
    }

    // ---- editing and deleting ------------------------------------------------

    @Test
    @DisplayName("A question never used in an exam is edited in place")
    void update_unusedQuestion_inPlace() throws Exception {
        UUID author = UUID.randomUUID();
        String questionId = createQuestion(author, questionBody("Old wording", CATEGORY, false));

        Map<String, Object> changed = questionBody("New wording", CATEGORY, true);
        changed.put("answerOptions", List.of(option("Yes", true), option("No", false)));
        changed.put("taskArea", "BASIC_SCIENCE");

        mockMvc.perform(put("/api/v1/questions/{id}", questionId)
                        .header("Authorization", bearerToken(author))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(questionId))
                .andExpect(jsonPath("$.stem").value("New wording"))
                .andExpect(jsonPath("$.taskArea").value("BASIC_SCIENCE"))
                .andExpect(jsonPath("$.isPublic").value(true))
                .andExpect(jsonPath("$.answerOptions", hasSize(2)))
                .andExpect(jsonPath("$.answerOptions[0].text").value("Yes"));

        mockMvc.perform(get("/api/v1/questions/mine")
                        .header("Authorization", bearerToken(author)))
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("Editing a question used in an exam saves a new one, and the old results still show the old one")
    void update_usedQuestion_keepsOldResults() throws Exception {
        UUID author = UUID.randomUUID();
        String questionId = createQuestion(author, questionBody("Wording in the exam", CATEGORY, false));
        String sessionId = takeExamWithRightAnswers(author, CATEGORY, 1);

        Map<String, Object> changed = questionBody("Better wording", CATEGORY, false);
        MvcResult result = mockMvc.perform(put("/api/v1/questions/{id}", questionId)
                        .header("Authorization", bearerToken(author))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(not(questionId)))
                .andExpect(jsonPath("$.stem").value("Better wording"))
                .andReturn();
        String newId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asString();

        // The list shows only the new version, and the old id is gone for editing
        mockMvc.perform(get("/api/v1/questions/mine")
                        .header("Authorization", bearerToken(author)))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(newId));
        mockMvc.perform(get("/api/v1/questions/{id}", questionId)
                        .header("Authorization", bearerToken(author)))
                .andExpect(status().isNotFound());

        // The finished exam still has the question as it was asked, with its score
        mockMvc.perform(get("/api/v1/exams/{sessionId}/result", sessionId)
                        .header("Authorization", bearerToken(author)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rawScore").value(1))
                .andExpect(jsonPath("$.answerDetails[0].stem").value("Wording in the exam"))
                .andExpect(jsonPath("$.answerDetails[0].isCorrect").value(true));

        // A new exam picks the new version
        String newSession = startExam(author, CATEGORY, 1, false);
        mockMvc.perform(get("/api/v1/exams/{sessionId}", newSession)
                        .header("Authorization", bearerToken(author)))
                .andExpect(jsonPath("$.questions[0].stem").value("Better wording"));
    }

    @Test
    @DisplayName("Deleting removes a question from the list and from new exams, and old results keep it")
    void delete_removesQuestion() throws Exception {
        UUID author = UUID.randomUUID();
        String used = createQuestion(author, questionBody("Used once", CATEGORY, false));
        String sessionId = takeExamWithRightAnswers(author, CATEGORY, 1);
        String unused = createQuestion(author, questionBody("Never used", CATEGORY, false));

        for (String id : List.of(unused, used)) {
            mockMvc.perform(delete("/api/v1/questions/{id}", id)
                            .header("Authorization", bearerToken(author)))
                    .andExpect(status().isNoContent());
        }

        mockMvc.perform(get("/api/v1/questions/mine")
                        .header("Authorization", bearerToken(author)))
                .andExpect(jsonPath("$.content", hasSize(0)));
        // The unused one is gone, the used one is kept for the old exam
        assertThat(questionRepository.findById(UUID.fromString(unused))).isEmpty();
        assertThat(questionRepository.findById(UUID.fromString(used))).isPresent();

        mockMvc.perform(get("/api/v1/exams/{sessionId}/result", sessionId)
                        .header("Authorization", bearerToken(author)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerDetails", hasSize(1)))
                .andExpect(jsonPath("$.answerDetails[0].stem").value("Used once"))
                .andExpect(jsonPath("$.rawScore").value(1));

        mockMvc.perform(post("/api/v1/exams")
                        .header("Authorization", bearerToken(author))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(examBody(CATEGORY, 1, false)))
                .andExpect(status().isUnprocessableContent());
    }

    // ---- which questions an exam can pick -----------------------------------

    @Test
    @DisplayName("Exams use the user's own questions, and other users' shared ones only when asked for")
    void examPool_ownAndShared() throws Exception {
        UUID author = UUID.randomUUID();
        UUID student = UUID.randomUUID();
        String sharedId = createQuestion(author, questionBody("Shared by the author", POOL_CATEGORY, true));
        createQuestion(author, questionBody("Private to the author", POOL_CATEGORY, false));
        String ownId = createQuestion(student, questionBody("Written by the student", POOL_CATEGORY, false));

        // Without shared questions: only the student's own question
        mockMvc.perform(post("/api/v1/exams")
                        .header("Authorization", bearerToken(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(examBody(POOL_CATEGORY, 2, false)))
                .andExpect(status().isUnprocessableContent());
        String ownOnly = startExam(student, POOL_CATEGORY, 1, false);
        assertThat(questionIdsOf(student, ownOnly)).containsExactly(ownId);

        // With shared questions: the shared one too, never the private one
        mockMvc.perform(post("/api/v1/exams")
                        .header("Authorization", bearerToken(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(examBody(POOL_CATEGORY, 3, true)))
                .andExpect(status().isUnprocessableContent());
        String withShared = startExam(student, POOL_CATEGORY, 2, true);
        assertThat(questionIdsOf(student, withShared)).containsExactlyInAnyOrder(ownId, sharedId);

        // The results say who wrote each question
        answerAllRight(student, withShared);
        MvcResult result = mockMvc.perform(post("/api/v1/exams/{sessionId}/complete", withShared)
                        .header("Authorization", bearerToken(student)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode details = objectMapper.readTree(result.getResponse().getContentAsString()).get("answerDetails");
        for (JsonNode detail : details) {
            String expected = detail.get("questionId").asString().equals(ownId) ? "YOU" : "ANOTHER_USER";
            assertThat(detail.get("source").asString()).isEqualTo(expected);
        }

        // The author's own exam uses both of the author's questions, shared or not
        String authorsExam = startExam(author, POOL_CATEGORY, 2, false);
        assertThat(questionIdsOf(author, authorsExam)).hasSize(2);
    }

    // ---- helpers -------------------------------------------------------------

    private Map<String, Object> questionBody(String stem, String category, boolean isPublic) {
        Map<String, Object> body = new HashMap<>();
        body.put("stem", stem);
        body.put("category", category);
        body.put("taskArea", "DIAGNOSTIC_STUDIES");
        body.put("difficulty", "MEDIUM");
        body.put("explanation", "Why the right answer is right.");
        body.put("isPublic", isPublic);
        body.put("answerOptions", List.of(
                option("Wrong answer 1", false),
                option("Wrong answer 2", false),
                option(RIGHT, true),
                option("Wrong answer 3", false)));
        return body;
    }

    private Map<String, Object> option(String text, boolean isCorrect) {
        return Map.of("text", text, "isCorrect", isCorrect);
    }

    private String createQuestion(UUID author, Map<String, Object> body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/questions")
                        .header("Authorization", bearerToken(author))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asString();
    }

    private void expectBadRequest(UUID author, Map<String, Object> body, String detail) throws Exception {
        mockMvc.perform(post("/api/v1/questions")
                        .header("Authorization", bearerToken(author))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(detail));
    }

    private Question saveStarterQuestion() {
        Question question = Question.builder()
                .stem("Starter question")
                .category(QuestionCategory.ANATOMY)
                .taskArea(TaskArea.BASIC_SCIENCE)
                .difficulty(DifficultyLevel.EASY)
                .explanation("Explanation")
                .build();
        // Options as well, in case an exam over all categories picks it
        for (int i = 0; i < 2; i++) {
            question.getAnswerOptions().add(AnswerOption.builder()
                    .question(question)
                    .text(i == 0 ? RIGHT : "Wrong answer")
                    .isCorrect(i == 0)
                    .orderIndex(i)
                    .build());
        }
        return questionRepository.save(question);
    }

    private String examBody(String category, int count, boolean includeShared) {
        return "{\"questionCount\": " + count + ", \"categoryFilter\": \"" + category
                + "\", \"timeLimitMinutes\": 0, \"includeSharedQuestions\": " + includeShared + "}";
    }

    private String startExam(UUID user, String category, int count, boolean includeShared) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/exams")
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(examBody(category, count, includeShared)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asString();
    }

    private JsonNode getExam(UUID user, String sessionId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/exams/{sessionId}", sessionId)
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private List<String> questionIdsOf(UUID user, String sessionId) throws Exception {
        List<String> ids = new ArrayList<>();
        for (JsonNode question : getExam(user, sessionId).get("questions")) {
            ids.add(question.get("id").asString());
        }
        return ids;
    }

    private void answerAllRight(UUID user, String sessionId) throws Exception {
        for (JsonNode question : getExam(user, sessionId).get("questions")) {
            String optionId = null;
            for (JsonNode option : question.get("answerOptions")) {
                if (option.get("text").asString().equals(RIGHT)) {
                    optionId = option.get("id").asString();
                }
            }
            mockMvc.perform(post("/api/v1/exams/{sessionId}/questions/{questionId}/answer",
                                    sessionId, question.get("id").asString())
                            .header("Authorization", bearerToken(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"selectedOptionId\": \"" + optionId + "\", \"timeSpentSeconds\": 10}"))
                    .andExpect(status().isOk());
        }
    }

    private String takeExamWithRightAnswers(UUID user, String category, int count) throws Exception {
        String sessionId = startExam(user, category, count, false);
        answerAllRight(user, sessionId);
        mockMvc.perform(post("/api/v1/exams/{sessionId}/complete", sessionId)
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk());
        return sessionId;
    }
}
