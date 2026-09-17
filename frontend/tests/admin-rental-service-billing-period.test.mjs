import { matchLocalizedSource } from './helpers/localizedSource.mjs';
import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const source = readFileSync(fileURLToPath(new URL('../src/components/AdminPropertyDetailWorkspace.vue', import.meta.url)), 'utf8');

test('所有租管业务服务都可选择单月或多月收费', () => {
  matchLocalizedSource(source, /v-model="item\.billingMode"/);
  matchLocalizedSource(source, /<option value="once">单个月<\/option>/);
  matchLocalizedSource(source, /<option value="months">多个月<\/option>/);
  matchLocalizedSource(source, /toggleRentalServiceMonth\(item, month\)/);
});

test('租管业务服务保存时保留收费模式和月份', () => {
  matchLocalizedSource(source, /billingMode:item\.billingMode==='months'\?'months':'once'/);
  matchLocalizedSource(source, /billingMonths:normalizeServiceMonths\(item\.billingMonths\)/);
});

test('固定业务费用也统一支持单月和多月规则', () => {
  for (const field of ['salesServiceFee', 'generalServiceFee', 'buildingManagementFee', 'fireInsuranceFee', 'landTaxFee', 'assessmentTaxFee']) {
    matchLocalizedSource(source, new RegExp(`amountField:'${field}'`));
  }
  matchLocalizedSource(source, /normalizeBuiltInServiceBilling\(service\)/);
  matchLocalizedSource(source, /toggleBuiltInServiceMonth\(service, month\)/);
  matchLocalizedSource(source, /buildingManagementBillingMonths:normalizeServiceMonths/);
  matchLocalizedSource(source, /assessmentTaxBillingMonths:normalizeServiceMonths/);
});
