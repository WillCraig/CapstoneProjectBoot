# GitHub Actions plan — implemented in Lab 51

| Trigger | Jobs | Evidence |
| ------- | ---- | -------- |
| `pull_request` | Angular `npm ci` / Karma / production build; Maven verify; Trivy repository and image reports; CodeQL Java + TypeScript | Required check runs and uploaded artifacts |
| `push` to `main` | Same gates, then push the scanned images to GHCR, record registry digests, and package the verified JAR with SHA-256 | `image-digests-<sha>` and `crm-package-<sha>` artifacts |
| `push` of a `v*` tag | Same publication path as `main`; no automatic environment promotion | Digest pair for a manual Capstone Deploy run |
| Weekly / manual | CodeQL refresh | Code scanning results |
| Manual `workflow_dispatch` | Render or deploy one environment from supplied digests, Terraform plan/Ansible checks, rollout, and `CUS-1001` smoke | Rendered-manifest artifact and approved deploy run |

## Rules

- Angular and Java are separate required gates; one cannot hide the other.
- Package once: image and deploy jobs consume verified artifacts/digests and
  never silently rebuild application code.
- Trivy is evidence-producing but currently non-blocking because the starter
  dependency baseline has documented fixed HIGH/CRITICAL findings. The owner
  and exit condition are in `docs/capstone-cicd-runbook.md`.
- Branch protection and GitHub Environment reviewers are external settings;
  the exact check and environment names are listed in the runbook.
