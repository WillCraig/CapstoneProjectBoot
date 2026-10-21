# Terraform / Ansible plan — Lab 51 implementation

## Terraform

- Code: `infra/terraform/`
- Scope: the four environment namespace names and Ingress-host contract for
  the shared **k3s** cluster (not OpenShift Projects).
- `manage_namespaces` defaults to `false` because the instructor assigns and
  owns those namespaces today. The workflow runs format, init without a
  backend, validate, and—after environment approval—plan only.
- There is no `terraform apply` in GitHub Actions.
- Before setting `manage_namespaces = true`, configure an approved remote
  backend and least-privilege cluster credentials. State files, plans, and
  credentials must never be committed.
- Forbidden: public database exposure, committed `*.tfstate`, and long-lived
  cluster credentials in YAML.

## Ansible

- Code: `infra/ansible/site.yml`
- Purpose: an idempotent, no-change promotion preflight rather than click-ops
  mutation. It validates the target environment, namespace, Ingress host,
  and both image digest formats, then prints only non-secret values.
- The deploy workflow installs `ansible-core`, syntax-checks the playbook,
  and runs it before rendering manifests.
- Actual cluster mutation remains in the digest-pinned Kustomize apply step
  after GitHub Environment approval.
- Secrets come from GitHub Environment secrets or Vault-backed operator
  tooling—never plaintext inventory or playbooks.

## Promotion sequence

1. Terraform format/validate and Ansible preflight.
2. GitHub Environment approval.
3. Terraform plan with `manage_namespaces=false` until ownership and the
   remote backend are explicitly approved.
4. Apply the rendered Kustomize overlay by image digest.
5. Roll out, smoke `CUS-1001`, and roll back by digest if needed.
