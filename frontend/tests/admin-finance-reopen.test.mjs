import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const component = (name) => readFileSync(new URL(`../src/components/${name}`, import.meta.url), 'utf8');

test('all finance history workspaces expose the reopen action', () => {
  for (const name of [
    'AdminFinanceWorkspace.vue',
    'AdminExpenseFinanceWorkspace.vue',
    'AdminReserveFinanceWorkspace.vue',
    'AdminRentFinanceWorkspace.vue'
  ]) {
    const source = component(name);
    assert.match(source, /reopenAdminFinanceReview/);
    assert.match(source, /finance\.reopen/);
  }
});

test('rent and reserve reopening require a reason and reload their history', () => {
  for (const name of ['AdminReserveFinanceWorkspace.vue', 'AdminRentFinanceWorkspace.vue']) {
    const source = component(name);
    assert.match(source, /v-model\.trim="reopenNote"/);
    assert.match(source, /await reopenAdminFinanceReview\(this\.selectedRow\.id, this\.reopenNote\)/);
    assert.match(source, /await this\.loadData\(\)/);
  }
});
