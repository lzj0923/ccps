<template>
  <section class="admin-dashboard-workspace">
    <div v-if="errorMessage" class="admin-owner-state error" role="alert">
      <strong>{{ $t('legacy.t_4315d9c136f8') }}</strong>
      <span>{{ $lt(errorMessage) }}</span>
      <button type="button" @click="loadData">{{ $t('legacy.t_0a12f2ebe04f') }}</button>
    </div>

    <template v-else>
      <header class="workbench-heading">
        <h1>{{ $t('ui.todayWorkbench') }}</h1>
      </header>

      <div class="workbench-top-grid">
        <section class="workbench-panel attention-panel" aria-labelledby="dashboard-attention-title">
          <header class="workbench-panel-head">
            <div><h2 id="dashboard-attention-title">{{ $t('ui.needsProcessing') }}</h2><p>{{ $t('ui.dashboardAttentionHint') }}</p></div>
            <span>{{ $t('ui.dashboardAttentionCount', { count: attentionItems.length }) }}</span>
          </header>

          <div v-if="loading && !attentionLoaded" class="dashboard-loading-list" aria-live="polite"><i v-for="index in 4" :key="index"></i></div>
          <div v-else-if="attentionItems.length" class="dashboard-attention-list">
            <article v-for="item in attentionItems" :key="item.key" :class="`is-${item.tone}`">
              <span class="dashboard-attention-icon" aria-hidden="true">
                <ReceiptText v-if="item.hasOverdueRent || item.hasPendingPayment" :size="20" />
                <WalletCards v-else-if="item.hasNegativeReserve" :size="20" />
                <Building2 v-else :size="20" />
              </span>
              <div class="dashboard-attention-property"><strong>{{ $lt(item.title) }}</strong><p>{{ $lt(item.hint) }}</p></div>
              <b>{{ item.value }}</b>
              <button type="button" :aria-label="$t('ui.openAttentionProperty')" @click="openAttentionProperty(item)"><ArrowRight :size="18" aria-hidden="true" /></button>
            </article>
          </div>
          <div v-else class="dashboard-attention-empty">
            <CircleCheck :size="24" aria-hidden="true" />
            <div><strong>{{ $t('ui.dashboardNoAttentionTitle') }}</strong><p>{{ $t('ui.dashboardNoAttentionHint') }}</p></div>
          </div>
        </section>

        <section class="workbench-panel summary-panel" aria-labelledby="operating-summary-title">
          <header class="workbench-panel-head"><div><h2 id="operating-summary-title">{{ $t('ui.operatingSummary') }}</h2><p>{{ $t('ui.operatingSummaryHint') }}</p></div></header>
          <div class="operating-summary-grid">
            <article v-for="card in summaryCards" :key="card.label">
              <component :is="card.icon" :size="28" aria-hidden="true" />
              <span>{{ $lt(card.label) }}</span><strong>{{ card.value }}</strong><small>{{ $lt(card.hint) }}</small>
            </article>
          </div>
        </section>
      </div>

      <div class="workbench-bottom-grid">
        <section class="workbench-panel rent-chart-panel" aria-labelledby="rent-chart-title">
          <header class="workbench-panel-head">
            <div><h2 id="rent-chart-title">{{ $t('ui.rentCollectionOverview') }}</h2><p>{{ $t('ui.rentCollectionOverviewHint', { year: chartYear }) }}</p></div>
            <span class="chart-year">{{ chartYear }}</span>
          </header>
          <div class="chart-legend"><span><i class="is-due"></i>{{ $t('ui.receivableRent') }}</span><span><i class="is-paid"></i>{{ $t('ui.receivedRent') }}</span></div>
          <div v-if="chartHasData" class="rent-chart-wrap">
            <svg viewBox="0 0 720 220" role="img" :aria-label="$t('ui.rentCollectionOverview')">
              <title>{{ $t('ui.rentCollectionOverview') }}</title>
              <line v-for="y in [32, 68, 104, 140, 176]" :key="y" x1="24" x2="704" :y1="y" :y2="y" class="chart-grid-line" />
              <rect v-for="month in rentChartMonths" :key="`bar-${month.key}`" :x="month.x" :y="month.paidY" width="24" :height="month.paidHeight" rx="2" class="chart-bar" />
              <polyline :points="rentChartPoints" class="chart-line" />
              <circle v-for="month in rentChartMonths" :key="`point-${month.key}`" :cx="month.x + 12" :cy="month.dueY" r="4" class="chart-point" />
              <text v-for="month in rentChartMonths" :key="`label-${month.key}`" :x="month.x + 12" y="208" text-anchor="middle">{{ $lt(month.label) }}</text>
            </svg>
          </div>
          <div v-else class="chart-empty">{{ $t('ui.noRentChartData') }}</div>
        </section>

        <section class="workbench-panel activity-panel" aria-labelledby="property-activity-title">
          <header class="workbench-panel-head">
            <div><h2 id="property-activity-title">{{ $t('ui.propertyActivity') }}</h2><p>{{ $t('ui.propertyActivityHint') }}</p></div>
            <button type="button" @click="openModule('adminFinance')">{{ $t('ui.viewMore') }}<ArrowRight :size="15" aria-hidden="true" /></button>
          </header>
          <div v-if="recentActivities.length" class="activity-list">
            <button v-for="activity in recentActivities" :key="activity.key" type="button" @click="openActivity(activity)">
              <span aria-hidden="true"><ReceiptText :size="20" /></span>
              <div><strong>{{ $lt(activity.title) }}</strong><p>{{ $lt(activity.hint) }}</p></div>
              <b>{{ activity.value }}</b>
            </button>
          </div>
          <div v-else class="activity-empty">{{ $t('ui.noPropertyActivity') }}</div>
        </section>
      </div>
    </template>
  </section>
