package com.aedstudio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AED·Studio — Ponto de entrada da aplicação Spring Boot.
 *
 * Inicia o servidor embarcado Tomcat na porta 8080.
 * Para trocar de perfil: --spring.profiles.active=prod
 */
@SpringBootApplication
public class AedStudioApplication {

    public static void main(String[] args) {
        SpringApplication.run(AedStudioApplication.class, args);
    }
}
