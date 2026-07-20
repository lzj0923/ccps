<template>
  <header v-if="currentModule.shell === 'owner-shell'" class="owner-header">
    <div class="owner-nav">
      <div class="owner-brand"><img class="ccps-header-logo" src="/ccps-logo.png" alt="CCPS 家慶企業" /></div>
      <nav><button v-for="module in ownerModules" :key="module.id" :class="{ active: module.id === currentId, 'owner-notice-link': module.id === 'ownerNotice' }" @click="selectModule(module.id)">{{ ownerNavLabel(module) }}<b v-if="module.id === 'ownerNotice'" class="owner-nav-badge">3</b></button></nav>
      <div class="owner-tools">
        <div ref="accountMenu" class="owner-account">
          <button class="owner-account-trigger" type="button" :aria-expanded="accountOpen" aria-haspopup="menu" @click.stop="accountOpen = !accountOpen">♙ 我的帳號</button>
          <div v-if="accountOpen" class="owner-account-menu" role="menu">
            <span>帳號選單</span>
            <button class="portal-menu-item" type="button" role="menuitem" @click="openAdminSystem">進入後臺管理</button>
            <button type="button" role="menuitem" :disabled="loggingOut" @click="performLogout">{{ loggingOut ? '正在退出…' : '退出登入' }}</button>
          </div>
        </div>
      </div>
    </div>
    <section v-if="!(currentId === 'ownerPayment' && ownerPaymentSubview === 'upload')" class="owner-hero" :class="{ 'expense-owner-hero': currentId === 'ownerExpenses' }"><div><span v-if="currentId !== 'myProperties' && currentId !== 'ownerPayment' && currentId !== 'rentIncome' && currentId !== 'ownerExpenses' && currentId !== 'ownerReserve' && currentId !== 'ownerNotice' && currentId !== 'ownerDocuments'">{{ currentModule.category }}</span><h1>{{ currentId === 'myProperties' ? '我的房产' : currentModule.title }}</h1><p v-if="currentId === 'myProperties'">— &nbsp;欢迎回来，尊贵的业主</p><p v-else-if="currentId === 'ownerPayment'" class="owner-breadcrumb">我的房产　&gt;　房款进度　&gt;　房款進度詳情</p><p v-else>{{ currentModule.hint }}</p></div></section>
  </header>
  <header v-else class="topbar">
    <div><h1>{{ currentModule.title }}</h1><p>{{ currentModule.hint }}</p></div>
    <div class="top-actions"><label class="global-search"><span class="search-mark">⌕</span><input v-model="globalSearch" @input="showToast('已套用搜索')" placeholder="搜索业主 / 单位 / 租客 / 文件"></label><button class="date-btn" @click="openDatePanel">{{ dateRange }}</button><button class="icon-btn" title="通知" @click="openAlertPanel">!<b>{{ alertItems.length }}</b></button><button class="user-btn"><span>AC</span>Admin CCPS</button></div>
  </header>
</template>

<script>
import pageBridge from '../pageBridge';
import { navigate } from '../router';
export default {
  props: { ownerPaymentSubview: { type: String, default: 'details' } },
  mixins: [pageBridge],
  data() {
    return { accountOpen: false, loggingOut: false };
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
    ownerNavLabel(module) {
      return { myProperties: '我的房产', ownerPayment: '房款进度', rentIncome: '租金收入', ownerExpenses: '收支维修', ownerReserve: '预备金', ownerNotice: '通知中心', ownerDocuments: '文件资料' }[module.id] || module.name;
    },
    closeAccountMenu(event) {
      if (!event || !this.$refs.accountMenu?.contains(event.target)) this.accountOpen = false;
    },
    handleAccountKeydown(event) {
      if (event.key === 'Escape') this.accountOpen = false;
    },
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
