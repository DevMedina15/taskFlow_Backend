package com.taskflow.api.task.service;

import com.taskflow.api.project.entity.Project;
import com.taskflow.api.project.repository.ProjectRepository;
import com.taskflow.api.shared.exception.ResourceNotFoundException;
import com.taskflow.api.task.dto.CreateTaskRequestDto;
import com.taskflow.api.task.dto.TaskResponseDto;
import com.taskflow.api.task.entity.Task;
import com.taskflow.api.task.repository.TaskRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;

    @Transactional
    public TaskResponseDto createTask (UUID projectId, CreateTaskRequestDto request) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado"));

        Task newTask = Task.builder()
                .project(project)
                .title(request.title())
                .description(request.description())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .estimatedTime(request.estimatedTime())
                .gitBranch(request.gitBranch())
                .build();

        Task savedTask = taskRepository.save(newTask);

        return new TaskResponseDto(
                savedTask.getId(),
                savedTask.getTitle(),
                savedTask.getDescription(),
                savedTask.getStatus(),
                savedTask.getPriority(),
                savedTask.getStartDate(),
                savedTask.getEndDate(),
                savedTask.getEstimatedTime(),
                savedTask.getGitBranch(),
                savedTask.getCreatedAt()

        );
    }
}
