# 房產流程中心 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在管理端新增獨立一級「流程中心」，讓使用者先選擇房產，再查看租房流程進度並直接跳到對應操作。

**Architecture:** 流程中心作為獨立管理端模組與路由，左側先選擇房產，右側沿用既有房產、業主、合約與租約資料計算流程狀態；每張卡透過既有房產詳情二級頁籤入口導航，不新增後端 API。

**Tech Stack:** Vue 3、既有單檔元件、現有 CSS；以 Vite build 作為驗證。

## Global Constraints

- 不新增後端資料表或 API。
- 不改變既有房產詳情二級功能。
- 流程狀態需明確區分已完成、進行中、待處理及未開始。
- 所有操作入口需導向現有工作台頁籤。

### Task 1: 新增獨立流程中心頁

**Files:**
- Add: `frontend/src/components/AdminPropertyProcessWorkspace.vue`
- Modify: `frontend/src/pages/AdminPage.vue`
- Modify: `frontend/src/router.js`
- Modify: `frontend/src/data/dashboardData.js`
- Modify: `frontend/src/components/SidebarNav.vue`
- Modify: `frontend/src/i18n/index.js`

**Steps:**

- [x] 在管理端導航新增獨立「流程中心」一級入口與路由。
- [x] 新增房產選擇、流程摘要、進度統計、流程時間線及下一步提示區塊。
- [x] 以現有房產／合約／租約資料計算展示狀態，並為各流程卡提供直接跳轉操作。
- [x] 新增 scoped styles，維持既有管理端視覺及響應式版面。
- [x] 執行 `npm exec -- vite build` 驗證編譯。
