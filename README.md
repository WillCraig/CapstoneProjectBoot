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

You need Docker, JDK 21, Maven, and Node 20+. In PowerShell, run Docker Compose from the project root:

```powershell
docker compose up -d
```

Backend (first terminal):

```powershell
Set-Location backend
mvn test                      # tests use the local Postgres and Kafka services
mvn spring-boot:run           # API on http://localhost:8080
```

Frontend (second terminal):

```powershell
Set-Location frontend
npm ci
npm start                     # UI on http://localhost:4200
```

Open http://localhost:4200 and click **Mock login** to open the customer choices screen.
From there, choose customer create/update, customer lookup, or customer interactions.

To change how the app looks, edit `frontend/src/styles.css` for site-wide styling and
`frontend/src/app/app.component.html` / `app.component.css` for the header and page shell.
The login screen markup and styles are in `frontend/src/app/features/login/login-page.component.html`
and `.css`; the interactions screen is in `frontend/src/app/features/interactions/interaction-list-page.component.html`
and `.css`. Its behavior and fixed demo customer are in the matching `.component.ts` file.

Test data:

| Customer id | Who |
| --- | --- |
| `CUS-1001` | Amina Khan |
| `CUS-1002` | Ravi Singh |
| `CUS-9999` | Doesn't exist, should come back 404 |

The API lives under `/api/v1/interactions`. For now it accepts a demo bearer token (`lab-demo-token`); real JWT checks replace that in Lab 51. Default correlation id is `lab-request-001`.

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
- [defense/](defense/) — Lab 52 presentation packet
- [CAPSTONE-BRIEF-AND-RUBRIC.md](CAPSTONE-BRIEF-AND-RUBRIC.md) — the course brief and self-check rubric

## Working in this repo

- Branch off `main`, one issue per branch, PR back into `main` with one non-author review.
- Diagrams are Mermaid in the markdown, not image exports.
- Secrets stay out of git. `.env` is ignored; only `.env.example` is committed.
