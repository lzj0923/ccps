import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const read = relativePath => readFileSync(fileURLToPath(new URL(relativePath, import.meta.url)), 'utf8');

test('业主端待办与文件资料使用稳定业务键进行国际化', () => {
  const properties = read('../src/components/MyPropertiesDashboard.vue');
  const documents = read('../src/components/DocumentCenterDashboard.vue');
  const i18n = read('../src/i18n/index.js');
  const service = read('../../backend/src/main/java/com/ccps/backend/service/OwnerDocumentService.java');

  assert.match(properties, /pendingItemCopy\(item\)/);
  assert.match(properties, /ui\.ownerPendingPaymentProof/);
  assert.match(documents, /documentCategoryLabel\(tab\)/);
  assert.match(documents, /documentStatusLabel\(file\.statusKey\)/);
  assert.match(documents, /documentStatusFilter:\s*'all'/);
  assert.match(i18n, /ownerPendingPaymentProof:\s*\['待上传缴费凭证'/);
  assert.match(i18n, /documentCategoryLease:\s*\['租赁合约'/);
  assert.match(service, /"approved",\s*"已確認"/);
});

test('房款进度注册模板和指标使用的全部图标组件', () => {
  const source = read('../src/components/PaymentProgressDetails.vue');

  assert.match(source, /components:\s*\{[^}]*ChartPie[^}]*Coins[^}]*FileText[^}]*Home[^}]*Hourglass[^}]*\}/s);
});

test('文件中心使用独立状态筛选，避免 pageBridge 同名 computed 冲突', () => {
  const source = read('../src/components/DocumentCenterDashboard.vue');

  assert.match(source, /documentStatusFilter:\s*'all'/);
  assert.doesNotMatch(source, /\bstatusFilter:\s*'全部狀態'/);
  assert.doesNotMatch(source, /v-model="statusFilter"/);
});

test('通知详情对相同正文去重后再渲染', () => {
  const source = read('../src/components/NotificationCenterDashboard.vue');

  assert.match(source, /noticeMessageParts\(\)/);
  assert.match(source, /v-for="message in noticeMessageParts"/);
  assert.doesNotMatch(source, /<p>\{\{ selectedNotice\.detail \|\| selectedNotice\.title \}\}<\/p><p>\{\{ selectedNotice\.message \|\| selectedNotice\.body \}\}<\/p>/);
});

test('业主详情电子邮件翻译词条不重复', () => {
  const source = read('../src/i18n/index.js');

  assert.match(source, /t_0d01e2e86669:\s*'电子邮件'/);
  assert.match(source, /t_0d01e2e86669:\s*'電子郵件'/);
  assert.doesNotMatch(source, /t_0d01e2e86669:\s*'电子邮件 电子邮件 电子邮件'/);
});

test('地区大屏指标面板允许 Vue 函数组件作为图标', () => {
  const source = read('../src/components/SmartDashboardMetricPanel.vue');

  assert.match(source, /icon:\s*\{\s*type:\s*\[Object, Function\]/);
});

test('财务中心下拉按钮具有区别于导航按钮的本地化名称', () => {
  const header = read('../src/components/PageHeader.vue');
  const i18n = read('../src/i18n/index.js');

  assert.match(header, /:aria-label="\$t\('ui\.financeMenuToggle'\)"/);
  assert.match(i18n, /financeMenuToggle:\s*\['展开财务中心菜单'/);
});
