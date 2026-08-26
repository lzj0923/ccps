<template>
  <section class="deposit-page">
    <header class="page-heading">
      <div><span class="eyebrow">租赁财务</span><h2>押金管理</h2><p>管理押金账单、账户异动及退租结算；代付与租客还款不会进入业主账户报表。</p></div>
      <button type="button" class="outline-button" :disabled="loading" @click="load(pager.page)">刷新</button>
    </header>

    <div class="summary-grid">
      <article><span>押金账户</span><strong>{{ summary.totalAccounts }}</strong><small>全部租约</small></article>
      <article class="warning"><span>待收押金</span><strong>{{ summary.pendingCollectionCount }}</strong><small>待确认账单</small></article>
      <article><span>在管账户</span><strong>{{ summary.activeCount }}</strong><small>租约进行中</small></article>
      <article class="warning"><span>待退租结算</span><strong>{{ summary.awaitingSettlementCount }}</strong><small>租约已结束</small></article>
      <article class="money"><span>账面押金余额</span><strong>RM {{ money(summary.totalHeld) }}</strong><small>已入账合计</small></article>
    </div>

    <div class="filter-card">
      <label class="search-box"><span>⌕</span><input v-model.trim="keyword" placeholder="搜索租客、租约、建案或房产" @keyup.enter="search"></label>
      <div class="status-tabs">
        <button v-for="item in statuses" :key="item.value" type="button" :class="{ active: status === item.value }" @click="changeStatus(item.value)">{{ item.label }}</button>
      </div>
    </div>

    <div class="account-card">
      <div v-if="loading" class="empty-state">正在加载押金账户…</div>
      <div v-else-if="error" class="empty-state error"><strong>{{ error }}</strong><button type="button" @click="load(pager.page)">重试</button></div>
      <template v-else>
        <div class="table-wrap"><table>
          <thead><tr><th>租客 / 租约</th><th>房产</th><th>租约状态</th><th>应收押金</th><th>账面余额</th><th>押金状态</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="row in rows" :key="row.leaseId">
              <td><strong>{{ row.tenantName }}</strong><small>{{ row.leaseNo }} · {{ row.tenantPhone || '未留电话' }}</small></td>
              <td><strong>{{ row.projectName }}</strong><small>{{ row.unitNo }} · {{ displayDate(row.startDate) }} 至 {{ displayDate(row.endDate) }}</small></td>
              <td><span class="lease-pill" :class="row.leaseStatus">{{ leaseStatusLabel(row.leaseStatus) }}</span></td>
              <td class="amount">RM {{ money(row.expectedDeposit) }}</td>
              <td class="amount strong">RM {{ money(row.postedBalance) }}<small v-if="Number(row.postedBalance) !== Number(row.availableBalance)">可用 RM {{ money(row.availableBalance) }}</small></td>
              <td><span class="account-pill" :class="row.accountStatus">{{ accountStatusLabel(row.accountStatus) }}</span></td>
              <td><button type="button" class="primary-button small" @click="openAccount(row)">进入账户</button></td>
            </tr>
            <tr v-if="!rows.length"><td colspan="7" class="empty-state">没有符合条件的押金账户</td></tr>
          </tbody>
        </table></div>
        <footer class="pager"><span>共 {{ pager.totalRows }} 个账户</span><div><button type="button" :disabled="pager.page <= 1" @click="load(pager.page - 1)">‹</button><span>{{ pager.page }} / {{ pager.totalPages }}</span><button type="button" :disabled="pager.page >= pager.totalPages" @click="load(pager.page + 1)">›</button></div></footer>
      </template>
    </div>

    <div v-if="detailOpen" class="modal-backdrop" @pointerdown.self="closeAccount">
      <section class="account-modal">
        <header class="modal-heading">
          <div><span class="eyebrow">押金账户 · {{ detail?.account?.leaseNo }}</span><h3>{{ detail?.account?.tenantName }}</h3><p>{{ detail?.account?.projectName }} · {{ detail?.account?.unitNo }} · {{ displayDate(detail?.account?.startDate) }} 至 {{ displayDate(detail?.account?.endDate) }}</p></div>
          <div class="heading-actions"><span v-if="detail" class="account-pill" :class="detail.account.accountStatus">{{ accountStatusLabel(detail.account.accountStatus) }}</span><button type="button" class="close-button" @click="closeAccount">×</button></div>
        </header>
        <div v-if="detailLoading" class="empty-state detail-loading">正在加载账户资料…</div>
        <div v-else-if="detail" class="account-body">
          <div class="balance-grid">
            <article><span>租约应收押金</span><strong>RM {{ money(detail.account.expectedDeposit) }}</strong></article>
            <article><span>账面余额</span><strong>RM {{ money(detail.account.postedBalance) }}</strong><small>已完成异动</small></article>
            <article class="available"><span>当前可用余额</span><strong>RM {{ money(detail.account.availableBalance) }}</strong><small>已扣除待确认结算</small></article>
            <article class="reserve"><span>业主预备金</span><strong>RM {{ money(detail.reserve.currentBalance) }}</strong><small>{{ detail.reserve.ownerName || '未关联业主' }}</small></article>
          </div>

          <section class="business-section bill-section">
            <div class="section-heading"><div><span>01</span><h4>押金账单</h4><p>租约建立时生成应收押金账单，确认收款后计入押金余额。</p></div><span class="bill-status" :class="detail.bill.confirmationStatus">{{ billStatusLabel(detail.bill.confirmationStatus) }}</span></div>
            <div class="bill-grid"><div><span>账单编号</span><strong>{{ detail.bill.transactionNo || '尚未生成' }}</strong></div><div><span>账单金额</span><strong>RM {{ money(detail.bill.amount) }}</strong></div><div><span>账单日期</span><strong>{{ displayDate(detail.bill.billDate) }}</strong></div><div><span>收款状态</span><strong>{{ billStatusLabel(detail.bill.confirmationStatus) }}</strong></div></div>
            <div v-if="can('confirm_collection')" class="section-footer"><p>押金金额与实际收款日期统一由财务确认。</p><button type="button" class="primary-button" @click="confirmCollection">前往财务确认</button></div>
            <p v-else-if="detail.bill.confirmationStatus === 'rejected'" class="inline-warning">该押金账单已驳回，请先修正租约押金资料。</p>
          </section>

          <section class="business-section ledger-section">
            <div class="section-heading"><div><span>02</span><h4>押金账户明细</h4><p>代付租客费用和租客还款只记录在这里，不进入业主账户报表。</p></div><div class="action-row"><button v-if="can('increase_deposit')" type="button" @click="openAction('adjustment_credit')">＋ 增加押金</button><button v-if="can('tenant_advance')" type="button" @click="openAction('tenant_advance')">代付租客费用</button><button v-if="can('tenant_repayment')" type="button" @click="openAction('tenant_repayment')">登记租客还款</button></div></div>
            <div v-if="detail.transactions.length" class="ledger-list"><article v-for="item in detail.transactions" :key="item.id"><div class="ledger-main"><span class="ledger-icon" :class="item.direction">{{ item.direction === 'credit' ? '+' : '−' }}</span><div><strong>{{ typeLabel(item.transactionType) }}</strong><p>{{ item.description || '无备注' }}</p><small>{{ displayDate(item.occurredOn) }} · {{ item.leaseNo }}</small></div></div><div class="ledger-value" :class="item.direction"><strong>{{ item.direction === 'credit' ? '+' : '−' }} RM {{ money(item.amount) }}</strong><span>余额 RM {{ money(item.balanceAfter) }}</span><small>{{ transactionStatusLabel(item.status) }}</small></div></article></div>
            <div v-else class="empty-state compact">暂无押金明细</div>
          </section>

          <section class="business-section settlement-section" :class="{ locked: detail.account.leaseStatus === 'active' }">
            <div class="section-heading"><div><span>03</span><h4>退租押金结算</h4><p>租约结束后，押金余款只能选择返还租客或没收；两种结果都会进入业主账户报表。</p></div><span class="settlement-lock">{{ detail.account.leaseStatus === 'active' ? '租约进行中，暂不可结算' : '可结算余额 RM ' + money(detail.account.availableBalance) }}</span></div>
            <div class="settlement-options"><article><div><strong>押金余款返还</strong><p>财务确认后从业主预备金扣除，并写入业主账户报表。</p><small>当前预备金：RM {{ money(detail.reserve.currentBalance) }}</small></div><button type="button" :disabled="!can('refund') || Number(detail.reserve.currentBalance) <= 0" @click="openAction('refund')">办理返还</button></article><article><div><strong>押金余款没收</strong><p>没收金额转为业主收入，并写入业主账户报表。</p><small>需经过财务确认后完成结算</small></div><button type="button" :disabled="!can('forfeiture')" @click="openAction('forfeiture')">办理没收</button></article></div>
          </section>
        </div>
      </section>
    </div>

    <div v-if="actionOpen" class="modal-backdrop action-layer" @pointerdown.self="closeAction">
      <form class="action-modal" @submit.prevent="saveAction">
        <header class="modal-heading"><div><span class="eyebrow">押金业务</span><h3>{{ actionTitle }}</h3><p>{{ detail?.account?.tenantName }} · {{ detail?.account?.leaseNo }}</p></div><button type="button" class="close-button" @click="closeAction">×</button></header>
        <div class="action-notice" :class="form.transactionType"><strong>{{ actionNotice.title }}</strong><p>{{ actionNotice.text }}</p></div>
        <div class="action-form"><label><span>金额（RM）</span><input v-model.number="form.amount" type="number" min="0.01" :max="actionMax || undefined" step="0.01" required><small v-if="actionMax">本次最多 RM {{ money(actionMax) }}</small></label><label><span>发生日期</span><input v-model="form.occurredOn" type="date" required></label><label class="wide"><span>业务说明</span><textarea v-model.trim="form.description" rows="3" maxlength="500" :placeholder="actionPlaceholder" required></textarea></label></div>
        <p v-if="actionError" class="form-error">{{ actionError }}</p>
        <footer><button type="button" class="outline-button" @click="closeAction">取消</button><button type="submit" class="primary-button" :disabled="actionBusy">{{ actionBusy ? '处理中…' : actionSubmitLabel }}</button></footer>
      </form>
    </div>
  </section>
