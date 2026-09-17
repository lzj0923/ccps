import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const workspace = readFileSync(new URL('../src/components/AdminMaintenanceWorkspace.vue', import.meta.url), 'utf8');
const toolbar = readFileSync(new URL('../src/components/ModuleToolbar.vue', import.meta.url), 'utf8');
const state = readFileSync(new URL('../src/composables/dashboardState.js', import.meta.url), 'utf8');
const api = readFileSync(new URL('../src/services/propertyApi.js', import.meta.url), 'utf8');
const financeMapper = readFileSync(new URL('../../backend/src/main/java/com/ccps/backend/mapper/AdminFinanceReviewMapper.java', import.meta.url), 'utf8');

test('maintenance toolbar opens the monthly cashflow workspace', () => {
  assert.match(toolbar, /showMonthlyCashflowAction[\s\S]*currentId === 'adminMaintenance'/);
  assert.match(toolbar, /@click="triggerMonthlyCashflowAction">\{\{ \$t\('finance\.monthlyCashflow'\) \}\}/);
  assert.match(toolbar, /adminMaintenanceMonthlyCashflowNonce \+= 1/);
  assert.match(state, /adminMaintenanceMonthlyCashflowNonce: 0/);
  assert.match(workspace, /'page\.adminMaintenanceMonthlyCashflowNonce'\(\) \{ this\.openMonthlyCashflow\(\); \}/);
});

test('monthly cashflow supports unit search, month selection, and existing property ledger API', () => {
  assert.match(workspace, /v-model\.trim="monthlyUnitSearch"/);
  assert.match(workspace, /v-model="monthlyMonth" type="month"/);
  assert.match(workspace, /monthlyFilteredUnits\(\)[\s\S]*unit\.projectName[\s\S]*unit\.unitNo[\s\S]*unit\.ownerName[\s\S]*unit\.tenantName/);
  assert.match(workspace, /fetchAdminPropertyCashflows\(unit\.ownerId, unit\.ownerUnitId, this\.monthlyMonth\)/);
  assert.match(api, /export function fetchAdminPropertyCashflows\(ownerId, ownerUnitId, month = ''\)[\s\S]*income-expenses/);
  assert.match(workspace, /String\(item\.occurredOn \|\| ''\)\.slice\(0, 7\) !== month/);
  assert.match(workspace, /item\.financeRecordId \? `finance:\$\{item\.financeRecordId\}` : `cashflow:\$\{item\.id\}`/);
});

test('monthly list includes all rows while totals only include confirmed operating rows', () => {
  assert.match(workspace, /monthlyConfirmedCashflowRows\(\) \{ return this\.monthlyCashflowRows\.filter\(item => item\.confirmationStatus === 'confirmed' && !\['deposit', 'deposit_refund'\]\.includes\(item\.category\)\); \}/);
  assert.match(workspace, /monthlyIncome\(\)[\s\S]*direction === 'income'/);
  assert.match(workspace, /monthlyExpense\(\)[\s\S]*direction === 'expense'/);
  assert.match(workspace, /v-for="item in monthlyCashflowRows"/);
  assert.match(financeMapper, /UPDATE cashflow_entries SET occurred_on=#\{transactionDate\} WHERE finance_record_id=#\{financeRecordId\}/);
});

test('each monthly cashflow exposes the requested business details and proof download', () => {
  for (const key of ['itemDescription', 'cashflowDate', 'amount', 'notes', 'paymentMethod', 'transactionNo', 'dataSource', 'paymentStatus']) {
    assert.match(workspace, new RegExp(`finance\\.${key}`));
  }
  assert.match(workspace, /openMonthlyCashflowDetail\(item\)/);
  assert.match(workspace, /downloadAdminPropertyCashflowProof\(unit\.ownerId, unit\.ownerUnitId, item\.id\)/);
});
