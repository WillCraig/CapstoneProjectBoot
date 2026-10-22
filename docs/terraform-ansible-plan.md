# Terraform / Ansible plan

## Terraform
- Scope (non-prod): k3s namespace names `northstar-crm-dev`, `-test`, `-stage` and `-prod-like`. We only validate and plan, we don't apply.
- State backend (no credentials in Git): none yet, since we don't apply anything.
- Forbidden: public DB, committed `*.tfstate`

## Ansible
- Playbooks: `site.yml` runs a preflight check before a deploy.
- Idempotence notes: it only runs checks, so it never changes anything.
- Secrets via vault / CI secrets: we use GitHub Environment secrets, never plaintext in the repo.
