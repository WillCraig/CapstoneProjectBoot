# ADR 006: JWT authentication with AGENT and ADMIN roles

**Status:** Accepted (built in the Lab 51 security work).
**Owner:** Will (deployment/security). **Team to confirm** whether someone else should own security.

## Context

The brief asks for JWT login and role-based access with AGENT and ADMIN. It also says Angular is not the security boundary. No external identity provider was given to us, so we needed something we could run on a laptop and in CI.

## Decision

- **The API issues its own token.** `POST /api/v1/auth/login` checks one of two synthetic accounts, `agent` (AGENT) or `admin` (AGENT + ADMIN). Passwords are stored as BCrypt hashes, and an account with no configured password is disabled. On success it returns an **HS256-signed JWT** that lasts **15 minutes** (`crm.security.jwt.ttl`), with issuer `northstar-crm`, a subject, an id and a `roles` claim.
- **Spring Security checks every request.** It runs as a stateless OAuth2 resource server and checks the signature, the expiry and the issuer. Rules, in order:
  - `OPTIONS`, `POST /api/v1/auth/login`, `/actuator/health/**` and `/error` are public.
  - The rest of `/actuator/**` needs ADMIN.
  - `DELETE /api/v1/customers/**` needs ADMIN.
  - The rest of `/api/**` needs AGENT.
  - Everything else is denied.
- **A business rule in the service layer.** `CustomerService.update` refuses a status change into or out of `CLOSED` unless the caller has ADMIN, so an agent can't use `PUT` to get around the `DELETE` rule.
- **No built-in secret.** The app refuses to start without `CRM_JWT_SECRET` (at least 32 bytes). Only the `local` and `test` profiles carry labeled throwaway values. Deployed environments get it from the `crm-secrets` Secret.
- **Angular** keeps the token in memory only (a signal, never `localStorage`). An interceptor adds `Authorization: Bearer` to first-party API calls only. A `401` sends the user to `/login`. Roles only hide buttons, and the server checks again on every call.
- CSRF protection is off, because nothing authenticates with cookies.

## Rejected

- **Server sessions with cookies.** They'd need CSRF protection and sticky state across pods.
- **An external identity provider (e.g. Keycloak).** It's the right long-term answer, but there was nothing provided and no time to run one. `DemoUserDirectory` is the only piece we'd swap out. Controllers only ever see a JWT.
- **Checking roles only in Angular.** The browser is untrusted.
- **Keeping the token in `localStorage`.** Any injected script could read it.
- **AGENT/MANAGER naming** (from an early sample). The brief says ADMIN.

## Consequences

- Proven by `SecurityIT`, which covers no token, a bad or expired token, wrong-role `403`s, admin-only close, and CORS. `CustomerServiceTest` covers the agent close/reopen denial.
- **Accepted gaps** (risk R-09): no refresh token, so users log in again after 15 minutes or after a page refresh. No revocation: logout only clears the browser copy, so a stolen token works until it expires. No login rate limit or lockout.
- Rotating `CRM_JWT_SECRET` logs everyone out, which is fine for a demo.
- An agent can still rename a `CLOSED` customer, as long as the status stays `CLOSED`. That's recorded as a known gap (R-17).
