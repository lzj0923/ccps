<template>
  <section class="owner-finance-page">
    <div class="owner-finance-summary">
      <article v-for="card in summaryCards" :key="card.key" :class="['owner-finance-summary-card', card.tone]">
        <div class="owner-finance-summary-icon"><component :is="card.icon" :size="24" :stroke-width="1.9" aria-hidden="true" /></div>
        <div><span>{{ card.label }}</span><strong>RM {{ money(card.value) }}</strong><small>{{ card.note }}</small></div>
      </article>
    </div>

    <section class="owner-finance-cumulative">
      <header>
        <div><span>{{ copy.cumulativeEyebrow }}</span><h2>{{ copy.cumulativeTitle }}</h2><p>{{ copy.cumulativeHint }}</p></div>
        <small>{{ copy.allRecords }}</small>
      </header>
      <div class="owner-finance-cumulative-values">
        <article class="income"><ArrowUpRight :size="21" :stroke-width="2" aria-hidden="true" /><div><span>{{ copy.cumulativeIncome }}</span><strong>RM {{ money(cumulativeIncome) }}</strong></div></article>
        <article class="expense"><ArrowDownLeft :size="21" :stroke-width="2" aria-hidden="true" /><div><span>{{ copy.cumulativeExpense }}</span><strong>RM {{ money(cumulativeExpense) }}</strong></div></article>
      </div>
    </section>

    <div v-if="errorMessage" class="owner-finance-message"><AlertCircle :size="17" aria-hidden="true" /><span>{{ errorMessage }}</span><button type="button" @click="loadData">{{ copy.retry }}</button></div>

    <div class="owner-finance-workspace">
      <main class="owner-finance-main">
        <section class="owner-finance-section owner-finance-modules">
          <header><div><h2>{{ copy.modulesTitle }}</h2><p>{{ copy.modulesHint }}</p></div><button type="button" :disabled="loading" @click="loadData"><RefreshCw :size="15" :class="{ spinning: loading }" aria-hidden="true" />{{ copy.refresh }}</button></header>
          <div class="owner-finance-module-grid">
            <button v-for="item in moduleCards" :key="item.id" type="button" @click="selectModule(item.id)">
              <span :class="['owner-finance-module-icon', item.tone]"><component :is="item.icon" :size="22" :stroke-width="1.9" aria-hidden="true" /></span>
              <span class="owner-finance-module-copy"><strong>{{ item.title }}</strong><small>{{ item.detail }}</small></span>
              <span class="owner-finance-module-value">{{ item.value }}</span>
              <ChevronRight :size="17" aria-hidden="true" />
            </button>
          </div>
        </section>

        <section class="owner-finance-section owner-finance-ledger">
          <header>
            <div><h2>{{ copy.ledgerTitle }}</h2><p>{{ periodLabel }} · {{ copy.ledgerHint }}</p></div>
            <div class="owner-finance-ledger-actions">
              <button v-for="option in flowFilters" :key="option.value" type="button" :class="{ active: flowFilter === option.value }" @click="flowFilter = option.value">{{ option.label }}</button>
            </div>
          </header>
          <div class="owner-finance-table-wrap">
            <table>
              <thead><tr><th>{{ copy.date }}</th><th>{{ copy.property }}</th><th>{{ copy.description }}</th><th>{{ copy.category }}</th><th>{{ copy.status }}</th><th>{{ copy.amount }}</th></tr></thead>
              <tbody>
                <tr v-for="row in visibleCashflow" :key="row.key">
                  <td>{{ row.date || '—' }}</td>
                  <td><strong>{{ row.project || '—' }}</strong><small>{{ row.unit || '—' }}</small></td>
                  <td>{{ row.description }}</td>
                  <td><span :class="['owner-finance-flow-type', row.direction]">{{ row.type }}</span></td>
                  <td><span class="owner-finance-flow-status">{{ row.status }}</span></td>
                  <td :class="['owner-finance-flow-amount', row.direction]">{{ row.direction === 'income' ? '+' : '−' }} RM {{ money(row.amount) }}</td>
                </tr>
                <tr v-if="loading"><td colspan="6" class="owner-finance-empty">{{ copy.loading }}</td></tr>
                <tr v-else-if="!visibleCashflow.length"><td colspan="6" class="owner-finance-empty">{{ copy.empty }}</td></tr>
              </tbody>
            </table>
          </div>
          <footer><span>{{ copy.totalPrefix }} {{ filteredCashflow.length }} {{ copy.totalSuffix }}</span><button type="button" @click="selectModule('ownerExpenses')">{{ copy.viewAll }}<ChevronRight :size="15" aria-hidden="true" /></button></footer>
        </section>
      </main>

      <aside class="owner-finance-side">
        <section class="owner-finance-side-card">
          <header><h2>{{ copy.attentionTitle }}</h2><span>{{ attentionItems.length }}</span></header>
          <div class="owner-finance-attention-list">
            <button v-for="item in attentionItems" :key="item.id" type="button" @click="selectModule(item.id)">
              <span :class="item.tone"><component :is="item.icon" :size="18" :stroke-width="1.9" aria-hidden="true" /></span>
              <span><strong>{{ item.title }}</strong><small>{{ item.detail }}</small></span>
              <ChevronRight :size="15" aria-hidden="true" />
            </button>
          </div>
        </section>

        <section class="owner-finance-side-card owner-finance-property-summary">
          <header><h2>{{ copy.propertyTitle }}</h2><span>{{ propertyRows.length }}</span></header>
          <div v-if="propertyRows.length">
            <article v-for="property in propertyRows.slice(0, 5)" :key="property.ownerUnitId">
              <div><strong>{{ property.projectName || '—' }}</strong><small>{{ property.unitNo || '—' }}</small></div>
              <dl><div><dt>{{ copy.income }}</dt><dd>RM {{ money(property.monthlyIncome) }}</dd></div><div><dt>{{ copy.expense }}</dt><dd>RM {{ money(property.monthlyExpense) }}</dd></div></dl>
            </article>
          </div>
          <p v-else>{{ copy.noProperty }}</p>
        </section>
      </aside>
    </div>
  </section>
