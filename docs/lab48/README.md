# Lab 48 planning pack

Where to find each Lab 48 deliverable. Status as of 2026-10-21.

| Deliverable | File | Status |
| --- | --- | --- |
| Stack, non-goals and diagrams | [architecture.md](../architecture.md) | Done |
| C4 context and container views | [context.md](../architecture/context.md), [container.md](../architecture/container.md) | Done. Cluster hosting TBD |
| ADRs | [001](../adrs/001-event-contract.md), [002](../adrs/002-postgresql.md), [003](../adrs/003-github-actions-digest-promotion.md), [004](../adrs/004-k3s-traefik-ingress.md), [005](../adrs/005-rest-api-contract.md), [006](../adrs/006-jwt-roles.md) | Done |
| NFRs | [nfrs.md](../nfrs.md) | Done. Deploy and performance rows not proven yet |
| Risk register | [risk-register.md](../risk-register.md) | Done. Owners need the team's OK |
| Backlog (CAP-12 to CAP-22) | [backlog.md](../backlog.md) | Done |
| Team roles | [team-roles.md](../team-roles.md) | Needs Brandon's piece and sign-offs |
| AI usage plan | [ai-usage-plan.md](../ai-usage-plan.md) | Done |
| GitHub Actions plan | [github-actions-plan.md](../github-actions-plan.md) | Done |
| Terraform and Ansible plan | [terraform-ansible-plan.md](../terraform-ansible-plan.md) | Done. State backend TBD |
| Environment strategy | [environment-strategy.md](../environment-strategy.md) | Done. Real namespaces TBD |
| Peer review | [lab48-plan-review.md](../reviews/lab48-plan-review.md) | Needs reviewers |

## Questions for the instructor

1. Is OpenShift or k3s graded? If OpenShift, can we get a Project?
2. How do we get into the shared k3s cluster, and where do PostgreSQL and Kafka run?
3. Can the repo go public for a day so CodeQL works, given the course brief is in git history?
4. Do we need an OpenAPI document?
