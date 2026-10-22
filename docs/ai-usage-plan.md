# AI usage plan

## Allowed
- Boilerplate (DTOs, Angular services and components)
- First drafts of tests
- Sketches of Terraform, workflows and k8s manifests
- Explaining errors and unfamiliar code

## Must verify
- [ ] Angular changes compile and match DTOs
- [ ] Flyway/SQL reviewed for PostgreSQL correctness
- [ ] No secrets introduced

## Forbidden
- Committing secrets or kubeconfig
- Applying Terraform/Ansible to prod without approval
- Copying proprietary third-party code

We used AI tools to help draft these docs and checked them against our code.
