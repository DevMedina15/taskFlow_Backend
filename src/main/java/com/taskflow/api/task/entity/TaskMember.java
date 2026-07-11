package com.taskflow.api.task.entity;

import com.taskflow.api.shared.entity.BaseEntity;
import com.taskflow.api.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "task_members", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"task_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_role", nullable = false, length = 50)
    private TaskRole role;
}
