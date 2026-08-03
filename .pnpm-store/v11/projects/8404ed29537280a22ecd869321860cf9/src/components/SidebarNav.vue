<template>
  <aside v-if="currentModule.shell === 'admin-shell'" class="sidebar">
    <div class="brand"><div class="crest" aria-label="CCPS">CC</div><div><strong>{{ $t('legacy.t_c6a41f9e64a4') }}</strong><span>{{ $t('login.adminConsole') }}</span></div></div>
    <nav class="nav">
      <button v-for="module in adminModules" :key="module.id" type="button" :title="moduleLabel(module)" :class="{ active: module.id === currentId }" @click="selectModule(module.id)">
        <span class="ico" aria-hidden="true"><component :is="sidebarIcon(module.id)" :size="19" :stroke-width="1.9" /></span><span>{{ moduleLabel(module) }}</span>
      </button>
    </nav>
    <div class="project-card"><div class="project-photo" role="img" :aria-label="$t('legacy.t_27200336fb1d')"></div><strong>{{ $t('legacy.t_42472d105d2f') }}</strong><span>{{ $t('legacy.t_3c941f6ff913') }}</span></div>
    <button class="portal-switch" type="button" @click="openOwnerSystem">{{ $t('common.backToOwner') }}</button>
  </aside>
</template>

<script>
import '../admin-icons.css';
import pageBridge from '../pageBridge';
import { navigate } from '../router';
import { Bell, Building2, ChartColumnIncreasing, History, House, ReceiptText, UsersRound, WalletCards, Wrench } from '@lucide/vue';
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

export default {
  mixins: [pageBridge],
  methods: { sidebarIcon(id) { return sidebarIcons[id] || ReceiptText; }, moduleLabel(module) { const key = `modules.${module.id}.name`; return this.$te(key) ? this.$t(key) : module.name; }, openOwnerSystem() { navigate('/owner'); } }
};
</script>
