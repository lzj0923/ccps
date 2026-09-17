import { matchLocalizedSource } from './helpers/localizedSource.mjs';
import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const panel = readFileSync(new URL('../src/components/AdminFundOperationsPanel.vue', import.meta.url), 'utf8');
const api = readFileSync(new URL('../src/services/propertyApi.js', import.meta.url), 'utf8');
const reserve = readFileSync(new URL('../src/components/AdminReserveWorkspace.vue', import.meta.url), 'utf8');
const contracts = readFileSync(new URL('../src/components/AdminPropertyDetailWorkspace.vue', import.meta.url), 'utf8');
const accounts = readFileSync(new URL('../src/components/AdminAccountsWorkspace.vue', import.meta.url), 'utf8');
const adminPermissions = readFileSync(new URL('../src/utils/adminPermissions.js', import.meta.url), 'utf8');

test('fund operations panel exposes transfer, remittance plan and finance batch workflows', () => {
  matchLocalizedSource(panel, /新建内部调拨/);
  matchLocalizedSource(panel, /暂缓汇款/);
  matchLocalizedSource(panel, /税金留存/);
  matchLocalizedSource(panel, /提交财务确认/);
  matchLocalizedSource(api, /fund-operations\/internal-transfers/);
  matchLocalizedSource(api, /fund-operations\/remittance-batches/);
  matchLocalizedSource(reserve, /AdminFundOperationsPanel/);
  matchLocalizedSource(reserve, /ref="detailDialog"/);
  matchLocalizedSource(reserve, /@click\.stop="openDetail\(account\.id\)"/);
  matchLocalizedSource(reserve, /ref="fundOperationsDialog"[\s\S]*<AdminFundOperationsPanel/);
  matchLocalizedSource(reserve, /@click="openFundOperations"/);
});

test('extended contract and employee states are editable in admin workspaces', () => {
  for (const status of ['lost', 'voided', 'returned', 'archived']) matchLocalizedSource(contracts, new RegExp(status));
  for (const field of ['employeeNo', 'department', 'jobTitle', 'hireDate', 'leaveDate']) {
    matchLocalizedSource(accounts, new RegExp(field));
  }
});

test('admin account list does not append an open-ended 至今 date range', () => {
  assert.doesNotMatch(accounts, /至\s*\{\{\s*account\.leaveDate\s*\|\|\s*['"]今['"]\s*\}\}/);
});

test('admin account list avoids a redundant department and job title column', () => {
  assert.doesNotMatch(accounts, /<th>部门\s*\/\s*职位<\/th>/);
  assert.doesNotMatch(accounts, /<td>\{\{\s*account\.department/);
});

test('admin role is presented to users as a position', () => {
  matchLocalizedSource(accounts, /<th>职位<\/th>/);
  matchLocalizedSource(accounts, /<label>职位<select v-model="accountForm\.staffRole"/);
  assert.doesNotMatch(accounts, /<label>职位<input v-model\.trim="accountForm\.jobTitle"/);
  assert.doesNotMatch(accounts, /管理员类型/);
});

test('only the super admin position keeps the 管理员 suffix', () => {
  matchLocalizedSource(adminPermissions, /SUPER_ADMIN:\s*['"]超级管理员['"]/);
  for (const position of ['财务', '业务', '客服', '行政']) {
    matchLocalizedSource(adminPermissions, new RegExp(`['"]${position}['"]`));
    assert.doesNotMatch(adminPermissions, new RegExp(`${position}管理员`));
  }
});
