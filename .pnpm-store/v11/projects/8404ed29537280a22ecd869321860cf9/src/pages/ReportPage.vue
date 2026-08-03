<template>
  <div class="page-view report-page">
    <PageHeader />
    <MetricsGrid />
    <AdminReportWorkspace />
  </div>
</template>

<script>
import pageBridge from '../pageBridge';
import PageHeader from '../components/PageHeader.vue';
import MetricsGrid from '../components/MetricsGrid.vue';
import AdminReportWorkspace from '../components/AdminReportWorkspace.vue';

export default {
  mixins: [pageBridge],
  components: { PageHeader, MetricsGrid, AdminReportWorkspace },
};
</script>
