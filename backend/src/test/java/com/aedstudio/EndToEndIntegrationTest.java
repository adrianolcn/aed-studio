package com.aedstudio.controller;

import com.aedstudio.dto.LoginRequest;
import com.aedstudio.dto.RegisterRequest;
import com.aedstudio.dto.TopicVisitRequest;
import com.aedstudio.dto.XpAwardRequest;
import com.aedstudio.model.User;
import com.aedstudio.repository.ExerciseAttemptRepository;
import com.aedstudio.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Integração ponta a ponta")
class EndToEndIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired ExerciseAttemptRepository exerciseAttemptRepository;

    @Test
    @DisplayName("register -> login -> me -> progress funciona com contrato oficial")
    void registerLoginMeProgress() throws Exception {
        RegisterRequest reg = new RegisterRequest("e2euser", "e2e@aedstudio.com", "Senha123!", "E2E User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value(reg.getEmail()));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(reg.getEmail(), reg.getPassword()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asText();

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(reg.getEmail()));

        mockMvc.perform(get("/api/progress").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTopics").value(20))
                .andExpect(jsonPath("$.topicsVisited").value(0))
                .andExpect(jsonPath("$.progressPercent").value(0))
                .andExpect(jsonPath("$.topicStates.algoritmos.state").value("AVAILABLE"))
                .andExpect(jsonPath("$.topicStates.tad.state").value("LOCKED"))
                .andExpect(jsonPath("$.level.level").value(1));
    }

    @Test
    @DisplayName("login -> estudar -> responder -> ganhar XP -> completar tópico")
    void activeLearningProgressFlow() throws Exception {
        String token = registerAndExtract("flowuser", "flow@aedstudio.com").get("accessToken").asText();

        mockMvc.perform(post("/api/progress/visit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TopicVisitRequest("algoritmos"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topicsVisited").value(1))
                .andExpect(jsonPath("$.totalXp").value(5))
                .andExpect(jsonPath("$.earnedEventKeys", hasItem("visit_algoritmos")));

        mockMvc.perform(get("/api/learning/topics/algoritmos/exercises")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("algoritmos-check"));

        mockMvc.perform(post("/api/learning/attempts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exerciseId\":\"algoritmos-check\",\"answer\":\"B\",\"timeSpentSeconds\":30}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.awarded").value(35))
                .andExpect(jsonPath("$.progress.totalXp").value(40))
                .andExpect(jsonPath("$.progress.topicsCompleted").value(1))
                .andExpect(jsonPath("$.progress.topics.algoritmos").value("COMPLETED"))
                .andExpect(jsonPath("$.progress.topicStates.tad.state").value("AVAILABLE"));

        mockMvc.perform(get("/api/progress").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalXp").value(40))
                .andExpect(jsonPath("$.topicsVisited").value(1))
                .andExpect(jsonPath("$.topicsCompleted").value(1))
                .andExpect(jsonPath("$.badges[0].id").value("first-topic"));
    }

    @Test
    @DisplayName("refresh rotacionado permite novo access token e rejeita refresh antigo")
    void refreshRotationFlow() throws Exception {
        JsonNode auth = registerAndExtract("refreshuser", "refresh@aedstudio.com");
        String firstRefresh = auth.get("refreshToken").asText();

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + firstRefresh + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        String newAccess = objectMapper.readTree(refreshResult.getResponse().getContentAsString()).get("accessToken").asText();

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + newAccess))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("refresh@aedstudio.com"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + firstRefresh + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("catálogo público usa os mesmos IDs aceitos pelo progresso")
    void catalogConsistency() throws Exception {
        mockMvc.perform(get("/api/catalog/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTopics").value(20))
                .andExpect(jsonPath("$.tracks[0].id").value("fundamentos"))
                .andExpect(jsonPath("$.topics[0].id").value("algoritmos"))
                .andExpect(jsonPath("$.topics[19].id").value("guloso"));

        String token = registerAndExtract("cataloguser", "catalog@aedstudio.com").get("accessToken").asText();
        mockMvc.perform(post("/api/progress/visit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TopicVisitRequest("algoritmos"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topics.algoritmos").value("VISITED"));
    }

    @Test
    @DisplayName("pré-requisito bloqueia e conclusão desbloqueia tópico seguinte")
    void prerequisiteUnlockFlow() throws Exception {
        String token = registerAndExtract("unlockuser", "unlock@aedstudio.com").get("accessToken").asText();

        mockMvc.perform(post("/api/progress/visit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TopicVisitRequest("tad"))))
                .andExpect(status().isLocked());

        mockMvc.perform(post("/api/learning/attempts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exerciseId\":\"algoritmos-check\",\"answer\":\"B\",\"timeSpentSeconds\":12}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress.topicStates.tad.state").value("AVAILABLE"));

        mockMvc.perform(post("/api/progress/visit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TopicVisitRequest("tad"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topics.tad").value("VISITED"));
    }

    @Test
    @DisplayName("gamificação concede nível e badge ao atingir 100 XP")
    void levelAndXpBadgeFlow() throws Exception {
        String token = registerAndExtract("xpbadgeuser", "xpbadge@aedstudio.com").get("accessToken").asText();

        for (int i = 1; i <= 10; i++) {
            XpAwardRequest request = new XpAwardRequest("algoritmos", "practice_bonus_" + i, 10);
            mockMvc.perform(post("/api/progress/xp")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/progress").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalXp").value(100))
                .andExpect(jsonPath("$.level.level").value(2))
                .andExpect(jsonPath("$.badges[*].id", hasItem("xp-100")));
    }

    @Test
    @DisplayName("streak de 7 dias concede badge e permanece no servidor")
    void streakSevenBadgeFlow() throws Exception {
        String token = registerAndExtract("streakuser", "streak@aedstudio.com").get("accessToken").asText();
        User user = userRepository.findByEmail("streak@aedstudio.com").orElseThrow();
        user.setStreakDays(6);
        user.setLastStudyDate(LocalDate.now().minusDays(1));
        userRepository.saveAndFlush(user);

        mockMvc.perform(post("/api/learning/attempts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exerciseId\":\"algoritmos-check\",\"answer\":\"B\",\"timeSpentSeconds\":18}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress.streakDays").value(7))
                .andExpect(jsonPath("$.progress.badges[*].id", hasItem("streak-7")));

        mockMvc.perform(get("/api/progress").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.streakDays").value(7))
                .andExpect(jsonPath("$.badges[*].id", hasItem("streak-7")));
    }

    @Test
    @DisplayName("simulador -> exercício dinâmico -> analytics -> recomendação")
    void advancedLearningFlow() throws Exception {
        String token = registerAndExtract("advanceduser", "advanced@aedstudio.com").get("accessToken").asText();

        mockMvc.perform(get("/api/recommendations").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primary.topicId").value("algoritmos"))
                .andExpect(jsonPath("$.primary.reason").isNotEmpty());

        mockMvc.perform(post("/api/simulations/events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topicId\":\"algoritmos\",\"simulatorType\":\"ARRAY\",\"action\":\"insert\",\"milestone\":\"FIRST_RUN\",\"stateSnapshot\":\"[3]\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.awarded").value(8))
                .andExpect(jsonPath("$.progress.totalXp").value(8));

        MvcResult generated = mockMvc.perform(post("/api/learning/generated-exercises")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topicId\":\"algoritmos\",\"difficulty\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.topicId").value("algoritmos"))
                .andExpect(jsonPath("$.type").value("TRACE_OUTPUT"))
                .andReturn();
        String generatedId = objectMapper.readTree(generated.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/learning/generated-exercises/history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(generatedId));

        mockMvc.perform(post("/api/learning/generated-exercises/" + generatedId + "/attempts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answer\":\"B\",\"timeSpentSeconds\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false))
                .andExpect(jsonPath("$.progress.topicsCompleted").value(0));

        mockMvc.perform(post("/api/learning/generated-exercises/" + generatedId + "/attempts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answer\":\"A\",\"timeSpentSeconds\":14}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.progress.topics.algoritmos").value("COMPLETED"))
                .andExpect(jsonPath("$.progress.topicStates.tad.state").value("AVAILABLE"));

        mockMvc.perform(get("/api/analytics/overview").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAttempts").value(2))
                .andExpect(jsonPath("$.overallAccuracyPercent").value(50))
                .andExpect(jsonPath("$.simulatorInteractions").value(1))
                .andExpect(jsonPath("$.attentionTopics[0].topicId").value("algoritmos"));

        mockMvc.perform(get("/api/recommendations").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.review[0].topicId").value("algoritmos"))
                .andExpect(jsonPath("$.nextSteps[*].topicId", hasItem("tad")));
    }

    @Test
    @DisplayName("missão de simulador valida estado formal e alimenta histórico de XP")
    void simulatorMissionAndXpHistoryFlow() throws Exception {
        String token = registerAndExtract("missionuser", "mission@aedstudio.com").get("accessToken").asText();

        completeTopic(token, "algoritmos-check");
        completeTopic(token, "tad-check");

        mockMvc.perform(get("/api/simulations/topics/arrays/missions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("arrays-map-3"))
                .andExpect(jsonPath("$[0].completed").value(false));

        mockMvc.perform(post("/api/simulations/missions/arrays-map-3/submit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stateSnapshot\":\"{\\\"values\\\":[3,6,9],\\\"edges\\\":[]}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.awarded").value(18));

        mockMvc.perform(get("/api/analytics/xp-history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].xp").isNumber())
                .andExpect(jsonPath("$[0].cumulativeXp").isNumber());
    }

    @Test
    @DisplayName("sandbox executa desafio de código com política restritiva e XP idempotente")
    void codeSandboxFlow() throws Exception {
        String token = registerAndExtract("codeuser", "code@aedstudio.com").get("accessToken").asText();

        mockMvc.perform(get("/api/code/topics/algoritmos/challenges")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("algoritmos-code-sum"))
                .andExpect(jsonPath("$[0].signature").value("solve(int[] values)"))
                .andExpect(jsonPath("$[0].examples[0]").isNotEmpty());

        mockMvc.perform(post("/api/code/run")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"challengeId\":\"algoritmos-code-sum\",\"code\":\"System.exit(0); return 0;\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(false))
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.submissionId").isNumber())
                .andExpect(jsonPath("$.feedback").value("Código rejeitado antes da execução por violar a política do sandbox."));

        String validCode = "int total = 0; for (int value : values) { total += value; } return total;";
        mockMvc.perform(post("/api/code/run")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.aedstudio.dto.CodeRunRequest("algoritmos-code-sum", validCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.submissionId").isNumber())
                .andExpect(jsonPath("$.awarded").value(30));

        mockMvc.perform(post("/api/code/run")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.aedstudio.dto.CodeRunRequest("algoritmos-code-sum", validCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(jsonPath("$.awarded").value(0));

        mockMvc.perform(get("/api/code/submissions")
                        .param("exerciseId", "algoritmos-code-sum")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].exerciseId").value("algoritmos-code-sum"))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$[0].passedTests").value(3));

        mockMvc.perform(get("/api/code/submissions/best")
                        .param("exerciseId", "algoritmos-code-sum")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.best").value(true))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("desafio de código respeita trilha, retorna dicas e cenários nomeados")
    void topicSpecificCodeChallengeFlow() throws Exception {
        String token = registerAndExtract("arraycodeuser", "arraycode@aedstudio.com").get("accessToken").asText();

        mockMvc.perform(get("/api/code/topics/arrays/challenges")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isLocked());

        completeTopic(token, "algoritmos-check");
        completeTopic(token, "tad-check");

        mockMvc.perform(get("/api/code/topics/arrays/challenges")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("arrays-code-search"))
                .andExpect(jsonPath("$[0].signature").value("solve(int[] values, int target)"))
                .andExpect(jsonPath("$[0].conceptualHint").isNotEmpty())
                .andExpect(jsonPath("$[0].pseudoSkeleton").isNotEmpty())
                .andExpect(jsonPath("$[0].difficulty").value(1));

        mockMvc.perform(post("/api/code/run")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.aedstudio.dto.CodeRunRequest("arrays-code-search", "return -1;"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(false))
                .andExpect(jsonPath("$.passedCount").value(1))
                .andExpect(jsonPath("$.totalChecks").value(3))
                .andExpect(jsonPath("$.hint").isNotEmpty())
                .andExpect(jsonPath("$.failedChecks[0]").isNotEmpty());

        String validCode = "for (int i = 0; i < values.length; i++) { if (values[i] == target) return i; } return -1;";
        mockMvc.perform(post("/api/code/run")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.aedstudio.dto.CodeRunRequest("arrays-code-search", validCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(jsonPath("$.passedCount").value(3))
                .andExpect(jsonPath("$.totalChecks").value(3))
                .andExpect(jsonPath("$.passedChecks[*]", hasItem("encontra no meio")))
                .andExpect(jsonPath("$.awarded").value(35));
    }

    @Test
    @DisplayName("judge suporta múltiplas assinaturas e analytics de código")
    void multiSignatureCodeJudgeAndAnalytics() throws Exception {
        String token = registerAndExtract("multicodeuser", "multicode@aedstudio.com").get("accessToken").asText();

        completeTopic(token, "algoritmos-check");

        mockMvc.perform(get("/api/code/topics/tad/challenges")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].signature").value("solve(String input)"));

        mockMvc.perform(post("/api/code/run")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.aedstudio.dto.CodeRunRequest(
                                "tad-code-contract-length",
                                "if (input == null) return 0; return input.trim().length();"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.passedCount").value(3));

        mockMvc.perform(get("/api/code/topics/notacao/challenges")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].signature").value("solve(int n)"));

        mockMvc.perform(post("/api/code/run")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.aedstudio.dto.CodeRunRequest(
                                "notacao-code-halving",
                                "int steps = 0; while (n > 1) { n = n / 2; steps++; } return steps;"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(true));

        mockMvc.perform(get("/api/analytics/overview")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codeSubmissions").value(2))
                .andExpect(jsonPath("$.codeSuccessPercent").value(100));
    }

    @Test
    @DisplayName("simulador respeita bloqueio pedagógico do tópico")
    void simulationLockedTopicRejected() throws Exception {
        String token = registerAndExtract("simlockuser", "simlock@aedstudio.com").get("accessToken").asText();

        mockMvc.perform(post("/api/simulations/events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topicId\":\"arrays\",\"simulatorType\":\"ARRAY\",\"action\":\"insert\",\"milestone\":\"FIRST_RUN\"}"))
                .andExpect(status().isLocked());
    }

    @Test
    @DisplayName("health público e CORS básico para dev/mobile")
    void healthAndCors() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(options("/api/progress")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5500")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5500"));
    }

    @Test
    @DisplayName("tópico inexistente é rejeitado de forma consistente")
    void unknownTopicRejected() throws Exception {
        String token = registerAndExtract("rejectuser", "reject@aedstudio.com").get("accessToken").asText();

        mockMvc.perform(post("/api/progress/visit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TopicVisitRequest("intro"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("topicId não cadastrado: intro"));
    }

    private JsonNode registerAndExtract(String username, String email) throws Exception {
        RegisterRequest req = new RegisterRequest(username, email, "Senha123!", username + " Full");
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private void completeTopic(String token, String exerciseId) throws Exception {
        mockMvc.perform(post("/api/learning/attempts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exerciseId\":\"" + exerciseId + "\",\"answer\":\"B\",\"timeSpentSeconds\":12}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true));
    }
}
