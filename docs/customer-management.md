# Customer management — what was built and why

Branch: `feature/customer-crud` (off `will_work`). Authored 2026-10-20, during Lab 51 week,
ahead of the Lab 52 defense.

This document explains the customer slice end to end: why it exists, what was built, the
decisions made along the way, how it was verified, and what it deliberately does not do.
It is written for two readers — a teammate picking the code up cold, and a defense
reviewer tracing a claim back to evidence.

## 1. Why this work exists

The capstone sources do not all describe the same scope, and this slice exists because of
that disagreement. Reading them in the wrong order produces the wrong product.

### What the capstone-level sources require

- **Week 6 master document** (`WEEK-LABS-INDEX.md`). It calls itself "the capstone master
  document" and says lab guides are day-to-day working instructions while the master
  document governs **scope**. Its functional scope table requires: "Customer management —
  Create, update, search, and delete customers."
- **Capstone brief** (`CAPSTONE-BRIEF-AND-RUBRIC.md` §2.3). Same requirement in lifecycle
  language: "Create, update, search, and managed customer lifecycle." Section 2.2 names
  the Ravi Singh journey explicitly: `PROSPECT → ACTIVE`, onboarding/status change.
- **Word project brief** (`Java_Angular_Fullstack_Capstone.docx`). The final demonstration
  scenario opens with an agent searching for `CUS-1001`, opening the profile, and only
  then recording an interaction.
- **Module 50 slides and pre-lab exercises.** Exercise 1 specifies the customer routes
  (`/customers`, `/customers/:id`, `/customers/new`, `/customers/:id/edit`). Exercise 2
  specifies the customer API (`GET` list, `GET` by id, `POST` → 201 + `Location`, `PUT`,
  `DELETE` → 204). Exercise 5 traces a customer update through `CustomerFormComponent →
  PUT /api/customers/CUS-1001 → CustomerController → CustomerService →
  CustomerRepository.save()`. The slides teach `CustomerSearch`, `CustomerProfile`, and a
  `CustomersService` with search and get-by-id.

### What the lab guides and starters actually assign

The day-to-day lab path builds exactly one functional slice: CAP-12, record a customer
interaction. The Lab 50 starter this repo began from contains a `customer` table, a JPA
entity, and a repository method (`findByPublicId`) — but only so the interaction flow can
reject an unknown customer with 404. No customer controller, service, or screen is ever
assigned step by step. Module 49's slice-selection exercise treats "customer create/read"
as one candidate slice among several and steers the lab toward interaction recording
instead. That is sequencing advice for a 45-minute lab block. It is not a scope deletion.

### Conclusion

The labs are the floor for process and evidence. The brief, master document, and rubric
are the scope for the product. A team that follows only the starters ships a product with
a hole in it, and an evaluator anchored to the brief finds that hole at the defense — in
the demo's very first beat (search → profile), which the starter-based app cannot perform
at all. The affected rubric categories are Backend and Messaging (20 points) and Frontend
and Persistence (15 points).

So the customer slice was built to brief scope, not starter scope. The rest of this
document describes that build.

## 2. Scope

### Built

- Full customer CRUD over REST: create, search/list, profile read, update (including
  lifecycle status change), delete.
- Angular journey: search → profile → interactions, plus create and edit screens.
- Automated tests at every layer: service unit, HTTP contract, PostgreSQL integration,
  Angular service, Angular components.

### Deliberately not built

- **Real JWT/RBAC** — *since built* (see [docs/lab51/security-and-observability.md](lab51/security-and-observability.md)):
  customer reads/creates/updates need AGENT; closing a customer (`DELETE`) is ADMIN-only.
- **Kafka events for customer changes** — *since built*: `CustomerStatusChangedV1` on
  `crm.customer.lifecycle.v1`, published after commit when status changes and audited by
  a deduping consumer with a DLT (see the Lab 51 doc).
- **Email field.** Module 50 Exercise 2 sketches a customer DTO with an `email` field.
  The database schema this repo shipped in Lab 49–50 has no email column, and the seeded
  fixtures have no emails. Adding a column now would mean a fourth migration to satisfy
  an exercise sketch that contradicts the frozen schema. The DTO matches the schema
  instead: name, status, created-at.
