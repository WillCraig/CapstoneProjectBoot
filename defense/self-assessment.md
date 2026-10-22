# Self-assessment

Honest status against the brief's defense list (`CAPSTONE-BRIEF-AND-RUBRIC.md` sections 9.2 and 9.3) and the Lab 52 outcomes. Scores are ours, not the instructor's. 4 would mean we would show it without a caveat. We are not there on deploy.

Checked 2026-10-21. Evidence IDs are in `defense/evidence-index.md`.

## Week close-out

| Outcome | Evidence | Status |
| --- | --- | --- |
| Lab 48: architecture, NFRs, backlog, ADRs, risks, AI plan | E01, E07, E02, E10-E12, E08, E14, E16 | Present. AI "Must verify" boxes in `docs/ai-usage-plan.md` are still unchecked. Team sign-off table in `docs/team-roles.md` is blank. |
| Lab 49: REST slice, Kafka, tests | E03, E10, E22, E28, E29 | Shown on the laptop and in CI (99 tests, 0 failures). Outbox not built. |
| Lab 50: Angular + Postgres | E04, E24, E25, E27 | Unit tests 43/43 here and on CI. UI click-through not repeated today. Selenium not in CI. |
| Lab 51: security, Actions, digest, deploy, smoke, rollback | E05, E17-E21, E23, E31, E33, E06 | Security and CI digests shown. Deploy, cluster smoke, and rollback not shown. |
| Lab 52 packet | This folder | Script, index, Q&A, retro, self-assessment, and outline are in git. No PDF. No panel yet (`defense/feedback-log.md`). |
| No secrets in the packet | Scrub before commit. Local key stays in `application-local.yml`, which this folder does not quote. | We grepped `defense/` before committing. |
| Limitations have owners | E08, `defense/retrospective.md`, `defense/notes/deployment-gap.md` | Deploy, CodeQL, and Trivy have a role and a measure. The speaker names are still "Team to confirm". |

## What the panel may ask to see

| Outcome | Evidence | Status |
| --- | --- | --- |
| Working Angular app | `README.md`, E25, E27 | `http://localhost:4200` returned 200 and the title Northstar CRM (IPv6 localhost only). We did not click through it. Selenium pass is the Oct 21 report only. |
| Architecture and the ADRs | E01, E10, E11, E12 | Can explain from the docs. OpenShift vs k3s is still an open question. |
| Backend, messaging, UI, Postgres | E03, E04, E29 | Can walk the code and the SQL. Cluster path not shown. |
| Automated tests | E22, E23, E24 | 99 backend, 43 frontend, local and on run 39649860445. |
| Security scan | E35, E39 | Trivy ran and did not fail the build. CodeQL passes now (run 39777251809). We have not read its finding list. |
| GitHub Actions and OpenShift | E23, E31, E33 | Actions and digests yes. OpenShift no. k3s deploy no. |
| Risks, limits, AI | E08, E14, retro | Risks are short and named. AI plan is a page, not a close-out of what each person used. |
| Q&A with evidence | `defense/technical-q-and-a.md` | Written. Not rehearsed out loud. |
| Retrospective | `defense/retrospective.md` | Written from the repo. Not a team meeting. |
| Individual reflection and next steps | Retro "Reflection" plus the five actions | One shared note. No separate write-up per person. |

## Starter scores

| Criterion | Score (1-5) | Why |
| --- | --- | --- |
| Architecture clarity | 3 | Diagrams and three ADRs match the code. OpenShift is unresolved. |
| Working vertical slice | 4 | Laptop API, seed data, and tests. Not the cluster. |
| Security / release | 3 | JWT and 403 are real. Release stops at the digest. |
| Evidence discipline | 3 | This index exists. Some Lab 49/50 pages still describe the old demo token. |
| Demo professionalism | 2 | Script is timed. Nobody has rehearsed it. No fallback video. |
| Honesty about gaps | 4 | Deploy, CodeQL, and Trivy are written as gaps. |

**Overall:** We can defend the slice and the pipeline. We cannot defend a deployment. Full credit on the security/CI/deploy category would be the wrong claim.
