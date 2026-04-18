package com.aedstudio.exception;

import com.aedstudio.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Centraliza o tratamento de exceções para todos os controllers REST.
 *
 * Garante respostas JSON padronizadas em vez de páginas de erro HTML,
 * o que é essencial para clientes SPA que consomem a API.
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.aedstudio.controller")
public class GlobalExceptionHandler {

    // ── 400 Bad Request ─────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        return error(HttpStatus.BAD_REQUEST, "Dados inválidos", message, request);
    }

    // ── 401 Unauthorized ────────────────────────────────────────────

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuth(
            AuthException ex, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "Não autorizado", ex.getMessage(), request);
    }

    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleSpringAuth(
            RuntimeException ex, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "Não autorizado",
                "E-mail ou senha inválidos", request);
    }

    // ── 403 Forbidden ───────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccess(
            AccessDeniedException ex, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "Acesso negado",
                "Você não tem permissão para este recurso", request);
    }

    // ── 409 Conflict ────────────────────────────────────────────────

    @ExceptionHandler({DuplicateResourceException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ErrorResponse> handleConflict(
            RuntimeException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "Conflito", ex.getMessage(), request);
    }

    // ── 422 Unprocessable Entity ────────────────────────────────────

    @ExceptionHandler(InvalidTopicException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTopic(
            InvalidTopicException ex, HttpServletRequest request) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "Tópico inválido", ex.getMessage(), request);
    }

    @ExceptionHandler(LockedTopicException.class)
    public ResponseEntity<ErrorResponse> handleLockedTopic(
            LockedTopicException ex, HttpServletRequest request) {
        return error(HttpStatus.LOCKED, "Tópico bloqueado", ex.getMessage(), request);
    }

    // ── 500 Internal Server Error ────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {
        log.error("Erro não tratado em {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente.", request);
    }

    // ── Builder ─────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> error(
            HttpStatus status, String error, String message,
            HttpServletRequest request) {

        ErrorResponse body = ErrorResponse.builder()
                .status(status.value())
                .error(error)
                .message(message)
                .path(request.getRequestURI())
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.status(status).body(body);
    }
}
