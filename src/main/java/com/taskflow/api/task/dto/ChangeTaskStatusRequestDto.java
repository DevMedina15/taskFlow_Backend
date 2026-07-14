package com.taskflow.api.task.dto;

import com.taskflow.api.task.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeTaskStatusRequestDto(
        @NotNull(message = "EL estado no puede ser nulo")
        TaskStatus status
) {
}
