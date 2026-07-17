package com.taskflow.api.task.service;

import com.taskflow.api.project.entity.ProjectRole;
import com.taskflow.api.project.repository.ProjectRepository;
import com.taskflow.api.shared.exception.ResourceNotFoundException;
import com.taskflow.api.task.dto.CommentResponseDto;
import com.taskflow.api.task.dto.CreateCommentRequestDto;
import com.taskflow.api.task.entity.Task;
import com.taskflow.api.task.entity.TaskComment;
import com.taskflow.api.task.repository.TaskCommentRepository;
import com.taskflow.api.task.repository.TaskRepository;
import com.taskflow.api.user.repository.UserRepository;
import com.taskflow.api.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskCommentService {

    private final TaskCommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    private record TaskContext(Task task, User user) {}

    private TaskContext getValidatedTaskContext(UUID taskId, String userEmail, String accessErrorMessage) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada"));

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        UUID projectId = task.getProject().getId();
        boolean isMember = projectRepository.findRoleByProjectAndUser(projectId, currentUser.getId()).isPresent();

        if (!isMember) {
            throw new AccessDeniedException(accessErrorMessage);
        }

        return new TaskContext(task, currentUser);
    }

    @Transactional
    public CommentResponseDto createComment(UUID taskId, CreateCommentRequestDto request, String userEmail) {

        TaskContext context = getValidatedTaskContext(
                taskId,
                userEmail,
                "No tienes permiso para comentar en esta tarea porque no eres miembro del proyecto"
        );

        TaskComment comment = TaskComment.builder()
                .content(request.content())
                .task(context.task())
                .author(context.user())
                .build();

        TaskComment savedComment = commentRepository.save(comment);

        return mapToResponseDto(savedComment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> getCommentsForTask(UUID taskId, String userEmail) {

        TaskContext context = getValidatedTaskContext(
                taskId,
                userEmail,
                "No tienes permiso para comentar en esta tarea porque no eres miembro del proyecto"
        );


        List<TaskComment> comments = commentRepository.findAllByTaskIdOrderByCreatedAtAsc(context.task().getId());

        return comments.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteComment(UUID commentId, String userEmail) {
        TaskComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comentario no encontrado"));

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        UUID projectId = comment.getTask().getProject().getId();

        ProjectRole userRole = projectRepository.findRoleByProjectAndUser(projectId, currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("No tienes acceso a este proyecto"));

        boolean isAuthor = comment.getAuthor().getId().equals(currentUser.getId());
        boolean isPrivilegedUser = userRole == ProjectRole.OWNER || userRole == ProjectRole.MANAGER;

        if (!isAuthor && !isPrivilegedUser) {
            throw new AccessDeniedException("No tienes permisos para eliminar este comentario");
        }

        commentRepository.delete(comment);
    }

    private CommentResponseDto mapToResponseDto(TaskComment comment) {
        return new CommentResponseDto(
                comment.getId(),
                comment.getContent(),
                comment.getAuthor().getId(),
                comment.getAuthor().getFullName(),
                comment.getAuthor().getEmail(),
                comment.getCreatedAt()
        );
    }
}