</template>

<script>
import { createAdminTenantDepositTransaction, fetchAdminDepositAccount, fetchAdminDepositAccounts } from '../services/propertyApi';
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
      actionError: '', form: emptyForm('adjustment_credit'),
      statuses: [{ value: '', label: '全部' }, { value: 'pending_collection', label: '待收押金' }, { value: 'active', label: '在管账户' }, { value: 'awaiting_settlement', label: '待退租结算' }, { value: 'settling', label: '结算确认中' }, { value: 'settled', label: '已结清' }],
    };
  },
  computed: {
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
    async openAccount(row) { this.selectedLeaseId = row.leaseId; this.detailOpen = true; await this.reloadDetail(); },
    closeAccount() { if (this.actionBusy) return; this.detailOpen = false; this.detail = null; this.selectedLeaseId = null; },
    async reloadDetail() { this.detailLoading = true; try { this.detail = await fetchAdminDepositAccount(this.selectedLeaseId); } catch (error) { this.page?.showToast?.(error?.message || '押金账户加载失败', 'error'); this.closeAccount(); } finally { this.detailLoading = false; } },
    can(action) { return Boolean(this.detail?.allowedActions?.includes(action)); },
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
        this.page?.showToast?.(`${this.actionTitle}已提交`);
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
button:disabled{opacity:.48;cursor:not-allowed}@media(max-width:1050px){.summary-grid{grid-template-columns:repeat(3,1fr)}.balance-grid{grid-template-columns:repeat(2,1fr)}}@media(max-width:760px){.deposit-page{padding:14px}.page-heading,.filter-card,.section-heading{align-items:flex-start;flex-direction:column}.summary-grid{grid-template-columns:1fr 1fr}.status-tabs{width:100%}.bill-grid,.settlement-options,.action-form{grid-template-columns:1fr}.action-form .wide{grid-column:auto}.action-row{justify-content:flex-start}.ledger-list article,.settlement-options article{align-items:flex-start}.balance-grid{grid-template-columns:1fr}.modal-backdrop{padding:8px}.account-modal{width:calc(100vw - 16px);max-height:calc(100vh - 16px)}}
}
</style>
