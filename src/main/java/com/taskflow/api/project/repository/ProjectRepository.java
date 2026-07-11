package com.taskflow.api.project.repository;

import com.taskflow.api.project.entity.Project;
import com.taskflow.api.project.entity.ProjectRole;
import com.taskflow.api.project.entity.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    boolean existsByCode(String code);

    @Query("""
        SELECT p FROM Project p 
        JOIN ProjectMember pm ON p.id = pm.project.id 
        WHERE pm.user.id = :userId 
        AND (:role IS NULL OR pm.role = :role) 
        AND (:status IS NULL OR p.status = :status) 
        AND (:search IS NULL OR 
             LOWER(p.name) LIKE :search OR 
             LOWER(p.code) LIKE :search OR 
             LOWER(p.organizationName) LIKE :search)
    """)
    Page<Project> findUserProjectsWithFilters(
            @Param("userId") UUID userId,
            @Param("role") ProjectRole role,
            @Param("status") ProjectStatus status,
            @Param("search") String search,
            Pageable pageable
    );


    @Query("SELECT pm.role FROM ProjectMember pm WHERE pm.project.id = :projectId AND pm.user.id = :userId")
    Optional<ProjectRole> findRoleByProjectAndUser(@Param("projectId") UUID projectId, @Param("userId") UUID userId);
}
