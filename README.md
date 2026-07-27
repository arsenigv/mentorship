# Mentorship

## Redis cache experiment

PostgreSQL must be running on `localhost:5432`. Start the local Redis cache and
the application:

```powershell
docker compose up -d redis
mvn spring-boot:run
```

The application caches user and task list/detail GET responses for 10 minutes.
Create operations clear the related list cache, while update and delete
operations clear both the list and affected detail entry.

To run the same application without caching:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=no-cache"
```

Use the same database data for both runs. Measure a first cold request, make one
warm-up request, and then measure several warm requests:

```powershell
curl.exe -s -o NUL -w "total=%{time_total}s`n" http://localhost:8080/api/tasks
curl.exe -s -o NUL http://localhost:8080/api/tasks
1..20 | ForEach-Object {
    curl.exe -s -o NUL -w "total=%{time_total}s`n" http://localhost:8080/api/tasks
}
```

Repeat the measurements with the `no-cache` profile. Redis is intentionally
ephemeral for this experiment; stop and remove its container with:

```powershell
docker compose down
```