</template>

<script>
import pageBridge from '../pageBridge';
import { fetchOwnerExpenses, fetchOwnerRentIncome, fetchOwnerReserve } from '../services/propertyApi';
import { AlertCircle, ArrowDownLeft, ArrowUpRight, ChevronRight, CircleDollarSign, Coins, RefreshCw, ShieldCheck, WalletCards, Wrench } from '@lucide/vue';

const localDate = value => `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}`;
const currentPeriod = () => {
  const now = new Date();
  return {
    year: now.getFullYear(), month: now.getMonth() + 1,
    startDate: localDate(new Date(now.getFullYear(), now.getMonth(), 1)),
    endDate: localDate(new Date(now.getFullYear(), now.getMonth() + 1, 0))
  };
};

const COPY = {
  'zh-CN': { cumulativeEyebrow: '累计收支', cumulativeTitle: '全部财务记录汇总', cumulativeHint: '统计系统内截至目前的全部收入流水与已登记支出。', cumulativeIncome: '累计总收入', cumulativeExpense: '累计总支出', allRecords: '全部记录', modulesTitle: '财务功能', modulesHint: '三个财务功能集中在这里，点击即可进入详细页面。', refresh: '刷新数据', retry: '重新加载', ledgerTitle: '金额流水', ledgerHint: '展示本月租金收入与支出记录', all: '全部', incomeOnly: '仅收入', expenseOnly: '仅支出', date: '日期', property: '房产', description: '说明', category: '类型', status: '状态', amount: '金额', loading: '正在加载财务数据…', empty: '本月暂无金额流水', totalPrefix: '共', totalSuffix: '笔流水', viewAll: '查看全部收支', attentionTitle: '需要关注', propertyTitle: '各房产本月收支', income: '收入', expense: '支出', noProperty: '暂无房产收支资料', totalIncome: '本月总收入', totalExpense: '本月总支出', netBalance: '本月净结余', reserveBalance: '预备金余额', confirmedRent: '本月全部收入流水', confirmedExpense: '本月已登记支出', incomeMinusExpense: '收入减去支出', availableReserve: '当前全部账户余额', rentModule: '租金收入', expenseModule: '收支维修', reserveModule: '预备金', received: '本月已收', unpaid: '待收', expenseDetail: '本月支出及维修记录', pendingMaintenance: '待处理维修', reserveDetail: '账户余额与充值扣款', lowAccounts: '低余额账户', rentFlow: '租金收入', expenseFlow: '房产支出', pendingRentTitle: '仍有租金待收', pendingMaintenanceTitle: '维修事项待处理', lowReserveTitle: '预备金低于标准', loadPartial: '部分财务资料加载失败，当前页面已显示可用数据。', loadFailed: '财务资料加载失败，请稍后重试。' },
  'zh-TW': { cumulativeEyebrow: '累計收支', cumulativeTitle: '全部財務記錄彙總', cumulativeHint: '統計系統內截至目前的全部收入流水與已登記支出。', cumulativeIncome: '累計總收入', cumulativeExpense: '累計總支出', allRecords: '全部記錄', modulesTitle: '財務功能', modulesHint: '三個財務功能集中在這裡，點擊即可進入詳細頁面。', refresh: '重新整理', retry: '重新載入', ledgerTitle: '金額流水', ledgerHint: '顯示本月租金收入與支出記錄', all: '全部', incomeOnly: '僅收入', expenseOnly: '僅支出', date: '日期', property: '房產', description: '說明', category: '類型', status: '狀態', amount: '金額', loading: '正在載入財務資料…', empty: '本月暫無金額流水', totalPrefix: '共', totalSuffix: '筆流水', viewAll: '查看全部收支', attentionTitle: '需要關注', propertyTitle: '各房產本月收支', income: '收入', expense: '支出', noProperty: '暫無房產收支資料', totalIncome: '本月總收入', totalExpense: '本月總支出', netBalance: '本月淨結餘', reserveBalance: '預備金餘額', confirmedRent: '本月全部收入流水', confirmedExpense: '本月已登記支出', incomeMinusExpense: '收入減去支出', availableReserve: '目前全部帳戶餘額', rentModule: '租金收入', expenseModule: '收支維修', reserveModule: '預備金', received: '本月已收', unpaid: '待收', expenseDetail: '本月支出及維修記錄', pendingMaintenance: '待處理維修', reserveDetail: '帳戶餘額與充值扣款', lowAccounts: '低餘額帳戶', rentFlow: '租金收入', expenseFlow: '房產支出', pendingRentTitle: '仍有租金待收', pendingMaintenanceTitle: '維修事項待處理', lowReserveTitle: '預備金低於標準', loadPartial: '部分財務資料載入失敗，目前頁面已顯示可用資料。', loadFailed: '財務資料載入失敗，請稍後重試。' },
  en: { cumulativeEyebrow: 'Cumulative finances', cumulativeTitle: 'All finance records', cumulativeHint: 'All income and expenses recorded in the system to date.', cumulativeIncome: 'Total income', cumulativeExpense: 'Total expenses', allRecords: 'All records', modulesTitle: 'Finance functions', modulesHint: 'Open rental income, expenses and maintenance, or reserve fund details.', refresh: 'Refresh', retry: 'Retry', ledgerTitle: 'Cash flow', ledgerHint: 'Rental income and expenses for this month', all: 'All', incomeOnly: 'Income', expenseOnly: 'Expenses', date: 'Date', property: 'Property', description: 'Description', category: 'Type', status: 'Status', amount: 'Amount', loading: 'Loading finance data…', empty: 'No cash flow for this month', totalPrefix: '', totalSuffix: 'entries', viewAll: 'View all expenses', attentionTitle: 'Needs attention', propertyTitle: 'Monthly property finances', income: 'Income', expense: 'Expenses', noProperty: 'No property finance data', totalIncome: 'Total income this month', totalExpense: 'Total expenses this month', netBalance: 'Net balance this month', reserveBalance: 'Reserve balance', confirmedRent: 'All income this month', confirmedExpense: 'Recorded expenses this month', incomeMinusExpense: 'Income less expenses', availableReserve: 'Available across all accounts', rentModule: 'Rental income', expenseModule: 'Income & maintenance', reserveModule: 'Reserve fund', received: 'Received this month', unpaid: 'Outstanding', expenseDetail: 'Expenses and maintenance this month', pendingMaintenance: 'Pending maintenance', reserveDetail: 'Balances, top-ups and deductions', lowAccounts: 'Low-balance accounts', rentFlow: 'Rental income', expenseFlow: 'Property expense', pendingRentTitle: 'Rent remains outstanding', pendingMaintenanceTitle: 'Maintenance needs attention', lowReserveTitle: 'Reserve below minimum', loadPartial: 'Some finance data could not be loaded. Available data is shown.', loadFailed: 'Finance data could not be loaded. Please try again.' }
};

