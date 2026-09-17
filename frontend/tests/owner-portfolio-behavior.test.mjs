import assert from 'node:assert/strict';
import test from 'node:test';
import { filterCashflows, groupCashflowMonths, summarizeCashflows, filterPropertyPhotos, searchOwnerProperties } from '../src/utils/ownerPortfolio.js';
import { ownerAppMessages } from '../src/i18n/ownerApp.js';

const records = [
  { key: 'old', occurredOn: '2025-12-31', direction: 'income', category: 'rent', amount: 1000, balanceAfter: 1000 },
  { key: 'rent', occurredOn: '2026-08-01', direction: 'income', category: 'rent', amount: 3000, balanceAfter: 4000 },
  { key: 'fee', occurredOn: '2026-08-02', direction: 'expense', category: 'service_fee', amount: 300, balanceAfter: 3700, description: 'Monthly service' },
  { key: 'tax', occurredOn: '2026-09-01', direction: 'expense', category: 'vat', amount: 21, balanceAfter: 3679 }
];
test('按年份、月份、类别及摘要组合筛选，并保留历年累计结余', () => {
  const result = filterCashflows(records, { year: 2026, month: 8, direction: 'expense', category: 'management_fee', search: 'SERVICE' });
  assert.deepEqual(result.map(item => item.key), ['fee']);
  assert.equal(result[0].balanceAfter, 3700);
  assert.deepEqual(summarizeCashflows(result), { income: 0, expense: 300, net: -300 });
});
test('年度汇总不混入上一年，空月份仍保留且明细分类与总额一致', () => {
  const months = groupCashflowMonths(records, 2026);
  assert.equal(months.length, 12);
  assert.equal(months[0].net, 0);
  assert.equal(months[7].income, 3000);
  assert.equal(months[7].expense, 300);
  assert.equal(months[7].net, months[7].categories.reduce((sum, item) => sum + item.net, 0));
  assert.equal(months[8].categories[0].category, 'tax');
});
test('同一租约的出租前与退租后照片分开，不混入其他租客或未归档照片', () => {
  const photos = [{ id: 1, leaseId: 10, rentalStage: 'BEFORE' }, { id: 2, leaseId: 10, rentalStage: 'MOVE_OUT' }, { id: 3, leaseId: 11, rentalStage: 'MOVE_OUT' }, { id: 4, rentalStage: 'DURING' }];
  assert.deepEqual(filterPropertyPhotos(photos, '10', 'before').map(item => item.id), [1]);
  assert.deepEqual(filterPropertyPhotos(photos, '10', 'after').map(item => item.id), [2]);
  assert.equal(filterPropertyPhotos(photos, '', 'all').length, 4);
});
test('房产搜索可联合状态筛选，并忽略首尾空格及大小写', () => {
  const properties = [{ projectName: 'KL Central', unitNo: 'B-01', tenantName: 'Jane', assetStage: 'OPERATING' }, { projectName: 'KL Central', unitNo: 'B-02', assetStage: 'PRE_HANDOVER' }];
  assert.equal(searchOwnerProperties(properties, ' kl ', 'OPERATING').length, 1);
  assert.equal(searchOwnerProperties(properties, 'jane')[0].unitNo, 'B-01');
  assert.deepEqual(searchOwnerProperties(properties, 'missing'), []);
});
test('新增功能的简体、繁体和英文翻译具有相同的结构', () => {
  const keys = value => Object.entries(value).flatMap(([key, item]) => typeof item === 'object' ? keys(item).map(child => key + '.' + child) : [key]);
  assert.deepEqual(keys(ownerAppMessages.en), keys(ownerAppMessages['zh-CN']));
  assert.deepEqual(keys(ownerAppMessages['zh-TW']), keys(ownerAppMessages['zh-CN']));
});
