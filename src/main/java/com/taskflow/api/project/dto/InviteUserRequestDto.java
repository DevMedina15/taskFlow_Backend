package com.taskflow.api.project.dto;

import com.taskflow.api.project.entity.ProjectRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteUserRequestDto(
        @NotBlank(message = "El correo del invitado es obligatorio")
        @Email(message = "El formato del correo electrónico no es válido")
        String guestEmail,

        @NotNull(message = "El rol asignado es obligatorio")
        ProjectRole role
) {
}
