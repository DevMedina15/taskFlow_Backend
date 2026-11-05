CREATE TABLE project_invitations (
     id UUID PRIMARY KEY,
     project_id UUID NOT NULL,
     guest_email VARCHAR(255) NOT NULL,
     role VARCHAR(50) NOT NULL,
     token VARCHAR(255) UNIQUE NOT NULL,
     status VARCHAR(50) NOT NULL,
     expiration_date TIMESTAMP NOT NULL,
     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

     CONSTRAINT fk_project_invitations_project
         FOREIGN KEY (project_id)
             REFERENCES projects (id)
             ON DELETE CASCADE
);

CREATE INDEX idx_project_invitations_token ON project_invitations(token);