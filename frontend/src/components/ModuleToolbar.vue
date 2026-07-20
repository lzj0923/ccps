<template>
  <section class="toolbar" :class="{ 'owner-toolbar': currentModule.shell === 'owner-shell' }">
    <label class="search-box"><span class="search-mark">⌕</span><input v-model="moduleSearch" :placeholder="toolbarSearchHint"></label>
    <select v-model="projectFilter">
      <option>全部建案</option>
      <option v-for="project in projectOptions" :key="project" :value="project">{{ project }}</option>
    </select>
    <select v-if="showStatusFilter" v-model="statusFilter"><option v-for="status in statusOptions" :key="status">{{ status }}</option></select>
    <button class="primary-btn" @click="triggerPrimaryAction">{{ toolbarPrimaryAction }}</button><button v-if="showSecondaryAction" class="primary-btn muted" @click="triggerSecondaryAction">{{ toolbarSecondaryAction }}</button><button class="ghost-btn" @click="exportCsv">匯出</button>
  </section>
</template>

<script>
import pageBridge from '../pageBridge';
export default {
  mixins: [pageBridge],
  computed: {
    embeddedAccounts() { return this.currentId === 'adminOwners' && this.page.adminOwnerWorkspaceTab === 'accounts'; },
    toolbarSearchHint() { return this.embeddedAccounts ? '搜尋登入帳號 / 姓名 / 手機號' : this.currentModule.searchHint; },
    toolbarPrimaryAction() { if (this.currentId === 'adminRentalMandates') return '建立委託'; if (this.currentId === 'adminReserve') return '設定預備金'; if (this.currentId === 'adminAlerts') return '新增提醒規則'; if (this.currentId === 'adminSync') return '建立匯出批次'; if (this.currentId === 'adminReports') return '產生報表'; if (this.currentId === 'adminFinance') return this.page.adminFinanceViewMode === 'history' ? '重新載入' : this.page.adminFinanceMode === 'rent' ? '確認租金' : '重新載入'; return this.embeddedAccounts ? '新增帳號' : this.currentModule.primaryAction; },
    toolbarSecondaryAction() { if (this.currentId === 'adminReserve') return '直接充值'; if (this.currentId === 'adminAlerts') return '執行全部規則'; if (['adminSync','adminReports'].includes(this.currentId)) return '重新載入'; return this.embeddedAccounts ? '帳號說明' : this.currentModule.secondaryAction; },
    showSecondaryAction() { return !['adminOwners', 'adminProperties'].includes(this.currentId) && !(this.currentId === 'adminFinance' && (this.page.adminFinanceMode === 'rent' || this.page.adminFinanceViewMode === 'history')); },
    showStatusFilter() { return !(this.currentId === 'adminFinance' && this.page.adminFinanceViewMode !== 'history' && this.page.adminFinanceMode !== 'rent'); },
    projectOptions() {
      if (this.embeddedAccounts) return ['全部帳號'];
      if (this.currentId === 'adminOwners') return this.page.adminOwnerProjects || [];
      if (this.currentId === 'adminProperties') return this.page.adminOwnerProjects || [];
      if (this.currentId === 'adminData') return this.page.adminBuildingProjects || [];
      if (this.currentId === 'adminFinance') return this.page.adminFinanceProjects || [];
      if (this.currentId === 'adminTenants') return this.page.adminTenancyProjects || [];
      if (this.currentId === 'adminRentalMandates') return this.page.adminRentalMandateProjects || [];
      if (this.currentId === 'adminMaintenance') return this.page.adminMaintenanceProjects || [];
      if (this.currentId === 'adminReserve') return this.page.adminReserveProjects || [];
      if (this.currentId === 'adminAlerts') return ['全部提醒'];
      if (this.currentId === 'adminReports') return this.page.adminReportProjects || [];
      if (this.currentId === 'adminSync') return ['全部批次'];
      if (this.currentId === 'adminAccounts') return ['全部帳號'];
      return ['Pavilion Square', 'CCP Residence', 'CCPS Heights'];
    },
    statusOptions() {
      if (this.embeddedAccounts) return ['全部狀態', '啟用', '停用'];
      if (this.currentId === 'adminOwners') return ['全部狀態', '啟用', '停用'];
      if (this.currentId === 'adminProperties') return ['全部出租狀態', '未交房', '已交房待出租', '出租中', '未啟用出租'];
      if (this.currentId === 'adminData') return ['全部狀態', '已完成', '部分付款', '待付款', '逾期'];
      if (this.currentId === 'adminFinance') {
        if (this.page.adminFinanceViewMode === 'history') return ['全部歷史', '已確認', '已退回'];
        return this.page.adminFinanceMode === 'rent' ? ['全部租金狀態', '未支付', '部分支付', '已逾期'] : ['全部狀態'];
      }
      if (this.currentId === 'adminTenants') return ['全部狀態', '已收', '部分收款', '待收', '逾期'];
      if (this.currentId === 'adminRentalMandates') return ['全部狀態', '草稿', '待審核', '啟用', '暫停', '已終止'];
      if (this.currentId === 'adminMaintenance') return ['全部狀態', '已付款', '部分付款', '待付款', '待處理', '處理中', '已完成', '已取消'];
      if (this.currentId === 'adminReserve') return ['全部狀態', '正常', '餘額不足', '待財務確認'];
      if (this.currentId === 'adminAlerts') return ['全部狀態', '啟用', '停用', '待發送', '發送失敗'];
      if (this.currentId === 'adminSync') return ['全部狀態', '處理中', '已匯出', '同步失敗'];
      if (this.currentId === 'adminReports') return ['全部狀態', '已完成', '產生中', '產生失敗'];
      if (this.currentId === 'adminAccounts') return ['全部狀態', '啟用', '停用'];
      return ['全部狀態', '正常', '已完成', '待處理', '逾期'];
    }
  }
};
</script>
