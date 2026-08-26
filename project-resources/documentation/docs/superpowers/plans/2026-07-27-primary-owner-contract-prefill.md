# Primary Owner Contract Prefill Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Automatically prefill the primary owner's name and identity/passport in the OTR and authorization-letter generator.

**Architecture:** Extend the existing tenancy list query with the active primary owner linked to the lease unit. Expose the values on each tenancy row, then use those values when the existing template dialog is opened. Empty database values remain editable blanks.

**Tech Stack:** Spring Boot, MyBatis, Java records, Vue 3, Vitest-style frontend tests, JUnit 5.

## Global Constraints

- Use only the active primary owner (`owner_units.is_primary = 1` and active status).
- Prefer `owners.identity_no`; fall back to `owners.passport_no` when the identity number is empty.
- Do not change the customer-provided PDF wording, layout, logo, or page count.

### Task 1: Add owner fields to tenancy data

**Files:**
- Modify: `backend/src/main/java/com/ccps/backend/dto/AdminTenancyResponse.java`
- Modify: `backend/src/main/java/com/ccps/backend/mapper/AdminTenancyMapper.java`
- Test: `backend/src/test/java/com/ccps/backend/mapper/AdminTenancyMapperSqlTest.java`

- [x] Add `ownerId`, `ownerName`, and `ownerIdentity` to `AdminTenancyResponse.Item`.
- [x] Join `owner_units` and `owners` through the lease unit, selecting only the active primary owner and falling back from identity number to passport number.
- [x] Extend the mapper SQL regression test to assert the primary-owner join and fallback expression are present.
- [x] Run `mvn -q -f backend/pom.xml -Dtest=AdminTenancyMapperSqlTest test`.

### Task 2: Use returned owner fields in the contract dialog

**Files:**
- Modify: `frontend/src/components/AdminTenancyWorkspace.vue`
- Test: `frontend/tests/admin-tenancy-contract-owner-prefill.test.mjs`

- [x] Add a failing test that checks `openTemplateGenerator` maps `selectedRow.ownerName` to `landlordName` and `selectedRow.ownerIdentity` to `landlordIdentity`.
- [x] Update the prefill object to use those row fields without changing the manual input fallback.
- [x] Run the focused frontend test and `npm exec -- vite build` from `frontend`.

### Task 3: Verify the complete flow

- [x] Run the backend unit tests for tenancy/template generation.
- [x] Start Spring Boot on a temporary port and confirm the application reaches `Started CcpsBackendApplication`.
- [x] Confirm the original two PDF resource hashes remain unchanged.
