import { matchLocalizedSource } from './helpers/localizedSource.mjs';
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

  matchLocalizedSource(toolbar, /ui\.ownerReserveRefund/, '頂部工具列必須顯示業主預備金返還按鈕');
  matchLocalizedSource(state, /adminReserveRefundNonce:\s*0/, '頁面狀態必須保留返還操作訊號');
  matchLocalizedSource(actions, /adminReserveRefundNonce \+= 1/, '點擊頂部按鈕必須發出返還操作訊號');
  matchLocalizedSource(pageBridge, /'triggerReserveRefundAction'/, '工具列橋接層必須轉發返還按鈕方法');
  matchLocalizedSource(workspace, /refundNonce\(\) \{ return this\.page\.adminReserveRefundNonce; \}/, '預備金工作區必須接收返還操作訊號');
});

test('顶部新增充值按钮打开充值表单而不是备用金设置', () => {
  const actions = read('../src/composables/dashboardActions.js');
  const start = actions.indexOf('triggerPrimaryAction()');
  const end = actions.indexOf('triggerSecondaryAction()', start);
  const primaryAction = actions.slice(start, end);
  matchLocalizedSource(primaryAction, /currentId === 'adminReserve'[\s\S]*adminReserveDirectTopupNonce \+= 1/);
  assert.doesNotMatch(primaryAction, /currentId === 'adminReserve'[\s\S]*adminReserveSettingsNonce \+= 1/);
});

test('顶部新增扣款进入预设预备金支付方式的支出表单', () => {
  const actions = read('../src/composables/dashboardActions.js');
  const state = read('../src/composables/dashboardState.js');
  const workspace = read('../src/components/AdminMaintenanceWorkspace.vue');
  const start = actions.indexOf('triggerSecondaryAction()');
  const secondaryAction = actions.slice(start);

  matchLocalizedSource(state, /adminExpenseCreateMode:\s*''/);
  matchLocalizedSource(secondaryAction, /currentId === 'adminReserve'[\s\S]*adminExpenseCreateMode = 'reserve-debit'[\s\S]*selectModule\('adminMaintenance'\)[\s\S]*adminExpenseCreateNonce \+= 1/);
  assert.doesNotMatch(secondaryAction, /currentId === 'adminReserve'[\s\S]{0,120}adminReserveDirectTopupNonce \+= 1/);
  matchLocalizedSource(workspace, /expenseCreateMode === 'reserve-debit'[\s\S]*ui\.addReserveDebit/);
  matchLocalizedSource(workspace, /expenseCreateMode === 'reserve-debit'\) this\.expenseForm\.settlementMethod = 'reserve'/);
});

