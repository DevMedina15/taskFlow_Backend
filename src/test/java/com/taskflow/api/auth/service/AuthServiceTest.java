package com.taskflow.api.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.taskflow.api.auth.dto.*;
import com.taskflow.api.auth.service.result.AuthSessionResult;
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

        verify(refreshTokenService, never()).createRefreshToken(any(User.class));
        verify(jwtService, never()).generateToken(any(SecurityUser.class));
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

        when(refreshTokenService.createRefreshToken(any(User.class)))
                .thenReturn("refresh-token");

        when(jwtService.generateToken(any(SecurityUser.class)))
                .thenReturn("access-token");

        AuthSessionResult response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-token");
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

        verify(refreshTokenService, never()).createRefreshToken(any(User.class));
        verify(jwtService, never()).generateToken(any(SecurityUser.class));
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

        when(refreshTokenService.createRefreshToken(any(User.class)))
                .thenReturn("refresh-token");

        when(jwtService.generateToken(any(SecurityUser.class)))
                .thenReturn("access-token");

        AuthSessionResult response = authService.login(loginDto);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-token");
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

        verify(refreshTokenService, never()).createRefreshToken(any(User.class));
        verify(jwtService, never()).generateToken(any(SecurityUser.class));
    }

    @Test
    void failedLoginWithGoogle() throws Exception {

        GoogleLoginRequestDto loginRequestDto =
                new GoogleLoginRequestDto("token");

        when(googleIdTokenVerifier.verify(anyString()))
                .thenReturn(null);

        Throwable error =
                catchThrowable(() -> authService.loginWithGoogle(loginRequestDto));

        assertThat(error)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Token de Google invalido");

        verify(refreshTokenService, never()).createRefreshToken(any(User.class));
        verify(jwtService, never()).generateToken(any(SecurityUser.class));
    }

    @Test
    void successfulLoginWithGoogle() throws Exception {

        GoogleLoginRequestDto loginDto =
                new GoogleLoginRequestDto("token");

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

        when(refreshTokenService.createRefreshToken(any(User.class)))
                .thenReturn("refresh-google");

        when(jwtService.generateToken(any(SecurityUser.class)))
                .thenReturn("access-google");

        AuthSessionResult response =
                authService.loginWithGoogle(loginDto);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-google");
        assertThat(response.refreshToken()).isEqualTo("refresh-google");
        assertThat(response.user()).isNotNull();
        assertThat(response.user().fullName()).isEqualTo("JUAN DAVID ANGELES MEDINA");
        assertThat(response.user().email()).isEqualTo("juandavid151199@gmail.com");
    }

    @Test
    void successfulLoginWithGoogleNewUser() throws Exception {

        GoogleLoginRequestDto loginDto =
                new GoogleLoginRequestDto("token");

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

        when(refreshTokenService.createRefreshToken(any(User.class)))
                .thenReturn("refresh-nuevo");

        when(jwtService.generateToken(any(SecurityUser.class)))
                .thenReturn("access-nuevo");

        AuthSessionResult response =
                authService.loginWithGoogle(loginDto);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-nuevo");
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

        AuthenticatedUserDto response =
                authService.getCurrentUser(securityUser);

        assertThat(response).isNotNull();
        assertThat(response.fullName()).isEqualTo("Juan David Angeles Medina");
        assertThat(response.email()).isEqualTo("juandavid151199@gmail.com");
    }

    @Test
    void refreshToken() {

        String oldRefreshToken = "refresh-token-viejo";

        User fakeUser = User.builder()
                .fullName("Juan David Angeles Medina")
                .email("juandavid151199@gmail.com")
                .password("contraseña-encriptada")
                .build();

        when(refreshTokenService.validateAndRotate(oldRefreshToken))
                .thenReturn(fakeUser);

        when(refreshTokenService.createRefreshToken(fakeUser))
                .thenReturn("refresh-token-nuevo");

        when(jwtService.generateToken(any(SecurityUser.class)))
                .thenReturn("access-token-nuevo");

        AuthSessionResult response =
                authService.refreshToken(oldRefreshToken);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-token-nuevo");
        assertThat(response.refreshToken()).isEqualTo("refresh-token-nuevo");
        assertThat(response.user()).isNotNull();
        assertThat(response.user().fullName()).isEqualTo("Juan David Angeles Medina");
        assertThat(response.user().email()).isEqualTo("juandavid151199@gmail.com");

        verify(refreshTokenService).validateAndRotate(oldRefreshToken);
        verify(refreshTokenService).createRefreshToken(fakeUser);
    }

    @Test
    void failedRefreshTokenWhenTokenIsNull() {

        Throwable error =
                catchThrowable(() -> authService.refreshToken(null));

        assertThat(error)
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Refresh token inválido");

        verify(refreshTokenService, never()).validateAndRotate(anyString());
        verify(refreshTokenService, never()).createRefreshToken(any(User.class));
        verify(jwtService, never()).generateToken(any(SecurityUser.class));
    }

    @Test
    void failedRefreshTokenWhenTokenIsBlank() {

        Throwable error =
                catchThrowable(() -> authService.refreshToken("   "));

        assertThat(error)
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Refresh token inválido");

        verify(refreshTokenService, never()).validateAndRotate(anyString());
        verify(refreshTokenService, never()).createRefreshToken(any(User.class));
        verify(jwtService, never()).generateToken(any(SecurityUser.class));
    }

    @Test
    void logout() {

        String refreshToken = "refresh-token";

        authService.logout(refreshToken);

        verify(refreshTokenService).revoke(refreshToken);
    }

    @Test
    void logoutDoesNothingWhenRefreshTokenIsNull() {

        authService.logout(null);

        verify(refreshTokenService, never()).revoke(anyString());
    }

    @Test
    void logoutDoesNothingWhenRefreshTokenIsBlank() {

        authService.logout("   ");

        verify(refreshTokenService, never()).revoke(anyString());
    }
}