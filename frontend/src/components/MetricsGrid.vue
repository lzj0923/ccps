<template>
<section v-if="currentModule.shell === 'owner-shell'" class="metrics owner-metrics"><article v-for="card in ownerSummaryCards" :key="card.label" class="metric"><div class="m-ico property-metric-icon" v-html="ownerMetricIcon(card.icon)"></div><div class="property-metric-copy"><span>{{ $lt(card.label) }}</span><strong>{{ $lt(card.value) }}</strong><small>{{ $lt(card.text) }}</small></div><div class="property-metric-mark" v-html="ownerMetricIcon(card.icon)"></div></article></section>
<section v-if="currentModule.shell === 'admin-shell'" class="metrics"><article v-for="(metric, index) in currentMetrics" :key="`${metric.label}-${index}`" class="metric" :class="metric.trend ? `metric-${metric.trend}` : 'metric-neutral'" :aria-label="`${$lt(metric.label)}: ${$lt(metric.value)}`"><div class="m-ico admin-metric-icon"><component :is="adminMetricIcon(index)" :size="19" :stroke-width="1.9" /></div><div><span>{{ $lt(metric.label) }}</span><strong>{{ $lt(metric.value) }}</strong><small :class="metric.trend">{{ $lt(metric.delta) }}</small><button v-if="metric.action" type="button" class="metric-detail-btn" @click="runMetricAction(metric.action)"><span>{{ metric.actionLabel ? $lt(metric.actionLabel) : $t('ui.details') }}</span><span aria-hidden="true">→</span></button></div></article></section>
</template>

<script>
import pageBridge from "../pageBridge";
import { AlertTriangle, BarChart3, BellRing, Building2, CalendarClock, CheckCircle2, CircleDollarSign, ClipboardCheck, Clock3, Database, Droplets, FileCheck2, Gauge, House, Landmark, PiggyBank, ReceiptText, ShieldCheck, TrendingDown, TrendingUp, UsersRound, WalletCards, Wrench } from '@lucide/vue';

const metricIconSets = {
  adminOwners: [UsersRound, House, Building2, ReceiptText, AlertTriangle],
  adminProperties: [House, CheckCircle2, CalendarClock, ReceiptText],
  adminData: [Landmark, CheckCircle2, ReceiptText, CheckCircle2, CalendarClock, CircleDollarSign, CalendarClock],
  adminTenants: [UsersRound, WalletCards, CheckCircle2, AlertTriangle, ReceiptText, TrendingDown, FileCheck2],
  adminFinance: [ClipboardCheck, CheckCircle2, ReceiptText, AlertTriangle, CircleDollarSign, Database, Gauge],
  adminMaintenance: [TrendingUp, TrendingDown, Wrench, Droplets, ClipboardCheck, Clock3, WalletCards],
  adminReserve: [WalletCards, AlertTriangle, TrendingUp, TrendingDown, ClipboardCheck, ShieldCheck],
  adminReports: [TrendingUp, TrendingDown, ReceiptText, WalletCards, Wrench, PiggyBank, Database]
};

export default {
  mixins: [pageBridge],
  methods: {
    adminMetricIcon(index) {
      return (metricIconSets[this.currentId] || [BarChart3])[index] || BarChart3;
    },
    runMetricAction(action) {
      window.dispatchEvent(new CustomEvent(action));
    }
  }
};
</script>

<style scoped>
/* Hallmark · component: metric action button · genre: modern-minimal · theme: existing CCPS
 * states: default · hover · focus · active · disabled
 * contrast: pass
 */
.metric-detail-btn{display:inline-flex;align-items:center;justify-content:center;gap:var(--space-admin-xs);min-height:2.25rem;margin-top:var(--space-admin-xs);border:1px solid var(--color-admin-rule-strong);border-radius:var(--radius-admin-control);background:var(--color-admin-accent-soft);padding:.375rem .625rem;color:var(--color-admin-accent);font-size:var(--text-admin-caption);font-weight:700;line-height:1;white-space:nowrap;cursor:pointer;transition:background-color var(--dur-admin-micro) var(--ease-admin-out),border-color var(--dur-admin-micro) var(--ease-admin-out),opacity var(--dur-admin-micro) var(--ease-admin-out)}.metric-detail-btn:focus-visible{outline:2px solid var(--color-admin-focus);outline-offset:2px}.metric-detail-btn:active{background:var(--color-admin-surface-muted)}.metric-detail-btn:disabled{opacity:.45;cursor:not-allowed}@media (hover:hover){.metric-detail-btn:hover{border-color:var(--color-admin-accent);background:var(--color-admin-surface-muted)}}@media (max-width:820px){.metric-detail-btn{min-height:2.75rem}}@media (prefers-reduced-motion:reduce){.metric-detail-btn{transition:none}}
</style>