- **Optimistic locking / 409 Conflict.** Exercise 5 mentions a stale-version 409 path.
  Neither the `customer` nor the `customer_interaction` table carries a version column,
  so the capstone schema never actually adopted that pattern. Concurrent edits are
  last-write-wins, same as interactions. Also recorded in §8.
- **Pagination.** Search returns the full match list ordered by name. The data set is two
  seeded customers plus whatever a demo creates; page/size parameters would be ceremony
  without a driver. The repository method is the natural place to add a `Pageable` later.

## 3. API contract

Base path: `/api/v1/customers`. All endpoints require the same authorization as
`/api/v1/interactions` and accept the same `X-Correlation-ID` header.

| Method | Path | Success | Purpose |
| --- | --- | --- | --- |
| GET | `/api/v1/customers?q={term}` | 200, array | Search by name or public ID; `q` optional |
| POST | `/api/v1/customers` | 201 + `Location` | Create; server generates the `CUS-####` ID |
| GET | `/api/v1/customers/{customerId}` | 200 | Profile read |
| PUT | `/api/v1/customers/{customerId}` | 200 | Update name and/or status |
| DELETE | `/api/v1/customers/{customerId}` | 204 | Close the customer (see §4.4) |

Request and response shapes:

```json
POST /api/v1/customers
{ "fullName": "Priya Nair", "status": "PROSPECT" }

201 Created
Location: /api/v1/customers/CUS-1003
{
  "customerId": "CUS-1003",
  "fullName": "Priya Nair",
  "status": "PROSPECT",
  "createdAt": "2026-10-20T16:40:00Z"
}
```

```json
PUT /api/v1/customers/CUS-1002
{ "fullName": "Ravi Singh", "status": "ACTIVE" }
```

Error contract is the one the interaction API already established — RFC 9457 Problem
Details (`application/problem+json`), produced by the shared `ApiExceptionHandler`:

- **400** on validation failure, with the per-field `errors` list (e.g. `status: status
  must be PROSPECT, ACTIVE, or CLOSED`).
- **404** with detail `Unknown customer: CUS-9999` for any unknown public ID, on read,
  update, and delete alike.
- **401** when the bearer [REDACTED] is missing or wrong.

Field rules: `fullName` is required, non-blank, max 200 characters (the column width).
`status` must be one of `PROSPECT`, `ACTIVE`, `CLOSED` — the same set the
`ck_customer_status` database constraint enforces, so API validation and storage can
never disagree. One asymmetry is intentional: create accepts only `PROSPECT` or
`ACTIVE`. A customer is never born closed; `CLOSED` is reached through update or delete.

## 4. Backend design

The slice follows the layering Lab 49 established, package for package, so a reviewer who
knows the interaction code already knows where everything below lives:

```text
CustomerController            api/            HTTP only: status codes, Location header
  CreateCustomerRequest       api/dto/        validated input records
  UpdateCustomerRequest       api/dto/
  CustomerResponse            api/dto/        output record; entity never leaks to JSON
CustomerService               service/        business rules, transaction boundaries
CustomerRepository            repo/           Spring Data JPA derived queries
CustomerEntity                domain/         JPA mapping for the existing customer table
```

Four decisions in this layer deserve explanation.

### 4.1 No new migration

The Lab 49/50 schema (`V1__crm_schema.sql`) already contains everything customer CRUD
needs: the `customer` table, the `PROSPECT/ACTIVE/CLOSED` status constraint, the unique
`public_id`, and both seeded fixtures. Adding a server-managed lifecycle on top of an
existing `CLOSED` value is application logic, not schema change. So this slice ships code
only — Flyway history is untouched, and environments that already applied V1–V3 need no
action. `CustomerEntity` gained setters for `publicId`, `fullName`, and `status`, plus
`createdAt` accessors; it was previously read-only because nothing had ever written a
customer.

### 4.2 Public ID generation

Customers are addressed everywhere — URLs, Kafka keys, demo scripts — by public ID
(`CUS-1001`), never by database key. The seeds establish the pattern: row 1 is
`CUS-1001`, row 2 is `CUS-1002`. Create continues it: the public ID is derived from the
generated database key as `CUS-(1000 + id)`. Deriving instead of counting or scanning for
a maximum keeps the value unique by construction and immune to races between two agents
creating customers at once.

