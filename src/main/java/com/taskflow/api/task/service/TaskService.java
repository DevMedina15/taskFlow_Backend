package com.taskflow.api.task.service;

import com.taskflow.api.project.entity.Project;
import com.taskflow.api.project.repository.ProjectMemberRepository;
import com.taskflow.api.project.repository.ProjectRepository;
import com.taskflow.api.shared.exception.BusinessRuleException;
import com.taskflow.api.shared.exception.ResourceNotFoundException;
import com.taskflow.api.task.dto.CreateTaskRequestDto;
import com.taskflow.api.task.dto.TaskResponseDto;
import com.taskflow.api.task.dto.UpdateTaskRequestDto;
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

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Transactional
    public TaskResponseDto createTask (UUID projectId, CreateTaskRequestDto request) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado"));

        int newSequence = project.getCurrentSequenceTask() + 1;
        project.setCurrentSequenceTask(newSequence);

        String generatedCode = project.getProjectKey() + "-" + newSequence;

        this.validateDates(request.startDate(), request.endDate());

        Task newTask = Task.builder()
                .project(project)
                .title(request.title())
                .code(generatedCode)
                .description(request.description())
                .status(request.status() != null ? request.status() : com.taskflow.api.task.entity.TaskStatus.TO_DO)
                .priority(request.priority() != null ? request.priority() : TaskPriority.LOW)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .estimatedTime(request.estimatedTime())
                .gitBranch(request.gitBranch())
                .build();

        if (request.assigneeId() != null) {
            validateProjectMember(projectId, request.assigneeId(), "El usuario asignado");
            newTask.setAssignee(userRepository.getReferenceById(request.assigneeId()));
        }

        if (request.reviewerId() != null) {
            validateProjectMember(projectId, request.reviewerId(), "El revisor");
            newTask.setReviewer(userRepository.getReferenceById(request.reviewerId()));
        }



        Task savedTask = taskRepository.save(newTask);

        return mapToResponseDto(savedTask);
    }


    @Transactional
    public TaskResponseDto updateTask (UUID projectId, UUID taskId, UpdateTaskRequestDto request) {
        Task  task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada"));

        if (!task.getProject().getId().equals(projectId)) {
            throw new BusinessRuleException("La tarea no pertenece  al proyecto especificado");
        }

        this.validateDates(request.startDate(), request.endDate());

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status());
        task.setPriority(request.priority());
        task.setStartDate(request.startDate());
        task.setEndDate(request.endDate());
        task.setEstimatedTime(request.estimatedTime());
        task.setGitBranch(request.gitBranch());
        if (request.assigneeId() != null) {
            validateProjectMember(projectId, request.assigneeId(), "El usuario asignado");
            task.setAssignee(userRepository.getReferenceById(request.assigneeId()));
        } else{
            task.setAssignee(null);
        }

        if (request.reviewerId() != null) {
            validateProjectMember(projectId, request.reviewerId(), "El revisor");
            task.setReviewer(userRepository.getReferenceById(request.reviewerId()));
        }else{
            task.setReviewer(null);
        }

        Task taskUpdate =  taskRepository.save(task);

        return mapToResponseDto(taskUpdate);

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

        return taskPage.map(this::mapToResponseDto);

    }

    private void validateProjectMember(UUID projectId, UUID userId, String errorPrefix) {
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(projectId, userId);
        if (!isMember) {
            throw new BusinessRuleException(errorPrefix + " no pertenece a este proyecto");
        }
    }

    private void validateDates(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && endDate != null) {
            if (endDate.isBefore(startDate)) {
                throw new BusinessRuleException("La fecha de finalización no puede ser menor a la fecha de inicio");
            }
        }
    }

    private TaskResponseDto mapToResponseDto(Task task) {
        return new TaskResponseDto(
                task.getId(),
                task.getTitle(),
                task.getCode(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getStartDate(),
                task.getEndDate(),
                task.getEstimatedTime(),
                task.getGitBranch(),
                task.getAssignee() != null ? task.getAssignee().getId() : null,
                task.getReviewer() != null ? task.getReviewer().getId() : null,
                task.getCreatedAt()
        );
    }
}
