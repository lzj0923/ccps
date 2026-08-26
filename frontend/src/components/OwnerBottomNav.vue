<template>
  <nav class="owner-bottom-nav" :aria-label="navLabel">
    <button
      v-for="item in items"
      :key="item.id"
      type="button"
      :class="{ active: item.activeIds.includes(currentId) }"
      :aria-current="item.activeIds.includes(currentId) ? 'page' : undefined"
      data-state="default"
      @click="selectModule(item.id)"
    >
      <component :is="item.icon" :size="21" :stroke-width="2" aria-hidden="true" />
      <span>{{ item.label }}</span>
      <b v-if="item.id === 'ownerNotice' && unreadCount > 0">{{ unreadCount > 99 ? '99+' : unreadCount }}</b>
    </button>
  </nav>
</template>

<script>
import { Building2, CircleDollarSign, Files, Landmark, Bell } from '@lucide/vue';
import pageBridge from '../pageBridge';

export default {
  mixins: [pageBridge],
  components: { Bell, Building2, CircleDollarSign, Files, Landmark },
  computed: {
    navLabel() {
      const locale = this.$i18n?.locale || 'zh-CN';
      if (locale === 'en') return 'Owner portal navigation';
      if (locale === 'zh-TW') return '業主端導覽';
      return '业主端导航';
    },
    unreadCount() { return Number(this.page.ownerNotificationUnreadCount || 0); },
    items() {
      return [
        { id: 'myProperties', activeIds: ['myProperties'], icon: Building2, label: this.ownerLabel('myProperties') },
        { id: 'ownerPayment', activeIds: ['ownerPayment'], icon: CircleDollarSign, label: this.ownerLabel('ownerPayment') },
        { id: 'ownerFinance', activeIds: ['ownerFinance', 'rentIncome', 'ownerExpenses', 'ownerReserve'], icon: Landmark, label: this.ownerLabel('ownerFinance') },
        { id: 'ownerNotice', activeIds: ['ownerNotice'], icon: Bell, label: this.ownerLabel('ownerNotice') },
        { id: 'ownerDocuments', activeIds: ['ownerDocuments'], icon: Files, label: this.ownerLabel('ownerDocuments') }
      ];
    }
  },
  methods: {
    ownerLabel(id) {
      const module = this.ownerModules.find(item => item.id === id);
      const key = `modules.${id}.name`;
      return this.$te(key) ? this.$t(key) : (module?.name || id);
    }
  }
};
</script>
