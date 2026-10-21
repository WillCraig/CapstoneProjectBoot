Lab 50 — Frontend Persistence Demo
Purpose
This document describes the Northstar CRM frontend-to-backend persistence workflow and the end-to-end scenarios for the Lab 50 capstone.
Verification status: The code paths listed below have been reviewed during implementation, but the complete application flow must still be run and tested. Do not treat a scenario as passing until it has been executed.

Application structure
- frontend/ — Angular customer search, customer profile, interaction timeline, and interaction form.
- backend/ — Spring Boot REST API, JPA entities and repositories, service logic, and Flyway migrations.
- docs/ — implementation and demonstration documentation.
  Customer workflow
1. Open the customer search page.
2. Search for a synthetic customer ID such as CUS-1001.
3. The Angular CustomersService requests GET /api/customers/{customerId}.
4. Open the customer profile to view the customer details.
5. From the profile, navigate to the interaction timeline or the new-interaction form.
   The backend exposes GET /api/customers for listing customers and GET /api/customers/{customerId} for retrieving a customer by ID.
   Interaction workflow
   The interaction API is scoped to a customer:
   Method	Endpoint	Expected behavior
   GET	/api/customers/{customerId}/interactions	List active interactions, newest first
   POST	/api/customers/{customerId}/interactions	Create an interaction
   PUT	/api/customers/{customerId}/interactions/{interactionId}	Update an active interaction
   DELETE	/api/customers/{customerId}/interactions/{interactionId}	Soft-delete an active interaction


Create
1. Open a customer's interaction timeline.
2. Choose Record interaction.
3. Select a channel (PHONE, EMAIL, or CHAT) and enter a nonblank summary of no more than 2,000 characters.
4. Submit the form.
5. Confirm the new interaction appears in the timeline after navigation/refresh.
   The create endpoint accepts CreateInteractionRequest. The controller supports X-Correlation-ID and X-Actor headers.
   Read
1. Open the customer's interaction timeline.
2. Confirm interactions are displayed newest first.
3. Confirm deleted interactions do not appear.
   Update
1. Choose Edit on an active interaction.
2. Confirm the existing channel and summary are loaded.
3. Change one or both fields and save.
4. Confirm the updated values appear in the timeline.
   The update endpoint accepts UpdateInteractionRequest. The form uses the same component for creation and editing, distinguished by the interactionId route parameter.
   Delete
1. Choose Delete on an active interaction.
2. Confirm the browser confirmation prompt.
3. Confirm the interaction disappears from the timeline.
4. Refresh the timeline and confirm the deleted interaction remains absent.
   Deletion is implemented as a soft delete using the deleted flag; the row is not intended to be physically removed by this endpoint.
   Persistence and migration
   The Flyway migration intended for the soft-delete field is:

backend/src/main/resources/db/migration/V5__add_interaction_soft_delete.sql
ALTER TABLE customer_interactions
ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;
Before the demo, confirm the migration filename does not conflict with another migration and that Flyway applies it successfully. The application uses PostgreSQL and JPA schema validation; the migration must be applied before relying on the new entity field.
Error-handling scenarios
Scenario	Expected behavior
Unknown customer ID	404 Not Found
Unknown or soft-deleted interaction	404 Not Found
Blank summary or invalid request fields	400 Bad Request
Invalid channel or malformed JSON	400 Bad Request
Unauthorized request, if security is enabled for the endpoint	401 or the configured authorization response
Unexpected server failure	Show a generic UI error; do not expose internal stack traces


The InteractionExceptionHandler handles customer-not-found and request-validation errors. It was updated to map InteractionNotFoundException to 404 Not Found.
End-to-end verification checklist
Run these after starting PostgreSQL, the backend, and the Angular frontend.
- [ ] Flyway migrations complete successfully at backend startup.
- [ ] CUS-1001 can be retrieved through the customer search page.
- [ ] The customer profile loads and its navigation links work.
- [ ] The interaction timeline loads for CUS-1001.
- [ ] Creating an interaction persists it and it appears in the timeline.
- [ ] An empty summary is rejected by the UI and API validation.
- [ ] A summary longer than 2,000 characters is rejected.
- [ ] Editing an interaction persists the changed values.
- [ ] Deleting an interaction hides it from the timeline.
- [ ] A deleted interaction remains absent after reloading the timeline.
- [ ] Updating or deleting a missing/deleted interaction returns 404.
- [ ] Error responses include the expected structured ApiError fields and correlation ID.
- [ ] Browser console and backend logs contain no unexpected errors during the flow.
  Selenium demonstration scenario notes
  Use synthetic test data only. Do not use real customer personal information or commit credentials.

Suggested browser automation scenario:
1. Navigate to the Angular application.
2. Search for CUS-1001.
3. Open the profile and navigate to the interaction timeline.
4. Record an interaction with channel EMAIL and a synthetic summary.
5. Verify the new summary appears.
6. Edit the interaction and verify the changed summary appears.
7. Delete the interaction and accept the confirmation prompt.
8. Reload the timeline and verify the deleted summary is absent.
   Capture the actual test result, browser/driver version, and any failure details when the scenario is executed. Do not mark the scenario as passed based solely on code inspection.
   Demo notes
- Use only synthetic customer IDs and interaction summaries.
- Keep secrets and local environment credentials out of source control and screenshots.
- Verify that the backend and frontend use the expected local API URL before the demo.
- Record any remaining failures and fixes before declaring the capstone complete.