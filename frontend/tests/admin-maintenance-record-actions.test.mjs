import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const workspace = readFileSync(new URL('../src/components/AdminMaintenanceWorkspace.vue', import.meta.url), 'utf8');
const toolbar = readFileSync(new URL('../src/components/ModuleToolbar.vue', import.meta.url), 'utf8');
const api = readFileSync(new URL('../src/services/propertyApi.js', import.meta.url), 'utf8');

test('expense and maintenance rows expose edit and delete actions', () => {
  assert.match(workspace, /@click\.stop="openExpenseEdit\(row\)"/);
  assert.match(workspace, /@click\.stop="removeExpense\(row\)"/);
  assert.match(workspace, /@click\.stop="openMaintenanceEdit\(row\)"/);
  assert.match(workspace, /@click\.stop="removeMaintenance\(row\)"/);
});

test('record action buttons use registered translation keys', () => {
  assert.doesNotMatch(workspace, /\$t\('project\.(?:edit|delete)'\)/);
  assert.equal((workspace.match(/\$t\('projectManagement\.edit'\)/g) || []).length, 4);
  assert.equal((workspace.match(/\$t\('projectManagement\.delete'\)/g) || []).length, 2);
});

test('workspace uses update and delete APIs for both record types', () => {
  assert.match(api, /export function updateAdminExpense/);
  assert.match(api, /export function deleteAdminExpense/);
  assert.match(api, /export function updateAdminMaintenance/);
  assert.match(api, /export function deleteAdminMaintenance/);
});

test('supports undo and a thirty-day recycle bin for deleted records', () => {
  assert.match(workspace, /撤销删除/);
  assert.match(workspace, /回收站/);
  assert.match(workspace, /fetchAdminRecycleBin/);
  assert.match(workspace, /restoreAdminRecycleBin/);
  assert.match(workspace, /lastDeletedRecycleBinId/);
  assert.match(workspace, /已付款.*gray|gray.*已付款/s);
  assert.match(workspace, /待付款.*red|red.*待付款/s);
  assert.match(api, /export function fetchAdminRecycleBin/);
  assert.match(api, /export function restoreAdminRecycleBin/);
});

test('owner termination disables every new direct payment action', () => {
  assert.match(workspace, /:disabled="!expenseDirectPaymentAllowed"/);
  assert.match(workspace, /:disabled="!handlingDirectPaymentAllowed"/);
  assert.match(workspace, /业主已解约，不能再新增代付款/);
  assert.match(workspace, /业主已解约，不能再提交代付款/);
});

test('rejected maintenance orders return to handling with a finance rejection hint', () => {
  assert.match(workspace, /maintenanceRowEditable\(row\)/);
  assert.match(workspace, /row\.confirmationStatus === 'rejected'/);
  assert.match(workspace, /openHandling\(row\)/);
  assert.match(workspace, /legacy\.t_155845fa2240/);
  assert.match(workspace, /fetchAdminMaintenanceDetail\(row\.id\)/);
  assert.match(workspace, /actualAmount: Number\(row\.estimatedAmount \?\? row\.amount \?\? 0\)/);
  assert.match(workspace, /detail\?\.actualAmount \?\? detail\?\.estimatedAmount \?\? row\.estimatedAmount \?\? row\.amount/);
  assert.match(workspace, /previousCompletion\?\.note \|\| detail\?\.description/);
  assert.match(workspace, /!\['completed','cancelled'\]\.includes\(row\.status\) \|\| row\.confirmationStatus === 'rejected'/);
  assert.doesNotMatch(workspace, /重新发起财务确认/);
  assert.match(workspace, /maintenanceRowEditable\(row\) \{ return Boolean\(row\) && row\.editable !== false; \}/);
});

test('district filter uses the project administrative region instead of only the city', () => {
  assert.match(toolbar, /currentId === 'adminMaintenance'/);
  assert.match(toolbar, /v-model="page\.adminMaintenanceDistrictFilter"/);
  assert.match(workspace, /matchesDistrict\(row\.state \|\| row\.city, this\.page\.adminMaintenanceDistrictFilter\)/);
  assert.match(workspace, /value === item \|\| value\.includes\(item\)/);
});
