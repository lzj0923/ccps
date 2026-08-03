<template>
<section v-if="currentModule.shell === 'owner-shell'" class="metrics owner-metrics"><article v-for="card in ownerSummaryCards" :key="card.label" class="metric"><div class="m-ico property-metric-icon" v-html="ownerMetricIcon(card.icon)"></div><div class="property-metric-copy"><span>{{ card.label }}</span><strong>{{ card.value }}</strong><small>{{ card.text }}</small></div><div class="property-metric-mark" v-html="ownerMetricIcon(card.icon)"></div></article></section>
<section v-if="currentModule.shell === 'admin-shell'" class="metrics"><article v-for="(metric, index) in currentMetrics" :key="metric.label" class="metric"><div class="m-ico admin-metric-icon"><component :is="adminMetricIcon(index)" :size="19" :stroke-width="1.9" /></div><div><span>{{ metric.label }}</span><strong>{{ metric.value }}</strong><small :class="metric.trend">{{ metric.delta }}</small></div></article></section>
</template>

<script>
import pageBridge from "../pageBridge";
import { AlertTriangle, BarChart3, BellRing, Building2, CalendarClock, CheckCircle2, CircleDollarSign, ClipboardCheck, Clock3, Database, Droplets, FileCheck2, Gauge, House, Landmark, PiggyBank, ReceiptText, ShieldCheck, TrendingDown, TrendingUp, UsersRound, WalletCards, Wrench } from '@lucide/vue';

const metricIconSets = {
  adminOwners: [UsersRound, House, Building2, ReceiptText, AlertTriangle],
  adminProperties: [House, CheckCircle2, CalendarClock, ReceiptText],
  adminData: [Landmark, CheckCircle2, ReceiptText, CalendarClock, CheckCircle2, CalendarClock, CircleDollarSign, CalendarClock],
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
    }
  }
};
</script>
