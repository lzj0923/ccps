import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const read = path => readFileSync(new URL(`../src/${path}`, import.meta.url), 'utf8');

test('房产列表提供安全删除入口并调用管理端房产接口', () => {
  const workspace = read('components/AdminOwnersWorkspace.vue');
  const api = read('services/propertyApi.js');

  assert.match(api, /export function deleteAdminProperty\(unitId\)/);
  assert.match(api, /`\/admin\/properties\/\$\{unitId\}`,[\s\S]*method: 'DELETE'/);
  assert.match(workspace, /deleteAdminProperty/);
  assert.match(workspace, /deletePropertyConfirm/);
  assert.match(workspace, /@click\.stop="removeProperty\(property\)"/);
  assert.match(workspace, /await deleteAdminProperty\(property\.unitId\)/);
  assert.match(workspace, /await this\.loadOwners\(\)/);
  assert.match(workspace, /await this\.loadProperties\(\)/);
});

test('房产删除文案明确仅无业务资料时允许删除', () => {
  const i18n = read('i18n/index.js');
  const api = read('services/propertyApi.js');

  assert.match(i18n, /仅无业务资料的房产可删除/);
  assert.match(api, /Property with related business records cannot be deleted/);
  assert.match(api, /为保护历史记录不能删除/);
});
