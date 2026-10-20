# Lab 49 — backend demo notes

Work in progress. The runbook sections (exact curl / SQL / Kafka evidence and
`mvn clean verify` results) land with the later Lab 49 slices below; nothing
here claims the full lab is done yet.

## Step 1 — vertical slice and definition of done

Selected story: **CAP-12 — Record customer interaction** (`docs/backlog.md`).
As an agent I want to POST an interaction for `CUS-1001` so that the note
persists in PostgreSQL and emits a versioned Kafka event.

Acceptance criteria (numbered, from `docs/backlog.md`):

1. `POST /api/v1/interactions` for `CUS-1001` returns `201` with correlation `lab-request-001`.
2. The interaction row is visible in PostgreSQL.
3. A `CustomerInteractionRecordedV1` event is published to `crm.customer.interactions.v1`.
4. `CUS-9999` returns `404`, with no row written and no event published.

Change list for the slice:

- **API:** `POST` + `GET /api/v1/interactions`, DTO records only (no JPA entity in JSON), Bean Validation, Problem Details errors.
- **Persistence:** Flyway `V1__crm_schema.sql`, JPA repositories, timeline query newest-first.
- **Event:** `CustomerInteractionRecordedV1` (frozen below), partition key = customer public id, publish **after** the DB transaction commits (`docs/architecture.md`: PostgreSQL is the system of record).
- **Security stub:** demo Bearer filter (`lab-demo-token`) for now; real JWT is Lab 51. Event `actor` is the `demo-agent` placeholder until then.
- **Observability:** `X-Correlation-ID` threaded header → row → event → logs. Logs carry ids and correlation only, never the interaction summary.

Fixture plan:

| ID | Who / use |
| --- | --- |
| `CUS-1001` | Amina Khan (`ACTIVE`, seeded by Flyway V1) — primary create target |
| `CUS-1002` | Ravi Singh (`PROSPECT`, seeded by Flyway V1) — secondary / list checks |
| `CUS-9999` | Does not exist — not-found paths |
| `lab-request-001` | `X-Correlation-ID` on HTTP and on the event |

Demo evidence checklist (definition of done):

- [ ] Happy-path curl: `201` for `CUS-1001` with `lab-request-001`
- [ ] Negative curls: invalid `interactionType` → `400`; `CUS-9999` → `404`; empty summary → `400`
- [ ] SQL proof the row exists in PostgreSQL
- [ ] Kafka proof: one V1 event per successful create, keyed by `CUS-1001`
- [x] Unit tests: service create/list, correlation precedence, no publish on not-found, publisher waits for after-commit
- [x] HTTP / persistence / messaging integration tests (Testcontainers: real PostgreSQL + Kafka; Kafka IT asserts the V1 payload, key, and that the summary never appears)
- [ ] `mvn -B clean verify` green twice with identical results
- [ ] No secrets, `target/`, or broker data committed

## Event contract V1 (frozen)

Topic: `crm.customer.interactions.v1` · key: customer public id (e.g. `CUS-1001`)

| Field | Type | Notes |
| --- | --- | --- |
| `eventId` | UUID | Fresh per event; consumer dedupe key |
| `eventType` | string | `CustomerInteractionRecorded` |
| `eventVersion` | int | `1` |
| `occurredAt` | instant | Publish time (UTC) |
| `correlationId` | string | From `X-Correlation-ID` (header > body > `lab-request-001`) |
| `actor` | string | `demo-agent` placeholder until Lab 51 JWT |
| `customerId` | string | Public id, e.g. `CUS-1001` |
| `interactionId` | string | Interaction UUID as string |
| `interactionType` | string | `CALL` / `EMAIL` / `NOTE` / `MEETING` |

The event never carries the interaction summary.

## Endpoint

`POST /api/v1/interactions` · `GET /api/v1/interactions?customerId=CUS-1001`

## Happy path

- Customer: `CUS-1001` (Amina Khan)
- Correlation: `lab-request-001`
- Expected: 201 with generated `id`

## Failure path

- Customer: `CUS-9999` → unknown customer

## Verify (so far)

```bash
cd backend
mvn -B clean verify   # 23 tests: 16 unit/MockMvc + 7 Testcontainers ITs, BUILD SUCCESS
```

The integration tests start their own PostgreSQL 16 and Kafka containers, so
no local Compose ports are needed. On this machine Docker runs under Colima,
which Testcontainers does not auto-detect — export these first:

```bash
export DOCKER_HOST=unix://$HOME/.colima/default/docker.sock
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
```

(Docker Desktop / Linux need neither. The pom pins Docker API `1.41` because
Testcontainers' client defaults to 1.32, which current Docker engines reject.)

Run-verify-twice evidence lands with the failure-experiments slice.

## Remaining Lab 49 slices

Problem Details error bodies, HTTP/JPA/Kafka integration tests, resilient
consumer with dedupe / bounded retry / DLT, failure experiments, and the full
reproduction runbook. Angular client is Lab 50.
