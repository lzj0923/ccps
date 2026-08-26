<template>
  <div class="page-view admin-page">
    <PageHeader />
    <MetricsGrid v-if="currentId !== 'adminOffMarketProperties' && currentId !== 'adminSmartDashboard' && currentId !== 'adminDashboard' && currentId !== 'adminProjects' && currentId !== 'adminRentalMandates' && currentId !== 'adminAudit' && currentId !== 'adminSystemBackup' && currentId !== 'adminProcess' && currentId !== 'adminRentalSigning' && currentId !== 'adminDeposits' && currentId !== 'adminTenantDirectory' && !propertyDetailId" />
    <ModuleToolbar v-if="currentId !== 'adminOffMarketProperties' && currentId !== 'adminSmartDashboard' && currentId !== 'adminDashboard' && currentId !== 'adminProjects' && currentId !== 'adminRentalMandates' && currentId !== 'adminFinance' && currentId !== 'adminAudit' && currentId !== 'adminSystemBackup' && currentId !== 'adminProcess' && currentId !== 'adminRentalSigning' && currentId !== 'adminDeposits' && currentId !== 'adminTenantDirectory' && !propertyDetailId" />
    <AdminSmartDashboardWorkspace v-if="currentId === 'adminSmartDashboard'" />
    <AdminDashboardWorkspace v-else-if="currentId === 'adminDashboard'" />
    <AdminProjectManagementWorkspace v-else-if="currentId === 'adminProjects'" />
    <AdminPropertyProcessWorkspace v-else-if="currentId === 'adminProcess'" />
    <AdminRentalSigningWorkspace v-else-if="currentId === 'adminRentalSigning'" />
    <AdminDepositWorkspace v-else-if="currentId === 'adminDeposits'" />
    <AdminPropertyDetailWorkspace v-else-if="propertyDetailId" :property-id="propertyDetailId" :return-path="currentId === 'adminOffMarketProperties' ? '/admin/off-market-rentals' : '/admin/properties'" />
    <AdminOffMarketPropertiesWorkspace v-else-if="currentId === 'adminOffMarketProperties'" />
    <AdminOwnersWorkspace v-else-if="currentId === 'adminOwners'" />
    <AdminOwnersWorkspace v-else-if="currentId === 'adminProperties'" mode="properties" />
    <AdminFinanceWorkspace v-else-if="currentId === 'adminFinance'" />
    <AdminTenantDirectoryWorkspace v-else-if="currentId === 'adminTenantDirectory'" />
    <AdminTenancyWorkspace v-else-if="currentId === 'adminTenants'" />
    <AdminRentalMandateWorkspace v-else-if="currentId === 'adminRentalMandates'" :page="page" @toast="page.showToast" />
    <AdminMaintenanceWorkspace v-else-if="currentId === 'adminMaintenance'" />
    <AdminReserveWorkspace v-else-if="currentId === 'adminReserve'" />
    <AdminReminderWorkspace v-else-if="currentId === 'adminAlerts'" />
    <AdminAccountsWorkspace v-else-if="currentId === 'adminAccounts'" />
    <AdminAuditWorkspace v-else-if="currentId === 'adminAudit'" />
    <AdminSystemBackupWorkspace v-else-if="currentId === 'adminSystemBackup'" />
    <DataWorkspace v-else />
  </div>
</template>

<script>
import pageBridge from '../pageBridge';
import PageHeader from '../components/PageHeader.vue';
import MetricsGrid from '../components/MetricsGrid.vue';
import ModuleToolbar from '../components/ModuleToolbar.vue';
import DataWorkspace from '../components/DataWorkspace.vue';
import AdminOwnersWorkspace from '../components/AdminOwnersWorkspace.vue';
import AdminPropertyDetailWorkspace from '../components/AdminPropertyDetailWorkspace.vue';
import AdminOffMarketPropertiesWorkspace from '../components/AdminOffMarketPropertiesWorkspace.vue';
import AdminFinanceWorkspace from '../components/AdminFinanceWorkspace.vue';
import AdminTenancyWorkspace from '../components/AdminTenancyWorkspace.vue';
import AdminTenantDirectoryWorkspace from '../components/AdminTenantDirectoryWorkspace.vue';
import AdminRentalMandateWorkspace from '../components/AdminRentalMandateWorkspace.vue';
import AdminMaintenanceWorkspace from '../components/AdminMaintenanceWorkspace.vue';
import AdminReserveWorkspace from '../components/AdminReserveWorkspace.vue';
import AdminReminderWorkspace from '../components/AdminReminderWorkspace.vue';
import AdminAccountsWorkspace from '../components/AdminAccountsWorkspace.vue';
import AdminAuditWorkspace from '../components/AdminAuditWorkspace.vue';
import AdminPropertyProcessWorkspace from '../components/AdminPropertyProcessWorkspace.vue';
import AdminRentalSigningWorkspace from '../components/AdminRentalSigningWorkspace.vue';
import AdminDepositWorkspace from '../components/AdminDepositWorkspace.vue';
import AdminDashboardWorkspace from '../components/AdminDashboardWorkspace.vue';
import AdminSmartDashboardWorkspace from '../components/AdminSmartDashboardWorkspace.vue';
import AdminSystemBackupWorkspace from '../components/AdminSystemBackupWorkspace.vue';
import AdminProjectManagementWorkspace from '../components/AdminProjectManagementWorkspace.vue';

export default {
  mixins: [pageBridge],
  data() { return { routePath: window.location.pathname }; },
  components: { PageHeader, MetricsGrid, ModuleToolbar, DataWorkspace, AdminSmartDashboardWorkspace, AdminDashboardWorkspace, AdminSystemBackupWorkspace, AdminProjectManagementWorkspace, AdminOwnersWorkspace, AdminPropertyDetailWorkspace, AdminOffMarketPropertiesWorkspace, AdminPropertyProcessWorkspace, AdminRentalSigningWorkspace, AdminDepositWorkspace, AdminFinanceWorkspace, AdminTenantDirectoryWorkspace, AdminTenancyWorkspace, AdminRentalMandateWorkspace, AdminMaintenanceWorkspace, AdminReserveWorkspace, AdminReminderWorkspace, AdminAccountsWorkspace, AdminAuditWorkspace },
  computed: { propertyDetailId() { const match = this.routePath.match(/^\/admin\/(?:properties|off-market-rentals)\/([^/]+)$/); return match ? match[1] : ''; } },
  mounted() { window.addEventListener('app-route-change', this.syncPropertyPath); window.addEventListener('popstate', this.syncPropertyPath); },
  beforeUnmount() { window.removeEventListener('app-route-change', this.syncPropertyPath); window.removeEventListener('popstate', this.syncPropertyPath); },
  methods: { syncPropertyPath() { this.routePath = window.location.pathname; } },
};
</script>
