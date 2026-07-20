<template>
  <div class="app-frame admin-shell">
    <SidebarNav />
    <main class="main">
      <ReportPage v-if="currentId === 'adminReports'" />
      <BuildingPaymentPage v-else-if="currentId === 'adminData'" />
      <AdminPage v-else />
      <AppModal />
      <ToastMessage />
    </main>
  </div>
</template>

<script>
import pageBridge from '../../pageBridge';
import SidebarNav from '../../components/SidebarNav.vue';
import AppModal from '../../components/AppModal.vue';
import ToastMessage from '../../components/ToastMessage.vue';
import AdminPage from '../../pages/AdminPage.vue';
import ReportPage from '../../pages/ReportPage.vue';
import BuildingPaymentPage from '../../pages/BuildingPaymentPage.vue';

export default {
  mixins: [pageBridge],
  components: { SidebarNav, AppModal, ToastMessage, AdminPage, ReportPage, BuildingPaymentPage }
};
</script>
