CREATE TABLE students
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    first_name VARCHAR(50)  NOT NULL,
    last_name  VARCHAR(50)  NOT NULL,
    age        INT          NOT NULL,
    email      VARCHAR(254) NOT NULL,
    password   VARCHAR(72)  NOT NULL,
    institute  VARCHAR(100) NOT NULL,
    batch      VARCHAR(100) NOT NULL,
    gender     VARCHAR(10)  NOT NULL,
    CONSTRAINT pk_students PRIMARY KEY (id),
    CONSTRAINT uk_students_email UNIQUE (email),
    CONSTRAINT ck_students_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
