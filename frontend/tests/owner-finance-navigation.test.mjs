import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const read = relativePath => readFileSync(new URL(relativePath, import.meta.url), 'utf8');
const header = read('../src/components/PageHeader.vue');
const dashboard = read('../src/components/OwnerFinanceDashboard.vue');
const ownerPage = read('../src/pages/OwnerPage.vue');
const router = read('../src/router.js');
const styles = read('../styles.css');

test('业主导航把三个财务页面收进可展开的财务中心', () => {
  assert.match(router, /path: '\/owner\/finance'.*moduleId: 'ownerFinance'/);
  assert.match(header, /FINANCE_MODULE_IDS = \['rentIncome', 'ownerExpenses', 'ownerReserve'\]/);
  assert.match(header, /class="owner-finance-menu" role="menu"/);
  assert.match(header, /selectFinanceModule\(child\.id\)/);
  assert.doesNotMatch(header, /ownerNavLabel\(child\)<\/span><ChevronRight/);
  assert.match(header, /class="owner-finance-link"[^>]*@click="openFinanceOverview"/);
  assert.match(header, /class="owner-finance-toggle"[^>]*@click\.stop="toggleFinanceMenu"/);
  assert.match(header, /openFinanceOverview\(\) \{ this\.financeOpen = false; this\.selectModule\('ownerFinance'\); \}/);
  assert.doesNotMatch(header, /toggleFinanceMenu\(\) \{\s*if \(this\.currentId/);
  assert.doesNotMatch(header, /financeIncome|financeExpense|\/owner\/finance\?view=/);
  assert.doesNotMatch(dashboard, /syncFlowFilterFromRoute/);
});

test('财务下拉菜单不会被顶部导航容器裁切', () => {
  assert.match(styles, /\.owner-header \.owner-nav nav\{overflow:visible\}/);
  assert.match(styles, /\.owner-finance-menu\{position:absolute;z-index:30/);
  assert.match(styles, /\.owner-finance-menu\{[^}]*width:100%;min-width:100%/);
});

test('财务中心显示房产概览并按单位查看完整金额流水', () => {
  assert.match(ownerPage, /OwnerFinanceDashboard v-else-if="currentId === 'ownerFinance'"/);
  assert.match(dashboard, /totalIncome: '本月总收入'/);
  assert.match(dashboard, /totalExpense: '本月总支出'/);
  assert.match(dashboard, /netBalance: '本月净结余'/);
  assert.match(dashboard, /reserveBalance: '预备金余额'/);
  assert.match(dashboard, /ledgerTitle: '金额流水'/);
  assert.match(dashboard, /cumulativeIncome: '累计总收入'/);
  assert.match(dashboard, /cumulativeExpense: '累计总支出'/);
  assert.match(dashboard, /expenses\.summary\?\.totalIncome/);
  assert.match(dashboard, /expenses\.summary\?\.totalExpense/);
  assert.match(dashboard, /fetchOwnerRentIncome/);
  assert.match(dashboard, /fetchOwnerExpenses/);
  assert.match(dashboard, /fetchOwnerReserve/);
  assert.match(dashboard, /owner-property-overview-grid/);
  assert.match(dashboard, /selectedPropertyCashflow\(\)/);
  assert.match(dashboard, /fetchOwnerPropertyCashflows\(ownerUnitId\)/);
  assert.match(dashboard, /row\.balanceAfter/);
  assert.doesNotMatch(dashboard, /<th>\{\{ copy\.status \}\}<\/th>/);
});
