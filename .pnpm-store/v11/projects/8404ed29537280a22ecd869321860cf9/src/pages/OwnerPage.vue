<template>
  <div class="page-view owner-page">
    <PageHeader :owner-payment-subview="paymentSubview" />
    <MyPropertiesDashboard v-if="currentId === 'myProperties'" />
    <template v-else-if="currentId === 'ownerPayment'">
      <PaymentProofUpload v-if="paymentSubview === 'upload'" :context="paymentUploadContext" @back="closePaymentUpload" />
      <PaymentProgressDetails v-else :initial-property-key="lastPaymentPropertyKey" @upload="openPaymentUpload" />
    </template>
    <RentIncomeDashboard v-else-if="currentId === 'rentIncome'" />
    <ExpenseMaintenanceDashboard v-else-if="currentId === 'ownerExpenses'" />
    <ReserveDashboard v-else-if="currentId === 'ownerReserve'" />
    <NotificationCenterDashboard v-else-if="currentId === 'ownerNotice'" />
    <DocumentCenterDashboard v-else-if="currentId === 'ownerDocuments'" />
    <template v-else>
      <MetricsGrid />
      <ModuleToolbar />
      <ModuleCards />
      <DataWorkspace />
    </template>
  </div>
</template>

<script>
import pageBridge from '../pageBridge';
import PageHeader from '../components/PageHeader.vue';
import MetricsGrid from '../components/MetricsGrid.vue';
import ModuleToolbar from '../components/ModuleToolbar.vue';
import ModuleCards from '../components/ModuleCards.vue';
import DataWorkspace from '../components/DataWorkspace.vue';
import MyPropertiesDashboard from '../components/MyPropertiesDashboard.vue';
import PaymentProgressDetails from '../components/PaymentProgressDetails.vue';
import PaymentProofUpload from '../components/PaymentProofUpload.vue';
import RentIncomeDashboard from '../components/RentIncomeDashboard.vue';
import ExpenseMaintenanceDashboard from '../components/ExpenseMaintenanceDashboard.vue';
import ReserveDashboard from '../components/ReserveDashboard.vue';
import NotificationCenterDashboard from '../components/NotificationCenterDashboard.vue';
import DocumentCenterDashboard from '../components/DocumentCenterDashboard.vue';

export default {
  mixins: [pageBridge],
  components: { PageHeader, MetricsGrid, ModuleToolbar, ModuleCards, DataWorkspace, MyPropertiesDashboard, PaymentProgressDetails, PaymentProofUpload, RentIncomeDashboard, ExpenseMaintenanceDashboard, ReserveDashboard, NotificationCenterDashboard, DocumentCenterDashboard },
  data() {
    return { paymentSubview: 'details', paymentUploadContext: null, lastPaymentPropertyKey: '' };
  },
  watch: {
    currentId(value) {
      if (value !== 'ownerPayment') this.closePaymentUpload();
    }
  },
  methods: {
    openPaymentUpload(context) {
      this.paymentUploadContext = context;
      this.lastPaymentPropertyKey = String(context.ownerUnitId || '');
      this.paymentSubview = 'upload';
      window.scrollTo({ top: 0, behavior: 'smooth' });
    },
    closePaymentUpload() {
      this.paymentSubview = 'details';
      this.paymentUploadContext = null;
    }
  }
};
</script>