</template>

<script>
import { ArrowRight, Building2, ChartColumnIncreasing, CircleCheck, ClipboardCheck, Gauge, ReceiptText, WalletCards } from '@lucide/vue';
import { fetchAdminDashboard, fetchAdminProperties, fetchAdminRentCollections, fetchAdminRentFinanceReviews, fetchAdminReserveOverview } from '../services/propertyApi';
import { navigate } from '../router';

export default {
  components: { ArrowRight, Building2, ChartColumnIncreasing, CircleCheck, ClipboardCheck, Gauge, ReceiptText, WalletCards },
  inject: ['page'],
  data() {
    return {
      dashboard: { summary: {}, regions: [] }, properties: [], reserveAccounts: [], rentFinanceRows: [], rentCollectionRows: [],
      rentFinanceSummary: {}, rentCollectionSummary: {}, loading: false, attentionLoaded: false, errorMessage: ''
    };
  },
  computed: {
    chartYear() { return Number(String(this.page.dateEnd || '').slice(0, 4)) || new Date().getFullYear(); },
    attentionItems() {
      const itemsByProperty = new Map();
      const propertyKey = row => row?.unitId ? `unit-${row.unitId}` : `property-${row?.projectName || ''}-${row?.unitNo || ''}`;
      const matchingProperty = row => this.properties.find(property => String(property.unitId) === String(row?.unitId)
        || (property.projectName === row?.projectName && property.unitNo === row?.unitNo));
      const ensureItem = row => {
        const key = propertyKey(row);
        if (!itemsByProperty.has(key)) itemsByProperty.set(key, {
          key, unitId: row.unitId, projectName: row.projectName || this.$t('ui.unsetProject'), unitNo: row.unitNo || '—',
          city: row.city || '', building: row.building || '', floorNo: row.floorNo || '', ownerNames: [],
          hasNegativeReserve: false, hasOverdueRent: false, hasPendingPayment: false,
          reserveBalance: 0, overdueAmount: 0, pendingPaymentAmount: 0
        });
        const item = itemsByProperty.get(key);
        const ownerName = row.ownerName || '';
        if (ownerName && !item.ownerNames.includes(ownerName)) item.ownerNames.push(ownerName);
        if (!item.city && row.city) item.city = row.city;
        if (!item.building && row.building) item.building = row.building;
        if (!item.floorNo && row.floorNo) item.floorNo = row.floorNo;
        return item;
      };

      for (const account of this.reserveAccounts.filter(row => Number(row.currentBalance || 0) < 0)) {
        const property = matchingProperty(account);
        const item = ensureItem({ ...account, ...property, ownerName: account.ownerName || property?.ownerName });
        item.hasNegativeReserve = true;
        item.reserveBalance += Number(account.currentBalance || 0);
      }
      for (const rent of this.rentCollectionRows.filter(row => Number(row.overdueDays || 0) > 0)) {
        const property = matchingProperty(rent);
        const item = ensureItem({ ...rent, ...property, ownerName: property?.ownerName });
        item.hasOverdueRent = true;
        item.overdueAmount += Number(rent.outstandingAmount || 0);
      }
      for (const payment of this.rentFinanceRows.filter(row => row.confirmationStatus === 'pending')) {
        const property = matchingProperty(payment);
        const item = ensureItem({ ...payment, ...property, ownerName: property?.ownerName });
        item.hasPendingPayment = true;
        item.pendingPaymentAmount += Number(payment.amount || 0);
      }

      return [...itemsByProperty.values()].map(item => {
        const location = [item.city, item.building, item.floorNo].filter(Boolean).join(' · ');
        const issues = [
          item.hasPendingPayment ? this.$t('ui.pendingPaymentReview') : '', item.hasOverdueRent ? this.$t('ui.overdueRent') : '',
          item.hasNegativeReserve ? this.$t('ui.reserveAttentionLabel') : ''
        ].filter(Boolean).join(' · ');
        return {
          ...item,
          tone: item.hasOverdueRent || item.hasNegativeReserve ? 'urgent' : 'review',
          title: `${item.projectName} · ${item.unitNo}`,
          hint: [issues, item.ownerNames.join('、') || this.$t('ui.ownerUnset'), location].filter(Boolean).join(' · '),
          value: item.hasOverdueRent ? `RM ${this.money(item.overdueAmount)}`
            : item.hasNegativeReserve ? `-RM ${this.money(Math.abs(item.reserveBalance))}`
              : item.hasPendingPayment ? `RM ${this.money(item.pendingPaymentAmount)}` : '—'
        };
      }).sort((a, b) => Number(b.hasOverdueRent) - Number(a.hasOverdueRent) || Number(b.hasNegativeReserve) - Number(a.hasNegativeReserve)
        || a.projectName.localeCompare(b.projectName) || a.unitNo.localeCompare(b.unitNo));
    },
    summaryCards() {
      const summary = this.dashboard.summary || {};
      return [
        { icon: Building2, label: this.$t('ui.managedProperties'), value: this.$t('ui.properties', { count: Number(summary.unitCount || 0) }), hint: this.$t('ui.liveDatabaseStatistics') },
        { icon: Gauge, label: this.$t('ui.overallOccupancy'), value: `${Number(summary.occupancyRate || 0).toFixed(1)}%`, hint: this.$t('ui.activeLeaseCalculationHint') },
        { icon: ChartColumnIncreasing, label: this.$t('ui.monthReceivedRent'), value: `RM ${this.money(this.rentFinanceSummary.monthAmount)}`, hint: this.$t('ui.confirmedRentPayments') },
        { icon: ClipboardCheck, label: this.$t('ui.pendingProperties'), value: this.$t('ui.properties', { count: this.attentionItems.length }), hint: this.$t('ui.propertyLevelSummary') }
      ];
    },
    rentChartMonths() {
      const invoiceMap = new Map();
      const addInvoice = row => {
        if (!row?.invoiceId || !row.billingMonth) return;
        const current = invoiceMap.get(row.invoiceId) || { billingMonth: row.billingMonth, due: 0, paid: 0 };
        current.due = Math.max(current.due, Number(row.invoiceAmount ?? row.amountDue ?? 0));
        current.paid = Math.max(current.paid, Number(row.invoicePaid ?? row.amountPaid ?? 0));
        invoiceMap.set(row.invoiceId, current);
      };
      this.rentFinanceRows.forEach(addInvoice);
      this.rentCollectionRows.forEach(addInvoice);
      const totals = Array.from({ length: 12 }, (_, index) => ({ key: `${this.chartYear}-${String(index + 1).padStart(2, '0')}`, label: `${index + 1}${this.$t('ui.monthSuffix')}`, due: 0, paid: 0 }));
      for (const invoice of invoiceMap.values()) {
        const key = String(invoice.billingMonth).slice(0, 7);
        const month = totals.find(item => item.key === key);
        if (month) { month.due += invoice.due; month.paid += invoice.paid; }
      }
      const max = Math.max(1, ...totals.flatMap(item => [item.due, item.paid]));
      return totals.map((item, index) => {
        const paidHeight = item.paid / max * 144;
        return { ...item, x: 24 + index * 58, paidHeight, paidY: 176 - paidHeight, dueY: 176 - item.due / max * 144 };
      });
    },
    rentChartPoints() { return this.rentChartMonths.map(month => `${month.x + 12},${month.dueY}`).join(' '); },
    chartHasData() { return this.rentChartMonths.some(month => month.due > 0 || month.paid > 0); },
    recentActivities() {
      return [...this.rentFinanceRows].sort((a, b) => String(b.transactionDate || b.submittedAt || '').localeCompare(String(a.transactionDate || a.submittedAt || '')))
        .slice(0, 5).map(row => {
          const property = this.properties.find(item => item.projectName === row.projectName && item.unitNo === row.unitNo);
          const statusKey = row.confirmationStatus === 'confirmed' ? 'rentPaymentConfirmed' : row.confirmationStatus === 'rejected' ? 'rentPaymentRejected' : 'pendingPaymentReview';
          return { key: `finance-${row.id}`, unitId: property?.unitId, title: this.$t(`ui.${statusKey}`), hint: [row.projectName, row.unitNo, row.transactionDate].filter(Boolean).join(' · '), value: `RM ${this.money(row.amount)}` };
        });
    }
  },
  watch: {
    'page.dateStart'() { this.loadData(); },
    'page.dateEnd'() { this.loadData(); }
  },
  mounted() { this.loadData(); },
  methods: {
    async loadData() {
      this.loading = true;
      this.errorMessage = '';
      try {
        const range = { startDate: `${this.chartYear}-01-01`, endDate: `${this.chartYear}-12-31` };
        const [dashboard, properties, reserve, finance, collections] = await Promise.all([
          fetchAdminDashboard({ startDate: this.page.dateStart, endDate: this.page.dateEnd }),
          this.fetchAllProperties(),
          fetchAdminReserveOverview(),
          this.fetchPagedBundle(fetchAdminRentFinanceReviews, range),
          this.fetchPagedBundle(fetchAdminRentCollections, range)
        ]);
        this.dashboard = dashboard;
        this.properties = properties;
        this.reserveAccounts = reserve.accounts || [];
        this.rentFinanceRows = finance.rows;
        this.rentFinanceSummary = finance.summary;
        this.rentCollectionRows = collections.rows;
        this.rentCollectionSummary = collections.summary;
        this.page.adminOwnerProjects = [...new Set(properties.map(property => property.projectName).filter(Boolean))].sort((a, b) => a.localeCompare(b));
        this.attentionLoaded = true;
      }
      catch (error) { this.errorMessage = error.message || 'API request failed'; }
      finally { this.loading = false; }
    },
    async fetchAllProperties() {
      const response = await this.fetchPagedBundle(fetchAdminProperties);
      return response.rows.map(item => ({
        ...item.property, ownerId: item.ownerId, ownerName: item.ownerName, rentalStatus: item.rentalStatus
      }));
    },
    async fetchPagedBundle(fetcher, filters = {}) {
      const first = await fetcher({ ...filters, page: 1, pageSize: 100 });
      const totalPages = Number(first.page?.totalPages || 1);
      const remaining = totalPages > 1 ? await Promise.all(Array.from({ length: totalPages - 1 }, (_, index) => fetcher({ ...filters, page: index + 2, pageSize: 100 }))) : [];
      return { rows: [first, ...remaining].flatMap(response => response.rows || []), summary: first.summary || {} };
    },
    openAttentionProperty(item) {
      if (item.unitId) navigate(`/admin/properties/${item.unitId}`);
      else { this.page.globalSearch = `${item.projectName} ${item.unitNo}`.trim(); this.openModule('adminProperties'); }
    },
    openActivity(activity) { if (activity.unitId) navigate(`/admin/properties/${activity.unitId}`); else this.openModule('adminFinance'); },
    openModule(moduleId) { this.page.selectModule(moduleId); },
    money(value) { return Number(value || 0).toLocaleString('en-MY', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
  }
};
</script>

<style scoped>
/* Hallmark · macrostructure: four-zone operations workbench · tone: restrained property operations · anchor hue: teal
 * theme: studied-DNA (source: 2026-08-31 CCPS UI concept image) · pre-emit critique: P5 H5 E5 S5 R5 V5
 */
:global(html),:global(body){overflow-x:clip}
.admin-dashboard-workspace{display:grid;gap:var(--space-native-md);min-width:0;margin:0 28px 32px;color:var(--admin-ink)}
.workbench-heading{display:flex;align-items:center;justify-content:space-between;gap:16px;padding:24px 8px 8px}
.workbench-heading h1{min-width:0;overflow-wrap:anywhere;margin:0;font-size:32px;line-height:1.1;letter-spacing:-.03em}
.workbench-panel-head button{display:inline-flex;align-items:center;justify-content:center;gap:8px;min-height:40px;padding:0 12px;border:1px solid var(--admin-line);border-radius:8px;background:var(--color-native-paper,var(--admin-soft));color:var(--admin-accent-dark);font-weight:800;white-space:nowrap;cursor:pointer}
.workbench-top-grid,.workbench-bottom-grid{display:grid;grid-template-columns:minmax(0,1.6fr) minmax(20rem,1fr);gap:16px;min-width:0}
.workbench-panel{min-width:0;overflow:hidden;border:1px solid var(--admin-line);border-radius:10px;background:var(--color-native-paper,var(--admin-soft));box-shadow:0 4px 16px color-mix(in srgb,var(--admin-ink) 5%,transparent)}
.workbench-panel-head{display:flex;min-height:72px;align-items:center;justify-content:space-between;gap:16px;padding:12px 20px;border-bottom:1px solid var(--admin-line)}.workbench-panel-head>div{min-width:0}.workbench-panel-head h2{margin:0;font-size:18px;line-height:1.25}.workbench-panel-head p{margin:4px 0 0;color:var(--admin-muted);font-size:11px}.workbench-panel-head>span{flex:0 0 auto;padding:4px 8px;border:1px solid var(--admin-line);border-radius:999px;color:var(--admin-accent-dark);font-size:11px;font-weight:800}
.attention-panel,.summary-panel{min-height:320px}
.dashboard-attention-list{display:grid;max-height:248px;overflow:auto;padding:0 16px}.dashboard-attention-list article{display:grid;grid-template-columns:40px minmax(0,1fr) auto 40px;align-items:center;gap:12px;min-height:64px;border-bottom:1px solid var(--admin-line)}.dashboard-attention-list article:last-child{border-bottom:0}
.dashboard-attention-icon{display:grid;width:40px;height:40px;place-items:center;border-radius:8px;background:color-mix(in srgb,var(--admin-accent) 10%,var(--admin-soft));color:var(--admin-accent-dark)}.dashboard-attention-list article.is-urgent .dashboard-attention-icon{background:color-mix(in srgb,var(--color-native-warm,var(--admin-gold)) 22%,var(--admin-soft));color:var(--admin-ink)}
.dashboard-attention-property{min-width:0}.dashboard-attention-property strong{display:block;overflow:hidden;color:var(--admin-ink);font-size:13px;text-overflow:ellipsis;white-space:nowrap}.dashboard-attention-property p{overflow:hidden;margin:4px 0 0;color:var(--admin-muted);font-size:11px;text-overflow:ellipsis;white-space:nowrap}.dashboard-attention-list article>b{color:var(--admin-ink);font-size:12px;white-space:nowrap}.dashboard-attention-list article>button{display:grid;width:40px;height:40px;place-items:center;border:0;border-radius:8px;background:transparent;color:var(--admin-ink);cursor:pointer}
.dashboard-attention-empty{display:flex;align-items:center;justify-content:center;gap:12px;min-height:232px;padding:24px;color:var(--admin-accent)}.dashboard-attention-empty strong{color:var(--admin-ink);font-size:13px}.dashboard-attention-empty p{margin:4px 0 0;color:var(--admin-muted);font-size:11px}
.dashboard-loading-list{display:grid;gap:8px;padding:12px 16px}.dashboard-loading-list i{height:48px;border-radius:8px;background:linear-gradient(90deg,var(--admin-soft),color-mix(in srgb,var(--admin-line) 55%,var(--admin-soft)),var(--admin-soft));background-size:200% 100%;animation:dashboard-shimmer 1.2s linear infinite}
.operating-summary-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));min-height:248px;padding:32px 16px}.operating-summary-grid article{display:flex;min-width:0;align-items:center;flex-direction:column;gap:8px;padding:0 12px;border-right:1px solid var(--admin-line);text-align:center}.operating-summary-grid article:last-child{border-right:0}.operating-summary-grid svg{color:var(--admin-accent)}.operating-summary-grid span{color:var(--admin-ink);font-size:13px}.operating-summary-grid strong{margin-top:4px;color:var(--admin-ink);font-size:22px;white-space:nowrap}.operating-summary-grid small{color:var(--admin-muted);font-size:10px;line-height:1.35}
.rent-chart-panel,.activity-panel{min-height:368px}.chart-legend{display:flex;align-items:center;gap:20px;padding:12px 20px 0;color:var(--admin-muted);font-size:11px}.chart-legend span{display:flex;align-items:center;gap:8px}.chart-legend i{display:block;width:20px;height:3px;background:var(--admin-accent)}.chart-legend i.is-paid{height:10px;background:color-mix(in srgb,var(--admin-accent) 62%,var(--admin-soft))}.chart-year{border-radius:6px!important}
.rent-chart-wrap{padding:0 16px 16px}.rent-chart-wrap svg{display:block;width:100%;height:248px;overflow:visible}.chart-grid-line{stroke:var(--admin-line);stroke-width:1}.chart-bar{fill:color-mix(in srgb,var(--admin-accent) 62%,var(--admin-soft))}.chart-line{fill:none;stroke:var(--admin-accent-dark);stroke-width:2.5;stroke-linecap:round;stroke-linejoin:round}.chart-point{fill:var(--color-native-paper,var(--admin-soft));stroke:var(--admin-accent-dark);stroke-width:2}.rent-chart-wrap text{fill:var(--admin-muted);font-size:10px}.chart-empty,.activity-empty{display:grid;min-height:276px;place-items:center;color:var(--admin-muted);font-size:12px}
.activity-list{display:grid;padding:0 16px}.activity-list>button{display:grid;grid-template-columns:40px minmax(0,1fr) auto;align-items:center;gap:12px;min-height:60px;padding:0;border:0;border-bottom:1px solid var(--admin-line);background:transparent;color:var(--admin-ink);text-align:left;cursor:pointer}.activity-list>button:last-child{border-bottom:0}.activity-list>button>span{display:grid;width:40px;height:40px;place-items:center;border-radius:8px;background:color-mix(in srgb,var(--admin-accent) 10%,var(--admin-soft));color:var(--admin-accent-dark)}.activity-list>button div{min-width:0}.activity-list strong{display:block;overflow:hidden;font-size:12px;text-overflow:ellipsis;white-space:nowrap}.activity-list p{overflow:hidden;margin:4px 0 0;color:var(--admin-muted);font-size:10px;text-overflow:ellipsis;white-space:nowrap}.activity-list b{font-size:11px;white-space:nowrap}
.workbench-panel-head button:hover,.dashboard-attention-list article>button:hover,.activity-list>button:hover{border-color:var(--admin-accent);background:color-mix(in srgb,var(--admin-accent) 8%,var(--admin-soft));color:var(--admin-accent-dark)}
.workbench-panel-head button:focus-visible,.dashboard-attention-list article>button:focus-visible,.activity-list>button:focus-visible{outline:2px solid var(--admin-accent);outline-offset:2px}.workbench-panel-head button:active,.dashboard-attention-list article>button:active,.activity-list>button:active{background:color-mix(in srgb,var(--admin-accent) 14%,var(--admin-soft))}.workbench-panel-head button:disabled{cursor:not-allowed;background:var(--admin-soft);color:var(--admin-muted);opacity:.55}
@keyframes dashboard-shimmer{to{background-position:-200% 0}}
@media(max-width:1180px){.workbench-top-grid,.workbench-bottom-grid{grid-template-columns:minmax(0,1fr)}.operating-summary-grid{min-height:208px}.rent-chart-panel,.activity-panel{min-height:0}}
@media(max-width:760px){.admin-dashboard-workspace{margin-inline:16px}.workbench-heading{align-items:flex-start;padding-inline:0}.workbench-heading h1{font-size:28px}.workbench-panel-head{padding-inline:16px}.operating-summary-grid{grid-template-columns:repeat(2,minmax(0,1fr));gap:20px 0;padding:24px 12px}.operating-summary-grid article:nth-child(2){border-right:0}.dashboard-attention-list article{grid-template-columns:40px minmax(0,1fr) 40px}.dashboard-attention-list article>b{grid-column:2}.dashboard-attention-list article>button{grid-column:3;grid-row:1 / span 2}.activity-list>button{grid-template-columns:40px minmax(0,1fr)}.activity-list>button>b{grid-column:2}}
@media(max-width:420px){.operating-summary-grid{grid-template-columns:minmax(0,1fr)}.operating-summary-grid article{padding:16px 0;border-right:0;border-bottom:1px solid var(--admin-line)}.operating-summary-grid article:last-child{border-bottom:0}.workbench-panel-head{align-items:flex-start;flex-direction:column}.workbench-panel-head>span{align-self:flex-start}}
@media(prefers-reduced-motion:reduce){.dashboard-loading-list i{animation:none}}
</style>
