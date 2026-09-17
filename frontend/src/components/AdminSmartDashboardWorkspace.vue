<template>
  <section ref="board" class="smart-board">
    <header class="board-head">
      <div class="board-title">
        <span class="eyebrow">{{ $t('ui.portfolioEyebrow') }}</span>
        <h1>{{ $t('legacy.t_8335962740cd') }}</h1>
        <p>{{ $t('legacy.t_0152a964e7bb') }}</p>
      </div>
      <div class="national-totals" :aria-label="$t('legacy.t_2a70025b7bf0')">
        <div><span>{{ $t('legacy.t_6b904c278cdd') }}</span><strong>{{ national.unitCount }}</strong><small>{{ $t('legacy.t_032231d845f8') }}</small></div>
        <div><span>{{ $t('legacy.t_4c21d564e994') }}</span><strong>{{ percent(national.occupancyRate) }}</strong></div>
        <div><span>{{ $t('legacy.t_1d736f0658fc') }}</span><strong>{{ $t('legacy.t_5e7b60c626a4') }} {{ compactMoney(national.totalRent) }}</strong></div>
        <div><span>{{ $t('legacy.t_3cc4199ab77a') }}</span><strong>{{ $t('legacy.t_5e7b60c626a4') }} {{ compactMoney(national.averageRent) }}</strong></div>
      </div>
      <div class="head-actions">
        <time>{{ formattedNow }}</time>
        <button type="button" :disabled="loading" :title="$t('legacy.t_048f7692c8a0')" @click="loadData"><RefreshCw :size="17" :class="{ spinning: loading }" />{{ $t('legacy.t_38108eaa1d32') }}</button>
        <button type="button" :title="$t('legacy.t_92ef6c7b2465')" @click="toggleFullscreen"><Maximize2 :size="17" />{{ $t('legacy.t_93c44f6b1b28') }}</button>
      </div>
    </header>

    <div v-if="errorMessage" class="board-error">
      <strong>{{ $t('legacy.t_d7115b03401c') }}</strong><span>{{ $lt(errorMessage) }}</span><button type="button" @click="loadData">{{ $t('legacy.t_0a12f2ebe04f') }}</button>
    </div>

    <div v-else class="board-grid">
      <MetricRanking
        class="panel-units"
        number="1"
        :title="$t('legacy.t_37351f04eaca')"
        :subtitle="$t('ui.regionalManagedUnitsHint')"
        :icon="Building2"
        :rows="rankedUnits"
        value-key="unitCount"
        :unit="$t('ui.unitSuffix')"
        :selected="selectedRegionName"
        @select="selectRegion"
      />

      <MetricRanking
        class="panel-rate"
        number="2"
        :title="$t('legacy.t_e5c25b3d0a49')"
        :subtitle="$t('ui.regionalOccupancyHint')"
        :icon="Gauge"
        :rows="rankedRate"
        value-key="occupancyRate"
        unit="%"
        :digits="1"
        :selected="selectedRegionName"
        @select="selectRegion"
      />

      <article class="map-panel data-panel">
        <div class="map-heading">
          <div><span class="panel-number"><MapPinned :size="16" /></span><div><h2>{{ $t('legacy.t_650bc4dc5661') }}</h2><p>{{ $t('legacy.t_c6839888c4df') }}</p></div></div>
          <span class="live-chip"><i></i> {{ $t('legacy.t_4a18eece2754') }}</span>
        </div>
        <div class="map-stage">
          <MalaysiaLeafletMap class="malaysia-map" :regions="allRegions" :selected="selectedRegionName" @select="selectRegion" />

          <div class="map-legend"><span><i class="legend-dot active"></i>{{ $t('legacy.t_7f06ec27ae46') }}</span><span><i class="legend-dot"></i>{{ $t('legacy.t_4b14bcba76f1') }}</span><span>{{ $t('legacy.t_e19d141dfb2e') }}</span></div>

          <section v-if="selectedRegion" class="selected-region-card">
            <div class="selected-title"><span>{{ $t('legacy.t_c2044a8b8223') }}</span><strong>{{ selectedRegion.regionName }}</strong></div>
            <div><span>{{ $t('legacy.t_4254d60c5efe') }}</span><strong>{{ selectedRegion.unitCount }} {{ $t('legacy.t_032231d845f8') }}</strong></div>
            <div><span>{{ $t('legacy.t_20fde4dfff4c') }}</span><strong>{{ percent(selectedRegion.occupancyRate) }}</strong></div>
            <div><span>{{ $t('legacy.t_b6b4888185fc') }}</span><strong>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(selectedRegion.totalRent) }}</strong></div>
            <div><span>{{ $t('legacy.t_8f230fb6b6a6') }}</span><strong>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(selectedRegion.averageRent) }}</strong></div>
          </section>
        </div>
      </article>

      <MetricRanking
        class="panel-rent"
        number="3"
        :title="$t('legacy.t_933e29034eea')"
        :subtitle="$t('ui.regionalRentHint')"
        :icon="WalletCards"
        :rows="rankedRent"
        value-key="totalRent"
        prefix="RM "
        money
        :selected="selectedRegionName"
        @select="selectRegion"
      />

      <MetricRanking
        class="panel-average"
        number="4"
        :title="$t('legacy.t_45d838d32bec')"
        :subtitle="$t('ui.regionalAverageRentHint')"
        :icon="ChartColumnIncreasing"
        :rows="rankedAverage"
        value-key="averageRent"
        prefix="RM "
        money
        :selected="selectedRegionName"
        @select="selectRegion"
      />

      <!-- eslint-disable-next-line @intlify/vue-i18n/no-raw-text -- panel indices are locale-independent -->
      <article class="future-panel panel-five data-panel"><span class="future-number">5</span><div><h2>{{ $t('legacy.t_ae94f2617fef') }}</h2><p>{{ $t('legacy.t_09e45afc86ed') }}</p></div><span class="future-chip">{{ $t('legacy.t_516527377e6a') }}</span></article>
      <!-- eslint-disable-next-line @intlify/vue-i18n/no-raw-text -- panel indices are locale-independent -->
      <article class="future-panel panel-six data-panel"><span class="future-number">6</span><div><h2>{{ $t('legacy.t_001f3cc6a187') }}</h2><p>{{ $t('legacy.t_09e45afc86ed') }}</p></div><span class="future-chip">{{ $t('legacy.t_516527377e6a') }}</span></article>
    </div>
  </section>
