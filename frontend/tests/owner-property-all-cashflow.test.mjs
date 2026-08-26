import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const read = relativePath => readFileSync(new URL(relativePath, import.meta.url), 'utf8');
const properties = read('../src/components/MyPropertiesDashboard.vue');
const finance = read('../src/components/OwnerFinanceDashboard.vue');
const api = read('../src/services/propertyApi.js');
const mapper = read('../../backend/src/main/java/com/ccps/backend/mapper/OwnerPropertyCashflowMapper.java');
const response = read('../../backend/src/main/java/com/ccps/backend/dto/OwnerPropertyCashflowResponse.java');
const service = read('../../backend/src/main/java/com/ccps/backend/service/OwnerPropertyCashflowService.java');

test('查看全部收支会带单位编号进入房产收支页', () => {
  assert.match(properties, /@click="openPropertyCashflow\(selectedProperty\)"/);
  assert.match(properties, /\/owner\/finance\?ownerUnitId=\$\{encodeURIComponent\(ownerUnitId\)\}&tab=cashflow/);
  assert.doesNotMatch(properties, /service-secondary-action" @click="openServiceModule\('ownerExpenses'\)"/);
});

test('房产收支页按单位加载包含收入支出和预备金的全部流水', () => {
  assert.match(api, /\/owner\/properties\/\$\{ownerUnitId\}\/cashflows/);
  assert.match(finance, /fetchOwnerPropertyCashflows/);
  assert.match(finance, /openRequestedProperty/);
  assert.match(finance, /propertyTab = requestedTab === 'cashflow' \? 'cashflow' : 'summary'/);
  assert.match(finance, /await fetchOwnerPropertyCashflows\(ownerUnitId\)/);
  assert.match(finance, /this\.propertyCashflows = response\.records \|\| \[\]/);
  assert.match(mapper, /FROM cashflow_entries/);
  assert.match(mapper, /FROM reserve_accounts/);
  assert.match(mapper, /o\.user_id = #\{userId\}/);
});

test('房产收支流水在金额后展示该单位每笔交易后的账户余额', () => {
  assert.match(finance, /<th class="owner-finance-number">\{\{ copy\.amount \}\}<\/th><th class="owner-finance-number">\{\{ balanceLabel \}\}<\/th>/);
  assert.match(finance, /money\(row\.balanceAfter\)/);
  assert.match(response, /BigDecimal balanceAfter/);
  assert.match(service, /withRunningBalances/);
  assert.match(service, /runningBalance = "income"\.equals\(row\.getDirection\(\)\)/);
});

test('预备金支付的支出只展示业务支出，不重复展示自动扣款', () => {
  assert.match(mapper, /NOT EXISTS\s*\(\s*SELECT 1\s+FROM cashflow_entries linked_ce\s+WHERE linked_ce\.finance_record_id = rt\.finance_record_id\s*\)/s);
});

test('金额和余额表头与对应数值统一右对齐', () => {
  assert.match(finance, /<th class="owner-finance-number">\{\{ copy\.amount \}\}<\/th><th class="owner-finance-number">\{\{ balanceLabel \}\}<\/th>/);
  assert.match(finance, /\.owner-finance-number\{text-align:right\}/);
  assert.match(finance, /font-variant-numeric:tabular-nums/);
});
