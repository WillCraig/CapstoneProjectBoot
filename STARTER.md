# Integrated starter (Lab 50 base + Lab 49 Kafka)

One codebase for the whole week, so nothing is thrown away between labs.
Package `com.northstar.crm`. Test customers: Amina Khan `CUS-1001`, Ravi Singh `CUS-1002`,
`CUS-9999` = not found. Default correlation id `lab-request-001`.

## Where things came from

| Piece | From |
|---|---|
| `frontend/` (Angular app, login, interaction screens) | Lab 50 starter |
| `backend/` API, DTO validation, error handler, demo bearer filter | Lab 50 starter |
| JPA entities, repositories, Flyway `V1__crm_schema.sql` | Lab 50 starter |
| `compose.yaml` Postgres, `.env.example`, `.gitignore` | Lab 50 starter |
| `backend/.../messaging/` (`CustomerInteractionRecordedV1`, `InteractionEventPublisher`) | Lab 49 starter (publisher adapted to the JPA entity) |
| `spring-kafka` dependency, Kafka config in `application.yml`, Kafka service in `compose.yaml` | Added to bring Lab 49 Kafka forward |
| `InteractionServiceTest` (unit, mocked repos) | Lab 49 starter, adapted |
| `InteractionApiIT` (integration, needs Postgres) | Lab 50 starter |
| `docs/lab49/`, `docs/lab50/` | Each lab's original docs and README |

## What the team still builds

**Lab 49 (backend)**
- `InteractionService.create`: follow TODO 1-6 (correlation id, reject unknown customer, map, save, publish, return DTO).
- `InteractionEventPublisher.publish`: send `CustomerInteractionRecordedV1` to topic `crm.customer.interactions.v1` with `KafkaTemplate`.
- Freeze the event fields with the Lab 48 decision record.

**Lab 50 (frontend + database)**
- `InteractionService.list`: unknown customer check, newest-first list, map to DTO.
- Finish the Angular TODOs (search `CUS-1001`, record an interaction, show errors).
- Prove the row lands in Postgres.

**Lab 51 (security + delivery)**
- Replace the demo bearer filter with real JWT checks and role rules.
- GitHub Actions pipeline, Docker images, OpenShift deploy and rollback.

`InteractionServiceTest` and `InteractionApiIT` **fail on purpose** until the service TODOs are done.

## How to run

```bash
cp .env.example .env          # local only, never commit .env
docker compose up -d          # Postgres 16 + Kafka (single node)

cd backend
mvn verify                    # tests fail until TODOs are done
mvn spring-boot:run

cd ../frontend
npm ci
npx ng serve                  # http://localhost:4200
```
