# What we missed: deployment

No cluster deploy has been run.

- `.github/workflows/capstone-deploy.yml` is `workflow_dispatch` only, digest-only, `dry_run` default `true`. `gh run list --workflow capstone-deploy.yml` returned no runs.
- `~/.kube/config` is not on this machine. `kubectl config current-context` says `current-context is not set`.
- Student number is not in the repo and was not confirmed here.
- The manifests on `main` still use namespace placeholders and host `crm-dev.example.invalid` (`deploy/k3s/overlays/dev/kustomization.yaml`). The backend container sets `runAsNonRoot: true` but the image `USER` is the name `spring` (`backend/Dockerfile`). The class branch adds numeric uid 10001 because k3s cannot resolve that name. That change is not on `main`.
- GitHub Environments, variables, and secrets in the runbook were not verified. This `gh` token cannot list packages (`read:packages` missing), so GHCR visibility is unknown.

## Work already on `feature/k3s-class-deploy` (not merged)

Four commits on top of `0235018`:

| Commit | What |
| --- | --- |
| `f679691` | `runAsUser` / `runAsGroup` 10001 |
| `060e886` | `deploy/k3s/overlays/class/` (Kafka in the namespace) and `scripts/deploy-class.sh` |
| `078e15a` | Deploy workflow: class target, no namespace create, smoke over HTTP |
| `2c121c3` | `docs/lab51/class-cluster-deploy.md` |

The class overlay pins the digests from CI run 39649860445 (same values as evidence E31). Host pattern in that file: `http://crm-api-studentNN.32.189.172.33.nip.io/`. Postgres URL in the overlay points at `32.189.172.33:5432/bootcamp` with schema `crm_studentNN`.

## Remaining steps

Do these from a checkout of `feature/k3s-class-deploy`, not from `main` and not from this defense branch. Do not commit the kubeconfig, the DB password, or the GitHub token.

1. Get `studentNN.yaml` from the instructor. Save it as `~/.kube/config`. `kubectl config current-context` must print `studentNN`.
2. Confirm the number. `scripts/deploy-class.sh` rejects anything that is not `student` plus two digits.
3. Get the class database username and password.
4. Get a GitHub token with `read:packages`, or confirm another way for the cluster to pull `ghcr.io/willcraig/capstone-crm-backend` and `capstone-crm-frontend`. Not confirmed from here.
5. Create `crm-secrets` and `ghcr-pull-secret` once, with the prompts in `docs/lab51/class-cluster-deploy.md`. The JWT value is `openssl rand -base64 48` at the prompt. Do not put it in git.
6. Dry run: `STUDENT=studentNN DRY_RUN=true scripts/deploy-class.sh`. Read the image lines. They should be the two digests from run 39649860445.
7. Deploy: `STUDENT=studentNN scripts/deploy-class.sh`. It applies, waits for `kafka`, `crm-backend`, and `crm-frontend`, and prints the nip.io URL.
8. Smoke: `CRM_BASE_URL=http://crm-api-$STUDENT.32.189.172.33.nip.io CRM_SMOKE_PASSWORD='<agent password>' bash scripts/smoke-crm.sh`. Expect `Smoke passed for CUS-1001 with correlation lab-request-001`.
9. Rollback only after a second deploy exists. Either `kubectl rollout undo deployment/crm-backend` and `deployment/crm-frontend`, or run `deploy-class.sh` again with the previous digests. Then smoke again. Flyway V1-V3 are additive. The runbook says older images still match this schema. That was not tried on a cluster.

The GitHub **Capstone Deploy** workflow is the other path (Environments, approval, `dry_run: true` first). It has never been started. The class script is the one written for this cluster.
