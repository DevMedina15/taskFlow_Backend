package com.taskflow.api.project.service;

import com.taskflow.api.project.dto.CreateProjectRequestDto;
import com.taskflow.api.project.dto.ProjectResponseDto;
import com.taskflow.api.project.entity.Project;
import com.taskflow.api.project.entity.ProjectMember;
import com.taskflow.api.project.entity.ProjectRole;
import com.taskflow.api.project.repository.ProjectMemberRepository;
import com.taskflow.api.project.repository.ProjectRepository;
import com.taskflow.api.shared.exception.ResourceNotFoundException;
import com.taskflow.api.user.UserRepository;
import com.taskflow.api.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado en la base de datos"));

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
                newProject.getStatus(),
                newProject.getCreatedAt()
        );
    }

    private String generateProjectCode() {
        return "PROJ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
