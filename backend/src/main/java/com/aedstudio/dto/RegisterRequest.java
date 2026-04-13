package com.aedstudio.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

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
