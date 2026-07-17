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
import com.taskflow.api.user.repository.UserRepository;
import com.taskflow.api.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectInvitationService {

    private final ProjectInvitationRepository invitationRepository;
    private final ProjectRepository projectRepository;
    private final RabbitTemplate rabbitTemplate; // El motor que Spring nos da para hablar con RabbitMQ
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Transactional
    public void inviteUser(UUID projectId, String guestEmail, ProjectRole role) {

        // Paso 1: Validar la existencia del proyecto
        // Si el ID del proyecto no existe en la base de datos, lanzamos un 404 de inmediato.
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado"));

        // Paso 2: Crear la Entidad de forma explícita
        // Definimos todas las reglas de negocio.
        ProjectInvitation invitation = ProjectInvitation.builder()
                .project(project)
                .guestEmail(guestEmail)
                .role(role)
                .token(UUID.randomUUID().toString()) // Generamos un token UUID único para el enlace del correo
                .status(InvitationStatus.PENDING)    // Toda invitación inicia en estado PENDIENTE
                .expirationDate(LocalDateTime.now().plusDays(1)) // La invitación tiene un tiempo de vida estricto de 24 horas
                .build();

        // Paso 3: Guardar en la Base de Datos
        // Primero aseguramos la persistencia. Si la base de datos falla aquí, la transacción hace rollback
        // y nunca se llega a enviar el mensaje a RabbitMQ. Esto evita enviar correos de invitaciones que no existen.
        invitationRepository.save(invitation);

        // Paso 4: Construir el Evento (DTO de mensajería)
        // Empaquetamos solo los datos mínimos que el sistema de correos necesita.
        // Al usar 'invitation.getRole().name()', convertimos el Enum a String para que viaje limpio en el JSON.
        ProjectInvitationEmailEvent emailEvent = new ProjectInvitationEmailEvent(
                invitation.getGuestEmail(),
                project.getName(),
                invitation.getRole().name(),
                invitation.getToken()
        );

        // Paso 5: Publicar el mensaje en RabbitMQ
        // Usamos el 'convertAndSend' de RabbitTemplate. Por detrás, Spring Boot convierte automáticamente
        // el objeto 'emailEvent' a formato JSON gracias al 'JacksonJsonMessageConverter' que configuramos en RabbitMQConfig.
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PROJECT_EXCHANGE,       // ¿A qué cartero le damos el mensaje? (Exchange)
                RabbitMQConfig.INVITATION_ROUTING_KEY, // ¿Qué dirección de entrega tiene? (Routing Key)
                emailEvent                             // El paquete real con la información
        );
    }

    public ProjectInvitationVerifyResponseDto verifyInvitationToken(String token) {
        ProjectInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Token de invitación inválido o no encontrado"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BusinessRuleException("Esta invitación ya fue procesada anteriormente.");
        }

        if (invitation.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("El enlace de invitación ha caducado.");
        }

        return new ProjectInvitationVerifyResponseDto(
                invitation.getProject().getName(),
                invitation.getRole().name(),
                invitation.getGuestEmail()
        );
    }

    @Transactional
    public void acceptInvitation(String token, String currentUserEmail) {
        ProjectInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Token de invitación inválido o no encontrado"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BusinessRuleException("Esta invitación ya fue procesada o ha expirado.");
        }

        if (invitation.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("El enlace de invitación ha caducado.");
        }

        if (!invitation.getGuestEmail().equalsIgnoreCase(currentUserEmail)) {
            throw new AccessDeniedException("No estás autorizado. El correo de la invitación no coincide con tu cuenta.");
        }

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);

        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado en el sistema."));


        ProjectMember newMember = ProjectMember.builder()
                .project(invitation.getProject())
                .user(user)
                .role(invitation.getRole())
                .build();

        projectMemberRepository.save(newMember);
    }

    public List<ProjectInvitationResponseDto> getProjectInvitations(UUID projectId) {

        List<ProjectInvitation> invitations = invitationRepository.findAllByProjectId(projectId);

        return invitations.stream()
                .map(inv -> new ProjectInvitationResponseDto(
                        inv.getId(),
                        inv.getGuestEmail(),
                        inv.getRole(),
                        inv.getStatus(),
                        inv.getExpirationDate(),
                        inv.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
}
