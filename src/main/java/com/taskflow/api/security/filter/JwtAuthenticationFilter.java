package com.taskflow.api.security.filter;

import com.taskflow.api.security.service.AuthCookieService;
import com.taskflow.api.security.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String jwt = extractJwtFromCookie(request);

        if (jwt == null || jwt.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            authenticateRequest(jwt, request);
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
            SecurityContextHolder.clearContext();

            log.warn(
                    "JWT invalido o expirado en {}. Motivo {}",
                    request.getRequestURI(),
                    ex.getClass().getSimpleName()
            );
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateRequest(
            String jwt,
            HttpServletRequest request
    ) {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        String userEmail = jwtService.extractUserName(jwt);

        if (userEmail == null || userEmail.isBlank()) {
            return;
        }

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(userEmail);

        if (!jwtService.isTokenValid(jwt, userDetails)) {
            return;
        }

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        authToken.setDetails(
                new WebAuthenticationDetailsSource()
                        .buildDetails(request)
        );

        SecurityContextHolder
                .getContext()
                .setAuthentication(authToken);
    }

    private String extractJwtFromCookie(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(
                request,
                AuthCookieService.ACCESS_TOKEN_COOKIE
        );

        return cookie == null ? null : cookie.getValue();
    }
}
