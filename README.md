# Student Management System — Backend

A REST API for registering, managing and signing in students, built with **Spring Boot** and **MySQL**. Accounts sign in with JWT access tokens, and what each account can do depends on its role (admin, lecturer or student). It serves the [Student Management frontend](https://github.com/ThisukaMW/Student-management).

## Tech stack

- **Java 21**, **Spring Boot 3.5.7**
- Spring Web (REST controllers) and Bean Validation
- **Spring Security** with the OAuth2 resource server (JWT, HS256) and BCrypt password hashing
- Spring Data JPA / Hibernate
- MySQL 8 (`mysql-connector-j`), schema managed by **Flyway**
- MapStruct (DTO ↔ entity mapping) and Lombok
- JUnit 5, Mockito, MockMvc and Spring Security Test
- Maven

## Architecture

The project follows a layered architecture, so each layer has one job:

```
src/main/java/edu/icet/
├── StudentManagementApplication   → entry point
├── controller/   AuthController, StudentController, AdminUserController  → HTTP endpoints
├── service/      AuthService, StudentService, UserService, TokenService,
│                 AdminUserService (+ Impl)            → business logic
├── repository/   StudentRepository, UserRepository, RefreshTokenRepository
│                 StudentSpecifications                → optional search filters
├── entity/       User, Role, Student, Gender, RefreshToken
├── dto/request/  StudentCreateRequest, StudentUpdateRequest, LoginRequest, RefreshTokenRequest,
│                 ChangePasswordRequest, CreateUserRequest
├── dto/response/ StudentResponse, AuthResponse, UserSummary, MeResponse, UserResponse, ApiError
├── mapper/       StudentMapper                        → MapStruct DTO ↔ entity mapping
├── security/     SecurityConfig, JwtConfig, JwtProperties, StudentSecurity,
│                 RestAuthenticationEntryPoint, RestAccessDeniedHandler
├── validation/   @ValidPassword                       → password rules
├── exception/    GlobalExceptionHandler + custom exceptions
└── config/       CorsConfig, AdminBootstrap, PasswordConfig, ClockConfig
src/main/resources/db/migration/                       → Flyway SQL migrations
```

- **Accounts and profiles are separate.** A `User` is a login account: email, BCrypt password hash, role, enabled flag. A `Student` is a profile linked one-to-one to a `STUDENT` account. Admins and lecturers have accounts but no profile.
- **Controllers** only handle HTTP. **Services** hold the business rules. **Authorization** is declared with `@PreAuthorize` on the controllers; the "is this my own profile?" check lives in `StudentSecurity`.
- **`UserService` is the only place accounts are created.** Email normalization, the unique-email rule and password hashing therefore can't be bypassed.
- **Responses never expose a password or password hash.** DTOs are Java records, and dependencies are injected through constructors.
- **Errors**, including 401 and 403, all use one JSON shape (see [Errors](#errors)).

## Authentication

The API is stateless: there are no sessions or cookies. Every protected request carries a short-lived **access token** (a signed JWT) in the `Authorization` header. When it expires, a long-lived **refresh token** gets a new pair.

```
register ──► login ──► call APIs with "Authorization: Bearer <accessToken>"
                │                     │ access token expires (15 min)
                │                     ▼
                │      refresh (send refreshToken) ──► new accessToken + new refreshToken
                ▼
             logout (send refreshToken) ──► that refresh token stops working
```

1. **Register:** `POST /api/v1/auth/register` creates a `STUDENT` account and its profile. It's public.
2. **Log in:** `POST /api/v1/auth/login` returns:
   ```json
   {
     "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
     "refreshToken": "q3V8l5c1G4Zr7oTqk2bXo9JmZ1nE0uWw3yVt6sRpQaA",
     "tokenType": "Bearer",
     "expiresIn": 900,
     "user": { "id": 2, "email": "nimal@example.com", "role": "STUDENT" }
   }
   ```
3. **Call the API:** send `Authorization: Bearer <accessToken>`. The token is valid for 15 minutes (`expiresIn` is in seconds). It contains the user id (`sub`), `email` and `role`. Spring's token validator allows **60 seconds of clock skew**, so in practice a token is accepted for up to about a minute after `exp`.
4. **Refresh:** when the access token expires (the API returns 401), send the refresh token to `POST /api/v1/auth/refresh`. You get a **new pair**, and the old refresh token stops working (rotation). Refresh tokens last 7 days.
5. **Log out:** `POST /api/v1/auth/logout` revokes the given refresh token. The access token keeps working until it expires, so the client should also discard it.

**How refresh tokens are protected:**
- They are random 256-bit values, and the database stores only their SHA-256 hash.
- Each one can be used once. If a refresh token that has already been used or revoked is presented again, the server assumes it was stolen and revokes **all** of that user's refresh tokens, so every device has to log in again.
- Changing your password also ends all sessions.
- Disabled accounts can't log in or refresh. An access token issued before the account was disabled still works until it expires (15 minutes at most, plus the clock skew).

**Writing a client:** because reuse revokes everything, a client must never send the same refresh token twice. Run one refresh at a time, including across browser tabs that share storage, and always send the newest refresh token you have. After any refresh, logout must send the *new* refresh token, not the old one. The [React frontend](https://github.com/ThisukaMW/Student-management#how-sign-in-works) does this: it keeps the access token in memory, the refresh token in `localStorage`, and serializes refreshes with the Web Locks API.

## Roles and permissions

| Endpoint                                 | Public | STUDENT                 | LECTURER | ADMIN |
|------------------------------------------|:------:|:-----------------------:|:--------:|:-----:|
| `POST /api/v1/auth/register`             | ✅     |                         |          |       |
| `POST /api/v1/auth/login`, `/refresh`    | ✅     |                         |          |       |
| `POST /api/v1/auth/logout`               |        | ✅                      | ✅       | ✅    |
| `GET /api/v1/auth/me`                    |        | ✅                      | ✅       | ✅    |
| `PUT /api/v1/auth/me/password`           |        | ✅                      | ✅       | ✅    |
| `GET /api/v1/students` (list/search)     |        | ❌                      | ✅       | ✅    |
| `GET /api/v1/students/{id}`              |        | own profile only        | ✅       | ✅    |
| `POST /api/v1/students`                  |        | ❌                      | ❌       | ✅    |
| `PUT /api/v1/students/{id}`              |        | own profile, but not `role` / `enabled` | ❌ | ✅ |
| `DELETE /api/v1/students/{id}`           |        | ❌                      | ❌       | ✅    |
| `POST /api/v1/admin/users`               |        | ❌                      | ❌       | ✅    |

- No token, or an invalid or expired one, returns **401**. A valid token without permission returns **403**.
- A student who asks for any profile that isn't theirs gets 403, **even if that id doesn't exist**, so ids can't be probed.
- `DELETE /api/v1/students/{id}` deletes the profile **and its login account**, and the account's refresh tokens go with it. A student account without a profile has no use, and deleting it frees the email to be registered again.
- **Lecturers and other admins** are created with `POST /api/v1/admin/users`. The first admin comes from `ADMIN_EMAIL` / `ADMIN_PASSWORD` (see [Getting started](#getting-started)).

## Data model

**Student profile** (`students` table, returned as `StudentResponse`)

| Field     | Type                          | Rules                                         |
|-----------|-------------------------------|-----------------------------------------------|
| id        | Long (auto-generated)         |                                               |
| firstName | String                        | required, max 50                              |
| lastName  | String                        | required, max 50                              |
| age       | Integer                       | required, 15–100                              |
| email     | String (from the account)     | required, valid email, max 254, unique        |
| password  | String (write-only)           | required on create, see [password rules](#password-rules) |
| institute | String                        | required, max 100                             |
| batch     | String                        | required, max 100                             |
| gender    | `MALE` \| `FEMALE` \| `OTHER` | required (case-insensitive on input)          |

**Account** (`users` table): `id`, `email` (unique), `passwordHash` (BCrypt), `role` (`ADMIN` | `LECTURER` | `STUDENT`), `enabled`, `createdAt`, `updatedAt`.

Emails are trimmed and lowercased before they are saved or compared, so `Nimal@Example.com` and `nimal@example.com` are the same account.

### Password rules

- At least **8 characters**
- At least **one letter** and **one digit**
- At most **72 bytes** (BCrypt's limit). That's 72 plain ASCII characters, or fewer if the password uses accented or other multi-byte characters.

Each broken rule is reported as its own field error, for example `"must contain at least one digit"`.

## API endpoints

### Auth — `/api/v1/auth`

| Method | Endpoint        | Body                                   | Success                         | Errors        |
|--------|-----------------|----------------------------------------|---------------------------------|---------------|
| POST   | `/register`     | student profile + `password`           | 201 + profile + `Location`      | 400, 409      |
| POST   | `/login`        | `email`, `password`                    | 200 + tokens                    | 400, 401      |
| POST   | `/refresh`      | `refreshToken`                         | 200 + new tokens                | 400, 401      |
| POST   | `/logout`       | `refreshToken`                         | 204                             | 401           |
| GET    | `/me`           | —                                      | 200 + account and profile       | 401           |
| PUT    | `/me/password`  | `currentPassword`, `newPassword`       | 204                             | 400, 401      |

- **Register** takes the same body as `POST /api/v1/students` (shown below) and creates a `STUDENT` account.
- **Login** returns the same 401 message, `"Invalid email or password"`, for an unknown email, a wrong password and a disabled account.
- **`/me`** returns `{ id, email, role, enabled, createdAt, student }`. `student` is the profile, or `null` for admins and lecturers.
- **`/me/password`:** a wrong `currentPassword` is a **400** field error, not 401, so the client doesn't mistake it for an expired token. Reusing the current password is also rejected. On success, all refresh tokens are revoked.

### Students — `/api/v1/students`

| Method | Endpoint      | Description                                     | Success | Errors             |
|--------|---------------|-------------------------------------------------|---------|--------------------|
| POST   | `/`           | Create a student account and profile (admin)    | 201     | 400, 401, 403, 409 |
| GET    | `/`           | List students (paginated, optional filters)     | 200     | 400, 401, 403      |
| GET    | `/{id}`       | Get one student                                 | 200     | 400, 401, 403, 404 |
| PUT    | `/{id}`       | Replace a student's details (not password)      | 200     | 400, 401, 403, 404, 409 |
| DELETE | `/{id}`       | Delete a student and their account (admin)      | 204     | 401, 403, 404      |

**Create** body (the admin sets the student's initial password):

```json
{
  "firstName": "Nimal",
  "lastName": "Perera",
  "age": 21,
  "email": "nimal@example.com",
  "password": "Secret123",
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

**List** query parameters:

| Query parameter | Meaning                                                           | Default   |
|-----------------|-------------------------------------------------------------------|-----------|
| `name`          | First **or** last name contains this text (case-insensitive)      | —         |
| `batch`         | Exact batch (case-insensitive)                                    | —         |
| `institute`     | Exact institute (case-insensitive)                                | —         |
| `page`          | Page number, starting at 0                                        | `0`       |
| `size`          | Page size (max 100)                                               | `20`      |
| `sort`          | `field,asc` or `field,desc`, e.g. `lastName,asc` (profile fields; not `email`) | `id,asc` |

```json
{
  "content": [
    { "id": 1, "firstName": "Nimal", "lastName": "Perera", "age": 21, "email": "nimal@example.com",
      "institute": "ICET", "batch": "2023", "gender": "MALE" }
  ],
  "page": { "size": 20, "number": 0, "totalElements": 1, "totalPages": 1 }
}
```

**Update:** send every profile field except `password`. Changing `email` changes the login email too, and an email another account uses returns `409`. Admins may also send `"role"` and `"enabled"`. Both are optional, and leaving one out keeps its current value. Disabling an account revokes its refresh tokens. A student who sends either field gets `403`.

```json
{
  "firstName": "Nimal",
  "lastName": "Silva",
  "age": 22,
  "email": "nimal@example.com",
  "institute": "ICET",
  "batch": "2024",
  "gender": "MALE",
  "enabled": false
}
```

### Admin — `/api/v1/admin`

| Method | Endpoint  | Body                                         | Success          | Errors             |
|--------|-----------|----------------------------------------------|------------------|--------------------|
| POST   | `/users`  | `email`, `password`, `role` (`LECTURER` or `ADMIN`) | 201 + account | 400, 401, 403, 409 |

`role: "STUDENT"` is rejected with 400, because students need a profile; create them with `POST /api/v1/students`.

## Errors

Every error, including 401 and 403, uses the same JSON shape:

```json
{
  "timestamp": "2026-11-08T10:09:02.787Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/auth/register",
  "fieldErrors": [
    { "field": "age", "message": "must be greater than or equal to 15" },
    { "field": "password", "message": "must contain at least one digit" }
  ]
}
```

`fieldErrors` is filled for validation failures and empty otherwise. Rejected values are never echoed back.

| Status | When                                                                                          |
|--------|-----------------------------------------------------------------------------------------------|
| 400    | Validation failed, malformed JSON, invalid enum value, non-numeric id, unknown `sort` field, wrong current password |
| 401    | Missing, invalid or expired access token; wrong login; invalid, expired or revoked refresh token |
| 403    | Authenticated, but the role or ownership rules don't allow it                                |
| 404    | Student not found (for staff), or unknown URL                                                 |
| 405    | Wrong HTTP method for the URL                                                                 |
| 409    | Email already used by another account                                                        |
| 500    | Unexpected error (details are logged, not returned)                                           |

401 responses also carry the standard `WWW-Authenticate: Bearer` header.

## Getting started

### Prerequisites

- JDK 21 or newer
- Maven
- MySQL 8 running locally

### 1. Environment variables

Secrets are never stored in the repo. Put them in a `.env` file in the project root (git-ignored) or set them as environment variables; environment variables take precedence.

| Variable         | Required | Purpose                                                                 |
|------------------|----------|-------------------------------------------------------------------------|
| `DB_PASSWORD`    | yes      | MySQL password for the `root` user                                      |
| `JWT_SECRET`     | yes      | Key that signs access tokens; at least 32 bytes. **The app won't start without it** |
| `ADMIN_EMAIL`    | first run | Email of the first admin, created on startup if no admin exists        |
| `ADMIN_PASSWORD` | first run | That admin's password (must meet the [password rules](#password-rules)) |
| `MYSQL_HOST`     | no       | MySQL host, default `localhost`                                         |

Example `.env` (placeholders, replace every value):

```properties
DB_PASSWORD=your_mysql_password
JWT_SECRET=replace-with-a-long-random-value-at-least-32-bytes
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=ChangeMe123
```

Generate a strong `JWT_SECRET` with:

```bash
openssl rand -base64 48
```

**Notes:**
- **Changing `JWT_SECRET`** invalidates every access token that has already been issued.
- **The admin variables are only used while no admin exists.** If they are missing or invalid, the app logs a warning and starts anyway, without creating an admin. The password is never logged. After the first admin exists, you can remove both lines.

Token lifetimes are set in `application.yml` under `app.security.jwt` (`access-token-ttl: 15m`, `refresh-token-ttl: 7d`).

### 2. Database

The `Student_info` database is created automatically on first run (`createDatabaseIfNotExist=true`). **Flyway** creates and updates the tables from `src/main/resources/db/migration/`, and Hibernate only *validates* that they match the entities (`ddl-auto: validate`).

| Migration | What it does |
|-----------|--------------|
| `V1__create_students_table.sql` | `students` table |
| `V2__create_users_and_link_students.sql` | `users` table; links each student to an account. **Deletes existing student rows**: before Phase 2 they had plain-text passwords and no account to link to |
| `V3__create_refresh_tokens_table.sql` | `refresh_tokens` table (hashed tokens) |

To change the schema, add a new migration such as `V4__add_phone_number.sql`. Never edit a migration that has already run.

### 3. CORS

Browsers may call the API from the origins in `app.cors.allowed-origins`. The defaults are `http://localhost:5173` (the [React frontend](https://github.com/ThisukaMW/Student-management)'s Vite dev server) and `http://localhost:3000`, plus VS Code Live Server on `http://127.0.0.1:5500` and `http://localhost:5500`, kept from the old HTML frontend. Origins must match exactly: `http://127.0.0.1:5173` is a different origin from `http://localhost:5173`. The `Authorization` header is allowed. To allow another origin, change it in `application.yml` or set an environment variable:

```properties
APP_CORS_ALLOWED_ORIGINS=http://localhost:8000,http://localhost:5173
```

### 4. Run the application

From the command line:

```bash
mvn spring-boot:run
```

Or in IntelliJ IDEA: open the project, right-click `pom.xml` → **Maven → Reload Project**, then run `edu.icet.StudentManagementApplication`.

The API starts on `http://localhost:8080`. On the first start you should see `Created the first ADMIN account: admin@example.com` in the log.

### 5. Try it with curl

These use bash (on Windows, run them in Git Bash). Replace the tokens with the values from the previous responses.

```bash
API=http://localhost:8080/api/v1

# Register a student (public)
curl -i -X POST $API/auth/register -H "Content-Type: application/json" \
  -d '{"firstName":"Nimal","lastName":"Perera","age":21,"email":"nimal@example.com","password":"Secret123","institute":"ICET","batch":"2023","gender":"MALE"}'

# Log in: copy accessToken and refreshToken from the response
curl -i -X POST $API/auth/login -H "Content-Type: application/json" \
  -d '{"email":"nimal@example.com","password":"Secret123"}'

ACCESS=paste-access-token-here
REFRESH=paste-refresh-token-here

# Who am I?
curl -i $API/auth/me -H "Authorization: Bearer $ACCESS"

# Protected endpoint: your own profile (use the student id from /me)
curl -i $API/students/1 -H "Authorization: Bearer $ACCESS"

# Get a new token pair (the old refresh token stops working)
curl -i -X POST $API/auth/refresh -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH\"}"

# Change password (then log in again)
curl -i -X PUT $API/auth/me/password -H "Authorization: Bearer $ACCESS" -H "Content-Type: application/json" \
  -d '{"currentPassword":"Secret123","newPassword":"NewSecret456"}'

# Log out (revokes the refresh token)
curl -i -X POST $API/auth/logout -H "Authorization: Bearer $ACCESS" -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH\"}"

# As admin: list students, and create a lecturer
curl -i -X POST $API/auth/login -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"ChangeMe123"}'
ADMIN=paste-admin-access-token-here
curl -i "$API/students?page=0&size=10" -H "Authorization: Bearer $ADMIN"
curl -i -X POST $API/admin/users -H "Authorization: Bearer $ADMIN" -H "Content-Type: application/json" \
  -d '{"email":"lecturer@example.com","password":"Teach1234","role":"LECTURER"}'
```

### 6. Run the tests

```bash
mvn verify
```

The tests need no database:
- **Service tests** use Mockito with a real BCrypt encoder and real JWT signing.
- **Controller tests** use `@WebMvcTest` with the real security configuration, sending tokens for each role, and check 401/403/400 behaviour.

## Troubleshooting

| Error | Cause and fix |
|-------|---------------|
| `JWT_SECRET is not set` / `JWT_SECRET is too short` | Add `JWT_SECRET` (at least 32 bytes) to `.env`. See [Environment variables](#1-environment-variables). |
| `No ADMIN account exists and ADMIN_EMAIL / ADMIN_PASSWORD are not set` | A warning, not an error. Add both to `.env` and restart to create the first admin. |
| `401` with `The access token is invalid or has expired` | The access token expired (15 min) or `JWT_SECRET` changed. Call `/auth/refresh`, or log in again. |
| `401` from `/auth/refresh` | The refresh token expired, was already used, was revoked by logout or a password change, or the account is disabled. Log in again. |
| `403 You do not have permission to perform this action` | Your role isn't allowed to do that. See [Roles and permissions](#roles-and-permissions). |
| `Could not find or load main class edu.icet.Main` | The main class is `edu.icet.StudentManagementApplication`. Delete the old IntelliJ run configuration and run the new class. |
| `Found non-empty schema(s) ... but no schema history table` | Old tables from before Flyway are still there: `DROP TABLE Student_info.student_entity, Student_info.student_entity_seq;` |
| `Schema-validation: missing table` or `wrong column type` | The tables don't match the entities. Add a Flyway migration instead of changing tables by hand. |
| `Access denied for user 'root'@'localhost' (using password: NO)` | No password was supplied. Add `DB_PASSWORD` to `.env` in the project root. |
| `Access denied for user 'root'@'localhost' (using password: YES)` | The password in `.env` doesn't match your MySQL root password. |
| `Communications link failure` | MySQL is not running. Start the `MySQL80` service. |
| `Port 8080 was already in use` | Another copy of the app is still running. Stop it first. |
| Browser shows a CORS error | Add the frontend's origin to `app.cors.allowed-origins`. |

## Related

- Frontend: [Student-management](https://github.com/ThisukaMW/Student-management), a React + Vite + MUI app that uses this API's JWT authentication. Run this backend on port 8080, then `npm run dev` in the frontend and open `http://localhost:5173`.
