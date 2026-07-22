package com.taskflow.api.auth.controller;

import com.taskflow.api.auth.dto.*;
import com.taskflow.api.auth.service.AuthService;
import com.taskflow.api.auth.service.result.AuthSessionResult;
import com.taskflow.api.security.model.SecurityUser;
import com.taskflow.api.security.service.AuthCookieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService authCookieService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(
            @Valid @RequestBody RegisterRequestDto request
    ) {
        AuthSessionResult result = authService.register(request);

        return authenticatedResponse(result, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(
            @Valid @RequestBody LoginRequestDto request
    ) {
        AuthSessionResult result = authService.login(request);

        return authenticatedResponse(result, HttpStatus.OK);
    }

    @PostMapping("/oauth2")
    public ResponseEntity<AuthResponseDto> loginWithGoogle(
            @Valid @RequestBody GoogleLoginRequestDto request
    ) {
        AuthSessionResult result = authService.loginWithGoogle(request);

        return authenticatedResponse(result, HttpStatus.OK);
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
            @CookieValue(
                    name = AuthCookieService.REFRESH_TOKEN_COOKIE,
                    required = false
            ) String refreshToken
    ) {
        AuthSessionResult result =
                authService.refreshToken(refreshToken);

        return authenticatedResponse(result, HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(
                    name = AuthCookieService.REFRESH_TOKEN_COOKIE,
                    required = false
            ) String refreshToken
    ) {
        authService.logout(refreshToken);

        return ResponseEntity.noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        authCookieService.deleteAccessTokenCookie().toString()
                )
                .header(
                        HttpHeaders.SET_COOKIE,
                        authCookieService.deleteRefreshTokenCookie().toString()
                )
                .build();
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfTokenResponseDto> csrfToken(
            CsrfToken csrfToken
    ) {
        return ResponseEntity.ok(
              new CsrfTokenResponseDto(
                      csrfToken.getHeaderName(),
                      csrfToken.getToken()
              )
        );
    }

    private ResponseEntity<AuthResponseDto> authenticatedResponse(
            AuthSessionResult result,
            HttpStatus status
    ) {
        return ResponseEntity.status(status)
                .header(
                        HttpHeaders.SET_COOKIE,
                        authCookieService
                                .createAccessTokenCookie(result.accessToken())
                                .toString()
                )
                .header(
                        HttpHeaders.SET_COOKIE,
                        authCookieService
                                .createRefreshTokenCookie(result.refreshToken())
                                .toString()
                )
                .body(new AuthResponseDto(result.user()));
    }
}
