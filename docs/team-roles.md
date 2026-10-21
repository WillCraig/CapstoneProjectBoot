# Team roles

There are four of us. Each person leads one area, but everyone should be able to walk through every part at the defense.

| Person | Leads |
| --- | --- |
| Will Craig | Planning, CI/CD, Docker, k3s, Terraform and Ansible, security, demo and defense |
| Keifer Kolar | Backend: REST services, Kafka, PostgreSQL and Flyway |
| Jason McGill | Frontend: Angular screens, styling, accessibility |
| Brandon | TBD, agree with Brandon |

## How we work

- `main` is protected. Every change goes through a PR with one approval from someone who didn't write it.
- We use short-lived `feature/` and `docs/` branches. Environments are places, not branches.
- We bring teammate work in file by file instead of merging whole diverged branches.
- Each PR names its story or ADR, says how it was tested, and fills in the AI section from the [AI usage plan](ai-usage-plan.md).
- Decisions go in `docs/adrs/`, not in chat.
- If you're stuck for 30 minutes, ask for help.
- Synthetic data only. Never commit `.env` files, tokens, kubeconfigs or Terraform state.

## Sign-off

| Name | Agree? | Date | Signature |
| --- | --- | --- | --- |
| Will Craig | | | |
| Keifer Kolar | | | |
| Jason McGill | | | |
| Brandon | | | |
