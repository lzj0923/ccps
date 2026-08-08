const computedKeys = [
  'currentModule', 'currentId', 'adminModules', 'adminPrimaryModules', 'adminNavGroups', 'ownerModules', 'currentHeaders', 'currentRows',
  'currentReport', 'dateRange', 'modalConfirmText', 'filteredRows', 'currentReminders', 'pagedOwnerRows',
  'selectedRow', 'currentMetrics', 'ownerMetrics', 'ownerSummaryCards', 'detailName', 'detailSubline',
  'detailStatus', 'detailTotal', 'detailPaid', 'detailPending', 'detailProgress', 'kanbanColumns',
  'selectedIndex', 'activeStep', 'viewMode', 'ownerPage', 'ownerPageSize', 'modalMode', 'dateStart',
  'dateEnd', 'datePreset', 'dateScope', 'dateNote', 'moduleSearch', 'globalSearch', 'projectFilter',
  'statusFilter', 'modalTitle', 'toastText', 'toastVisible', 'form', 'alertItems'
];

const methodKeys = [
  'selectModule', 'initials', 'toNumber', 'isMoney', 'isStatus', 'tagClass', 'moneyClass', 'ownerMetricIcon', 'openModal',
  'openDatePanel', 'openAlertPanel', 'triggerPrimaryAction', 'triggerSecondaryAction', 'applyDatePreset', 'confirmModal', 'selectAlert', 'metricIcon',
  'setupReportTabs', 'renderReportTab', 'openUploadModal', 'saveForm', 'exportCsv', 'showToast', 'handleLogout'
];

// These values live on the injected root page but are edited directly by child
// controls through v-model or template assignments. Forward both reads and
// writes so a filter change is applied on the same input/change event.
const writableKeys = new Set([
  'currentId', 'selectedIndex', 'activeStep', 'viewMode', 'ownerPage', 'ownerPageSize',
  'modalMode', 'dateStart', 'dateEnd', 'datePreset', 'dateScope', 'dateNote',
  'moduleSearch', 'globalSearch', 'projectFilter', 'statusFilter', 'modalTitle',
  'toastText', 'toastVisible', 'form', 'alertItems'
]);

export default {
  inject: ['page'],
  computed: Object.fromEntries(computedKeys.map((key) => [key, writableKeys.has(key) ? {
    get() { return this.page[key]; },
    set(value) { this.page[key] = value; }
  } : function () {
    return this.page[key];
  }])),
  methods: Object.fromEntries(methodKeys.map((key) => [key, function (...args) {
    return this.page[key](...args);
  }])),
};
