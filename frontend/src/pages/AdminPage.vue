<template>
  <div class="page-view admin-page">
    <PageHeader />
    <MetricsGrid v-if="currentId !== 'adminRentalMandates' && currentId !== 'adminAudit' && currentId !== 'adminProcess' && currentId !== 'adminTenantDirectory' && !propertyDetailId" />
    <ModuleToolbar v-if="currentId !== 'adminRentalMandates' && currentId !== 'adminFinance' && currentId !== 'adminAudit' && currentId !== 'adminProcess' && currentId !== 'adminTenantDirectory' && !propertyDetailId" />
    <AdminPropertyProcessWorkspace v-if="currentId === 'adminProcess'" />
    <AdminPropertyDetailWorkspace v-else-if="propertyDetailId" :property-id="propertyDetailId" />
    <AdminOwnersWorkspace v-else-if="currentId === 'adminOwners'" />
    <AdminOwnersWorkspace v-else-if="currentId === 'adminProperties'" mode="properties" />
    <AdminFinanceWorkspace v-else-if="currentId === 'adminFinance'" />
    <AdminTenantDirectoryWorkspace v-else-if="currentId === 'adminTenantDirectory'" />
    <AdminTenancyWorkspace v-else-if="currentId === 'adminTenants'" />
    <AdminRentalMandateWorkspace v-else-if="currentId === 'adminRentalMandates'" :page="page" @toast="page.showToast" />
    <AdminMaintenanceWorkspace v-else-if="currentId === 'adminMaintenance'" />
    <AdminReserveWorkspace v-else-if="currentId === 'adminReserve'" />
    <AdminReminderWorkspace v-else-if="currentId === 'adminAlerts'" />
    <AdminAuditWorkspace v-else-if="currentId === 'adminAudit'" />
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
import AdminFinanceWorkspace from '../components/AdminFinanceWorkspace.vue';
import AdminTenancyWorkspace from '../components/AdminTenancyWorkspace.vue';
import AdminTenantDirectoryWorkspace from '../components/AdminTenantDirectoryWorkspace.vue';
import AdminRentalMandateWorkspace from '../components/AdminRentalMandateWorkspace.vue';
import AdminMaintenanceWorkspace from '../components/AdminMaintenanceWorkspace.vue';
import AdminReserveWorkspace from '../components/AdminReserveWorkspace.vue';
import AdminReminderWorkspace from '../components/AdminReminderWorkspace.vue';
import AdminAuditWorkspace from '../components/AdminAuditWorkspace.vue';
import AdminPropertyProcessWorkspace from '../components/AdminPropertyProcessWorkspace.vue';

export default {
  mixins: [pageBridge],
  data() { return { routePath: window.location.pathname }; },
  components: { PageHeader, MetricsGrid, ModuleToolbar, DataWorkspace, AdminOwnersWorkspace, AdminPropertyDetailWorkspace, AdminPropertyProcessWorkspace, AdminFinanceWorkspace, AdminTenantDirectoryWorkspace, AdminTenancyWorkspace, AdminRentalMandateWorkspace, AdminMaintenanceWorkspace, AdminReserveWorkspace, AdminReminderWorkspace, AdminAuditWorkspace },
  computed: { propertyDetailId() { const match = this.routePath.match(/^\/admin\/properties\/([^/]+)$/); return match ? match[1] : ''; } },
  mounted() { window.addEventListener('app-route-change', this.syncPropertyPath); window.addEventListener('popstate', this.syncPropertyPath); },
  beforeUnmount() { window.removeEventListener('app-route-change', this.syncPropertyPath); window.removeEventListener('popstate', this.syncPropertyPath); },
  methods: { syncPropertyPath() { this.routePath = window.location.pathname; } },
};
</script>
