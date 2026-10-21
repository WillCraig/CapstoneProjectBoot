# AI usage plan

Our stack is Angular, Spring Boot, PostgreSQL, Kafka, GitHub Actions and k3s. We don't let AI suggestions move us to React, Oracle, SOAP, Bitbucket or a different platform. The team makes those calls.

We use GitHub Copilot, Cursor, ChatGPT, IntelliJ AI Assistant and Google Gemini. Any other tool follows the same rules.

## Allowed

- Boilerplate code, like DTOs and Angular services and components
- First drafts of unit and integration tests
- Sketches of Terraform, GitHub Actions workflows and k8s manifests
- Explaining unfamiliar code, errors and stack traces
- A first review pass before a teammate reviews the PR
- Proofreading docs

## Must verify

AI-written code goes through the same CI checks as everything else.

- [ ] Angular: `npm run build` and `npm test` pass in `frontend/`
- [ ] Angular: TypeScript models match a real response from the running API
- [ ] Flyway/SQL: the migration is a new `V<n>__*.sql` file and uses PostgreSQL types only (no `VARCHAR2`, `NUMBER` or `SYSDATE`)
- [ ] Flyway/SQL: the migration applies cleanly to an empty Postgres 16
- [ ] Terraform, workflows and manifests pass `terraform validate`, a green run, or `kubectl apply --dry-run=client`
- [ ] `git diff` has no secrets, tokens or passwords before pushing
- [ ] The author can explain every changed line without the AI tool

## Forbidden

- Pasting secrets, tokens, passwords, kubeconfigs or customer data into a prompt
- Committing secrets or kubeconfig files
- Letting AI deploy to or change the shared cluster
- Applying Terraform, Ansible or kubectl changes to prod without team approval
- Copying proprietary or license-incompatible code

## Attribution

Every PR description has an "AI assistance" section that says which tool was used, what it drafted, and who checked it. If no AI was used, write "None." Reviewers don't approve until it's filled in.

## AI on this planning pack

We drafted these Lab 48 docs with help from AI tools, including Copilot, Cursor and an AI agent, and then cut them down. We checked the facts against the code on `will_work`. The platform question, roles, risk owners and sign-offs are our decisions, not the tools'.
