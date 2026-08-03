# 財務確認篩選列位置調整 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 將財務確認的搜尋與篩選工具列移到財務類型切換列下方，使管理員清楚知道篩選作用於目前類型。

**Architecture:** 僅調整既有 Vue 模板的渲染順序與必要的 CSS 間距，不更動狀態、事件、API 或服務端分頁。共用的管理端頂部工具列由 `AdminPage.vue` 組裝，因此在該頁依財務模組調整區塊順序。

**Tech Stack:** Vue 3、CSS、Vite 6

## Global Constraints

- 不改變財務確認 API、搜尋參數、篩選值、分頁或批量操作。
- 不啟動 Spring Boot。
- 工作區含有使用者既有未提交修改，不建立 Git 提交。

---

### Task 1: 調整財務確認工具列順序

**Files:**
- Modify: `frontend/src/pages/AdminPage.vue`
- Modify only if spacing requires it: `frontend/src/admin-theme.css`
- Verify: `frontend/src/pages/AdminPage.vue`

**Interfaces:**
- Consumes: `activeModule`、`globalSearch`、`moduleSearch`、`projectFilter`、`statusFilter` 與既有財務操作事件。
- Produces: 財務類型切換列下方的完整搜尋／篩選／操作工具列，所有事件與資料綁定保持原樣。

- [ ] **Step 1: 記錄調整前模板順序**

Run:

```powershell
rg -n "adminFinance|filter|toolbar|moduleSearch|projectFilter|statusFilter" frontend/src/pages/AdminPage.vue
```

Expected: 財務工具列目前位於財務類型切換列之前。

- [ ] **Step 2: 移動既有模板區塊**

在 `AdminPage.vue` 中把原有搜尋／建案／狀態／操作工具列整體移至財務類型切換列之後。保留原有 `v-if`、`v-model`、`@click`、placeholder 與按鈕，不複製第二套狀態。

- [ ] **Step 3: 核對模板綁定沒有增減**

Run:

```powershell
rg -n "globalSearch|moduleSearch|projectFilter|statusFilter|adminFinance" frontend/src/pages/AdminPage.vue
```

Expected: 原有搜尋、建案、狀態及操作事件仍各保留一份，財務類型切換列先於篩選列出現。

- [ ] **Step 4: 執行前端生產編譯**

Run:

```powershell
cd frontend
npm exec -- vite build
```

Expected: `built` 且退出碼為 0；允許既有的 chunk size 警告。
