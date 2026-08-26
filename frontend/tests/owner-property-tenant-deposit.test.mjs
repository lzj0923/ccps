import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const read = relativePath => readFileSync(new URL(relativePath, import.meta.url), 'utf8');
const properties = read('../src/components/MyPropertiesDashboard.vue');
const mapper = read('../../backend/src/main/java/com/ccps/backend/mapper/OwnerDashboardMapper.java');
const response = read('../../backend/src/main/java/com/ccps/backend/dto/OwnerDashboardResponse.java');

test('业主房产列表和详情显示有效租约的租客押金金额', () => {
  assert.match(mapper, /SUM\(l\.deposit_amount\).*tenant_deposit_amount/);
  assert.match(mapper, /WHERE l\.status = 'active'/);
  assert.match(response, /BigDecimal tenantDepositAmount/);
  assert.match(properties, /tenantDepositText: this\.money\(property\.tenantDepositAmount\)/);
  assert.match(properties, /\{\{ tenantDepositLabel \}\}.*\{\{ property\.tenantDepositText \}\}/);
  assert.match(properties, /\{\{ tenantDepositLabel \}\}.*\{\{ selectedProperty\.tenantDepositText \}\}/);
});

test('顶部汇总卡展示租客押金而不是业主预备金', () => {
  assert.match(response, /BigDecimal tenantDepositAmount,\s*BigDecimal reserveBalance/);
  assert.match(properties, /label: this\.tenantDepositLabel, value: this\.money\(summary\.tenantDepositAmount\)/);
  assert.doesNotMatch(properties, /label: '出租中預備金'/);
});
