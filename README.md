# Mentorship

Mentorship is a Java 21 and Spring Boot REST API for managing users and their
assigned tasks. It uses PostgreSQL, Liquibase migrations, Redis caching, and
Springdoc OpenAPI documentation.

## Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL on `localhost:5432`
- Redis on `localhost:6379` when caching is enabled

The repository has no Maven wrapper, so all commands use an installed `mvn`.

## Development setup

Create the development database:

```sql
CREATE DATABASE mentorship_dev;
```

The default development credentials are `postgres` / `postgres`. They are local
defaults only and can be changed in `application-dev.yaml`.

Start Redis and run the application:

```powershell
docker compose up -d redis
mvn spring-boot:run
```

The `dev` profile is active by default. Liquibase creates the schema, and
`db/dev-data.sql` idempotently inserts five users and twenty sample tasks on
startup.

To run development without Redis caching:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=dev" "-Dspring-boot.run.arguments=--spring.cache.type=none"
```

The API is available at `http://localhost:8080`.

## Debugging with the dev profile

In IntelliJ IDEA, create a Spring Boot run configuration for
`org.nakrut.MentorshipApplication`, set the active profile to `dev`, and start it
with **Debug**. The equivalent environment variable is:

```text
SPRING_PROFILES_ACTIVE=dev
```

For remote debugging from Maven, start the application suspended on port 5005:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=dev" "-Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
```

Then attach the IDE debugger to `localhost:5005`.

## API documentation

With the application running:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

Main resources:

| Method | Path | Description |
| --- | --- | --- |
| `GET`, `POST` | `/api/users` | List or create users |
| `GET`, `PUT`, `DELETE` | `/api/users/{id}` | Read, update, or delete a user |
| `GET`, `POST` | `/api/tasks` | Filter/page tasks or create a task |
| `GET`, `PUT`, `DELETE` | `/api/tasks/{id}` | Read, update, or delete a task |

Expected failures use RFC Problem Detail responses. Validation and malformed
input return `400`, missing resources return `404`, conflicts return `409`, and
unexpected failures return a sanitized `500` response.

## Task paging and sorting

The task collection endpoint accepts these query parameters:

- `status`: optional exact status filter (`TODO`, `IN_PROGRESS`, or `DONE`)
- `dueDate`: optional exact due-date filter in `YYYY-MM-DD` format

- `page`: zero-based page number; default `0`
- `size`: requested page size; default `20`, maximum `100`
- `sort`: `field,direction`; repeat the parameter for multiple sort fields

Supported task sort fields are `id`, `title`, and `status`. Directions are `asc`
and `desc`. Status sorting follows workflow order `TODO`, `IN_PROGRESS`, `DONE`.
An ID ascending tie-breaker is added when `id` is not explicitly requested.

Example:

```powershell
curl.exe "http://localhost:8080/api/tasks?status=TODO&dueDate=2026-09-10&page=0&size=10&sort=status,asc&sort=title,asc"
```

The filters are independent and use AND semantics when supplied together.
Task create and update requests require a calendar-only `dueDate`; past,
current, and future dates are accepted.

Task collection responses contain `content`, `page`, `size`, `totalElements`,
`totalPages`, `first`, and `last`.

## Database

Liquibase is the schema authority. Ordered change sets are included from
`src/main/resources/db/changelog/db.changelog-master.xml`; Hibernate uses
`ddl-auto: validate` and never creates the schema itself.

The schema contains:

- `app_users`, with a unique non-null username
- `tasks`, with a required owner, title, status, due date, and category
- database checks for valid status and category values
- indexes for task status, due-date, and owner lookups

Development seed data runs only with the `dev` profile. The `prod` profile reads
database configuration from `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`.

## Caching

Redis caches user and task list/detail reads for ten minutes. Create operations
clear the related list cache; update and delete operations clear the list cache
and the affected detail entry. Configure Redis with `REDIS_HOST` and
`REDIS_PORT`.

Stop the development Redis container with:

```powershell
docker compose down
```

## Build and test

```powershell
mvn test
mvn clean package
java -jar target/mentorship-0.0.1-SNAPSHOT.jar
```

The full test suite requires PostgreSQL and uses `mentorship_dev` for its Spring
Boot integration tests. Redis is not required by the tests.
