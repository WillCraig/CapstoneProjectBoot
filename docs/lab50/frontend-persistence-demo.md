# Lab 50 — frontend + PostgreSQL persistence demo

> **Dated lab record.** The run steps below (backend on port 8080, the demo bearer token) are
> superseded. Current instructions: [README.md — Run it locally](../../README.md#run-it-locally).
> Auth and roles: [../lab51/security-and-observability.md](../lab51/security-and-observability.md).

> **Superseded (Lab 51):** the demo bearer token and *Mock login* described below were replaced by
> real JWT login with AGENT/ADMIN roles. See [../lab51/security-and-observability.md](../lab51/security-and-observability.md).

This is the reproduction runbook for the Angular side of the Northstar CRM
interaction slice. The backend contract and Kafka evidence remain in
[`../lab49/backend-demo.md`](../lab49/backend-demo.md); the Lab 48 story is
CAP-12 in [`../backlog.md`](../backlog.md), expanded here with the Angular
list/detail seed.

> The guide names this file `docs/frontend-persistence-demo.md`; this repo
> keeps lab notes under `docs/lab50/`, matching Lab 49.

## What was built

```text
Angular routes/forms
  -> correlation + auth + unauthorized interceptors
  -> InteractionApiService (typed HttpClient)
  -> Spring Boot /api/v1/interactions
  -> InteractionService
  -> Spring Data JPA
  -> PostgreSQL, with Flyway V1 + V2 + V3
```

### Routes

| Route | Screen | Access |
| --- | --- | --- |
| `/login` | Mock-login shell | Public |
| `/interactions` | Customer timeline, fixture shortcuts, delete | Guarded |
| `/interactions/new` | Create interaction | Guarded |
| `/interactions/:id` | Detail, edit, delete | Guarded |

All four components are lazy-loaded. An unauthenticated visit to an
interactions route redirects to `/login`.

### API alignment

| UI action | HTTP call | Request/response |
| --- | --- | --- |
| Load timeline | `GET /api/v1/interactions?customerId=CUS-1001` | `Interaction[]` |
| Create | `POST /api/v1/interactions` | `CreateInteractionRequest` → `201 Interaction` |
| Open detail | `GET /api/v1/interactions/{id}` | `Interaction` |
| Edit | `PUT /api/v1/interactions/{id}` | `UpdateInteractionRequest` → `Interaction` |
| Delete | `DELETE /api/v1/interactions/{id}` | `204`, soft-delete in PostgreSQL |

The Angular `InteractionType` union is exactly `CALL | EMAIL | NOTE | MEETING`,
matching the Spring DTO Bean Validation pattern and the PostgreSQL check
constraint. JPA entities are never returned as JSON.

### Auth and correlation

- `AuthSessionService` holds the demo token in an Angular signal **in memory
  only**. A full browser reload intentionally signs the user out.
- `authInterceptor` is the one place that formats `Authorization: Bearer …`;
  it attaches the token only to URLs beginning with `environment.apiBaseUrl`
  and avoids a doubled `Bearer ` prefix.
- `correlationInterceptor` adds `X-Correlation-ID: lab-request-001` to API
  calls unless the caller already set one.
- `unauthorizedInterceptor` clears the session and routes to `/login` on 401.
- This is still a demo bearer scheme, not production JWT validation. Lab 51
  replaces the static token and supplies the real actor for events.

### Persistence design

- `V1__crm_schema.sql`: `customer` and `customer_interaction`, seeded with
  Amina (`CUS-1001`, `ACTIVE`) and Ravi (`CUS-1002`, `PROSPECT`).
- `V2__processed_event.sql`: Lab 49 consumer idempotency.
- `V3__interaction_soft_delete.sql`: nullable `deleted_at` on
  `customer_interaction`.
- Hibernate restricts normal interaction reads to `deleted_at is null`.
  `DELETE` therefore hides a record from timeline/detail/update while keeping
  its PostgreSQL audit row.
- `findByCustomer_PublicIdOrderByCreatedAtDesc` uses the V1
  `(customer_id, created_at DESC)` index for timelines.

## Run it

Prerequisites: Docker, JDK 21, Maven, Node LTS. From the repo root:

```bash
cp .env.example .env       # local only; never commit .env
docker compose up -d       # PostgreSQL 5432 + Kafka 9092
```

Backend, in one terminal:

```bash
cd backend
mvn spring-boot:run        # http://localhost:8080
```

Frontend, in a second terminal:

```bash
cd frontend
npm ci
npx ng serve               # http://localhost:4200
```

Open <http://localhost:4200>, choose **Mock login**, and the Amina timeline
loads. `environment.apiBaseUrl` points at `http://localhost:8080`; Spring's
CORS configuration allows `http://localhost:4200` for GET, POST, PUT, and
DELETE with `Authorization`, `Content-Type`, and `X-Correlation-ID` headers.

> Evidence-run note: on this workstation an unrelated stack already held 4200,
> 5432, and 8080. The recorded browser run therefore used the existing
> `crm49` PostgreSQL/Kafka containers, backend port 18080, and a temporary
> Angular dev-server proxy on 4201. Those overrides were local only and were
> restored before commit; no application code depended on them.

## Browser CRUD script

Use `CUS-1001` throughout. Independent API/SQL checks follow so a UI toast is
not the only evidence.

1. Visit `/`; observe the guarded redirect to `/login`.
2. Click `[data-testid="mock-login"]`; observe Amina's timeline.
3. Click `[data-testid="new-interaction"]`.
4. On the create form:
   - Customer ID: `CUS-1001`
   - Type: `CALL`
   - Summary: `Lab 50 browser CRUD check`
   - Click `[data-testid="create"]`.
5. Observe navigation to the created interaction's detail page.
6. Change the type to `EMAIL` and summary to `Lab 50 browser CRUD updated`;
   click `[data-testid="save-interaction"]` and observe “Changes saved.”
7. Return to the timeline; the first row must show the updated type/summary.
8. Run the SQL proof below before deleting.
9. Click that row's `[data-testid="delete-interaction"]`; it disappears from
   the timeline.
10. Search for `CUS-9999`; observe the alert `Unknown customer: CUS-9999`.
11. On the create form, enter a whitespace-only summary and save; observe
    `Validation failed: summary: must not be blank`.
12. Log out; observe the login page. Trying an interactions URL while logged
    out redirects there too.

### SQL proof

After step 7, this query returned `EMAIL | Lab 50 browser CRUD updated` with a
null `deleted_at`:

```sql
SELECT interaction_type, summary, deleted_at
FROM customer_interaction
WHERE summary LIKE 'Lab 50 browser CRUD%';
```

After step 9, the same row still existed but `deleted_at IS NOT NULL` was true.
That is the persistence proof for soft-delete: hidden from the API/UI, retained
for audit.

### API smoke (optional cross-check)

```bash
TOKEN=<demo token documented in the root README>
curl -fsS -H "Authorization: Bearer $TOKEN" \
  -H 'X-Correlation-ID: lab-request-001' \
  'http://localhost:8080/api/v1/interactions?customerId=CUS-1001'
```

Without the bearer header, expect 401; with `CUS-9999`, expect a 404 Problem
Detail; invalid type or blank summary returns 400 Problem Details with an
`errors` list.

## Observed live result

The browser run on 2026-10-20 created interaction
`842f7f81-a67e-4931-b33c-d2123b123788`, changed it from `CALL` to `EMAIL`,
showed the updated row at the head of Amina's timeline, matched it with a
direct PostgreSQL query, soft-deleted it from the list, and verified that the
database row remained with `deleted_at` set. `CUS-9999`, blank-summary
validation, guarded redirect, and logout were also exercised in the same run.
No screenshot was taken or committed.

## Selenium scenario notes

These are ready-to-automate outlines. The selectors already exist in the UI.
Use one browser session per scenario because the demo token is memory-only;
every guarded scenario begins with mock login. Prefer waiting for a route,
row, or alert over fixed sleeps.

### Stable selectors

| Purpose | Selector |
| --- | --- |
| Mock login | `[data-testid="mock-login"]` |
| Customer search field | `[data-testid="customer-id"]` |
| Load timeline | `[data-testid="load-interactions"]` |
| Amina / Ravi shortcuts | `[data-testid="customer-amina"]`, `[data-testid="customer-ravi"]` |
| New interaction | `[data-testid="new-interaction"]` |
| Create fields | `create-customer-id`, `create-type`, `create-summary` |
| Save create | `[data-testid="create"]` |
| Timeline rows | `[data-testid="interaction-row"]` |
| Row detail / delete | `interaction-detail`, `delete-interaction` inside the row |
| Edit fields | `edit-type`, `edit-summary` |
| Save edit | `[data-testid="save-interaction"]` |
| Errors | `list-error`, `create-error`, `detail-error` |
| Saved confirmation | `[data-testid="saved-message"]` |

### Scenario A — highest value: full CRUD for Amina

1. Open `/` → assert `/login`.
2. Click mock login → assert `/interactions` and at least the loaded timeline
   state (rows or the empty-state notice).
3. Create a unique summary such as `Lab 50 Selenium <timestamp>`.
4. Assert the browser lands on `/interactions/{uuid}` and the detail shows
   `CUS-1001` plus `lab-request-001`.
5. Edit the summary, save, assert `[data-testid="saved-message"]`.
6. Return to the list and find the row by its unique summary.
7. Outside Selenium, run the SQL proof for that summary.
8. Delete the row, assert it disappears, then confirm `deleted_at` in SQL.

This catches the highest-risk chain in one pass: guard, interceptor, CORS,
DTO mapping, transaction/persistence, timeline refresh, and soft-delete.

### Scenario B — validation and not-found errors

1. Log in and search `CUS-9999`; assert `Unknown customer: CUS-9999` in
   `[data-testid="list-error"]`.
2. Open create, enter only spaces as the summary, save, and assert
   `Validation failed: summary: must not be blank`.
3. Assert the browser remains on the create route and no new row appears.

### Scenario C — auth lifecycle

1. Open `/interactions` in a fresh session; assert redirect to `/login`.
2. Log in, confirm the timeline API call succeeds, log out, and reopen
   `/interactions`; assert the login page again.
3. A future JWT-expired case belongs with Lab 51: seed an expired token,
   trigger a call, and assert the same redirect through
   `unauthorizedInterceptor`.

### Scenario D — Ravi filter and timeline scoping

1. Log in and click `[data-testid="customer-ravi"]`.
2. Assert rows belong to the Ravi timeline, then create a unique Ravi note.
3. Switch back to Amina and assert the Ravi summary is absent.

## Failure experiments

| # | Experiment | Evidence / disposition |
| - | ---------- | ------------------------ |
| 1 | No token / interceptor | Not re-run by deleting production code. The interceptor unit tests prove no header is added before login, `InteractionApiIT` proves the backend returns 401 without a bearer, and the live run proved guarded redirect plus logout. Restore is trivial because no code was broken. |
| 2 | Break Flyway checksum | Not run destructively. Editing an already-applied migration is intentionally forbidden in this repo; the policy is to restore the original bytes or add a new migration, never rewrite applied history. The empty-Testcontainers-database ITs prove V1→V3 applies cleanly. |
| 3 | Wrong `apiBaseUrl` / blocked ports | Encountered the local equivalent during evidence capture: 4200 was occupied by an unrelated app and a temporary 4201 proxy initially drew Spring CORS 403s until its forwarded `Origin` was set to the allowed 4200 origin. The committed configuration remains API `8080` + Angular `4200`; temporary overrides were restored. |

## Automated coverage

### Backend — 36 tests

- Service and MockMvc coverage for create/list/detail/update/soft-delete,
  unknown interaction 404s, Problem Details, and no-event-on-update/delete.
- `InteractionApiIT` drives update + soft-delete through the real HTTP and
  database stack and still proves the Lab 49 Kafka event on create.
- `InteractionPersistenceIT` proves the Flyway seeds, per-customer timeline
  ordering, and that a soft-deleted interaction is hidden from `findById`
  and the timeline.

### Frontend — 11 Karma tests

- Interceptors attach bearer/correlation only to API requests, and never leak
  them to an external URL.
- `InteractionApiService` issues the expected GET/POST/PUT/DELETE calls.
- Page tests cover timeline loading, create navigation, and detail
  update/delete with a fake API.
- `apiErrorMessage` renders Problem Details and gives plain-language network
  and 401 messages.

## Known limitations

- The token is a Lab 49/50 demo bearer, not a validated JWT with expiry,
  refresh, roles, or a real actor. Lab 51 owns that hardening.
- Update and soft-delete do not publish new Kafka event types. The UI rereads
  the REST API immediately, so it is consistent for one user; cross-client
  live updates from Kafka are conceptual only in this lab.
- There is no customers REST resource. The UI knows the two seeded fixtures
  and accepts free-form public IDs, which is what makes the `CUS-9999` error
  path demonstrable.
- A page reload clears the in-memory token by design. That is safer than the
  demo token in `localStorage`, but a production build would use the Lab 51
  auth flow instead.
- The XSS probe on the login page is starter security evidence: Angular
  interpolation renders it as text. It is synthetic and not user data.

## Reflection

1. **DTO alignment is enforced at three layers:** the TypeScript union, Spring
   Bean Validation, and the PostgreSQL check constraint all name the same four
   interaction types, while shared field names and Problem Details keep error
   handling symmetric.
2. **Persistence is proven outside the UI:** the browser-created ID and edited
   summary were read directly from PostgreSQL, then delete was proven by both
   UI absence and a non-null `deleted_at`.
3. **The highest-value Selenium scenario is full CRUD in one session.** It is
   the only outline that would catch a broken guard, interceptor, CORS rule,
   DTO mismatch, or soft-delete filter before the Lab 52 defense.

