# Repository Guidelines

## Project Structure

This repository is a single-module Maven application using Java 21 and Spring Boot. Production code is under `src/main/java/org/nakrut`, with the application entry point in the root package so component scanning includes all subpackages.

- `controller`: REST endpoints under `/api/users` and `/api/tasks`
- `dto`: validated request records and serializable response records
- `service`: transactional business logic, logging, and cache annotations
- `repository`: Spring Data JPA repositories
- `mapper`: request/entity/response conversion
- `model`: JPA entities and domain enums
- `exception`: domain exceptions and RFC 9457 Problem Detail handling
- `config`: shared application constants and configuration support

Runtime configuration is in `src/main/resources`. `application.yaml` contains the default PostgreSQL and Redis settings, while `application-no-cache.yaml` disables caching. Liquibase migrations live under `src/main/resources/db/changelog`. Tests mirror the production package structure under `src/test/java`. Maven output belongs in `target/` and must not be committed.

## Build and Local Development

This repository does not contain Maven wrapper scripts, so use an installed `mvn`.

- `docker compose up -d redis` starts the Redis service defined in `compose.yaml`.
- `mvn spring-boot:run` starts the application on port 8080 with Redis caching enabled.
- `mvn spring-boot:run "-Dspring-boot.run.profiles=no-cache"` runs it without caching.
- `mvn test` compiles the project and runs all tests.
- `mvn clean package` runs a clean build and creates `target/mentorship-0.0.1-SNAPSHOT.jar`.
- `java -jar target/mentorship-0.0.1-SNAPSHOT.jar` runs the packaged application.

PostgreSQL database `mentorship` must be reachable on `localhost:5432` for the application and the full context test. Redis must be reachable on `localhost:6379` only when running the application with its default cache configuration; tests use either the `no-cache` profile or an in-memory cache manager. Swagger UI is available at `/swagger-ui.html`, and the OpenAPI document is at `/v3/api-docs`.

## Architecture and Coding Conventions

Use four-space indentation and standard Java naming: `PascalCase` classes, `camelCase` members, and lowercase packages. No formatter or linter is configured, so follow nearby style and organize imports consistently.

Keep controllers limited to HTTP concerns, validation, and status codes. Put business rules and transaction boundaries in services, persistence queries in repositories, and conversion logic in mappers. Prefer constructor injection; Lombok's `@RequiredArgsConstructor` is the established pattern.

Request and response DTOs are Java records. Add Jakarta Bean Validation constraints to request DTOs and keep persistence entities out of controller responses. Cacheable response DTOs and their nested values must remain Java-serializable because Redis uses JDK serialization by default.

JPA entities use protected no-argument constructors and expose setters only for intentionally mutable fields. Do not add generated `toString`, `equals`, or `hashCode` methods that traverse lazy relationships. Keep task ownership immutable unless a business requirement explicitly introduces reassignment.

Services return DTOs, use `@Transactional(readOnly = true)` for reads and `@Transactional` for writes, and log state-changing operations without sensitive request data. Expected domain failures should extend the existing exception hierarchy and be mapped by `GlobalExceptionHandler`. Preserve sanitized RFC Problem Detail responses for validation, malformed input, conflicts, missing resources, and unexpected failures.

## Database and Cache Rules

Liquibase is the schema authority, and Hibernate uses `ddl-auto: validate`. Add new ordered change sets to `db.changelog-master.xml`; do not edit a change set that may already have been applied. Keep entity column constraints synchronized with migrations.

Cache names are declared in `org.nakrut.config.CacheNames` and listed separately in `application.yaml`; update both locations together. Maintain the established eviction rules:

- List and detail reads are cached independently.
- Create operations evict the corresponding list cache.
- Update and delete operations evict the corresponding list and affected detail cache.
- The `no-cache` profile must preserve identical application behavior apart from caching.

## Testing Guidelines

Tests use JUnit 5, AssertJ, Mockito, Spring Boot Test, and MockMvc. Name classes `*Tests` and methods after observable behavior, such as `createsTaskWithTodoStatus()`.

- Use Mockito unit tests for isolated service behavior.
- Test mappers and DTO validation without loading Spring.
- Use standalone MockMvc for controllers and exception response contracts.
- Use a focused Spring test with an in-memory `CacheManager` for cache proxy behavior.
- Reserve `@SpringBootTest` for full context integration; the existing context test activates `no-cache` but still requires PostgreSQL for JPA and Liquibase.

Add or update tests whenever refactoring observable mapping, validation, exception, transaction, or cache behavior. Run `mvn test` before submitting changes. There is no enforced coverage threshold.

## Configuration and Security

Treat values in `application.yaml` as local-development defaults only. Do not commit production credentials or `.env` files. Override configuration through environment variables, including:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT`

Avoid logging credentials, full database errors, or other internal details in API responses.

## Commits and Pull Requests

Git history is available and uses short, focused subjects. Prefer imperative commit subjects, optionally following Conventional Commits, such as `refactor: centralize cache names` or `test: simplify cache setup`. Keep commits focused.

Pull requests should explain the purpose, summarize notable changes, identify database, cache, or configuration impact, and report test results. Link relevant issues. Include screenshots only for visible UI or API documentation changes.
