package com.taskflow.api.auth.service;

import com.taskflow.api.auth.dto.AuthResponseDto;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

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
}
