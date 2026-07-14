package com.taskflow.api.project.dto;

public record ProjectInvitationEmailEvent(
        String guestEmail,
        String projectName,
        String role,
        String token
) {
}
