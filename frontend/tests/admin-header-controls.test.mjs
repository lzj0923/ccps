import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = relativePath => readFileSync(new URL(relativePath, import.meta.url), 'utf8');
const header = read('../src/components/PageHeader.vue');
const modal = read('../src/components/AppModal.vue');
const actions = read('../src/composables/dashboardActions.js');
const state = read('../src/composables/dashboardState.js');
const smartDashboard = read('../src/components/AdminSmartDashboardWorkspace.vue');
const managementDashboard = read('../src/components/AdminDashboardWorkspace.vue');
const api = read('../src/services/propertyApi.js');
const adminTheme = read('../src/admin-theme.css');

test('顶部日期入口打开独立日期表单并校验起止日期', () => {
  assert.match(header, /aria-haspopup="dialog"[^>]*@click="openDatePanel"/);
  assert.match(modal, /v-if="modalMode === 'date'"/);
  assert.match(modal, /v-model="dateDraftStart"[^>]*type="date"/);
  assert.match(modal, /v-model="dateDraftEnd"[^>]*type="date"/);
  assert.match(actions, /validateDateRange\(this\.dateDraftStart, this\.dateDraftEnd\)/);
  assert.match(actions, /this\.modal\?\.close\(\)/);
});

test('顶部通知入口读取真实提醒并能进入完整提醒中心', () => {
  assert.match(header, /refreshAdminAlerts\(\)/);
  assert.match(header, /adminAlertCount/);
  assert.match(actions, /fetchAdminReminders/);
  assert.match(actions, /async refreshAdminAlerts\(\)/);
  assert.match(actions, /selectModule\('adminAlerts'\)/);
  assert.match(actions, /adminReminderOpenNotificationsNonce \+= 1/);
  assert.match(actions, /\$nextTick\(\(\) => \{ this\.globalSearch = item\.title/);
  assert.doesNotMatch(state, /房款即將到期|SQL 同步失敗/);
});

test('两个管理总览都把全局日期范围传给统计接口并在日期变化时刷新', () => {
  assert.match(api, /export function fetchAdminDashboard\(filters = \{\}\)/);
  for (const source of [smartDashboard, managementDashboard]) {
    assert.match(source, /'page\.dateStart'\(\) \{ this\.loadData\(\); \}/);
    assert.match(source, /'page\.dateEnd'\(\) \{ this\.loadData\(\); \}/);
    assert.match(source, /fetchAdminDashboard\(\{ startDate: this\.page\.dateStart, endDate: this\.page\.dateEnd \}\)/);
  }
});

test('工作台隐藏标题后顶部操作区仍占满可用宽度并保持右对齐', () => {
  assert.match(header, /class="topbar-copy"/);
  assert.match(adminTheme, /\.admin-shell \.topbar-copy\{flex:0 1 220px;min-width:0\}/);
  assert.doesNotMatch(adminTheme, /\.admin-shell \.topbar>div:first-child\{flex:0 1 220px/);
  assert.match(adminTheme, /\.admin-shell \.dashboard-topbar \.top-actions\{flex:1 1 auto;width:100%;justify-content:flex-end\}/);
});
