package com.taskflow.api.task.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignMemberRequestDto(
        @NotNull(message = "El usuario es obligatorio")
        UUID userId
) {
}
