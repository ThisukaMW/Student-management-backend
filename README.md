# Student Management System — Backend

A REST API for registering, managing and signing in students, built with **Spring Boot** and **MySQL**. It serves the [Student Management frontend](https://github.com/ThisukaMW/Student-management).

## Tech stack

- **Java 21**, **Spring Boot 3.5.7**
- Spring Web (REST controllers) and Bean Validation
- Spring Data JPA / Hibernate
- MySQL 8 (`mysql-connector-j`), schema managed by **Flyway**
- MapStruct (DTO ↔ entity mapping) and Lombok
- JUnit 5, Mockito and MockMvc for tests
- Maven

## Architecture

The project follows a layered architecture, so each layer has one job:

```
src/main/java/edu/icet/
├── StudentManagementApplication   → entry point
├── controller/   StudentController, AuthController    → HTTP endpoints (REST)
├── service/      StudentService, AuthService          → business logic (interfaces)
│                 StudentServiceImpl, AuthServiceImpl  → implementations
├── repository/   StudentRepository                    → Spring Data JPA repository
│                 StudentSpecifications                → optional search filters
├── entity/       Student, Gender                      → JPA entity mapped to the students table
├── dto/request/  StudentCreateRequest, StudentUpdateRequest, LoginRequest
├── dto/response/ StudentResponse, ApiError
├── mapper/       StudentMapper                        → MapStruct DTO ↔ entity mapping
├── exception/    GlobalExceptionHandler + custom exceptions
└── config/       CorsConfig, CorsProperties
src/main/resources/db/migration/                       → Flyway SQL migrations
```

- **Controllers** only handle HTTP: they validate the request body and return a `ResponseEntity` with the right status.
- **Services** hold the business rules (unique email, not-found checks, login).
- **DTOs** are Java records. `StudentResponse` never contains the password, so the database model is never exposed through the API.
- **Dependencies** are injected through constructors (`@RequiredArgsConstructor` with `final` fields).
- **Errors** are all turned into one JSON shape by `GlobalExceptionHandler` (see [Errors](#errors)).

> **Not yet secure:** passwords are stored and compared in plain text, and there is no authentication on the student endpoints. Spring Security with hashed passwords is planned for Phase 2.

## Student model

| Field     | Type                          | Rules                                    |
|-----------|-------------------------------|------------------------------------------|
| id        | Long (auto-generated)         |                                          |
| firstName | String                        | required, max 50                         |
| lastName  | String                        | required, max 50                         |
| age       | Integer                       | required, 15–100                         |
| email     | String                        | required, valid email, max 254, unique   |
| password  | String (write-only)           | required on create, 8–72 characters      |
| institute | String                        | required, max 100                        |
| batch     | String                        | required, max 100                        |
| gender    | `MALE` \| `FEMALE` \| `OTHER` | required (case-insensitive on input)     |

Emails are trimmed and lowercased before they are saved or compared, so `Nimal@Example.com` and `nimal@example.com` are the same account.

## API endpoints

| Method | Endpoint                    | Description                                   | Success | Errors        |
|--------|-----------------------------|-----------------------------------------------|---------|---------------|
| POST   | `/api/v1/students`          | Register a student                            | 201     | 400, 409      |
| GET    | `/api/v1/students`          | List students (paginated, optional filters)   | 200     | 400           |
| GET    | `/api/v1/students/{id}`     | Get one student                               | 200     | 400, 404      |
| PUT    | `/api/v1/students/{id}`     | Replace a student's details (not password)    | 200     | 400, 404, 409 |
| DELETE | `/api/v1/students/{id}`     | Delete a student                              | 204     | 404           |
| POST   | `/api/v1/auth/login`        | Sign in with email and password               | 200     | 400, 401      |

### Register — `POST /api/v1/students`

```json
{
  "firstName": "Nimal",
  "lastName": "Perera",
  "age": 21,
  "email": "nimal@example.com",
  "password": "secret123",
  "institute": "ICET",
  "batch": "2023",
  "gender": "MALE"
}
```

Response `201 Created`, with a `Location: http://localhost:8080/api/v1/students/1` header:

```json
{
  "id": 1,
  "firstName": "Nimal",
  "lastName": "Perera",
  "age": 21,
  "email": "nimal@example.com",
  "institute": "ICET",
  "batch": "2023",
  "gender": "MALE"
}
```

### List — `GET /api/v1/students`

| Query parameter | Meaning                                                           | Default   |
|-----------------|-------------------------------------------------------------------|-----------|
| `name`          | First **or** last name contains this text (case-insensitive)      | —         |
| `batch`         | Exact batch (case-insensitive)                                    | —         |
| `institute`     | Exact institute (case-insensitive)                                | —         |
| `page`          | Page number, starting at 0                                        | `0`       |
| `size`          | Page size (max 100)                                               | `20`      |
| `sort`          | `field,asc` or `field,desc`, e.g. `lastName,asc`                  | `id,asc`  |

Example: `GET /api/v1/students?name=per&batch=2023&page=0&size=10&sort=lastName,asc`

```json
{
  "content": [
    { "id": 1, "firstName": "Nimal", "lastName": "Perera", "age": 21, "email": "nimal@example.com",
      "institute": "ICET", "batch": "2023", "gender": "MALE" }
  ],
  "page": { "size": 10, "number": 0, "totalElements": 1, "totalPages": 1 }
}
```

### Update — `PUT /api/v1/students/{id}`

Send every field except `password` (password changes come in Phase 2). Changing the email to one that another student uses returns `409`.

```json
{
  "firstName": "Nimal",
  "lastName": "Silva",
  "age": 22,
  "email": "nimal@example.com",
  "institute": "ICET",
  "batch": "2024",
  "gender": "MALE"
}
```

### Sign in — `POST /api/v1/auth/login`

```json
{ "email": "nimal@example.com", "password": "secret123" }
```

Returns `200` with the student (same shape as above) or `401` with `"Invalid email or password"`. The message is the same for an unknown email and a wrong password.

## Errors

Every error uses the same JSON shape:

```json
{
  "timestamp": "2026-11-08T10:09:02.787Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/students",
  "fieldErrors": [
    { "field": "age", "message": "must be greater than or equal to 15" },
    { "field": "email", "message": "must be a well-formed email address" }
  ]
}
```

`fieldErrors` is filled for validation failures and empty otherwise. Rejected values are never echoed back.

| Status | When                                                                        |
|--------|-----------------------------------------------------------------------------|
| 400    | Validation failed, malformed JSON, invalid `gender`, non-numeric id, unknown `sort` field |
| 401    | Wrong email or password on login                                            |
| 404    | Student not found, or unknown URL                                           |
| 405    | Wrong HTTP method for the URL                                               |
| 409    | Email already used by another student                                       |
| 500    | Unexpected error (details are logged, not returned)                         |

## Getting started

### Prerequisites

- JDK 21 or newer
- Maven
- MySQL 8 running locally

### 1. Configure the connection

The MySQL password is read from `DB_PASSWORD`, so it is never stored in the repo. Create a `.env` file in the project root (it is git-ignored):

```properties
DB_PASSWORD=your_password
```

A `DB_PASSWORD` environment variable works too and takes precedence over `.env`. If MySQL is not on `localhost`, add `MYSQL_HOST=your_host` to `.env` as well. The username defaults to `root` in `src/main/resources/application.yml`.

### 2. Database

The `Student_info` database is created automatically on first run (`createDatabaseIfNotExist=true`). **Flyway** then creates the `students` table from `src/main/resources/db/migration/V1__create_students_table.sql`, and Hibernate only *validates* that the table matches the entity (`ddl-auto: validate`).

To change the schema, add a new migration such as `V2__add_phone_number.sql`. Never edit a migration that has already run.

> **Upgrading from the old version?** The old app created `student_entity` and `student_entity_seq` tables. Flyway refuses to start on a database that has tables but no Flyway history, so drop them once:
>
> ```sql
> DROP TABLE Student_info.student_entity, Student_info.student_entity_seq;
> ```

### 3. CORS

Browsers may call the API from the origins in `app.cors.allowed-origins` (default `http://localhost:5173`, `http://localhost:3000`, and VS Code Live Server on `http://127.0.0.1:5500` and `http://localhost:5500`). To allow another origin, change it in `application.yml` or set an environment variable:

```properties
APP_CORS_ALLOWED_ORIGINS=http://localhost:8000,http://127.0.0.1:5500
```

### 4. Run the application

From the command line:

```bash
mvn spring-boot:run
```

Or in IntelliJ IDEA: open the project, right-click `pom.xml` → **Maven → Reload Project**, then run `edu.icet.StudentManagementApplication`.

The API starts on `http://localhost:8080`.

### 5. Run the tests

```bash
mvn verify
```

The tests need no database: service tests use Mockito, and controller tests use `@WebMvcTest`.

## Troubleshooting

| Error | Cause and fix |
|-------|---------------|
| `Could not find or load main class edu.icet.Main` | The main class is now `edu.icet.StudentManagementApplication`. Delete the old IntelliJ run configuration and run the new class. |
| `Found non-empty schema(s) ... but no schema history table` | Old tables from before Flyway are still there. Drop them as described in [Database](#2-database). |
| `Schema-validation: missing table [students]` or `wrong column type` | The table doesn't match the entity. Add a Flyway migration instead of changing the table by hand. |
| `Access denied for user 'root'@'localhost' (using password: NO)` | No password was supplied. Create the `.env` file described above in the project root. |
| `Access denied for user 'root'@'localhost' (using password: YES)` | The password in `.env` doesn't match your MySQL root password. |
| `Communications link failure` | MySQL is not running. Start the `MySQL80` service. |
| `Port 8080 was already in use` | Another copy of the app is still running. Stop it first. |
| Browser shows a CORS error | Add the frontend's origin to `app.cors.allowed-origins`. Opening the HTML files straight from disk (`file://`) can't work; serve them with Live Server instead. |

## Related

- Frontend: [Student-management](https://github.com/ThisukaMW/Student-management)
