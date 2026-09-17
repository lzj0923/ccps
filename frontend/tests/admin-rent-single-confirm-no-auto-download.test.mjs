import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const source = readFileSync(new URL('../src/components/AdminRentCollectionWorkspace.vue', import.meta.url), 'utf8');
const financeSource = readFileSync(new URL('../src/components/AdminRentFinanceWorkspace.vue', import.meta.url), 'utf8');
const apiSource = readFileSync(new URL('../src/services/propertyApi.js', import.meta.url), 'utf8');
const submitConfirm = source.match(/async submitConfirm\(\)\{([\s\S]*?)\},\s*openBatchConfirm\(\)/)?.[1] || '';

test('单笔确认租金成功后不自动下载收据', () => {
  assert.ok(submitConfirm, '应找到单笔确认租金方法');
  assert.doesNotMatch(submitConfirm, /downloadReceipt\s*\(/);
  assert.match(submitConfirm, /收據已產生/);
});

test('手动下载收据统一使用当前财务收据接口', () => {
  assert.match(financeSource, /getAdminFinanceDocumentUrl\(row\.id,\s*['"]receipt['"]\)/);
  assert.doesNotMatch(financeSource, /getAdminRentReceiptUrl/);
  assert.doesNotMatch(source, /fetchAdminRentReceipt|async downloadReceipt\(/);
  assert.doesNotMatch(apiSource, /rent-payments\/\$\{financeRecordId\}\/receipt/);
});
