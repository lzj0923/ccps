<template>
  <div class="app-frame owner-shell">
    <main class="main">
      <OwnerPage :key="localeRefreshKey" />
      <AppModal />
      <ToastMessage />
    </main>
    <OwnerBottomNav v-if="nativeMode" />
  </div>
</template>

<script>
import OwnerPage from '../../pages/OwnerPage.vue';
import AppModal from '../../components/AppModal.vue';
import ToastMessage from '../../components/ToastMessage.vue';
import OwnerBottomNav from '../../components/OwnerBottomNav.vue';
import { i18n } from '../../i18n';
import { isNativeApp } from '../../nativeApp';

export default {
  components: { OwnerPage, AppModal, ToastMessage, OwnerBottomNav },
  data() { return { nativeMode: isNativeApp }; },
  computed: {
    localeRefreshKey() { return i18n.global.locale.value; }
  }
};
</script>
