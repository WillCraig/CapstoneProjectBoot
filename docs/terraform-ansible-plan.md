# Terraform and Ansible plan

Our target is the shared k3s cluster. Right now both tools only validate and plan. Nothing in our pipeline applies infrastructure changes, and neither tool builds or retags images.

## Terraform

- Scope: `infra/terraform/` uses the `hashicorp/kubernetes` provider. It holds the four namespace names (`northstar-crm-dev`, `-test`, `-stage`, `-prod-like`) and their Ingress hosts. These are placeholders until the instructor assigns real ones. It only creates namespaces if `manage_namespaces = true`, and the default is `false` because the instructor owns them.
- In the deploy workflow we run `terraform fmt -check`, `init -backend=false` and `validate`. After approval we run `plan -var=manage_namespaces=false`. There is no `terraform apply`.
- State backend: none yet. Before anyone turns on `manage_namespaces`, we need a remote backend with locking. TBD, ask the instructor.
- Credentials come only from the kubeconfig the deploy writes from the `KUBECONFIG_B64` secret, never from `.tf` or `.tfvars` files.
- Forbidden: a public database, open `0.0.0.0/0` access, committed `*.tfstate` or `*.tfvars`, and secrets in `.tf` files. `infra/terraform/.gitignore` blocks state, plans and tfvars.

## Ansible

- Playbooks: `infra/ansible/site.yml` runs a preflight on `localhost`. It checks the environment name, namespace, Ingress host and both digest formats, and prints only non-secret values.
- Idempotence: the playbook only uses `assert` and `debug` tasks, so every run reports `ok` and never `changed`.
- Secrets: they come from GitHub Environment secrets. The inventory is `-i localhost,`, so there are no hosts or passwords in Git.

## Deploy order

1. Terraform validate and the Ansible preflight. These also run on a dry run.
2. GitHub Environment approval.
3. Terraform plan.
4. Create `ghcr-pull-secret` and `crm-secrets` from Environment secrets.
5. Apply the digest-pinned overlay, wait for rollout, and run the smoke test.
