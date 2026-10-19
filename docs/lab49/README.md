The Lab 49 code already lives in this repo under `backend/`. Nothing to copy; work on a branch off `main`.

# Lab 49 — backend interaction slice (~45 minutes)

**Theme:** Capstone backend interaction slice

## Activity card

| | |
| --- | --- |
| **Checkpoint** | **E** |
| **Must prove** | service TODOs · compile · CUS-1001 fixtures · event V1 sketch |
| **Hard gate** | Pre-lab Pass · Lab 48 story |

## 45-minute session checklist

- [ ] Open `backend/` in IntelliJ with the JDK 21 SDK
- [ ] Fill `// TODO` in `InteractionService` (validate customer, persist, publish)
- [ ] Fill `CreateInteractionRequest` validation annotations
- [ ] Wire controller → service; return 201 shape for `CUS-1001`
- [ ] Sketch event fields in `CustomerInteractionRecordedV1`
- [ ] Run build smoke (`mvn -B -f backend/pom.xml test`)

## Build / smoke checklist

```bash
cd backend
mvn -B test
```

Expect failures until TODOs are filled, then both `InteractionServiceTest` methods green. Angular UI is Lab 50 — do not start it here.

## Timed-path Pass criteria

| Criterion | Pass / Fail |
| --------- | ----------- |
| Service TODOs filled (no `UnsupportedOperationException`) | Pass / Fail |
| Compile succeeds | Pass / Fail |
| At least one unit test for create-interaction path green (or documented baseline) | Pass / Fail |
| Fixtures `CUS-1001` / `lab-request-001` appear in code or test | Pass / Fail |

Full path (multi-day): Flyway migration, Kafka IT, Problem Details, consumer/DLT, [`backend-demo.md`](backend-demo.md) — see the Lab 49 GUIDE.

### Troubleshooting

| Symptom | Fix |
| --- | --- |
| UnsupportedOperationException left | Fill InteractionService TODOs |
| Validation missing | Annotate CreateInteractionRequest |
| Wrong module path | Run Maven from `backend/` (or `mvn -f backend/pom.xml`) |
| Tests red until TODOs | Expected; then green session tests |
