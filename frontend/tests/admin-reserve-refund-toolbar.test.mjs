import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const read = (relativePath) => readFileSync(fileURLToPath(new URL(relativePath, import.meta.url)), 'utf8');

test('預備金頁面在頂部工具列提供業主預備金返還入口', () => {
  const toolbar = read('../src/components/ModuleToolbar.vue');
  const state = read('../src/composables/dashboardState.js');
  const actions = read('../src/composables/dashboardActions.js');
  const pageBridge = read('../src/pageBridge.js');
  const workspace = read('../src/components/AdminReserveWorkspace.vue');

  assert.match(toolbar, /ui\.ownerReserveRefund/, '頂部工具列必須顯示業主預備金返還按鈕');
  assert.match(state, /adminReserveRefundNonce:\s*0/, '頁面狀態必須保留返還操作訊號');
  assert.match(actions, /adminReserveRefundNonce \+= 1/, '點擊頂部按鈕必須發出返還操作訊號');
  assert.match(pageBridge, /'triggerReserveRefundAction'/, '工具列橋接層必須轉發返還按鈕方法');
  assert.match(workspace, /refundNonce\(\) \{ return this\.page\.adminReserveRefundNonce; \}/, '預備金工作區必須接收返還操作訊號');
});

test('顶部新增充值按钮打开充值表单而不是备用金设置', () => {
  const actions = read('../src/composables/dashboardActions.js');
  const start = actions.indexOf('triggerPrimaryAction()');
  const end = actions.indexOf('triggerSecondaryAction()', start);
  const primaryAction = actions.slice(start, end);
  assert.match(primaryAction, /currentId === 'adminReserve'[\s\S]*adminReserveDirectTopupNonce \+= 1/);
  assert.doesNotMatch(primaryAction, /currentId === 'adminReserve'[\s\S]*adminReserveSettingsNonce \+= 1/);
});

