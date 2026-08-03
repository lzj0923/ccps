# 租赁合约读取房产交接清单 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让租赁合约 PDF 的 Inventory List 读取对应房产的启用交接清单。

**Architecture:** 由交接清单 Mapper 按租约查询清单项目；模板控制器把项目传给 PDF 服务；PDF 服务覆盖模板第 15–17 页的固定库存文字并按分类流式绘制动态清单。

**Tech Stack:** Spring Boot 3.4、MyBatis、OpenPDF、JUnit 5。

## Global Constraints

- 只读取 `enabled=true` 的房产交接清单项目。
- 保留客户模板原有 22 页结构和其他条款。
- 入住／退租栏保持空白，数量读取清单的默认数量。

### Task 1: 查询租约对应的房产清单

**Files:**
- Modify: `backend/src/main/java/com/ccps/backend/mapper/AdminPropertyHandoverChecklistMapper.java`
- Modify: `backend/src/main/java/com/ccps/backend/controller/AdminContractTemplateController.java`
- Test: `backend/src/test/java/com/ccps/backend/service/TenancyAgreementPdfServiceTest.java`

- [x] **Step 1: Write the failing PDF test**

构造启用和停用的 Inventory 项目，调用 PDF 服务，提取第 15–17 页文字；断言启用项目存在、停用项目不存在。

- [x] **Step 2: Run the focused test and confirm it fails**

Run: `mvn -q -f backend/pom.xml "-Dtest=TenancyAgreementPdfServiceTest" test`

Expected: FAIL because the PDF service does not accept inventory rows.

- [x] **Step 3: Add the lease-to-checklist query and controller wiring**

新增 `rowsForLease(Long leaseId)`，通过 `leases.unit_id = owner_units.unit_id` 找到房产清单并过滤 `enabled=1`；控制器生成租赁合约时把结果传给 PDF 服务。

### Task 2: 动态绘制合约 Inventory List

**Files:**
- Modify: `backend/src/main/java/com/ccps/backend/service/TenancyAgreementPdfService.java`
- Test: `backend/src/test/java/com/ccps/backend/service/TenancyAgreementPdfServiceTest.java`

- [x] **Step 1: Implement dynamic page overlays**

覆盖第 15–17 页原库存区域，按分类绘制标题、列标题、序号、项目名称和默认数量；项目按清单顺序跨页排列。

- [x] **Step 2: Run focused PDF tests**

Run: `mvn -q -f backend/pom.xml "-Dtest=TenancyAgreementPdfServiceTest" test`

Expected: PASS and page count remains 22.

### Task 3: Full verification and record

**Files:**
- Modify: `docs/客戶問題每日紀錄.md`

- [x] **Step 1: Run backend compilation and focused regression tests**

Run: `mvn -q -f backend/pom.xml test`

Expected: All tests pass.

- [x] **Step 2: Build the frontend**

Run: `npm exec -- vite build` in `frontend`.

Expected: Build succeeds.

- [x] **Step 3: Record the completed change**

记录租赁合约已读取房产交接清单，并保留模板结构。
