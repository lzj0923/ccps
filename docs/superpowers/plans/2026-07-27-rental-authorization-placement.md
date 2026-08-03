# 租赁授权委托书入口调整 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将授权委托书生成入口从租客与租金移动到出租委托详情。

**Architecture:** 复用现有合同模板服务和 `AdminTenancyWorkspace` 的弹窗/生成逻辑，在出租委托组件中增加同样的生成入口和预填数据，不改后端 API。

**Tech Stack:** Vue 3、现有 i18n、现有 `propertyApi` 合同模板请求。

## Global Constraints

- 不新增数据库字段或后端接口。
- 不改变 OTR 出价函现有入口和行为。
- 所有新增界面文字必须使用 i18n。

### Task 1: 移除租客页面授权入口

**Files:**
- Modify: `frontend/src/components/AdminTenancyWorkspace.vue`
- Test: `frontend/tests/admin-tenancy-contract-i18n.test.mjs`

- [x] 删除授权委托书按钮及其仅用于该按钮的调用路径。
- [x] 保留 OTR 按钮、弹窗和 i18n 词条。
- [x] 更新测试，确认租客页面没有授权按钮而有 OTR 按钮。

### Task 2: 在出租委托详情增加授权生成入口

**Files:**
- Modify: `frontend/src/components/AdminRentalMandateWorkspace.vue`
- Test: `frontend/tests/admin-rental-mandate-authorization.test.mjs`

- [x] 增加 i18n 按钮和弹窗文字引用。
- [x] 在每条启用状态的委托记录操作列显示按钮，并预填业主、物业地址、委托起止日期。
- [x] 调用现有 `generateAdminContractTemplate('authorization', fields)` 并下载返回 PDF。
- [x] 同时将生成的 PDF 归档到当前出租委托的授权文件列表。
- [x] 未选中委托时不显示按钮。

### Task 3: 回归验证

- [x] 运行前端行为测试。
- [x] 运行 `npm exec -- vite build`。
- [x] 运行 `git diff --check`。
