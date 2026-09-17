import { modules, headers, rows, featureMap, workflowMap, reminderMap, reportTabConfigs } from '../data/dashboardData';
import { fetchOwnerDashboard } from '../services/propertyApi';
import { resolveRoute } from '../router';

const moneyText = value => Number(value || 0).toLocaleString('en-US', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2
});

const emptyOwnerDashboard = () => ({
  summary: {
    propertyCount: 0,
    monthlyRentIncome: 0,
    unpaidPropertyAmount: 0,
    reserveBalance: 0
  },
  properties: [],
  notifications: [],
  pendingItems: []
});

const propertyToRow = property => [
  property.projectName || '-',
  property.unitNo || '-',
  property.unitType || '-',
  property.areaSqm == null ? '-' : `${property.areaSqm} m²`,
  property.assetStage === 'OPERATING' ? '出租中' : property.assetStage === 'DISPOSED' ? '已處置' : '未交房',
  (property.services || []).map(service => ({ RENTAL: '代租', RESALE: '代售', MANAGEMENT: '代管' })[service] || service).join('、') || '-',
  moneyText(property.purchasePrice),
  moneyText(property.paidAmount),
  moneyText(property.remainingAmount),
  property.assetStage === 'PRE_HANDOVER' ? (property.paymentStatus || '-') : '不適用'
];

