CREATE TABLE users
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'LECTURER', 'STUDENT'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Existing students have plain-text passwords (which can't be hashed in SQL) and no
-- account to link to, so the disposable test data from before Phase 2 is cleared.
DELETE FROM students;

-- Email and password move to users; each student profile now belongs to exactly one account.
-- Deleting an account deletes its profile.
ALTER TABLE students
    DROP INDEX uk_students_email,
    DROP COLUMN email,
    DROP COLUMN password,
    ADD COLUMN user_id BIGINT NOT NULL AFTER id,
    ADD CONSTRAINT uk_students_user UNIQUE (user_id),
    ADD CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
