package com.taskflow.api.task.controller;

import com.taskflow.api.task.dto.CreateTaskRequestDto;
import com.taskflow.api.task.dto.TaskResponseDto;
import com.taskflow.api.task.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
}
