package com.taskflow.api.auth.controller;

import com.taskflow.api.auth.dto.*;
import com.taskflow.api.auth.service.AuthService;
import com.taskflow.api.security.model.SecurityUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        return new ResponseEntity<>(authService.register(request), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/oauth2")
    public ResponseEntity<AuthResponseDto> loginWithGoogle(
            @Valid @RequestBody GoogleLoginRequestDto request
    ) {
        return ResponseEntity.ok(authService.loginWithGoogle(request));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthenticatedUserDto> getCurrentUser(
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        return ResponseEntity.ok(
                authService.getCurrentUser(securityUser)
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDto request
    ) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody RefreshTokenRequestDto request
    ) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }
}
