package com.taskflow.api.project.dto;

public record ProjectInvitationVerifyResponseDto(
        String projectName,
        String role,
        String guestEmail
) {
}
