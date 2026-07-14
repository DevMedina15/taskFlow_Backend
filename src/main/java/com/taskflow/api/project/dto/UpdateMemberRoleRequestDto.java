package com.taskflow.api.project.dto;

import com.taskflow.api.project.entity.ProjectRole;

public record UpdateMemberRoleRequestDto(
        ProjectRole role
) {
}
