The Lab 50 code already lives in this repo under `frontend/` and `backend/`. Nothing to copy; work on a branch off `main`.

# Lab 50 — Angular + PostgreSQL slice (~45 minutes)

**Theme:** Angular feature + JWT interceptor + PostgreSQL persistence smoke

## Activity card

| | |
| --- | --- |
| **Checkpoint** | **E** |
| **Must prove** | Route + service TODO · interceptor · migration TODO · one CRUD smoke |
| **Hard gate** | Pre-lab Pass · Lab 48/49 inputs · Node + PostgreSQL |

## 45-minute checklist

- [ ] Fill `auth.interceptor.ts` Bearer TODO
- [ ] Fill `InteractionApiService` HttpClient TODOs
- [ ] Fill `InteractionService` persist/list TODOs
- [ ] Confirm Flyway `V1__crm_schema.sql` (PostgreSQL, not Oracle)
- [ ] Smoke create/list for `CUS-1001`
- [ ] Start the [`frontend-persistence-demo.md`](frontend-persistence-demo.md) outline

## Smoke test

```bash
cp .env.example .env
docker compose up -d
cd backend && mvn -B test
cd ../frontend && npm ci && npx ng build
```

`ng` is not required on PATH — use `npx ng`. Angular UI is Lab 50; the k3s deploy is Lab 51.

## Timed-path Pass criteria

| Criterion | Pass / Fail |
| --------- | ----------- |
| Angular route reaches interactions UI | Pass / Fail |
| HttpClient call authenticated via interceptor | Pass / Fail |
| PostgreSQL row exists after create (or documented seed+read) | Pass / Fail |
| Demo doc outline started | Pass / Fail |

Continue remaining GUIDE steps as homework / multi-day.
