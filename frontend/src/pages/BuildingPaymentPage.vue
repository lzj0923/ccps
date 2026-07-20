<template>
  <div class="page-view building-payment-page">
    <PageHeader />
    <MetricsGrid />
    <ModuleToolbar />
    <AdminBuildingPaymentWorkspace />
  </div>
</template>

<script>
import pageBridge from '../pageBridge';
import PageHeader from '../components/PageHeader.vue';
import MetricsGrid from '../components/MetricsGrid.vue';
import ModuleToolbar from '../components/ModuleToolbar.vue';
import AdminBuildingPaymentWorkspace from '../components/AdminBuildingPaymentWorkspace.vue';

export default {
  mixins: [pageBridge],
  components: { PageHeader, MetricsGrid, ModuleToolbar, AdminBuildingPaymentWorkspace },
};
</script>
