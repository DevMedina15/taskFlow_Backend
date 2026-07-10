package com.taskflow.api.security.filter;

import com.taskflow.api.security.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 1. Validamos si vino la cabecera en el formato correcto

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extraemos el token (cortamos los primeros 7 caracteres: "Bearer ")
        jwt = authHeader.substring(7);
        userEmail = jwtService.extractUserName(jwt);

        // 3. Si el token tiene un correo y el usuario no está autenticado en este instante...
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Vamos a la BD a buscar al usuario
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // 4. Si el token es legítimo y no ha caducado...
            if (jwtService.isTokenValid(jwt, userDetails)) {

                // 5. Creamos el pase de entrada oficial de Spring Security
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                // Le agregamos detalles extra como la IP de la petición
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 6. Guardamos al usuario en la "RAM temporal" de esta petición
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
