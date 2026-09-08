# Observability with Prometheus and Grafana

This guide explains the Mentorship application's observability stack and
provides exercises for learning Micrometer, Spring Boot Actuator, Prometheus,
Grafana, and PromQL.

## Architecture

The metric data follows this path:

```text
Spring Boot code
    |
    v
Micrometer MeterRegistry
    |
    v
Actuator /actuator/prometheus
    |  Prometheus scrapes every 5 seconds
    v
Prometheus time-series database
    |  PromQL queries
    v
Grafana dashboard
```

Each component has a separate responsibility:

- **Micrometer** is the instrumentation API used by the application. Spring
  Boot automatically registers JVM, HTTP server, process, and database-pool
  meters. `ApplicationMetrics` registers the application's business counters.
- **Spring Boot Actuator** publishes management endpoints. The `metrics`
  endpoint is useful for inspecting meters, while the `prometheus` endpoint
  converts them to the Prometheus exposition format.
- **Prometheus** pulls, or scrapes, the exposition endpoint every five seconds
  and stores timestamped samples. It evaluates PromQL expressions over those
  samples.
- **Grafana** queries Prometheus and turns the results into dashboards. The data
  source and Mentorship dashboard are provisioned from files in this repository.

The Spring Boot application and PostgreSQL run on the host. Docker Compose runs
Redis, Prometheus, and Grafana. Prometheus reaches the host application through
`host.docker.internal:8080`.

## Start and stop the environment

Prerequisites are Java 21, Maven 3.9+, Docker Desktop, and PostgreSQL with the
`mentorship_dev` database available on `localhost:5432`.

Start Redis, Prometheus, and Grafana:

```powershell
docker compose up -d
```

Start the application in another terminal:

```powershell
mvn spring-boot:run
```

The `dev` Spring profile is active by default. It is the only profile that
exposes the `metrics` and `prometheus` Actuator endpoints.

Inspect or stop the containers with:

```powershell
docker compose ps
docker compose logs prometheus
docker compose logs grafana
docker compose down
```

`docker compose down` removes the containers but preserves the named
`prometheus-data` and `grafana-data` volumes. Do not use `docker compose down -v`
unless you intentionally want to delete the stored metrics and Grafana state.

## Development URLs and credentials

| Component | URL | Authentication |
| --- | --- | --- |
| Application | <http://localhost:8080> | None |
| Swagger UI | <http://localhost:8080/swagger-ui.html> | None |
| Actuator meter inspection | <http://localhost:8080/actuator/metrics> | None; development only |
| Prometheus exposition | <http://localhost:8080/actuator/prometheus> | None; development only |
| Prometheus | <http://localhost:9090> | None |
| Prometheus targets | <http://localhost:9090/targets> | None |
| Grafana | <http://localhost:3000> | `admin` / `admin` by default |
| Mentorship dashboard | <http://localhost:3000/d/mentorship-overview/mentorship-overview> | Grafana login |

The Grafana credentials are local defaults. Override them before the first
Grafana startup with `GRAFANA_ADMIN_USER` and `GRAFANA_ADMIN_PASSWORD`.

The provisioned JSON file is the dashboard's source of truth. Grafana does not
allow UI changes to overwrite it. After changing the JSON, allow the file
provider ten seconds to rescan it; if the change is not loaded, run:

```powershell
docker compose restart grafana
```

## Metric concepts

### Counter

A counter is cumulative and only increases while the application process is
running. It resets when that process restarts. Prometheus counter names normally
end in `_total`.

The custom Micrometer meter
`mentorship.business.operations` is exported as
`mentorship_business_operations_total`. It counts successful create, update,
and delete operations by `resource` and `operation`.

Counters usually should not be graphed directly because the raw value depends
on how long the process has been running. Use `rate()` or `increase()` instead.

### Gauge

A gauge represents a current value that can increase or decrease. Examples in
this project include `process_cpu_usage`, `jvm_memory_used_bytes`, and
`hikaricp_connections_active`.

A gauge is a snapshot, not an event count. Functions intended for counters,
such as `rate()`, normally should not be applied to gauges.

### Timer and histogram

A Micrometer timer records how many operations occurred and how long they took.
The Spring MVC HTTP timer is exported as metric families including:

- `http_server_requests_seconds_count`
- `http_server_requests_seconds_sum`
- `http_server_requests_seconds_max`

The development configuration enables a percentile histogram for
`http.server.requests`, adding
`http_server_requests_seconds_bucket`. Each bucket counts requests whose
duration was at or below its `le` boundary. Prometheus can estimate a percentile
from those buckets with `histogram_quantile()`.

The dashboard's p95 latency is an estimate below which approximately 95% of
observed request durations fall. Histogram aggregation is useful across
multiple application instances; averaging instance percentiles is not.

