package com.taskflow.api.task.controller;

import com.taskflow.api.shared.dto.PageResponseDto;
import com.taskflow.api.task.dto.ChangeTaskStatusRequestDto;
import com.taskflow.api.task.dto.CreateTaskRequestDto;
import com.taskflow.api.task.dto.TaskResponseDto;
import com.taskflow.api.task.dto.UpdateTaskRequestDto;
import com.taskflow.api.task.entity.TaskPriority;
import com.taskflow.api.task.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping("/{projectId}/tasks")
    public ResponseEntity<TaskResponseDto> createTask (
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateTaskRequestDto request
            ) {
        TaskResponseDto response = taskService.createTask(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }

    @PutMapping("/{projectId}/tasks/{taskId}")
    public ResponseEntity<TaskResponseDto> updateTask (
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskRequestDto request
    ) {
        TaskResponseDto response = taskService.updateTask(projectId, taskId, request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{projectId}/tasks")
    public ResponseEntity<PageResponseDto<TaskResponseDto>> findAllTasks (

            @PathVariable UUID projectId,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false, defaultValue = "false") boolean myTasks,
            @RequestParam(required = false, defaultValue = "false") boolean myReviews,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {

        Page<TaskResponseDto> page = taskService.getTaskWithFilters(
                projectId,
                priority,
                myTasks,
                myReviews,
                pageable
        );

        PageResponseDto<TaskResponseDto> cleanResponse = new PageResponseDto<>(
                page.getContent(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                !page.isLast()
        );

        return ResponseEntity.ok(cleanResponse);

    }

    @GetMapping("/{projectId}/tasks/{taskId}")
    public ResponseEntity<TaskResponseDto> findTask (
            @PathVariable UUID projectId,
            @PathVariable UUID taskId
    ) {
        TaskResponseDto response = taskService.getById(projectId, taskId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{projectId}/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable UUID projectId,
            @PathVariable UUID taskId
    ) {
        taskService.deleteById(projectId, taskId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{projectId}/tasks/{taskId}/status")
    public ResponseEntity<TaskResponseDto> updateTaskStatus(
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @Valid @RequestBody ChangeTaskStatusRequestDto request
    ) {
        taskService.updateTaskStatus(projectId, taskId, request);

        return ResponseEntity.noContent().build();
    }
}
