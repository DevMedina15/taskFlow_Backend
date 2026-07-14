package com.taskflow.api.project.repository;

import com.taskflow.api.project.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);

    List<ProjectMember> findAllByProjectId(UUID projectId);

    Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);
}
