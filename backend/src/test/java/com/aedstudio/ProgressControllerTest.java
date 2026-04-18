package com.aedstudio.controller;

import com.aedstudio.dto.RegisterRequest;
import com.aedstudio.dto.TopicVisitRequest;
import com.aedstudio.dto.XpAwardRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ProgressController - testes de integração")
class ProgressControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /visit registra visita de forma idempotente")
    void visit_idempotent() throws Exception {
        String token = registerAndGetToken();
        TopicVisitRequest req = new TopicVisitRequest("algoritmos");

        mockMvc.perform(post("/api/progress/visit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topicsVisited").value(1))
                .andExpect(jsonPath("$.totalXp").value(5))
                .andExpect(jsonPath("$.earnedEventKeys", hasItem("visit_algoritmos")))
                .andExpect(jsonPath("$.topicStates.algoritmos.state").value("VISITED"))
                .andExpect(jsonPath("$.topicStates.tad.state").value("LOCKED"));

        mockMvc.perform(post("/api/progress/visit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/progress")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topicsVisited").value(1))
                .andExpect(jsonPath("$.totalTopics").value(20))
                .andExpect(jsonPath("$.progressPercent").value(0))
                .andExpect(jsonPath("$.topics", hasKey("algoritmos")));
    }

    @Test
    @DisplayName("POST /xp concede XP uma vez sem concluir tópico fora do fluxo de exercício")
    void awardXp_idempotent() throws Exception {
        String token = registerAndGetToken();
        XpAwardRequest req = new XpAwardRequest("algoritmos", "bonus_algoritmos_reading", 10);

        mockMvc.perform(post("/api/progress/xp")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.awarded").value(10))
                .andExpect(jsonPath("$.alreadyEarned").value(false))
                .andExpect(jsonPath("$.progress.totalXp").value(10))
                .andExpect(jsonPath("$.progress.topicsCompleted").value(0));

        mockMvc.perform(post("/api/progress/xp")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.awarded").value(0))
                .andExpect(jsonPath("$.alreadyEarned").value(true))
                .andExpect(jsonPath("$.progress.totalXp").value(10));

        mockMvc.perform(get("/api/progress")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalXp").value(10))
                .andExpect(jsonPath("$.topicsCompleted").value(0))
                .andExpect(jsonPath("$.earnedEventKeys", hasItem("bonus_algoritmos_reading")));
    }

    @Test
    @DisplayName("POST /learning/attempts corrige exercício, conclui tópico e desbloqueia próximo")
    void exerciseCompletesAndUnlocksNextTopic() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(post("/api/progress/visit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TopicVisitRequest("algoritmos"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/learning/attempts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exerciseId\":\"algoritmos-check\",\"answer\":\"B\",\"timeSpentSeconds\":42}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.topicCompleted").value(true))
                .andExpect(jsonPath("$.awarded").value(35))
                .andExpect(jsonPath("$.progress.topics.algoritmos").value("COMPLETED"))
                .andExpect(jsonPath("$.progress.topicStates.tad.state").value("AVAILABLE"))
                .andExpect(jsonPath("$.progress.badges[0].id").value("first-topic"));
    }

    @Test
    @DisplayName("POST /visit rejeita tópico bloqueado por pré-requisito")
    void visit_lockedTopic() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(post("/api/progress/visit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TopicVisitRequest("tad"))))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.error").value("Tópico bloqueado"));
    }

    @Test
    @DisplayName("POST /visit rejeita topicId fora do catálogo")
    void visit_invalidTopic() throws Exception {
        String token = registerAndGetToken();
        TopicVisitRequest req = new TopicVisitRequest("topic-inexistente");

        mockMvc.perform(post("/api/progress/visit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("topicId não cadastrado: topic-inexistente"));
    }

    @Test
    @DisplayName("POST /xp rejeita topicId fora do catálogo")
    void awardXp_invalidTopic() throws Exception {
        String token = registerAndGetToken();
        XpAwardRequest req = new XpAwardRequest("topic-inexistente", "quiz_topic-inexistente_q1", 10);

        mockMvc.perform(post("/api/progress/xp")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("topicId não cadastrado: topic-inexistente"));
    }

    @Test
    @DisplayName("GET /progress rejeita usuário não autenticado")
    void progress_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/progress"))
                .andExpect(status().isUnauthorized());
    }

    private String registerAndGetToken() throws Exception {
        RegisterRequest req = new RegisterRequest(
                "progressuser",
                "progress@aedstudio.com",
                "Senha123!",
                "Progress User");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }
}
