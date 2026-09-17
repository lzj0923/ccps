import { matchLocalizedSource } from './helpers/localizedSource.mjs';
import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const readComponent = name => readFileSync(new URL(`../src/components/${name}`, import.meta.url), 'utf8');
const main = readComponent('AdminFinanceWorkspace.vue');

test('history mode stays active while switching all four finance types', () => {
  const switchMethod = main.match(/switchFinanceType\(type\) \{[\s\S]*?\},\n\s*switchViewMode/)?.[0] || '';
  assert.doesNotMatch(switchMethod, /adminFinanceViewMode\s*=\s*'pending'/);
  matchLocalizedSource(switchMethod, /this\.viewMode === 'history' \? '全部歷史'/);
  matchLocalizedSource(main, /switchViewMode\('pending'\)/);
});

for (const name of [
  'AdminFinanceWorkspace.vue',
  'AdminRentFinanceWorkspace.vue',
  'AdminReserveFinanceWorkspace.vue',
  'AdminExpenseFinanceWorkspace.vue'
]) {
  test(`${name} history supports selected batch documents and reopen`, () => {
    const source = readComponent(name);
    matchLocalizedSource(source, /v-model="selectedIds"/);
    matchLocalizedSource(source, /批量下载发票/);
    matchLocalizedSource(source, /批量下载收据/);
    matchLocalizedSource(source, /批量退回/);
    matchLocalizedSource(source, /batchReopenAdminFinanceReviews\(ids,/);
  });
}
