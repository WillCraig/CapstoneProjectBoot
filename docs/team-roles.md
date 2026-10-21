# Team roles and working agreements

Four people. Each person leads one area, and **everyone should still be able to walk through every major part at the defense**. The course suggests six roles (architecture/docs, backend/messaging, frontend/UX, data/persistence, security/delivery, demo/defense). With four people we combine them as shown.

## Leads

| Person | Leads | Includes | Backup |
| --- | --- | --- | --- |
| Will Craig | Planning and deployment | Lab 48 planning pack, GitHub Actions, Docker images, k3s manifests, Terraform/Ansible, demo and defense coordination | Team to confirm |
| Keifer Kolar | Backend | Spring Boot REST services, Kafka, PostgreSQL / Flyway | Team to confirm |
| Jason McGill | Frontend | Angular screens, styling, accessibility | Team to confirm |
| Brandon | A smaller, defined piece | **Team to confirm** what it is (to be agreed with Brandon) | Team to confirm |

Not yet assigned (**Team to confirm**): who owns security (JWT, roles, scans; Will by default, since it ships with deployment), and who owns data/persistence if it isn't Keifer.

## Side duties (Team to confirm)

| Duty | Who | What |
| --- | --- | --- |
| Merge keeper | ______ | Keep PRs moving, enforce one non-author approval, keep `main` green |
| Evidence lead | ______ | Own the Lab 52 evidence index, so proof lands during the week, not Thursday night |
| Instructor contact | ______ | Batch questions to the instructor, and record answers in ADRs or the risk register |

## Working agreements

1. **Branches:** `main` plus short-lived branches (`feature/…`, `docs/…`). No `dev`, `stage` or `prod` branches. Environments are places, not branches ([environment strategy](environment-strategy.md)).
2. **Pull requests:** `main` is protected. Every change goes in through a PR with **one approval from someone other than the author**. Reviewers rotate. No direct pushes to `main`.
3. **Bring work in file by file:** teammate work is copied onto the PR branch file by file and reviewed. Whole diverged branches don't get merged (R-19).
4. **Every PR** names its story (CAP-##) or ADR, says how it was tested (including one failure-path check), and fills in the "AI assistance" section of the [AI usage plan](ai-usage-plan.md).
5. **Decisions** go in `docs/adrs/`, not chat. Open questions go in the open-questions table in [architecture.md](architecture.md) or the [risk register](risk-register.md).
6. **Daily sync:** 15 minutes at the start of lab time (done / next / blocked). **Team to confirm** the time.
7. **Stuck rule:** ask for help after 30 minutes stuck.
8. **Evidence:** whoever finishes a story adds its proof to the evidence index the same day.
9. **Data and secrets:** synthetic fixtures only (`CUS-1001` Amina Khan, `CUS-1002` Ravi Singh, `CUS-9999`, `lab-request-001`). Never commit `.env` files, tokens, kubeconfigs or Terraform state.
10. **Disagreements:** the lead for that area proposes, the team discusses for up to 10 minutes, then the lead decides and an ADR records it. **Team to confirm.**

## Agreement sign-off

Each teammate signs to agree to the roles and working agreements above.

| Name | Agree? (Yes / Changes needed) | Date | Signature |
| --- | --- | --- | --- |
| Will Craig | | | |
| Keifer Kolar | | | |
| Jason McGill | | | |
| Brandon | | | |
