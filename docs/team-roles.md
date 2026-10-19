# Team roles and working agreements

Four people, four workstreams. Everyone should still be able to walk through every major part at the defense.

| Workstream | Owner | Backup |
| --- | --- | --- |
| Frontend (Angular) | Jason | Will |
| Backend / Kafka | Brandon | Keifer |
| Database (PostgreSQL / Flyway) | Keifer | Brandon |
| Security / deploy (JWT, Actions, k3s) | Keifer | Will |

Shared Lab 48 docs and Lab 52 defense work are everyone's, with the owner of the closest workstream driving the draft.

## Side duties

| Duty | Who | What |
| --- | --- | --- |
| Merge keeper | Will | Keep PRs moving, require one non-author approval, keep `main` green |
| Evidence lead | Will | Own `defense/evidence-index.md` so proof lands during the week, not Thursday night |
| Instructor contact | Will | Batch questions to the instructor and record answers in ADRs or the risk register |

## Branch and review rule

Every change goes through a pull request into `main` with **one non-author approval**. Reviewers rotate so the same person is not always rubber-stamping. No direct pushes to `main`.
