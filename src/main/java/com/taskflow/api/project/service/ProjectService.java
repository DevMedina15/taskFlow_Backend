package com.taskflow.api.project.service;

import com.taskflow.api.project.dto.CreateProjectRequestDto;
import com.taskflow.api.project.dto.ProjectMemberResponseDto;
import com.taskflow.api.project.dto.ProjectResponseDto;
import com.taskflow.api.project.dto.UpdateProjectRequestDto;
import com.taskflow.api.project.entity.Project;
import com.taskflow.api.project.entity.ProjectMember;
import com.taskflow.api.project.entity.ProjectRole;
import com.taskflow.api.project.entity.ProjectStatus;
import com.taskflow.api.project.repository.ProjectMemberRepository;
import com.taskflow.api.project.repository.ProjectRepository;
import com.taskflow.api.shared.exception.BusinessRuleException;
import com.taskflow.api.shared.exception.ResourceNotFoundException;
import com.taskflow.api.user.UserRepository;
import com.taskflow.api.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;


    @Transactional
    public ProjectResponseDto createProject(CreateProjectRequestDto request) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        String finalCode;
        boolean isCodeExist;

        do {
            finalCode = generateProjectCode();
            isCodeExist = projectRepository.existsByCode(finalCode);
        } while (isCodeExist);

        Project newProject = Project.builder()
                .code(finalCode)
                .name(request.name())
                .description(request.description())
                .organizationName(request.organizationName().toUpperCase())
                .projectKey(request.projectKey().toUpperCase())
                .build();

        projectRepository.save(newProject);

        ProjectMember newProjectMember = ProjectMember.builder()
                .project(newProject)
                .user(currentUser)
                .role(ProjectRole.OWNER)
                .build();

        projectMemberRepository.save(newProjectMember);

        return new ProjectResponseDto(
                newProject.getId(),
                newProject.getCode(),
                newProject.getName(),
                newProject.getDescription(),
                newProject.getOrganizationName(),
                newProject.getProjectKey(),
                newProject.getStatus(),
                newProject.getCreatedAt()
        );
    }

    private String generateProjectCode() {
        return "PROJ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponseDto> getUserProjects (
            ProjectRole role,
            ProjectStatus status,
            String search,
            Pageable pageable ) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser  = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        String searchParam = (search != null && !search.trim().isEmpty())
                ? "%" + search.toLowerCase() + "%"
                : null;

        Page<Project> projectPage = projectRepository.findUserProjectsWithFilters(
                currentUser.getId(),
                role,
                status,
                searchParam,
                pageable
        );

        return projectPage.map(project -> new ProjectResponseDto(
                project.getId(),
                project.getCode(),
                project.getName(),
                project.getDescription(),
                project.getOrganizationName(),
                project.getProjectKey(),
                project.getStatus(),
                project.getCreatedAt()
        ));
    }

    public ProjectResponseDto updateProject (UUID projectId, UpdateProjectRequestDto request) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado"));

        project.setName(request.name());
        project.setDescription(request.description());
        project.setOrganizationName(request.organizationName());

        if (request.status() != null) {
            project.setStatus(request.status());
        }

        Project updatedProject = projectRepository.save(project);

        return new ProjectResponseDto(
                updatedProject.getId(),
                updatedProject.getCode(),
                updatedProject.getName(),
                updatedProject.getDescription(),
                updatedProject.getOrganizationName(),
                updatedProject.getProjectKey(),
                updatedProject.getStatus(),
                updatedProject.getCreatedAt()
        );
    }

    @Transactional
    public void deleteProject(UUID projectId) {

        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        ProjectRole userRole = projectRepository.findRoleByProjectAndUser(projectId, currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("No tienes acceso a este proyecto"));

        if (userRole != ProjectRole.OWNER) {
            throw new AccessDeniedException("Acción denegada, no puedes realizar esta accion");
        }

        projectRepository.deleteById(projectId);
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponseDto> getProjectMembers(UUID projectId) {

        List<ProjectMember> members = projectMemberRepository.findAllByProjectId(projectId);

        return members.stream()
                .map(member -> new ProjectMemberResponseDto(
                        member.getUser().getId(),
                        member.getUser().getEmail(),
                        member.getRole().name(),
                        member.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeMemberFromProject(UUID projectId, UUID userId) {
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no pertenece a este proyecto."));

        if (member.getRole() == ProjectRole.OWNER) throw new BusinessRuleException("No puedes realizar esta accion");

        projectMemberRepository.delete(member);
    }
}
