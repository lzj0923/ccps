<template>
  <header v-if="currentModule.shell === 'owner-shell'" class="owner-header">
    <div class="owner-nav">
      <div class="owner-brand"><img class="ccps-header-logo" src="/ccps-logo.png" :alt="$t('legacy.t_107b9d73136a')" /></div>
      <nav>
        <template v-for="module in primaryOwnerModules" :key="module.id">
          <div v-if="module.id === 'ownerFinance'" ref="financeMenu" class="owner-finance-nav" :class="{ active: financeSectionActive, open: financeOpen }">
            <button class="owner-finance-link" type="button" @click="openFinanceOverview">{{ ownerNavLabel(module) }}</button>
            <button class="owner-finance-toggle" type="button" :aria-label="ownerNavLabel(module)" :aria-expanded="financeOpen" aria-haspopup="menu" @click.stop="toggleFinanceMenu"><ChevronDown :size="15" :stroke-width="2.2" aria-hidden="true" /></button>
            <div v-if="financeOpen" class="owner-finance-menu" role="menu">
              <button v-for="child in financeModules" :key="child.id" type="button" role="menuitem" :class="{ active: child.id === currentId }" @click.stop="selectFinanceModule(child.id)">
                <span>{{ ownerNavLabel(child) }}</span>
              </button>
            </div>
          </div>
          <button v-else :class="{ active: module.id === currentId, 'owner-notice-link': module.id === 'ownerNotice' }" @click="selectModule(module.id)">{{ ownerNavLabel(module) }}<b v-if="module.id === 'ownerNotice'" class="owner-nav-badge">{{ $t('legacy.t_77de68daecd8') }}</b></button>
        </template>
      </nav>
      <div class="owner-tools">
        <LanguageSwitcher />
        <div ref="accountMenu" class="owner-account">
          <button class="owner-account-trigger" type="button" :aria-expanded="accountOpen" aria-haspopup="menu" @click.stop="accountOpen = !accountOpen">♙ {{ $t('common.account') }}</button>
          <div v-if="accountOpen" class="owner-account-menu" role="menu">
            <span>{{ $t('common.accountMenu') }}</span>
            <button class="portal-menu-item" type="button" role="menuitem" @click="openAdminSystem">{{ $t('common.enterAdmin') }}</button>
            <button type="button" role="menuitem" :disabled="loggingOut" @click="performLogout">{{ loggingOut ? $t('common.loggingOut') : $t('common.logout') }}</button>
          </div>
        </div>
      </div>
    </div>
    <section v-if="!(currentId === 'ownerPayment' && ownerPaymentSubview === 'upload')" class="owner-hero" :class="{ 'expense-owner-hero': currentId === 'ownerExpenses' }"><div><span v-if="currentId !== 'myProperties' && currentId !== 'ownerPayment' && currentId !== 'ownerFinance' && currentId !== 'rentIncome' && currentId !== 'ownerExpenses' && currentId !== 'ownerReserve' && currentId !== 'ownerNotice' && currentId !== 'ownerDocuments'">{{ currentModule.category }}</span><h1>{{ currentId === 'myProperties' ? $t('common.ownerProperties') : moduleText(currentModule, 'title') }}</h1><p v-if="currentId === 'myProperties'">— &nbsp;{{ $t('common.welcomeOwner') }}</p><p v-else-if="currentId === 'ownerPayment'" class="owner-breadcrumb">{{ $t('common.paymentBreadcrumb') }}</p><p v-else>{{ moduleText(currentModule, 'hint') }}</p></div></section>
  </header>
  <header v-else class="topbar">
    <div><h1>{{ moduleText(currentModule, 'title') }}</h1><p>{{ moduleText(currentModule, 'hint') }}</p></div>
    <div class="top-actions"><label class="global-search"><Search class="top-action-icon" :size="16" :stroke-width="2" aria-hidden="true" /><input v-model="globalSearch" @input="showToast($t('common.searchApplied'))" :placeholder="$t('common.searchPlaceholder')"></label><LanguageSwitcher /><button class="date-btn" @click="openDatePanel"><CalendarDays :size="16" :stroke-width="1.9" aria-hidden="true" /><span>{{ dateRange }}</span></button><button class="icon-btn" :title="$t('common.notifications')" @click="openAlertPanel"><Bell :size="18" :stroke-width="2" aria-hidden="true" /><b>{{ alertItems.length }}</b></button><button class="user-btn"><span>{{ $t('legacy.t_b1fb3bec6fdb') }}</span>{{ $t('legacy.t_dcc12647b008') }}</button></div>
  </header>
</template>

<script>
import pageBridge from '../pageBridge';
import { navigate } from '../router';
import LanguageSwitcher from './LanguageSwitcher.vue';
import { Bell, CalendarDays, ChevronDown, Search } from '@lucide/vue';
const FINANCE_MODULE_IDS = ['rentIncome', 'ownerExpenses', 'ownerReserve'];
export default {
  props: { ownerPaymentSubview: { type: String, default: 'details' } },
  mixins: [pageBridge],
  components: { Bell, CalendarDays, ChevronDown, LanguageSwitcher, Search },
  data() {
    return { accountOpen: false, financeOpen: false, loggingOut: false };
  },
  computed: {
    financeModules() { return FINANCE_MODULE_IDS.map(id => this.ownerModules.find(module => module.id === id)).filter(Boolean); },
    primaryOwnerModules() { return this.ownerModules.filter(module => !FINANCE_MODULE_IDS.includes(module.id)); },
    financeSectionActive() { return this.currentId === 'ownerFinance' || FINANCE_MODULE_IDS.includes(this.currentId); }
  },
  mounted() {
    document.addEventListener('click', this.closeAccountMenu);
    document.addEventListener('keydown', this.handleAccountKeydown);
  },
  beforeUnmount() {
    document.removeEventListener('click', this.closeAccountMenu);
    document.removeEventListener('keydown', this.handleAccountKeydown);
  },
  methods: {
    moduleText(module, field) { const key = `modules.${module.id}.${field}`; return this.$te(key) ? this.$t(key) : module[field]; },
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
    openAdminSystem() { navigate('/admin'); },
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
