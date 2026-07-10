package com.taskflow.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto (

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Formato invalido")
        String email,
        @NotBlank(message = "La contraseña es obligatorio")
        String password
){ }
