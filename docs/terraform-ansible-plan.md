# Terraform / Ansible plan (planned in Lab 48, built in Lab 51)

Target: the shared **k3s** cluster with Traefik Ingress. OpenShift is an open question for the instructor ([ADR 004](adrs/004-k3s-traefik-ingress.md)). Infrastructure is written down and runs the same way twice, with no hand-clicking on the cluster. Today both tools **validate and plan only**. Nothing applies infrastructure changes.

## Ownership

Each resource has one owner.

| Tool | Path | Owns | Leaves alone |
| --- | --- | --- | --- |
| Terraform | `infra/terraform/` | The contract for the four namespace names and Ingress hosts. Creating namespaces only if `manage_namespaces = true` | Images, digests, ConfigMaps, Secrets, Kafka topics |
| Ansible | `infra/ansible/site.yml` | A no-change promotion **preflight**: checks the environment name, namespace, Ingress host and both digest formats, and prints only non-secret values | Cluster changes of any kind |
| GitHub Actions + Kustomize | `.github/workflows/capstone-deploy.yml`, `deploy/k3s/` | Creating the runtime Secrets, applying the digest-pinned overlay, rollout, smoke | Namespace ownership (instructor), building images (CI) |

Neither tool rebuilds or retags containers.

## Terraform

- Provider: `hashicorp/kubernetes` (the lock file `.terraform.lock.hcl` is committed).
- `manage_namespaces` defaults to **`false`**, because the instructor assigns and owns the namespaces. With `false`, `plan` is a contract check that changes nothing.
- The placeholders in `variables.tf` are namespaces `northstar-crm-{dev,test,stage,prod-like}` and hosts `crm-<env>.example.invalid`. Both are validated as DNS labels.
- In the deploy workflow: `terraform fmt -check`, then `init -backend=false`, then `validate`. After Environment approval it runs `plan -var=manage_namespaces=false`. **There is no `terraform apply` in Actions.**

### State and secrets

- No state backend yet: `init -backend=false`, and `plan` runs with `-lock=false`. Before anyone sets `manage_namespaces = true`, we need an approved **remote backend with locking** and least-privilege cluster credentials. **Team to confirm** the backend with the instructor.
- `*.tfstate`, `*.tfstate.*`, `*.tfplan`, `.terraform/` and `*.tfvars` are gitignored in `infra/terraform/.gitignore`. Only a `*.tfvars.example` may be committed.
- Credentials come from the runtime environment only (the kubeconfig written from the `KUBECONFIG_B64` Environment secret during a deploy). Never from `.tf` files or tfvars.
- **Forbidden:** a public database, open `0.0.0.0/0` access, secrets in `.tf`/`.tfvars`, committed state, a kubeconfig in the repo.

## Ansible

- `site.yml` runs against `localhost` only, uses only `assert` and `debug` tasks, and changes nothing. That makes it **idempotent by design**: every run reports `ok`, never `changed`.
- The deploy workflow installs `ansible-core`, runs `--syntax-check`, then runs the playbook before rendering manifests.
- Secrets come from GitHub Environment secrets. The inventory is `-i localhost,`, so there are no real hosts or passwords in Git.

## Promotion sequence

1. Terraform fmt and validate, plus the Ansible syntax check and preflight (also runs on dry runs).
2. GitHub Environment approval.
3. Terraform plan with `manage_namespaces=false`.
4. Create or update `ghcr-pull-secret` and `crm-secrets` from Environment secrets.
5. Apply the rendered Kustomize overlay, pinned by digest.
6. Wait for rollout, smoke `CUS-1001`, and roll back by digest if needed.

## Digest and rollback

The digest Actions built is what lands in every environment. Terraform and Ansible never change the app image. App rollback means redeploying the previous digest pair. A Terraform plan that shows unexpected destroys stops in review.
