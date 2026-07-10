package com.taskflow.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequestDto(

        @NotBlank(message = "El nombre es obligatorio")
        String fullName,
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Formato inválido")
        String email,
        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {
}
