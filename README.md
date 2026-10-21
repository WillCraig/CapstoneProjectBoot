# CapstoneProjectBoot — Northstar CRM

Our Week 6 capstone: a small customer management platform for Northstar. Agents sign in, look up a customer, and record interactions (calls, emails, notes) against them. The record is saved in PostgreSQL first, then a `CustomerInteractionRecordedV1` event goes out on Kafka.

## Stack

| Layer | What we use |
| --- | --- |
| Frontend | Angular 19 (`frontend/`) |
| API | REST, documented with OpenAPI |
| Backend | Spring Boot 3.3, Java 21 (`backend/`, package `com.northstar.crm`) |
| Database | PostgreSQL 16, schema managed by Flyway |
| Events | Kafka, topic `crm.customer.interactions.v1` |
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
mvn clean verify              # unit, MockMvc, JPA, API, and Kafka integration tests
mvn spring-boot:run           # API on http://localhost:8080
```

Frontend (second terminal):

```bash
cd frontend
npm ci
npx ng serve                  # UI on http://localhost:4200
```

Open http://localhost:4200, click **Mock login**, and search for a customer.

Test data:

| Customer id | Who |
| --- | --- |
| `CUS-1001` | Amina Khan |
| `CUS-1002` | Ravi Singh |
| `CUS-9999` | Doesn't exist, should come back 404 |

The API lives under `/api/v1/interactions`. For now it accepts a demo bearer token (`lab-demo-token`); Lab 51 secures delivery with CI/CD gates, while replacing this demo scheme with real JWT validation remains future application hardening. Default correlation id is `lab-request-001`.

To stop everything: `docker compose down` (add `-v` to wipe the database).

## Repo layout

```text
backend/     Spring Boot API, JPA entities, Flyway migrations, Kafka publisher
frontend/    Angular app: login, interceptors, interaction screens
docs/        Architecture, ADRs, plans, backlog, lab notes
defense/     Lab 52 demo and defense packet
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
  - [Customer management](docs/customer-management.md) — customer CRUD slice (scope, design, tests)
- [defense/](defense/) — Lab 52 presentation packet
- [CAPSTONE-BRIEF-AND-RUBRIC.md](CAPSTONE-BRIEF-AND-RUBRIC.md) — the course brief and self-check rubric

## Working in this repo

- Branch off `main`, one issue per branch, PR back into `main` with one non-author review.
- Diagrams are Mermaid in the markdown, not image exports.
- Secrets stay out of git. `.env` is ignored; only `.env.example` is committed.
