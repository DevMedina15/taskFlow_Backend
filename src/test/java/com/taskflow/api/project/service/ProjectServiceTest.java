package com.taskflow.api.project.service;

import com.taskflow.api.project.dto.CreateProjectRequestDto;
import com.taskflow.api.project.dto.ProjectMemberResponseDto;
import com.taskflow.api.project.dto.ProjectResponseDto;
import com.taskflow.api.project.dto.UpdateProjectRequestDto;
import com.taskflow.api.project.entity.Project;
import com.taskflow.api.project.entity.ProjectMember;
import com.taskflow.api.project.entity.ProjectRole;
import com.taskflow.api.project.entity.ProjectStatus;
import com.taskflow.api.project.repository.ProjectMemberRepository;
import com.taskflow.api.project.repository.ProjectRepository;
import com.taskflow.api.shared.exception.BusinessRuleException;
import com.taskflow.api.shared.exception.ResourceNotFoundException;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectService projectService;


    // ==========================================
    // TESTS PARA: createProject
    // ==========================================

    @Test
    @DisplayName("Debe crear un proyecto con éxito")
    void createProject_Success() {
        CreateProjectRequestDto request = new CreateProjectRequestDto("App", "Desc", "Org", "KEY");
        String email = "test@gmail.com";

        User user = User.builder()
                .email(email)
                .build();


        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(projectRepository.existsByCode(anyString())).thenReturn(false);

        ProjectResponseDto response = projectService.createProject(request, email);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("App");
        verify(projectRepository).save(any(Project.class));
        verify(projectMemberRepository).save(any(ProjectMember.class));
    }

    @Test
    @DisplayName("Debe regenerar el código si ya existe en otro proyecto")
    void createProject_CodeCollision() {
        CreateProjectRequestDto request = new CreateProjectRequestDto("App", "Desc", "Org", "KEY");
        String email = "test@gmail.com";

        User user = User.builder().email(email).build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        // Devuelve true la primera vez (colisión), y false la segunda
        when(projectRepository.existsByCode(anyString())).thenReturn(true, true);

        projectService.createProject(request, email);

        verify(projectRepository, times(2)).existsByCode(anyString());
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    @DisplayName("Fallar al crear proyecto si el usuario no existe")
    void createProject_UserNotFound() {

        CreateProjectRequestDto request = new CreateProjectRequestDto("App", "Desc", "Org", "KEY");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        Throwable error = catchThrowable(() -> projectService.createProject(request, "test@gmail.com"));

        assertThat(error).isInstanceOf(ResourceNotFoundException.class).hasMessage("Usuario no encontrado");
        verify(projectRepository, never()).save(any());
    }

    // ==========================================
    // TESTS PARA: getUserProjects
    // ==========================================

    @Test
    @DisplayName("Debe retornar proyectos paginados del usuario")
    void getUserProjects_Success() {
        String email = "test@gmail.com";

        User user = User.builder().email(email).build();
        Pageable pageable = PageRequest.of(0, 10);
        Project project = Project.builder()
                .code("PROJ-123").name("Test").status(ProjectStatus.ACTIVE).build();

        Page<Project> page = new PageImpl<>(List.of(project));

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        // Se prueba mandando la búsqueda nula
        when(projectRepository.findUserProjectsWithFilters(user.getId(), null, null, null, pageable))
                .thenReturn(page);

        Page<ProjectResponseDto> result = projectService.getUserProjects(null, null, null, pageable, email);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Test");
    }

    @Test
    @DisplayName("Debe aplicar formato a la variable search si no es nula")
    void getUserProjects_WithSearchParam() {
        String email = "test@gmail.com";
        User user = User.builder().email(email).build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Project> page = new PageImpl<>(List.of());

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(projectRepository.findUserProjectsWithFilters(user.getId(), null, null, "%buscar%", pageable))
                .thenReturn(page);

        projectService.getUserProjects(null, null, "BUSCAR", pageable, email);

        verify(projectRepository).findUserProjectsWithFilters(user.getId(), null, null, "%buscar%", pageable);
    }

    // ==========================================
    // TESTS PARA: updateProject
    // ==========================================

    @Test
    @DisplayName("Debe actualizar un proyecto existente")
    void updateProject_Success() {
        UUID projectId = UUID.randomUUID();
        UpdateProjectRequestDto request = new UpdateProjectRequestDto("Nuevo Nombre", "Nueva Desc", "Nueva Org", ProjectStatus.ARCHIVED);

        Project project = Project.builder().status(ProjectStatus.ACTIVE).build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenReturn(project);

        ProjectResponseDto result = projectService.updateProject(projectId, request);

        assertThat(project.getName()).isEqualTo("Nuevo Nombre");
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ARCHIVED);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Fallar al actualizar si el proyecto no existe")
    void updateProject_NotFound() {
        UpdateProjectRequestDto request = new UpdateProjectRequestDto("N", "D", "O", null);
        when(projectRepository.findById(any())).thenReturn(Optional.empty());

        Throwable error = catchThrowable(() -> projectService.updateProject(UUID.randomUUID(), request));

        assertThat(error).isInstanceOf(ResourceNotFoundException.class).hasMessage("Proyecto no encontrado");
    }

    // ==========================================
    // TESTS PARA: deleteProject
    // ==========================================

    @Test
    @DisplayName("Debe eliminar proyecto si el usuario es OWNER")
    void deleteProject_Success() {
        UUID projectId = UUID.randomUUID();
        String email = "test@gmail.com";
        User user = User.builder().email(email).build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(projectRepository.findRoleByProjectAndUser(projectId, user.getId())).thenReturn(Optional.of(ProjectRole.OWNER));

        projectService.deleteProject(projectId, email);

        verify(projectRepository).deleteById(projectId);
    }

    @Test
    @DisplayName("Fallar al eliminar si el rol no es OWNER")
    void deleteProject_NotOwner() {
        UUID projectId = UUID.randomUUID();
        String email = "test@gmail.com";
        User user = User.builder().email(email).build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(projectRepository.findRoleByProjectAndUser(projectId, user.getId())).thenReturn(Optional.of(ProjectRole.DEVELOPER));

        Throwable error = catchThrowable(() -> projectService.deleteProject(projectId, email));

        assertThat(error).isInstanceOf(AccessDeniedException.class).hasMessage("Acción denegada, no puedes realizar esta accion");
        verify(projectRepository, never()).deleteById(any());
    }

    // ==========================================
    // TESTS PARA: getProjectMembers
    // ==========================================

    @Test
    @DisplayName("Debe listar miembros de un proyecto")
    void getProjectMembers_Success() {
        UUID projectId = UUID.randomUUID();
        User user = User.builder().email("u@gmail.com").build();
        ProjectMember member = ProjectMember.builder().user(user).role(ProjectRole.DEVELOPER).build();

        when(projectMemberRepository.findAllByProjectId(projectId)).thenReturn(List.of(member));

        List<ProjectMemberResponseDto> result = projectService.getProjectMembers(projectId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).isEqualTo("u@gmail.com");
        assertThat(result.get(0).role()).isEqualTo("DEVELOPER");
    }

    // ==========================================
    // TESTS PARA: removeMemberFromProject
    // ==========================================

    @Test
    @DisplayName("Debe remover a un miembro que no sea OWNER")
    void removeMemberFromProject_Success() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ProjectMember member = ProjectMember.builder().role(ProjectRole.DEVELOPER).build();

        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(member));

        projectService.removeMemberFromProject(projectId, userId);

        verify(projectMemberRepository).delete(member);
    }

    @Test
    @DisplayName("Fallar al remover un miembro si este es OWNER")
    void removeMemberFromProject_IsOwner() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ProjectMember member = ProjectMember.builder().role(ProjectRole.OWNER).build();

        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(member));

        Throwable error = catchThrowable(() -> projectService.removeMemberFromProject(projectId, userId));

        assertThat(error).isInstanceOf(BusinessRuleException.class).hasMessage("No puedes realizar esta accion");
        verify(projectMemberRepository, never()).delete(any());
    }

    // ==========================================
    // TESTS PARA: updateMemberRole
    // ==========================================

    @Test
    @DisplayName("Debe actualizar el rol de un miembro")
    void updateMemberRole_Success() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ProjectMember member = ProjectMember.builder().role(ProjectRole.DEVELOPER).build();

        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(member));

        projectService.updateMemberRole(projectId, userId, ProjectRole.MANAGER);

        assertThat(member.getRole()).isEqualTo(ProjectRole.MANAGER);
        verify(projectMemberRepository).save(member);
    }

    @Test
    @DisplayName("Fallar al actualizar rol si el miembro no existe")
    void updateMemberRole_NotFound() {
        when(projectMemberRepository.findByProjectIdAndUserId(any(), any())).thenReturn(Optional.empty());

        Throwable error = catchThrowable(() -> projectService.updateMemberRole(UUID.randomUUID(), UUID.randomUUID(), ProjectRole.VIEWER));

        assertThat(error).isInstanceOf(ResourceNotFoundException.class).hasMessage("El usuario no pertenece a este proyecto.");
    }

}