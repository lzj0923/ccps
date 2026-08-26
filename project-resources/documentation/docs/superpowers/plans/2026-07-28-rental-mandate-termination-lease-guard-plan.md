# 出租委托终止租约检查实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 终止出租委托前阻止仍有有效租约的单位，并显示明确原因。

**Architecture:** 后端在状态变更事务内查询同一单位的有效租约，作为最终业务约束；前端沿用现有错误提示，不改变委托状态。通过服务层单元测试锁定“有租约拒绝、无租约继续”的两种结果。

**Tech Stack:** Spring Boot 3.4、MyBatis 注解 Mapper、JUnit 5、Vue 3。

## Global Constraints

- 有效租约必须同时满足 `status='active'`、`start_date <= CURRENT_DATE`、`end_date IS NULL OR end_date >= CURRENT_DATE`。
- 有效租约存在时，终止请求必须失败，且不能写入状态、服务或审计变更。

### Task 1: 后端有效租约检查

**Files:**
- Modify: `backend/src/main/java/com/ccps/backend/mapper/AdminRentalMandateMapper.java`
- Modify: `backend/src/main/java/com/ccps/backend/service/AdminRentalMandateService.java`
- Test: `backend/src/test/java/com/ccps/backend/service/AdminRentalMandateServiceTest.java`

- [x] **Step 1: Write the failing test**

在 `AdminRentalMandateServiceTest` 增加测试：模拟 active 委托、同单位有 1 笔有效租约，调用 `updateStatus(..., terminated, ...)`，断言抛出包含“有效租约”的 `ResponseStatusException`，并验证 `updateStatus` 与 `updateRentalService` 均未调用。

- [x] **Step 2: Run the test to verify it fails**

Run: `mvn -q -f backend/pom.xml "-Dtest=AdminRentalMandateServiceTest" test`

Expected: 编译或断言失败，因为 Mapper 尚无有效租约查询及服务层拦截。

- [x] **Step 3: Write minimal implementation**

在 Mapper 增加 `countActiveLeasesForMandate(Long mandateId)`，按有效租约条件查询；在 `updateStatus` 处理 `terminated` 且当前状态为 active/suspended 时先查询，数量大于 0 就抛出明确的 `409` 错误。

- [x] **Step 4: Run the test to verify it passes**

Run: `mvn -q -f backend/pom.xml "-Dtest=AdminRentalMandateServiceTest" test`

Expected: PASS。

### Task 2: 前端提示与回归验证

**Files:**
- Modify: `frontend/src/components/AdminRentalMandateWorkspace.vue`
- Modify: `project-resources/documentation/docs/客戶問題每日紀錄.md`

- [x] **Step 1: Preserve the existing termination flow**

继续先要求填写终止原因；后端返回错误时使用现有 toast 显示，不关闭状态列表或伪造成功提示。

- [x] **Step 2: Build the frontend**

Run: `npm exec -- vite build` in `frontend`。

Expected: 构建成功。

- [x] **Step 3: Run the full backend test suite**

Run: `mvn -q -f backend/pom.xml test`。

Expected: 全部测试通过。

- [x] **Step 4: Record the user-facing change**

在每日问题日志记录：有效租约会阻止终止委托，用户需先结束或转移租约。
