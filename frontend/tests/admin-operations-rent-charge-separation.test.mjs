import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const component = readFileSync(
  new URL('../src/components/AdminPropertyProcessWorkspace.vue', import.meta.url),
  'utf8',
);
const i18n = readFileSync(new URL('../src/i18n/index.js', import.meta.url), 'utf8');

test('租金收款与租客账单使用独立页签和内容区', () => {
  assert.match(component, /operationsTabs\.charges/);
  assert.match(component, /operationsTab === 'charges'/);
  assert.match(i18n, /charges:\s*\['租客账单'/);
  assert.match(component, /operationsRentTotal/);
});

test('租客账单显示本地化类型和承担方而不是内部枚举', () => {
  assert.match(component, /operationsChargeTypeLabel\(charge\.chargeType\)/);
  assert.match(component, /operationsChargePayerLabel\(charge\.payer\)/);
  assert.match(component, /operationsChargeDescriptionPrefix/);
  assert.doesNotMatch(component, /\{\{ charge\.chargeType \}\} · \{\{ charge\.payer \}\}/);
  assert.match(i18n, /operationsFormChargeTitle:\s*\['新增租客账单费用'/);
});
