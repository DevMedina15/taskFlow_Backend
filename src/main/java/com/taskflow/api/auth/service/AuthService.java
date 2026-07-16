package com.taskflow.api.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.taskflow.api.auth.dto.AuthResponseDto;
import com.taskflow.api.auth.dto.GoogleLoginRequestDto;
import com.taskflow.api.auth.dto.LoginRequestDto;
import com.taskflow.api.auth.dto.RegisterRequestDto;
import com.taskflow.api.security.model.SecurityUser;
import com.taskflow.api.security.service.JwtService;
import com.taskflow.api.shared.exception.BusinessRuleException;
import com.taskflow.api.user.UserRepository;
import com.taskflow.api.user.entity.Role;
import com.taskflow.api.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.hibernate.boot.model.process.internal.UserTypeResolution;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;


    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

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

        SecurityUser securityUser = new SecurityUser(user);
        String jwtToken = jwtService.generateToken(securityUser);

        return new AuthResponseDto(jwtToken);
    }

    public AuthResponseDto login(LoginRequestDto request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessRuleException("Credenciales incorrectas"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessRuleException("Credenciales incorrectas");
        }

        SecurityUser securityUser = new SecurityUser(user);
        String jwtToken = jwtService.generateToken(securityUser);

        return new AuthResponseDto(jwtToken);

    }

    public AuthResponseDto loginWithGoogle(GoogleLoginRequestDto request) {
        try {

            // 1. Configuracion de el validator con las librerias descargadas
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            // 2. Se desencripta y valida la firma del token
            GoogleIdToken idToken = verifier.verify(request.idToken());

            if (idToken == null) {
                throw new BusinessRuleException("Token de Google invalido");
            }

            // 3. Extracción de informaión del usuario
            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            // 4. Lógica de negocio: Buscar o crear
            Optional<User> userOptional = userRepository.findByEmail(email);
            User user;

            if (userOptional.isPresent()) {
                user = userOptional.get();
            } else {
                String randomPassword = UUID.randomUUID().toString();

                user = User.builder()
                        .email(email)
                        .fullName(name != null ? name : "Usuario de Google")
                        .password(passwordEncoder.encode(randomPassword))
                        .role(Role.SYS_USER)
                        .build();
                userRepository.save(user);
            }

            // 5. Lo incluimos al sistema y se le genera su JWT

            SecurityUser securityUser = new SecurityUser(user);
            String jwtToken = jwtService.generateToken(securityUser);

            return new AuthResponseDto(jwtToken);

        } catch (Exception e) {
            throw new BusinessRuleException("Error en autenticación Google: " +  e.getMessage());
        }
    }
}