export default {
  data() {
    const currentYear = new Date().getFullYear();
    const route = resolveRoute();
    const systemMode = route.mode === 'admin' ? 'admin' : 'owner';
    const defaultPage = route.moduleId || (systemMode === 'admin' ? 'adminSmartDashboard' : 'myProperties');
    const startPage = route.moduleId;
    const requestedModule = modules.find(module => module.id === startPage);
    const validStartPage = requestedModule && requestedModule.shell === `${systemMode}-shell` ? startPage : defaultPage;
    return {
      currentId: validStartPage,
      selectedIndex: 0,
      activeStep: 1,
      viewMode: "list",
      ownerPage: 1,
      ownerPageSize: 10,
      activeReportKey: "propertyPayment",
      tenantStatusTab: "全部",
      financeStatusTab: "全部",
      modalMode: "form",
      modal: null,
      dateStart: `${currentYear}-01-01`,
      dateEnd: `${currentYear}-12-31`,
      dateDraftStart: `${currentYear}-01-01`,
      dateDraftEnd: `${currentYear}-12-31`,
      dateFilterError: '',
      datePreset: "year",
      dateScope: "目前頁面",
      dateNote: "依照選定日期範圍篩選目前頁面資料，並同步套用到報表導出條件。",
      moduleSearch: "",
      globalSearch: "",
      projectFilter: "全部項目",
      statusFilter: "全部狀態",
      modalTitle: "新增資料",
      toastText: "已完成",
      toastVisible: false,
      databaseLoading: false,
      databaseError: '',
      adminOwnerProjects: [],
      adminOwnerMetrics: null,
      adminOwnerWorkspaceTab: 'owners',
      adminDataMetrics: null,
      adminBuildingProjects: [],
      adminFinanceMetrics: null,
      adminMaintenanceMetrics: null,
      adminMaintenanceProjects: [],
      adminMaintenanceDistrictFilter: '',
      adminMaintenanceExportHeaders: [],
      adminMaintenanceExportRows: [],
      adminMaintenanceExportFileName: '支出记录-export.csv',
      adminReserveMetrics: null,
      adminReserveProjects: [],
      adminReserveExportHeaders: [],
      adminReserveExportRows: [],
      adminReminderMetrics: null,
      adminReminderExportHeaders: [],
      adminReminderExportRows: [],
      adminReportMetrics: null,
      adminReportProjects: [],
      adminReportExportHeaders: [],
      adminReportExportRows: [],
      adminFinanceProjects: [],
      adminFinanceMode: 'property',
      adminFinanceViewMode: 'pending',
      adminFinanceTargetInvoiceId: null,
      adminTenancyMetrics: null,
      adminTenancyProjects: [],
      adminTenancyExportHeaders: [],
      adminTenancyExportRows: [],
      adminRentalMandateMetrics: null,
      adminRentalMandateProjects: [],
      adminRentalMandateExportHeaders: ['委託編號', '業主', '建案', '單位', '狀態'],
      adminRentalMandateExportRows: [],
      adminOwnerExportHeaders: [],
      adminOwnerExportRows: [],
      adminOwnerCreateNonce: 0,
      adminPropertyCreateNonce: 0,
      adminAccountCreateNonce: 0,
      adminBuildingProjectCreateNonce: 0,
      adminBuildingPropertyCreateNonce: 0,
      adminPaymentPlanCreateNonce: 0,
      adminFinanceRefreshNonce: 0,
      adminFinanceBatchNonce: 0,
      adminRentProofUploadNonce: 0,
      adminRentCollectionNonce: 0,
      adminExpenseCreateNonce: 0,
      adminExpenseCreateMode: '',
      adminMaintenanceCreateNonce: 0,
      adminMaintenanceMonthlyCashflowNonce: 0,
      adminReserveRefreshNonce: 0,
      adminReserveSettingsNonce: 0,
      adminReserveDirectTopupNonce: 0,
      adminReserveRefundNonce: 0,
      adminReminderCreateNonce: 0,
      adminReminderRunNonce: 0,
      adminReminderOpenNotificationsNonce: 0,
      adminReportCreateNonce: 0,
      adminReportRefreshNonce: 0,
      adminTenantCreateNonce: 0,
      adminRentalMandateCreateNonce: 0,
      adminRentalMandateRefreshNonce: 0,
      adminLeaseCreateNonce: 0,
      ownerNotificationUnreadCount: 0,
      adminAlertCount: 0,
      alertLoading: false,
      alertError: '',
      ownerDashboard: emptyOwnerDashboard(),
      form: { name: "Tan Wei Ming", phone: "+60 12-345 6789", project: "Pavilion Square", status: "正常", channel: "Email", dueDate: "2025-06-15", note: "依照目前頁面建立對應資料。" },
      alertItems: [],
      modules, headers, rows, featureMap, workflowMap, reminderMap, reportTabConfigs
    };
  },
  watch: {
    authenticated(isAuthenticated) {
      if (isAuthenticated && this.roleMode(this.currentUser) === 'owner') this.loadOwnerDashboard();
    },
    currentId() {
      this.selectedIndex = 0;
      this.activeStep = 1;
      this.viewMode = "list";
      this.ownerPage = 1;
      this.moduleSearch = "";
      this.globalSearch = "";
      this.statusFilter = "全部狀態";
      if (this.currentId === 'myProperties' && this.authenticated) this.loadOwnerDashboard();
      this.$nextTick(this.setupReportTabs);
    },
    filteredRows() {
      if (this.ownerPage > this.ownerTotalPages) this.ownerPage = this.ownerTotalPages;
      if (this.selectedIndex >= this.filteredRows.length) this.selectedIndex = 0;
    },
    moduleSearch() { this.ownerPage = 1; },
    projectFilter() { this.ownerPage = 1; },
    statusFilter() { this.ownerPage = 1; },
    ownerPageSize() { this.ownerPage = 1; },
    tenantStatusTab() { this.ownerPage = 1; },
    financeStatusTab() { this.ownerPage = 1; },
    globalSearch() {
      this.$nextTick(this.renderReportTab);
    }
  },
  mounted() {
    this.$nextTick(this.setupReportTabs);
  },
  methods: {
    async loadOwnerDashboard() {
      this.databaseLoading = true;
      this.databaseError = '';
      this.rows.myProperties = [];
      try {
        const dashboard = await fetchOwnerDashboard();
        this.ownerDashboard = {
          ...emptyOwnerDashboard(),
          ...dashboard,
          summary: { ...emptyOwnerDashboard().summary, ...(dashboard.summary || {}) },
          properties: dashboard.properties || [],
          notifications: dashboard.notifications || [],
          pendingItems: dashboard.pendingItems || []
        };
        const properties = this.ownerDashboard.properties;
        this.rows.myProperties = properties.map(propertyToRow);
        const propertyModule = this.modules.find(module => module.id === 'myProperties');
        if (propertyModule) {
          propertyModule.cards = properties.slice(0, 3).map(property => ({
            label: property.projectName || '未設定建案',
            title: property.unitNo || '未設定單位',
            text: property.assetStage === 'OPERATING'
              ? (property.services || []).map(service => ({ RENTAL: '代租', RESALE: '代售', MANAGEMENT: '代管' })[service] || service).join('、') || '尚未啟用服務'
              : property.unitType || '房型未設定',
            value: property.assetStage === 'OPERATING' ? '出租中' : property.assetStage === 'DISPOSED' ? '已處置' : property.paymentStatus || '未設定'
          }));
          propertyModule.detailBlocks = properties[0] ? [
            { title: '房產資訊', items: [
              { label: '建案', value: properties[0].projectName || '-' },
              { label: '城市', value: properties[0].city || '-' },
              { label: '面積', value: properties[0].areaSqm == null ? '-' : `${properties[0].areaSqm} m²` },
              { label: '房產階段', value: properties[0].assetStage === 'OPERATING' ? '出租中' : properties[0].assetStage === 'DISPOSED' ? '已處置' : '未交房' }
            ] },
            { title: '狀態摘要', items: [
              { label: '房型', value: properties[0].unitType || '-' },
              { label: '房產總價', value: moneyText(properties[0].purchasePrice) },
              { label: properties[0].assetStage === 'PRE_HANDOVER' ? '付款狀態' : '營運服務', value: properties[0].assetStage === 'PRE_HANDOVER'
                ? properties[0].paymentStatus || '-'
                : (properties[0].services || []).map(service => ({ RENTAL: '代租', RESALE: '代售', MANAGEMENT: '代管' })[service] || service).join('、') || '尚未啟用' }
            ] }
          ] : [];
        }
      } catch (error) {
        this.ownerDashboard = emptyOwnerDashboard();
        this.databaseError = error.message;
        this.showToast('房產資料載入失敗');
      } finally {
        this.databaseLoading = false;
      }
    }
  }
};
