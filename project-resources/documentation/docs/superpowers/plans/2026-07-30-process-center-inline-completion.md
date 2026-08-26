# Process Center Inline Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep property handover checklist, handover photos/attachments, and role-specific next actions inside the current rental process center.

**Architecture:** Reuse the existing property API endpoints and current-rental workbench. The action drawer will expose a compact handover editor that saves checklist items and optional photos before completing handover. Action visibility will be derived from the stage owner role and the current admin role without changing the four-stage domain model.

**Tech Stack:** Vue 3 SFC, existing `propertyApi.js` request helpers, Node test runner, Vite, Spring Boot/MyBatis backend.

## Global Constraints

- Preserve the current-rental-only model; historical mandates and leases must not affect current actions.
- Keep the four stages and remove no existing action.
- Use the existing `ownerUnitId` for property checklist/photo APIs and the actual lease `unitId` for tenancy APIs.
- All new visible labels require Simplified Chinese, Traditional Chinese, and English entries.
- Do not add a second property or rental workflow screen.

---

### Task 1: Inline handover checklist and photos

**Files:**
- Modify: `frontend/src/components/AdminPropertyProcessWorkspace.vue`
- Modify: `frontend/src/services/propertyApi.js`
- Modify: `frontend/src/i18n/index.js`
- Test: `frontend/tests/admin-property-process-workspace.test.mjs`

**Interfaces:**
- Consume: `fetchAdminPropertyHandoverChecklist`, `createAdminPropertyHandoverChecklistItem`, `updateAdminPropertyHandoverChecklistItem`, `fetchAdminPropertyPhotos`, `createAdminPropertyPhoto`.
- Produce: the `complete_handover` drawer submits property data, checklist updates, and selected photo files before refreshing the current workspace.

- [x] **Step 1: Write a failing test** asserting the action drawer references checklist/photo APIs, renders checklist rows and a multi-file photo input.
- [x] **Step 2: Run the focused workspace test and confirm the new assertions fail for the missing APIs/UI.
- [x] **Step 3: Add the smallest UI/API wiring: load checklist rows, update existing rows, create new rows, and upload selected photos using the current property owner/unit identifiers.
- [x] **Step 4: Add validation so an incomplete checklist is shown as a blocking reason and cannot be silently marked complete.
- [x] **Step 5: Run the focused test and Vite build; confirm both pass.

### Task 2: Role-specific next actions

**Files:**
- Modify: `frontend/src/components/AdminPropertyProcessWorkspace.vue`
- Modify: `frontend/src/i18n/index.js`
- Test: `frontend/tests/admin-property-process-workspace.test.mjs`

**Interfaces:**
- Consume: `stage.ownerRole`, current action metadata, and the authenticated admin session role if available on the component/app store.
- Produce: one primary action per stage, disabled with a clear explanation when the current role is not the owner.

- [x] **Step 1: Write a failing test** asserting role gating and the “负责人” explanation are present.
- [x] **Step 2: Run the focused workspace test and confirm it fails.
- [x] **Step 3: Implement role normalization and action gating, preserving read-only history/details links for non-owners.
- [x] **Step 4: Add translated role/permission copy and verify no mixed-language labels are introduced.
- [x] **Step 5: Run the focused test and Vite build; confirm both pass.

### Task 3: Full regression and handoff

**Files:**
- Modify: `.codex/task-log.md`

- [x] **Step 1: Run the four rental workflow test files.
- [x] **Step 2: Run the Vite production build.
- [x] **Step 3: Inspect the diff for accidental changes to unrelated modules and record the completed work and any environment limitation.
