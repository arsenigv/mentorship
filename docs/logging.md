# Centralized logging with ELK

This guide explains how application logs travel through the local Elastic
Stack and how to inspect them in Kibana. The setup is intended for development
and learning only.

## Architecture

```text
Spring Boot service and exception logs
    |
    | ECS JSON, one event per line
    v
logs/mentorship.json
    |
    | Logstash file input
    v
Elasticsearch data stream: logs-mentorship-development
    |
    v
Kibana Discover
```

The application and PostgreSQL run on the host. Docker Compose runs Redis,
Prometheus, Grafana, Elasticsearch, Logstash, and Kibana. The `elk` Spring
profile enables the JSON log file, and the `elk` Compose profile enables the
three Elastic Stack containers.

Spring Boot rotates `logs/mentorship.json` after 10 MB, retains up to seven
archives, and caps the archive total at 100 MB. The `logs/` directory is
ignored by Git. Logstash stores its file position in its named data volume, so
restarting it does not normally ingest the same lines again.

## Prerequisites

- Java 21 and Maven 3.9+
- PostgreSQL with `mentorship_dev` available on `localhost:5432`
- Docker Desktop with Docker Compose
- At least 4 GB of memory available to Docker Desktop

The local Elasticsearch container uses a 512 MB JVM heap and Logstash uses a
256 MB heap. Running ELK together with Prometheus and Grafana therefore needs
more memory than the normal development environment.

## Start the environment

Start all Compose services, including the optional ELK services:

```powershell
docker compose --profile elk up -d
```

Check their state:

```powershell
docker compose --profile elk ps
```

Elasticsearch, Logstash, and Kibana can take one or two minutes to become
ready. Start the application in another terminal with both required Spring
profiles:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=dev,elk"
```

The `dev` profile supplies the local database settings and seed data. The
`elk` profile writes ECS JSON to `logs/mentorship.json`. Starting the
application without `elk` preserves its normal console-only logging behavior.

## Verify each component

Verify the application and its local log file:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
Get-Content logs/mentorship.json -Tail 1
Get-Content logs/mentorship.json -Tail 1 | ConvertFrom-Json
```

The parsed log event should contain fields such as `@timestamp`, `message`,
`log.level`, `service.name`, and `ecs.version`.

Verify Elasticsearch and its cluster health:

```powershell
Invoke-RestMethod http://localhost:9200
Invoke-RestMethod http://localhost:9200/_cluster/health
```

A `yellow` cluster is expected and healthy enough for this single-node setup:
Elasticsearch cannot assign replica shards to a second node. `green` is also
valid.

Verify that Logstash created the data stream and sent documents to it:

```powershell
Invoke-RestMethod `
  http://localhost:9200/_data_stream/logs-mentorship-development

Invoke-RestMethod `
  http://localhost:9200/logs-mentorship-development/_count
```

Open Kibana at <http://localhost:5601>. Authentication is disabled in this
local setup.

## Generate searchable events

Create a unique user to generate a successful business event:

```powershell
$elkUsername = "elk-$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())"
$userBody = @{ username = $elkUsername } | ConvertTo-Json

$newUser = Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8080/api/users `
  -ContentType application/json `
  -Body $userBody

$newUser
```

The resulting log has `event.action=user_create`,
`event.outcome=success`, `mentorship.resource=user`, and the new ID in
`mentorship.entity_id`.

Submit the same username again to generate a rejected operation:

```powershell
try {
    Invoke-RestMethod `
      -Method Post `
      -Uri http://localhost:8080/api/users `
      -ContentType application/json `
      -Body $userBody
} catch {
    Write-Host "Expected conflict: $($_.Exception.Message)"
}
```

This produces a WARN event with `event.action=user_create` and
`event.outcome=failure` without recording the username or request body.

Generate a failed lookup as another example:

```powershell
try {
    Invoke-RestMethod http://localhost:8080/api/users/999999999
} catch {
    Write-Host "Expected not-found response: $($_.Exception.Message)"
}
```

Allow several seconds for Logstash and Elasticsearch to process new events.

## Inspect logs in Kibana

Create the data view once:

