package com.taskflow.api.user.service;

import com.taskflow.api.user.entity.User;
import com.taskflow.api.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("Debe cargar al usuario exitosamente y mapearlo a SecurityUser")
    void loadUserByUsername_Success() {
        String email = "test@gmail.com";
        User user = User.builder().email(email).password("password123").build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserDetails result = customUserDetailsService.loadUserByUsername(email);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(email);
        verify(userRepository).findByEmail(email);
    }

    @Test
    @DisplayName("Debe lanzar UsernameNotFoundException si el usuario no existe")
    void loadUserByUsername_NotFound() {
        String email = "noexiste@gmail.com";

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        Throwable error = catchThrowable(() -> customUserDetailsService.loadUserByUsername(email));

        assertThat(error).isInstanceOf(UsernameNotFoundException.class).hasMessage("Usuario no encontrado");
        verify(userRepository).findByEmail(email);
    }
}