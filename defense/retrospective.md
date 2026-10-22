# Retrospective

Blameless. The panel is Thu/Fri Oct 22-23, 2026. This is from the repo and the checks on Oct 21, not from a facilitated meeting. Team to confirm anything we got wrong.

## Went well

- CAP-12 is on `main`: login, lookup, a saved interaction, a Kafka event after commit, tests around it.
- Auth is a real JWT. Agent close is 403 in `SecurityIT` and on the laptop API. The UI hide is not the only control.
- One Capstone CI run on `0235018` published both image digests.
- The Lab 48 docs on this branch match the system we actually built (Postgres, Kafka after commit, digest deploy).

## What got in the way

- The class-cluster fixes never landed on `main`. Main still has placeholder hosts. The numeric uid and the in-namespace Kafka are four commits to the side.
- Nobody on this machine has `~/.kube/config`. The deploy workflow has no runs. We kept writing the happy path while the cluster step waited on an instructor file.
- Capstone CI can be green in the same minute CodeQL is red. Branch protection does not require either check. A red scan does not stop a merge, and a green scan does not mean the image is clean, because Trivy exits 0.
- Speaker and operator are not named. The demo script cannot assign itself.
- We accepted no outbox. The Oct 21 Kafka stop showed a 201 with no `processed_event` row. That is the trade-off we wrote down, and it is still open.

## Actions

| Action | Owner | Due | Success measure |
| --- | --- | --- | --- |
| Put the class overlay on the branch we deploy, get `studentNN.yaml` onto the laptop, dry-run, deploy, smoke | Deploy role | Before the panel if the yaml shows up. Otherwise say the gap in the room. Team to confirm the date. | One smoke line from the class host: `Smoke passed for CUS-1001 with correlation lab-request-001`, plus the run or the script output saved outside git (no kubeconfig) |
| Record one fallback pass of the 11 minute script, including the 403 curl and the digest page | Speaker / operator. Names: Team to confirm | Before the panel | A video or a timed notes file that covers sign-in, CUS-1001, the note, the 403, and run 39649860445. Not committed if it could show a token |
| Require the four Capstone CI jobs on `main`. Leave CodeQL off that list until it can pass | Repo admin role | After the panel, so tonight's open PRs are not stuck | A pull request with a broken unit test cannot merge |
| Either enable code scanning and keep one green CodeQL URL, or disable `.github/workflows/codeql.yml` and say so in the index | Security role | Team to confirm | The evidence index links a green run, or it says the workflow is disabled and Trivy is the scan we run |
| Upgrade the Spring Boot parent and base images, retest, then set the image Trivy `exit-code` to 1 | Backend role and CI role | After the defense | A later main CI run is green with `exit-code` 1, and the runbook's 44/53 fixable counts are replaced with the new counts |

Five actions. No sixth.

## Reflection

The decision that shapes the story is publish-after-commit with no outbox, plus deploy-by-digest. We can show a saved row and a digest, and we have to say the cluster step is still open.

The strongest proof the CRM works is `InteractionApiIT` plus the laptop SQL for `CUS-1001`, backed by the 99 tests on run 39649860445. It is not the slide.

The hard question is "why isn't it deployed?" The answer is the missing kubeconfig and the unmerged class branch. We should not talk past that.
