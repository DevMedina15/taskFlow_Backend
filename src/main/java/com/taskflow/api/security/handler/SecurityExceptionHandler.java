package com.taskflow.api.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.api.shared.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class SecurityExceptionHandler {

    private final ObjectMapper objectMapper;

    public void handleAuthenticationException(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        ErrorResponseDto error = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message("No autenticado. Debes iniciar session")
                .path(request.getRequestURI())
                .build();
        writeResponse(response, HttpStatus.UNAUTHORIZED, error);
    }

    public void handleAccessDeniedException(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException exception
    ) throws IOException {

        ErrorResponseDto error = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message("No tienes permisos para relizar esta operación.")
                .path(request.getRequestURI())
                .build();
        writeResponse(response, HttpStatus.FORBIDDEN, error);
    }

    private void writeResponse(
            HttpServletResponse response,
            HttpStatus status,
            ErrorResponseDto error
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getWriter(), error);
    }

}