</template>

<script>
import { markRaw } from 'vue';
import { Building2, ChartColumnIncreasing, Gauge, MapPinned, Maximize2, RefreshCw, WalletCards } from '@lucide/vue';
import { fetchAdminDashboard } from '../services/propertyApi';
import { findStateByArea, malaysiaLocations } from '../data/malaysiaLocations';
import SmartDashboardMetricPanel from './SmartDashboardMetricPanel.vue';
import MalaysiaLeafletMap from './MalaysiaLeafletMap.vue';

const malaysiaStateNames = malaysiaLocations.map(location => location.state);

export default {
  components: { MetricRanking: SmartDashboardMetricPanel, MalaysiaLeafletMap, RefreshCw, Maximize2, MapPinned },
  inject: ['page'],
  data() {
    return {
      Building2: markRaw(Building2), ChartColumnIncreasing: markRaw(ChartColumnIncreasing), Gauge: markRaw(Gauge),
      WalletCards: markRaw(WalletCards), dashboard: { summary: {}, regions: [] }, loading: false,
      errorMessage: '', selectedRegionName: '', now: new Date(), clockTimer: null
    };
  },
  computed: {
    national() {
      const regions = this.regionsWithData;
      const unitCount = regions.reduce((sum, row) => sum + row.unitCount, 0);
      const occupiedCount = regions.reduce((sum, row) => sum + row.occupiedCount, 0);
      const totalRent = regions.reduce((sum, row) => sum + row.totalRent, 0);
      return { unitCount, occupiedCount, totalRent, occupancyRate: unitCount ? occupiedCount / unitCount * 100 : 0, averageRent: occupiedCount ? totalRent / occupiedCount : 0 };
    },
    normalizedData() {
      const grouped = new Map();
      (this.dashboard.regions || []).forEach(raw => {
        const rawName = String(raw.regionName || '').trim();
        const regionName = malaysiaStateNames.includes(rawName) ? rawName : (findStateByArea(rawName) || rawName || this.$t('ui.unsetRegion'));
        const row = grouped.get(regionName) || { regionName, unitCount: 0, occupiedCount: 0, totalRent: 0, tenantDeposit: 0, reserveBalance: 0 };
        row.unitCount += Number(raw.unitCount || 0);
        row.occupiedCount += Number(raw.occupiedCount || 0);
        row.totalRent += Number(raw.totalRent || 0);
        row.tenantDeposit += Number(raw.tenantDeposit || 0);
        row.reserveBalance += Number(raw.reserveBalance || 0);
        grouped.set(regionName, row);
      });
      return [...grouped.values()].map(row => ({
        ...row,
        occupancyRate: row.unitCount ? row.occupiedCount / row.unitCount * 100 : 0,
        averageRent: row.occupiedCount ? row.totalRent / row.occupiedCount : 0
      }));
    },
    regionsWithData() { return this.normalizedData.filter(row => malaysiaStateNames.includes(row.regionName) || row.unitCount); },
    allRegions() {
      const data = new Map(this.normalizedData.map(row => [row.regionName, row]));
      return malaysiaStateNames.map(regionName => data.get(regionName) || {
        regionName, unitCount: 0, occupiedCount: 0, occupancyRate: 0, totalRent: 0, averageRent: 0, tenantDeposit: 0, reserveBalance: 0
      });
    },
    visibleRegions() {
      const keyword = String(this.page?.globalSearch || this.page?.moduleSearch || '').trim().toLowerCase();
      return this.allRegions.filter(row => !keyword || row.regionName.toLowerCase().includes(keyword));
    },
    rankedUnits() { return this.rankBy('unitCount'); },
    rankedRate() { return this.rankBy('occupancyRate'); },
    rankedRent() { return this.rankBy('totalRent'); },
    rankedAverage() { return this.rankBy('averageRent'); },
    selectedRegion() { return this.allRegions.find(row => row.regionName === this.selectedRegionName) || this.allRegions[0]; },
    formattedNow() {
      return new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false }).format(this.now);
    }
  },
  mounted() {
    this.loadData();
    this.clockTimer = window.setInterval(() => { this.now = new Date(); }, 1000);
  },
  watch: {
    'page.dateStart'() { this.loadData(); },
    'page.dateEnd'() { this.loadData(); }
  },
  beforeUnmount() { if (this.clockTimer) window.clearInterval(this.clockTimer); },
  methods: {
    async loadData() {
      this.loading = true;
      this.errorMessage = '';
      try {
        this.dashboard = await fetchAdminDashboard({ startDate: this.page.dateStart, endDate: this.page.dateEnd });
        const firstDataRegion = this.allRegions.find(row => row.unitCount) || this.allRegions[0];
        if (!this.allRegions.some(row => row.regionName === this.selectedRegionName)) this.selectedRegionName = firstDataRegion?.regionName || '';
      } catch (error) { this.errorMessage = error.message || 'API request failed'; }
      finally { this.loading = false; }
    },
    rankBy(key) { return [...this.visibleRegions].sort((a, b) => Number(b[key] || 0) - Number(a[key] || 0) || a.regionName.localeCompare(b.regionName)); },
    selectRegion(regionName) { this.selectedRegionName = regionName; },
    percent(value) { return `${Number(value || 0).toFixed(1)}%`; },
    money(value) { return Number(value || 0).toLocaleString('en-MY', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); },
    compactMoney(value) { return Number(value || 0).toLocaleString('en-MY', { notation: Number(value || 0) >= 100000 ? 'compact' : 'standard', minimumFractionDigits: Number(value || 0) < 100000 ? 2 : 0, maximumFractionDigits: 2 }); },
    async toggleFullscreen() { if (document.fullscreenElement) await document.exitFullscreen(); else await this.$refs.board?.requestFullscreen(); }
  }
};
</script>

