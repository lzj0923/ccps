<template>
  <article class="data-panel metric-panel">
    <div class="metric-heading">
      <div class="metric-title"><span class="metric-number">{{ number }}</span><div><h2>{{ title }}</h2><p>{{ subtitle }}</p></div></div>
      <component :is="icon" :size="22" aria-hidden="true" />
    </div>
    <div class="metric-list">
      <button v-for="(row, index) in rows" :key="row.regionName" type="button" :class="{ active: selected === row.regionName, zero: Number(row[valueKey] || 0) === 0 }" @click="$emit('select', row.regionName)">
        <span class="metric-rank">{{ String(index + 1).padStart(2, '0') }}</span>
        <span class="metric-region"><b>{{ row.regionName }}</b><i><em :style="{ width: barWidth(row) }"></em></i></span>
        <strong>{{ prefix }}{{ displayValue(row[valueKey]) }}{{ unit }}</strong>
      </button>
    </div>
  </article>
</template>

<script>
export default {
  props: {
    number: String,
    title: String,
    subtitle: String,
    icon: { type: [Object, Function], required: true },
    rows: { type: Array, default: () => [] },
    valueKey: String,
    unit: { type: String, default: '' },
    prefix: { type: String, default: '' },
    digits: { type: Number, default: 0 },
    money: Boolean,
    selected: String
  },
  emits: ['select'],
  methods: {
    displayValue(value) {
      const number = Number(value || 0);
      return this.money
        ? number.toLocaleString('en-MY', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
        : number.toLocaleString('en-MY', { minimumFractionDigits: this.digits, maximumFractionDigits: this.digits });
    },
    barWidth(row) {
      const max = Math.max(...this.rows.map(item => Number(item[this.valueKey] || 0)), 1);
      const current = Number(row[this.valueKey] || 0);
      return `${Math.max(current > 0 ? 5 : 0, current / max * 100)}%`;
    }
  }
};
</script>

<style scoped>
.metric-panel{min-height:286px}.metric-heading{display:flex;align-items:center;justify-content:space-between;gap:10px;min-height:62px;padding:11px 13px;border-bottom:1px solid var(--line);background:linear-gradient(90deg,rgba(28,110,132,.2),transparent)}.metric-heading>svg{flex:0 0 auto;color:var(--cyan)}.metric-title{display:flex;align-items:center;gap:10px;min-width:0}.metric-number{display:grid;place-items:center;flex:0 0 auto;width:31px;height:31px;border:1px solid rgba(81,233,228,.42);border-radius:8px;color:var(--cyan);background:rgba(33,200,198,.11);font-size:15px;font-weight:800}.metric-title h2{margin:0;color:#f1feff;font-size:15px}.metric-title p{margin:4px 0 0;color:var(--muted);font-size:11px;line-height:1.35}.metric-list{height:224px;padding:7px 8px;overflow:auto;scrollbar-width:thin;scrollbar-color:#1c6680 transparent}.metric-list button{display:grid;grid-template-columns:25px minmax(0,1fr) auto;align-items:center;gap:8px;width:100%;padding:8px 6px;border:1px solid transparent;border-radius:7px;color:#d7edf3;background:transparent;text-align:left;cursor:pointer;transition:background .16s,border-color .16s,opacity .16s}.metric-list button:hover,.metric-list button.active{border-color:rgba(83,226,222,.3);background:rgba(33,200,198,.11)}.metric-list button.zero:not(.active){opacity:.56}.metric-list button.active{box-shadow:inset 3px 0 #f2bd32}.metric-rank{color:#7295a7;font-size:11px;font-variant-numeric:tabular-nums}.metric-region{display:grid;gap:6px;min-width:0}.metric-region b{overflow:hidden;font-size:12px;text-overflow:ellipsis;white-space:nowrap}.metric-region i{height:4px;border-radius:99px;background:#102f49;overflow:hidden}.metric-region em{display:block;height:100%;border-radius:inherit;background:linear-gradient(90deg,#16aeb1,#6ce8e3);box-shadow:0 0 8px rgba(64,229,222,.45)}.metric-list strong{color:#fff;font-size:12px;font-variant-numeric:tabular-nums;white-space:nowrap}
</style>
