<template>
  <section class="admin-dashboard-workspace">
    <div v-if="errorMessage" class="admin-owner-state error"><strong>{{ $t('legacy.t_4315d9c136f8') }}</strong><span>{{ errorMessage }}</span><button type="button" @click="loadData">{{ $t('legacy.t_0a12f2ebe04f') }}</button></div>
    <template v-else>
      <div class="dashboard-summary-grid">
        <article v-for="card in summaryCards" :key="card.label"><span>{{ card.label }}</span><strong>{{ card.value }}</strong><small>{{ card.hint }}</small></article>
      </div>
      <div class="panel region-overview-panel">
        <div class="panel-head"><div><h2>{{ $t('legacy.t_6d9da8dd3f55') }}</h2><span>{{ filteredRegions.length }} {{ $t('legacy.t_6ca61fe95d63') }}</span></div><button type="button" :disabled="loading" @click="loadData">{{ loading ? $t('legacy.t_c5a023301ff2') : $t('legacy.t_048f7692c8a0') }}</button></div>
        <div class="table-wrap"><table><thead><tr><th>{{ $t('legacy.t_17fc93c9cdbb') }}</th><th>{{ $t('legacy.t_0b47c54dd936') }}</th><th>{{ $t('legacy.t_a06483bc49df') }}</th><th>{{ $t('legacy.t_c9752a3620d7') }}</th><th>{{ $t('legacy.t_5f782027fcde') }}</th><th>{{ $t('legacy.t_2256c68d143f') }}</th><th>{{ $t('legacy.t_819c8bd23823') }}</th><th>{{ $t('legacy.t_1b8989e7c086') }}</th></tr></thead><tbody><tr v-for="row in filteredRegions" :key="row.regionName"><td><strong>{{ row.regionName }}</strong></td><td>{{ row.unitCount }} {{ $t('legacy.t_032231d845f8') }}</td><td>{{ row.occupiedCount }} {{ $t('legacy.t_032231d845f8') }}</td><td><div class="occupancy-cell"><b>{{ Number(row.occupancyRate || 0).toFixed(1) }}%</b><span><i :style="{ width: `${Math.min(100, Number(row.occupancyRate || 0))}%` }"></i></span></div></td><td>RM {{ money(row.totalRent) }}</td><td>RM {{ money(row.averageRent) }}</td><td>RM {{ money(row.tenantDeposit) }}</td><td :class="{ danger: Number(row.reserveBalance) < 0 }">RM {{ money(row.reserveBalance) }}</td></tr><tr v-if="!loading && !filteredRegions.length"><td colspan="8" class="admin-owner-empty">{{ $t('legacy.t_fde45c6b5bcf') }}</td></tr></tbody></table></div>
      </div>
    </template>
  </section>
</template>

<script>
import { fetchAdminDashboard } from '../services/propertyApi';
export default {
  inject: ['page'],
  data() { return { dashboard: { summary: {}, regions: [] }, loading: false, errorMessage: '' }; },
  computed: {
    filteredRegions() { const keyword = String(this.page.globalSearch || this.page.moduleSearch || '').trim().toLowerCase(); return (this.dashboard.regions || []).filter(row => !keyword || String(row.regionName).toLowerCase().includes(keyword)); },
    summaryCards() { const s = this.dashboard.summary || {}; return [
      { label: this.$t('ui.managedUnits'), value: `${Number(s.unitCount || 0)} ${this.$t('ui.unitSuffix')}`, hint: this.$t('ui.occupiedUnitsHint', { count: Number(s.occupiedCount || 0) }) },
      { label: this.$t('ui.overallOccupancy'), value: `${Number(s.occupancyRate || 0).toFixed(1)}%`, hint: this.$t('ui.activeLeaseCalculationHint') },
      { label: this.$t('ui.monthlyRentTotal'), value: `RM ${this.money(s.totalRent)}`, hint: this.$t('ui.averageRentHint', { amount: this.money(s.averageRent) }) },
      { label: this.$t('ui.tenantDepositBalance'), value: `RM ${this.money(s.tenantDeposit)}`, hint: this.$t('ui.depositLedgerBalanceHint') },
      { label: this.$t('ui.ownerReserveBalance'), value: `RM ${this.money(s.reserveBalance)}`, hint: this.$t('ui.negativeBalanceAllowedHint') }
    ]; }
  },
  mounted() { this.loadData(); },
  methods: {
    async loadData() { this.loading = true; this.errorMessage = ''; try { this.dashboard = await fetchAdminDashboard(); } catch (error) { this.errorMessage = error.message || 'API request failed'; } finally { this.loading = false; } },
    money(value) { return Number(value || 0).toLocaleString('en-MY', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
  }
};
</script>

<style scoped>
.admin-dashboard-workspace{display:grid;gap:18px;margin:0 28px 28px}.dashboard-summary-grid{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:12px}.dashboard-summary-grid article{display:grid;gap:8px;padding:20px;border:1px solid #d7e2ee;border-top:3px solid #07969c;border-radius:10px;background:#fff;box-shadow:0 7px 18px rgba(17,56,86,.06)}.dashboard-summary-grid span{color:#53677e;font-weight:600}.dashboard-summary-grid strong{color:#073a69;font-size:24px}.dashboard-summary-grid small{color:#8392a5}.region-overview-panel{min-width:0;overflow:hidden}.region-overview-panel .panel-head button{min-height:36px;padding:0 14px;border:1px solid #b9cadc;border-radius:7px;background:#fff;color:#164a78;font-weight:700}.region-overview-panel table{font-size:14px}.region-overview-panel th,.region-overview-panel td{padding:15px 14px}.occupancy-cell{display:grid;gap:6px;min-width:110px}.occupancy-cell>span{height:6px;border-radius:999px;background:#e7edf4;overflow:hidden}.occupancy-cell i{display:block;height:100%;border-radius:inherit;background:#07969c}.danger{color:#c62828!important;font-weight:700}@media(max-width:1280px){.dashboard-summary-grid{grid-template-columns:repeat(3,1fr)}}@media(max-width:760px){.admin-dashboard-workspace{margin-inline:14px}.dashboard-summary-grid{grid-template-columns:1fr}}
</style>