There is one wrinkle, and the code comments call it out in numbered steps. `public_id` is
`NOT NULL`, but the database key only exists after the first insert. So create (1) builds
the entity from the validated request, (2) saves it with a short-lived unique placeholder
in `public_id`, then (3) overwrites the placeholder with the derived `CUS-####` value and
saves again. Both saves happen inside one transaction, so no reader can ever observe the
placeholder. The placeholder is `PENDING-` plus 22 hex characters — 30 characters total,
chosen to fit the `VARCHAR(32)` column. That constraint is not theoretical: the first
version used a full UUID (44 characters) and the PostgreSQL integration test caught the
overflow on its first run (see §6.2).

### 4.3 Search semantics

`GET /api/v1/customers?q=` takes one free-text term and matches it, case-insensitively,
against both the public ID and the full name (`findByPublicIdContainingIgnoreCaseOr…`
in `CustomerRepository`). One box accepts "amina", "CUS-100", or "singh" — which is how
an agent actually works a phone queue, sometimes with a name and sometimes with a
half-remembered ID. An absent or blank `q` returns every customer. Results are ordered
by name in the query itself, so ordering is deterministic for tests and demos instead of
depending on insert order.

### 4.4 Delete means close

`DELETE` sets `status = CLOSED` and keeps the row. Three reasons:

1. **Referential integrity.** `customer_interaction.customer_id` is a foreign key. A hard
   delete of any customer with interaction history would either fail on the constraint or
   cascade-erase the audit trail. The Module 50 slides warn about exactly this cascade.
2. **Consistency.** Interaction delete already works this way — a `deleted_at` timestamp
   hides the row while PostgreSQL retains it for audit (Lab 50, Flyway V3). Customer
   close is the same rule at the aggregate level: business history is never destroyed by
   a UI action.
3. **The brief's own wording.** The master document's scope table says "delete"; the
   brief (§2.3) says "managed customer lifecycle," and the schema ships a `CLOSED`
   status that nothing previously used. Close-on-delete satisfies both readings: the HTTP
   verb and the 204 contract from Exercise 2 are honored, and the lifecycle model stays
   intact. A closed customer remains readable by ID (its profile shows `CLOSED`), which
   is what an agent needs when a former customer calls back.

In the service, every read, update, and delete begins by loading the customer through one
private `findCustomer` that throws the existing `UnknownCustomerException`. That exception
was already mapped to a 404 Problem Detail for the interaction API, so customer endpoints
inherited the exact same error shape with no new handler code. Updates run in a
transaction that opens at the service method and commits on return — the boundary
Exercise 5 asks students to be able to name.

## 5. Frontend design

New feature folder: `frontend/src/app/features/customers/`, mirroring the interactions
feature file for file:

```text
customer.model.ts              Customer, CustomerStatus, request types
customer-api.service.ts        typed HttpClient gateway (components never build URLs)
customer-list-page.component.ts    /customers            search + results
customer-detail-page.component.ts  /customers/:id        profile
customer-form-page.component.ts    /customers/new        create
                                   /customers/:id/edit   edit
customer-api.service.spec.ts   \
customer-pages.spec.ts         /   see §6
```

The four routes are the four routes from Module 50 Exercise 1, all behind the existing
`authGuard`, all lazy-loaded like the interaction routes. Components follow the
established house style exactly: standalone, signal-based state (`rows`, `error`,
`loading`, `saving`), `FormsModule` template forms, `data-testid` attributes for UI
verification, and every error routed through the shared `apiErrorMessage` so users see
the API's Problem Detail text rather than a raw status code. Authentication and
correlation headers come from the existing interceptors; no customer code touches them.

Three choices are worth noting:

- **The profile is the hub.** The detail page shows name, ID, status, and
  customer-since, then hands off: *View interactions* and *Record an interaction* link
  into the existing interaction pages with the customer ID as a query parameter (both
  interaction pages already read that parameter), and *Edit customer* goes to the form.
  This makes the defense demo a single click path — search, profile, timeline, record —
  instead of the previous flow, where the agent had to type a customer ID from memory
  into the interactions screen. The profile deliberately does not embed the timeline;
  the interaction list page already owns that job, and duplicating it would create two
  timeline implementations to keep in sync.
- **One form component, two routes.** Create and edit share `CustomerFormPageComponent`;
  the presence of the `:id` route parameter is the only difference. Exercise 1's own
  discussion question asks what happens when create and edit are separate components and
  the form gains a field — the answer is drift. The shared form also guarantees the
  status options stay honest: create offers `PROSPECT`/`ACTIVE`, edit adds `CLOSED`,
  matching the API's asymmetric validation (§3).
