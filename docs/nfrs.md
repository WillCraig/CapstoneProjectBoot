# Non-functional requirements

| Target | How we check |
| --- | --- |
| Requests with no token or a bad token get 401, and an agent doing admin stuff gets 403 | `SecurityIT` |
| Tokens expire after 15 minutes | `application.yml` |
| No secrets in git | `.gitignore` + Trivy secret scan |
| Bad input or `CUS-9999` saves nothing and sends no event | `InteractionApiIT` |
| PR checks finish in under 10 minutes | Actions run times (about 3-4 min now) |
| Rollback to the last digests in 10 minutes or less | Not done yet, needs a cluster |