1. In Kibana, open **Stack Management** and then **Data Views**.
2. Select **Create data view**.
3. Use `Mentorship Logs` as the name.
4. Use `logs-mentorship-development` as the index pattern.
5. Select `@timestamp` as the timestamp field and save the data view.

Open **Discover**, select **Mentorship Logs**, and set the time range to
**Last 15 minutes**. Add these columns to make the events easy to compare:

```text
@timestamp
log.level
message
event.action
event.outcome
mentorship.resource
mentorship.entity_id
```

Useful Kibana Query Language searches include:

```text
service.name : "mentorship"
```

```text
event.action : "user_create"
```

```text
event.action : "user_create" and event.outcome : "failure"
```

```text
log.level : "WARN" or log.level : "ERROR"
```

The application uses these business fields:

| Field | Meaning | Example |
| --- | --- | --- |
| `event.action` | Operation represented by the event | `task_update` |
| `event.outcome` | Whether the operation succeeded | `success` |
| `mentorship.resource` | Domain resource | `user` or `task` |
| `mentorship.entity_id` | ID of the affected entity | `42` |
| `mentorship.owner_id` | Owner ID involved in a task event | `7` |
| `mentorship.task_status` | Status assigned to a task | `IN_PROGRESS` |
| `error.type` | Java exception type for logged exceptions | `DataIntegrityViolationException` |

Expand a document in Discover to inspect its complete JSON. Application logs
must not contain usernames, request bodies, credentials, task descriptions, or
raw database errors.

## Verify restart and outage behavior

Read the current document count:

```powershell
(Invoke-RestMethod `
  http://localhost:9200/logs-mentorship-development/_count).count
```

Restart Logstash and check the count again:

```powershell
docker compose --profile elk restart logstash
```

Previously consumed events should not be duplicated because the `logstash-data`
volume preserves the input position. Generate one new API event and confirm the
count increases.

To prove that logging does not couple application availability to Logstash,
temporarily stop it:

```powershell
docker compose --profile elk stop logstash
```

Generate an API event and confirm that it was still written locally:

```powershell
Get-Content logs/mentorship.json -Tail 5
```

Restart Logstash:

```powershell
docker compose --profile elk start logstash
```

The new local event should appear in Elasticsearch and Kibana after Logstash
resumes reading the file.

## Troubleshooting

Inspect the relevant container logs first:

```powershell
docker compose --profile elk logs --tail 100 elasticsearch
docker compose --profile elk logs --tail 100 logstash
docker compose --profile elk logs --tail 100 kibana
```

If `logs/mentorship.json` does not exist, confirm that the application was
started with `dev,elk`, not only `dev`. If the local file contains events but
the data stream is missing, inspect the Logstash logs for JSON parsing,
permissions, pipeline syntax, or Elasticsearch connection errors.

If the data stream exists but Discover shows no results, confirm the selected
data view and widen the time range. Also verify that `@timestamp` was selected
as the data view's time field.

If Elasticsearch exits during startup, check Docker Desktop's available memory
and the Elasticsearch container logs. If port startup fails, confirm that ports
`9200` and `5601` are not already in use:

```powershell
Get-NetTCPConnection -LocalPort 9200,5601 -ErrorAction SilentlyContinue
```

Validate the Compose model and inspect the Logstash pipeline without changing
application code:

```powershell
docker compose --profile elk config
docker compose --profile elk run --rm --no-deps logstash `
  /usr/share/logstash/bin/logstash `
  --config.test_and_exit `
  --path.data /tmp/logstash-config-test `
  -f /usr/share/logstash/pipeline/mentorship.conf
```

## Security and cleanup

Elasticsearch security and authentication are intentionally disabled. The
Elasticsearch and Kibana ports are bound to `127.0.0.1` to limit access to the
local machine. Do not reuse this configuration in production. A production
deployment requires authentication, TLS, protected secrets, retention rules,
backups, monitoring, and deliberate resource sizing.

Stop and remove the containers while preserving Elasticsearch, Logstash,
Prometheus, and Grafana data:

```powershell
docker compose --profile elk down
```

The following command is destructive: it also removes every named volume in
the Compose project, including stored logs, metrics, Grafana state, and
Logstash's saved file position:

```powershell
docker compose --profile elk down -v
```

Use `-v` only when intentionally resetting the complete local observability
environment.
