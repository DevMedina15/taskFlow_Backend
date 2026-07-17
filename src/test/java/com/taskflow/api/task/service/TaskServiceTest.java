package com.taskflow.api.task.service;

import com.taskflow.api.project.entity.Project;
import com.taskflow.api.project.entity.ProjectRole;
import com.taskflow.api.project.repository.ProjectMemberRepository;
import com.taskflow.api.project.repository.ProjectRepository;
import com.taskflow.api.shared.exception.BusinessRuleException;
import com.taskflow.api.task.dto.ChangeTaskStatusRequestDto;
import com.taskflow.api.task.dto.CreateTaskRequestDto;
import com.taskflow.api.task.dto.TaskResponseDto;
import com.taskflow.api.task.dto.UpdateTaskRequestDto;
import com.taskflow.api.task.entity.Task;
import com.taskflow.api.task.entity.TaskPriority;
import com.taskflow.api.task.entity.TaskStatus;
import com.taskflow.api.task.repository.TaskRepository;
import com.taskflow.api.user.UserRepository;
import com.taskflow.api.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @InjectMocks
    private TaskService taskService;

    // ==========================================
    // TESTS PARA: createTask
    // ==========================================

    @Test
    @DisplayName("Debe crear una tarea con éxito sin asignados")
    void createTask_Success() {
        UUID projectId = UUID.randomUUID();
        Project project = Project.builder()
                .projectKey("APP")
                .currentSequenceTask(5)
                .build();
        project.setId(projectId);

        CreateTaskRequestDto request = new CreateTaskRequestDto(
                "Nueva Tarea", "Desc", null, null, null, null, 5, "feat/branch", null, null
        );

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task savedTask = invocation.getArgument(0);
            savedTask.setId(UUID.randomUUID());
            return savedTask;
        });

        TaskResponseDto response = taskService.createTask(projectId, request);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Nueva Tarea");
        assertThat(response.code()).isEqualTo("APP-6");
        assertThat(project.getCurrentSequenceTask()).isEqualTo(6);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("Fallar al crear tarea si la fecha fin es antes de la fecha inicio")
    void createTask_InvalidDates() {
        UUID projectId = UUID.randomUUID();
        Project project = Project.builder().projectKey("APP").currentSequenceTask(1).build();
        project.setId(projectId);

        CreateTaskRequestDto request = new CreateTaskRequestDto(
                "T", "D", null, null,
                LocalDateTime.now().plusDays(5), // Start
                LocalDateTime.now().plusDays(1), // End (Inválido)
                5, "branch", null, null
        );

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        Throwable error = catchThrowable(() -> taskService.createTask(projectId, request));

        assertThat(error).isInstanceOf(BusinessRuleException.class)
                .hasMessage("La fecha de finalización no puede ser menor a la fecha de inicio");
        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Fallar al crear tarea si el assignee no es miembro del proyecto")
    void createTask_AssigneeNotMember() {
        UUID projectId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        Project project = Project.builder().projectKey("APP").currentSequenceTask(1).build();
        project.setId(projectId);

        CreateTaskRequestDto request = new CreateTaskRequestDto(
                "T", "D", null, null, null, null, 5, "branch", assigneeId, null
        );

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, assigneeId)).thenReturn(false);

        Throwable error = catchThrowable(() -> taskService.createTask(projectId, request));

        assertThat(error).isInstanceOf(BusinessRuleException.class)
                .hasMessage("El usuario asignado no pertenece a este proyecto");
    }

    // ==========================================
    // TESTS PARA: updateTask
    // ==========================================

    @Test
    @DisplayName("Debe actualizar la tarea exitosamente")
    void updateTask_Success() {
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        Project project = Project.builder().build();
        project.setId(projectId);

        Task task = Task.builder().project(project).title("Viejo").build();
        task.setId(taskId);

        UpdateTaskRequestDto request = new UpdateTaskRequestDto(
                "Nuevo Título", "Desc", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, null, null, 10, "branch", null, null
        );

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        TaskResponseDto response = taskService.updateTask(projectId, taskId, request);

        assertThat(response.title()).isEqualTo("Nuevo Título");
        assertThat(task.getTitle()).isEqualTo("Nuevo Título");
    }

    @Test
    @DisplayName("Fallar al actualizar si la tarea no pertenece al proyecto")
    void updateTask_WrongProject() {
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        Project wrongProject = Project.builder().build();
        wrongProject.setId(UUID.randomUUID()); // ID diferente

        Task task = Task.builder().project(wrongProject).build();
        task.setId(taskId);

        UpdateTaskRequestDto request = new UpdateTaskRequestDto("T", "D", null, null, null, null, 10, "b", null, null);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        Throwable error = catchThrowable(() -> taskService.updateTask(projectId, taskId, request));

        assertThat(error).isInstanceOf(BusinessRuleException.class)
                .hasMessage("La tarea no pertenece  al proyecto especificado");
    }

    // ==========================================
    // TESTS PARA: getTaskWithFilters
    // ==========================================

    @Test
    @DisplayName("Debe listar tareas aplicando el filtro myTasks")
    void getTaskWithFilters_MyTasks() {
        UUID projectId = UUID.randomUUID();
        String email = "test@gmail.com";
        User user = User.builder().email(email).build();
        user.setId(UUID.randomUUID());

        Task task = Task.builder().title("My Task").build();
        task.setId(UUID.randomUUID());

        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> taskPage = new PageImpl<>(List.of(task));

        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // Se busca con myTasks = true (searchAssigneeId = user.getId())
        when(taskRepository.findTasksWithFilter(projectId, null, user.getId(), null, pageable))
                .thenReturn(taskPage);

        Page<TaskResponseDto> result = taskService.getTaskWithFilters(projectId, null, true, false, pageable, email);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("My Task");
    }

    // ==========================================
    // TESTS PARA: getById
    // ==========================================

    @Test
    @DisplayName("Debe retornar la tarea por ID")
    void getById_Success() {
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        Project project = Project.builder().build();
        project.setId(projectId);

        Task task = Task.builder().project(project).title("Encontrada").build();
        task.setId(taskId);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        TaskResponseDto response = taskService.getById(projectId, taskId);

        assertThat(response.title()).isEqualTo("Encontrada");
    }

    // ==========================================
    // TESTS PARA: deleteById
    // ==========================================

    @Test
    @DisplayName("Debe eliminar tarea si el usuario es MANAGER")
    void deleteById_Success() {
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        String email = "manager@gmail.com";

        User user = User.builder().email(email).build();
        user.setId(UUID.randomUUID());

        Project project = Project.builder().build();
        project.setId(projectId);

        Task task = Task.builder().project(project).build();
        task.setId(taskId);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(projectRepository.findRoleByProjectAndUser(projectId, user.getId()))
                .thenReturn(Optional.of(ProjectRole.MANAGER));

        taskService.deleteById(projectId, taskId, email);

        verify(taskRepository).delete(task);
    }

    @Test
    @DisplayName("Fallar al eliminar si el rol es DEVELOPER")
    void deleteById_NotAuthorized() {
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        String email = "dev@gmail.com";

        User user = User.builder().email(email).build();
        user.setId(UUID.randomUUID());

        Project project = Project.builder().build();
        project.setId(projectId);

        Task task = Task.builder().project(project).build();
        task.setId(taskId);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(projectRepository.findRoleByProjectAndUser(projectId, user.getId()))
                .thenReturn(Optional.of(ProjectRole.DEVELOPER));

        Throwable error = catchThrowable(() -> taskService.deleteById(projectId, taskId, email));

        assertThat(error).isInstanceOf(AccessDeniedException.class)
                .hasMessage("Acción denegada, no puedes realizar esta accion");
        verify(taskRepository, never()).delete(any());
    }

    // ==========================================
    // TESTS PARA: updateTaskStatus
    // ==========================================

    @Test
    @DisplayName("Debe cambiar únicamente el estado de la tarea")
    void updateTaskStatus_Success() {
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        Project project = Project.builder().build();
        project.setId(projectId);

        Task task = Task.builder().project(project).status(TaskStatus.TO_DO).build();
        task.setId(taskId);

        ChangeTaskStatusRequestDto request = new ChangeTaskStatusRequestDto(TaskStatus.DONE);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        TaskResponseDto response = taskService.updateTaskStatus(projectId, taskId, request);

        assertThat(response.status()).isEqualTo(TaskStatus.DONE);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
    }
}