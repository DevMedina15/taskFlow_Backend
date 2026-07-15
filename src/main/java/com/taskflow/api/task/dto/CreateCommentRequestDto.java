package com.taskflow.api.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequestDto(
        @NotBlank(message = "El contenido del comentario no puede estar vacío")
        @Size(max = 2000, message = "El comentario no puede exceder los 2000 caracteres")
        String content
) {
}
