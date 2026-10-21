# Risk register

Living list. Add a row whenever we cut scope or learn a new blocker. Owners are workstreams until names are assigned in `docs/team-roles.md`.

| ID | Risk | Likelihood / impact | Mitigation | Owner | Date |
| --- | --- | --- | --- | --- | --- |
| R-01 | No JWT issuer / signing details defined yet | Med / High | Keep demo bearer until Lab 51; record instructor answer in the security ADR; do not hard-code a fake issuer | Security / deploy | 2026-10-19 |
| R-02 | Kafka consumer side effect undefined (what happens on consume) | Med / Med | Ship producer-only for CAP-12 unless instructor says otherwise; document the cut in the event ADR and backlog | Backend / Kafka | 2026-10-19 |
| R-03 | Shared k3s access / namespace names unknown | Med / High | Environment strategy rows stay "pending instructor"; ask in one batch; fallback is local Compose + recorded evidence if cluster access slips | Security / deploy | 2026-10-19 |
| R-04 | Self-hosted Actions runner not proven yet | Med / High | Prove a hello-world workflow on the runner before Lab 51 CI; course sample is `ubuntu-latest` — self-hosted is team policy | Security / deploy | 2026-10-19 |
| R-05 | Graded vs practice unclear for some Lab 48 notes / exercises | Low / Med | Treat `docs/` planning pack as the graded path; keep pre-lab `notes/` out of the Lab 48 merge PR; confirm with instructor if unsure | Shared (instructor contact) | 2026-10-19 |

## Scope cuts

When we drop or defer work, add a row here (what we cut, why, who decided, date). Start empty beyond the seeds above.
