package com.taskflow.api.task.dto;

import com.taskflow.api.task.entity.TaskPriority;
import com.taskflow.api.task.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateTaskRequestDto(
        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 255, message = "El titulo no puede exceder los 255 caracteres")
        String title,
        @Size(max = 700, message = "La descripción es muy larga")
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Integer estimatedTime,
        @Size(max = 150, message = "La rama no puede exceder los 150 caracteres")
        String gitBranch,
        UUID assigneeId,
        UUID reviewerId
) {
}
