<template>
  <header v-if="currentModule.shell === 'owner-shell'" class="owner-header" :class="{ 'owner-mobile-entry-header': ['ownerProjects', 'myProperties', 'ownerRentalHub', 'ownerMore'].includes(currentId) }">
    <div class="owner-nav">
      <div class="owner-brand"><img class="ccps-header-logo" src="/ccps-logo.png" :alt="$t('legacy.t_107b9d73136a')" /></div>
      <nav>
        <template v-for="module in primaryOwnerModules" :key="module.id">
          <div v-if="module.id === 'ownerFinance'" ref="financeMenu" class="owner-finance-nav" :class="{ active: financeSectionActive, open: financeOpen }">
            <button class="owner-finance-link" type="button" @click="openFinanceOverview">{{ $lt(ownerNavLabel(module)) }}</button>
            <button class="owner-finance-toggle" type="button" :aria-label="$t('ui.financeMenuToggle')" :aria-expanded="financeOpen" aria-haspopup="menu" @click.stop="toggleFinanceMenu"><ChevronDown :size="15" :stroke-width="2.2" aria-hidden="true" /></button>
            <div v-if="financeOpen" class="owner-finance-menu" role="menu">
              <button v-for="child in financeModules" :key="child.id" type="button" role="menuitem" :class="{ active: child.id === currentId }" @click.stop="selectFinanceModule(child.id)">
                <span>{{ $lt(ownerNavLabel(child)) }}</span>
              </button>
            </div>
          </div>
          <button v-else :class="{ active: module.id === currentId, 'owner-notice-link': module.id === 'ownerNotice' }" @click="selectModule(module.id)">{{ $lt(ownerNavLabel(module)) }}<b v-if="module.id === 'ownerNotice' && ownerNotificationUnreadCount > 0" class="owner-nav-badge">{{ ownerNotificationUnreadCount }}</b></button>
        </template>
      </nav>
      <div class="owner-tools">
        <LanguageSwitcher />
        <div ref="accountMenu" class="owner-account">
          <button class="owner-account-trigger" type="button" :aria-expanded="accountOpen" aria-haspopup="menu" @click.stop="accountOpen = !accountOpen">♙ {{ $t('common.account') }}</button>
          <div v-if="accountOpen" class="owner-account-menu" role="menu">
            <span>{{ $t('common.accountMenu') }}</span>
            <button type="button" role="menuitem" :disabled="loggingOut" @click="performLogout">{{ loggingOut ? $t('common.loggingOut') : $t('common.logout') }}</button>
          </div>
        </div>
      </div>
    </div>
    <section v-if="!(currentId === 'ownerPayment' && ownerPaymentSubview === 'upload')" class="owner-hero" :class="{ 'expense-owner-hero': currentId === 'ownerExpenses' }"><div><span v-if="currentId !== 'myProperties' && currentId !== 'ownerPayment' && currentId !== 'ownerFinance' && currentId !== 'rentIncome' && currentId !== 'ownerExpenses' && currentId !== 'ownerReserve' && currentId !== 'ownerNotice' && currentId !== 'ownerDocuments'">{{ currentModule.category }}</span><h1>{{ currentId === 'myProperties' ? $t('common.ownerProperties') : moduleText(currentModule, 'title') }}</h1><p v-if="currentId === 'myProperties'">— &nbsp;{{ $t('common.welcomeOwner') }}</p><p v-else-if="currentId === 'ownerPayment'" class="owner-breadcrumb">{{ $t('common.paymentBreadcrumb') }}</p><p v-else>{{ moduleText(currentModule, 'hint') }}</p></div></section>
  </header>
  <header v-else class="topbar" :class="{ 'dashboard-topbar': currentId === 'adminDashboard' }">
    <div v-if="currentId !== 'adminDashboard'" class="topbar-copy"><span class="topbar-context">{{ adminSectionLabel }}</span><h1>{{ moduleText(currentModule, 'title') }}</h1><p>{{ moduleText(currentModule, 'hint') }}</p></div>
    <div class="top-actions">
      <label class="global-search"><Search class="top-action-icon" :size="16" :stroke-width="2" aria-hidden="true" /><input v-model="globalSearch" @input="showToast($t('common.searchApplied'))" :placeholder="$t('common.searchPlaceholder')"></label>
      <LanguageSwitcher />
      <button class="date-btn" type="button" aria-haspopup="dialog" @click="openDatePanel"><CalendarDays :size="16" :stroke-width="1.9" aria-hidden="true" /><span>{{ dateRange }}</span></button>
      <button class="icon-btn" type="button" :title="$t('common.notifications')" aria-haspopup="dialog" @click="openAlertPanel"><Bell :size="18" :stroke-width="2" aria-hidden="true" /><b v-if="adminAlertCount">{{ adminAlertCount > 99 ? '99+' : adminAlertCount }}</b></button>
      <div ref="accountMenu" class="admin-account">
        <button class="user-btn admin-account-trigger" type="button" :aria-expanded="accountOpen" aria-haspopup="menu" @click.stop="accountOpen = !accountOpen">
          <span>{{ adminInitial }}</span><strong>{{ adminDisplayName }}</strong><ChevronDown :size="14" :class="{ rotated: accountOpen }" aria-hidden="true" />
        </button>
        <div v-if="accountOpen" class="admin-account-menu" role="menu">
          <div class="admin-account-summary"><span>{{ adminDisplayName }}</span><small>{{ adminRoleLabel }}</small></div>
          <button type="button" role="menuitem" :disabled="loggingOut" @click="performLogout"><LogOut :size="16" aria-hidden="true" />{{ loggingOut ? $t('common.loggingOut') : $t('common.logout') }}</button>
        </div>
      </div>
    </div>
  </header>
