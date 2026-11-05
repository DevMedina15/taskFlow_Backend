package com.taskflow.api.task.service;

import com.taskflow.api.project.entity.Project;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskCommentServiceTest {

    @Mock
    private TaskCommentRepository commentRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private TaskCommentService commentService;

    // ==========================================
    // TESTS PARA: getValidatedTaskContext
    // ==========================================

    @Test
    @DisplayName("Fallar si la tarea no existe al intentar comentar")
    void createComment_TaskNotFound() {
        when(taskRepository.findById(any())).thenReturn(Optional.empty());

        Throwable error = catchThrowable(() -> commentService.createComment(UUID.randomUUID(), new CreateCommentRequestDto("Test"), "u@gmail.com"));

        assertThat(error).isInstanceOf(ResourceNotFoundException.class).hasMessage("Tarea no encontrada");
    }

    @Test
    @DisplayName("Fallar si el usuario no es miembro del proyecto al intentar comentar")
    void createComment_NotProjectMember() {
        UUID taskId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        String email = "guest@gmail.com";

        Project project = Project.builder().build();
        project.setId(projectId);

        Task task = Task.builder().project(project).build();
        task.setId(taskId);

        User user = User.builder().email(email).build();
        user.setId(UUID.randomUUID());

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(projectRepository.findRoleByProjectAndUser(projectId, user.getId())).thenReturn(Optional.empty());

        Throwable error = catchThrowable(() -> commentService.createComment(taskId, new CreateCommentRequestDto("Test"), email));

        assertThat(error).isInstanceOf(AccessDeniedException.class).hasMessage("No tienes permiso para comentar en esta tarea porque no eres miembro del proyecto");
    }

    // ==========================================
    // TESTS PARA: createComment
    // ==========================================

    @Test
    @DisplayName("Debe crear un comentario con éxito")
    void createComment_Success() {
        UUID taskId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        String email = "user@gmail.com";

        Project project = Project.builder().build();
        project.setId(projectId);

        Task task = Task.builder().project(project).build();
        task.setId(taskId);

        User user = User.builder().email(email).fullName("Juan Angeles").build();
        user.setId(UUID.randomUUID());

        CreateCommentRequestDto request = new CreateCommentRequestDto("Este es un comentario");

        TaskComment savedComment = TaskComment.builder()
                .content("Este es un comentario")
                .task(task)
                .author(user)
                .build();
        savedComment.setId(UUID.randomUUID());

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(projectRepository.findRoleByProjectAndUser(projectId, user.getId())).thenReturn(Optional.of(ProjectRole.DEVELOPER));
        when(commentRepository.save(any(TaskComment.class))).thenReturn(savedComment);

        CommentResponseDto response = commentService.createComment(taskId, request, email);

        assertThat(response.content()).isEqualTo("Este es un comentario");
        verify(commentRepository).save(any(TaskComment.class));
    }

    // ==========================================
    // TESTS PARA: getCommentsForTask
    // ==========================================

    @Test
    @DisplayName("Debe retornar la lista de comentarios de una tarea")
    void getCommentsForTask_Success() {
        UUID taskId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        String email = "user@gmail.com";

        Project project = Project.builder().build();
        project.setId(projectId);

        Task task = Task.builder().project(project).build();
        task.setId(taskId);

        User user = User.builder().email(email).fullName("Juan Angeles").build();
        user.setId(UUID.randomUUID());

        TaskComment comment = TaskComment.builder().task(task).author(user).content("Comentario 1").build();
        comment.setId(UUID.randomUUID());

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(projectRepository.findRoleByProjectAndUser(projectId, user.getId())).thenReturn(Optional.of(ProjectRole.DEVELOPER));
        when(commentRepository.findAllByTaskIdOrderByCreatedAtAsc(taskId)).thenReturn(List.of(comment));

        List<CommentResponseDto> result = commentService.getCommentsForTask(taskId, email);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).content()).isEqualTo("Comentario 1");
    }

    // ==========================================
    // TESTS PARA: deleteComment
    // ==========================================

    @Test
    @DisplayName("Debe eliminar comentario si el usuario es el autor original")
    void deleteComment_Success_AsAuthor() {
        UUID commentId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        String email = "author@gmail.com";

        User author = User.builder().email(email).build();
        author.setId(UUID.randomUUID()); // Mismo ID que el currentUser

        Project project = Project.builder().build();
        project.setId(projectId);

        Task task = Task.builder().project(project).build();
        task.setId(UUID.randomUUID());

        TaskComment comment = TaskComment.builder().author(author).task(task).build();
        comment.setId(commentId);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(author));
        when(projectRepository.findRoleByProjectAndUser(projectId, author.getId())).thenReturn(Optional.of(ProjectRole.DEVELOPER));

        commentService.deleteComment(commentId, email);

        verify(commentRepository).delete(comment);
    }

    @Test
    @DisplayName("Debe eliminar comentario de otro usuario si el actual es OWNER o MANAGER")
    void deleteComment_Success_AsManager() {
        UUID commentId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        String email = "manager@gmail.com";

        User author = User.builder().email("author@gmail.com").build();
        author.setId(UUID.randomUUID());

        User manager = User.builder().email(email).build();
        manager.setId(UUID.randomUUID()); // ID distinto al autor

        Project project = Project.builder().build();
        project.setId(projectId);

        Task task = Task.builder().project(project).build();
        task.setId(UUID.randomUUID());

        TaskComment comment = TaskComment.builder().author(author).task(task).build();
        comment.setId(commentId);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(manager));
        // El usuario actual es MANAGER
        when(projectRepository.findRoleByProjectAndUser(projectId, manager.getId())).thenReturn(Optional.of(ProjectRole.MANAGER));

        commentService.deleteComment(commentId, email);

        verify(commentRepository).delete(comment);
    }

    @Test
    @DisplayName("Fallar al eliminar si no es autor y no tiene rol privilegiado")
    void deleteComment_Fail_NotAuthorNorPrivileged() {
        UUID commentId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        String email = "dev@gmail.com";

        User author = User.builder().email("author@gmail.com").build();
        author.setId(UUID.randomUUID());

        User otherDev = User.builder().email(email).build();
        otherDev.setId(UUID.randomUUID()); // ID distinto al autor

        Project project = Project.builder().build();
        project.setId(projectId);

        Task task = Task.builder().project(project).build();
        task.setId(UUID.randomUUID());

        TaskComment comment = TaskComment.builder().author(author).task(task).build();
        comment.setId(commentId);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(otherDev));
        // El usuario actual es un DEVELOPER normal
        when(projectRepository.findRoleByProjectAndUser(projectId, otherDev.getId())).thenReturn(Optional.of(ProjectRole.DEVELOPER));

        Throwable error = catchThrowable(() -> commentService.deleteComment(commentId, email));

        assertThat(error).isInstanceOf(AccessDeniedException.class).hasMessage("No tienes permisos para eliminar este comentario");
        verify(commentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Fallar al eliminar si el comentario no existe")
    void deleteComment_CommentNotFound() {
        when(commentRepository.findById(any())).thenReturn(Optional.empty());

        Throwable error = catchThrowable(() -> commentService.deleteComment(UUID.randomUUID(), "user@gmail.com"));

        assertThat(error).isInstanceOf(ResourceNotFoundException.class).hasMessage("Comentario no encontrado");
    }
}