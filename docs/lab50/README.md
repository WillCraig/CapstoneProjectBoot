The Lab 50 code already lives in this repo under `frontend/` and `backend/`. Nothing to copy; work on a branch off `main`.

# Lab 50 — Angular + PostgreSQL slice (~45 minutes)

**Theme:** Angular feature + bearer interceptor + PostgreSQL persistence smoke

## Activity card

| | |
| --- | --- |
| **Checkpoint** | **E** |
| **Must prove** | Route + service TODO · interceptor · migration TODO · one CRUD smoke |
| **Hard gate** | Pre-lab Pass · Lab 48/49 inputs · Node + PostgreSQL |

## 45-minute checklist

- [x] Fill `auth.interceptor.ts` bearer handling
- [x] Fill `InteractionApiService` HttpClient TODOs
- [x] Fill `InteractionService` persist/list TODOs (Lab 49)
- [x] Confirm Flyway `V1__crm_schema.sql` (PostgreSQL, not Oracle)
- [x] Smoke create/list for `CUS-1001`
- [x] Complete the [`frontend-persistence-demo.md`](frontend-persistence-demo.md) runbook

Full path also completed: lazy list/create/detail routes, backend
detail/update endpoints, Flyway V3 soft-delete, browser CRUD evidence, and
Selenium scenario notes in the demo runbook.

## Smoke test

```bash
cp .env.example .env
docker compose up -d
cd backend && mvn -B clean verify
cd ../frontend && npm ci && npx ng build
CHROME_BIN='/Applications/Google Chrome.app/Contents/MacOS/Google Chrome' \
  npx ng test --watch=false --browsers=ChromeHeadless
```

`ng` is not required on PATH — use `npx ng`. Backend ITs use Testcontainers,
so Docker (Docker Desktop on the Mac) must be running; no extra variables are needed. Angular UI is Lab 50; the k3s deploy is Lab 51.

## Timed-path Pass criteria

| Criterion | Pass / Fail |
| --------- | ----------- |
| Angular route reaches interactions UI | Pass / Fail |
| HttpClient call authenticated via interceptor | Pass / Fail |
| PostgreSQL row exists after create (or documented seed+read) | Pass / Fail |
| Demo doc outline started | Pass / Fail |

Continue remaining GUIDE steps as homework / multi-day.
