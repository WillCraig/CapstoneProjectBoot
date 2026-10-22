# Demo recording guide (about 8–10 minutes)

## Set up (2 minutes, before you hit record)

1. Stack is running: open http://localhost:4200 (use "localhost", not 127.0.0.1).
2. Browser: one tab on http://localhost:4200/login, one tab on
   https://github.com/WillCraig/CapstoneProjectBoot/actions/runs/39777251733
3. Terminal at the repo root (`cd ~/TEK/CapstoneProjectBoot` on this laptop), font size bumped up (Cmd + a few times).
   Optional: a second terminal running the backend so the log is visible.
4. Do Not Disturb on. Close Slack, Messages, Mail.
5. Have the agent password ready (README.md), but don't open README on screen.
6. Start recording: Cmd+Shift+5, Record Entire Screen, Options, pick your mic.

## Part 1: the UI (about 4 minutes)

| # | Do | Say |
|---|----|-----|
| 1 | Show the login page. | "Northstar CRM. Agents need one place to find a customer and log what happened on a call. Angular front end, Spring Boot API, Postgres, and Kafka." |
| 2 | Sign in as `agent`. Header shows `agent (AGENT)`. | "I'm signing in as an agent. The token is only kept in memory, so a refresh logs you out. The page isn't the security; the API is." |
| 3 | Click **Look up a customer**, type `Amina`, **Search**. | "Looking up our seeded customer, Amina Khan." |
| 4 | Click **Profile** on `CUS-1001`. Shows ACTIVE. | "Here's her profile: CUS-1001, active." |
| 5 | Point at "Only an ADMIN can close a customer." | "As an agent I don't get a Close button. That's just a hint in the UI; I'll show the API actually blocks it in a minute." |
| 6 | Click **Record an interaction**. Type NOTE, summary `Resolved login question`, create. | "Now I'll log the call as a note." |
| 7 | Detail page for the new interaction appears. | "It saved. Next I'll prove where it went." |

Don't scroll the full customer list (there are extra test rows). Don't refresh the page.

## Part 2: the script (about 4 minutes)

In the terminal, from the repo root:

    bash defense/demo-walkthrough.sh

Type the agent password when asked (it doesn't show). Press Enter between steps.

| Step | What you'll see | Say |
|------|-----------------|-----|
| 1 App is up | ✔ API is up | "Quick health check." |
| 2 Test customers | Amina ACTIVE, Ravi PROSPECT | "Our two seeded customers. Remember Ravi is a prospect." |
| 3 Sign in | ✔ Signed in | "Same agent login from the terminal. The token is never printed." |
| 4 Note in Postgres | Top row: CUS-1001, NOTE, lab-request-001, **t** | "The note I just made is in Postgres. That's our source of truth. Older rows marked f are earlier test notes that were soft-deleted." |
| 5 Kafka event | JSON with customerId CUS-1001, NOTE, correlationId | "After the save we publish an event to Kafka. Notice the summary isn't in it, only IDs." |
| 6 Bad input | HTTP 400, "Validation failed" | "Bad input gets a clean error with the allowed types, not a stack trace." |
| 7 Close Ravi | HTTP 403, then ✔ Ravi is still PROSPECT | "This is the important one. Same agent tries to close Ravi straight through the API. 403, and Ravi didn't change. The rule lives on the server." |
| 8 Unknown customer | HTTP 404 | "Unknown customer, clean 404." |
| 9 Fake token | HTTP 401 | "And a fake token gets 401." |

## Part 3: CI and what's left (about 1 minute)

| Do | Say |
|----|-----|
| Switch to the Actions run tab. Scroll to the digest lines. | "CI on main: 56 frontend tests and 99 backend tests passed, including 17 security tests. CodeQL passes too. Trivy scans and saves a report. The images were pushed and we deploy them by digest, not a rebuild." |
| Stay there. | "What we haven't done is the deploy to the class cluster. The config is ready on a branch, feature/k3s-class-deploy; the next step is my student namespace, deploy those digests, and smoke test CUS-1001." |

Stop recording (the stop button in the menu bar). Watch it once: no password or token visible, the 403 and "Ravi is still PROSPECT" are readable.

## If something goes wrong while recording

- A stumble: keep going. One honest take is fine.
- Login fails in the script: check the password and run the script again.
- API not up: start the backend (see demo-commands.md), then start the recording over.
