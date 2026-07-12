package com.taskflow.api.task.service;

import com.taskflow.api.project.entity.Project;
import com.taskflow.api.project.repository.ProjectRepository;
import com.taskflow.api.shared.exception.ResourceNotFoundException;
import com.taskflow.api.task.dto.CreateTaskRequestDto;
import com.taskflow.api.task.dto.TaskResponseDto;
import com.taskflow.api.task.entity.Task;
import com.taskflow.api.task.entity.TaskPriority;
import com.taskflow.api.task.repository.TaskRepository;
import com.taskflow.api.user.UserRepository;
import com.taskflow.api.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

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

    @Transactional(readOnly = true)
    public Page<TaskResponseDto> getTaskWithFilters (
            UUID projectId,
            TaskPriority priority,
            boolean myTasks,
            boolean myReviews,
            Pageable pageable) {

        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        boolean projectExist = projectRepository.existsById(projectId);

        if (!projectExist) {
            throw new ResourceNotFoundException("Proyecto no encontrado");
        }

        UUID searchAssigneeId = null;
        UUID searchReviewerId = null;

        if (myTasks || myReviews) {
            User currentUser = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
            if (myTasks) {
                searchAssigneeId = currentUser.getId();
            }
            if (myReviews) {
                searchReviewerId = currentUser.getId();
            }
        }

        Page<Task> taskPage = taskRepository.findTasksWithFilter(
                projectId,
                priority,
                searchAssigneeId,
                searchReviewerId,
                pageable);

        return taskPage.map(task -> new TaskResponseDto(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getStartDate(),
                task.getEndDate(),
                task.getEstimatedTime(),
                task.getGitBranch(),
                task.getCreatedAt()
        ));

    }
}
