<template>
  <section class="deposit-page">
    <header class="page-heading">
      <div><span class="eyebrow">{{ $t('legacy.t_ceeaac7cacd7') }}</span><h2>{{ $t('legacy.t_38cd5a1fa273') }}</h2><p>{{ $t('legacy.t_60ae6a9260dc') }}</p></div>
      <button type="button" class="outline-button" :disabled="loading" @click="load(pager.page)">{{ $t('legacy.t_38108eaa1d32') }}</button>
    </header>

    <div class="summary-grid">
      <article><span>{{ $t('legacy.t_36130af34aaf') }}</span><strong>{{ summary.totalAccounts }}</strong><small>{{ $t('legacy.t_84038e1f69d4') }}</small></article>
      <article class="warning"><span>{{ $t('legacy.t_d5d2b31f4b21') }}</span><strong>{{ summary.pendingCollectionCount }}</strong><small>{{ $t('legacy.t_5e2fc4f416db') }}</small></article>
      <article><span>{{ $t('legacy.t_3604baa26d53') }}</span><strong>{{ summary.activeCount }}</strong><small>{{ $t('legacy.t_414cb7438de8') }}</small></article>
      <article class="warning"><span>{{ $t('legacy.t_f314c828c34f') }}</span><strong>{{ summary.awaitingSettlementCount }}</strong><small>{{ $t('legacy.t_574031e958da') }}</small></article>
      <article class="money"><span>{{ $t('legacy.t_236e63b29aa0') }}</span><strong>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(summary.totalHeld) }}</strong><small>{{ $t('legacy.t_b5f12c8a216a') }}</small></article>
    </div>

    <div class="filter-card">
      <label class="search-box"><span>⌕</span><input v-model.trim="keyword" :placeholder="$t('legacy.t_98960f15232f')" @keyup.enter="search"></label>
      <div class="status-tabs">
        <button v-for="item in statuses" :key="item.value" type="button" :class="{ active: status === item.value }" @click="changeStatus(item.value)">{{ $lt(item.label) }}</button>
      </div>
    </div>

    <div class="account-card">
      <div v-if="loading" class="empty-state">{{ $t('legacy.t_b055f3591bb3') }}</div>
      <div v-else-if="error" class="empty-state error"><strong>{{ $lt(error) }}</strong><button type="button" @click="load(pager.page)">{{ $t('legacy.t_e2d53a6d3a6a') }}</button></div>
      <template v-else>
        <div class="table-wrap"><table>
          <thead><tr><th>{{ $t('legacy.t_b44a1841d1e4') }}</th><th>{{ $t('legacy.t_6ad917dd275c') }}</th><th>{{ $t('legacy.t_a872b0d92332') }}</th><th>{{ $t('legacy.t_5d6833e56e9f') }}</th><th>{{ $t('legacy.t_2551b2246ebd') }}</th><th>{{ $t('legacy.t_00a43ac8e374') }}</th><th>{{ $t('legacy.t_f3ea6d345e2a') }}</th></tr></thead>
          <tbody>
            <tr v-for="row in rows" :key="row.leaseId">
              <td><strong>{{ row.tenantName }}</strong><small>{{ row.leaseNo }} · {{ row.tenantPhone || $t('legacy.t_ad9d566e140f') }}</small></td>
              <td><strong>{{ row.projectName }}</strong><small>{{ row.unitNo }} · {{ displayDate(row.startDate) }} {{ $t('legacy.t_43401e739ef4') }} {{ displayDate(row.endDate) }}</small></td>
              <td><span class="lease-pill" :class="row.leaseStatus">{{ $lt(leaseStatusLabel(row.leaseStatus)) }}</span></td>
              <td class="amount">{{ $t('legacy.t_5e7b60c626a4') }} {{ money(row.expectedDeposit) }}</td>
              <td class="amount strong">{{ $t('legacy.t_5e7b60c626a4') }} {{ money(row.postedBalance) }}<small v-if="Number(row.postedBalance) !== Number(row.availableBalance)">{{ $t('legacy.t_58d8f6b0c0ca') }} {{ money(row.availableBalance) }}</small></td>
              <td><span class="account-pill" :class="row.accountStatus">{{ $lt(accountStatusLabel(row.accountStatus)) }}</span></td>
              <td><button type="button" class="primary-button small" @click="openAccount(row)">{{ $t('legacy.t_acb4b6435b71') }}</button></td>
            </tr>
            <tr v-if="!rows.length"><td colspan="7" class="empty-state">{{ $t('legacy.t_7cba54dbcea8') }}</td></tr>
          </tbody>
        </table></div>
        <footer class="pager"><span>{{ $t('legacy.t_3b6ef811b85a') }} {{ pager.totalRows }} {{ $t('legacy.t_a3b79a0ba6f3') }}</span><div><button type="button" :disabled="pager.page <= 1" @click="load(pager.page - 1)">‹</button><span>{{ pager.page }} / {{ pager.totalPages }}</span><button type="button" :disabled="pager.page >= pager.totalPages" @click="load(pager.page + 1)">›</button></div></footer>
      </template>
    </div>

    <div v-if="detailOpen" class="modal-backdrop" @pointerdown.self="closeAccount">
      <section class="account-modal">
        <header class="modal-heading">
          <div><span class="eyebrow">{{ $t('legacy.t_349e2c57c74d') }} {{ detail?.account?.leaseNo }}</span><h3>{{ detail?.account?.tenantName }}</h3><p>{{ detail?.account?.projectName }} · {{ detail?.account?.unitNo }} · {{ displayDate(detail?.account?.startDate) }} {{ $t('legacy.t_43401e739ef4') }} {{ displayDate(detail?.account?.endDate) }}</p></div>
          <div class="heading-actions"><span v-if="detail" class="account-pill" :class="detail.account.accountStatus">{{ $lt(accountStatusLabel(detail.account.accountStatus)) }}</span><button type="button" class="close-button" @click="closeAccount">×</button></div>
        </header>
        <div v-if="detailLoading" class="empty-state detail-loading">{{ $t('legacy.t_6d2c01854db1') }}</div>
        <div v-else-if="detail" class="account-body">
          <div class="balance-grid">
            <article><span>{{ $t('legacy.t_14593e314ae1') }}</span><strong>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(detail.account.expectedDeposit) }}</strong></article>
            <article><span>{{ $t('legacy.t_2551b2246ebd') }}</span><strong>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(detail.account.postedBalance) }}</strong><small>{{ $t('legacy.t_a0b832af312e') }}</small></article>
            <article class="available"><span>{{ $t('legacy.t_4b56cdc83ca1') }}</span><strong>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(detail.account.availableBalance) }}</strong><small>{{ $t('legacy.t_0a31592d558e') }}</small></article>
            <article class="reserve"><span>{{ $t('legacy.t_919b92479c3d') }}</span><strong>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(detail.reserve.currentBalance) }}</strong><small>{{ detail.reserve.ownerName || $t('legacy.t_43b8a8e8a435') }}</small></article>
          </div>

          <section v-for="bill in depositBills" :key="bill.financeRecordId || 'unbilled'" class="business-section bill-section">
            <div class="section-heading"><div><span>{{ $t('legacy.t_ddfe163345d3') }}</span><h4>{{ $t('legacy.t_93134dc548b9') }}</h4><p>{{ $t('legacy.t_bf1d45a1ce05') }}</p></div><span class="bill-status" :class="bill.confirmationStatus">{{ $lt(billStatusLabel(bill.confirmationStatus)) }}</span></div>
            <div class="bill-grid"><div><span>{{ $t('legacy.t_043fe53dbac3') }}</span><strong>{{ bill.transactionNo || $t('legacy.t_6da8b19d8be1') }}</strong></div><div><span>{{ $t('legacy.t_23283a9a4982') }}</span><strong>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(bill.amount) }}</strong></div><div><span>{{ $t('legacy.t_6020be9192cc') }}</span><strong>{{ displayDate(bill.billDate) }}</strong></div><div><span>{{ $t('legacy.t_c56f2365c456') }}</span><strong>{{ $lt(billStatusLabel(bill.confirmationStatus)) }}</strong></div></div>
            <div v-if="can('confirm_collection') && bill.financeRecordId === detail.bill.financeRecordId" class="section-footer"><p>{{ $t('legacy.t_2bc01088abbd') }}</p><button type="button" class="primary-button" @click="confirmCollection">{{ $t('legacy.t_429b23a266be') }}</button></div>
            <p v-else-if="bill.confirmationStatus === 'rejected'" class="inline-warning">{{ $t('legacy.t_f2b57ea77e6f') }}</p>
          </section>

          <section class="business-section ledger-section">
            <div class="section-heading">
              <div><span>{{ $t('legacy.t_bcac9d1d8eab') }}</span><h4>{{ $t('legacy.t_df9ab5f5ab47') }}</h4><p>{{ $t('legacy.t_323017f802ce') }}</p></div>
              <div v-if="!deleteMode" class="action-row">
                <button v-if="deletableTransactions.length" type="button" class="danger-button" @click="startDeleteMode">{{ $t('tenancy.depositDelete') }}</button>
                <button v-if="can('increase_deposit')" type="button" @click="openAction('adjustment_credit')">{{ $t('legacy.t_1495a7b47037') }}</button>
                <button v-if="can('tenant_advance')" type="button" @click="openAction('tenant_advance')">{{ $t('legacy.t_cfbb0c2f2dd0') }}</button>
                <button v-if="can('tenant_repayment')" type="button" @click="openAction('tenant_repayment')">{{ $t('legacy.t_19045e256960') }}</button>
              </div>
              <div v-else class="delete-toolbar">
                <label><input type="checkbox" :checked="allDeletableSelected" @change="toggleDeleteAll"> {{ $t('tenancy.depositDeleteSelectAll') }}</label>
                <span>{{ $t('tenancy.depositDeleteSelected', { count: selectedTransactionIds.length }) }}</span>
                <button type="button" class="outline-button" :disabled="deleteBusy" @click="cancelDeleteMode">{{ $t('tenancy.depositDeleteCancel') }}</button>
                <button type="button" class="delete-confirm-button" :disabled="deleteBusy || !selectedTransactionIds.length" @click="confirmDeleteTransactions">{{ deleteBusy ? $t('tenancy.depositDeleting') : $t('tenancy.depositDeleteConfirm') }}</button>
              </div>
            </div>
            <p v-if="deleteMode" class="delete-hint">{{ $t('tenancy.depositDeleteHint') }}</p>
            <div v-if="detail.transactions.length" class="ledger-list">
              <article v-for="item in detail.transactions" :key="item.id" :class="{ 'delete-selecting': deleteMode, 'delete-selected': isTransactionSelected(item.id), 'delete-locked': deleteMode && !isTransactionDeletable(item) }" @click="toggleTransactionSelection(item)">
                <label v-if="deleteMode && isTransactionDeletable(item)" class="ledger-checkbox" @click.stop><input v-model="selectedTransactionIds" type="checkbox" :value="item.id" :disabled="deleteBusy" :aria-label="$t('ui.selectItemAria', { name: $lt(typeLabel(item.transactionType)) })"></label>
                <div class="ledger-main"><span class="ledger-icon" :class="item.direction">{{ item.direction === 'credit' ? '+' : '−' }}</span><div><strong>{{ $lt(typeLabel(item.transactionType)) }}</strong><p>{{ item.description || $t('legacy.t_4823293f83bd') }}</p><small>{{ displayDate(item.occurredOn) }} · {{ item.leaseNo }}</small></div></div>
                <div class="ledger-value" :class="item.direction"><strong>{{ item.direction === 'credit' ? '+' : '−' }} {{ $t('legacy.t_5e7b60c626a4') }} {{ money(item.amount) }}</strong><span>{{ $t('legacy.t_1ee40bf5c7f1') }} {{ money(item.balanceAfter) }}</span><small>{{ deleteMode && !isTransactionDeletable(item) ? $t('tenancy.depositDeleteLocked') : $lt(transactionStatusLabel(item.status)) }}</small></div>
              </article>
            </div>
            <div v-else class="empty-state compact">{{ $t('legacy.t_11d252356585') }}</div>
          </section>

          <section class="business-section settlement-section" :class="{ locked: detail.account.leaseStatus === 'active' }">
            <div class="section-heading"><div><span>{{ $t('legacy.t_3ea6c91e241f') }}</span><h4>{{ $t('legacy.t_b7ec2805ce8a') }}</h4><p>{{ $t('legacy.t_e9243d163b6d') }}</p></div><span class="settlement-lock">{{ detail.account.leaseStatus === 'active' ? $t('legacy.t_3d9e093c629e') : $t('legacy.t_5c1da664c555') + money(detail.account.availableBalance) }}</span></div>
            <div class="settlement-options"><article><div><strong>{{ $t('legacy.t_b8832de5770a') }}</strong><p>{{ $t('legacy.t_af35f59087ef') }}</p><small>{{ $t('legacy.t_00de6b6da1ca') }} {{ money(detail.reserve.currentBalance) }}</small></div><button type="button" :disabled="!can('refund') || Number(detail.reserve.currentBalance) <= 0" @click="openAction('refund')">{{ $t('legacy.t_b32073e459e7') }}</button></article><article><div><strong>{{ $t('legacy.t_1dc0f5770f80') }}</strong><p>{{ $t('legacy.t_525ed0da54b7') }}</p><small>{{ $t('legacy.t_e2da24a6343b') }}</small></div><button type="button" :disabled="!can('forfeiture')" @click="openAction('forfeiture')">{{ $t('legacy.t_4a4d28b67e82') }}</button></article></div>
          </section>
        </div>
      </section>
    </div>

    <div v-if="actionOpen" class="modal-backdrop action-layer" @pointerdown.self="closeAction">
      <form class="action-modal" @submit.prevent="saveAction">
        <header class="modal-heading"><div><span class="eyebrow">{{ $t('legacy.t_1ca533b6447e') }}</span><h3>{{ actionTitle }}</h3><p>{{ detail?.account?.tenantName }} · {{ detail?.account?.leaseNo }}</p></div><button type="button" class="close-button" @click="closeAction">×</button></header>
        <div class="action-notice" :class="form.transactionType"><strong>{{ $lt(actionNotice.title) }}</strong><p>{{ $lt(actionNotice.text) }}</p></div>
        <div class="action-form"><label><span>{{ $t('legacy.t_042bd2d90dcd') }}</span><input v-model.number="form.amount" type="number" min="0.01" :max="actionMax || undefined" step="0.01" required><small v-if="actionMax">{{ $t('legacy.t_851024181eb9') }} {{ money(actionMax) }}</small></label><label><span>{{ $t('legacy.t_7bb548c5ae54') }}</span><input v-model="form.occurredOn" type="date" required></label><label class="wide"><span>{{ $t('legacy.t_aaa4bf2e63b9') }}</span><textarea v-model.trim="form.description" rows="3" maxlength="500" :placeholder="actionPlaceholder" required></textarea></label></div>
        <p v-if="actionError" class="form-error">{{ $lt(actionError) }}</p>
        <footer><button type="button" class="outline-button" @click="closeAction">{{ $t('legacy.t_4d0b4688c787') }}</button><button type="submit" class="primary-button" :disabled="actionBusy">{{ actionBusy ? $t('legacy.t_1cac8ac7f58f') : actionSubmitLabel }}</button></footer>
      </form>
    </div>
  </section>
