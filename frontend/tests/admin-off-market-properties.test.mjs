import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const read = path => readFileSync(new URL(`../src/${path}`, import.meta.url), 'utf8');

test('下架的是出租房源，并在租赁运营中提供独立资料库', () => {
  const router = read('router.js');
  const page = read('pages/AdminPage.vue');
  const workspace = read('components/AdminOffMarketPropertiesWorkspace.vue');
  const navigation = read('composables/dashboardViewModel.js');
  assert.match(router, /admin\/off-market-rentals/);
  assert.match(page, /AdminOffMarketPropertiesWorkspace/);
  assert.match(workspace, /房产资产不会被下架/);
  assert.match(workspace, /重新上架招租/);
  assert.match(navigation, /rental.*adminOffMarketProperties/s);
});

test('停止招租入口位于租房流程，房产清单不提供资产下架', () => {
  const process = read('components/AdminPropertyProcessWorkspace.vue');
  const owners = read('components/AdminOwnersWorkspace.vue');
  const api = read('services/propertyApi.js');
  assert.match(process, /停止招租并下架房源/);
  assert.match(process, /房产资产、历史资料、账单和已有租约都会保留/);
  assert.doesNotMatch(owners, /确认下架|offMarketForm\.reasonCode/);
  assert.match(api, /\/admin\/rental-listings\/\$\{unitId\}\/off-market/);
});
