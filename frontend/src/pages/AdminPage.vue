<template>
  <div class="page-view admin-page">
    <PageHeader />
    <MetricsGrid />
    <ModuleToolbar />
    <AdminOwnersWorkspace v-if="currentId === 'adminOwners'" />
    <AdminOwnersWorkspace v-else-if="currentId === 'adminProperties'" mode="properties" />
    <AdminFinanceWorkspace v-else-if="currentId === 'adminFinance'" />
    <AdminTenancyWorkspace v-else-if="currentId === 'adminTenants'" />
    <AdminRentalMandateWorkspace v-else-if="currentId === 'adminRentalMandates'" :page="page" />
    <AdminMaintenanceWorkspace v-else-if="currentId === 'adminMaintenance'" />
    <AdminReserveWorkspace v-else-if="currentId === 'adminReserve'" />
    <AdminReminderWorkspace v-else-if="currentId === 'adminAlerts'" />
    <AdminSyncWorkspace v-else-if="currentId === 'adminSync'" />
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
import AdminFinanceWorkspace from '../components/AdminFinanceWorkspace.vue';
import AdminTenancyWorkspace from '../components/AdminTenancyWorkspace.vue';
import AdminRentalMandateWorkspace from '../components/AdminRentalMandateWorkspace.vue';
import AdminMaintenanceWorkspace from '../components/AdminMaintenanceWorkspace.vue';
import AdminReserveWorkspace from '../components/AdminReserveWorkspace.vue';
import AdminReminderWorkspace from '../components/AdminReminderWorkspace.vue';
import AdminSyncWorkspace from '../components/AdminSyncWorkspace.vue';

export default {
  mixins: [pageBridge],
  components: { PageHeader, MetricsGrid, ModuleToolbar, DataWorkspace, AdminOwnersWorkspace, AdminFinanceWorkspace, AdminTenancyWorkspace, AdminRentalMandateWorkspace, AdminMaintenanceWorkspace, AdminReserveWorkspace, AdminReminderWorkspace, AdminSyncWorkspace },
};
</script>
