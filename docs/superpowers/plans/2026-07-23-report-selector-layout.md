# 報表類型選擇區 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 將報表類型改為無橫向捲動的卡片網格，保留既有的選取與產生報表流程。

**Architecture:** 僅調整 `AdminReportWorkspace.vue` 的類型選擇標記與 scoped CSS；資料來源、`selectedType` 綁定、產生報表 API 均不變。

**Tech Stack:** Vue 3、Scoped CSS、Node test、Vite。

## Global Constraints

保留既有報表類型、鍵盤可聚焦按鈕和 `selectedType` 選取行為。

---

### Task 1: 報表類型卡片網格

**Files:**
- Modify: `frontend/src/components/AdminReportWorkspace.vue`
- Test: `frontend/tests/admin-report-selector-layout.test.mjs`

- [ ] **Step 1: Write the failing test**

Assert the selector has a card class, uses a responsive grid, has no horizontal overflow, and preserves the active selector binding.

- [ ] **Step 2: Run test to verify it fails**

Run: `node --test tests/admin-report-selector-layout.test.mjs`

- [ ] **Step 3: Write minimal implementation**

Add card metadata classes in the existing button loop and replace the horizontal flex strip with a CSS grid using `repeat(auto-fit, minmax(190px, 1fr))`.

- [ ] **Step 4: Run test and build**

Run: `node --test tests/admin-report-selector-layout.test.mjs && npm run build`

