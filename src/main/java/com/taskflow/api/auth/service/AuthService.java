package com.taskflow.api.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.taskflow.api.auth.dto.*;
import com.taskflow.api.auth.service.result.AuthSessionResult;
import com.taskflow.api.security.model.SecurityUser;
import com.taskflow.api.security.service.JwtService;
import com.taskflow.api.shared.exception.BusinessRuleException;
import com.taskflow.api.user.repository.UserRepository;
import com.taskflow.api.user.entity.Role;
import com.taskflow.api.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final RefreshTokenService refreshTokenService;

    public AuthSessionResult register(RegisterRequestDto request) {

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new BusinessRuleException("El correo ya está registrado en el sistema");
        }

        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.SYS_USER)
                .build();

        userRepository.save(user);

        return buildAuthSession(user);
    }

    public AuthSessionResult login(LoginRequestDto request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        return buildAuthSession(user);
    }

    public AuthSessionResult loginWithGoogle(GoogleLoginRequestDto request) {
        try {
            GoogleIdToken idToken = googleIdTokenVerifier.verify(request.idToken());

            if (idToken == null) {
                throw new BusinessRuleException("Token de Google invalido");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            String email = payload.getEmail();
            String name = (String) payload.get("name");

            Optional<User> userOptional = userRepository.findByEmail(email);

            User user = userOptional.orElseGet(() -> {
                String randomPassword = UUID.randomUUID().toString();

                User newUser = User.builder()
                        .email(email)
                        .fullName(name != null ? name : "Usuario de Google")
                        .password(passwordEncoder.encode(randomPassword))
                        .role(Role.SYS_USER)
                        .build();

                return userRepository.save(newUser);
            });

            return buildAuthSession(user);

        } catch (BusinessRuleException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessRuleException("Error en autenticacion Google");
        }
    }

    public AuthenticatedUserDto getCurrentUser(SecurityUser securityUser) {
        return buildAuthenticatedUser(securityUser.getUser());
    }

    public AuthSessionResult refreshToken(String refreshToken) {

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadCredentialsException("Refresh token inválido");
        }

        User user = refreshTokenService.validateAndRotate(refreshToken);

        String newRefreshToken = refreshTokenService.createRefreshToken(user);

        return buildAuthSession(user, newRefreshToken);
    }

    public void logout(String refreshToken) {

        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        refreshTokenService.revoke(refreshToken);
    }

    private AuthSessionResult buildAuthSession(User user) {

        String refreshToken = refreshTokenService.createRefreshToken(user);

        return buildAuthSession(user, refreshToken);
    }

    private AuthSessionResult buildAuthSession(User user, String refreshToken) {

        SecurityUser securityUser = new SecurityUser(user);

        String accessToken = jwtService.generateToken(securityUser);

        AuthenticatedUserDto authenticatedUserDto = buildAuthenticatedUser(user);

        return new AuthSessionResult(
                accessToken,
                refreshToken,
                authenticatedUserDto
        );
    }

    private AuthenticatedUserDto buildAuthenticatedUser(User user) {
        return new AuthenticatedUserDto(
                user.getFullName(),
                user.getEmail()
        );
    }
}
