import { matchLocalizedSource } from './helpers/localizedSource.mjs';
import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const read = path => readFileSync(new URL(`../src/${path}`, import.meta.url), 'utf8');

test('下架的是出租房源，并在租赁运营中提供独立资料库', () => {
  const router = read('router.js');
  const page = read('pages/AdminPage.vue');
  const workspace = read('components/AdminOffMarketPropertiesWorkspace.vue');
  const navigation = read('composables/dashboardViewModel.js');
  matchLocalizedSource(router, /admin\/off-market-rentals/);
  matchLocalizedSource(page, /AdminOffMarketPropertiesWorkspace/);
  matchLocalizedSource(workspace, /资料完整保留/);
  matchLocalizedSource(workspace, /重新上架招租/);
  matchLocalizedSource(navigation, /rental.*adminOffMarketProperties/s);
});

test('停止招租入口位于租房流程，房产清单不提供资产下架', () => {
  const process = read('components/AdminPropertyProcessWorkspace.vue');
  const owners = read('components/AdminOwnersWorkspace.vue');
  const api = read('services/propertyApi.js');
  matchLocalizedSource(process, /confirmRentalOffMarket/);
  matchLocalizedSource(process, /下架仅停止后续招租，不影响已有租约、账单及历史资料/);
  assert.doesNotMatch(owners, /确认下架|offMarketForm\.reasonCode/);
  matchLocalizedSource(api, /\/admin\/rental-listings\/\$\{unitId\}\/off-market/);
});
