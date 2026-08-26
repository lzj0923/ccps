import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const readBackend = relativePath => readFileSync(
  new URL(`../../backend/src/main/java/com/ccps/backend/mapper/${relativePath}`, import.meta.url),
  'utf8',
);

const expenses = readBackend('OwnerExpenseMaintenanceMapper.java');
const dashboard = readBackend('OwnerDashboardMapper.java');
const propertyCashflow = readBackend('OwnerPropertyCashflowMapper.java');

test('屋主支出列表和汇总只读取财务已确认记录', () => {
  const confirmedFilters = expenses.match(/fr\.confirmation_status = 'confirmed'/g) || [];
  assert.ok(confirmedFilters.length >= 3, '支出总额、累计汇总和支出列表都必须过滤待确认记录');
});
test('屋主房产概览和单位全部收支不展示待确认流水', () => {
  assert.match(dashboard, /cashflow_entries[\s\S]*?fr\.confirmation_status = 'confirmed'[\s\S]*?GROUP BY ce\.unit_id/);
  assert.match(propertyCashflow, /FROM cashflow_entries[\s\S]*?fr\.confirmation_status = 'confirmed'/);
  assert.doesNotMatch(propertyCashflow, /findPendingReserveTopups/);
});
