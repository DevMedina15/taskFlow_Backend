package com.taskflow.api.auth.dto;

public record AuthResponseDto(
        String accessToken,
        String refreshToken,
        AuthenticatedUserDto user
) {
}
