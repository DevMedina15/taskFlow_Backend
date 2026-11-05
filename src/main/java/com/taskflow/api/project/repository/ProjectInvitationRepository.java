package com.taskflow.api.project.repository;

import com.taskflow.api.project.entity.InvitationStatus;
import com.taskflow.api.project.entity.ProjectInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectInvitationRepository extends JpaRepository<ProjectInvitation, UUID> {
    Optional<ProjectInvitation> findByToken(String token);

    @Modifying
    @Query("UPDATE ProjectInvitation pi " +
            "SET pi.status = :newStatus " +
            "WHERE pi.status = :pendingStatus " +
            "AND pi.expirationDate < :now")
    int expireOldInvitations(
            @Param("newStatus") InvitationStatus newStatus,
            @Param("pendingStatus") InvitationStatus pendingStatus,
            @Param("now") LocalDateTime now
    );

    List<ProjectInvitation> findAllByProjectId(UUID projectId);
}
