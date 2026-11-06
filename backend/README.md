# CodePilot Backend

The backend REST API for CodePilot, an AI-powered developer workspace.

## Technology Stack

- Java 21
- Spring Boot
- Spring Security and JWT Authentication
- Spring Data JPA / Hibernate
- PostgreSQL
- Maven
- Springdoc OpenAPI / Swagger
- JUnit 5, Mockito, MockMvc
- Testcontainers

## Prerequisites

- JDK 21
- PostgreSQL
- Docker Desktop (required for Testcontainers integration tests)

## Configuration

The application uses environment variables for database credentials and JWT configuration.

| Variable | Purpose |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Active profile: `dev` or `prod` |
| `DB_URL` | PostgreSQL JDBC connection URL |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | Base64-encoded JWT signing secret |

The development profile defaults to `jdbc:postgresql://localhost:5432/codepilot` and username `postgres`. Set the database password and JWT secret through environment variables.

Never commit real credentials or secrets to Git.

## Run Locally

Open a terminal in the `backend` directory.

```powershell
# Verify Java version
java -version

# Clean and compile
.\mvnw.cmd clean compile

# Run tests and verify the application
.\mvnw.cmd clean verify

# Start the backend
.\mvnw.cmd spring-boot:run
```

The default active profile is `dev`.

## API Documentation

After starting the application:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Use the Swagger UI to explore and test the available REST endpoints.

## Main API Modules

- Authentication — registration and login
- Workspaces — workspace management
- Projects — project management within workspaces
- Tasks — task management within projects

Protected endpoints require JWT authentication.

## Testing

Run the complete test suite:

```powershell
.\mvnw.cmd clean verify
```

Integration tests use Testcontainers with PostgreSQL. Docker Desktop must be available when running those tests.

## Profiles

- `dev`: local development configuration; Hibernate schema update enabled.
- `prod`: production configuration; Hibernate schema validation enabled.

Production database schema changes should be managed through versioned database migrations.