- **The app now lands on Customers.** The root and wildcard redirects in
  `app.routes.ts` and the post-login navigation in the login page previously went to
  `/interactions`. They now go to `/customers`, because customer search is the start of
  the agent journey in every capstone-level source. The header nav gained *Customers*
  and *New customer* links alongside the existing interaction links; nothing was removed,
  and the interaction list page still accepts a typed customer ID directly, so the old
  workflow remains available as a fallback during the demo.

## 6. Verification

### 6.1 Test inventory

25 new backend tests and 8 new frontend tests, each mirroring the test class the
interaction slice already had at that layer:

| Layer | Test class | Tests | What it proves |
| --- | --- | ---: | --- |
| Service unit | `CustomerServiceTest` | 9 | ID generation, search term handling, status transition, close-keeps-row, unknown ID never saves |
| HTTP contract | `CustomerControllerTest` | 9 | 201 + `Location`, 400 Problem Details, 404 Problem Detail, 401 without bearer [REDACTED] |
| Full-stack IT | `CustomerApiIT` | 7 | Real rows in PostgreSQL: create persists, Ravi `PROSPECT → ACTIVE` in the database, close keeps the row, invalid create persists nothing |
| Angular service | `customer-api.service.spec.ts` | 3 | Correct URLs, verbs, `q` parameter, typed bodies |
| Angular pages | `customer-pages.spec.ts` | 5 | Search renders, profile loads and closes, form creates vs. updates and routes correctly |

Full-suite results on 2026-10-20 (JDK 21, Testcontainers PostgreSQL 16 + Kafka):

- `mvn -B clean verify` — **61 tests, 0 failures** (the 36 pre-existing interaction and
  messaging tests still pass unchanged).
- `npx ng build` — production build succeeds; each customer page is its own lazy chunk.
- `npx ng test --watch=false --browsers=ChromeHeadless` — **21 tests, all SUCCESS**.
- Not yet done: a manual browser click-through against the live Compose stack. The
  headless suite and the API ITs cover the behavior; a five-minute manual run (§7) is
  still worth doing before the defense, because the panel watches a browser, not a test
  report.

The integration test cleans up after itself. Because the Testcontainers instance is
shared across IT classes, `CustomerApiIT` restores the seeds after every test — created
customers are removed, Amina returns to `ACTIVE`, Ravi returns to `PROSPECT` — so test
order can never leak a closed or activated fixture into the interaction ITs.

### 6.2 The bug the tests caught

The first full `mvn verify` run failed exactly one test:
`CustomerApiIT.create_persistsRowWithGeneratedPublicId` with
`value too long for type character varying(32)`. The unit and controller tests had all
passed, because mocked repositories never enforce column widths. Only the real-PostgreSQL
test could see that the 44-character placeholder ID (§4.2) did not fit `VARCHAR(32)`.
The fix shortened the placeholder to 30 characters; the re-run was fully green. This is
the argument for keeping at least one test per slice at the real-database layer: schema
constraints are behavior, and mocks cannot testify about them.

## 7. Running it and the demo path

Same commands as the rest of the capstone (see the root README). With the backend on
:8080 and the frontend on :4200, sign in as `agent` (or `admin`, needed to close a
customer); local-profile passwords are in `backend/src/main/resources/application-local.yml`.
The app lands on Customers.

A five-minute manual verification — and the natural Lab 52 demo order:

1. Search `amina`. One result. Open the profile: Amina Khan, `CUS-1001`, `ACTIVE`.
2. Follow *Record an interaction*. The customer ID is already filled in. Save a note,
   then *View interactions* from the profile to see it in the timeline. (This is the
   CAP-12 slice working through the new journey.)
3. Create a customer: *New customer*, `Priya Nair`, `PROSPECT`. The app routes to her
   new profile with a generated `CUS-####` ID.
4. Do the lifecycle beat: open Ravi Singh (`CUS-1002`, `PROSPECT`), *Edit customer*,
   set status `ACTIVE`, save. Profile now shows `ACTIVE` — the onboarding journey from
   brief §2.2.
