# Repository Guidelines

## Project Structure & Module Organization

This repository is a single-module Maven application using Java 21 and Spring Boot. Production code lives in `src/main/java/org/nakrut`. Keep the application entry point in the root package so Spring can discover subpackages. Domain entities and enums belong in `src/main/java/org/nakrut/model`; add future controllers, services, and repositories under corresponding `controller`, `service`, and `repository` packages.

Configuration files belong in `src/main/resources`; the current database settings are in `application.yaml`. Tests mirror the production package structure under `src/test/java`. Maven-generated output is stored in `target/` and must not be committed.

## Build, Test, and Development Commands

- `mvn spring-boot:run` starts the application locally on the default port, 8080.
- `mvn test` compiles the project and runs all tests.
- `mvn clean package` removes prior build output, tests the application, and creates an executable JAR in `target/`.
- `java -jar target/mentorship-0.0.1-SNAPSHOT.jar` runs the packaged application.

A PostgreSQL database named `mentorship` must be reachable on `localhost:5432` before starting the application or integration tests.

## Coding Style & Naming Conventions

Use four-space indentation and standard Java conventions: `PascalCase` for classes, `camelCase` for methods and fields, and lowercase package names. Keep controllers thin, place business rules in services, and isolate persistence access in repositories. Prefer constructor injection for Spring dependencies. Use Lombok only where it improves readability; avoid generated `toString`, `equals`, or `hashCode` methods that traverse JPA relationships. No formatter or linter is currently configured, so follow the style of nearby files and organize imports through the IDE.

## Testing Guidelines

Tests use JUnit 5 and Spring Boot Test. Name test classes `*Tests` and methods after observable behavior, such as `createsTaskWithCategory()`. Add focused unit tests for business logic and use `@SpringBootTest` only when the full application context is required. Run `mvn test` before submitting changes. No coverage threshold is currently enforced.

## Commit & Pull Request Guidelines

Git history is unavailable in this workspace. Use short, imperative commit subjects, optionally following Conventional Commits, for example `feat: add task repository` or `test: cover user creation`. Keep commits focused. Pull requests should explain the purpose, summarize notable changes, identify database or configuration impacts, link relevant issues, and report test results. Include screenshots only for visible UI or API documentation changes.

## Security & Configuration

Do not commit production credentials. Override datasource values with environment-specific configuration or environment variables, and keep secrets outside the repository. Treat the credentials in `application.yaml` as local-development defaults only.
