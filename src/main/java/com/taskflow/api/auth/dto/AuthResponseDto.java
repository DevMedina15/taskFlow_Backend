package com.taskflow.api.auth.dto;

public record AuthResponseDto(
        String accessToken,
        AuthenticatedUserDto user
) {
}
