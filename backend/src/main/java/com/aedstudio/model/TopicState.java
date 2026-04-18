package com.aedstudio.model;

/**
 * Estado de progresso de um tópico para um usuário.
 *
 * VISITED   → usuário abriu o tópico, mas não completou quiz/desafio
 * COMPLETED → usuário respondeu pelo menos um quiz corretamente
 */
public enum TopicState {
    LOCKED,
    AVAILABLE,
    VISITED,
    COMPLETED
}
