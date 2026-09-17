import { matchLocalizedSource } from './helpers/localizedSource.mjs';
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
  matchLocalizedSource(api, /JSON\.stringify\(\{ transactionDate, receiptDate, note \}\)/);
  matchLocalizedSource(property, /实际收款日期[\s\S]*财务入账日期/);
  matchLocalizedSource(reserve, /财务入账日期[\s\S]*confirmAdminFinanceReview/);
  matchLocalizedSource(expense, /财务入账日期[\s\S]*confirmAdminFinanceReview/);
});

test('non-finance workspaces route users to the central finance module', () => {
  const deposit = read('components/AdminDepositWorkspace.vue');
  const process = read('components/AdminPropertyProcessWorkspace.vue');
  const detail = read('components/AdminPropertyDetailWorkspace.vue');
  assert.doesNotMatch(deposit, /confirmAdminFinanceReview/);
  matchLocalizedSource(deposit, /selectModule\('adminFinance'\)/);
  assert.doesNotMatch(process, /await confirmAdminFinanceReview/);
  matchLocalizedSource(process, /goToCentralFinance/);
  assert.doesNotMatch(detail, /await confirmAdminRentCollection/);
  matchLocalizedSource(detail, /adminFinanceMode='rent'/);
});
