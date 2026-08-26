import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const processWorkspace = readFileSync(
  new URL('../src/components/AdminPropertyProcessWorkspace.vue', import.meta.url),
  'utf8',
);
const financeWorkspace = readFileSync(
  new URL('../src/components/AdminFinanceWorkspace.vue', import.meta.url),
  'utf8',
);
const financeList = readFileSync(
  new URL('../src/components/AdminExpenseFinanceWorkspace.vue', import.meta.url),
  'utf8',
);
const i18n = readFileSync(new URL('../src/i18n/index.js', import.meta.url), 'utf8');

test('新增租客费用进入待财务确认且待确认金额不计入账单汇总', () => {
  assert.match(processWorkspace, /operationsConfirmedCharges/);
  assert.match(processWorkspace, /confirmationStatus === 'confirmed'/);
  assert.match(processWorkspace, /createAdminPropertyOperationsCharge/);
  assert.match(processWorkspace, /operationsChargeSubmitted/);
  assert.match(i18n, /尚未计入应收/);
});

test('财务中心把租客费用与收支维修分成独立入口', () => {
  assert.doesNotMatch(financeWorkspace, /switchFinanceType\('tenant_charge'\)/);
  assert.match(financeWorkspace, /adminFinanceMode === 'tenant_charge' \? 'expense'/);
  assert.match(financeWorkspace, /switchFinanceType\('cashflow_maintenance'\)/);
  assert.match(financeWorkspace, />租客费用</);
  assert.match(financeWorkspace, />收支与维修</);
  assert.match(financeList, /reviewType === 'cashflow_maintenance' \? '收支与维修'/);
  assert.doesNotMatch(financeList, /费用承担方/);
  assert.match(i18n, /tenantChargeTab:\s*\['租客费用'/);
});
