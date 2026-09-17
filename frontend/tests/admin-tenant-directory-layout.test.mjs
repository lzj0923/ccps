import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const workspace = readFileSync(
  new URL('../src/components/AdminTenantDirectoryWorkspace.vue', import.meta.url),
  'utf8',
);

test('从租房流程精确打开当前租客编辑框', () => {
  assert.match(workspace, /applyProcessRoute/);
  assert.match(workspace, /workflow !== 'edit' \|\| !tenantId/);
  assert.match(workspace, /String\(row\.tenantId\) === String\(tenantId\)/);
  assert.match(workspace, /if \(tenant\) this\.openEdit\(tenant\)/);
});

test('租客搜索框只由外层搜索容器绘制边框', () => {
  assert.match(workspace, /class="tenant-directory-search"/);
  assert.match(workspace, /class="tenant-directory-search-input"/);
  assert.match(
    workspace,
    /\.tenant-directory\s+\.directory-filter\s+>\s+\.tenant-directory-search\s+>\s+\.tenant-directory-search-input\s*\{[\s\S]{0,300}border:\s*0\s*!important;[\s\S]{0,300}background:\s*transparent\s*!important;/,
  );
});

test('租客操作按钮使用页面专用容器并保持清晰间距', () => {
  assert.match(workspace, /class="tenant-row-actions"/);
  assert.doesNotMatch(workspace, /class="row-actions"/);
  assert.match(
    workspace,
    /\.tenant-row-actions\s*\{[\s\S]{0,240}gap:\s*var\(--space-admin-xs\);/,
  );
});
