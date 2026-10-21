# Selenium UI test (e2e)

One automated happy path through the real Angular UI and Spring Boot API:
sign in as `agent` → search **Amina** → open **CUS-1001** → record an interaction
→ see it on her timeline → soft-delete it again (keeps runs repeatable).

It is **not** part of `ng test` or `mvn test` and not in default CI: it needs a
running backend, PostgreSQL, Kafka, and a real Chrome.

## Run it locally

```bash
# 1. Infrastructure (repo root)
docker compose up -d                       # Postgres :5432, Kafka :9092

# 2. Backend with the local profile (dev-only throwaway JWT secret + demo passwords)
cd backend
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run      # API on :8080

# 3. Angular in e2e mode (port 4300, proxies /api -> CRM_E2E_API_TARGET)
cd frontend
npm ci
CRM_E2E_API_TARGET=http://localhost:8080 npm run e2e:serve

# 4. The test (another terminal)
cd frontend
npm run e2e                                 # headless Chrome
CRM_E2E_HEADLESS=false npm run e2e          # watch it drive the browser
```

| Variable | Default | Meaning |
| --- | --- | --- |
| `CRM_E2E_BASE_URL` | `http://localhost:4300` | Where the Angular app is served |
| `CRM_E2E_API_TARGET` | `http://localhost:8080` | Backend the e2e dev server proxies `/api` to |
| `CRM_E2E_USERNAME` | `agent` | Demo AGENT user |
| `CRM_E2E_PASSWORD` | `agent-local-dev` | Matches `application-local.yml` (throwaway, not a secret) |
| `CRM_E2E_HEADLESS` | `true` | `false` opens a visible Chrome window |

Chrome must be installed; Selenium Manager downloads a matching chromedriver.
Selectors are `data-testid` attributes only, so styling changes do not break the test.
