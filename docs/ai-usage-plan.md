# Northstar CRM: AI Usage Plan (Lab 48)

Our stack is Angular, Spring Boot, PostgreSQL, Kafka, GitHub Actions, and k3s. Don't let AI suggestions pull us toward OpenShift, React, Oracle, SOAP or Bitbucket. (Whether OpenShift is graded is an open question for the instructor, see [ADR 004](adrs/004-k3s-traefik-ingress.md). Only the team changes the platform, never an AI suggestion.)

**Tools we use:** GitHub Copilot, Cursor, ChatGPT, IntelliJ AI Assistant, and Google Gemini. Anything similar follows the same rules.

## Allowed
- Boilerplate code (DTOs, getters/setters, Angular services and components)
- First drafts of unit and integration tests
- Sketches of Terraform HCL, GitHub Actions workflows, and k8s manifests
- Explaining unfamiliar code, errors, and stack traces
- Code review help before a human reviewer looks at the PR
- Tightening and proofreading docs

## Must verify
AI-drafted code goes through the same CI gates as everything else. No skipped checks.

- [ ] Angular: `npm run build` and `npm test` pass in `frontend/`
- [ ] Angular: TypeScript models match the Spring Boot API response DTOs (field names and types checked against a real response from the running API)
- [ ] Flyway/SQL: migration uses PostgreSQL types only (no `VARCHAR2`, `NUMBER`, `SYSDATE`, or other Oracle syntax)
- [ ] Flyway/SQL: migration runs clean on an empty Postgres 16 database (`docker compose up postgres`, then start the backend and confirm Flyway applies it)
- [ ] HCL, Actions, and k8s sketches pass `terraform validate`, a green workflow run, or `kubectl apply --dry-run=client` before review
- [ ] `git diff` checked for secrets, tokens, and hardcoded passwords before pushing
- [ ] All CI checks are green on the PR
- [ ] The author can explain every changed line without the AI tool

## Forbidden
- Pasting secrets, tokens, passwords, kubeconfig, or customer PII into any AI prompt
- Committing secrets or kubeconfig files
- Letting AI deploy to or change the shared k3s cluster on its own
- Production applies (Terraform, Ansible, kubectl) without team approval
- Copying proprietary or license-incompatible code from AI output or anywhere else

## Attribution
Every PR has an "AI assistance" section in its description. It says which tool was used, what it drafted, and who verified it (name plus which checks above they ran). If no AI was used, write "None." The reviewer doesn't approve until that section is filled in.

## Example checks for the two areas the lab calls out

- **Angular:** when AI drafted a page or service, we checked the TypeScript model against a real response (e.g. `GET /api/v1/customers/CUS-1001` returns `customerId`, `fullName`, `status`, `createdAt`), and checked that `ng test` covers the loading, error and `403` paths.
- **Flyway/SQL:** when AI drafted a migration, we checked it was a **new** `V<n>__*.sql` (merged ones are never edited), PostgreSQL types only, and that the Testcontainers ITs applied it from empty.

## How AI was used on this planning pack

The Lab 48 documents were drafted with help from AI assistants (GitHub Copilot, Cursor, and an AI agent). Some early samples (branch `48-grok`) were AI-written and marked "team to confirm". Every claim in the final pack was checked against the code on `will_work` (file paths, test names, workflow YAML), and anything we couldn't confirm is marked **Team to confirm**. Human-owned decisions: the platform question, roles and owners, risk acceptance, and the sign-off in [the plan review](reviews/lab48-plan-review.md).
