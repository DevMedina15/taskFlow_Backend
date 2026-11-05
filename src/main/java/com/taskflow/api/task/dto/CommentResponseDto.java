package com.taskflow.api.task.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentResponseDto(
        UUID id,
        String content,
        UUID authorId,
        String authorName,
        String authorEmail,
        LocalDateTime createdAt
) {
}
