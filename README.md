# Student Management System — Backend

A REST API for registering students and signing them in, built with **Spring Boot** and **MySQL**. It serves the [Student Management frontend](https://github.com/ThisukaMW/Student-management).

## Tech stack

- **Java 21**, **Spring Boot 3.5.7**
- Spring Web (REST controllers)
- Spring Data JPA / Hibernate
- MySQL (`mysql-connector-j`)
- Lombok
- Maven

## Architecture

The project follows a layered architecture, so each layer has one job:

```
src/main/java/edu/icet/
├── controller/   StudentController       → HTTP endpoints (REST)
├── service/      StudentService          → business logic (interface)
│                 StudentServiceImpl      → implementation
├── repository/   StudentRepository       → Spring Data JPA repository
├── dao/          StudentEntity           → JPA entity mapped to MySQL
└── dto/          Student                 → object sent to / from the API
```

- The **controller** only handles requests and responses, and delegates to the service.
- The **service** maps between the `Student` DTO and the `StudentEntity`, so the database model is never exposed directly through the API.
- The **repository** extends Spring Data JPA, so no SQL is written by hand.
- CORS is enabled on the controller so the separate HTML/JavaScript frontend can call the API.

## Student model

| Field       | Type   |
|-------------|--------|
| id          | Long (auto-generated) |
| firstName   | String |
| lastName    | String |
| age         | int    |
| email       | String |
| password    | String |
| institute   | String |
| batch       | String |
| gender      | String |

## API endpoints

Base path: `/information`

| Method | Endpoint                              | Description                          |
|--------|---------------------------------------|--------------------------------------|
| POST   | `/information`                        | Register a new student               |
| GET    | `/information`                        | Get all students                     |
| GET    | `/information/{email}/{password}`     | Find a student by email and password (sign in) |

Example request body for `POST /information`:

```json
{
  "firstName": "Nimal",
  "lastName": "Perera",
  "age": 21,
  "email": "nimal@example.com",
  "password": "secret123",
  "institute": "ICET",
  "batch": "2023",
  "gender": "Male"
}
```

## Getting started

### Prerequisites

- JDK 21 or newer
- Maven
- MySQL 8 running locally

### 1. Database

The `Student_info` database is created automatically on first run (`createDatabaseIfNotExist=true`), and `ddl-auto: update` lets Hibernate create the student table.

### 2. Configure the connection

The MySQL password is read from `DB_PASSWORD`, so it is never stored in the repo. Create a `.env` file in the project root (it is git-ignored):

```properties
DB_PASSWORD=your_password
```

A `DB_PASSWORD` environment variable works too and takes precedence over `.env`.

If MySQL is not on `localhost`, add `MYSQL_HOST=your_host` to `.env` as well. The username defaults to `root` in `src/main/resources/application.yml`.

### 3. Run the application

From the command line:

```bash
mvn spring-boot:run
```

Or in IntelliJ IDEA: open the project, right-click `pom.xml` → **Maven → Reload Project**, then run `edu.icet.Main`.

The API starts on `http://localhost:8080`.

### 4. Try it

Use Postman, or open the [frontend](https://github.com/ThisukaMW/Student-management) in a browser.

## Troubleshooting

| Error | Cause and fix |
|-------|---------------|
| `Could not find or load main class edu.icet.Main` | `Main.java` must be in `src/main/java/edu/icet/`. Files in `src/main/resources/` are not compiled. Reload the Maven project after moving it. |
| `Access denied for user 'root'@'localhost' (using password: NO)` | No password was supplied. Create the `.env` file described above in the project root. |
| `Access denied for user 'root'@'localhost' (using password: YES)` | The password in `.env` doesn't match your MySQL root password. |
| `Communications link failure` | MySQL is not running. Start the `MySQL80` service. |
| `Port 8080 was already in use` | Another copy of the app is still running. Stop it first. |

## Related

- Frontend: [Student-management](https://github.com/ThisukaMW/Student-management)
