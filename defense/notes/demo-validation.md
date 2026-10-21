# Demo validation notes

Checked 2026-10-21 about 5:46-5:54 PM ET. A stack was already running. It was left running.

| Port | What was already there |
| --- | --- |
| 5432 | container `crm-postgres` (healthy) |
| 9092 | container `crm-kafka` |
| 8081 | Java pid 42016, cwd `backend/` of the main checkout, `com.northstar.crm.CrmApplication` |
| 4200 | `ng serve --port 4200` (pid 46576), listening on `[::1]` only |

Do not start a second compose project on 5432/9092, and do not `docker compose down` this one unless the team means to.

## Start from scratch (README)

From the repo root, with Docker Desktop running, JDK 21, Maven, Node:

```bash
cp .env.example .env
docker compose up -d
```

Backend:

```bash
cd backend
SPRING_PROFILES_ACTIVE=local SERVER_PORT=8081 mvn spring-boot:run
```

Frontend, other terminal:

```bash
cd frontend
npm ci
npx ng serve
```

UI: http://localhost:4200 (this `ng serve` is on IPv6 localhost only; `http://127.0.0.1:4200` did not connect)
API: http://localhost:8081

The local profile is what supplies the signing key and the two demo passwords. Those values stay in `backend/src/main/resources/application-local.yml`. Do not paste them into slides. The password string is also written in `README.md`.

CORS allows 4200 and 4201 only. Other Angular ports get a 403 on login.

## Health

```bash
curl -sS http://127.0.0.1:8081/actuator/health/readiness
curl -sS http://127.0.0.1:8081/actuator/health/liveness
```

Observed on the already-running API: readiness `200` and `{"status":"UP"}`. The response also echoed `X-Correlation-Id`.

`http://localhost:4200/` returned 200 and `<title>Northstar CRM`. `http://127.0.0.1:4200` did not connect. Use `localhost`.

## Fixtures on that database

```bash
docker exec crm-postgres psql -U crm -d crm -c \
  "SELECT public_id, full_name, status FROM customer ORDER BY public_id;"
```

Observed:

| public_id | full_name | status |
| --- | --- | --- |
| CUS-1001 | Amina Khan | ACTIVE |
| CUS-1002 | Ravi Singh | PROSPECT |
| CUS-1003 | (extra local row, not in Flyway) | ACTIVE |
| CUS-1004 | (extra local row, not in Flyway) | CLOSED |

Search **Amina** or **Ravi** so the extra rows stay off camera. Do not invent a new customer during the demo.

`GET /api/v1/customers/CUS-1001` as `agent` returned Amina Khan, ACTIVE.
`GET /api/v1/customers/CUS-1002` returned Ravi Singh, PROSPECT.
`GET /api/v1/customers/CUS-9999` returned 404 `Unknown customer: CUS-9999`.

## Smoke (do this before the panel if you want a fresh line)

Against the laptop API, frontend skipped:

```bash
CRM_BASE_URL=http://localhost:8081 \
CRM_SMOKE_SKIP_FRONTEND=true \
CRM_SMOKE_PASSWORD='<local agent password from README>' \
bash scripts/smoke-crm.sh
```

Expected last line, from the script and from `reports/local-test-run-2026-10-21.md`:

```text
Smoke passed for CUS-1001 with correlation lab-request-001
```

Not re-run while writing this pack. The script creates a note and soft-deletes it.

## Kafka console (path checked on `crm-kafka`)

```bash
docker exec crm-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic crm.customer.interactions.v1 \
  --from-beginning --timeout-ms 8000
```

The event has no summary field. It does have `correlationId` and `customerId`.

## SQL after a demo note

```bash
docker exec crm-postgres psql -U crm -d crm -c \
  "SELECT c.public_id, i.interaction_type, i.correlation_id, i.deleted_at IS NULL AS visible
   FROM customer_interaction i
   JOIN customer c ON c.customer_id = i.customer_id
   WHERE i.correlation_id = 'lab-request-001'
   ORDER BY i.created_at DESC
   LIMIT 5;"
```

## Tests run for this pack

Backend, worktree, 2026-10-21 17:48 ET:

```text
mvn -B clean verify
Tests run: 99, Failures: 0, Errors: 0, Skipped: 0
SecurityIT: Tests run: 17
BUILD SUCCESS
```

Frontend, same worktree:

```text
npm ci
npx ng test --watch=false --browsers=ChromeHeadless
TOTAL: 43 SUCCESS
```

CI on `0235018` (run 39649860445) reported the same 99 and 43.
