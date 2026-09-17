<template>
  <div v-if="properties.length" class="owner-property-grid-v2">
    <article v-for="property in properties" :key="property.ownerUnitId">
      <header>
        <OwnerPropertyCover :property="property" />
        <div class="owner-property-identity"><h2>{{ property.projectName || $t('legacy.t_efc349204510') }}</h2><p>{{ property.unitNo || '—' }}<template v-if="property.areaSqm"> · {{ property.areaSqm }} m²</template></p><span>{{ property.city || $t('legacy.t_a4320ae5c6b6') }}</span></div>
        <b :class="{ pre: property.assetStage === 'PRE_HANDOVER' }">{{ $lt(stageLabel(property.assetStage)) }}</b>
      </header>
      <dl>
        <div class="owner-property-price"><dt>{{ $t('legacy.t_90aab4d3f61e') }}</dt><dd>{{ money(property.purchasePrice, property.currency) }}</dd></div>
        <div><dt>{{ $t('legacy.t_ee023ed023f9') }}</dt><dd>{{ formatMonth(property.purchaseDate) }}</dd></div>
        <div v-if="hasOwnerPaymentRecords(property)"><dt>{{ $t('legacy.t_b0a5b93d3722') }}</dt><dd>{{ money(property.paidAmount, property.currency) }}</dd><small>{{ percent(property) }}%</small></div>
        <div v-if="hasOwnerPaymentRecords(property)"><dt>{{ $t('legacy.t_96b4a1fd550b') }}</dt><dd>{{ money(property.remainingAmount, property.currency) }}</dd><small>{{ 100 - percent(property) }}%</small></div>
      </dl>
      <div v-if="hasOwnerPaymentRecords(property)" class="owner-progress-v2" :aria-label="`${$t('legacy.t_390e249deb38')} ${percent(property)}%`"><i :style="{ width: `${percent(property)}%` }"></i></div>
      <p v-else class="owner-context-note">{{ $t('ownerApp.paymentNotArchived') }}</p>
      <nav :aria-label="$t('legacy.t_d285e91a7abd')">
        <button type="button" @click="$emit('open', property, 'property')"><Landmark :size="16" />{{ $t('ownerApp.facts') }}</button>
        <button v-if="property.assetStage === 'PRE_HANDOVER'" type="button" @click="$emit('open', property, 'payment')"><CircleDollarSign :size="16" />{{ $t('ownerApp.payment') }}</button>
        <button v-else type="button" @click="$emit('open', property, 'cashflow')"><ChartNoAxesColumnIncreasing :size="16" />{{ $t('ownerApp.cashflow') }}</button>
        <button type="button" @click="$emit('open', property, 'files')"><Files :size="16" />{{ $t('ownerApp.files') }}</button>
      </nav>
    </article>
  </div>
  <div v-else class="owner-app-state"><Building2 :size="24" /><strong>{{ $t('legacy.t_d9810ccbb682') }}</strong><span>{{ $t('legacy.t_f683eed468ac') }}</span></div>
</template>

<script>
import { formatMonth } from '../../utils/dateFormat';
import { Building2, ChartNoAxesColumnIncreasing, CircleDollarSign, Files, Landmark } from '@lucide/vue';
import OwnerPropertyCover from './OwnerPropertyCover.vue';
import { hasOwnerPaymentRecords } from '../../utils/ownerPortfolio';

export default {
  components: { OwnerPropertyCover, Building2, ChartNoAxesColumnIncreasing, CircleDollarSign, Files, Landmark },
  props: { properties: { type: Array, default: () => [] }, currency: { type: String, default: 'MYR' } },
  emits: ['open'],
  methods: {
    hasOwnerPaymentRecords,
    initials(value) { return String(value || 'CC').split(/\s+/).map(word => word[0]).join('').slice(0, 2).toUpperCase(); },
    stageLabel(value) { return this.$lt(value === 'PRE_HANDOVER' ? '预售／未交房' : value === 'OPERATING' ? '已交房' : '待确认'); },
    formatMonth,
    money(value, currency) { return `${currency || this.currency} ${Number(value || 0).toLocaleString(this.$i18n.locale, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`; },
    percent(property) { const total = Number(property.purchasePrice || 0); return total ? Math.min(100, Math.max(0, Math.round(Number(property.paidAmount || 0) / total * 100))) : 0; }
  }
};
</script>
