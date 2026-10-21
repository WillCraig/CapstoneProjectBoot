# ADR 006: JWT with AGENT and ADMIN roles

Status: Accepted

## Context

The brief asks for JWT login with AGENT and ADMIN roles, and says Angular is not the security boundary. We weren't given an identity provider, so we needed something that runs on a laptop and in CI.

## Decision

The API issues its own tokens. `POST /api/v1/auth/login` checks one of two demo accounts, `agent` and `admin`, against BCrypt hashes. It returns an HS256 JWT that lasts 15 minutes and carries a `roles` claim. The app won't start without a `CRM_JWT_SECRET` of at least 32 bytes.

Spring Security runs as a stateless resource server and checks every request:

- Login and `/actuator/health` are public.
- `DELETE /api/v1/customers/**` and the rest of `/actuator` need ADMIN.
- The rest of `/api/**` needs AGENT, and anything else is denied.

`CustomerService` also stops an agent from moving a customer into or out of `CLOSED` through `PUT`. Angular keeps the token in memory, not `localStorage`, and an interceptor adds it to API calls.

Alternatives: session cookies would need CSRF protection and sticky sessions. Keycloak is the better long-term answer, but nobody gave us one to use.

## Consequences

`SecurityIT` and `CustomerServiceTest` cover the `401` and `403` cases. We have no refresh token, no revocation and no login rate limit (R-9), so users log in again after 15 minutes or a page refresh. An agent can still rename a `CLOSED` customer as long as the status stays `CLOSED`.
