package com.taskflow.api.project.controller;

import com.taskflow.api.project.dto.*;
import com.taskflow.api.project.entity.ProjectRole;
import com.taskflow.api.project.entity.ProjectStatus;
import com.taskflow.api.project.service.ProjectService;
import com.taskflow.api.shared.dto.PageResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ProjectResponseDto> createProject(
            @Valid @RequestBody CreateProjectRequestDto request,
            Principal principal)
    {
        String currentUserEmail = principal.getName();
        ProjectResponseDto response = projectService.createProject(request, currentUserEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponseDto<ProjectResponseDto>> getUserProjects(

            @RequestParam(required = false) ProjectRole role,
            @RequestParam(required = false)ProjectStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Principal principal)
    {
        String currentUserEmail = principal.getName();

        Page<ProjectResponseDto> page = projectService.getUserProjects(
                role,
                status,
                search,
                pageable,
                currentUserEmail
        );

        PageResponseDto<ProjectResponseDto> cleanResponse = new PageResponseDto<>(
                page.getContent(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                !page.isLast()
        );
        return ResponseEntity.ok(cleanResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponseDto> updateProject
            (@PathVariable UUID id,
             @Valid @RequestBody UpdateProjectRequestDto request
    ) {
        ProjectResponseDto updatedProject = projectService.updateProject(id, request);
        return ResponseEntity.ok(updatedProject);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable UUID id,
            Principal principal
    )
    {
        String currentUserEmail = principal.getName();
        projectService.deleteProject(id, currentUserEmail);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{projectId}/members")
    public ResponseEntity<List<ProjectMemberResponseDto>> getProjectMembers(@PathVariable UUID projectId) {
        List<ProjectMemberResponseDto> members = projectService.getProjectMembers(projectId);
        return ResponseEntity.ok(members);
    }

    @DeleteMapping("/{projectId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID projectId,
            @PathVariable UUID userId
    ) {
        projectService.removeMemberFromProject(projectId, userId);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{projectId}/members/{userId}/role")
    public ResponseEntity<Void> updateMemberRole(
            @PathVariable UUID projectId,
            @PathVariable UUID userId,
            @RequestBody UpdateMemberRoleRequestDto request
    ) {
        projectService.updateMemberRole(projectId, userId, request.role());
        return ResponseEntity.ok().build();
    }

}