</template>

<script>
import pageBridge from '../pageBridge';
import LanguageSwitcher from './LanguageSwitcher.vue';
import { Bell, CalendarDays, ChevronDown, LogOut, Search } from '@lucide/vue';
import { ADMIN_STAFF_ROLES, adminStaffRole } from '../utils/adminPermissions';
import { fetchOwnerNotifications } from '../services/propertyApi';
const FINANCE_MODULE_IDS = ['rentIncome', 'ownerExpenses', 'ownerReserve'];
export default {
  props: { ownerPaymentSubview: { type: String, default: 'details' } },
  mixins: [pageBridge],
  components: { Bell, CalendarDays, ChevronDown, LanguageSwitcher, LogOut, Search },
  data() {
    return { accountOpen: false, financeOpen: false, loggingOut: false };
  },
  computed: {
    financeModules() { return FINANCE_MODULE_IDS.map(id => this.ownerModules.find(module => module.id === id)).filter(Boolean); },
    primaryOwnerModules() { return this.ownerModules.filter(module => !FINANCE_MODULE_IDS.includes(module.id) && !module.mobileOnly); },
    financeSectionActive() { return this.currentId === 'ownerFinance' || FINANCE_MODULE_IDS.includes(this.currentId); },
    ownerNotificationUnreadCount() { return Number(this.page.ownerNotificationUnreadCount || 0); },
    adminAlertCount() { return Number(this.page.adminAlertCount || 0); },
    adminSectionLabel() {
      if (this.adminPrimaryModules.some(module => module.id === this.currentId)) return this.$t('navigation.overview');
      const group = this.adminNavGroups.find(item => item.modules.some(module => module.id === this.currentId));
      return group?.labelKey && this.$te(group.labelKey) ? this.$t(group.labelKey) : this.$t('navigation.overview');
    },
    adminDisplayName() { return this.page.currentUser?.displayName || this.page.currentUser?.username || this.$lt('管理员'); },
    adminInitial() { return String(this.adminDisplayName).trim().slice(0, 1).toUpperCase() || '管'; },
    adminRoleLabel() { return this.$lt(ADMIN_STAFF_ROLES[adminStaffRole(this.page.currentUser)] || '后台管理员'); }
  },
  mounted() {
    document.addEventListener('click', this.closeAccountMenu);
    document.addEventListener('keydown', this.handleAccountKeydown);
    if (this.currentModule?.shell === 'owner-shell') this.loadOwnerNotificationCount();
    else this.refreshAdminAlerts();
  },
  beforeUnmount() {
    document.removeEventListener('click', this.closeAccountMenu);
    document.removeEventListener('keydown', this.handleAccountKeydown);
  },
  methods: {
    moduleText(module, field) { const key = `modules.${module.id}.${field}`; const translated = this.$te(key) ? this.$t(key) : ''; return translated || this.$lt(module[field]); },
    ownerNavLabel(module) { return this.moduleText(module, 'name'); },
    closeAccountMenu(event) {
      if (!event || !this.$refs.accountMenu?.contains(event.target)) this.accountOpen = false;
      const financeMenu = Array.isArray(this.$refs.financeMenu) ? this.$refs.financeMenu[0] : this.$refs.financeMenu;
      if (!event || !financeMenu?.contains(event.target)) this.financeOpen = false;
    },
    handleAccountKeydown(event) {
      if (event.key === 'Escape') { this.accountOpen = false; this.financeOpen = false; }
    },
    toggleFinanceMenu() {
      this.financeOpen = !this.financeOpen;
    },
    openFinanceOverview() { this.financeOpen = false; this.selectModule('ownerFinance'); },
    selectFinanceModule(id) { this.financeOpen = false; this.selectModule(id); },
    async loadOwnerNotificationCount() {
      try {
        const data = await fetchOwnerNotifications();
        const notifications = Array.isArray(data?.notifications) ? data.notifications : [];
        this.page.ownerNotificationUnreadCount = notifications.filter(item => item.status !== 'read').length;
      } catch {
        // 通知中心页面成功加载后会再次同步准确数量。
      }
    },
    async performLogout() {
      if (this.loggingOut) return;
      this.loggingOut = true;
      const loggedOut = await this.handleLogout();
      this.loggingOut = false;
      if (loggedOut) this.accountOpen = false;
    }
  }
};
</script>
