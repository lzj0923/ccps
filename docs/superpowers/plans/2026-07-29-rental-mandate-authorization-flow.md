# Rental Mandate Authorization Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Require a signed owner authorization document before a rental mandate can become active, while keeping OTR generation available only after activation.

**Architecture:** Reuse the existing rental-mandate document relation `authorization` as the proof of owner authorization. The backend review transition will reject activation when that relation is missing; the frontend will clearly separate generating the blank authorization PDF from uploading the signed copy and refresh the selected mandate after upload.

**Tech Stack:** Spring Boot service/mapper tests, Vue 3 single-file components, existing `propertyApi` document APIs, Node test runner.

## Global Constraints

- Do not change database tables or migrate existing records.
- OTR remains a per-rental transaction and is not moved into rental-mandate authorization.
- Only `active` rental mandates can generate an OTR; draft, pending review, suspended, terminated, and expired mandates cannot.
- Existing generated authorization PDFs remain downloadable; activation requires an uploaded authorization document.

---

### Task 1: Add the backend authorization gate

**Files:**
- Modify: `backend/src/main/java/com/ccps/backend/mapper/AdminRentalMandateMapper.java`
- Modify: `backend/src/main/java/com/ccps/backend/service/AdminRentalMandateService.java`
- Test: `backend/src/test/java/com/ccps/backend/service/AdminRentalMandateServiceTest.java`

- [ ] Write a failing test proving an approved pending mandate is rejected when no authorization document exists.
- [ ] Run the focused test and confirm it fails because the service currently activates without checking documents.
- [ ] Add a mapper count query for `document_links.entity_type='rental_mandate'`, the mandate id, and `relation_type='authorization'`.
- [ ] In `review`, check the count before transitioning to `active`; throw HTTP 409 with a user-facing authorization message when it is zero.
- [ ] Run the focused test and confirm it passes.

### Task 2: Clarify the authorization upload workflow in the mandate page

**Files:**
- Modify: `frontend/src/components/AdminRentalMandateWorkspace.vue`
- Modify: `frontend/src/i18n/index.js` (only if a required message is missing)
- Test: `frontend/tests/admin-rental-mandate-authorization-flow.test.mjs`

- [ ] Write a failing source-level regression test requiring a signed-upload action/message and a disabled activation path until the authorization document is present.
- [ ] Run the test and confirm it fails against the current page.
- [ ] Keep “生成授权委托书” as a download-only action; do not automatically upload the unsigned generated PDF as proof.
- [ ] Label the `authorization` upload slot as the signed owner authorization and refresh the selected mandate/documents after upload.
- [ ] Display the backend rejection message in the existing review error area/toast.
- [ ] Run the frontend regression test and confirm it passes.

### Task 3: Verify OTR remains downstream of active authorization

**Files:**
- Modify: `frontend/tests/contract-template-restore.test.mjs`

- [ ] Add assertions that the OTR action is still in the tenancy page and authorization generation/upload is in the rental-mandate page.
- [ ] Run the contract and authorization regression tests together.
- [ ] Run JavaScript syntax checks for both modified Vue scripts and `propertyApi.js`.

### Approved follow-up: guided authorization workspace

The workflow was extended after review: a pending mandate without authorization now opens a dedicated authorization workspace. Generation uploads an `authorization_draft` (unsigned) document, the existing online-signing action is available on that draft, and only the signed result is accepted by the activation gate. Attachments, handover and history are shown only after opening the separate mandate-management control.
