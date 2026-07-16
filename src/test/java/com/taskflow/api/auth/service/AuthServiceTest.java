package com.taskflow.api.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.taskflow.api.auth.dto.AuthResponseDto;
import com.taskflow.api.auth.dto.GoogleLoginRequestDto;
import com.taskflow.api.auth.dto.LoginRequestDto;
import com.taskflow.api.auth.dto.RegisterRequestDto;
import com.taskflow.api.security.model.SecurityUser;
import com.taskflow.api.security.service.JwtService;
import com.taskflow.api.shared.exception.BusinessRuleException;
import com.taskflow.api.user.UserRepository;
import com.taskflow.api.user.entity.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private GoogleIdTokenVerifier googleIdTokenVerifier;

    @InjectMocks
    private AuthService authService;


    @Test
    void failedRegistration() {

        RegisterRequestDto request = new RegisterRequestDto(
                "Juan David Angeles Medina",
                "juandavid151199@gmail.com",
                "960358293");

        when(userRepository.findByEmail("juandavid151199@gmail.com")).thenReturn(Optional.of(new User()));

        Throwable error = catchThrowable(() -> authService.register(request));

        assertThat(error)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El correo ya está registrado en el sistema");
    }

    @Test
    void successfulRegistration() {
        RegisterRequestDto request = new RegisterRequestDto(
                "Juan David Angeles Medina",
                "juandavid151199@gmail.com",
                "960358293");

        when(userRepository.findByEmail("juandavid151199@gmail.com")).thenReturn(Optional.empty());

        when(passwordEncoder.encode(anyString())).thenReturn("clave-encriptada-falsa");

        when(jwtService.generateToken(any(SecurityUser.class))).thenReturn("token");

        AuthResponseDto response = authService.register(request);

        assertThat(response.jwtToken()).isEqualTo("token");

        verify(userRepository).save(any(User.class));
    }

    @Test
    void failedLogin() {

        LoginRequestDto loginDto = new LoginRequestDto("juandavid151199@gmail.com", "960358293");

        User fakeUser = User.builder()
                .password("contraseña-encriptada")
                .build();

        when(userRepository.findByEmail(loginDto.email())).thenReturn(Optional.of(fakeUser));

        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        Throwable error = catchThrowable(() -> authService.login(loginDto));

        assertThat(error)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Credenciales incorrectas");

    }

    @Test
    void successfulLogin() {
        LoginRequestDto loginDto = new LoginRequestDto("juandavid151199@gmail.com", "960358293");

        User fakeUser = User.builder()
                .email("juandavid151199@gmail.com")
                .password("contraseña-encriptada")
                .build();

        when(userRepository.findByEmail(loginDto.email())).thenReturn(Optional.of(fakeUser));

        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        when(jwtService.generateToken(any(SecurityUser.class))).thenReturn("token");

        AuthResponseDto responseDto = authService.login(loginDto);

        assertThat(responseDto).isNotNull();
        assertThat(responseDto.jwtToken()).isEqualTo("token");

    }

    @Test
    void failedLoginUserNotFound() {

        LoginRequestDto loginDto = new LoginRequestDto("fantasma@gmail.com", "960358293");

        when(userRepository.findByEmail(loginDto.email())).thenReturn(Optional.empty());

        Throwable error = catchThrowable(() -> authService.login(loginDto));

        assertThat(error)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Credenciales incorrectas");
    }

    @Test
    void failedLoginWithGoogle() throws Exception {

        GoogleLoginRequestDto loginRequestDto = new GoogleLoginRequestDto("token");

        when(googleIdTokenVerifier.verify(anyString())).thenReturn(null);

        Throwable error = catchThrowable(() -> authService.loginWithGoogle(loginRequestDto));

        assertThat(error)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Error en autenticación Google: Token de Google invalido");

    }

    @Test
    void successfulLoginWithGoogle() throws Exception {

        GoogleLoginRequestDto loginDto = new GoogleLoginRequestDto("token");

        GoogleIdToken idTokenMock = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payloadFake = new GoogleIdToken.Payload();
        payloadFake.setEmail("juandavid151199@gmail.com");
        payloadFake.set("name", "JUAN DAVID ANGELES MEDINA");

        when(idTokenMock.getPayload()).thenReturn(payloadFake);
        when(googleIdTokenVerifier.verify(anyString())).thenReturn(idTokenMock);

        User fakeUser = User.builder()
                .email("juandavid151199@gmail.com")
                .password("password")
                .build();
        when(userRepository.findByEmail("juandavid151199@gmail.com")).thenReturn(Optional.of(fakeUser));

        when(jwtService.generateToken(any(SecurityUser.class))).thenReturn("jwt");

        AuthResponseDto response = authService.loginWithGoogle(loginDto);

        assertThat(response).isNotNull();
        assertThat(response.jwtToken()).isEqualTo("jwt");


    }

    @Test
    void successfulLoginWithGoogleNewUser() throws Exception {

        GoogleLoginRequestDto loginDto = new GoogleLoginRequestDto("token");

        GoogleIdToken idTokenMock = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payloadFake = new GoogleIdToken.Payload();
        payloadFake.setEmail("nuevo-usuario@gmail.com");
        payloadFake.set("name", "Nuevo Usuario");

        when(idTokenMock.getPayload()).thenReturn(payloadFake);
        when(googleIdTokenVerifier.verify(anyString())).thenReturn(idTokenMock);

        when(userRepository.findByEmail("nuevo-usuario@gmail.com")).thenReturn(Optional.empty());

        when(passwordEncoder.encode(anyString())).thenReturn("clave-aleatoria-encriptada");
        when(jwtService.generateToken(any(SecurityUser.class))).thenReturn("jwt-nuevo");

        AuthResponseDto response = authService.loginWithGoogle(loginDto);

        assertThat(response).isNotNull();
        assertThat(response.jwtToken()).isEqualTo("jwt-nuevo");

        verify(userRepository).save(any(User.class));
    }
}