5. Negative paths: open the profile URL `/customers/CUS-9999` directly for the Problem
   Detail message; try creating a customer with a blank name for the field-level
   validation error.
6. Close Priya from her profile. The app returns to the Customers list, where she now
   shows status `CLOSED`, and her profile URL still resolves — the audit-preserving
   delete from §4.4.

API-only spot checks (correlation header matches the rest of the capstone):

```bash
curl -fsS -H "Authorization: Bearer [REDACTED]" \
  "http://localhost:8080/api/v1/customers?q=amina"
curl -fsS -i -X POST -H "Authorization: Bearer [REDACTED]" \
  -H "Content-Type: application/json" -H "X-Correlation-ID: lab-request-001" \
  -d '{"fullName":"Priya Nair","status":"PROSPECT"}' \
  "http://localhost:8080/api/v1/customers"
```

## 8. Residual risks and open scope

Honest gaps, in the capstone's own "evidence over assertion" spirit:

- ~~Authentication is still the demo token.~~ Resolved: JWT + AGENT/ADMIN roles; close is ADMIN-only.
- ~~No customer lifecycle event on Kafka.~~ Resolved: `CustomerStatusChangedV1`.
- **Concurrent edits are last-write-wins.** No version column, no 409. Acceptable for a
  two-fixture training system; wrong for production. The migration that adds `version`
  columns to both tables is the fix, and it should be done for interactions at the same
  time so the two aggregates behave identically.
- **Search has no pagination and no minimum term length.** Fine at demo scale; the
  repository is the single place to add `Pageable` when a real data volume appears.
- **Committed locally** on `feature/customer-crud` (not pushed).
### Likely defense questions, with pointers

- *"Why does delete not delete?"* — §4.4: foreign keys, audit preservation, brief
  lifecycle wording. The interaction soft-delete in Flyway V3 is the precedent.
- *"Where is the transaction boundary on update?"* — opens at
  `CustomerService.update`, commits on normal return; proof is the IT asserting the new
  status through the repository after the HTTP call, not the response body.
- *"How do you know the UI really wrote to PostgreSQL?"* — `CustomerApiIT` reads the row
  back through JPA after the REST call, and the §7 manual path can be paired with a
  `SELECT public_id, full_name, status FROM customer;` during the demo.
- *"Why is there no new migration?"* — §4.1: the schema shipped in Lab 49 already had
  the table and constraints; this slice is code over an existing contract.
- *"What happens on CUS-9999?"* — 404 Problem Detail on every verb; the unit tests also
  assert that an unknown-ID update or delete never calls `save`.

## 9. File inventory

New files:

```text
backend/src/main/java/com/northstar/crm/api/CustomerController.java
backend/src/main/java/com/northstar/crm/api/dto/CreateCustomerRequest.java
backend/src/main/java/com/northstar/crm/api/dto/UpdateCustomerRequest.java
backend/src/main/java/com/northstar/crm/api/dto/CustomerResponse.java
backend/src/main/java/com/northstar/crm/service/CustomerService.java
backend/src/test/java/com/northstar/crm/api/CustomerControllerTest.java
backend/src/test/java/com/northstar/crm/api/CustomerApiIT.java
backend/src/test/java/com/northstar/crm/service/CustomerServiceTest.java
frontend/src/app/features/customers/customer.model.ts
frontend/src/app/features/customers/customer-api.service.ts
frontend/src/app/features/customers/customer-list-page.component.ts
frontend/src/app/features/customers/customer-detail-page.component.ts
frontend/src/app/features/customers/customer-form-page.component.ts
frontend/src/app/features/customers/customer-api.service.spec.ts
frontend/src/app/features/customers/customer-pages.spec.ts
docs/customer-management.md   (this document)
```

Modified files:

```text
backend/src/main/java/com/northstar/crm/domain/CustomerEntity.java   setters, createdAt
backend/src/main/java/com/northstar/crm/repo/CustomerRepository.java list + search queries
frontend/src/app/app.routes.ts                                       customer routes, landing
frontend/src/app/app.component.html                                  nav links
frontend/src/app/features/login/login-page.component.ts              post-login landing
README.md                                                            docs index link
```

No changes to: interaction backend or frontend behavior, Kafka contracts, Flyway
migrations, CI workflows, Docker or k3s configuration. The existing pipelines pick the
new tests up automatically because they run the same `mvn verify` and Angular test
suites.
