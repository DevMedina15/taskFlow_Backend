package com.taskflow.api.auth.service.result;

import com.taskflow.api.auth.dto.AuthenticatedUserDto;

public record AuthSessionResult(
        String accessToken,
        String refreshToken,
        AuthenticatedUserDto user
) {
}
