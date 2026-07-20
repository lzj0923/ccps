<template>
  <div class="app-frame owner-shell">
    <main class="main">
      <OwnerPage />
      <AppModal />
      <ToastMessage />
    </main>
  </div>
</template>

<script>
import OwnerPage from '../../pages/OwnerPage.vue';
import AppModal from '../../components/AppModal.vue';
import ToastMessage from '../../components/ToastMessage.vue';

export default {
  components: { OwnerPage, AppModal, ToastMessage }
};
</script>
