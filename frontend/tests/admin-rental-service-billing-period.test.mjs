import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const source = readFileSync(fileURLToPath(new URL('../src/components/AdminPropertyDetailWorkspace.vue', import.meta.url)), 'utf8');

test('所有租管业务服务都可选择单月或多月收费', () => {
  assert.match(source, /v-model="item\.billingMode"/);
  assert.match(source, /<option value="once">单个月<\/option>/);
  assert.match(source, /<option value="months">多个月<\/option>/);
  assert.match(source, /toggleRentalServiceMonth\(item, month\)/);
});

test('租管业务服务保存时保留收费模式和月份', () => {
  assert.match(source, /billingMode:item\.billingMode==='months'\?'months':'once'/);
  assert.match(source, /billingMonths:normalizeServiceMonths\(item\.billingMonths\)/);
});

test('固定业务费用也统一支持单月和多月规则', () => {
  for (const field of ['salesServiceFee', 'generalServiceFee', 'buildingManagementFee', 'fireInsuranceFee', 'landTaxFee', 'assessmentTaxFee']) {
    assert.match(source, new RegExp(`amountField:'${field}'`));
  }
  assert.match(source, /normalizeBuiltInServiceBilling\(service\)/);
  assert.match(source, /toggleBuiltInServiceMonth\(service, month\)/);
  assert.match(source, /buildingManagementBillingMonths:normalizeServiceMonths/);
  assert.match(source, /assessmentTaxBillingMonths:normalizeServiceMonths/);
});
