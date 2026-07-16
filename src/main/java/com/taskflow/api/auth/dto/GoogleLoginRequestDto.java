package com.taskflow.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequestDto(
        @NotBlank(message = "El token de google es obligatorio")
        String idToken
) {
}
