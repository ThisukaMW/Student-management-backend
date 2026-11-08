# Student Management System — Backend

A REST API for registering students and signing them in, built with **Spring Boot** and **MySQL**. It serves the [Student Management frontend](https://github.com/ThisukaMW/Student-management).

## Tech stack

- **Java 18**, **Spring Boot 3.1.2**
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

Set `MYSQL_HOST` too if MySQL is not on `localhost`. The username defaults to `root` in `src/main/resources/application.yml`.

### 3. Run the application

```bash
mvn spring-boot:run
```

The API starts on `http://localhost:8080`.

### 4. Try it

Use Postman, or open the [frontend](https://github.com/ThisukaMW/Student-management) in a browser.


## Related

- Frontend: [Student-management](https://github.com/ThisukaMW/Student-management)