test('单笔及批量备用金返还均可选择并提交日期', () => {
  const workspace = read('../src/components/AdminReserveWorkspace.vue');
  const singleRequest = read('../../backend/src/main/java/com/ccps/backend/dto/AdminReserveRefundRequest.java');
  const batchRequest = read('../../backend/src/main/java/com/ccps/backend/dto/AdminReserveBatchRefundRequest.java');
  const service = read('../../backend/src/main/java/com/ccps/backend/service/AdminReserveManagementService.java');

  matchLocalizedSource(workspace, /v-model="refund\.paymentDate" type="date" required/);
  matchLocalizedSource(workspace, /v-model="batchRefund\.paymentDate" type="date" required/);
  matchLocalizedSource(workspace, /createAdminReserveRefund\([^\n]*paymentDate: this\.refund\.paymentDate/);
  matchLocalizedSource(workspace, /createAdminReserveRefunds\([^\n]*paymentDate: this\.batchRefund\.paymentDate/);
  matchLocalizedSource(singleRequest, /@NotNull LocalDate paymentDate/);
  matchLocalizedSource(batchRequest, /@NotNull LocalDate paymentDate/);
  matchLocalizedSource(service, /refund\.setPaymentDate\(request\.paymentDate\(\)\)/);
  matchLocalizedSource(service, /LocalDate scheduledDate = request\.paymentDate\(\)\.plusDays\(index - 1L\)/);
});

test('直接充值进入财务待确认且待确认指标可跳转财务模块', () => {
  const workspace = read('../src/components/AdminReserveWorkspace.vue');
  const financeWorkspace = read('../src/components/AdminReserveFinanceWorkspace.vue');
  const messages = read('../src/i18n/legacy.generated.js');

  matchLocalizedSource(workspace, /action: 'admin-reserve-pending-topups'/);
  matchLocalizedSource(workspace, /window\.addEventListener\('admin-reserve-pending-topups', this\.goFinance\)/);
  matchLocalizedSource(workspace, /adminFinanceMode = 'reserve'[\s\S]*adminFinanceViewMode = 'pending'[\s\S]*selectModule\('adminFinance'\)/);
  matchLocalizedSource(workspace, /充值已提交财务确认/);
  matchLocalizedSource(financeWorkspace, /type: 'reserve'/);
  matchLocalizedSource(financeWorkspace, /confirmAdminFinanceReview/);
  assert.doesNotMatch(financeWorkspace, /reviewAdminReserveTopup/);
  matchLocalizedSource(messages, /提交充值资料后进入财务确认/);
  assert.doesNotMatch(messages, /不经待确认流程/);
});

test('备用金列表可全选当前筛选结果并显示半选状态', () => {
  const workspace = read('../src/components/AdminReserveWorkspace.vue');
  matchLocalizedSource(workspace, /aria-label="全选当前筛选结果"/);
  matchLocalizedSource(workspace, /:checked="allFilteredRefundSelected"/);
  matchLocalizedSource(workspace, /:indeterminate="someFilteredRefundSelected"/);
  matchLocalizedSource(workspace, /@change="toggleAllRefundAccounts"/);
  matchLocalizedSource(workspace, /for \(const account of this\.filteredAccounts\)/);
});

test('备用金备注可编辑并在批量返还时按房产显示', () => {
  const workspace = read('../src/components/AdminReserveWorkspace.vue');
  matchLocalizedSource(workspace, /v-model\.trim="settings\.remarks"/);
  matchLocalizedSource(workspace, /orderReserveRemarks\(selectedAccount\.remarks\) \|\| '暂无备用金备注'/);
  matchLocalizedSource(workspace, /orderReserveRemarks\(item\.remarks\) \|\| '暂无备用金备注'/);
  matchLocalizedSource(workspace, /remarks: account\.remarks \|\| ''/);
  matchLocalizedSource(workspace, /remarks: remarks \|\| null/);
});

test('批量返还项目显示租约起止日期并按日月年格式化', () => {
  const workspace = read('../src/components/AdminReserveWorkspace.vue');
  matchLocalizedSource(workspace, /租约：\{\{ leaseDateRange\(item\) \}\}/);
  matchLocalizedSource(workspace, /leaseStartDate: account\.leaseStartDate/);
  matchLocalizedSource(workspace, /leaseEndDate: account\.leaseEndDate/);
  matchLocalizedSource(workspace, /leaseDateRange\(item\)/);
  matchLocalizedSource(workspace, /leaseDate\(value\) \{ return formatDate\(value, '未设置'\); \}/);
});

test('备用金账户接口带出当前租约日期', () => {
  const dto = read('../../backend/src/main/java/com/ccps/backend/dto/AdminReserveManagementResponse.java');
  const mapper = read('../../backend/src/main/java/com/ccps/backend/mapper/AdminReserveManagementMapper.java');
  matchLocalizedSource(dto, /LocalDate leaseStartDate, LocalDate leaseEndDate/);
  matchLocalizedSource(mapper, /lease_start_date/);
  matchLocalizedSource(mapper, /lease_end_date/);
});

test('批量返还按银行每日额度拆分并联动海外手续费', () => {
  const workspace = read('../src/components/AdminReserveWorkspace.vue');
  const request = read('../../backend/src/main/java/com/ccps/backend/dto/AdminReserveBatchRefundRequest.java');
  const service = read('../../backend/src/main/java/com/ccps/backend/service/AdminReserveManagementService.java');
  matchLocalizedSource(workspace, /v-model\.number="item\.bankAccountId"/);
  matchLocalizedSource(workspace, /refundBankAccounts/);
  matchLocalizedSource(workspace, /refundScheduleText\(item\)/);
  matchLocalizedSource(workspace, /海外银行会同步建立可修改的手续费支出/);
  matchLocalizedSource(request, /Long bankAccountId/);
  matchLocalizedSource(service, /RoundingMode\.CEILING/);
  matchLocalizedSource(service, /MANUAL-CF-BANK-FEE-/);
  matchLocalizedSource(service, /insertRefundTransfer/);
});

test('批量返还允许单个单位覆盖默认付款方式且仅银行转账要求银行账户', () => {
  const workspace = read('../src/components/AdminReserveWorkspace.vue');
  const request = read('../../backend/src/main/java/com/ccps/backend/dto/AdminReserveBatchRefundRequest.java');
  const service = read('../../backend/src/main/java/com/ccps/backend/service/AdminReserveManagementService.java');

  assert.doesNotMatch(workspace, /尚未设置银行账户`\); return;/, '无银行账户不应阻断批量返还弹窗');
  matchLocalizedSource(workspace, /v-model="item\.paymentMethod"/, '每个单位都应可覆盖批次默认付款方式');
  matchLocalizedSource(workspace, /effectiveRefundPaymentMethod\(item\)/, '单笔付款方式应回退到批次默认值');
  matchLocalizedSource(workspace, /refundNeedsBank\(item\)/, '只有有效付款方式为银行转账时才校验银行账户');
  matchLocalizedSource(workspace, /paymentMethod: this\.effectiveRefundPaymentMethod\(item\)/, '请求应携带每一笔的有效付款方式');
  matchLocalizedSource(request, /String paymentMethod/, '后端批量明细应接收单笔付款方式');
  matchLocalizedSource(service, /item\.paymentMethod\(\)/, '后端应优先使用单笔付款方式');
});

test('批量返还使用全屏工作台并在桌面端保持一单位一排', () => {
  const workspace = read('../src/components/AdminReserveWorkspace.vue');
  matchLocalizedSource(workspace, /class="batch-refund-form"/);
  matchLocalizedSource(workspace, /class="batch-refund-columns"/);
  matchLocalizedSource(workspace, /已选择 \{\{ batchRefund\.items\.length \}\} 个单位/);
  matchLocalizedSource(workspace, /\.batch-refund-dialog\{[^}]*height:100dvh/);
  matchLocalizedSource(workspace, /\.batch-refund-form\{[^}]*grid-template-rows:auto minmax\(0,1fr\) auto auto/);
  matchLocalizedSource(workspace, /@media\(min-width:72rem\)\{\.batch-refund-columns,\.batch-refund-list article\{display:grid;grid-template-columns:/);
});