test('单笔及批量备用金返还均可选择并提交日期', () => {
  const workspace = read('../src/components/AdminReserveWorkspace.vue');
  const singleRequest = read('../../backend/src/main/java/com/ccps/backend/dto/AdminReserveRefundRequest.java');
  const batchRequest = read('../../backend/src/main/java/com/ccps/backend/dto/AdminReserveBatchRefundRequest.java');
  const service = read('../../backend/src/main/java/com/ccps/backend/service/AdminReserveManagementService.java');

  assert.match(workspace, /v-model="refund\.paymentDate" type="date" required/);
  assert.match(workspace, /v-model="batchRefund\.paymentDate" type="date" required/);
  assert.match(workspace, /createAdminReserveRefund\([^\n]*paymentDate: this\.refund\.paymentDate/);
  assert.match(workspace, /createAdminReserveRefunds\([^\n]*paymentDate: this\.batchRefund\.paymentDate/);
  assert.match(singleRequest, /@NotNull LocalDate paymentDate/);
  assert.match(batchRequest, /@NotNull LocalDate paymentDate/);
  assert.match(service, /refund\.setPaymentDate\(request\.paymentDate\(\)\)/);
  assert.match(service, /LocalDate scheduledDate = request\.paymentDate\(\)\.plusDays\(index - 1L\)/);
});

test('直接充值进入财务待确认且待确认指标可跳转财务模块', () => {
  const workspace = read('../src/components/AdminReserveWorkspace.vue');
  const financeWorkspace = read('../src/components/AdminReserveFinanceWorkspace.vue');
  const messages = read('../src/i18n/legacy.generated.js');

  assert.match(workspace, /action: 'admin-reserve-pending-topups'/);
  assert.match(workspace, /window\.addEventListener\('admin-reserve-pending-topups', this\.goFinance\)/);
  assert.match(workspace, /adminFinanceMode = 'reserve'[\s\S]*adminFinanceViewMode = 'pending'[\s\S]*selectModule\('adminFinance'\)/);
  assert.match(workspace, /充值已提交财务确认/);
  assert.match(financeWorkspace, /type: 'reserve'/);
  assert.match(financeWorkspace, /reviewAdminReserveTopup/);
  assert.match(messages, /提交充值资料后进入财务确认/);
  assert.doesNotMatch(messages, /不经待确认流程/);
});

test('备用金列表可全选当前筛选结果并显示半选状态', () => {
  const workspace = read('../src/components/AdminReserveWorkspace.vue');
  assert.match(workspace, /aria-label="全选当前筛选结果"/);
  assert.match(workspace, /:checked="allFilteredRefundSelected"/);
  assert.match(workspace, /:indeterminate="someFilteredRefundSelected"/);
  assert.match(workspace, /@change="toggleAllRefundAccounts"/);
  assert.match(workspace, /for \(const account of this\.filteredAccounts\)/);
});

test('备用金备注可编辑并在批量返还时按房产显示', () => {
  const workspace = read('../src/components/AdminReserveWorkspace.vue');
  assert.match(workspace, /v-model\.trim="settings\.remarks"/);
  assert.match(workspace, /orderReserveRemarks\(selectedAccount\.remarks\) \|\| '暂无备用金备注'/);
  assert.match(workspace, /orderReserveRemarks\(item\.remarks\) \|\| '暂无备用金备注'/);
  assert.match(workspace, /remarks: account\.remarks \|\| ''/);
  assert.match(workspace, /remarks: remarks \|\| null/);
});

test('批量返还项目显示租约起止日期并按日月年格式化', () => {
  const workspace = read('../src/components/AdminReserveWorkspace.vue');
  assert.match(workspace, /租约：\{\{ leaseDateRange\(item\) \}\}/);
  assert.match(workspace, /leaseStartDate: account\.leaseStartDate/);
  assert.match(workspace, /leaseEndDate: account\.leaseEndDate/);
  assert.match(workspace, /leaseDateRange\(item\)/);
  assert.match(workspace, /padStart\(2, '0'\)/);
});

test('备用金账户接口带出当前租约日期', () => {
  const dto = read('../../backend/src/main/java/com/ccps/backend/dto/AdminReserveManagementResponse.java');
  const mapper = read('../../backend/src/main/java/com/ccps/backend/mapper/AdminReserveManagementMapper.java');
  assert.match(dto, /LocalDate leaseStartDate, LocalDate leaseEndDate/);
  assert.match(mapper, /lease_start_date/);
  assert.match(mapper, /lease_end_date/);
});

test('批量返还按银行每日额度拆分并联动海外手续费', () => {
  const workspace = read('../src/components/AdminReserveWorkspace.vue');
  const request = read('../../backend/src/main/java/com/ccps/backend/dto/AdminReserveBatchRefundRequest.java');
  const service = read('../../backend/src/main/java/com/ccps/backend/service/AdminReserveManagementService.java');
  assert.match(workspace, /v-model\.number="item\.bankAccountId"/);
  assert.match(workspace, /refundBankAccounts/);
  assert.match(workspace, /refundScheduleText\(item\)/);
  assert.match(workspace, /海外银行会同步建立可修改的手续费支出/);
  assert.match(request, /Long bankAccountId/);
  assert.match(service, /RoundingMode\.CEILING/);
  assert.match(service, /MANUAL-CF-BANK-FEE-/);
  assert.match(service, /insertRefundTransfer/);
});

test('批量返还使用全屏工作台并在桌面端保持一单位一排', () => {
  const workspace = read('../src/components/AdminReserveWorkspace.vue');
  assert.match(workspace, /class="batch-refund-form"/);
  assert.match(workspace, /class="batch-refund-columns"/);
  assert.match(workspace, /已选择 \{\{ batchRefund\.items\.length \}\} 个单位/);
  assert.match(workspace, /\.batch-refund-dialog\{[^}]*height:100dvh/);
  assert.match(workspace, /\.batch-refund-form\{[^}]*grid-template-rows:auto minmax\(0,1fr\) auto auto/);
  assert.match(workspace, /@media\(min-width:72rem\)\{\.batch-refund-columns,\.batch-refund-list article\{display:grid;grid-template-columns:/);
});
