CREATE TABLE tasks (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
   title VARCHAR(255) NOT NULL,
   description TEXT,
   status VARCHAR(50) NOT NULL DEFAULT 'TO_DO', -- TO_DO, IN_PROGRESS, REVIEW, DONE
   priority VARCHAR(30) NOT NULL DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH, URGENT
   start_date TIMESTAMP,
   end_date TIMESTAMP,
   estimated_time INT, -- Tiempo guardado en dias
   git_branch VARCHAR(150),
   created_at TIMESTAMP NOT NULL,
   updated_at TIMESTAMP NOT NULL
);

CREATE TABLE task_members (
  task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  task_role VARCHAR(50) NOT NULL, -- 'ASSIGNEE', 'REVIEWER'
  PRIMARY KEY (task_id, user_id)
);