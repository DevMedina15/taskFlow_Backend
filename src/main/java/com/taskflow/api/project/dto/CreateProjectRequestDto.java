package com.taskflow.api.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequestDto(
        @NotBlank(message = "El nombre del proyecto es obligatorio")
        @Size(max = 150, message = "El nombre no puede exceder los 150 caracteres")
        String name,
        @Size(max = 500, message = "La descripción es muy larga")
        String description,
        @NotBlank(message = "La organizacion es obligatoria")
        @Size(max = 50, message = "El nombre de la organizacion no puede exceder los 50 caracteres")
        String organizationName
) {
}
