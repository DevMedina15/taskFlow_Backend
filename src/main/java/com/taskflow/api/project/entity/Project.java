package com.taskflow.api.project.entity;

import com.taskflow.api.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project extends BaseEntity {

    @Column(nullable = false, length = 20, unique = true)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "organization_name", length = 50 ,nullable = false)
    private String organizationName;

    @Column(name = "project_key", length = 5)
    private String projectKey;

    @Column(name = "current_task_sequence", nullable = false)
    @Builder.Default
    private Integer currentSequenceTask = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private ProjectStatus status = ProjectStatus.ACTIVE;


}
