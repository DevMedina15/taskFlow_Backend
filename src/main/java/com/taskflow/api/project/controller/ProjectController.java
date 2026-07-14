package com.taskflow.api.project.controller;

import com.taskflow.api.project.dto.CreateProjectRequestDto;
import com.taskflow.api.project.dto.ProjectMemberResponseDto;
import com.taskflow.api.project.dto.ProjectResponseDto;
import com.taskflow.api.project.dto.UpdateProjectRequestDto;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ProjectResponseDto> createProject(@Valid @RequestBody CreateProjectRequestDto request) {
        ProjectResponseDto response = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponseDto<ProjectResponseDto>> getUserProjects(

            @RequestParam(required = false) ProjectRole role,
            @RequestParam(required = false)ProjectStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable

            ) {
        Page<ProjectResponseDto> page = projectService.getUserProjects(
                role,
                status,
                search,
                pageable
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
    public ResponseEntity<Void> deleteProject(@PathVariable UUID id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{projectId}/members")
    public ResponseEntity<List<ProjectMemberResponseDto>> getProjectMembers(@PathVariable UUID projectId) {
        List<ProjectMemberResponseDto> members = projectService.getProjectMembers(projectId);
        return ResponseEntity.ok(members);
    }

}
