-- 1. Eliminamos la tabla de miembos por tarea

DROP TABLE IF EXISTS task_members;

-- 2. Agregamos las columnas a la tabla task
ALTER TABLE tasks ADD COLUMN assignee_id UUID;
ALTER TABLE tasks ADD COLUMN reviewer_id UUID;

-- 3. Creamos relaciones
ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_assignee FOREIGN KEY (assignee_id) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id) ON DELETE SET NULL;
