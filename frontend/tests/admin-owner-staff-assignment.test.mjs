import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const ownerWorkspace = readFileSync(new URL('../src/components/AdminOwnersWorkspace.vue', import.meta.url), 'utf8');
const api = readFileSync(new URL('../src/services/propertyApi.js', import.meta.url), 'utf8');

test('新增和编辑业主可以多选管理员账号并提交关联编号', () => {
  assert.match(ownerWorkspace, /OwnerStaffMultiSelect/);
  assert.match(ownerWorkspace, /v-model="ownerForm\.responsibleUserIds"/);
  assert.match(ownerWorkspace, /fetchAdminOwnerStaffOptions/);
  assert.match(ownerWorkspace, /responsibleUserIds: \[\]/);
  assert.match(ownerWorkspace, /owner\.responsibleStaff.*map\(staff => staff\.id\)/s);
  assert.match(ownerWorkspace, /\$t\('ownerStaff\.required'\)/);
});

test('工作人员选项使用业主模块的只读接口', () => {
  assert.match(api, /export function fetchAdminOwnerStaffOptions\(\)/);
  assert.match(api, /\/admin\/owners\/staff-options/);
});