</template>

<script>
import { createAdminTenantDepositTransaction, deleteAdminTenantDepositTransactions, fetchAdminDepositAccount, fetchAdminDepositAccounts } from '../services/propertyApi';
import { formatDate } from '../utils/dateFormat';

const localToday = () => new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10);
const emptyForm = type => ({ transactionType: type, amount: '', occurredOn: localToday(), description: '' });

export default {
  name: 'AdminDepositWorkspace',
  inject: ['page'],
  data() {
    return {
      rows: [], summary: { totalAccounts: 0, pendingCollectionCount: 0, activeCount: 0, awaitingSettlementCount: 0, settlingCount: 0, settledCount: 0, totalHeld: 0 },
      pager: { page: 1, pageSize: 20, totalRows: 0, totalPages: 1 }, keyword: '', status: '', loading: false, error: '',
      detailOpen: false, detailLoading: false, selectedLeaseId: null, detail: null, actionOpen: false, actionBusy: false,
      actionError: '', form: emptyForm('adjustment_credit'), deleteMode: false, deleteBusy: false,
      selectedTransactionIds: [],
      statuses: [{ value: '', label: '全部' }, { value: 'pending_collection', label: '待收押金' }, { value: 'active', label: '在管账户' }, { value: 'awaiting_settlement', label: '待退租结算' }, { value: 'settling', label: '结算确认中' }, { value: 'settled', label: '已结清' }],
    };
  },
  computed: {
    depositBills() { return this.detail?.bills?.length ? this.detail.bills : [this.detail.bill]; },
    actionTitle() { return this.typeLabel(this.form.transactionType); },
    actionSubmitLabel() { return ({ refund: '提交返还', forfeiture: '提交没收', tenant_advance: '确认代付', tenant_repayment: '确认还款', adjustment_credit: '确认增加' })[this.form.transactionType] || '确认保存'; },
    actionMax() {
      if (!this.detail) return 0;
      if (this.form.transactionType === 'refund') return Math.min(Number(this.detail.account.availableBalance || 0), Number(this.detail.reserve.currentBalance || 0));
      if (['forfeiture', 'tenant_advance'].includes(this.form.transactionType)) return Number(this.detail.account.availableBalance || 0);
      if (this.form.transactionType === 'tenant_repayment') return this.outstandingAdvance;
      return 0;
    },
    outstandingAdvance() { return (this.detail?.transactions || []).filter(item => item.status === 'posted').reduce((sum, item) => sum + (item.transactionType === 'tenant_advance' ? Number(item.amount || 0) : item.transactionType === 'tenant_repayment' ? -Number(item.amount || 0) : 0), 0); },
    deletableTransactions() { return (this.detail?.transactions || []).filter(item => this.isTransactionDeletable(item)); },
    allDeletableSelected() { return this.deletableTransactions.length > 0 && this.selectedTransactionIds.length === this.deletableTransactions.length; },
    actionNotice() {
      return ({
        adjustment_credit: { title: '增加押金余额', text: '用于补充或修正押金余额，只记录在押金账户中。' },
        tenant_advance: { title: '代付租客费用', text: '从押金余额扣减，不会写入业主账户报表。' },
        tenant_repayment: { title: '租客归还代付款', text: '归还金额补回押金余额，不会写入业主账户报表。' },
        refund: { title: '押金余款返还', text: '提交后等待财务确认；确认时从业主预备金扣除并进入业主账户报表。' },
        forfeiture: { title: '押金余款没收', text: '提交后等待财务确认；确认后作为业主收入进入业主账户报表。' },
      })[this.form.transactionType] || { title: '', text: '' };
    },
    actionPlaceholder() { return ({ adjustment_credit: '例如：补收门禁卡押金', tenant_advance: '填写代付项目及原因', tenant_repayment: '填写租客还款方式或凭证编号', refund: '填写退租结算说明及收款人', forfeiture: '填写没收原因及依据' })[this.form.transactionType] || '填写业务说明'; },
  },
  mounted() { this.load(1); },
  methods: {
    async load(page = 1) { this.loading = true; this.error = ''; try { const data = await fetchAdminDepositAccounts({ page, pageSize: this.pager.pageSize, keyword: this.keyword, status: this.status }); this.rows = data.rows || []; this.summary = data.summary || this.summary; this.pager = data.page || this.pager; } catch (error) { this.error = error?.message || '押金账户加载失败'; } finally { this.loading = false; } },
    search() { this.load(1); },
    changeStatus(status) { this.status = status; this.load(1); },
    async openAccount(row) { this.selectedLeaseId = row.leaseId; this.detailOpen = true; this.cancelDeleteMode(); await this.reloadDetail(); },
    closeAccount() { if (this.actionBusy || this.deleteBusy) return; this.cancelDeleteMode(); this.detailOpen = false; this.detail = null; this.selectedLeaseId = null; },
    async reloadDetail() { this.detailLoading = true; try { this.detail = await fetchAdminDepositAccount(this.selectedLeaseId); } catch (error) { this.page?.showToast?.(error?.message || '押金账户加载失败', 'error'); this.closeAccount(); } finally { this.detailLoading = false; } },
    can(action) { return Boolean(this.detail?.allowedActions?.includes(action)); },
    isTransactionDeletable(item) { return item?.status === 'posted' && !item.financeRecordId && ['adjustment_credit', 'adjustment_debit', 'tenant_advance', 'tenant_repayment'].includes(item.transactionType); },
    isTransactionSelected(id) { return this.selectedTransactionIds.includes(id); },
    startDeleteMode() { this.selectedTransactionIds = []; this.deleteMode = true; },
    cancelDeleteMode() { if (this.deleteBusy) return; this.deleteMode = false; this.selectedTransactionIds = []; },
    toggleDeleteAll(event) { this.selectedTransactionIds = event.target.checked ? this.deletableTransactions.map(item => item.id) : []; },
    toggleTransactionSelection(item) {
      if (!this.deleteMode || this.deleteBusy || !this.isTransactionDeletable(item)) return;
      this.selectedTransactionIds = this.isTransactionSelected(item.id)
        ? this.selectedTransactionIds.filter(id => id !== item.id)
        : [...this.selectedTransactionIds, item.id];
    },
    async confirmDeleteTransactions() {
      if (!this.selectedTransactionIds.length) return;
      if (!window.confirm(this.$t('tenancy.depositDeletePrompt', { count: this.selectedTransactionIds.length }))) return;
      this.deleteBusy = true;
      try {
        await deleteAdminTenantDepositTransactions(this.selectedLeaseId, this.selectedTransactionIds);
        this.deleteMode = false;
        this.selectedTransactionIds = [];
        this.page?.showToast?.(this.$t('tenancy.depositDeleted'));
        await Promise.all([this.reloadDetail(), this.load(this.pager.page)]);
      } catch (error) { this.page?.showToast?.(error?.message || this.$t('tenancy.depositDeleteFailed'), 'error'); }
      finally { this.deleteBusy = false; }
    },
    openAction(type) { this.form = emptyForm(type); this.actionError = ''; this.actionOpen = true; },
    closeAction() { if (!this.actionBusy) { this.actionOpen = false; this.actionError = ''; } },
    async saveAction() {
      this.actionError = '';
      const amount = Number(this.form.amount || 0);
      if (amount <= 0) { this.actionError = '金额必须大于 0'; return; }
      if (this.actionMax && amount > this.actionMax + 0.0001) { this.actionError = `金额不能超过 RM ${this.money(this.actionMax)}`; return; }
      this.actionBusy = true;
      try {
        await createAdminTenantDepositTransaction(this.selectedLeaseId, { ...this.form, amount });
        this.actionOpen = false;
        this.page?.showToast?.(this.$ltf`${this.actionTitle}已提交`);
        await Promise.all([this.reloadDetail(), this.load(this.pager.page)]);
      } catch (error) { this.actionError = error?.message || '押金业务保存失败'; }
      finally { this.actionBusy = false; }
    },
    confirmCollection() {
      this.page.adminFinanceMode = 'tenant_deposit';
      this.page.adminFinanceViewMode = 'pending';
      this.page.selectModule('adminFinance');
    },
    displayDate(value) { return formatDate(value); },
    money(value) { return Number(value || 0).toLocaleString('en-MY', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); },
    leaseStatusLabel(value) { return ({ active: '进行中', expired: '已到期', terminated: '已终止', transferred: '已转租' })[value] || value || '—'; },
    accountStatusLabel(value) { return ({ pending_collection: '待收押金', collection_rejected: '账单已驳回', active: '在管中', awaiting_settlement: '待退租结算', settling: '结算确认中', settled: '已结清' })[value] || value || '—'; },
    billStatusLabel(value) { return ({ pending: '待确认收款', confirmed: '已确认收款', rejected: '已驳回', unbilled: '未生成账单' })[value] || value || '—'; },
    transactionStatusLabel(value) { return ({ posted: '已入账', pending: '待财务确认', cancelled: '已取消' })[value] || value || '—'; },
    typeLabel(value) { return ({ collection: '押金收款', adjustment_credit: '增加押金', adjustment_debit: '押金调减', tenant_advance: '代付租客费用', tenant_repayment: '租客归还代付款', refund: '押金余款返还', forfeiture: '押金余款没收', rent_deduction: '押金抵扣租金' })[value] || value || '押金异动'; },
  },
};
</script>

