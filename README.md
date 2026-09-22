# Flashpad API

> **Status: Work in Progress (MVP / Phase 1)**  
> An open-source, real-time online notepad platform inspired by [Dontpad](https://dontpad.com), built with Java 21 and Spring Boot.

---

## About the Project

**Flashpad** is an open-source collaborative notepad platform inspired by [Dontpad](https://dontpad.com), designed to provide fast, anonymous, URL-based scratchpads. Navigate to any path, view the note, and edit it freely without mandatory registration.

### Vision & Philosophy

- **Current & Initial Focus:** The current stage is strictly dedicated to **in-depth study, research, and learning** within the modern Java 21 and Spring Boot ecosystem — mastering clean architecture, concurrency, database migrations, and real-time streaming.
- **Ultimate Goal:** The final ambition is to deploy Flashpad as a **publicly available and fully hosted service**.

> [!NOTE]  
> This repository contains the backend API (`flashpad-api`). The project is actively under development, currently in its **MVP phase**. More features, infrastructure, and a dedicated web frontend will be introduced across subsequent roadmap milestones.

---

## Current MVP Scope (Phase 1)

The current milestone focuses on core note lifecycle and real-time streaming:

- **Slug & Path Normalization**: Dynamic note resolution with URL-friendly paths (lowercase, trimmed, hyphens for spaces).
- **CRUD Operations**: Idempotent creation and updates via standard HTTP methods (`GET`, `PUT`).
- **Real-Time Synchronization (SSE)**: Clients connected to a note's stream receive immediate push notifications whenever content changes, powered by Spring's `SseEmitter` and concurrent in-memory session management.
- **Database Migrations**: Automatic schema versioning with Flyway and PostgreSQL.

### MVP Endpoints

| HTTP Method | Endpoint | Description |
|---|---|---|
| `GET` | `/{path}` | Retrieves the current note content (or returns 404 if not created). |
| `PUT` | `/{path}` | Creates or updates note content (idempotent; broadcasts change via SSE). |
| `GET` | `/{path}/stream` | Subscribes to Server-Sent Events (SSE) for real-time updates on `{path}`. |

---

## Tech Stack

- **Language:** Java 21
- **Framework:** Spring Boot 4.x
- **Web Layer:** Spring WebMvc (`SseEmitter` for streaming)
- **Data Persistence:** Spring Data JPA / Hibernate
- **Database & Migrations:** PostgreSQL & Flyway
- **Validation:** Jakarta Bean Validation (`jakarta.validation`)
- **Testing:** JUnit 5, MockMvc, and Testcontainers *(planned)*
- **Build Tool:** Apache Maven (via Maven Wrapper)

---

## Getting Started

### Prerequisites

- **Java Development Kit (JDK):** Version 21 or higher
- **PostgreSQL:** Local instance or running via Docker

### 1. Configure the Database

Configure your PostgreSQL connection in `src/main/resources/application.properties` (or via an ignored `application-local.properties`):

```properties
spring.application.name=flashpad-api

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/flashpad
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=validate

# Flyway Migrations
spring.flyway.enabled=true
```

### 2. Run Database Migrations & Start the Application

Execute using Maven:

```bash
mvn clean spring-boot:run
```

*(Or use `.\mvnw.cmd` / `./mvnw` if you have the wrapper locally).*

The API will be available at `http://localhost:8080`.

---

## License

This project is and will always remain open-source, distributed under the [MIT License](LICENSE).

