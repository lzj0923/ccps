<template>
  <aside v-if="currentModule.shell === 'admin-shell'" class="sidebar">
    <div class="brand"><div class="crest" aria-label="CCPS">CC</div><div><strong>{{ $t('legacy.t_c6a41f9e64a4') }}</strong><span>{{ $t('login.adminConsole') }}</span></div></div>
    <nav class="nav" :aria-label="$t('login.adminConsole')">
      <section v-for="group in adminNavGroups" :key="group.id" class="nav-group" :class="{ 'has-active': group.id === currentGroupId }">
        <button
          class="nav-group-toggle"
          type="button"
          :aria-expanded="isGroupExpanded(group)"
          :aria-controls="`admin-nav-group-${group.id}`"
          @click="toggleGroup(group)"
        >
          <span class="nav-group-title">
            <span class="nav-group-icon" aria-hidden="true"><component :is="groupIcon(group.id)" :size="14" :stroke-width="2" /></span>
            <span>{{ groupLabel(group) }}</span>
          </span>
          <span class="nav-group-meta"><span>{{ group.modules.length }}</span><ChevronDown class="nav-group-chevron" :class="{ expanded: isGroupExpanded(group) }" :size="15" :stroke-width="2" aria-hidden="true" /></span>
        </button>
        <div v-show="isGroupExpanded(group)" :id="`admin-nav-group-${group.id}`" class="nav-group-items">
          <button v-for="module in group.modules" :key="module.id" type="button" :title="moduleLabel(module)" :class="{ active: module.id === currentId }" @click="selectModule(module.id)">
            <span class="ico" aria-hidden="true"><component :is="sidebarIcon(module.id)" :size="19" :stroke-width="1.9" /></span><span>{{ moduleLabel(module) }}</span>
          </button>
        </div>
      </section>
    </nav>
    <div class="project-card"><div class="project-photo" role="img" :aria-label="$t('legacy.t_27200336fb1d')"></div><strong>{{ $t('legacy.t_42472d105d2f') }}</strong><span>{{ $t('legacy.t_3c941f6ff913') }}</span></div>
    <button class="portal-switch" type="button" @click="openOwnerSystem">{{ $t('common.backToOwner') }}</button>
  </aside>
</template>

<script>
import '../admin-icons.css';
import pageBridge from '../pageBridge';
import { navigate } from '../router';
import { Bell, Building2, ChartColumnIncreasing, ChevronDown, History, House, ReceiptText, UsersRound, WalletCards, Wrench } from '@lucide/vue';
const sidebarIcons = {
  adminOwners: House,
  adminProperties: Building2,
  adminProcess: ChartColumnIncreasing,
  adminTenantDirectory: UsersRound,
  adminTenants: UsersRound,
  adminMaintenance: Wrench,
  adminFinance: ReceiptText,
  adminData: Building2,
  adminReserve: WalletCards,
  adminAlerts: Bell,
  adminReports: ChartColumnIncreasing,
  adminAudit: History
};
const groupIcons = {
  assets: Building2,
  rental: UsersRound,
  finance: WalletCards,
  operations: Bell,
  system: History
};

export default {
  mixins: [pageBridge],
  data() { return { expandedGroupIds: [] }; },
  computed: {
    currentGroupId() {
      return this.adminNavGroups.find(group => group.modules.some(module => module.id === this.currentId))?.id || '';
    }
  },
  watch: {
    currentGroupId(groupId) {
      if (groupId && !this.expandedGroupIds.includes(groupId)) this.expandedGroupIds = [...this.expandedGroupIds, groupId];
    }
  },
  mounted() { if (this.currentGroupId) this.expandedGroupIds = [this.currentGroupId]; },
  methods: {
    sidebarIcon(id) { return sidebarIcons[id] || ReceiptText; },
    groupIcon(id) { return groupIcons[id] || Building2; },
    groupLabel(group) { return group.labelKey && this.$te(group.labelKey) ? this.$t(group.labelKey) : group.id; },
    moduleLabel(module) { const key = `modules.${module.id}.name`; return this.$te(key) ? this.$t(key) : module.name; },
    isGroupExpanded(group) { return this.expandedGroupIds.includes(group.id); },
    toggleGroup(group) {
      this.expandedGroupIds = this.isGroupExpanded(group)
        ? this.expandedGroupIds.filter(id => id !== group.id)
        : [...this.expandedGroupIds, group.id];
    },
    openOwnerSystem() { navigate('/owner'); }
  }
};
</script>
