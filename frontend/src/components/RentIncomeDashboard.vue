<template>
  <section class="rent-income-page">
    <div class="rent-kpi-grid">
      <article v-for="card in summaryCards" :key="card.label" class="rent-kpi-card">
        <div class="rent-kpi-icon" v-html="icons[card.icon]"></div>
        <div class="rent-kpi-copy">
          <span>{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
          <small :class="card.trend === 'down' ? 'negative' : 'positive'">
            {{ card.comparison }} <b>{{ card.delta }}</b>
            <i>{{ card.trend === 'down' ? '↓' : '↗' }}</i>
          </small>
        </div>
      </article>
    </div>

    <div class="rent-income-layout">
      <div class="rent-main-column">
        <form class="rent-filter-card" @submit.prevent="applyFilters">
          <label>
            <span>房產</span>
            <select v-model="draftFilters.projectId"><option v-for="property in properties" :key="property.id || 'all'" :value="property.id">{{ property.name }}</option></select>
          </label>
          <label>
            <span>月份</span>
            <select v-model.number="draftFilters.month"><option v-for="month in months" :key="month.value" :value="month.value">{{ month.label }}</option></select>
          </label>
          <label>
            <span>年份</span>
            <select v-model.number="draftFilters.year"><option v-for="year in years" :key="year" :value="year">{{ year }}年</option></select>
          </label>
          <label>
            <span>收款狀態</span>
            <select v-model="draftFilters.status"><option v-for="status in statuses" :key="status.value || 'all'" :value="status.value">{{ status.label }}</option></select>
          </label>
          <div class="rent-filter-actions">
            <button class="rent-query-button" type="submit" :disabled="loading">{{ loading ? '查詢中' : '查詢' }}</button>
            <button class="rent-reset-button" type="button" :disabled="loading" @click="resetFilters">重置</button>
          </div>
          <button class="rent-export-button" type="button" :disabled="loading || !filteredRows.length" @click="exportRows">
            <span v-html="icons.download"></span> 匯出報表
          </button>
        </form>

        <section class="rent-table-card">
          <header class="rent-card-heading">
            <div><h2>租金收入記錄</h2><span>共 {{ filteredRows.length }} 筆記錄</span></div>
          </header>
          <div class="rent-table-wrap">
            <table class="rent-table">
              <thead>
                <tr><th>房產</th><th>租客</th><th>應收日期</th><th>應收租金 (RM)</th><th>已收金額 (RM)</th><th>未收金額 (RM)</th><th>到帳日期</th><th>狀態</th><th>財務確認</th></tr>
              </thead>
              <tbody>
                <tr v-for="row in pagedRows" :key="row.invoiceId">
                  <td>
                    <div class="rent-property-cell">
                      <i :style="{ backgroundPosition: row.photoPosition }"></i>
                      <span><strong>{{ row.project }}</strong><small>{{ row.unit }}</small></span>
                    </div>
                  </td>
                  <td>{{ row.tenant }}</td>
                  <td>{{ row.dueDate }}</td>
                  <td>{{ row.rent }}</td>
                  <td :class="Number(row.paid.replaceAll(',', '')) ? 'amount-paid' : 'amount-empty'">{{ row.paid }}</td>
                  <td>{{ row.unpaid }}</td>
                  <td>{{ row.receivedDate }}</td>
                  <td><span class="rent-status" :class="statusClass(row.status)"><i></i>{{ row.status }}</span></td>
                  <td>
                    <span v-if="row.confirmed" class="rent-confirmed"><b>✓</b> 已確認</span>
                    <button v-else-if="row.canConfirm" class="rent-confirm-button" :disabled="confirmingInvoiceId === row.invoiceId" @click="confirmReceipt(row)">
                      {{ confirmingInvoiceId === row.invoiceId ? '確認中…' : '確認到賬' }}
                    </button>
                    <span v-else class="rent-pending">{{ row.confirmation }}</span>
                  </td>
                </tr>
                <tr v-if="loading"><td colspan="9" class="rent-empty">正在從資料庫載入租金記錄…</td></tr>
                <tr v-else-if="error"><td colspan="9" class="rent-empty">{{ error }}</td></tr>
                <tr v-else-if="!pagedRows.length"><td colspan="9" class="rent-empty">沒有符合條件的租金記錄</td></tr>
              </tbody>
            </table>
          </div>
          <footer class="rent-table-footer">
            <span>顯示第 {{ pageStart }} 至 {{ pageEnd }} 項，共 {{ filteredRows.length }} 項</span>
            <div class="rent-pagination">
              <button :disabled="currentPage === 1" @click="currentPage--">‹</button>
              <button v-for="number in totalPages" :key="number" :class="{ active: currentPage === number }" @click="currentPage = number">{{ number }}</button>
              <button :disabled="currentPage === totalPages" @click="currentPage++">›</button>
              <select v-model.number="pageSize"><option :value="8">8 / 頁</option><option :value="10">10 / 頁</option><option :value="20">20 / 頁</option></select>
            </div>
          </footer>
        </section>
      </div>

      <aside class="rent-side-column">
        <section class="rent-side-card rent-trend-card">
          <header><h2>年度租金收入趨勢 (RM)</h2><button @click="showToast('年度租金收入詳情')">查看詳情 <span>→</span></button></header>
          <div class="rent-trend-list">
            <div v-for="item in yearlyTrend" :key="item.year">
              <span>{{ item.year }}</span>
              <i><b :class="{ current: item.current }" :style="{ width: `${item.percent}%` }"></b></i>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </section>

        <section class="rent-side-card rent-recent-card">
          <header><h2>最近到帳記錄</h2><button @click="showToast('已顯示全部到帳記錄')">查看全部 <span>→</span></button></header>
          <div class="rent-recent-list">
            <article v-for="item in recentReceipts" :key="`${item.unit}-${item.date}`">
              <i>✓</i>
              <div><strong>{{ item.project }} · {{ item.unit }}</strong><small>{{ item.tenant }} · {{ item.date }}</small></div>
              <b>RM {{ item.amount }}</b>
            </article>
            <div v-if="!loading && !recentReceipts.length" class="rent-empty">暫無到帳記錄</div>
          </div>
          <footer>所有時間均以馬來西亞時間 (GMT+8) 為準</footer>
        </section>
      </aside>
    </div>
  </section>
</template>

<script>
import pageBridge from '../pageBridge';
import { confirmOwnerRentReceipt, fetchOwnerRentIncome } from '../services/propertyApi';
import { downloadCsv } from '../utils/csvExporter';

const now = new Date();
const defaultFilters = () => ({ projectId: '', month: now.getMonth() + 1, year: now.getFullYear(), status: '' });
const emptySummary = () => ({
  monthlyAmountDue: 0,
  monthlyAmountPaid: 0,
  monthlyUnpaidAmount: 0,
  annualAmountPaid: 0,
  amountDueChangePercent: 0,
  amountPaidChangePercent: 0,
  unpaidChangePercent: 0,
  annualChangePercent: 0
});

const statusLabels = { paid: '已收款', partial: '部分收款', unpaid: '未收款', overdue: '已逾期' };
const confirmationLabels = { confirmed: '已確認', pending: '待確認', rejected: '已拒絕' };

export default {
  mixins: [pageBridge],
  data() {
    return {
      loading: false,
      confirmingInvoiceId: null,
      error: '',
      currentPage: 1,
      pageSize: 8,
      draftFilters: defaultFilters(),
      filters: defaultFilters(),
      properties: [{ id: '', name: '全部房產' }],
      months: Array.from({ length: 12 }, (_, index) => ({ value: index + 1, label: `${String(index + 1).padStart(2, '0')}月` })),
      years: [now.getFullYear()],
      statuses: [
        { value: '', label: '全部狀態' },
        { value: 'paid', label: '已收款' },
        { value: 'partial', label: '部分收款' },
        { value: 'unpaid', label: '未收款' },
        { value: 'overdue', label: '已逾期' }
      ],
      summary: emptySummary(),
      trendRows: [],
      receiptRows: [],
      rows: [],
      icons: {
        receipt: '<svg viewBox="0 0 24 24" fill="none"><path d="M6 3h12v18l-3-2-3 2-3-2-3 2V3Z"/><path d="M9 8h6M9 12h6M9 16h3"/></svg>',
        wallet: '<svg viewBox="0 0 24 24" fill="none"><path d="M4 7.5h15a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-13a2 2 0 0 1 2-2h12"/><path d="M16 12h5v4h-5a2 2 0 0 1 0-4Z"/></svg>',
        clock: '<svg viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="9"/><path d="M12 7v6l4 2"/></svg>',
        chart: '<svg viewBox="0 0 24 24" fill="none"><path d="M4 20V10M10 20V4M16 20v-7M22 20V7"/><path d="m4 7 5-4 6 5 6-5"/></svg>',
        download: '<svg viewBox="0 0 24 24" fill="none"><path d="M12 3v12m0 0 4-4m-4 4-4-4M4 17v3h16v-3"/></svg>'
      }
    };
  },
  computed: {
    summaryCards() {
      return [
        this.summaryCard('receipt', '本月應收租金', this.summary.monthlyAmountDue, '較上月', this.summary.amountDueChangePercent),
        this.summaryCard('wallet', '本月已收租金', this.summary.monthlyAmountPaid, '較上月', this.summary.amountPaidChangePercent),
        this.summaryCard('clock', '未收金額', this.summary.monthlyUnpaidAmount, '較上月', this.summary.unpaidChangePercent),
        this.summaryCard('chart', '年度累計租金收入', this.summary.annualAmountPaid, '較去年', this.summary.annualChangePercent)
      ];
    },
    yearlyTrend() {
      const maximum = Math.max(0, ...this.trendRows.map(item => Number(item.amountPaid || 0)));
      return this.trendRows.map(item => ({
        year: String(item.year),
        value: this.money(item.amountPaid),
        percent: maximum ? Math.max(4, Math.round(Number(item.amountPaid || 0) / maximum * 100)) : 0,
        current: Number(item.year) === Number(this.filters.year)
      }));
    },
    recentReceipts() {
      return this.receiptRows.map(item => ({
        project: item.projectName,
        unit: item.unitNo,
        tenant: item.tenantName,
        date: item.receivedDate || '-',
        amount: this.money(item.amount)
      }));
    },
    filteredRows() { return this.rows; },
    totalPages() { return Math.max(1, Math.ceil(this.filteredRows.length / this.pageSize)); },
    pagedRows() { return this.filteredRows.slice((this.currentPage - 1) * this.pageSize, this.currentPage * this.pageSize); },
    pageStart() { return this.filteredRows.length ? (this.currentPage - 1) * this.pageSize + 1 : 0; },
    pageEnd() { return Math.min(this.currentPage * this.pageSize, this.filteredRows.length); }
  },
  watch: {
    pageSize() { this.currentPage = 1; }
  },
  mounted() {
    this.loadRentIncome();
  },
  methods: {
    async loadRentIncome(showResult = false) {
      this.loading = true;
      this.error = '';
      try {
        const result = await fetchOwnerRentIncome(this.filters);
        this.summary = { ...emptySummary(), ...(result.summary || {}) };
        this.properties = [{ id: '', name: '全部房產' }, ...(result.properties || [])];
        this.years = result.availableYears?.length ? result.availableYears : [this.filters.year];
        this.trendRows = result.yearlyTrend || [];
        this.receiptRows = result.recentReceipts || [];
        this.rows = (result.records || []).map((row, index) => ({
          invoiceId: row.invoiceId,
          project: row.projectName,
          unit: row.unitNo,
          tenant: row.tenantName,
          dueDate: row.dueDate,
          rent: this.money(row.amountDue),
          paid: this.money(row.amountPaid),
          unpaid: this.money(row.unpaidAmount),
          receivedDate: row.receivedDate || '-',
          status: statusLabels[row.status] || row.status,
          confirmed: row.confirmationStatus === 'confirmed',
          canConfirm: row.confirmationStatus === 'pending',
          confirmation: confirmationLabels[row.confirmationStatus] || '-',
          photoPosition: `${8 + ((Number(row.invoiceId) || index) * 17) % 80}% center`
        }));
        if (showResult) this.showToast(`已查詢 ${this.rows.length} 筆租金記錄`);
      } catch (error) {
        this.summary = emptySummary();
        this.trendRows = [];
        this.receiptRows = [];
        this.rows = [];
        this.error = error.message || '無法載入租金收入資料';
        this.showToast(this.error);
      } finally {
        this.loading = false;
      }
    },
    async applyFilters() {
      this.filters = { ...this.draftFilters };
      this.currentPage = 1;
      await this.loadRentIncome(true);
    },
    async resetFilters() {
      this.draftFilters = defaultFilters();
      this.filters = { ...this.draftFilters };
      this.currentPage = 1;
      await this.loadRentIncome();
      this.showToast('租金篩選已重置');
    },
    async confirmReceipt(row) {
      if (this.confirmingInvoiceId !== null) return;
      const accepted = window.confirm(`確認 ${row.project} · ${row.unit} 已到賬 RM ${row.paid}？`);
      if (!accepted) return;
      this.confirmingInvoiceId = row.invoiceId;
      try {
        await confirmOwnerRentReceipt(row.invoiceId);
        await this.loadRentIncome();
        if (typeof this.page.loadOwnerDashboard === 'function') await this.page.loadOwnerDashboard();
        this.showToast(`${row.unit} 租金已確認到賬`);
      } catch (error) {
        this.showToast(error.message || '確認到賬失敗');
      } finally {
        this.confirmingInvoiceId = null;
      }
    },
    money(value) {
      return Number(value || 0).toLocaleString('en-MY', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    },
    summaryCard(icon, label, value, comparison, change) {
      const numericChange = Number(change || 0);
      return {
        icon,
        label,
        value: `RM ${this.money(value)}`,
        comparison,
        delta: `${numericChange > 0 ? '+' : ''}${numericChange.toFixed(2)}%`,
        trend: numericChange < 0 ? 'down' : 'up'
      };
    },
    statusClass(status) {
      return { '已收款': 'paid', '部分收款': 'partial', '未收款': 'unpaid', '已逾期': 'overdue' }[status] || 'unpaid';
    },
    exportRowsLegacy() {
      const header = ['房產', '單位', '租客', '應收日期', '應收租金', '已收金額', '未收金額', '到帳日期', '狀態', '財務確認'];
      const body = this.filteredRows.map(row => [row.project, row.unit, row.tenant, row.dueDate, row.rent, row.paid, row.unpaid, row.receivedDate, row.status, row.confirmation]);
      const csv = [header, ...body].map(row => row.map(value => `"${String(value).replaceAll('"', '""')}"`).join(',')).join('\n');
      const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' });
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = '租金收入報表.csv';
      link.click();
      URL.revokeObjectURL(link.href);
      this.showToast('租金收入報表已匯出');
    },
    exportRows() {
      const headers = ['房產', '單位', '租客', '應收日期', '應收租金', '已收金額', '未收金額', '到帳日期', '狀態', '財務確認'];
      const rows = this.filteredRows.map((row) => [row.project, row.unit, row.tenant, row.dueDate, row.rent, row.paid, row.unpaid, row.receivedDate, row.status, row.confirmation]);
      downloadCsv('租金收入明細.csv', headers, rows);
    }
  }
};
</script>
