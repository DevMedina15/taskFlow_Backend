package com.taskflow.api.auth.dto;

public record AuthenticatedUserDto(
        String fullName,
        String email
) {
}
