import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const read = relativePath => readFileSync(fileURLToPath(new URL(relativePath, import.meta.url)), 'utf8');

test('租客启停使用状态专用接口，不重复提交历史联系方式', () => {
  const workspace = read('../src/components/AdminTenantDirectoryWorkspace.vue');
  const api = read('../src/services/propertyApi.js');
  const controller = read('../../backend/src/main/java/com/ccps/backend/controller/AdminTenancyController.java');

  assert.match(workspace, /updateAdminTenantStatus\(row\.tenantId, row\.status === 'active' \? 'inactive' : 'active'\)/);
  assert.doesNotMatch(workspace, /async toggleStatus\(row\)[\s\S]{0,300}phone:\s*row\.phone/);
  assert.match(api, /method: 'PATCH'[\s\S]*JSON\.stringify\(\{ status \}\)/);
  assert.match(controller, /@PatchMapping\("\/tenants\/\{tenantId\}\/status"\)/);
});

test('管理员编辑表单使用独立状态并完整回填联系方式', () => {
  const source = read('../src/components/AdminAccountsWorkspace.vue');

  assert.match(source, /accountForm:\s*emptyForm\(\)/);
  assert.match(source, /this\.accountForm = \{[^}]*email: account\.email \|\| ''[^}]*phone: account\.phone \|\| ''/);
  assert.doesNotMatch(source, /\bform:\s*emptyForm\(\)/);
});

test('登录页不展示测试账号且区分管理员与业主登录按钮', () => {
  const source = read('../src/pages/LoginPage.vue');

  assert.doesNotMatch(source, /class="login-demo"/);
  assert.doesNotMatch(source, /demoUsername|demoLabel/);
  assert.match(source, /isAdminPortal \? this\.\$t\('login\.signIn'\) : this\.\$t\('login\.ownerSignIn'\)/);
});

test('导航、指标、报表和项目表单不再触发已知 Vue 警告', () => {
  const sidebar = read('../src/components/SidebarNav.vue');
  const metrics = read('../src/components/MetricsGrid.vue');
  const reports = read('../src/components/AdminReportWorkspace.vue');
  const projects = read('../src/components/AdminProjectManagementWorkspace.vue');

  assert.match(sidebar, /components:\s*\{ ChevronDown \}/);
  assert.match(metrics, /:key="`\$\{metric\.label\}-\$\{index\}`"/);
  assert.match(reports, /typeLabel\(v\) \{\s*if \(!v\) return '';/);
  assert.match(reports, /this\.\$te\(key\)/);
  assert.match(projects, /projectForm:\s*emptyForm\(\)/);
  assert.doesNotMatch(projects, /\bform:\s*emptyForm\(\)/);
});
