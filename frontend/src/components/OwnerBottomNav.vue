<template>
  <nav class="owner-bottom-nav" :aria-label="navLabel">
    <button
      v-for="item in items"
      :key="item.id"
      type="button"
      :class="{ active: isActive(item), 'is-primary': item.primary }"
      :aria-current="isActive(item) ? 'page' : undefined"
      :data-state="isActive(item) ? 'active' : 'default'"
      @click="selectModule(item.id)"
    >
      <span class="owner-bottom-nav__icon" aria-hidden="true">
        <component :is="item.icon" :size="22" :stroke-width="1.9" />
      </span>
      <span class="owner-bottom-nav__label">{{ $t('ownerApp.navigation.' + item.id) }}</span>
      <b v-if="item.id === 'ownerNotice' && unreadCount > 0" :aria-label="unreadLabel">{{ unreadCount > 99 ? '99+' : unreadCount }}</b>
    </button>
  </nav>
</template>

<script>
import { Bell, Building2, CircleUserRound, House, KeyRound } from '@lucide/vue';
import pageBridge from '../pageBridge';

export default {
  mixins: [pageBridge],
  components: { Bell, Building2, CircleUserRound, House, KeyRound },
  computed: {
    navLabel() {
      const locale = this.$i18n?.locale || 'zh-CN';
      if (locale === 'en') return 'Owner portal navigation';
      if (locale === 'zh-TW') return '業主端導覽';
      return '业主端导航';
    },
    unreadCount() { return Number(this.page.ownerNotificationUnreadCount || 0); },
    unreadLabel() {
      const locale = this.$i18n?.locale || 'zh-CN';
      if (locale === 'en') return `${this.unreadCount} unread messages`;
      if (locale === 'zh-TW') return `${this.unreadCount} 則未讀訊息`;
      return `${this.unreadCount} 条未读消息`;
    },
    items() {
      return [
        { id: 'myProperties', activeIds: ['myProperties', 'ownerPayment'], icon: House, label: '首页' },
        { id: 'ownerNotice', activeIds: ['ownerNotice'], icon: Bell, label: '消息' },
        { id: 'ownerProjects', activeIds: ['ownerProjects'], icon: Building2, label: '资产', primary: true },
        { id: 'ownerRentalHub', activeIds: ['ownerRentalHub', 'ownerFinance', 'rentIncome', 'ownerExpenses', 'ownerReserve'], icon: KeyRound, label: '租务' },
        { id: 'ownerMore', activeIds: ['ownerMore', 'ownerDocuments'], icon: CircleUserRound, label: '我的' }
      ];
    }
  },
  methods: {
    isActive(item) {
      return item.activeIds.includes(this.currentId);
    },
    ownerLabel(id) {
      const module = this.ownerModules.find(item => item.id === id);
      const key = `modules.${id}.name`;
      return this.$te(key) ? this.$t(key) : (module?.name || id);
    }
  }
};
</script>
