package com.taskflow.api.task.repository;

import com.taskflow.api.task.entity.Task;
import com.taskflow.api.task.entity.TaskPriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    @Query("""
        SELECT t FROM Task t WHERE t.project.id = :projectId
        AND (:priority IS NULL OR t.priority = :priority)
        AND (:assignedUserId IS NULL OR EXISTS (
            SELECT 1 FROM TaskMember tm
            WHERE tm.task.id = t.id
            AND tm.user.id = :assignedUserId
        ))
        """)
    Page<Task> findTasksWithFilter(
            UUID projectId,
            TaskPriority priority,
            UUID assignedUserId,
            Pageable pageable
    );
}