### Labels and cardinality

Labels add dimensions to a metric. For example:

```text
mentorship_business_operations_total{
  resource="task",
  operation="create"
}
```

The custom labels are deliberately bounded:

- `resource`: `user` or `task`
- `operation`: `create`, `update`, or `delete`
- `status`: `TODO`, `IN_PROGRESS`, or `DONE`

Cardinality is the number of distinct label combinations. Every combination
becomes a separate time series and consumes memory and storage. Never put user
IDs, task IDs, usernames, descriptions, request bodies, timestamps, or other
unbounded values in metric labels.

### `rate()` and `increase()`

Both functions are intended for counters and compensate for normal counter
resets:

- `rate(counter[5m])` calculates the average per-second change during the last
  five minutes. It is appropriate for traffic throughput.
- `increase(counter[5m])` estimates the total change during the last five
  minutes. It is appropriate for questions such as "how many successful writes
  occurred?"

Grafana supplies useful interval variables:

- `$__rate_interval` selects a safe window based on the dashboard resolution
  and Prometheus scrape interval.
- `$__range` is the complete time range currently selected in the dashboard.

Changing the dashboard time range therefore changes the result of business
panels that use `increase(...[$__range])`.

## Custom business metrics

`ApplicationMetrics` pre-registers all expected label combinations, so their
series exist at zero before the first operation. Service methods increment them
only after successful persistence operations:

```text
mentorship_business_operations_total{resource, operation}
mentorship_task_status_assignments_total{status}
```

The task-status counter records assignment events. A task creation records its
initial `TODO` status, and an update records the newly assigned status. It does
not describe how many tasks currently have each status. A gauge backed by a
database query would be required for that different question.

Validation failures, missing resources, duplicate usernames, and other rejected
writes do not increment the success counters.

## Dashboard PromQL

The provisioned **Mentorship Overview** dashboard contains these queries.

### Application availability

```promql
max(up{job="mentorship"})
```

Prometheus creates `up` automatically for every scrape target. `1` means the
last scrape succeeded; `0` means it failed.

### HTTP request rate

```promql
sum(
  rate(
    http_server_requests_seconds_count{
      job="mentorship",
      uri!~"/actuator.*"
    }[$__rate_interval]
  )
)
```

This shows non-Actuator requests per second. Excluding Actuator prevents
Prometheus's own scrape requests from looking like application traffic.

### HTTP p95 latency

```promql
histogram_quantile(
  0.95,
  sum by (le) (
    rate(
      http_server_requests_seconds_bucket{
        job="mentorship",
        uri!~"/actuator.*"
      }[$__rate_interval]
    )
  )
)
```

The query calculates a rate for each histogram bucket, combines instances while
preserving the required `le` boundary, and estimates the 95th percentile.

### HTTP 5xx percentage

```promql
100
* (
  sum(
    rate(
      http_server_requests_seconds_count{
        job="mentorship",
        status=~"5.."
      }[$__rate_interval]
    )
  ) or vector(0)
)
/ clamp_min(
  sum(
    rate(
      http_server_requests_seconds_count{
        job="mentorship"
      }[$__rate_interval]
    )
  ),
  0.001
)
```

`or vector(0)` returns zero when no 5xx series exists. `clamp_min()` prevents
division by zero when traffic is absent. Expected validation failures are 4xx
responses and therefore do not increase this panel.

### Process CPU

```promql
100 * process_cpu_usage{job="mentorship"}
```

Micrometer exports CPU usage as a ratio, so multiplying by 100 produces a
percentage.

### JVM heap usage

```promql
100
* sum(jvm_memory_used_bytes{job="mentorship", area="heap"})
/ sum(jvm_memory_max_bytes{job="mentorship", area="heap"})
```

This calculates used heap as a percentage of the JVM's reported maximum heap.

### Database connections

```promql
hikaricp_connections_active{job="mentorship"}
hikaricp_connections_idle{job="mentorship"}
hikaricp_connections_pending{job="mentorship"}
```

These gauges show work currently using a connection, available pooled
connections, and threads waiting for a connection.

### Successful business operations

```promql
sum by (resource, operation) (
  increase(
    mentorship_business_operations_total{
      job="mentorship"
    }[$__range]
  )
)
```

This reports successful writes during the selected dashboard time range.

### Task status assignments

```promql
sum by (status) (
  increase(
    mentorship_task_status_assignments_total{
      job="mentorship"
    }[$__range]
  )
)
```

This reports task creation/update assignments during the selected time range,
grouped by the assigned status.

## Exercises

Run these exercises with the application and all Compose services running. The
dashboard refreshes every five seconds; allow at least one scrape interval
after generating traffic.

