package com.taskflow.api.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateTaskRequestDto(
        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 255, message = "El titulo no puede exceder los 255 caracteres")
        String title,
        @Size(max = 500, message = "La descripción es muy larga")
        String description,

        LocalDateTime startDate,
        LocalDateTime endDate,
        Integer estimatedTime,
        @Size(max = 150, message = "La rama no puede exceder los 150 caracteres")
        String gitBranch

) {
}
