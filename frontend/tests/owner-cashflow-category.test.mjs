import test from 'node:test';
import assert from 'node:assert/strict';
import { cashflowCategory, filterCashflows, groupCashflowMonths } from '../src/utils/ownerPortfolio.js';
import { ownerAppMessages } from '../src/i18n/ownerApp.js';

test('租客押金收入在明细、筛选、月度汇总中一致归类，不再显示其他支出', () => {
  const record = { category: 'deposit', direction: 'income', amount: 1000, occurredOn: '2026-08-07', balanceAfter: 910 };
  const category = cashflowCategory(record.category, record.direction);
  assert.equal(category, 'deposit');
  assert.equal(ownerAppMessages['zh-CN'].categories[category], '租客押金');
  assert.equal(filterCashflows([record], { year: 2026, category: 'deposit' }).length, 1);
  assert.equal(filterCashflows([record], { year: 2026, category: 'expense' }).length, 0);
  assert.deepEqual(groupCashflowMonths([record], 2026)[7].categories, [{ category: 'deposit', income: 1000, expense: 0, net: 1000, count: 1 }]);
  assert.equal(record.balanceAfter, 910);
});
test('未知收入不能被标成其他支出，未知支出仍属于其他支出', () => {
  assert.equal(cashflowCategory('legacy_other', 'income'), 'income');
  assert.equal(cashflowCategory('legacy_other', 'expense'), 'expense');
  assert.equal(cashflowCategory('income'), 'income');
  assert.equal(cashflowCategory('tenant_deposit', 'expense'), 'deposit');
});
