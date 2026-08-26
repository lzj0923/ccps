# 模板字段定位 PDF 私有字段错误 - 修复报告

## BUG描述
- 页面/功能：附件签约 / 模板字段定位
- 操作步骤：点击“定位模板字段”，等待 PDF 模板载入
- 期望结果：显示 PDF 页面和可拖动的字段位置
- 实际结果：页面空白，并提示 `Cannot read from private field`

## 复现步骤
1. 使用 PDF.js 读取模板并取得 `PDFDocumentProxy`。
2. 将该对象放入 Vue 响应式状态。
3. 通过响应式状态调用 `getPage(1)`。
4. 稳定复现 PDF.js 私有字段无法读取的错误。

## 根因分析
- 表象：模板定位弹窗无法显示 PDF。
- 根因：Vue 将 PDF.js 文档实例包装为响应式 Proxy；PDF.js 使用 JavaScript 私有字段保存页面状态，私有字段不能通过 Proxy 实例访问。
- 定位结果：直接调用原始 PDF 对象正常，经过 `reactive` 包装后调用 `getPage` 可稳定复现。

## 修复方案
- 修改文件：`frontend/src/components/AdminRentalSigningWorkspace.vue`
- 使用 Vue `markRaw` 标记 PDF.js 文档对象，确保它不会被转换为响应式 Proxy。
- 修改仅影响 PDF 模板预览对象，不改变字段坐标、模板版本或文件生成逻辑。

## 单元测试
- 测试文件：`frontend/tests/admin-template-pdf-reactivity.test.mjs`
- 测试场景：PDF.js 文档保存到 Vue 状态后仍可读取第一页。
- 修复前：失败，并复现私有字段错误。
- 修复后：通过。

## 手动验证步骤
1. 打开“附件签约”。
2. 点击代租管合约或授权委托书的“定位模板字段”。
3. 确认 PDF 页面正常显示。
4. 切换页面并拖动字段，确认预览不会变为空白。
5. 保存字段位置后重新打开，确认位置保留。

## 影响范围
- 直接影响：附件签约的 PDF 模板字段定位。
- 不影响：历史文件、电子签署、模板版本及其他附件生成。
- 回归结果：相关测试 10 项通过，ESLint 通过，前端生产构建通过。
