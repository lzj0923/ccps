import { toNumber } from '../utils/dashboardFormatters';
import { downloadCsv, downloadPaymentReport } from '../utils/csvExporter';

import { navigate, resolveRoute, routeForModule } from '../router';
import { fetchAdminReminders } from '../services/propertyApi';
import { translateLegacyText } from '../i18n';

const validateDateRange = (startDate, endDate) => Boolean(startDate && endDate && startDate <= endDate);

export default {
  methods: {
    openDatePanel() {
      this.modalMode = "date";
      this.modalTitle = this.$t('common.dateFilter');
      this.dateDraftStart = this.dateStart;
      this.dateDraftEnd = this.dateEnd;
      this.dateFilterError = '';
      if (this.modal?.open) this.modal.close();
      this.$nextTick(() => this.modal?.showModal());
    },
    openAlertPanel() {
      this.modalMode = "alerts";
      this.modalTitle = this.$t('common.notificationCenter');
      if (this.modal?.open) this.modal.close();
      this.$nextTick(() => this.modal?.showModal());
      this.refreshAdminAlerts();
    },
    async refreshAdminAlerts() {
      if (resolveRoute().mode !== 'admin' || this.alertLoading) return;
      this.alertLoading = true;
      this.alertError = '';
      try {
        const data = await fetchAdminReminders();
        const summary = data?.summary || {};
        this.adminAlertCount = Number(summary.pendingDeliveryCount || 0) + Number(summary.failedDeliveryCount || 0);
        this.alertItems = (Array.isArray(data?.notifications) ? data.notifications : []).slice(0, 12).map(item => {
          const delivery = String(item.deliverySummary || '');
          const failed = Boolean(item.failureReason) || delivery.includes(':failed');
          const pending = delivery.includes(':pending') || delivery.includes(':sending');
          const status = failed ? this.$t('common.deliveryFailed') : pending ? this.$t('common.pendingDelivery') : this.$t('common.sent');
          return {
            id: item.id,
            title: item.title || this.$t('common.systemNotification'),
            detail: item.body || item.ruleName || '',
            status,
            tone: failed ? 'red' : pending ? 'orange' : 'green',
            time: item.createdAt ? String(item.createdAt).replace('T', ' ').slice(0, 16) : '',
            relatedType: item.relatedType,
            relatedId: item.relatedId
          };
        });
      } catch (error) {
        this.alertItems = [];
        this.adminAlertCount = 0;
        this.alertError = error.message || this.$t('common.notificationLoadFailed');
      } finally {
        this.alertLoading = false;
      }
    },
    triggerPrimaryAction() {
      if (this.currentId === 'adminOwners' && this.adminOwnerWorkspaceTab === 'accounts') {
        this.adminAccountCreateNonce += 1;
        return;
      }
      if (this.currentId === 'adminOwners') {
        this.adminOwnerCreateNonce += 1;
        return;
      }
      if (this.currentId === 'adminProperties') {
        this.adminPropertyCreateNonce += 1;
        return;
      }
      if (this.currentId === 'adminAccounts') {
        this.adminAccountCreateNonce += 1;
        return;
      }
      if (this.currentId === 'adminData') {
        this.adminBuildingProjectCreateNonce += 1;
        return;
      }
      if (this.currentId === 'adminFinance') {
        if (this.adminFinanceViewMode === 'history') {
          this.adminFinanceRefreshNonce += 1;
          return;
        }
        if (this.adminFinanceMode === 'rent') {
          this.adminRentCollectionNonce += 1;
          return;
        }
        this.adminFinanceRefreshNonce += 1;
        return;
      }
      if (this.currentId === 'adminRentalMandates') { this.adminRentalMandateCreateNonce += 1; return; }
      if (this.currentId === 'adminTenants') {
        this.adminTenantCreateNonce += 1;
        return;
      }
      if (this.currentId === 'adminMaintenance') {
        this.adminExpenseCreateMode = '';
        this.adminExpenseCreateNonce += 1;
        return;
      }
      if (this.currentId === 'adminReserve') {
        this.adminReserveDirectTopupNonce += 1;
        return;
      }
      if (this.currentId === 'adminAlerts') {
        this.adminReminderCreateNonce += 1;
        return;
      }
      if (this.currentId === 'adminReports') { this.adminReportCreateNonce += 1; return; }
      this.openModal(this.currentModule.primaryAction);
    },
    triggerSecondaryAction() {
      if (this.currentId === 'adminOwners' && this.adminOwnerWorkspaceTab === 'accounts') {
        this.showToast('帳號可在列表中修改、停用或恢復');
        return;
      }
      if (this.currentId === 'adminOwners') {
        this.adminPropertyCreateNonce += 1;
        return;
      }
      if (this.currentId === 'adminAccounts') {
        this.showToast('帳號可在列表中修改、停用或刪除');
        return;
      }
      if (this.currentId === 'adminData') {
        this.adminPaymentPlanCreateNonce += 1;
        return;
      }
      if (this.currentId === 'adminFinance') {
        this.adminFinanceBatchNonce += 1;
        return;
      }
      if (this.currentId === 'adminRentalMandates') { this.adminRentalMandateRefreshNonce += 1; return; }
      if (this.currentId === 'adminTenants') {
        this.adminLeaseCreateNonce += 1;
        return;
      }
      if (this.currentId === 'adminMaintenance') {
        this.adminMaintenanceCreateNonce += 1;
        return;
      }
      if (this.currentId === 'adminReserve') {
        this.adminExpenseCreateMode = 'reserve-debit';
        this.selectModule('adminMaintenance');
        this.$nextTick(() => { this.adminExpenseCreateNonce += 1; });
        return;
      }
      if (this.currentId === 'adminAlerts') {
        this.adminReminderRunNonce += 1;
        return;
      }
      if (this.currentId === 'adminReports') { this.adminReportRefreshNonce += 1; return; }
      this.openModal(this.currentModule.secondaryAction);
    },
    triggerReserveRefundAction() {
      if (this.currentId === 'adminReserve') this.adminReserveRefundNonce += 1;
    },
    applyDatePreset() {
      const year = new Date().getFullYear();
      const month = String(new Date().getMonth() + 1).padStart(2, '0');
      const monthEnd = new Date(year, new Date().getMonth() + 1, 0).getDate();
      const presets = {
        year: [`${year}-01-01`, `${year}-12-31`],
        h1: [`${year}-01-01`, `${year}-06-30`],
        h2: [`${year}-07-01`, `${year}-12-31`],
        month: [`${year}-${month}-01`, `${year}-${month}-${monthEnd}`]
      };
      const range = presets[this.datePreset];
      if (!range) return;
      this.dateDraftStart = range[0];
      this.dateDraftEnd = range[1];
      this.dateFilterError = '';
    },
    confirmModal() {
      if (this.modalMode === "date") {
        if (!validateDateRange(this.dateDraftStart, this.dateDraftEnd)) {
          this.dateFilterError = this.$t('common.dateRangeInvalid');
          return;
        }
        this.dateStart = this.dateDraftStart;
        this.dateEnd = this.dateDraftEnd;
        this.dateFilterError = '';
        this.modal?.close();
        this.showToast(this.$t('common.dateApplied', { range: this.dateRange }));
        this.$nextTick(this.renderReportTab);
        return;
      }
      if (this.modalMode === "alerts") {
        this.modal?.close();
        this.globalSearch = '';
        this.adminReminderOpenNotificationsNonce += 1;
        this.selectModule('adminAlerts');
        return;
      }
      if (this.modalMode === "upload") {
        this.showToast("單據已上傳，等待財務確認");
        return;
      }
      this.showToast("資料已儲存");
    },
    selectAlert(item) {
      this.modal?.close();
      this.adminReminderOpenNotificationsNonce += 1;
      this.selectModule('adminAlerts');
      this.$nextTick(() => { this.globalSearch = item.title || ''; });
    },
    ownerMetricIcon(icon) {
      const icons = {
        building: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 20V4h11v16M3 20h18M8 8h1M12 8h1M8 12h1M12 12h1M8 16h1M12 16h1M16 20v-6h3v6"/></svg>',
        coins: '<svg viewBox="0 0 24 24" aria-hidden="true"><ellipse cx="10" cy="6" rx="6" ry="3"/><path d="M4 6v5c0 1.7 2.7 3 6 3s6-1.3 6-3V6M4 11v5c0 1.7 2.7 3 6 3 1.2 0 2.3-.2 3.8-.7"/><path d="M16 10c2.2.3 4 1.3 4 3v5c0 1.7-2.7 3-6 3-1.3 0-2.4-.2-3.4-.6"/></svg>',
        receipt: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 3h12v18l-2-1.5L14 21l-2-1.5L10 21l-2-1.5L6 21V3Z"/><path d="M9 8h6M9 12h6M9 16h3"/></svg>',
        shield: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3 5 6v5c0 4.5 2.8 7.8 7 10 4.2-2.2 7-5.5 7-10V6l-7-3Z"/><path d="m9 12 2 2 4-4"/></svg>'
      };
      return icons[icon] || icons.building;
    },
    metricIcon(icon) {
      const icons = {
        "人": '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M16 20v-1.6c0-1.8-1.4-3.2-3.2-3.2H7.2C5.4 15.2 4 16.6 4 18.4V20"/><circle cx="10" cy="8" r="3.2"/><path d="M20 20v-1.4c0-1.5-.9-2.7-2.2-3.1"/><path d="M15.8 5.1a3 3 0 0 1 0 5.8"/></svg>',
        "房": '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3.5 11.3 12 4l8.5 7.3"/><path d="M5.8 10.2V20h12.4v-9.8"/><path d="M9.7 20v-5.4h4.6V20"/></svg>',
        "樓": '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 20V4h12v16"/><path d="M9 8h.1M12 8h.1M15 8h.1M9 12h.1M12 12h.1M15 12h.1M9 16h.1M12 16h.1M15 16h.1"/><path d="M4 20h16"/></svg>',
        "屋": '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 11.5 12 5l8 6.5"/><path d="M6 10.5V20h12v-9.5"/><path d="M9 20v-5h6v5"/><path d="M9 9h6"/> </svg>',
        "租": '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 20V9l8-5 8 5v11"/><path d="M8 20v-6h8v6"/><path d="M9 10h6"/></svg>',
        "款": '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="8.5"/><path d="M12 7.3v9.4M15 9.2c-.7-.7-1.6-1.1-2.8-1.1-1.3 0-2.2.7-2.2 1.8 0 2.6 5.2 1.3 5.2 4.2 0 1.1-.9 1.9-2.5 1.9-1.3 0-2.4-.5-3.2-1.3"/></svg>',
        "✓": '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="8.5"/><path d="m8.5 12.2 2.2 2.2 4.8-5.1"/></svg>',
        "!": '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 4 3.5 19h17L12 4Z"/><path d="M12 9v4.5"/><path d="M12 16.6h.01"/></svg>',
        "期": '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="4" y="5" width="16" height="15" rx="2"/><path d="M8 3v4M16 3v4M4 10h16M8 14h2M12 14h2M16 14h1M8 17h2M12 17h2"/></svg>',
        "$": '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="8.5"/><path d="M12 7v10M15 9c-.6-.6-1.5-1-2.6-1-1.3 0-2.2.6-2.2 1.6 0 2.5 5 1.2 5 4 0 1-.9 1.8-2.5 1.8-1.2 0-2.3-.4-3.1-1.2"/></svg>',
        "日": '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="5" y="4" width="14" height="16" rx="2"/><path d="M8 8h8M8 12h8M8 16h5"/></svg>',
        "部": '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 7h14M5 12h10M5 17h7"/><circle cx="18" cy="17" r="2"/></svg>',
        "逾": '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="8.5"/><path d="M12 7v5l3 2"/></svg>',
        "財": '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 7h14v12H5z"/><path d="M8 7V5h8v2M8 12h8M8 15h5"/></svg>'
      };
      Object.assign(icons, {
        "收": '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 19h16"/><path d="M7 16V9"/><path d="M12 16V5"/><path d="M17 16v-4"/><path d="m6 8 3-3 3 3 5-5"/></svg>',
        "支": '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 3h12v18l-3-1.8-3 1.8-3-1.8L6 21V3Z"/><path d="M9 8h6M9 12h6M9 16h4"/></svg>',
        "修": '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m14.7 6.3 3-3a4 4 0 0 1-5 5l-7.4 7.4a2 2 0 1 0 3 3l7.4-7.4a4 4 0 0 1 5-5l-3 3"/></svg>',
        "水": '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3S6.5 9.2 6.5 14a5.5 5.5 0 0 0 11 0C17.5 9.2 12 3 12 3Z"/><path d="M9.5 15.5c.6 1.1 1.5 1.7 2.7 1.7"/></svg>',
        "管": '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M9 4h6l1 2h3v14H5V6h3l1-2Z"/><path d="M9 11h6M9 15h4"/></svg>',
        "待": '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="8.5"/><path d="M12 7v5l3 2"/></svg>',
        "餘": '<svg viewBox="0 0 24 24" aria-hidden="true"><ellipse cx="12" cy="6" rx="6" ry="3"/><path d="M6 6v5c0 1.7 2.7 3 6 3s6-1.3 6-3V6"/><path d="M6 11v5c0 1.7 2.7 3 6 3s6-1.3 6-3v-5"/></svg>',
        "錢": '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 7h16v11H4z"/><path d="M7 10h4"/><circle cx="16" cy="12.5" r="2.2"/></svg>',
        "戶": '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="8" r="3"/><path d="M5 19c.8-3.2 3.1-5 7-5s6.2 1.8 7 5"/></svg>',
        "入": '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="8.5"/><path d="M12 16V7"/><path d="m8.5 10.5 3.5-3.5 3.5 3.5"/></svg>',
        "出": '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="8.5"/><path d="M12 8v9"/><path d="m8.5 13.5 3.5 3.5 3.5-3.5"/></svg>',
        "盾": '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3 5.5 5.5v5.8c0 4.1 2.6 7.5 6.5 9.2 3.9-1.7 6.5-5.1 6.5-9.2V5.5L12 3Z"/><path d="m9 12 2 2 4-5"/></svg>'
        ,"鏈": '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M10 13a5 5 0 0 0 7.1 0l1.4-1.4a5 5 0 0 0-7.1-7.1L10.5 5"/><path d="M14 11a5 5 0 0 0-7.1 0l-1.4 1.4a5 5 0 0 0 7.1 7.1l.9-.9"/></svg>',
        "同": '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 7h12l-3-3"/><path d="M20 17H8l3 3"/><path d="M16 4l3 3-3 3M8 14l-3 3 3 3"/></svg>',
        "批": '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 4h11l3 3v13H6z"/><path d="M17 4v4h4"/><path d="M9 12h7M9 16h5"/></svg>',
        "成": '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="8.5"/><path d="m8.5 12.5 2.2 2.2 4.8-5.2"/></svg>',
        "敗": '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 4 3.5 19h17L12 4Z"/><path d="M12 9v4.5M12 16.5h.01"/></svg>',
        "時": '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="8.5"/><path d="M12 7v5l3 2"/></svg>'
      });
      return icons[icon] || `<span>${icon}</span>`;
    },
    selectReportTab(key) {
      this.activeReportKey = key;
      this.$nextTick(this.renderReportTab);
    },
    setupReportTabs() {
      if (this.currentId !== "adminReports") return;
      const tabBar = document.querySelector(".report-tabs");
      if (!tabBar) return;
      const buttons = Array.from(tabBar.querySelectorAll("button"));
      this.reportTabConfigs.forEach((tab, index) => {
        const button = buttons[index];
        if (!button) return;
        button.type = "button";
        button.textContent = tab.label;
        button.onclick = () => this.selectReportTab(tab.key);
      });
      this.renderReportTab();
    },
    renderReportTab() {
      if (this.currentId !== "adminReports" || !this.currentReport) return;
      const tabBar = document.querySelector(".report-tabs");
      if (tabBar) {
        Array.from(tabBar.querySelectorAll("button")).forEach((button, index) => {
          button.classList.toggle("active", this.reportTabConfigs[index]?.key === this.activeReportKey);
        });
      }

      const chartTitles = document.querySelectorAll(".report-charts .chart-card h3");
      if (chartTitles[0]) chartTitles[0].textContent = this.currentReport.chartTitle;
      if (chartTitles[1]) chartTitles[1].textContent = this.currentReport.splitTitle;

      const bars = document.querySelectorAll(".bar-chart i");
      this.currentReport.bars.forEach((height, index) => {
        if (bars[index]) bars[index].style.height = `${height}%`;
      });

      const donutLabel = document.querySelector(".donut span");
      if (donutLabel) donutLabel.textContent = this.currentReport.unitLabel;

      const legendItems = document.querySelectorAll(".donut-layout li");
      this.currentReport.legend.forEach((item, index) => {
        const row = legendItems[index];
        if (!row) return;
        row.innerHTML = `<b class="${item[0]}"></b><span>${item[1]}</span><strong>${item[2]}</strong>`;
      });

      const blocks = document.querySelectorAll(".export-block");
      if (blocks[0]) {
        const label = blocks[0].querySelector("strong");
        if (label) label.textContent = this.currentReport.label;
      }
      if (blocks[1]) {
        const title = blocks[1].querySelector("span");
        const text = blocks[1].querySelector("p");
        if (title) title.textContent = "已選擇字段（共 12 項）";
        if (text) text.textContent = this.currentReport.fields;
      }
      if (blocks[2]) {
        const title = blocks[2].querySelector("span");
        const text = blocks[2].querySelector("p");
        if (title) title.textContent = "篩選條件";
        if (text) text.innerHTML = this.currentReport.filters.join("<br>");
      }
      const metaRows = document.querySelectorAll(".export-meta div");
      if (metaRows[0]) {
        const label = metaRows[0].querySelector("span");
        const value = metaRows[0].querySelector("b");
        if (label) label.textContent = "預計記錄數";
        if (value) value.textContent = `${this.currentReport.rows.length * 18 + 46} 條`;
      }
      if (metaRows[1]) {
        const label = metaRows[1].querySelector("span");
        const value = metaRows[1].querySelector("b");
        if (label) label.textContent = "上次導出時間";
        if (value) value.textContent = "2025-06-15 10:30:22";
      }
      const pagerCount = document.querySelector(".report-pager > span");
      if (pagerCount) pagerCount.textContent = `共 ${this.displayedReportRows.length} 條記錄`;
    },
    selectModule(id) {
      const systemMode = resolveRoute().mode === 'admin' ? 'admin' : 'owner';
      const targetMode = String(id).startsWith('admin') ? 'admin' : 'owner';
      if (targetMode !== systemMode) return;
      this.currentId = id;
      navigate(routeForModule(id));
    },
    initials(name) { return String(name).split(/\s+/).filter(Boolean).slice(0, 2).map(part => part[0]).join("").toUpperCase() || "CC"; },
    toNumber(value) { return toNumber(value); },
    isMoney(value) { return /^\d{1,3}(,\d{3})*\.\d{2}$/.test(String(value)); },
    isStatus(value) { return ["正常", "已完成", "待確認", "待處理", "不足", "逾期", "啟用", "可下載", "等待", "處理中", "出租中", "提醒中", "待補件", "即將到期", "部分付款", "待付款", "已確認", "未確認", "已同步", "待同步", "未同步", "待財務確認", "已付款", "部分收款", "未付款", "同步失敗"].includes(String(value)); },
    tagClass(value) {
      if (["正常", "已完成", "已確認", "已付款", "已同步", "啟用", "可下載", "出租中", "餘額正常", "同步成功"].includes(value)) return "green";
      if (["部分完成", "待確認", "待處理", "等待", "處理中", "提醒中", "待補件", "即將到期", "部分付款", "部分收款", "待同步", "未同步", "未確認", "待財務確認", "同步中", "低於標準"].includes(value)) return "orange";
      if (["不足", "逾期", "已逾期", "未付款", "待付款", "同步失敗"].includes(value)) return "red";
      return "gray";
    },
    moneyClass(cell, row) {
      if (!this.isMoney(cell)) return "";
      if (cell === "0.00") return "money-green";
      return row.some(value => ["待確認", "待處理", "不足", "逾期"].includes(value)) ? "money-red" : "";
    },
    openModal(title) {
      this.modalMode = "form";
      this.modalTitle = title;
      this.modal?.showModal();
    },
    openUploadModal() {
      this.modalMode = "upload";
      this.modalTitle = "上傳單據";
      this.modal?.showModal();
    },
    saveForm() { this.confirmModal(); },
    exportCsvLegacy() {
      const csvRows = [this.currentHeaders.slice(0, -1), ...this.filteredRows];
      const csv = csvRows.map(row => row.map(value => `"${String(value).replaceAll('"', '""')}"`).join(",")).join("\n");
      const blob = new Blob(["\ufeff" + csv], { type: "text/csv;charset=utf-8" });
      const link = document.createElement("a");
      link.href = URL.createObjectURL(blob);
      link.download = `${this.currentId}-export.csv`;
      link.click();
      URL.revokeObjectURL(link.href);
      this.showToast("CSV 已匯出");
    },
    exportCsv() {
      if (this.currentId === 'adminMaintenance' && this.adminMaintenanceExportHeaders?.length) {
        downloadPaymentReport(this.adminMaintenanceExportFileName || '收支与维修-export.csv', this.adminMaintenanceExportHeaders, this.adminMaintenanceExportRows || []);
        this.showToast('XLSX 已匯出');
        return;
      }
      if (this.currentId === 'adminReserve' && this.adminReserveExportHeaders?.length) {
        downloadCsv('adminReserve-export.csv', this.adminReserveExportHeaders, this.adminReserveExportRows || []);
        this.showToast('CSV 已匯出');
        return;
      }
      if (this.currentId === 'adminRentalMandates' && this.adminRentalMandateExportHeaders?.length) { downloadCsv('adminRentalMandates-export.csv', this.adminRentalMandateExportHeaders, this.adminRentalMandateExportRows || []); this.showToast('CSV 已匯出'); return; }
      if (this.currentId === 'adminTenants' && this.adminTenancyExportHeaders?.length) {
        downloadCsv('adminTenants-export.csv', this.adminTenancyExportHeaders, this.adminTenancyExportRows || []);
        this.showToast('CSV 已匯出');
        return;
      }
      if (this.currentId === 'adminAlerts' && this.adminReminderExportHeaders?.length) {
        downloadCsv('adminAlerts-export.csv', this.adminReminderExportHeaders, this.adminReminderExportRows || []);
        this.showToast('CSV 已匯出');
        return;
      }
      if (this.currentId === 'adminReports' && this.adminReportExportHeaders?.length) { downloadCsv('adminReports-export.csv', this.adminReportExportHeaders, this.adminReportExportRows || []); this.showToast('CSV 已匯出'); return; }
      const isAdminOwnerExport = this.currentId === 'adminOwners' || this.currentId === 'adminProperties';
      const headers = isAdminOwnerExport ? this.adminOwnerExportHeaders : this.currentHeaders.slice(0, -1);
      const rows = isAdminOwnerExport ? this.adminOwnerExportRows : this.filteredRows;
      downloadCsv(`${this.currentId}-export.csv`, headers, rows);
      this.showToast('CSV 已匯出');
    },

    showToast(text) {
      this.toastText = translateLegacyText(text);
      this.toastVisible = true;
      window.setTimeout(() => { this.toastVisible = false; }, 1800);
    }
  }
};
