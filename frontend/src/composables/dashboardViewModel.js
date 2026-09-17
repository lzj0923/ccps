import { adminKpis } from '../data/dashboardData';
import { moneyText, toNumber } from '../utils/dashboardFormatters';
import { canAccessAdminModule, canManageAdminModule } from '../utils/adminPermissions';
import { localizeLegacyTree } from '../i18n';

const ADMIN_NAV_GROUPS = [
  { id: 'assets', labelKey: 'navigation.assets', moduleIds: ['adminProjects', 'adminOwners', 'adminProperties', 'adminData'] },
  { id: 'rental', labelKey: 'navigation.rental', moduleIds: ['adminProcess', 'adminRentalSigning', 'adminDeposits', 'adminTenantDirectory', 'adminTenants', 'adminRentalMandates', 'adminOffMarketProperties'] },
  { id: 'finance', labelKey: 'navigation.finance', moduleIds: ['adminMaintenance', 'adminFinance', 'adminReserve'] },
  { id: 'operations', labelKey: 'navigation.operations', moduleIds: ['adminAlerts', 'adminReports'] },
  { id: 'system', labelKey: 'navigation.system', moduleIds: ['adminAccounts', 'adminAudit', 'adminSystemBackup'] }
];

export default {
  computed: {
    currentModule() { return localizeLegacyTree(this.modules.find(module => module.id === this.currentId)); },
    ownerModules() { return this.modules.filter(module => module.shell === "owner-shell").sort((a, b) => Number(a.code) - Number(b.code)); },
    adminPrimaryModules() {
      const order = ['adminSmartDashboard', 'adminDashboard'];
      return this.modules
        .filter(module => module.shell === 'admin-shell' && order.includes(module.id) && canAccessAdminModule(this.currentUser, module.id))
        .sort((a, b) => order.indexOf(a.id) - order.indexOf(b.id));
    },
    adminNavGroups() {
      const modulesById = new Map(this.modules.map(module => [module.id, module]));
      return ADMIN_NAV_GROUPS.map(group => ({
        ...group,
        modules: group.moduleIds.map(id => modulesById.get(id)).filter(module => module?.shell === 'admin-shell' && canAccessAdminModule(this.currentUser, module.id))
      })).filter(group => group.modules.length);
    },
    adminModules() {
      return [...this.adminPrimaryModules, ...this.adminNavGroups.flatMap(group => group.modules)];
    },
    canManageCurrentAdminModule() { return this.currentModule?.shell !== 'admin-shell' || canManageAdminModule(this.currentUser, this.currentId); },
    currentHeaders() { return this.headers[this.currentId]; },
    currentRows() { return this.rows[this.currentId]; },
    currentReport() { return this.reportTabConfigs.find(tab => tab.key === this.activeReportKey) || this.reportTabConfigs[0]; },
    dateRange() { return `${this.dateStart} ~ ${this.dateEnd}`; },
    modalConfirmText() {
      if (this.modalMode === "date") return this.$t('common.applyFilter');
      if (this.modalMode === "alerts") return this.$t('common.viewAllNotifications');
      if (this.modalMode === "upload") return "確認上傳";
      return "保存";
    },
    displayedReportRows() {
      const keyword = this.globalSearch.trim().toLowerCase();
      return this.reportRows.filter(row => !keyword || row.join(" ").toLowerCase().includes(keyword));
    },
    reportRows() {
      if (this.currentReport) return this.currentReport.rows;
      return [
        ["房款繳費進度", "2026-01-01 ~ 2026-12-31", "Pavilion Square", "2,580,000.00", "已完成", "2026-06-15 10:30:22"],
        ["租金收款記錄", "2026-01-01 ~ 2026-12-31", "Pavilion Square", "1,450,000.00", "已完成", "2026-06-15 10:30:22"],
        ["收支明細", "2026-01-01 ~ 2026-12-31", "全部項目", "1,125,300.00", "已完成", "2026-06-15 10:30:22"],
        ["維修費用", "2026-01-01 ~ 2026-12-31", "全部項目", "185,600.00", "已完成", "2026-06-15 10:30:22"],
        ["預備金流水", "2026-01-01 ~ 2026-12-31", "全部項目", "2,500,000.00", "已完成", "2026-06-15 10:20:31"],
        ["財務確認記錄", "2026-01-01 ~ 2026-12-31", "全部項目", "-", "已完成", "2026-06-15 10:30:22"],
        ["SQL 同步記錄", "2026-01-01 ~ 2026-12-31", "全部項目", "-", "已完成", "2026-06-15 10:30:22"],
        ["房款繳費進度（逾期）", "2026-01-01 ~ 2026-12-31", "全部項目", "1,200,000.00", "部分完成", "2026-06-15 10:30:22"]
      ];
    },
    currentFeatures() { return this.featureMap[this.currentId].map(([code, title, items]) => ({ code, title, items })); },
    currentWorkflow() { return this.workflowMap[this.currentId]; },
    currentReminders() { return this.reminderMap[this.currentId]; },
    filteredRows() {
      const keyword = (this.globalSearch || this.moduleSearch).trim().toLowerCase();
      return (this.currentRows || []).filter(row => {
        const text = row.join(" ").toLowerCase();
        const matchesKeyword = !keyword || text.includes(keyword);
        const projectAll = String(this.projectFilter).includes("全部");
        const statusAll = String(this.statusFilter).includes("全部");
        const matchesProject = projectAll || row.includes(this.projectFilter);
        const matchesStatus = statusAll || row.includes(this.statusFilter);
        const matchesTenantTab = this.currentId !== "adminTenants" || this.tenantStatusTab === "全部" || row.includes(this.tenantStatusTab);
        const matchesFinanceTab = this.currentId !== "adminFinance" || this.financeStatusTab === "全部" || row.includes(this.financeStatusTab);
        return matchesKeyword && matchesProject && matchesStatus && matchesTenantTab && matchesFinanceTab;
      });
    },
    tenantTabCounts() {
      const sourceRows = this.rows.adminTenants || [];
      const count = label => sourceRows.filter(row => row.includes(label)).length;
      return [
        { label: "全部", count: sourceRows.length },
        { label: "待繳", count: count("待收") },
        { label: "部分收款", count: count("部分收款") },
        { label: "逾期", count: count("逾期") },
        { label: "待財務確認", count: count("待財務確認") },
        { label: "已收", count: count("已收") }
      ];
    },
    financeTabCounts() {
      const sourceRows = this.rows.adminFinance || [];
      const count = label => sourceRows.filter(row => row.includes(label)).length;
      return [
        { label: "全部", count: sourceRows.length },
        { label: "待確認", count: count("待確認") },
        { label: "部分收款", count: count("部分收款") },
        { label: "已確認", count: count("已確認") },
        { label: "未付款", count: count("未付款") },
        { label: "部分付款", count: count("部分付款") },
        { label: "已付款", count: count("已付款") },
        { label: "同步失敗", count: count("同步失敗") }
      ];
    },
    ownerTotalPages() { return Math.max(1, Math.ceil(this.filteredRows.length / this.ownerPageSize)); },
    ownerPageNumbers() { return Array.from({ length: this.ownerTotalPages }, (_, index) => index + 1); },
    pagedOwnerRows() {
      const start = (this.ownerPage - 1) * this.ownerPageSize;
      return this.filteredRows.slice(start, start + this.ownerPageSize);
    },
    selectedRow() { return this.filteredRows[this.selectedIndex] || this.filteredRows[0] || []; },
    currentMetrics() {
      if ((this.currentId === "adminOwners" || this.currentId === "adminProperties") && this.adminOwnerMetrics?.length) {
        return this.adminOwnerMetrics;
      }
      if (this.currentId === "adminData" && this.adminDataMetrics?.length) {
        return this.adminDataMetrics;
      }
      if (this.currentId === "adminFinance" && this.adminFinanceMetrics?.length) {
        return this.adminFinanceMetrics;
      }
      if (this.currentId === "adminMaintenance" && this.adminMaintenanceMetrics?.length) {
        return this.adminMaintenanceMetrics;
      }
      if (this.currentId === "adminReserve" && this.adminReserveMetrics?.length) {
        return this.adminReserveMetrics;
      }
      if (this.currentId === "adminAlerts" && this.adminReminderMetrics?.length) {
        return this.adminReminderMetrics;
      }
      if (this.currentId === "adminReports" && this.adminReportMetrics?.length) return this.adminReportMetrics;
      if (this.currentId === "adminTenants" && this.adminTenancyMetrics?.length) {
        return this.adminTenancyMetrics;
      }
      if (this.currentId === "adminRentalMandates" && this.adminRentalMandateMetrics?.length) {
        return this.adminRentalMandateMetrics;
      }
      if (this.currentModule.shell === "admin-shell" && adminKpis[this.currentId]) {
        return adminKpis[this.currentId].map(([icon, label, value, delta, trend]) => ({ icon, label, value, delta, trend }));
      }
      const pending = this.currentRows.filter(row => row.some(cell => ["待確認", "待處理", "不足", "逾期"].includes(cell))).length;
      const completed = this.currentRows.filter(row => row.some(cell => ["正常", "已完成", "啟用"].includes(cell))).length;
      return [
        { icon: "01", label: "總記錄", value: `${this.currentRows.length} 筆`, delta: "即時資料", trend: "up" },
        { icon: "02", label: "待處理", value: `${pending} 筆`, delta: pending ? "需要跟進" : "無待辦", trend: pending ? "down" : "up" },
        { icon: "03", label: "已完成/正常", value: `${completed} 筆`, delta: "可查詢", trend: "up" },
        { icon: "04", label: "提醒規則", value: `${this.currentReminders.length} 條`, delta: "已啟用", trend: "up" },
        { icon: "RM", label: "關聯金額", value: `RM ${moneyText(this.totalAmount)}`, delta: "頁面合計", trend: "up" }
      ];
    },
    ownerMetrics() {
      return this.currentMetrics.slice(0, 4);
    },
    ownerSummaryCards() {
      if (this.currentId === "myProperties") {
        return [
          { icon: "building", label: "名下房產數量", value: `${this.rows.myProperties.length}`, text: "全部單位" },
          { icon: "coins", label: "本月租金收入", value: "RM 0.00", text: "本月總收入" },
          { icon: "receipt", label: "未繳房款", value: "RM 0.00", text: "剩餘應繳金額" },
          { icon: "shield", label: "當前預備金餘額", value: "RM 0.00", text: "可用餘額" }
        ];
      }
      const cards = this.currentModule.cards || [];
      if (cards.length) {
        const mapped = cards.slice(0, 4).map((card, index) => ({
          icon: ["01", "02", "03", "04"][index] || "CC",
          label: card.label,
          value: card.title,
          text: card.text
        }));
        return [...mapped, ...this.ownerMetrics.slice(mapped.length)].slice(0, 4);
      }
      return this.ownerMetrics;
    },
    totalAmount() {
      return this.currentRows.reduce((sum, row) => sum + toNumber(row.find(cell => this.isMoney(cell)) || 0), 0);
    },
    detailName() { return this.selectedRow[0] || this.currentModule.name; },
    detailSubline() { return `${this.selectedRow[1] || "Pavilion Square"} · ${this.currentModule.name}`; },
    detailStatus() { return this.selectedRow.find(cell => this.isStatus(cell)) || "正常"; },
    detailTotal() { return this.selectedRow.find(cell => this.isMoney(cell)) || "0.00"; },
    detailPaid() {
      const amounts = this.selectedRow.filter(cell => this.isMoney(cell));
      return amounts[1] || amounts[0] || "0.00";
    },
    detailPending() {
      const amounts = this.selectedRow.filter(cell => this.isMoney(cell));
      if (amounts.length >= 3) return amounts[2];
      if (amounts.length >= 2) return moneyText(Math.max(toNumber(amounts[0]) - toNumber(amounts[1]), 0));
      return "0.00";
    },
    detailProgress() {
      const total = toNumber(this.detailTotal);
      const paid = toNumber(this.detailPaid);
      if (!total) return 60;
      return Math.min(100, Math.max(8, Math.round((paid / total) * 100)));
    },
    reserveProgress() {
      if (this.currentId !== "adminReserve") return 100;
      const balance = toNumber(this.selectedRow[2] || 0);
      const minimum = toNumber(this.selectedRow[3] || 1);
      if (!minimum) return 100;
      return Math.min(140, Math.max(0, Math.round((balance / minimum) * 100)));
    },
    reserveSurplus() {
      if (this.currentId !== "adminReserve") return "0.00";
      return moneyText(Math.max(toNumber(this.selectedRow[2] || 0) - toNumber(this.selectedRow[3] || 0), 0));
    },
    syncProgress() {
      return 100;
    },
    kanbanColumns() {
      return ["正常", "已完成", "啟用", "待確認", "待處理", "不足", "逾期"].map(name => ({
        name,
        rows: this.filteredRows.filter(row => row.includes(name)).slice(0, 5)
      })).filter(column => column.rows.length);
    }
  }
};
