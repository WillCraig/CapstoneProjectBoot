# Lab 48 planning pack: index and checklist

One place to find every Lab 48 deliverable. Labs 49–52 and the defense evidence index link here. Requirements come from `LAB-48-GUIDE.md` and its starter templates, the capstone brief (`CAPSTONE-BRIEF-AND-RUBRIC.md`, sections 3 and 6), and the "Full-Stack Architecture & Planning" row of `Java_Angular_Fullstack_Capstone_Rubric.docx`.

Status key: **Done** = written and checked against the code on `will_work`. **Done, items open** = written, with listed parts marked *Team to confirm*. **Needs team** = needs teammates or the instructor before it can be finished.

Status as of 2026-10-21.

| # | Deliverable (source) | File | Status |
| --- | --- | --- | --- |
| 1 | Stack table, components and non-goals; Oracle, React, SOAP and Bitbucket rejected (Guide step 1, deliverable 1) | [architecture.md](../architecture.md) | Done. OpenShift vs k3s is an instructor question |
| 2 | Diagram: Angular → REST → Spring Boot → PostgreSQL (Guide step 2) | [architecture.md, Diagram 1](../architecture.md#diagram-1-application-flow-cap-12-record-an-interaction) | Done |
| 3 | Diagram: Spring Boot ↔ Kafka topics (Guide step 2) | [architecture.md, Diagram 2](../architecture.md#diagram-2-kafka-topics-and-consumers) | Done |
| 4 | Diagram: JWT interceptor → API security (Guide step 2) | [architecture.md, Diagram 3](../architecture.md#diagram-3-jwt-login-and-api-security) | Done |
| 5 | Delivery diagram (starter template) | [architecture.md, Delivery path](../architecture.md#delivery-path) | Done |
| 6 | C4 context view with trust boundaries (brief §3, rubric) | [architecture/context.md](../architecture/context.md) | Done, items open (DB/Kafka/cluster hosting) |
| 7 | C4 container view (brief §3, rubric) | [architecture/container.md](../architecture/container.md) | Done, items open (same) |
| 8 | 2–3+ short ADRs (Guide step 1) | [adrs/001](../adrs/001-event-contract.md) · [002](../adrs/002-postgresql.md) · [003](../adrs/003-github-actions-digest-promotion.md) · [004](../adrs/004-k3s-traefik-ingress.md) · [005](../adrs/005-rest-api-contract.md) · [006](../adrs/006-jwt-roles.md) | Done (6 ADRs) |
| 9 | Measurable NFRs (brief §2.4, rubric) | [nfrs.md](../nfrs.md) | Done. Some rows not yet evidenced (deploy, performance) |
| 10 | Risk register with owners and dates (brief §6, rubric) | [risk-register.md](../risk-register.md) | Done. Owners are proposals: **Needs team** |
| 11 | Backlog: CAP-12 plus 4–6 related stories, prioritized (Guide step 6, deliverable 6) | [backlog.md](../backlog.md) | Done (CAP-12 to CAP-22 with status) |
| 12 | Team roles and working agreements (brief §6 evidence pack) | [team-roles.md](../team-roles.md) | **Needs team:** Brandon's piece, backups, side duties, signatures |
| 13 | AI usage plan: allowed, verify (Angular + Flyway), forbidden, disclosure (Guide step 3, deliverable 2) | [ai-usage-plan.md](../ai-usage-plan.md) | Done |
| 14 | GitHub Actions plan: PR / main / tag, gates, artifacts, deploy ownership (Guide step 4, deliverable 3) | [github-actions-plan.md](../github-actions-plan.md) | Done. Required-check names: Team to confirm |
| 15 | Terraform / Ansible plan: scope, state, idempotence, secrets (Guide step 5, deliverable 4) | [terraform-ansible-plan.md](../terraform-ansible-plan.md) | Done. Remote backend: Team to confirm |
| 16 | Environment strategy: dev/test/stage/prod-like ↔ k3s namespaces, promotion, approvals (Guide step 5, deliverable 5) | [environment-strategy.md](../environment-strategy.md) | Done. Real namespaces and hosts: instructor |
| 17 | Peer review notes and sign-off (Guide step 6) | [reviews/lab48-plan-review.md](../reviews/lab48-plan-review.md) | **Needs team:** blank, for reviewers to fill in and sign |
| 18 | No secrets, real customer data, or Bitbucket/Oracle/React/SOAP as the taught path (deliverable 7) | whole pack | Done. Synthetic fixtures only, no credentials |

## Timed-path self-check (starter README)

| Criterion | Where |
| --- | --- |
| Architecture uses the mandatory stack | [architecture.md](../architecture.md) stack table |
| CAP-12 backlog seed present | [backlog.md](../backlog.md#cap-12-record-customer-interaction) |
| Actions plan mentions Angular build/test | [github-actions-plan.md](../github-actions-plan.md) |
| AI plan has verification + forbids | [ai-usage-plan.md](../ai-usage-plan.md) |

Pass/Fail marks are for the team to give during the review, not written here (the lab says not to write them down).

## Questions for the instructor

1. Is **OpenShift** (rubric docx) or **k3s** (brief and labs) graded? If OpenShift, can we get a Project and access? ([ADR 004](../adrs/004-k3s-traefik-ingress.md))
2. Shared k3s access: namespaces, Ingress hosts, and where PostgreSQL and Kafka run ([environment strategy](../environment-strategy.md)).
3. CodeQL on a private personal repo can't upload results. Is a short public window OK, given the course brief and rubric are in git history? (R-03, R-16)
4. Node 20 or 22 in CI? (R-13)
5. Is an OpenAPI document required? (CAP-21)
6. Which Lab 48 items are graded and which are practice? (R-20)

## Fixtures used everywhere

`CUS-1001` Amina Khan (ACTIVE), `CUS-1002` Ravi Singh (PROSPECT), `CUS-9999` (not found), correlation id `lab-request-001`, demo users `agent` / `admin`. Package: `com.northstar.crm`.
