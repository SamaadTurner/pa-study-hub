package com.pastudyhub.exam.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/**
 * Reporting shared questions, browsing them, and the per-category counts.
 * Each test has a category of its own that no migration or other test uses, so
 * the shared questions of one test never show up in another.
 */
@DisplayName("Shared questions: reports, browsing and counts")
class SharedQuestionsIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QuestionRepository questionRepository;

    @Test
    @DisplayName("A user who had a shared question in a finished exam can report it once, and the author sees it")
    void report_onceAndSeenByAuthor() throws Exception {
        UUID author = UUID.randomUUID();
        UUID student = UUID.randomUUID();
        String questionId = writeQuestion(author, "Pediatrics question", "PEDIATRICS", true);
        String sessionId = takeExam(student, "PEDIATRICS", 1, true);

        report(student, questionId, "WRONG_ANSWER", "Option B is right, not A")
                .andExpect(status().isCreated());
        report(student, questionId, "UNCLEAR", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("You already reported this question"));

        mockMvc.perform(get("/api/v1/exams/{id}/result", sessionId).header("Authorization", bearerToken(student)))
                .andExpect(jsonPath("$.answerDetails[0].source").value("ANOTHER_USER"))
                .andExpect(jsonPath("$.answerDetails[0].reportedByYou").value(true));

        // The author sees the reason and the note, not who sent it
        mockMvc.perform(get("/api/v1/questions/mine").header("Authorization", bearerToken(author)))
                .andExpect(jsonPath("$.content[0].reports", hasSize(1)))
                .andExpect(jsonPath("$.content[0].reports[0].reason").value("WRONG_ANSWER"))
                .andExpect(jsonPath("$.content[0].reports[0].note").value("Option B is right, not A"))
                .andExpect(jsonPath("$.content[0].reports[0].reporterId").doesNotExist())
                .andExpect(jsonPath("$.content[0].hiddenFromShared").value(false));
    }

    @Test
    @DisplayName("Reports are refused from people who never had the question, on your own, on starter and on changed questions")
    void report_refused() throws Exception {
        UUID author = UUID.randomUUID();
        UUID student = UUID.randomUUID();
        String questionId = writeQuestion(author, "Reproductive question", "REPRODUCTIVE", true);

        report(UUID.randomUUID(), questionId, "UNCLEAR", null)
                .andExpect(status().isForbidden());
        report(author, questionId, "UNCLEAR", null)
                .andExpect(status().isBadRequest());

        Question starter = saveStarterQuestion();
        report(student, starter.getId().toString(), "UNCLEAR", null)
                .andExpect(status().isBadRequest());

        // The student had it in an exam, then the author edited it: the old version is retired
        takeExam(student, "REPRODUCTIVE", 1, true);
        mockMvc.perform(put("/api/v1/questions/{id}", questionId).header("Authorization", bearerToken(author))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(questionBody("Better wording", "REPRODUCTIVE", true))))
                .andExpect(status().isOk());
        report(student, questionId, "WRONG_ANSWER", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("The author has changed or removed this question already"));
    }

    @Test
    @DisplayName("With 3 reports a shared question leaves other people's exams, the counts and the browse list, but not the author's")
    void threeReports_hideQuestion() throws Exception {
        UUID author = UUID.randomUUID();
        String questionId = writeQuestion(author, "Emergency question", "EMERGENCY_MEDICINE", true);
        for (int i = 0; i < 3; i++) {
            UUID student = UUID.randomUUID();
            takeExam(student, "EMERGENCY_MEDICINE", 1, true);
            report(student, questionId, "WRONG_ANSWER", null).andExpect(status().isCreated());
        }

        UUID someoneNew = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/exams").header("Authorization", bearerToken(someoneNew))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(examBody("EMERGENCY_MEDICINE", 1, true)))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/api/v1/questions/counts").param("includeShared", "true")
                        .header("Authorization", bearerToken(someoneNew)))
                .andExpect(jsonPath("$.byCategory.EMERGENCY_MEDICINE").doesNotExist());
        mockMvc.perform(get("/api/v1/questions/shared").param("category", "EMERGENCY_MEDICINE")
                        .header("Authorization", bearerToken(someoneNew)))
                .andExpect(jsonPath("$.content", hasSize(0)));

        mockMvc.perform(get("/api/v1/questions/mine").header("Authorization", bearerToken(author)))
                .andExpect(jsonPath("$.content[0].hiddenFromShared").value(true))
                .andExpect(jsonPath("$.content[0].reports", hasSize(3)));
        takeExam(author, "EMERGENCY_MEDICINE", 1, false);
    }

    @Test
    @DisplayName("The browse list has other people's shared questions with answers, never private ones or your own")
    void browse_sharedByOthers() throws Exception {
        UUID writer = UUID.randomUUID();
        UUID reader = UUID.randomUUID();
        writeQuestion(writer, "Shared one", "INFECTIOUS_DISEASE", true);
        writeQuestion(writer, "Shared two", "INFECTIOUS_DISEASE", true);
        writeQuestion(writer, "Private", "INFECTIOUS_DISEASE", false);
        writeQuestion(reader, "The reader's own", "INFECTIOUS_DISEASE", true);

        MvcResult result = mockMvc.perform(get("/api/v1/questions/shared").param("category", "INFECTIOUS_DISEASE")
                        .header("Authorization", bearerToken(reader)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].answerOptions[0].isCorrect").exists())
                .andExpect(jsonPath("$.content[0].explanation").value("Why the right answer is right."))
                .andReturn();
        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString()).get("content");
        List<String> stems = List.of(content.get(0).get("stem").asText(), content.get(1).get("stem").asText());
        assertThat(stems).containsExactlyInAnyOrder("Shared one", "Shared two");
        assertThat(content.get(0).has("ownerId")).isFalse();
    }

    @Test
    @DisplayName("The counts follow what an exam can pick: your own always, others' shared ones only when asked for")
    void counts_followExamPool() throws Exception {
        UUID writer = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        writeQuestion(writer, "Hematology 1", "HEMATOLOGY", false);
        writeQuestion(writer, "Hematology 2", "HEMATOLOGY", true);

        mockMvc.perform(get("/api/v1/questions/counts").header("Authorization", bearerToken(writer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byCategory.HEMATOLOGY").value(2));
        mockMvc.perform(get("/api/v1/questions/counts").header("Authorization", bearerToken(other)))
                .andExpect(jsonPath("$.byCategory.HEMATOLOGY").doesNotExist());
        mockMvc.perform(get("/api/v1/questions/counts").param("includeShared", "true")
                        .header("Authorization", bearerToken(other)))
                .andExpect(jsonPath("$.byCategory.HEMATOLOGY").value(1));
    }

    // ---- helpers -------------------------------------------------------------

    private Map<String, Object> questionBody(String stem, String category, boolean shared) {
        Map<String, Object> body = new HashMap<>();
        body.put("stem", stem);
        body.put("category", category);
        body.put("taskArea", "MOST_LIKELY_DIAGNOSIS");
        body.put("difficulty", "MEDIUM");
        body.put("explanation", "Why the right answer is right.");
        body.put("isPublic", shared);
        body.put("answerOptions", List.of(Map.of("text", "Right", "isCorrect", true),
                Map.of("text", "Wrong", "isCorrect", false)));
        return body;
    }

    private String writeQuestion(UUID author, String stem, String category, boolean shared) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/questions").header("Authorization", bearerToken(author))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(questionBody(stem, category, shared))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private Question saveStarterQuestion() {
        Question question = Question.builder()
                .stem("Starter question for reports")
                .category(QuestionCategory.ANATOMY)
                .taskArea(TaskArea.BASIC_SCIENCE)
                .difficulty(DifficultyLevel.EASY)
                .explanation("Explanation")
                .build();
        for (int i = 0; i < 2; i++) {
            question.getAnswerOptions().add(AnswerOption.builder()
                    .question(question).text(i == 0 ? "Right" : "Wrong").isCorrect(i == 0).orderIndex(i).build());
        }
        return questionRepository.save(question);
    }

    private String examBody(String category, int count, boolean includeShared) {
        return "{\"questionCount\": " + count + ", \"categoryFilter\": \"" + category
                + "\", \"timeLimitMinutes\": 0, \"includeSharedQuestions\": " + includeShared + "}";
    }

    // Starts an exam, answers every question with its first option and finishes it
    private String takeExam(UUID user, String category, int count, boolean includeShared) throws Exception {
        MvcResult started = mockMvc.perform(post("/api/v1/exams").header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(examBody(category, count, includeShared)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode exam = objectMapper.readTree(started.getResponse().getContentAsString());
        String sessionId = exam.get("id").asText();
        for (JsonNode question : exam.get("questions")) {
            mockMvc.perform(post("/api/v1/exams/{id}/questions/{q}/answer", sessionId, question.get("id").asText())
                            .header("Authorization", bearerToken(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"selectedOptionId\": \"" + question.get("answerOptions").get(0).get("id").asText()
                                    + "\", \"timeSpentSeconds\": 5}"))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/v1/exams/{id}/complete", sessionId).header("Authorization", bearerToken(user)))
                .andExpect(status().isOk());
        return sessionId;
    }

    private ResultActions report(UUID user, String questionId, String reason, String note) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("reason", reason);
        if (note != null) {
            body.put("note", note);
        }
        return mockMvc.perform(post("/api/v1/questions/{id}/reports", questionId)
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }
}
