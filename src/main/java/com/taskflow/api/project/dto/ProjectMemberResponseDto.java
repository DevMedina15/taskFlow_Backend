package com.taskflow.api.project.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectMemberResponseDto(
        UUID userId,
        String email,
        String role,
        LocalDateTime joinedAt
) {
}
