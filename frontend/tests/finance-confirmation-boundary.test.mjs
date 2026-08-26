import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { formatDate, formatDateTime } from '../src/utils/dateFormat.js';

const read = file => readFileSync(new URL(`../src/${file}`, import.meta.url), 'utf8');

test('all visible finance dates use DD/MM/YYYY', () => {
  assert.equal(formatDate('2026-08-25'), '25/08/2026');
  assert.equal(formatDateTime('2026-08-25T14:30:59'), '25/08/2026 14:30');
  assert.equal(formatDate(null), '—');
});

test('finance confirmations carry the finance-selected dates', () => {
  const api = read('services/propertyApi.js');
  const property = read('components/AdminFinanceWorkspace.vue');
  const reserve = read('components/AdminReserveFinanceWorkspace.vue');
  const expense = read('components/AdminExpenseFinanceWorkspace.vue');
  assert.match(api, /JSON\.stringify\(\{ transactionDate, receiptDate, note \}\)/);
  assert.match(property, /实际收款日期[\s\S]*财务入账日期/);
  assert.match(reserve, /财务入账日期[\s\S]*confirmAdminFinanceReview/);
  assert.match(expense, /财务入账日期[\s\S]*confirmAdminFinanceReview/);
});

test('non-finance workspaces route users to the central finance module', () => {
  const deposit = read('components/AdminDepositWorkspace.vue');
  const process = read('components/AdminPropertyProcessWorkspace.vue');
  const detail = read('components/AdminPropertyDetailWorkspace.vue');
  assert.doesNotMatch(deposit, /confirmAdminFinanceReview/);
  assert.match(deposit, /selectModule\('adminFinance'\)/);
  assert.doesNotMatch(process, /await confirmAdminFinanceReview/);
  assert.match(process, /goToCentralFinance/);
  assert.doesNotMatch(detail, /await confirmAdminRentCollection/);
  assert.match(detail, /adminFinanceMode='rent'/);
});
