package com.taskflow.api.project.controller;

import com.taskflow.api.project.dto.InviteUserRequestDto;
import com.taskflow.api.project.service.ProjectInvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
