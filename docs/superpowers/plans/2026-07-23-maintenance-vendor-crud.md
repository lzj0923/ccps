# 修繕服務商 CRUD Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在修繕報告中新增二級服務商管理頁，支援新增、查看、修改、停用，並同步修繕工單選單。

**Architecture:** 後端新增管理員服務商 REST API，沿用現有 `vendors` 表及管理員登入權限；前端新增獨立 `AdminVendorWorkspace.vue`，從修繕頁按鈕進入二級路徑，返回後重新載入服務商選項。

**Tech Stack:** Spring Boot/MyBatis、MySQL、Vue 3、Vite、現有 `propertyApi` request 封裝。

## Global Constraints

- 停用取代實體刪除，歷史維修記錄不得被破壞。
- 服務商名稱必填；編號由後端自動產生且唯一。
- 管理員接口必須沿用現有登入及權限檢查。

---

### Task 1: 後端服務商 DTO、Mapper 與 API

**Files:**
- Create: `backend/src/main/java/com/ccps/backend/dto/AdminVendorRequest.java`
- Create: `backend/src/main/java/com/ccps/backend/dto/AdminVendorResponse.java`
- Create: `backend/src/main/java/com/ccps/backend/controller/AdminVendorController.java`
- Modify: `backend/src/main/java/com/ccps/backend/mapper/AdminMaintenanceMapper.java`
- Modify: `backend/src/main/java/com/ccps/backend/service/AdminMaintenanceService.java`

**Interfaces:**
- `GET /api/admin/vendors` returns active and inactive vendors for the management page.
- `POST /api/admin/vendors` accepts `name`, `contactName`, `phone`, `email` and returns the created vendor.
- `PUT /api/admin/vendors/{id}` updates the same fields and status.
- `DELETE /api/admin/vendors/{id}` sets status to `inactive` and never deletes the row.

- [ ] **Step 1: Add request/response types and validation.**
- [ ] **Step 2: Add mapper queries for list, insert, update and status change.**
- [ ] **Step 3: Add service methods with unique code generation and admin audit event.**
- [ ] **Step 4: Add controller routes using the existing admin authentication conventions.**
- [ ] **Step 5: Run backend tests or document dependency-download blockage.**

### Task 2: 前端 API 與二級服務商管理頁

**Files:**
- Modify: `frontend/src/services/propertyApi.js`
- Create: `frontend/src/components/AdminVendorWorkspace.vue`
- Modify: `frontend/src/pages/AdminPage.vue`
- Modify: `frontend/src/router.js`

- [ ] **Step 1: Add API helpers for list/create/update/deactivate.**
- [ ] **Step 2: Add list table, search, create/edit form, status badge and deactivate action.**
- [ ] **Step 3: Register `/admin/maintenance/vendors` and render the workspace.**
- [ ] **Step 4: Build frontend and verify the management page compiles.**

### Task 3: 修繕報告入口與選單同步

**Files:**
- Modify: `frontend/src/components/AdminMaintenanceWorkspace.vue`
- Modify: `frontend/src/components/AdminPropertyDetailWorkspace.vue`

- [ ] **Step 1: Add a 「服務商管理」 button that navigates to the secondary page.**
- [ ] **Step 2: Reload vendor options when returning to the maintenance page.**
- [ ] **Step 3: Verify new active vendors appear in create/edit maintenance forms and inactive vendors remain visible only in historical details.**

### Task 4: 文件與每日紀錄

**Files:**
- Modify: `docs/客戶問題每日紀錄.md`

- [ ] **Step 1: Record the service-provider CRUD change and verification result.**
- [ ] **Step 2: Run `git diff --check` and final frontend build.**
