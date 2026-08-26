# 附件签约下载 404 与 PDF Worker 加载失败 - 修复报告

## BUG描述
- 页面/功能：管理后台 → 租赁运营 → 附件签约。
- 操作步骤：下载本期租赁合同；打开“模板字段定位”。
- 期望结果：租赁合同正常下载；PDF 模板正常显示。
- 实际结果：合同下载返回 HTTP 404；PDF.js 报 `Failed to fetch dynamically imported module`。

## 复现步骤
1. 选择 `KL / A-10-10`，下载租约 `LEASE-20260817-9BD9EEA430AF4`。
2. 前端请求 `/api/admin/owners/10/properties/28/contracts/19/file`，服务器返回 404 `Contract not found`。
3. 请求正确的租约接口 `/api/admin/tenancy/leases/19/contract?download=true`，服务器返回 200 和 PDF。
4. 请求 `/assets/pdf.worker.min-yatZIOMy.mjs`，文件返回 200，但 `Content-Type` 为 `application/octet-stream`，浏览器拒绝将其作为 ES 模块导入。

## 根因分析
- 租约下载：附件签约列表把合并返回的 `L_LEASE` 租约合同当作 `property_contract_records` 普通合同下载，丢失了 `contractType` 和 `leaseId`，因此调用了错误接口。
- PDF Worker：Nginx 未识别 `.mjs`，使用默认二进制 MIME 类型返回 ES 模块。

## 修复方案
- 修改文件：`frontend/src/components/AdminRentalSigningWorkspace.vue`
  - 文件列表保留 `contractType` 与 `leaseId`。
  - `L_LEASE` 使用 `fetchAdminLeaseContract`；其他合同继续使用原接口。
- 修改文件：`docker/frontend/nginx.conf`
  - 为 `.mjs` 增加独立 location，并返回 `application/javascript`。
- 服务器同步修改实际使用的 Nginx 站点配置，执行 `nginx -t` 后重载。

## 单元测试
- 测试文件：`frontend/tests/admin-rental-signing-workspace.test.mjs`
- 新增用例：
  - `downloads linked lease contracts through the tenancy contract endpoint`
  - `serves pdf worker mjs files with a JavaScript MIME type`
- 修复前：2 项失败；修复后：11/11 通过。
- 前端生产构建：通过。
- 全量前端测试：202 项中 179 项通过，23 项现有测试失败；失败集中在其他页面的既有源码断言及端口占用，与本次两个新增用例无关。

## 手动验证步骤
1. 打开附件签约页面，选择 `KL / A-10-10`。
2. 下载 `LEASE-20260817-9BD9EEA430AF4`，确认返回 PDF 且不再出现 404。
3. 点击“定位模板字段”，确认 PDF 页面正常显示。
4. 在网络面板确认 `.mjs` 返回 200，且 `Content-Type` 为 `application/javascript`。

## 影响范围
- 直接影响：附件签约中的租赁合同下载、PDF 模板字段定位。
- 间接影响：普通合同、委托文件、交接报告和房产附件下载逻辑保持不变。
- 回归测试结果：针对性测试与生产构建通过；全量测试存在 23 项非本次改动导致的既有失败。
