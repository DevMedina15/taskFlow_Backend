package com.taskflow.api.project.dto;

import com.taskflow.api.project.entity.InvitationStatus;
import com.taskflow.api.project.entity.ProjectRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectInvitationResponseDto(
        UUID id,
        String guestEmail,
        ProjectRole role,
        InvitationStatus status,
        LocalDateTime expirationDate,
        LocalDateTime createdAt
) {
}
