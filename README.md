# CapstoneProjectBoot — Northstar CRM

Our Week 6 capstone: a small customer management platform for Northstar. Agents sign in, look up a customer, and record interactions (calls, emails, notes) against them. The record is saved in PostgreSQL first, then a `CustomerInteractionRecordedV1` event goes out on Kafka; customer status changes publish `CustomerStatusChangedV1`. Access is JWT-based with AGENT and ADMIN roles.

## Stack

| Layer | What we use |
| --- | --- |
| Frontend | Angular 19 (`frontend/`) |
| API | REST, documented with OpenAPI |
| Backend | Spring Boot 3.3, Java 21 (`backend/`, package `com.northstar.crm`) |
| Database | PostgreSQL 16, schema managed by Flyway |
| Events | Kafka, topics `crm.customer.interactions.v1` and `crm.customer.lifecycle.v1` (+ `.DLT`) |
| Security | Spring Security JWT resource server (HS256), roles AGENT / ADMIN |
| Observability | Actuator liveness/readiness, correlation id (`X-Correlation-Id`) in every log line |
| CI/CD | GitHub Actions |
| Containers | Docker, Docker Compose for local dev |
| Deploy | Shared k3s cluster |
| Infra | Terraform and Ansible |

## Run it locally

You need Docker, JDK 21, Maven, and Node 20+.

```bash
cp .env.example .env          # local only, never commit .env
docker compose up -d          # Postgres 16 + single-node Kafka
```

Backend (first terminal):

```bash
cd backend
mvn clean verify              # unit, MockMvc, security, JPA, API, and Kafka integration tests
                              # (Testcontainers; on Colima also export
                              #  DOCKER_HOST=unix://$HOME/.colima/default/docker.sock
                              #  TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock)
SPRING_PROFILES_ACTIVE=local SERVER_PORT=8081 mvn spring-boot:run
                              # API on http://localhost:8081 (frontend dev env points here)
```

Frontend (second terminal):

```bash
cd frontend
npm ci
npx ng serve                  # UI on http://localhost:4200
```

The backend refuses to start without `CRM_JWT_SECRET`. The `local` profile
(`backend/src/main/resources/application-local.yml`) supplies throwaway dev-only values:

| User | Roles | Local-profile password |
| --- | --- | --- |
| `agent` | AGENT | `agent-local-dev` |
| `admin` | AGENT, ADMIN | `admin-local-dev` |

Open http://localhost:4200, sign in as `agent`, and search for a customer. Only `admin`
can close a customer (the API returns 403 for `agent`; the UI hides the button).

Health: `GET /actuator/health/readiness` and `/actuator/health/liveness` (public).
Selenium UI test: see [frontend/e2e/README.md](frontend/e2e/README.md).

Test data:

| Customer id | Who |
| --- | --- |
| `CUS-1001` | Amina Khan |
| `CUS-1002` | Ravi Singh |
| `CUS-9999` | Doesn't exist, should come back 404 |

The API lives under `/api/v1/customers` and `/api/v1/interactions`. Get a token from
`POST /api/v1/auth/login` (`{"username":"agent","password":"..."}`) and send it as
`Authorization: Bearer <jwt>`; tokens expire after 15 minutes. Default correlation id is
`lab-request-001`. Details: [docs/lab51/security-and-observability.md](docs/lab51/security-and-observability.md).

To stop everything: `docker compose down` (add `-v` to wipe the database).

## Repo layout

```text
backend/     Spring Boot API, JPA entities, Flyway migrations, Kafka publisher
frontend/    Angular app: login, interceptors, customer + interaction screens, e2e/ Selenium test
docs/        Architecture, ADRs, plans, backlog, lab notes
defense/     Lab 52 demo and defense packet
reports/     Sanitized evidence (test runs, scans, deploy/smoke/rollback)
compose.yaml Local Postgres + Kafka
```

## Docs

- [docs/](docs/) — planning pack and lab notes
  - [Architecture](docs/architecture.md)
  - [AI usage plan](docs/ai-usage-plan.md)
  - [Environment strategy](docs/environment-strategy.md)
  - [GitHub Actions plan](docs/github-actions-plan.md)
  - [Terraform / Ansible plan](docs/terraform-ansible-plan.md)
  - [Backlog](docs/backlog.md)
  - [Lab 49 backend notes](docs/lab49/README.md) · [Lab 50 frontend + persistence notes](docs/lab50/README.md)
  - [Lab 51 CI/CD runbook](docs/capstone-cicd-runbook.md)
  - [Lab 51 security and observability](docs/lab51/security-and-observability.md) — roles, endpoint matrix, negative tests, health
- [reports/](reports/) — sanitized test, scan, and deploy evidence
  - [Customer management](docs/customer-management.md) — customer CRUD slice (scope, design, tests)
- [defense/](defense/) — Lab 52 presentation packet
- [CAPSTONE-BRIEF-AND-RUBRIC.md](CAPSTONE-BRIEF-AND-RUBRIC.md) — the course brief and self-check rubric

## Working in this repo

- Branch off `main`, one issue per branch, PR back into `main` with one non-author review.
- Diagrams are Mermaid in the markdown, not image exports.
- Secrets stay out of git. `.env` is ignored; only `.env.example` is committed.
