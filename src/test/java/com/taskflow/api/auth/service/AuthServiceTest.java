package com.taskflow.api.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.taskflow.api.auth.dto.*;
import com.taskflow.api.security.model.SecurityUser;
import com.taskflow.api.security.service.JwtService;
import com.taskflow.api.shared.exception.BusinessRuleException;
import com.taskflow.api.user.entity.User;
import com.taskflow.api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void failedRegistration() {

        RegisterRequestDto request = new RegisterRequestDto(
                "Juan David Angeles Medina",
                "juandavid151199@gmail.com",
                "960358293"
        );

        when(userRepository.findByEmail("juandavid151199@gmail.com"))
                .thenReturn(Optional.of(new User()));

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
                "960358293"
        );

        when(userRepository.findByEmail("juandavid151199@gmail.com"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode(anyString()))
                .thenReturn("clave-encriptada-falsa");

        when(jwtService.generateToken(any(SecurityUser.class)))
                .thenReturn("token");

        when(refreshTokenService.createRefreshToken(any(User.class)))
                .thenReturn("refresh-token");

        AuthResponseDto response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.user()).isNotNull();
        assertThat(response.user().fullName()).isEqualTo("Juan David Angeles Medina");
        assertThat(response.user().email()).isEqualTo("juandavid151199@gmail.com");

        verify(userRepository).save(any(User.class));
    }

    @Test
    void failedLogin() {

        LoginRequestDto loginDto = new LoginRequestDto(
                "juandavid151199@gmail.com",
                "960358293"
        );

        User fakeUser = User.builder()
                .email("juandavid151199@gmail.com")
                .fullName("Juan David Angeles Medina")
                .password("contraseña-encriptada")
                .build();

        when(userRepository.findByEmail(loginDto.email()))
                .thenReturn(Optional.of(fakeUser));

        when(passwordEncoder.matches(anyString(), anyString()))
                .thenReturn(false);

        Throwable error = catchThrowable(() -> authService.login(loginDto));

        assertThat(error)
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales incorrectas");
    }

    @Test
    void successfulLogin() {

        LoginRequestDto loginDto = new LoginRequestDto(
                "juandavid151199@gmail.com",
                "960358293"
        );

        User fakeUser = User.builder()
                .email("juandavid151199@gmail.com")
                .fullName("Juan David Angeles Medina")
                .password("contraseña-encriptada")
                .build();

        when(userRepository.findByEmail(loginDto.email()))
                .thenReturn(Optional.of(fakeUser));

        when(passwordEncoder.matches(anyString(), anyString()))
                .thenReturn(true);

        when(jwtService.generateToken(any(SecurityUser.class)))
                .thenReturn("token");

        when(refreshTokenService.createRefreshToken(any(User.class)))
                .thenReturn("refresh-token");

        AuthResponseDto response = authService.login(loginDto);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.user()).isNotNull();
        assertThat(response.user().fullName()).isEqualTo("Juan David Angeles Medina");
        assertThat(response.user().email()).isEqualTo("juandavid151199@gmail.com");
    }

    @Test
    void failedLoginUserNotFound() {

        LoginRequestDto loginDto = new LoginRequestDto(
                "fantasma@gmail.com",
                "960358293"
        );

        when(userRepository.findByEmail(loginDto.email()))
                .thenReturn(Optional.empty());

        Throwable error = catchThrowable(() -> authService.login(loginDto));

        assertThat(error)
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales incorrectas");
    }

    @Test
    void failedLoginWithGoogle() throws Exception {

        GoogleLoginRequestDto loginRequestDto = new GoogleLoginRequestDto("token");

        when(googleIdTokenVerifier.verify(anyString()))
                .thenReturn(null);

        Throwable error = catchThrowable(() -> authService.loginWithGoogle(loginRequestDto));

        assertThat(error)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Token de Google invalido");
    }

    @Test
    void successfulLoginWithGoogle() throws Exception {

        GoogleLoginRequestDto loginDto = new GoogleLoginRequestDto("token");

        GoogleIdToken idTokenMock = mock(GoogleIdToken.class);

        GoogleIdToken.Payload payloadFake = new GoogleIdToken.Payload();
        payloadFake.setEmail("juandavid151199@gmail.com");
        payloadFake.set("name", "JUAN DAVID ANGELES MEDINA");

        when(idTokenMock.getPayload())
                .thenReturn(payloadFake);

        when(googleIdTokenVerifier.verify(anyString()))
                .thenReturn(idTokenMock);

        User fakeUser = User.builder()
                .email("juandavid151199@gmail.com")
                .fullName("JUAN DAVID ANGELES MEDINA")
                .password("password")
                .build();

        when(userRepository.findByEmail("juandavid151199@gmail.com"))
                .thenReturn(Optional.of(fakeUser));

        when(jwtService.generateToken(any(SecurityUser.class)))
                .thenReturn("jwt");

        when(refreshTokenService.createRefreshToken(any(User.class)))
                .thenReturn("refresh-google");

        AuthResponseDto response = authService.loginWithGoogle(loginDto);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("jwt");
        assertThat(response.refreshToken()).isEqualTo("refresh-google");
        assertThat(response.user()).isNotNull();
        assertThat(response.user().fullName()).isEqualTo("JUAN DAVID ANGELES MEDINA");
        assertThat(response.user().email()).isEqualTo("juandavid151199@gmail.com");
    }

    @Test
    void successfulLoginWithGoogleNewUser() throws Exception {

        GoogleLoginRequestDto loginDto = new GoogleLoginRequestDto("token");

        GoogleIdToken idTokenMock = mock(GoogleIdToken.class);

        GoogleIdToken.Payload payloadFake = new GoogleIdToken.Payload();
        payloadFake.setEmail("nuevo-usuario@gmail.com");
        payloadFake.set("name", "Nuevo Usuario");

        when(idTokenMock.getPayload())
                .thenReturn(payloadFake);

        when(googleIdTokenVerifier.verify(anyString()))
                .thenReturn(idTokenMock);

        when(userRepository.findByEmail("nuevo-usuario@gmail.com"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode(anyString()))
                .thenReturn("clave-aleatoria-encriptada");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(jwtService.generateToken(any(SecurityUser.class)))
                .thenReturn("jwt-nuevo");
        when(refreshTokenService.createRefreshToken(any(User.class)))
                .thenReturn("refresh-nuevo");

        AuthResponseDto response = authService.loginWithGoogle(loginDto);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("jwt-nuevo");
        assertThat(response.refreshToken()).isEqualTo("refresh-nuevo");
        assertThat(response.user()).isNotNull();
        assertThat(response.user().fullName()).isEqualTo("Nuevo Usuario");
        assertThat(response.user().email()).isEqualTo("nuevo-usuario@gmail.com");

        verify(userRepository).save(any(User.class));
    }

    @Test
    void getCurrentUser() {

        User fakeUser = User.builder()
                .fullName("Juan David Angeles Medina")
                .email("juandavid151199@gmail.com")
                .password("contraseña-encriptada")
                .build();

        SecurityUser securityUser = new SecurityUser(fakeUser);

        AuthenticatedUserDto response = authService.getCurrentUser(securityUser);

        assertThat(response).isNotNull();
        assertThat(response.fullName()).isEqualTo("Juan David Angeles Medina");
        assertThat(response.email()).isEqualTo("juandavid151199@gmail.com");

    }

    @Test
    void refreshToken() {

        RefreshTokenRequestDto request =
                new RefreshTokenRequestDto("refresh-token-viejo");

        User fakeUser = User.builder()
                .fullName("Juan David Angeles Medina")
                .email("juandavid151199@gmail.com")
                .password("contraseña-encriptada")
                .build();

        when(refreshTokenService.validateAndRotate("refresh-token-viejo"))
                .thenReturn(fakeUser);

        when(refreshTokenService.createRefreshToken(fakeUser))
                .thenReturn("refresh-token-nuevo");

        when(jwtService.generateToken(any(SecurityUser.class)))
                .thenReturn("access-token-nuevo");

        AuthResponseDto response = authService.refreshToken(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-token-nuevo");
        assertThat(response.refreshToken()).isEqualTo("refresh-token-nuevo");
        assertThat(response.user()).isNotNull();
        assertThat(response.user().fullName()).isEqualTo("Juan David Angeles Medina");
        assertThat(response.user().email()).isEqualTo("juandavid151199@gmail.com");

        verify(refreshTokenService).validateAndRotate("refresh-token-viejo");
        verify(refreshTokenService).createRefreshToken(fakeUser);
    }

    @Test
    void logout() {

        RefreshTokenRequestDto request =
                new RefreshTokenRequestDto("refresh-token");

        authService.logout(request);

        verify(refreshTokenService).revoke("refresh-token");
    }
}