package com.taskflow.api.project.service;

import com.taskflow.api.project.dto.ProjectInvitationEmailEvent;
import com.taskflow.api.project.dto.ProjectInvitationResponseDto;
import com.taskflow.api.project.dto.ProjectInvitationVerifyResponseDto;
import com.taskflow.api.project.entity.*;
import com.taskflow.api.project.repository.ProjectInvitationRepository;
import com.taskflow.api.project.repository.ProjectMemberRepository;
import com.taskflow.api.project.repository.ProjectRepository;
import com.taskflow.api.shared.config.RabbitMQConfig;
import com.taskflow.api.shared.exception.BusinessRuleException;
import com.taskflow.api.shared.exception.ResourceNotFoundException;
import com.taskflow.api.user.UserRepository;
import com.taskflow.api.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectInvitationServiceTest {

    @Mock
    private ProjectInvitationRepository invitationRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @InjectMocks
    private ProjectInvitationService invitationService;

    // ==========================================
    // TESTS PARA: inviteUser
    // ==========================================

    @Test
    @DisplayName("Debe crear la invitación y enviar el evento a RabbitMQ")
    void inviteUser_Success() {
        UUID projectId = UUID.randomUUID();
        Project project = Project.builder().name("App Project").build();
        String guestEmail = "guest@gmail.com";

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        invitationService.inviteUser(projectId, guestEmail, ProjectRole.DEVELOPER);

        verify(invitationRepository).save(any(ProjectInvitation.class));
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.PROJECT_EXCHANGE),
                eq(RabbitMQConfig.INVITATION_ROUTING_KEY),
                any(ProjectInvitationEmailEvent.class)
        );
    }

    @Test
    @DisplayName("Fallar al invitar si el proyecto no existe")
    void inviteUser_ProjectNotFound() {
        UUID projectId = UUID.randomUUID();
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        Throwable error = catchThrowable(() -> invitationService.inviteUser(projectId, "guest@gmail.com", ProjectRole.DEVELOPER));

        assertThat(error).isInstanceOf(ResourceNotFoundException.class).hasMessage("Proyecto no encontrado");
        verify(invitationRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    // ==========================================
    // TESTS PARA: verifyInvitationToken
    // ==========================================

    @Test
    @DisplayName("Debe verificar el token correctamente y devolver DTO")
    void verifyInvitationToken_Success() {
        String token = UUID.randomUUID().toString();
        Project project = Project.builder().name("App Project").build();
        ProjectInvitation invitation = ProjectInvitation.builder()
                .project(project)
                .role(ProjectRole.DEVELOPER)
                .guestEmail("guest@gmail.com")
                .status(InvitationStatus.PENDING)
                .expirationDate(LocalDateTime.now().plusDays(1))
                .build();

        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(invitation));

        ProjectInvitationVerifyResponseDto result = invitationService.verifyInvitationToken(token);

        assertThat(result.projectName()).isEqualTo("App Project");
        assertThat(result.role()).isEqualTo("DEVELOPER");
        assertThat(result.guestEmail()).isEqualTo("guest@gmail.com");
    }

    @Test
    @DisplayName("Fallar verificación si el token no existe")
    void verifyInvitationToken_NotFound() {
        when(invitationRepository.findByToken(anyString())).thenReturn(Optional.empty());

        Throwable error = catchThrowable(() -> invitationService.verifyInvitationToken("invalid-token"));

        assertThat(error).isInstanceOf(ResourceNotFoundException.class).hasMessage("Token de invitación inválido o no encontrado");
    }

    @Test
    @DisplayName("Fallar verificación si el estado no es PENDING")
    void verifyInvitationToken_NotPending() {
        ProjectInvitation invitation = ProjectInvitation.builder()
                .status(InvitationStatus.ACCEPTED)
                .build();

        when(invitationRepository.findByToken(anyString())).thenReturn(Optional.of(invitation));

        Throwable error = catchThrowable(() -> invitationService.verifyInvitationToken("token"));

        assertThat(error).isInstanceOf(BusinessRuleException.class).hasMessage("Esta invitación ya fue procesada anteriormente.");
    }

    @Test
    @DisplayName("Fallar verificación si el token está expirado")
    void verifyInvitationToken_Expired() {
        ProjectInvitation invitation = ProjectInvitation.builder()
                .status(InvitationStatus.PENDING)
                .expirationDate(LocalDateTime.now().minusDays(1)) // Fecha en el pasado
                .build();

        when(invitationRepository.findByToken(anyString())).thenReturn(Optional.of(invitation));

        Throwable error = catchThrowable(() -> invitationService.verifyInvitationToken("token"));

        assertThat(error).isInstanceOf(BusinessRuleException.class).hasMessage("El enlace de invitación ha caducado.");
    }

    // ==========================================
    // TESTS PARA: acceptInvitation
    // ==========================================

    @Test
    @DisplayName("Debe aceptar la invitación y registrar al usuario como miembro")
    void acceptInvitation_Success() {
        String token = UUID.randomUUID().toString();
        String email = "guest@gmail.com";

        Project project = Project.builder().build();
        project.setId(UUID.randomUUID());

        User user = User.builder().email(email).build();
        user.setId(UUID.randomUUID());

        ProjectInvitation invitation = ProjectInvitation.builder()
                .project(project)
                .guestEmail(email)
                .role(ProjectRole.DEVELOPER)
                .status(InvitationStatus.PENDING)
                .expirationDate(LocalDateTime.now().plusDays(1))
                .build();

        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(invitation));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        invitationService.acceptInvitation(token, email);

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        verify(invitationRepository).save(invitation);
        verify(projectMemberRepository).save(any(ProjectMember.class));
    }

    @Test
    @DisplayName("Fallar al aceptar si el correo no coincide")
    void acceptInvitation_EmailMismatch() {
        String token = UUID.randomUUID().toString();
        ProjectInvitation invitation = ProjectInvitation.builder()
                .guestEmail("guest@gmail.com")
                .status(InvitationStatus.PENDING)
                .expirationDate(LocalDateTime.now().plusDays(1))
                .build();

        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(invitation));

        Throwable error = catchThrowable(() -> invitationService.acceptInvitation(token, "hacker@gmail.com"));

        assertThat(error).isInstanceOf(AccessDeniedException.class).hasMessage("No estás autorizado. El correo de la invitación no coincide con tu cuenta.");
        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Fallar al aceptar si el usuario no existe en la BD")
    void acceptInvitation_UserNotFound() {
        String token = UUID.randomUUID().toString();
        String email = "guest@gmail.com";
        ProjectInvitation invitation = ProjectInvitation.builder()
                .guestEmail(email)
                .status(InvitationStatus.PENDING)
                .expirationDate(LocalDateTime.now().plusDays(1))
                .build();

        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(invitation));
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        Throwable error = catchThrowable(() -> invitationService.acceptInvitation(token, email));

        assertThat(error).isInstanceOf(ResourceNotFoundException.class).hasMessage("Usuario no encontrado en el sistema.");
        verify(projectMemberRepository, never()).save(any());
    }

    // ==========================================
    // TESTS PARA: getProjectInvitations
    // ==========================================

    @Test
    @DisplayName("Debe listar las invitaciones de un proyecto")
    void getProjectInvitations_Success() {
        UUID projectId = UUID.randomUUID();
        ProjectInvitation invitation = ProjectInvitation.builder()
                .guestEmail("guest@gmail.com")
                .role(ProjectRole.DEVELOPER)
                .status(InvitationStatus.PENDING)
                .build();

        when(invitationRepository.findAllByProjectId(projectId)).thenReturn(List.of(invitation));

        List<ProjectInvitationResponseDto> result = invitationService.getProjectInvitations(projectId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).guestEmail()).isEqualTo("guest@gmail.com");
        assertThat(result.get(0).role()).isEqualTo(ProjectRole.DEVELOPER);
        assertThat(result.get(0).status()).isEqualTo(InvitationStatus.PENDING);
    }
}