export default {
  components: { AlertCircle, ArrowDownLeft, ArrowUpRight, ChevronRight, CircleDollarSign, Coins, RefreshCw, ShieldCheck, WalletCards, Wrench },
  mixins: [pageBridge],
  data() { return { loading: false, errorMessage: '', flowFilter: 'all', rent: { summary: {}, records: [] }, expenses: { summary: {}, expenses: [] }, reserve: { summary: {}, accounts: [] }, period: currentPeriod() }; },
  computed: {
    copy() { return COPY[this.$i18n.locale] || COPY['zh-CN']; },
    propertyRows() { return this.page.ownerDashboard?.properties || []; },
    periodLabel() { return `${this.period.year}-${String(this.period.month).padStart(2, '0')}`; },
    totalIncome() {
      if (this.propertyRows.length) return this.propertyRows.reduce((sum, property) => sum + Number(property.monthlyIncome || 0), 0);
      return Number(this.rent.summary?.monthlyAmountPaid || 0);
    },
    totalExpense() {
      if (this.propertyRows.length) return this.propertyRows.reduce((sum, property) => sum + Number(property.monthlyExpense || 0), 0);
      return Number(this.expenses.summary?.monthlyExpense || 0);
    },
    cumulativeIncome() { return Number(this.expenses.summary?.totalIncome || 0); },
    cumulativeExpense() { return Number(this.expenses.summary?.totalExpense || 0); },
    reserveBalance() { return Number(this.reserve.summary?.totalBalance ?? this.page.ownerDashboard?.summary?.reserveBalance ?? 0); },
    summaryCards() { return [
      { key: 'income', icon: ArrowUpRight, label: this.copy.totalIncome, value: this.totalIncome, note: this.copy.confirmedRent, tone: 'income' },
      { key: 'expense', icon: ArrowDownLeft, label: this.copy.totalExpense, value: this.totalExpense, note: this.copy.confirmedExpense, tone: 'expense' },
      { key: 'net', icon: CircleDollarSign, label: this.copy.netBalance, value: this.totalIncome - this.totalExpense, note: this.copy.incomeMinusExpense, tone: 'net' },
      { key: 'reserve', icon: WalletCards, label: this.copy.reserveBalance, value: this.reserveBalance, note: this.copy.availableReserve, tone: 'reserve' }
    ]; },
    moduleCards() { return [
      { id: 'rentIncome', icon: Coins, tone: 'income', title: this.copy.rentModule, detail: `${this.copy.received} RM ${this.money(this.rent.summary?.monthlyAmountPaid)} · ${this.copy.unpaid} RM ${this.money(this.rent.summary?.monthlyUnpaidAmount)}`, value: `${this.rent.records?.length || 0}` },
      { id: 'ownerExpenses', icon: Wrench, tone: 'expense', title: this.copy.expenseModule, detail: this.copy.expenseDetail, value: `RM ${this.money(this.totalExpense)}` },
      { id: 'ownerReserve', icon: ShieldCheck, tone: 'reserve', title: this.copy.reserveModule, detail: this.copy.reserveDetail, value: `RM ${this.money(this.reserveBalance)}` }
    ]; },
    cashflow() {
      const incomeRows = (this.rent.records || []).filter(row => Number(row.amountPaid || 0) > 0).map(row => ({ key: `rent-${row.invoiceId}`, direction: 'income', date: row.receivedDate || row.dueDate, project: row.projectName, unit: row.unitNo, description: `${row.tenantName || '—'} · ${this.copy.rentFlow}`, type: this.copy.income, status: this.statusLabel(row.confirmationStatus || row.status), amount: Number(row.amountPaid || 0) }));
      const expenseRows = (this.expenses.expenses || []).filter(row => Number(row.amount || 0) > 0).map(row => ({ key: `expense-${row.id}`, direction: 'expense', date: row.occurredOn, project: row.projectName, unit: row.unitNo, description: row.description || this.copy.expenseFlow, type: this.copy.expense, status: this.statusLabel(row.confirmationStatus || row.paymentStatus), amount: Number(row.amount || 0) }));
      return [...incomeRows, ...expenseRows].sort((a, b) => String(b.date || '').localeCompare(String(a.date || '')));
    },
    filteredCashflow() { return this.flowFilter === 'all' ? this.cashflow : this.cashflow.filter(row => row.direction === this.flowFilter); },
    visibleCashflow() { return this.filteredCashflow.slice(0, 10); },
    flowFilters() { return [{ value: 'all', label: this.copy.all }, { value: 'income', label: this.copy.incomeOnly }, { value: 'expense', label: this.copy.expenseOnly }]; },
    attentionItems() { return [
      { id: 'rentIncome', icon: Coins, tone: 'gold', title: this.copy.pendingRentTitle, detail: `RM ${this.money(this.rent.summary?.monthlyUnpaidAmount)}` },
      { id: 'ownerExpenses', icon: Wrench, tone: 'blue', title: this.copy.pendingMaintenanceTitle, detail: `${this.expenses.summary?.pendingMaintenanceCount || 0} ${this.copy.pendingMaintenance}` },
      { id: 'ownerReserve', icon: ShieldCheck, tone: 'red', title: this.copy.lowReserveTitle, detail: `${this.reserve.summary?.lowBalanceCount || 0} ${this.copy.lowAccounts}` }
    ]; }
  },
  mounted() { this.loadData(); },
  methods: {
    async loadData() {
      this.loading = true; this.errorMessage = ''; this.period = currentPeriod();
      const results = await Promise.allSettled([
        fetchOwnerRentIncome({ year: this.period.year, month: this.period.month }),
        fetchOwnerExpenses({ startDate: this.period.startDate, endDate: this.period.endDate }),
        fetchOwnerReserve({ startDate: this.period.startDate, endDate: this.period.endDate })
      ]);
      if (results[0].status === 'fulfilled') this.rent = results[0].value;
      if (results[1].status === 'fulfilled') this.expenses = results[1].value;
      if (results[2].status === 'fulfilled') this.reserve = results[2].value;
      const failures = results.filter(result => result.status === 'rejected').length;
      this.errorMessage = failures === 3 ? this.copy.loadFailed : failures ? this.copy.loadPartial : '';
      if (typeof this.page.loadOwnerDashboard === 'function') await this.page.loadOwnerDashboard();
      this.loading = false;
    },
    money(value) { return Number(value || 0).toLocaleString('en-MY', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); },
    statusLabel(value) { return { confirmed: '已确认', pending: '待确认', rejected: '已拒绝', paid: '已付款', partial: '部分付款', unpaid: '未付款', completed: '已完成', processing: '处理中' }[value] || value || '—'; }
  }
};
</script>
