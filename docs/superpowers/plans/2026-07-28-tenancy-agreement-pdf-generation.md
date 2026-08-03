# 租赁合约 PDF 自动生成 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Use the customer-provided 22-page tenancy agreement PDF as a fixed template and generate a filled PDF from the selected lease.

**Architecture:** Add a dedicated PDF stamper service that overlays only dynamic fields on the original template. Reuse the existing contract-template endpoint and tenancy dialog, changing the tenancy-agreement response from DOCX to PDF.

**Tech Stack:** Spring Boot, OpenPDF (`PdfReader`/`PdfStamper`), Vue 3, existing admin contract-template API.

## Global Constraints

- Keep the customer PDF wording, page order, photos, page size, and formatting unchanged.
- Do not generate or download Word for this template.
- Do not overwrite the source template.
- Empty input fields must not retain sample tenant/owner data.

### Task 1: PDF service and tests

**Files:**
- Create: `backend/src/main/java/com/ccps/backend/service/TenancyAgreementPdfService.java`
- Create: `backend/src/test/java/com/ccps/backend/service/TenancyAgreementPdfServiceTest.java`
- Add: `backend/src/main/resources/contract-templates/conlay-tenancy-agreement-template.pdf`

- [ ] Write tests for 22-page output, PDF readability, no sample names, and informative `.pdf` filename.
- [ ] Run the targeted test and confirm it fails because the service does not exist.
- [ ] Implement PDF stamping for the cover, repeated header, first schedule pages, and agreement signature/date fields while preserving all other pages.
- [ ] Run the targeted test and confirm it passes.

### Task 2: Endpoint and frontend download

**Files:**
- Modify: `backend/src/main/java/com/ccps/backend/controller/AdminContractTemplateController.java`
- Modify: `frontend/src/services/propertyApi.js`
- Modify: `frontend/src/components/AdminTenancyWorkspace.vue`
- Modify: `frontend/src/i18n/index.js`
- Modify: `frontend/tests/admin-tenancy-contract-i18n.test.mjs`

- [ ] Route `tenancy-agreement` to the PDF service and return `application/pdf` with a `.pdf` filename.
- [ ] Make the client expect `.pdf` for tenancy agreements and show the PDF download label.
- [ ] Keep the existing OTR and authorization PDF behavior unchanged.
- [ ] Add frontend assertions for the PDF route and extension.
- [ ] Run backend tests, frontend tests, and Vite build.
