package com.taskflow.api.project.dto;

import com.taskflow.api.project.entity.ProjectStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectResponseDto(
        UUID id,
        String code,
        String name,
        String description,
        String organizationName,
        String projectKey,
        ProjectStatus status,
        LocalDateTime createdAt
) {
}
