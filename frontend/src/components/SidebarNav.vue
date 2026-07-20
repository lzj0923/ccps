<template>
  <aside v-if="currentModule.shell === 'admin-shell'" class="sidebar">
    <div class="brand"><div class="crest">CC</div><div><strong>CCPS</strong><span>系統管理平台</span></div></div>
    <nav class="nav">
      <button v-for="module in adminModules" :key="module.id" type="button" :title="module.name" :class="{ active: module.id === currentId }" @click="selectModule(module.id)">
        <span class="ico" aria-hidden="true"><component :is="sidebarIcon(module.id)" :size="19" :stroke-width="1.9" /></span><span>{{ module.name }}</span>
      </button>
    </nav>
    <div class="project-card"><div class="project-photo" role="img" aria-label="Pavilion Square 城市景觀"></div><strong>PAVILION SQUARE</strong><span>KUALA LUMPUR</span></div>
    <button class="portal-switch" type="button" @click="openOwnerSystem">返回業主端</button>
  </aside>
</template>

<script>
import '../admin-icons.css';
import pageBridge from '../pageBridge';
import { navigate } from '../router';
import { Bell, Building2, ChartColumnIncreasing, Database, House, ReceiptText, UsersRound, WalletCards, Wrench } from '@lucide/vue';
const sidebarIcons = {
  adminOwners: House,
  adminProperties: Building2,
  adminTenants: UsersRound,
  adminMaintenance: Wrench,
  adminFinance: ReceiptText,
  adminData: Building2,
  adminReserve: WalletCards,
  adminAlerts: Bell,
  adminSync: Database,
  adminReports: ChartColumnIncreasing
};

export default {
  mixins: [pageBridge],
  methods: { sidebarIcon(id) { return sidebarIcons[id] || ReceiptText; }, openOwnerSystem() { navigate('/owner'); } }
};
</script>
