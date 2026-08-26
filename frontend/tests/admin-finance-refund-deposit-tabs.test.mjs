import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const workspace = readFileSync(new URL('../src/components/AdminFinanceWorkspace.vue', import.meta.url), 'utf8');
const expense = readFileSync(new URL('../src/components/AdminExpenseFinanceWorkspace.vue', import.meta.url), 'utf8');

test('reserve refunds and tenant deposits are independent finance confirmation tabs', () => {
  assert.match(workspace, /switchFinanceType\('reserve_refund'\)/);
  assert.match(workspace, /switchFinanceType\('tenant_deposit'\)/);
  assert.match(workspace, /:review-type="financeType"/);
  assert.match(expense, /type: this\.reviewType/);
  assert.match(expense, /fetchAdminFinanceProjects\(this\.reviewType\)/);
});
