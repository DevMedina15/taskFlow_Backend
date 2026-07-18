package com.taskflow.api.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.taskflow.api.auth.dto.*;
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

    public AuthResponseDto register(RegisterRequestDto request) {

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

        return buildAuthResponse(user);
    }

    public AuthResponseDto login(LoginRequestDto request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        return buildAuthResponse(user);

    }

    public AuthResponseDto loginWithGoogle(GoogleLoginRequestDto request) {
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



            return buildAuthResponse(user);

        } catch (BusinessRuleException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessRuleException("Error en autenticacion Google");
        }
    }

    private AuthResponseDto buildAuthResponse(User user) {

        SecurityUser securityUser = new SecurityUser(user);

        String accessTOken = jwtService.generateToken(securityUser);

        AuthenticatedUserDto authenticatedUserDto = new AuthenticatedUserDto(
            user.getFullName(),
            user.getEmail()
        );

        return new AuthResponseDto(accessTOken, authenticatedUserDto);

    }
}
