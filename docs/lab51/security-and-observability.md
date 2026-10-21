# Lab 51 — security and observability

What the rubric asks for (brief §2.3 / §4 / §8.1): *"JWT authentication and role-based
authorization (AGENT/ADMIN); Angular is not the security boundary"* and *"health checks,
logging/metrics"*. This page is the claim → proof map for both.

## Authentication

- `POST /api/v1/auth/login` with `{"username","password"}` returns
  `{accessToken, tokenType:"Bearer", expiresIn, username, roles}`.
- The token is an **HS256 JWT** (`iss=northstar-crm`, `sub=<username>`, `roles=[...]`,
  `jti`, 15-minute `exp`). Spring Security's `NimbusJwtDecoder` verifies signature, issuer,
  and expiry on every request. Sessions are stateless; there is no cookie, so CSRF does
  not apply.
- Demo users are synthetic: `agent` (AGENT) and `admin` (AGENT + ADMIN). Passwords are only
  held as BCrypt hashes in memory; an account with no configured password is disabled.
  Unknown usernames still run one BCrypt check so response timing does not reveal which
  usernames exist. Wrong user and wrong password return the same 401 message.
- Code: `backend/.../security/` (`SecurityConfig`, `TokenService`, `DemoUserDirectory`,
  `ProblemDetailAuthHandlers`, `CurrentActor`) and `api/AuthController`.
- Trade-off we accept: no refresh token and no revocation list. The short TTL bounds the
  damage of a stolen token. A real IdP (OIDC) would replace `AuthController` and
  `DemoUserDirectory` without touching controllers, because they only ever see a JWT.

### Configuration (names only)

| Env var | Where the value lives | Notes |
| --- | --- | --- |
| `CRM_JWT_SECRET` | k3s Secret `crm-secrets/jwt-secret` (from GitHub Environment secret) | >= 32 bytes; app refuses to start without it |
| `CRM_AGENT_PASSWORD` | `crm-secrets/agent-password` | BCrypt hash (`$2a$...`) recommended; plain values are hashed at startup |
| `CRM_ADMIN_PASSWORD` | `crm-secrets/admin-password` | same |
| `CRM_JWT_TTL` | optional | default `15m` |
| `CRM_CORS_ALLOWED_ORIGINS` | optional | default `http://localhost:4200,…:4201`; k3s serves UI and API from one origin |

`SPRING_PROFILES_ACTIVE=local` (`application-local.yml`) supplies publicly-known throwaway
values for laptop runs only. Tests use `src/test/resources/application-test.yml`. No real
secret is committed anywhere.

## Authorization — endpoint matrix

Enforced in `SecurityConfig` (the API is the boundary; Angular only hides buttons).

| Method + path | Anonymous | AGENT | ADMIN |
| --- | --- | --- | --- |
| `POST /api/v1/auth/login` | ✅ | ✅ | ✅ |
| `GET /actuator/health`, `/actuator/health/{liveness,readiness}` | ✅ | ✅ | ✅ |
| `GET/POST/PUT /api/v1/customers/**` | 401 | ✅ | ✅ |
| `DELETE /api/v1/customers/{id}` (close) | 401 | **403** | ✅ |
| `GET/POST/PUT/DELETE /api/v1/interactions/**` | 401 | ✅ | ✅ |
| `/actuator/metrics`, other actuator | 401 | 403 | ✅ |
| anything else | 401 | 403 | 403 |

401 and 403 are RFC 9457 Problem Details (`application/problem+json`) with `correlationId`,
and 401 carries `WWW-Authenticate: Bearer`.

Why closing is ADMIN-only: it changes a customer's lifecycle for every agent and emits an
audit event. Normal agent work (search, profile, record interactions) never needs it.

## Negative tests (proof)

`backend/src/test/java/com/northstar/crm/security/SecurityIT.java` (real PostgreSQL + Kafka):

| Case | Expected |
| --- | --- |
| No token | 401 Problem Details, `WWW-Authenticate: Bearer`, correlationId echoed |
| Expired token (signed correctly) | 401 "Bearer token is invalid or expired." |
| Token signed with another key claiming ADMIN | 401; Ravi stays `PROSPECT` |
| Old `lab-demo-token` | 401 |
| AGENT closes CUS-1002 | 403 Problem Details; status unchanged |
| ADMIN closes CUS-1002 | 204; status `CLOSED` |
| Login wrong password / unknown user | 401, same message |
| Login blank fields | 400 |
| `/actuator/metrics` anonymous / AGENT / ADMIN | 401 / 403 / 200 |
| CORS preflight from `localhost:4200` vs `evil.example` | allowed / 403 |

Every existing controller and API test now authenticates with a **real signed JWT** from
`support/TestTokens` (no mocked security context). Angular: `auth-session.spec.ts`
(401 logs out, 403 keeps the session, failed login does not redirect, token not in web
storage) and `login-page.spec.ts`.

Event `actor` is now the JWT subject (`agent` / `admin`) instead of a placeholder, on both
`CustomerInteractionRecordedV1` and `CustomerStatusChangedV1` (asserted in
`InteractionApiIT` and `CustomerLifecycleEventConsumerIT`).

## Angular

- `LoginPageComponent` posts to `/api/v1/auth/login`; `AuthSessionService` keeps the JWT in
  an in-memory signal (not localStorage/sessionStorage). A refresh signs you out; that is
  the price of not leaving a token where injected script can read it.
- `authInterceptor` adds `Authorization: Bearer` to first-party `/api` calls only.
- `unauthorizedInterceptor`: 401 → clear session, go to `/login`; 403 → stay signed in and
  show the Problem Details message.
- `authGuard` and the hidden "Close customer" button are UX only.

## Observability

| Concern | Implementation | Proof |
| --- | --- | --- |
| Liveness | `/actuator/health/liveness` (livenessState only, so a DB outage never restart-loops pods) | `HealthEndpointsIT` |
| Readiness | `/actuator/health/readiness` (readinessState + PostgreSQL) | `HealthEndpointsIT`, smoke script |
| k3s probes | `deploy/k3s/base/backend.yaml`: httpGet startup/liveness/readiness (was tcpSocket) | rendered by `kubectl kustomize deploy/k3s/overlays/dev` |
| Correlation | `CorrelationIdFilter` reads `X-Correlation-Id` (or generates a UUID), puts it in MDC, echoes it on the response; unsafe values (newlines, > 64 chars) are replaced | `CorrelationIdFilterTest`, `HealthEndpointsIT` |
| Logs | every line: `INFO [correlationId=lab-request-001] ...`; Kafka handlers set it from the event | `reports/local-test-run-2026-10-21.md` |
| Errors | all Problem Details (400/401/403/404) carry `correlationId` | `InteractionControllerTest`, `SecurityIT` |
| Metrics | `/actuator/metrics` (ADMIN only) | `SecurityIT` |

Spring Boot 3.3 has no built-in structured JSON console logging (that arrives in 3.4), so
the correlation id is added through `logging.pattern.level`. Logs never contain passwords,
tokens, or interaction summaries.