<style>
@scope (.deposit-page) {
  :scope { padding:18px 24px 30px;color:#123e70 }
.deposit-page{padding:18px 24px 30px;color:#123e70}.page-heading,.filter-card,.pager,.modal-heading,.section-heading,.section-footer,.action-modal footer{display:flex;align-items:center;justify-content:space-between;gap:16px}.eyebrow{color:#008d94;font-size:11px;font-weight:800;letter-spacing:.08em}.page-heading h2,.modal-heading h3{margin:4px 0;color:#073c78}.page-heading h2{font-size:24px}.page-heading p,.modal-heading p{margin:0;color:#7088a2;font-size:13px}.outline-button,.primary-button,.filter-card button,.pager button,.action-row button,.settlement-options button{border:1px solid #c9dbe9;border-radius:8px;background:#fff;color:#164b7d;cursor:pointer}.outline-button{padding:10px 16px}.primary-button{padding:10px 16px;border-color:#078d94;background:#078d94;color:#fff;font-weight:800}.primary-button.small{padding:7px 12px;font-size:12px}.summary-grid{display:grid;grid-template-columns:repeat(5,minmax(140px,1fr));gap:12px;margin:18px 0}.summary-grid article{padding:14px 16px;border:1px solid #d8e6ef;border-top:3px solid #0a969c;border-radius:11px;background:#fff}.summary-grid article.warning{border-top-color:#e3a600}.summary-grid article.money{border-top-color:#176fbe}.summary-grid span,.summary-grid small{display:block;color:#7188a1;font-size:11px}.summary-grid strong{display:block;margin:5px 0;color:#073e75;font-size:23px}.filter-card{justify-content:flex-start;padding:12px 14px;border:1px solid #d8e5ef;border-radius:11px 11px 0 0;background:#fff}.search-box{display:flex;align-items:center;gap:8px;width:min(360px,100%);padding:0 11px;border:1px solid #cbdce9;border-radius:8px}.search-box input{width:100%;padding:10px 0;border:0;outline:0;color:#174776;font:inherit}.status-tabs{display:flex;align-items:center;gap:5px;overflow:auto}.filter-card button{padding:8px 11px;white-space:nowrap}.filter-card button.active{border-color:#078f96;background:#e9f8f7;color:#087a80;font-weight:800}.account-card{border:1px solid #d8e5ef;border-top:0;border-radius:0 0 11px 11px;background:#fff;overflow:hidden}.table-wrap{overflow:auto}.account-card table{width:100%;min-width:1120px;border-collapse:collapse;font-size:13px}.account-card th{padding:11px 13px;text-align:left;color:#55718d;background:#f4f8fb;border-bottom:1px solid #dce7ef}.account-card td{padding:12px 13px;border-bottom:1px solid #e5edf3;vertical-align:middle}.account-card td strong,.account-card td small{display:block}.account-card td small{margin-top:4px;color:#7a90a5;font-size:11px}.account-card .amount{white-space:nowrap;font-variant-numeric:tabular-nums}.account-card .amount.strong{color:#087d79;font-weight:800}.lease-pill,.account-pill,.bill-status{display:inline-flex;padding:4px 9px;border-radius:999px;font-size:11px;white-space:nowrap}.lease-pill.active,.account-pill.active,.account-pill.settled,.bill-status.confirmed{background:#e7f8ed;color:#128753}.lease-pill.expired,.lease-pill.terminated,.lease-pill.transferred{background:#eef2f6;color:#64778b}.account-pill.pending_collection,.account-pill.awaiting_settlement,.account-pill.settling,.bill-status.pending{background:#fff3d5;color:#a66b00}.account-pill.collection_rejected,.bill-status.rejected{background:#fdebea;color:#c64b44}.pager{padding:12px 14px;color:#667f98;font-size:12px}.pager div{display:flex;align-items:center;gap:9px}.pager button{padding:5px 10px}.empty-state{padding:42px;text-align:center;color:#778da3}.empty-state.error{color:#c34c47}.empty-state.error button{margin-left:10px}.empty-state.compact{padding:20px}.modal-backdrop{position:fixed;inset:0;z-index:40;display:grid;place-items:center;padding:18px;background:rgba(5,29,49,.48)}.account-modal{width:min(1160px,calc(100vw - 32px));max-height:calc(100vh - 36px);border-radius:14px;background:#f5f8fa;box-shadow:0 25px 70px rgba(4,35,59,.28);overflow:hidden}.modal-heading{padding:17px 20px;border-bottom:1px solid #dbe7ef;background:#fff}.modal-heading h3{font-size:20px}.heading-actions{display:flex;align-items:center;gap:12px}.close-button{display:grid;place-items:center;width:34px;height:34px;border:1px solid #c9dae8;border-radius:8px;background:#fff;color:#174b77;font-size:22px;cursor:pointer}.account-body{max-height:calc(100vh - 132px);padding:16px 18px 24px;overflow:auto}.balance-grid{display:grid;grid-template-columns:repeat(4,minmax(150px,1fr));gap:10px}.balance-grid article{padding:13px 15px;border:1px solid #d8e5ee;border-radius:10px;background:#fff}.balance-grid span,.balance-grid small{display:block;color:#74899f;font-size:11px}.balance-grid strong{display:block;margin:5px 0;color:#0a4378;font-size:20px}.balance-grid .available{border-color:#9ddbd7;background:#f0faf9}.balance-grid .reserve{border-color:#efd39a;background:#fffaf0}.business-section{margin-top:13px;padding:16px 18px;border:1px solid #d8e5ee;border-radius:11px;background:#fff}.section-heading{align-items:flex-start}.section-heading>div:first-child{display:grid;grid-template-columns:auto 1fr;column-gap:9px}.section-heading>div:first-child>span{grid-row:1/3;display:grid;place-items:center;width:30px;height:30px;border-radius:8px;background:#e9f7f7;color:#078890;font-weight:900}.section-heading h4{margin:0;color:#0a4377}.section-heading p{grid-column:2;margin:4px 0 0;color:#72889d;font-size:12px}.bill-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:9px;margin-top:14px}.bill-grid>div{padding:11px;border-radius:8px;background:#f5f8fa}.bill-grid span,.bill-grid strong{display:block}.bill-grid span{color:#71879c;font-size:11px}.bill-grid strong{margin-top:5px;color:#193f69;font-size:13px}.section-footer{margin-top:12px;padding-top:12px;border-top:1px solid #e2ebf1}.section-footer p{margin:0;color:#72879a;font-size:12px}.inline-warning{margin:12px 0 0;padding:10px;border-radius:7px;background:#fff0ee;color:#b94d45;font-size:12px}.action-row{display:flex;flex-wrap:wrap;justify-content:flex-end;gap:7px}.action-row button,.settlement-options button{padding:8px 11px}.action-row button:first-child{border-color:#078f96;color:#078087;font-weight:800}.ledger-list{display:grid;gap:8px;margin-top:14px}.ledger-list article{display:flex;align-items:center;justify-content:space-between;gap:15px;padding:11px 13px;border:1px solid #e0e9ef;border-radius:9px;background:#fbfdfe}.ledger-main{display:flex;align-items:center;gap:11px}.ledger-icon{display:grid;place-items:center;width:30px;height:30px;border-radius:50%;font-weight:900}.ledger-icon.credit{background:#e5f8ed;color:#168c53}.ledger-icon.debit{background:#fff0ea;color:#c25c3d}.ledger-main p{margin:3px 0;color:#647e96;font-size:12px}.ledger-main small,.ledger-value span,.ledger-value small{display:block;color:#7a8fa3;font-size:11px}.ledger-value{text-align:right}.ledger-value strong{display:block}.ledger-value.credit strong{color:#168b53}.ledger-value.debit strong{color:#ca5a3d}.ledger-value span{margin-top:3px}.settlement-section{border-color:#ead49e;background:#fffdf8}.settlement-section.locked{border-color:#dce5eb;background:#f8fafb}.settlement-lock{padding:6px 10px;border-radius:999px;background:#fff1cf;color:#9f6900;font-size:11px}.locked .settlement-lock{background:#edf1f4;color:#677b8c}.settlement-options{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:14px}.settlement-options article{display:flex;align-items:center;justify-content:space-between;gap:15px;padding:13px;border:1px solid #eadcba;border-radius:9px;background:#fff}.settlement-options strong,.settlement-options p,.settlement-options small{display:block}.settlement-options p{margin:4px 0;color:#6e8295;font-size:12px}.settlement-options small{color:#a47218;font-size:11px}.settlement-options button{flex:0 0 auto;border-color:#d69d24;color:#996300;font-weight:800}.action-layer{z-index:50}.action-modal{width:min(620px,calc(100vw - 32px));border-radius:13px;background:#fff;box-shadow:0 24px 65px rgba(4,35,59,.3);overflow:hidden}.action-notice{margin:16px 20px 0;padding:11px 13px;border-left:4px solid #078f96;border-radius:7px;background:#eef9f8}.action-notice.refund,.action-notice.forfeiture{border-color:#dfa610;background:#fff9e9}.action-notice strong{color:#164b75}.action-notice p{margin:4px 0 0;color:#698198;font-size:12px}.action-form{display:grid;grid-template-columns:1fr 1fr;gap:14px;padding:18px 20px}.action-form label{display:grid;gap:6px;color:#315878;font-size:12px}.action-form input,.action-form textarea{box-sizing:border-box;width:100%;padding:10px;border:1px solid #c9dbe9;border-radius:7px;color:#163f6b;font:inherit}.action-form small{color:#7b8fa3}.action-form .wide{grid-column:1/-1}.action-form textarea{resize:vertical}.form-error{margin:0 20px 14px;padding:9px 11px;border-radius:7px;background:#fdecea;color:#bd4741;font-size:12px}.action-modal footer{justify-content:flex-end;padding:14px 20px;border-top:1px solid #dce7ef}.detail-loading{background:#fff}
.action-row .danger-button{border-color:#e1aaa5;color:#b9433d;font-weight:800}.delete-toolbar{display:flex;align-items:center;justify-content:flex-end;flex-wrap:wrap;gap:9px;color:#607b94;font-size:12px}.delete-toolbar label{display:flex;align-items:center;gap:5px;cursor:pointer}.delete-toolbar button{padding:8px 11px}.delete-confirm-button{padding:8px 11px;border:1px solid #c94b43;border-radius:8px;background:#c94b43;color:#fff;font-weight:800;cursor:pointer}.delete-hint{margin:12px 0 0;padding:9px 11px;border-radius:7px;background:#fff7f6;color:#9d4c47;font-size:12px}.ledger-list article.delete-selecting{cursor:pointer}.ledger-list article.delete-selected{border-color:#cf635d;background:#fff7f6}.ledger-list article.delete-locked{cursor:not-allowed;opacity:.62}.ledger-checkbox{display:grid;place-items:center;flex:0 0 auto}.ledger-checkbox input{width:17px;height:17px;accent-color:#c94b43}.ledger-main{flex:1}
button:disabled{opacity:.48;cursor:not-allowed}@media(max-width:1050px){.summary-grid{grid-template-columns:repeat(3,1fr)}.balance-grid{grid-template-columns:repeat(2,1fr)}}@media(max-width:760px){.deposit-page{padding:14px}.page-heading,.filter-card,.section-heading{align-items:flex-start;flex-direction:column}.summary-grid{grid-template-columns:1fr 1fr}.status-tabs{width:100%}.bill-grid,.settlement-options,.action-form{grid-template-columns:1fr}.action-form .wide{grid-column:auto}.action-row,.delete-toolbar{justify-content:flex-start}.ledger-list article,.settlement-options article{align-items:flex-start}.balance-grid{grid-template-columns:1fr}.modal-backdrop{padding:8px}.account-modal{width:calc(100vw - 16px);max-height:calc(100vh - 16px)}}
}
</style>
