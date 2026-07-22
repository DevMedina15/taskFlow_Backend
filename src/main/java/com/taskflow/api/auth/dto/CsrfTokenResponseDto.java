package com.taskflow.api.auth.dto;

public record CsrfTokenResponseDto(
        String headerName,
        String token
) {
}
