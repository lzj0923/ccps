import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const workspace = readFileSync(new URL('../src/components/AdminMaintenanceWorkspace.vue', import.meta.url), 'utf8');
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

test('owner termination disables every new direct payment action', () => {
  assert.match(workspace, /:disabled="!expenseDirectPaymentAllowed"/);
  assert.match(workspace, /:disabled="!handlingDirectPaymentAllowed"/);
  assert.match(workspace, /业主已解约，不能再新增代付款/);
  assert.match(workspace, /业主已解约，不能再提交代付款/);
});
