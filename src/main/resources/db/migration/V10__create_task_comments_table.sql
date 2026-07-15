CREATE TABLE task_comments (
   id UUID PRIMARY KEY,
   task_id UUID NOT NULL,
   author_id UUID NOT NULL,
   content TEXT NOT NULL,
   created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
   updated_at TIMESTAMP WITHOUT TIME ZONE,

   CONSTRAINT fk_comments_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
   CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users(id)
);

CREATE INDEX idx_comments_task_id ON task_comments(task_id);