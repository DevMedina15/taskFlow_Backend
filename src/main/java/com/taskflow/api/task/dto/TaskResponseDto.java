package com.taskflow.api.task.dto;

import com.taskflow.api.task.entity.TaskPriority;
import com.taskflow.api.task.entity.TaskStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record TaskResponseDto(
        UUID id,
        String title,
        String code,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Integer estimatedTime,
        String gitBranch,
        UUID assigneeId,
        UUID reviewerId,
        LocalDateTime createdAt
) {
}