### 1. Inspect the source metrics

List available Actuator meter names:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/metrics
```

Inspect one logical Micrometer meter:

```powershell
Invoke-RestMethod `
  'http://localhost:8080/actuator/metrics/mentorship.business.operations?tag=resource:user&tag=operation:create'
```

Compare it with the Prometheus-formatted series:

```powershell
(Invoke-WebRequest http://localhost:8080/actuator/prometheus).Content `
  -split "`n" |
  Select-String '^mentorship_'
```

Notice the dot-to-underscore name conversion and `_total` counter suffix.

### 2. Generate HTTP traffic

```powershell
1..50 | ForEach-Object {
    Invoke-WebRequest http://localhost:8080/api/users | Out-Null
    Invoke-WebRequest http://localhost:8080/api/tasks | Out-Null
}
```

Open the dashboard and observe **Request Rate** and **HTTP p95 Latency**. Change
the time range between 5 and 15 minutes and inspect the effect.

### 3. Create a user and observe a business counter

```powershell
$testUsername = "metrics-$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())"
$userBody = @{ username = $testUsername } | ConvertTo-Json

$newUser = Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8080/api/users `
  -ContentType 'application/json' `
  -Body $userBody

$newUser
```

Inspect the user/create counter through Actuator, then wait for Prometheus to
scrape it and check **Successful Business Operations** in Grafana.

### 4. Create and update a task

Find an existing owner rather than assuming database IDs:

```powershell
$users = Invoke-RestMethod http://localhost:8080/api/users
$ownerId = ($users | Where-Object username -eq 'alice' | Select-Object -First 1).id
$dueDate = (Get-Date).Date.AddDays(7).ToString('yyyy-MM-dd')
```

Create the task. New tasks start in `TODO`:

```powershell
$taskBody = @{
    title = 'Observe Prometheus metrics'
    description = 'Generate a successful task write'
    dueDate = $dueDate
    category = 'EDUCATION'
    userId = $ownerId
} | ConvertTo-Json

$newTask = Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8080/api/tasks `
  -ContentType 'application/json' `
  -Body $taskBody
```

Update its status:

```powershell
$updateBody = @{
    title = $newTask.title
    description = $newTask.description
    status = 'IN_PROGRESS'
    dueDate = $newTask.dueDate
    category = $newTask.category
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Put `
  -Uri "http://localhost:8080/api/tasks/$($newTask.id)" `
  -ContentType 'application/json' `
  -Body $updateBody
```

After the next scrape, the business dashboard should show a task/create and a
task/update event. The status panel should show a `TODO` and an `IN_PROGRESS`
assignment.

### 5. Prove rejected writes do not count as successes

Create a helper that reads the user/create count directly from Actuator:

```powershell
function Get-UserCreateCount {
    $metric = Invoke-RestMethod `
      'http://localhost:8080/actuator/metrics/mentorship.business.operations?tag=resource:user&tag=operation:create'

    return ($metric.measurements |
      Where-Object statistic -eq 'COUNT').value
}

$before = Get-UserCreateCount

