# 出租委托到期自动停用实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将结束日期已过的启用或暂停出租委托自动标记为已到期停用，并释放房产供新委托使用。

**Architecture:** 后端以幂等同步方法统一处理到期状态，在列表、选项和创建前调用；数据库新增 `expired` 状态约束，服务记录同步结束。前端只对 `active`、`suspended` 显示原有操作，`expired` 仅可查看。

**Tech Stack:** Spring Boot 3.4、MyBatis、MySQL、Vue 3、Vite、JUnit 5。

## Global Constraints

- `end_date < 当前日期` 才算到期，结束日期当天仍有效。
- 自动到期状态使用 `expired`，不替代人工 `terminated`。
- 自动处理必须幂等，不重复写入状态历史或审计。
- 到期委托保留查看、历史和附件下载，不允许状态操作、授权委托书和交接操作。

---

### Task 1: 更新数据库状态约束

**Files:**
- Modify: `database/schema.sql:252-282`
- Modify: 运行中的 MySQL `rental_mandates` 状态约束

**Interfaces:** `rental_mandates.status` 接受 `expired`。

- [ ] **Step 1: 修改建表约束**

将 `chk_rental_mandates_status` 的允许值加入 `expired`。

- [ ] **Step 2: 执行现有数据库的安全迁移**

先读取现有约束名称，再删除并重建同名约束，确认已有数据不变。

- [ ] **Step 3: 验证约束**

查询 `SHOW CREATE TABLE rental_mandates`，确认 `expired` 出现在约束中。

### Task 2: 后端增加到期同步

**Files:**
- Modify: `backend/src/main/java/com/ccps/backend/mapper/AdminRentalMandateMapper.java`
- Modify: `backend/src/main/java/com/ccps/backend/service/AdminRentalMandateService.java`
- Test: `backend/src/test/java/com/ccps/backend/service/AdminRentalMandateServiceTest.java`

**Interfaces:** 新增 `expireEndedMandates()`，将 `active`/`suspended` 且 `end_date < CURRENT_DATE` 的记录改为 `expired`，同步结束出租服务，并写入历史和审计。

- [ ] **Step 1: 写服务层失败测试**

覆盖：到期记录转为 `expired`、结束服务；重复调用不再更新；结束日期为今天不转换。

- [ ] **Step 2: 增加 mapper 查询和状态更新方法**

使用数据库日期判断，并限定原状态为 `active` 或 `suspended`，避免覆盖人工终止或草稿。

- [ ] **Step 3: 实现事务同步方法**

锁定候选记录，逐笔更新状态、写入 `rental_mandate_status_history`，写入 `audit_logs`，并调用现有 `updateRentalService(id, "terminated")` 结束出租服务。

- [ ] **Step 4: 在列表、选项、创建前调用同步**

在 `find()`、`options()`、`create()` 开始处调用同步；创建时再次执行，防止并发下旧委托继续占用房产。

- [ ] **Step 5: 运行服务层测试**

运行 `mvn -q -f backend/pom.xml -Dtest=AdminRentalMandateServiceTest test`，确认新增测试通过。

### Task 3: 后端释放房产并保留历史

**Files:**
- Modify: `backend/src/main/java/com/ccps/backend/mapper/AdminRentalMandateMapper.java`
- Modify: `backend/src/main/java/com/ccps/backend/service/AdminRentalMandateService.java`

**Interfaces:** 新委托可选房产和重叠检查忽略 `expired`，但历史列表仍能按状态查询到 `expired`。

- [ ] **Step 1: 调整可选房产查询**

保留现有排除条件中的 `draft`、`pending_review`、`active`、`suspended`，不加入 `expired`。

- [ ] **Step 2: 调整创建前房产占用检查**

保持相同状态集合，确保到期委托释放房产。

- [ ] **Step 3: 调整重叠检查**

只检查 `pending_review`、`active`、`suspended`，不检查 `expired`。

### Task 4: 前端显示停用状态并隐藏操作

**Files:**
- Modify: `frontend/src/components/AdminRentalMandateWorkspace.vue`
- Modify: `frontend/src/i18n/*` 中出租委托翻译键

**Interfaces:** `expired` 显示「已到期／停用」，只保留行点击、历史和附件下载。

- [ ] **Step 1: 增加状态筛选选项与标签**

加入 `expired` 过滤项和三语标签。

- [ ] **Step 2: 限制操作按钮**

现有 `v-if` 只允许 `active`/`suspended` 的暂停、终止、授权和交接按钮；`expired` 不显示这些按钮。

- [ ] **Step 3: 运行前端构建**

运行 `npm exec -- vite build`，确认无模板或翻译错误。

### Task 5: 回归验证与记录

**Files:**
- Modify: `docs/客戶問題每日紀錄.md`

- [ ] **Step 1: 检查数据库样本**

确认过期委托变为 `expired`，状态历史有一条自动到期记录，服务状态已结束。

- [ ] **Step 2: 验证新增委托流程**

确认该房产出现在新增委托下拉框，并可成功建立新草稿；同一房产不会允许两个有效委托重叠。

- [ ] **Step 3: 更新每日记录**

记录到期自动停用、房产释放和验证结果。

- [ ] **Step 4: 运行最终检查**

运行 `git diff --check`、后端测试和前端构建。
