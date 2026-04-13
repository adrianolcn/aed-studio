package com.aedstudio.controller;

import com.aedstudio.dto.LoginRequest;
import com.aedstudio.dto.RegisterRequest;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para o fluxo completo de autenticação.
 *
 * Usa H2 em memória (perfil "test") para isolamento.
 * Cada teste é transacional e faz rollback ao final.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController — testes de integração")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private static final String EMAIL    = "test@aedstudio.com";
    private static final String PASSWORD = "Senha123!";
    private static final String USERNAME = "testuser";

    // ── Registro ────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /register → 201 com tokens e dados do usuário")
    void register_success() throws Exception {
        RegisterRequest req = new RegisterRequest(USERNAME, EMAIL, PASSWORD, "Test User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(EMAIL))
                .andExpect(jsonPath("$.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.user.xp").value(0));
    }

    @Test
    @DisplayName("POST /register → 400 com e-mail inválido")
    void register_invalidEmail() throws Exception {
        RegisterRequest req = new RegisterRequest(USERNAME, "nao-e-email", PASSWORD, "Test");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("E-mail inválido")));
    }

    @Test
    @DisplayName("POST /register → 400 com senha fraca")
    void register_weakPassword() throws Exception {
        RegisterRequest req = new RegisterRequest(USERNAME, EMAIL, "12345678", "Test");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /register → 401 com e-mail duplicado")
    void register_duplicateEmail() throws Exception {
        RegisterRequest req = new RegisterRequest(USERNAME, EMAIL, PASSWORD, "Test User");

        // Primeiro registro
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Segundo registro com mesmo e-mail
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // ── Login JWT ────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /login → 200 com tokens após registro")
    void login_success() throws Exception {
        // Registra primeiro
        RegisterRequest reg = new RegisterRequest(USERNAME, EMAIL, PASSWORD, "Test User");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        // Login
        LoginRequest login = new LoginRequest(EMAIL, PASSWORD);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    @Test
    @DisplayName("POST /login → 401 com senha errada")
    void login_wrongPassword() throws Exception {
        RegisterRequest reg = new RegisterRequest(USERNAME, EMAIL, PASSWORD, "Test User");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest(EMAIL, "SenhaErrada1");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos"));
    }

    // ── GET /me ──────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /me → 200 com usuário autenticado")
    void me_authenticated() throws Exception {
        // Registra e extrai token
        RegisterRequest reg = new RegisterRequest(USERNAME, EMAIL, PASSWORD, "Test User");
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();

        // Acessa /me com token
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.username").value(USERNAME));
    }

    @Test
    @DisplayName("GET /me → 401 sem token")
    void me_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
