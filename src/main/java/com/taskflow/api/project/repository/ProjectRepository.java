package com.taskflow.api.project.repository;

import com.taskflow.api.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    boolean existsByCode(String code);

}
