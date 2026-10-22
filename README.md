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

You need Docker (Docker Desktop on a Mac, running), JDK 21, Maven, and Node 20+.

```bash
cp .env.example .env          # local only, never commit .env
docker compose up -d          # Postgres 16 + single-node Kafka
```

Backend (first terminal):

```bash
cd backend
mvn clean verify              # unit, MockMvc, security, JPA, API, and Kafka integration tests
                              # (Testcontainers: needs Docker running, e.g. Docker Desktop)
SPRING_PROFILES_ACTIVE=local SERVER_PORT=8081 mvn spring-boot:run
                              # API on http://localhost:8081 (frontend dev env points here)
```

Frontend (second terminal):

```bash
cd frontend
npm ci
npx ng serve                  # UI on http://localhost:4200
```

#### Windows (PowerShell)

Same steps, with Docker Desktop running. PowerShell sets environment variables with
`$env:NAME = "value"`; they last until that terminal is closed.

```powershell
Copy-Item .env.example .env   # local only, never commit .env
docker compose up -d          # Postgres 16 + single-node Kafka
```

Backend (first terminal):

```powershell
cd backend
mvn clean verify
$env:SPRING_PROFILES_ACTIVE = "local"
$env:SERVER_PORT = "8081"
mvn spring-boot:run           # API on http://localhost:8081
```

Frontend (second terminal):

```powershell
cd frontend
npm ci
npx ng serve                  # UI on http://localhost:4200
```

Selenium e2e (third terminal for the server, then `npm run e2e` in another):

```powershell
cd frontend
$env:CRM_E2E_API_TARGET = "http://localhost:8081"
npm run e2e:serve
```

Find what is holding a port (the Windows version of the `lsof` check below):
`Get-NetTCPConnection -State Listen -LocalPort 5432,9092,8081,4200 | Select-Object LocalPort, OwningProcess`,
then `Get-Process -Id <OwningProcess>`.
`scripts/smoke-crm.sh` is a bash script; run it from Git Bash or WSL.

The backend refuses to start without `CRM_JWT_SECRET`. The `local` profile
(`backend/src/main/resources/application-local.yml`) supplies throwaway dev-only values:

| User | Roles | Local-profile password |
| --- | --- | --- |
| `agent` | AGENT | `agent-local-dev` |
| `admin` | AGENT, ADMIN | `admin-local-dev` |

Open http://localhost:4200, sign in as `agent`, and pick **Look up a customer** on the home
screen to search. Only `admin`
can close a customer or reopen a closed one, whether by the close button or by setting the
status in the edit form (the API returns 403 for `agent`; the UI hides those options).

Health: `GET /actuator/health/readiness` and `/actuator/health/liveness` (public).
Selenium UI test: see [frontend/e2e/README.md](frontend/e2e/README.md).

Changing the look: site-wide styles are in `frontend/src/styles.css`, the header in
`frontend/src/app/app.component.html` / `.css`, and each page's styles in its feature folder
(`customer-pages.component.css` for the customer pages, `interaction-list-page.component.css`
for the interaction pages, `login-page.component.css` for sign-in).

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

### Troubleshooting

- **Port already in use** (`port is already allocated` from Compose, or `Port 4200 is already
  in use` from `ng serve`): run `lsof -nP -iTCP:5432 -iTCP:9092 -iTCP:8081 -iTCP:4200 -sTCP:LISTEN`
  and `docker ps`. Stop the other project's container (`docker stop <name>`; its data is kept)
  or process, or run Angular on 4201 (`npx ng serve --port 4201`). The backend only allows
  browser origins 4200 and 4201 (on `localhost` or `127.0.0.1`), so other ports get a CORS 403 on login.
- **`127.0.0.1:4200` does not load:** `ng serve` is set to listen on `127.0.0.1` in
  `angular.json`, so both `http://localhost:4200` and `http://127.0.0.1:4200` work. An older
  checkout listened only on IPv6 `::1`, where only `localhost` worked.
- **Login fails or the app shows the old demo-token screen:** an old backend is still running
  on 8081. Stop it (Ctrl+C, or `pkill -f spring-boot:run`), start it again with the command
  above, and hard-refresh the browser.
- **Selenium (`npm run e2e`) times out on `/customers`:** start the e2e server with
  `CRM_E2E_API_TARGET=http://localhost:8081 npm run e2e:serve`. The proxy defaults to 8080.
- **Smoke script against a laptop backend:** set `CRM_SMOKE_SKIP_FRONTEND=true`, or it fails
  with a 401 on `/`:
  `CRM_BASE_URL=http://localhost:8081 CRM_SMOKE_SKIP_FRONTEND=true CRM_SMOKE_PASSWORD=agent-local-dev bash scripts/smoke-crm.sh`
- **`docker build` fails on a missing JAR or `dist/`:** the Dockerfiles only copy prebuilt
  output. Run `mvn clean verify` in `backend/` and `npx ng build` in `frontend/` first, then
  `docker build -t northstar-crm-backend:local backend` and
  `docker build -t northstar-crm-frontend:local frontend`.

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
  - [Lab 48 planning pack index](docs/lab48/README.md) — every Lab 48 deliverable, file and status
  - [Architecture](docs/architecture.md)
  - [AI usage plan](docs/ai-usage-plan.md)
  - [Environment strategy](docs/environment-strategy.md)
  - [GitHub Actions plan](docs/github-actions-plan.md)
  - [Terraform / Ansible plan](docs/terraform-ansible-plan.md)
  - [Backlog](docs/backlog.md)
  - [ADRs](docs/adrs/) · [NFRs](docs/nfrs.md) · [Risk register](docs/risk-register.md) · [Team roles](docs/team-roles.md)
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
