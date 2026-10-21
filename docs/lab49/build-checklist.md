# Lab 49 build checklist

## Session (~45 min)

- [x] `mvn -B -f backend/pom.xml compile`
- [x] Fill `InteractionService` TODOs
- [x] `InteractionServiceTest` green for Amina path
- [x] Unknown customer path throws

## Full path

- [x] Flyway migration (`V1__crm_schema.sql`, `V2__processed_event.sql`)
- [x] JPA entity + repository
- [x] Kafka publisher + consumer + DLT
- [x] `mvn -B clean verify` green twice (29 tests, identical results)
- [x] Backend demo runbook ([backend-demo.md](backend-demo.md))
