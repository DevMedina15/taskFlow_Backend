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
        AND (:assignedUserId IS NULL OR t.assignee.id = :assignedUserId)
        AND (:reviewerId IS NULL OR t.reviewer.id = :reviewerId)
        """)
    Page<Task> findTasksWithFilter(
            UUID projectId,
            TaskPriority priority,
            UUID assignedUserId,
            UUID reviewerId,
            Pageable pageable
    );
}
