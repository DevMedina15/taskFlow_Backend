package com.taskflow.api.project.controller;

import com.taskflow.api.project.dto.InviteUserRequestDto;
import com.taskflow.api.project.dto.ProjectInvitationResponseDto;
import com.taskflow.api.project.dto.ProjectInvitationVerifyResponseDto;
import com.taskflow.api.project.service.ProjectInvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectInvitationController {

    private final ProjectInvitationService invitationService;

    @PostMapping("/{projectId}/invitations")
    public ResponseEntity<Void> inviteUser(
            @PathVariable UUID projectId,
            @Valid @RequestBody InviteUserRequestDto request
    ) {
        // Ejecutamos el flujo completo del servicio (BD + RabbitMQ)
        invitationService.inviteUser(projectId, request.guestEmail(), request.role());

        // Retornamos un HTTP 201 Created sin cuerpo, ya que la acción se completó con éxito
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // Endpoint sin autenticacion para la hora de la invitacion se pueda ver al darle click en el front
    @GetMapping("/verify")
    public ResponseEntity<ProjectInvitationVerifyResponseDto> verifyToken(@RequestParam String token) {
        ProjectInvitationVerifyResponseDto response = invitationService.verifyInvitationToken(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/invitations/accept")
    public ResponseEntity<Void> acceptInvitation(
            @RequestParam String token,
            Principal principal
    ) {
        String currentUserEmail = principal.getName();
        invitationService.acceptInvitation(token, currentUserEmail);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{projectId}/invitations")
    public ResponseEntity<List<ProjectInvitationResponseDto>> getProjectInvitations(
            @PathVariable UUID projectId
    ) {
        List<ProjectInvitationResponseDto> invitations = invitationService.getProjectInvitations(projectId);
        return ResponseEntity.ok(invitations);
    }
}