try {
    Invoke-RestMethod `
      -Method Post `
      -Uri http://localhost:8080/api/users `
      -ContentType 'application/json' `
      -Body (@{ username = 'alice' } | ConvertTo-Json)
} catch {
    Write-Host "Expected rejection: $($_.Exception.Message)"
}

$after = Get-UserCreateCount
"Before: $before; after: $after"
```

The values must be equal. The HTTP request meter still records the rejected
request because it occurred, but the business success counter does not.

### 6. Query Prometheus directly

Open <http://localhost:9090/query> and evaluate:

```promql
up{job="mentorship"}
```

```promql
rate(http_server_requests_seconds_count{job="mentorship"}[1m])
```

```promql
increase(mentorship_business_operations_total{job="mentorship"}[15m])
```

Use the Table and Graph views. Compare the labels returned before and after
adding `sum by (resource, operation) (...)`.

### 7. Observe restart behavior and persistence

Restart Prometheus and Grafana without removing their volumes:

```powershell
docker compose restart prometheus grafana
```

After both services become healthy, reopen the dashboard and select a time range
that includes the earlier exercises. The historical samples should remain.

Restarting the Java application resets its in-memory counters to zero. Prometheus
retains samples from before the restart, and `rate()` and `increase()` account
for the reset. This is different from deleting the `prometheus-data` volume.

## Verification checklist

### Actuator is available in development

```powershell
curl.exe -f http://localhost:8080/actuator/prometheus
```

Expected result: HTTP `200` and Prometheus-formatted text.

### Prometheus reports the application as up

Use <http://localhost:9090/targets>, or query the HTTP API:

```powershell
curl.exe `
  'http://localhost:9090/api/v1/query?query=up%7Bjob%3D%22mentorship%22%7D'
```

Expected result: the sample value is `1`.

### Grafana provisioning succeeded

```powershell
curl.exe -u admin:admin `
  http://localhost:3000/api/datasources/uid/prometheus

curl.exe -u admin:admin `
  http://localhost:3000/api/dashboards/uid/mentorship-overview
```

Expected results:

- The data source UID is `prometheus` and its URL is
  `http://prometheus:9090`.
- The dashboard UID is `mentorship-overview`, its folder is `Mentorship`, and
  its `provisioned` value is `true`.

### Traffic and business behavior

Complete exercises 2 through 5 and confirm:

- Reads change the HTTP request-rate and latency panels.
- Successful writes change the corresponding business panels.
- Task creates and updates change the assignment panel.
- A rejected duplicate-user write does not change the success counter.

### Prometheus and Grafana retain data

Complete exercise 7. Ordinary container restart or `docker compose down`
followed by `docker compose up -d` must preserve earlier data. Never add `-v` to
this persistence test.

### Configuration and tests pass

```powershell
docker compose config --quiet
mvn test
```

The Compose command must exit with code zero, and Maven must report `BUILD
SUCCESS`.

### Non-development profiles hide detailed management endpoints

Stop the development application. In one PowerShell terminal, provide the
production profile's required local database settings and run it on a separate
port with caching disabled:

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5432/mentorship_dev'
$env:SPRING_DATASOURCE_USERNAME = 'postgres'
$env:SPRING_DATASOURCE_PASSWORD = 'postgres'

mvn spring-boot:run `
  '-Dspring-boot.run.profiles=prod' `
  '-Dspring-boot.run.arguments=--server.port=8081 --spring.cache.type=none'
```

From another terminal:

```powershell
curl.exe -i http://localhost:8081/actuator/health
curl.exe -i http://localhost:8081/actuator/metrics
curl.exe -i http://localhost:8081/actuator/prometheus
```

The health endpoint should return `200`. The `metrics` and `prometheus`
endpoints should return `404`. Stop the application with Ctrl+C. Remove the
temporary environment variables if they are no longer needed:

```powershell
Remove-Item Env:SPRING_DATASOURCE_URL
Remove-Item Env:SPRING_DATASOURCE_USERNAME
Remove-Item Env:SPRING_DATASOURCE_PASSWORD
```

## Troubleshooting

### Prometheus target is down

1. Confirm the Java application is running and
   <http://localhost:8080/actuator/prometheus> returns `200`.
2. Confirm the `dev` profile is active.
3. Inspect <http://localhost:9090/targets> for the scrape error.
4. Check Prometheus logs with `docker compose logs prometheus`.
5. Confirm `prometheus.yaml` targets `host.docker.internal:8080` and uses
   `/actuator/prometheus`.
6. On a non-Docker-Desktop Linux environment, configure an equivalent host
   gateway before using `host.docker.internal`.

### Grafana dashboard or data source is missing

1. Check `docker compose logs grafana` for provisioning errors.
2. Confirm the Compose mounts for `/etc/grafana/provisioning` and
   `/var/lib/grafana/dashboards` exist.
3. Confirm the data source UID is `prometheus`.
4. Confirm the dashboard appears in the `Mentorship` folder.
5. Validate the files with `docker compose config --quiet` and PowerShell's
   `ConvertFrom-Json`.

### A panel has no data

1. Confirm `up{job="mentorship"}` equals `1` in Prometheus.
2. Generate relevant traffic and wait at least one five-second scrape interval.
3. Select a dashboard range that includes the traffic.
4. Query the panel's base metric directly in Prometheus before applying
   aggregation functions.
5. Check label names and values in the returned series.
6. Remember that a pre-registered counter can correctly remain zero and that
   Grafana may display no increase when no event occurred in the selected range.

## Production next steps

This stack is intentionally configured as a local learning environment. Before
using the same ideas in production:

- Authenticate and authorize access to Grafana, Prometheus, and management
  endpoints; remove default credentials.
- Bind Actuator to a separate management port or private interface and allow
  access only from the monitoring network.
- Add TLS between users and services, and between monitoring components where
  the network is not trusted.
- Define Prometheus retention limits and durable storage, then back up Grafana
  configuration and dashboards.
- Add recording rules and actionable alerts for availability, error rate,
  latency, resource pressure, and scrape failures.
- Centralize structured application and infrastructure logs and correlate them
  with metric labels that have bounded cardinality.
- Add distributed tracing, such as OpenTelemetry, and propagate trace context
  between services.
- Store secrets in a secrets manager rather than source control or plain Compose
  defaults.
- Review every new metric label for cardinality, privacy, and operational value.
