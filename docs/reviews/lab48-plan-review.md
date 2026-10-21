# Lab 48 plan review and sign-off

Lab 48 asks for a peer review of the plan with the sign-off recorded ("No Lab 49+ merge without a reviewed architecture and delivery plan"). Fill this in during the review. Reviewers mark each check themselves. **Nothing below is pre-filled or pre-signed.**

**Branch / PR:** `docs/lab48-pack` → PR #____ · **Review date:** __________ · **Reviewers:** __________________

## Checklist (Pass / Fail, with a note)

| # | Check | Result | Note |
| --- | --- | --- | --- |
| 1 | Architecture uses Angular · REST · Spring Boot · PostgreSQL · Kafka · k3s · GitHub Actions ([architecture.md](../architecture.md)) | | |
| 2 | No Oracle, React, Bitbucket or SOAP as the chosen path (only as non-goals). The OpenShift question is recorded, not silently decided ([ADR 004](../adrs/004-k3s-traefik-ingress.md)) | | |
| 3 | The three required diagrams (app flow, Kafka, JWT security) and the C4 context/container views render on GitHub and match the code | | |
| 4 | Trust boundaries are stated. Angular is not the security boundary | | |
| 5 | Every ADR (001–006) has a decision, a rejected alternative and a consequence | | |
| 6 | Every NFR has a number, a check, a proving lab and a status ([nfrs.md](../nfrs.md)) | | |
| 7 | CAP-12 acceptance uses `CUS-1001`, `CUS-9999` and `lab-request-001` ([backlog.md](../backlog.md)) | | |
| 8 | The Actions plan covers Angular build/test, Maven verify, scans, package-once, digests and an approval-gated deploy ([github-actions-plan.md](../github-actions-plan.md)) | | |
| 9 | The AI plan has verification checks (including Angular and Flyway), forbidden items and PR disclosure ([ai-usage-plan.md](../ai-usage-plan.md)) | | |
| 10 | Environments are places, not branches. The same digest is promoted ([environment-strategy.md](../environment-strategy.md)) | | |
| 11 | Terraform/Ansible scope, state, idempotence and secrets are described with no credentials ([terraform-ansible-plan.md](../terraform-ansible-plan.md)) | | |
| 12 | Risks have owners and dates ([risk-register.md](../risk-register.md)) | | |
| 13 | No secrets, tokens or real personal data anywhere in the pack | | |

## Findings

| # | Doc | Issue | Fix owner | Resolved? |
| --- | --- | --- | --- | --- |
| 1 | | | | |
| 2 | | | | |
| 3 | | | | |

## Sign-off

Each reviewer signs for themselves. The author doesn't sign as a reviewer.

| Name | Role | Decision (Approve / Changes needed) | Date | Signature |
| --- | --- | --- | --- | --- |
| Keifer Kolar | Backend | | | |
| Jason McGill | Frontend | | | |
| Brandon | Team to confirm | | | |
| Will Craig (author) | Planning & deployment | n/a (author) | | |
