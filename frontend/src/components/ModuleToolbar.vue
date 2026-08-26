<template>
  <section class="toolbar" :class="{ 'owner-toolbar': currentModule.shell === 'owner-shell' }">
    <label class="search-box"><span class="search-mark">⌕</span><input v-model="moduleSearch" :placeholder="toolbarSearchHint"></label>
    <select v-if="currentId === 'adminMaintenance'" v-model="page.adminMaintenanceDistrictFilter" class="maintenance-toolbar-district" aria-label="行政区筛选">
      <option value="">全部行政区</option>
      <option value="kuala_lumpur">吉隆坡</option>
      <option value="johor">新山</option>
      <option value="melaka">马六甲</option>
      <option value="other">其他</option>
    </select>
    <select v-model="projectFilter">
      <option value="全部建案">{{ $t('ui.allProjects') }}</option>
      <option v-for="(project, index) in projectOptions" :key="projectOptionKey(project, index)" :value="project">{{ optionLabel(project) }}</option>
    </select>
    <select v-if="showStatusFilter" v-model="statusFilter"><option v-for="status in statusOptions" :key="status" :value="status">{{ optionLabel(status) }}</option></select>
    <button v-if="canManageCurrentAdminModule" class="primary-btn" @click="triggerPrimaryAction">{{ toolbarPrimaryAction }}</button><button v-if="canManageCurrentAdminModule && showBuildingPropertyAction" class="primary-btn muted" @click="triggerBuildingPropertyAction">{{ $t('building.addPreHandoverProperty') }}</button><button v-if="canManageCurrentAdminModule && showSecondaryAction" class="primary-btn muted" @click="triggerSecondaryAction">{{ toolbarSecondaryAction }}</button><button v-if="canManageCurrentAdminModule && showReserveRefundAction" class="primary-btn muted" @click="triggerReserveRefundAction">{{ $t('ui.ownerReserveRefund') }}</button><button class="ghost-btn" @click="exportCsv">{{ $t('ui.export') }}</button>
  </section>
</template>

<script>
import pageBridge from '../pageBridge';
export default {
  mixins: [pageBridge],
  computed: {
    embeddedAccounts() { return this.currentId === 'adminOwners' && this.page.adminOwnerWorkspaceTab === 'accounts'; },
    toolbarSearchHint() { if (this.embeddedAccounts) return this.$t('ui.searchAccounts'); if (this.currentId === 'adminOwners') return this.$t('ui.searchOwners'); if (this.currentId === 'adminProperties') return this.$t('properties.searchProperties'); if (this.currentId === 'adminAlerts') return this.$t('ui.searchReminders'); if (this.currentId === 'adminFinance') return this.$t('finance.search'); return this.currentModule.searchHint; },
    toolbarPrimaryAction() { if (this.currentId === 'adminAlerts') return this.$t('ui.addReminderRule'); if (this.currentId === 'adminFinance') return this.$t('ui.reload'); if (this.currentId === 'adminProperties') return this.$t('properties.addProperty'); if (this.currentId === 'adminOwners') return this.$t('ui.addOwner'); if (this.currentId === 'adminData') return this.$t('building.createProject'); if (this.currentId === 'adminTenants') return this.$t('ui.addTenant'); if (this.currentId === 'adminMaintenance') return this.$t('ui.addExpense'); if (this.currentId === 'adminReserve') return this.$t('ui.addReserveTopup'); if (this.currentId === 'adminReports') return this.$t('ui.addReport'); return this.embeddedAccounts ? this.$t('ui.addAccount') : this.currentModule.primaryAction; },
    toolbarSecondaryAction() { if (this.currentId === 'adminAlerts') return this.$t('ui.runAllRules'); if (this.currentId === 'adminData') return this.$t('building.createPaymentPlan'); if (this.currentId === 'adminReports') return this.$t('ui.reload'); if (this.currentId === 'adminFinance') return this.$t('finance.batchConfirm'); if (this.currentId === 'adminTenants') return this.$t('ui.addLease'); if (this.currentId === 'adminMaintenance') return this.$t('ui.addMaintenance'); if (this.currentId === 'adminReserve') return this.$t('ui.addReserveDebit'); return this.embeddedAccounts ? this.$t('ui.accountHelp') : this.currentModule.secondaryAction; },
    showSecondaryAction() { return Boolean(String(this.toolbarSecondaryAction || '').trim()) && !['adminOwners', 'adminProperties'].includes(this.currentId) && !(this.currentId === 'adminFinance' && this.page.adminFinanceViewMode === 'history'); },
    showBuildingPropertyAction() { return this.currentId === 'adminData'; },
    showReserveRefundAction() { return this.currentId === 'adminReserve'; },
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
      if (this.currentId === 'adminReports') return ['全部狀態', '已完成', '產生中', '產生失敗'];
      if (this.currentId === 'adminAccounts') return ['全部狀態', '啟用', '停用'];
      return ['全部狀態', '正常', '已完成', '待處理', '逾期'];
    }
  },
  methods: {
    triggerBuildingPropertyAction() { this.page.adminBuildingPropertyCreateNonce += 1; },
    projectOptionKey(project, index) {
      if (!project || typeof project !== 'object') return project;
      return project.id ?? project.projectId ?? project.value ?? project.name ?? project.projectName ?? index;
    },
    optionLabel(value) {
      if (value === null || value === undefined) return '';
      if (typeof value === 'object') return value.name ?? value.projectName ?? value.label ?? value.code ?? '—';
      const key = {
        '全部建案': 'allProjects',
        '全部狀態': 'allStatus',
        '全部提醒': 'allReminders',
        '全部批次': 'allBatches',
        '啟用': 'enabled',
        '停用': 'disabled',
        '待發送': 'pending',
        '發送失敗': 'failed',
        '全部狀態': 'finance.allStatuses',
        '全部歷史': 'finance.allHistory',
        '已確認': 'finance.confirmed',
        '已退回': 'finance.rejected',
        '全部租金狀態': 'finance.allRentStatuses',
        '未支付': 'finance.unpaid',
        '部分支付': 'finance.partiallyPaid',
        '已逾期': 'finance.overdue',
        '全部出租狀態': 'properties.allRentalStatuses',
        '未交房': 'properties.preHandover',
        '已交房待出租': 'properties.pendingRental',
        '出租中': 'properties.rented',
        '未啟用出租': 'properties.notForRent'
      }[value];
      return key ? this.$t(key.includes('.') ? key : `ui.${key}`) : value;
    }
  }
};
</script>
