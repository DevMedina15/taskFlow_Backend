package com.taskflow.api.project.scheduler;

import com.taskflow.api.project.entity.InvitationStatus;
import com.taskflow.api.project.repository.ProjectInvitationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvitationCleanupScheduler {

    private final ProjectInvitationRepository invitationRepository;

    // Ejecucion cada hora
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanExpiredInvitations() {
        log.info("Cron Job: Iniciando limpieza automática de invitaciones vencidas...");

        int modifiedRows = invitationRepository.expireOldInvitations(
                InvitationStatus.EXPIRED,
                InvitationStatus.PENDING,
                LocalDateTime.now()
        );

        if (modifiedRows > 0) {
            log.info("Cron Job: Éxito. Se cambiaron {} invitaciones vencidas a estado EXPIRED.", modifiedRows);
        } else {
            log.info("Cron Job: No se encontraron invitaciones vencidas para limpiar.");
        }
    }
}
