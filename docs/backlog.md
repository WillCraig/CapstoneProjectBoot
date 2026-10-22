# Capstone backlog

## CAP-12: Record customer interaction
**As** an agent **I want** to POST an interaction for `CUS-1001`  
**So that** the note persists in PostgreSQL and emits a versioned Kafka event.

**Acceptance**
1. `201` with correlation `lab-request-001`
2. Row visible in PostgreSQL
3. Event `CustomerInteractionRecordedV1` published
4. `CUS-9999` → `404`

Status: done.

## Additional seeds
- CAP-13 Angular list/detail for interactions: agent sees a customer's interactions newest first and can open one. Done.
- CAP-14 JWT login shell: log in once, token goes on every API call, 401 sends you back to login. Done.
- CAP-15 Flyway interaction table: schema builds from an empty database. Done.
- CAP-16 Actions PR gate (Angular + Maven): every PR runs `ng test`, `ng build` and `mvn verify`. Done.
- CAP-17 k3s smoke via Ingress: deploy, log in and create an interaction through the Ingress. Waiting on cluster access.
