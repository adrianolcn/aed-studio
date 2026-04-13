package com.aedstudio.dto;

import com.aedstudio.model.TopicState;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// ═══════════════════════════════════════════════════════
//  DTOs de Autenticação
// ═══════════════════════════════════════════════════════

/**
 * Corpo da requisição de registro.
 * POST /api/auth/register
 */
class RegisterRequest {

    @NotBlank(message = "Nome de usuário é obrigatório")
    @Size(min = 2, max = 50, message = "Username deve ter entre 2 e 50 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$",
             message = "Username só pode conter letras, números, _, . e -")
    private String username;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
             message = "Senha deve conter ao menos 1 letra maiúscula, 1 minúscula e 1 número")
    private String password;

    @NotBlank(message = "Nome completo é obrigatório")
    @Size(min = 2, max = 100)
    private String fullName;
}

/**
 * Corpo da requisição de login.
 * POST /api/auth/login
 */
class LoginRequest {

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    private String password;
}

/**
 * Corpo da requisição de refresh.
 * POST /api/auth/refresh
 */
class RefreshTokenRequest {

    @NotBlank
    private String refreshToken;
}

/**
 * Resposta de autenticação bem-sucedida.
 * Retornado no login e no refresh.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn;  // segundos até expirar o access token
    private UserSummary user;
}

/**
 * Resumo do usuário — incluído na AuthResponse e no perfil.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class UserSummary {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String role;
    private Integer xp;
    private Integer streakDays;
    private Integer topicsCompleted;
    private LocalDate lastStudyDate;
}

// ═══════════════════════════════════════════════════════
//  DTOs de Progresso
// ═══════════════════════════════════════════════════════

/**
 * Registra uma visita a um tópico.
 * POST /api/progress/visit
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
class TopicVisitRequest {

    @NotBlank(message = "topicId é obrigatório")
    private String topicId;
}

/**
 * Registra ganho de XP (quiz correto, desafio de código).
 * POST /api/progress/xp
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
class XpAwardRequest {

    @NotBlank(message = "topicId é obrigatório")
    private String topicId;

    @NotBlank(message = "reason é obrigatório")
    private String reason;  // ex: "quiz_tad-q1", "code_tad"

    @Min(value = 1, message = "XP deve ser positivo")
    @Max(value = 500, message = "XP por evento não pode exceder 500")
    private Integer amount;
}

/**
 * Estado completo de progresso — retornado ao carregar a plataforma.
 * GET /api/progress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ProgressResponse {
    private Integer totalXp;
    private Integer streakDays;
    private Integer topicsVisited;
    private Integer topicsCompleted;
    private Integer totalTopics;
    private Integer progressPercent;
    private LocalDate lastStudyDate;
    // mapa topicId → estado ("VISITED" | "COMPLETED")
    private Map<String, String> topics;
    // conjunto de eventKeys já ganhos (para não re-awardar no front)
    private List<String> earnedEventKeys;
}

/**
 * Resposta genérica de erro.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private String path;
    private long timestamp;
}
