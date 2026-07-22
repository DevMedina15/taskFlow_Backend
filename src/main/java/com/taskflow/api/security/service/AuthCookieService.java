package com.taskflow.api.security.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class AuthCookieService {

    public static final String ACCESS_TOKEN_COOKIE = "access_token";
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private final boolean secure;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public AuthCookieService(
            @Value("${api.security.cookie.secure:false}") boolean secure,
            @Value("${api.security.jwt.expiration}") long accessTokenExpiration,
            @Value("${api.security.refresh-token.expiration}") long refreshTokenExpiration
    ) {
        this.secure = secure;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public ResponseCookie createAccessTokenCookie(String accessToken) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, accessToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/api")
                .maxAge(Duration.ofMillis(accessTokenExpiration))
                .build();
    }

    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/api/v1/auth")
                .maxAge(Duration.ofMillis(refreshTokenExpiration))
                .build();
    }

    public ResponseCookie deleteCookie(String name, String path) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(path)
                .maxAge(Duration.ZERO)
                .build();
    }

    public ResponseCookie deleteAccessTokenCookie() {
        return deleteCookie(ACCESS_TOKEN_COOKIE, "/api");
    }

    public ResponseCookie deleteRefreshTokenCookie() {
        return deleteCookie(REFRESH_TOKEN_COOKIE, "/api/v1/auth");
    }

    public List<ResponseCookie> deleteAuthCookies() {
        return List.of(
                deleteCookie(ACCESS_TOKEN_COOKIE, "/api"),
                deleteCookie(ACCESS_TOKEN_COOKIE, "/"),
                deleteCookie(ACCESS_TOKEN_COOKIE, "/api/v1/auth"),

                deleteCookie(REFRESH_TOKEN_COOKIE, "/api/v1/auth"),
                deleteCookie(REFRESH_TOKEN_COOKIE, "/api"),
                deleteCookie(REFRESH_TOKEN_COOKIE, "/")
        );
    }


}
