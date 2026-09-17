<template>
  <article class="owner-transaction-row">
    <span :class="item.direction"><ArrowDownLeft v-if="item.direction === 'income'" :size="16" /><ArrowUpRight v-else :size="16" /></span>
    <div><strong>{{ $lt(categoryLabel(item.category)) }}</strong><p>{{ item.description || $t('legacy.t_f28c7ae1e106') }}</p><small>{{ formatDate(item.occurredOn) }} · {{ $lt(statusLabel(item.status)) }}</small></div>
    <b :class="{ expense: item.direction === 'expense' }">{{ item.direction === 'income' ? '+' : '-' }}{{ currency }} {{ amount(item.amount) }}</b>
    <footer class="owner-transaction-evidence">
      <span>{{ $t('ownerApp.balance') }} <strong>{{ item.balanceAfter == null ? '—' : `${currency} ${amount(item.balanceAfter)}` }}</strong></span>
      <div v-if="item.documentIds?.length"><button v-for="(id, index) in item.documentIds" :key="id" type="button" @click="$emit('open-document', id)"><Paperclip :size="14" />{{ $t('ownerApp.attachmentNumber', { n: index + 1 }) }}</button></div>
      <small v-else>{{ $t('ownerApp.noAttachment') }}</small>
    </footer>
  </article>
</template>

<script>
import { formatDate, formatDateTime, formatMonth } from '../../utils/dateFormat';
import { ArrowDownLeft, ArrowUpRight, Paperclip } from '@lucide/vue';
import { cashflowCategory } from '../../utils/ownerPortfolio';
export default {
  components: { ArrowDownLeft, ArrowUpRight, Paperclip },
  emits: ['open-document'],
  props: { item: { type: Object, required: true }, currency: { type: String, default: 'MYR' } },
  methods: {
    formatDate, formatDateTime, formatMonth,
    amount(value) { return Number(value || 0).toLocaleString(this.$i18n.locale, { minimumFractionDigits: 2, maximumFractionDigits: 2 }); },
    categoryLabel(value) { return this.$t(`ownerApp.categories.${cashflowCategory(value, this.item.direction)}`); },
    statusLabel(value) { return this.$lt({ confirmed: '已确认', pending: '待确认', paid: '已完成' }[value] || value || '已入账'); }
  }
};
</script>