<style scoped>
.smart-board{--board-bg:#06182b;--panel:#09213a;--panel-2:#0b2946;--line:rgba(87,203,215,.24);--teal:#21c8c6;--cyan:#6ce8e3;--text:#eefcff;--muted:#8ba9ba;position:relative;margin:0 24px 26px;padding:16px;min-width:0;border:1px solid #123f5d;border-radius:16px;color:var(--text);background:radial-gradient(circle at 50% -20%,rgba(21,107,131,.42),transparent 42%),linear-gradient(145deg,#071a2e,#041222 68%);box-shadow:0 20px 50px rgba(6,31,51,.18);overflow:hidden}.smart-board::before{content:"";position:absolute;inset:0;pointer-events:none;background-image:linear-gradient(rgba(87,203,215,.025) 1px,transparent 1px),linear-gradient(90deg,rgba(87,203,215,.025) 1px,transparent 1px);background-size:32px 32px}.smart-board:fullscreen{margin:0;border-radius:0;overflow:auto}.board-head,.board-grid,.board-error{position:relative;z-index:1}.board-head{display:grid;grid-template-columns:minmax(260px,1fr) minmax(480px,1.5fr) auto;align-items:center;gap:18px;padding:5px 6px 18px;border-bottom:1px solid var(--line)}.board-title{display:grid;gap:3px}.eyebrow{color:var(--teal);font-size:11px;font-weight:800;letter-spacing:.16em}.board-title h1{margin:0;font-size:25px;letter-spacing:.04em}.board-title p{margin:0;color:var(--muted);font-size:12px}.national-totals{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));overflow:hidden;border:1px solid var(--line);border-radius:10px;background:rgba(7,27,47,.6)}.national-totals>div{padding:9px 12px;border-right:1px solid var(--line)}.national-totals>div:last-child{border:0}.national-totals span{display:block;color:var(--muted);font-size:11px}.national-totals strong{display:inline-block;margin-top:3px;color:#fff;font-size:16px}.national-totals small{margin-left:3px;color:var(--muted)}.head-actions{display:flex;align-items:center;justify-content:flex-end;gap:8px}.head-actions time{margin-right:3px;color:#bdd7e3;font-variant-numeric:tabular-nums;font-size:12px}.head-actions button,.board-error button{display:inline-flex;align-items:center;gap:5px;padding:8px 10px;border:1px solid var(--line);border-radius:7px;color:#dffcff;background:#0b2a45;cursor:pointer}.head-actions button:hover,.board-error button:hover{border-color:var(--teal);background:#0c3851}.head-actions button:disabled{opacity:.55;cursor:wait}.spinning{animation:spin 1s linear infinite}.board-error{display:flex;align-items:center;gap:14px;margin-top:16px;padding:24px;border:1px solid rgba(255,103,103,.45);border-radius:10px;background:rgba(107,27,39,.3)}.board-error span{flex:1;color:#ffc1c1}.board-grid{display:grid;grid-template-columns:minmax(220px,.82fr) minmax(520px,2.25fr) minmax(230px,.9fr);grid-template-areas:"units map rent" "rate map average" "five five six";gap:12px;margin-top:14px}.data-panel{min-width:0;border:1px solid var(--line);border-radius:11px;background:linear-gradient(150deg,rgba(12,42,70,.96),rgba(6,26,46,.96));box-shadow:inset 0 1px rgba(143,245,242,.04),0 10px 30px rgba(0,0,0,.12);overflow:hidden}.panel-units{grid-area:units}.panel-rate{grid-area:rate}.map-panel{grid-area:map}.panel-rent{grid-area:rent}.panel-average{grid-area:average}.panel-five{grid-area:five}.panel-six{grid-area:six}.map-heading{display:flex;align-items:center;justify-content:space-between;gap:10px;min-height:57px;padding:10px 14px;border-bottom:1px solid var(--line);background:linear-gradient(90deg,rgba(28,110,132,.18),transparent)}.map-heading>div{display:flex;align-items:center;gap:9px;min-width:0}.map-heading>svg{flex:0 0 auto;color:var(--cyan)}.panel-number,.future-number{display:grid;place-items:center;flex:0 0 auto;width:27px;height:27px;border:1px solid rgba(81,233,228,.35);border-radius:8px;color:var(--cyan);background:rgba(33,200,198,.09);font-size:13px;font-weight:800}.map-heading h2,.future-panel h2{margin:0;color:#f1feff;font-size:14px}.map-heading p,.future-panel p{margin:3px 0 0;color:var(--muted);font-size:11px;line-height:1.35}.ranking-list{height:214px;padding:6px 8px;overflow:auto;scrollbar-width:thin;scrollbar-color:#1c6680 transparent}.ranking-list button{display:grid;grid-template-columns:24px minmax(0,1fr) auto;align-items:center;gap:7px;width:100%;padding:7px 5px;border:1px solid transparent;border-radius:7px;color:#d7edf3;background:transparent;text-align:left;cursor:pointer}.ranking-list button:hover,.ranking-list button.active{border-color:rgba(83,226,222,.28);background:rgba(33,200,198,.1)}.rank{color:#67899d;font-size:11px;font-variant-numeric:tabular-nums}.rank-main{display:grid;gap:5px;min-width:0}.rank-main b{overflow:hidden;font-size:11px;text-overflow:ellipsis;white-space:nowrap}.rank-main i{height:3px;border-radius:99px;background:#102f49;overflow:hidden}.rank-main em{display:block;height:100%;border-radius:inherit;background:linear-gradient(90deg,#16aeb1,#6ce8e3);box-shadow:0 0 8px rgba(64,229,222,.45)}.ranking-list strong{color:#fff;font-size:11px;font-variant-numeric:tabular-nums;white-space:nowrap}.live-chip,.future-chip{display:inline-flex;align-items:center;gap:6px;border:1px solid rgba(54,203,200,.22);border-radius:99px;padding:5px 8px;color:#a7c2ce;background:rgba(13,56,75,.55);font-size:11px}.live-chip i{width:6px;height:6px;border-radius:50%;background:#45e4ae;box-shadow:0 0 9px #45e4ae}.map-stage{position:relative;min-height:500px}.malaysia-map{display:block;width:100%;height:430px}.state-map path{fill:#28c9c8;stroke:#83eeea;stroke-width:1;vector-effect:non-scaling-stroke;cursor:pointer;transition:fill .16s,fill-opacity .16s,stroke .16s;filter:url(#stateGlow)}.state-map a:hover path,.state-map a:focus path{fill:#67ebe5!important;fill-opacity:.88!important;stroke:#e9ffff;stroke-width:1.5}.state-map path.empty{fill:#173a55;stroke:#48738b}.state-map path.active{fill:#f1ba2d!important;fill-opacity:.96!important;stroke:#fff0a8;stroke-width:2}.state-map text{fill:#b8dce2;font-size:11px;font-weight:700;text-anchor:middle;paint-order:stroke;stroke:#06182b;stroke-width:3px;stroke-linejoin:round;pointer-events:none}.state-map text.active{fill:#ffe99b;font-size:11px}.selected-anchor{fill:#fff1a8;stroke:#f1ba2d;stroke-width:2;filter:url(#stateGlow);pointer-events:none}.sea-label{fill:rgba(98,208,213,.13);font-size:28px;font-weight:800;letter-spacing:.38em}.map-legend{position:absolute;left:14px;bottom:82px;display:flex;gap:13px;padding:6px 9px;border:1px solid var(--line);border-radius:6px;color:#92adba;background:rgba(3,17,31,.76);font-size:11px}.map-legend span{display:flex;align-items:center;gap:5px}.legend-dot{width:6px;height:6px;border-radius:50%;background:#46677c}.legend-dot.active{background:#2be1d8;box-shadow:0 0 6px #2be1d8}.selected-region-card{position:absolute;right:12px;bottom:12px;left:12px;display:grid;grid-template-columns:1.25fr repeat(4,1fr);gap:1px;border:1px solid var(--line);border-radius:8px;background:rgba(4,20,36,.9);overflow:hidden;backdrop-filter:blur(5px)}.selected-region-card>div{display:grid;gap:3px;padding:9px 11px;border-right:1px solid var(--line)}.selected-region-card>div:last-child{border:0}.selected-region-card span{color:var(--muted);font-size:11px}.selected-region-card strong{color:#fff;font-size:12px;white-space:nowrap}.selected-title strong{color:var(--cyan);font-size:15px}.future-panel{display:flex;align-items:center;gap:10px;min-height:66px;padding:11px 13px}.future-panel>div{flex:1}.future-number{border-style:dashed;color:#6b93a5;background:rgba(30,71,94,.25)}.future-chip{color:#6f8d9e;border-style:dashed;background:transparent}@keyframes spin{to{transform:rotate(360deg)}}

/* 利用东西马之间的海域展示当前州属，避免破坏真实地理比例。 */
.map-legend{bottom:12px;background:rgba(3,17,31,.8);backdrop-filter:blur(5px)}
.selected-region-card{top:48%;right:auto;bottom:auto;left:50%;width:min(310px,40%);grid-template-columns:repeat(2,minmax(0,1fr));gap:0;border-color:rgba(87,203,215,.42);border-top:2px solid #f1ba2d;border-radius:9px;background:linear-gradient(145deg,rgba(4,20,36,.94),rgba(7,35,56,.92));box-shadow:0 14px 34px rgba(0,0,0,.28),0 0 22px rgba(42,203,202,.09);transform:translate(-50%,-50%);backdrop-filter:blur(7px)}
.selected-region-card>div{padding:8px 10px;border-top:1px solid var(--line);border-right:1px solid var(--line)}
.selected-region-card>div:nth-child(odd){border-right:0}
.selected-title{grid-column:1/-1;display:flex!important;align-items:center;justify-content:space-between;gap:12px;border-top:0!important;border-right:0!important;background:rgba(19,91,111,.16)}
.selected-title::before{content:"";width:6px;height:6px;flex:0 0 auto;border-radius:50%;background:#f1ba2d;box-shadow:0 0 9px rgba(241,186,45,.75)}
.selected-title span{margin-right:auto}

.map-zone-frames rect{fill:rgba(6,31,52,.48);stroke:rgba(81,211,215,.2);stroke-width:1}.map-zone-frames text{fill:#80adb9;font-size:11px;font-weight:700;letter-spacing:.08em}.map-zone-frames text tspan:first-child{fill:#d5f6f5;font-size:13px;letter-spacing:.04em}.map-zone-frames path{fill:none;stroke:rgba(96,214,217,.3);stroke-width:1.5;stroke-dasharray:3 4}.state-map path{filter:none;stroke:#76d4da;stroke-width:1.1}.state-map path.active{filter:url(#stateGlow)}.state-map text{font-size:11px;font-weight:700}.state-map text.active{font-size:12px}.state-leader{stroke:#91cbd1;stroke-width:.8;stroke-dasharray:2 2;pointer-events:none}.sea-label{font-size:18px;letter-spacing:.3em}

/* MetricRanking 是本文件内的子组件，需要用深度选择器承接大屏主题。 */
.metric-panel :deep(.panel-heading){display:flex;align-items:center;justify-content:space-between;gap:10px;min-height:57px;padding:10px 12px;border-bottom:1px solid var(--line);background:linear-gradient(90deg,rgba(28,110,132,.18),transparent)}
.metric-panel :deep(.panel-heading>div){display:flex;align-items:center;gap:9px;min-width:0}.metric-panel :deep(.panel-heading>svg){flex:0 0 auto;color:var(--cyan)}
.metric-panel :deep(.panel-number){display:grid;place-items:center;flex:0 0 auto;width:27px;height:27px;border:1px solid rgba(81,233,228,.35);border-radius:8px;color:var(--cyan);background:rgba(33,200,198,.09);font-size:13px;font-weight:800}
.metric-panel :deep(.panel-heading h2){margin:0;color:#f1feff;font-size:14px}.metric-panel :deep(.panel-heading p){margin:3px 0 0;color:var(--muted);font-size:11px;line-height:1.35}
.metric-panel :deep(.ranking-list){height:214px;padding:6px 8px;overflow:auto;scrollbar-width:thin;scrollbar-color:#1c6680 transparent}
.metric-panel :deep(.ranking-list button){display:grid;grid-template-columns:24px minmax(0,1fr) auto;align-items:center;gap:7px;width:100%;padding:7px 5px;border:1px solid transparent;border-radius:7px;color:#d7edf3;background:transparent;text-align:left;cursor:pointer}
.metric-panel :deep(.ranking-list button:hover),.metric-panel :deep(.ranking-list button.active){border-color:rgba(83,226,222,.28);background:rgba(33,200,198,.1)}
.metric-panel :deep(.rank){color:#67899d;font-size:11px;font-variant-numeric:tabular-nums}.metric-panel :deep(.rank-main){display:grid;gap:5px;min-width:0}.metric-panel :deep(.rank-main b){overflow:hidden;font-size:11px;text-overflow:ellipsis;white-space:nowrap}.metric-panel :deep(.rank-main i){height:3px;border-radius:99px;background:#102f49;overflow:hidden}.metric-panel :deep(.rank-main em){display:block;height:100%;border-radius:inherit;background:linear-gradient(90deg,#16aeb1,#6ce8e3);box-shadow:0 0 8px rgba(64,229,222,.45)}.metric-panel :deep(.ranking-list strong){color:#fff;font-size:11px;font-variant-numeric:tabular-nums;white-space:nowrap}
@media(max-width:1380px){.board-head{grid-template-columns:1fr auto}.national-totals{grid-column:1/-1;grid-row:2}.board-grid{grid-template-columns:minmax(210px,.8fr) minmax(470px,2fr);grid-template-areas:"units map" "rate map" "rent average" "five six"}.ranking-list{height:200px}}
@media(max-width:900px){.smart-board{margin-inline:12px;padding:12px}.board-head{grid-template-columns:1fr}.head-actions{justify-content:flex-start;flex-wrap:wrap}.national-totals{grid-template-columns:repeat(2,1fr)}.board-grid{grid-template-columns:1fr;grid-template-areas:"map" "units" "rate" "rent" "average" "five" "six"}.map-stage{min-height:470px}.selected-region-card{grid-template-columns:repeat(2,1fr)}.selected-region-card>div{border-bottom:1px solid var(--line)}.selected-title{grid-column:1/-1}.map-legend{bottom:160px;flex-wrap:wrap}}
@media(max-width:560px){.national-totals{grid-template-columns:1fr 1fr}.map-stage{min-height:430px}.malaysia-map{height:360px}.map-legend{display:none}.selected-region-card{position:relative;right:auto;bottom:auto;left:auto;margin:0 8px 8px}.selected-region-card>div{padding:8px}.selected-region-card strong{font-size:11px}}
@media(max-width:900px) and (min-width:561px){.selected-region-card{top:48%;bottom:auto;width:min(330px,46%);grid-template-columns:repeat(2,minmax(0,1fr));transform:translate(-50%,-50%)}.map-legend{bottom:12px}}
@media(max-width:560px){.selected-region-card{top:auto;width:auto;transform:none}}
</style>
