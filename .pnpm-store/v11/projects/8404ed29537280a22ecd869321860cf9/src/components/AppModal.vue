<template>
  <dialog ref="modal" class="modal">
    <form method="dialog">
      <div class="modal-head"><h3>{{ modalTitle }}</h3><button value="cancel" class="icon-close">{{ $t('legacy.t_c032adc1ff62') }}</button></div>
      <div v-if="modalMode === 'alerts'" class="notice-list">
        <button v-for="item in alertItems" :key="item.title" class="notice-item" @click="selectAlert(item)">
          <span class="tag" :class="tagClass(item.status)">{{ item.status }}</span><strong>{{ item.title }}</strong><small>{{ item.detail }}</small>
        </button>
      </div>
      <div v-else class="form-grid">
        <label>{{ $t('legacy.t_709a23220f2c') }}<input v-model="form.name"></label>
        <label>{{ $t('legacy.t_77064d526523') }}<input v-model="form.phone"></label>
        <label>{{ $t('legacy.t_f6f4da8d93e8') }}<select v-model="form.project"><option>{{ $t('legacy.t_fcbc7a96a87f') }}</option><option>{{ $t('legacy.t_5a00a9ab139b') }}</option></select></label>
        <label>{{ $t('legacy.t_bae7d5be7082') }}<select v-model="form.status"><option>{{ $t('legacy.t_a733b809d2f1') }}</option><option>{{ $t('legacy.t_96f608c16cef') }}</option></select></label>
        <label class="wide">{{ $t('legacy.t_70440046a3dc') }}<textarea v-model="form.note"></textarea></label>
      </div>
      <menu><button value="cancel" class="ghost-btn">{{ $t('legacy.t_77dfd2135f4d') }}</button><button value="default" class="primary-btn" @click="saveForm">{{ modalConfirmText }}</button></menu>
    </form>
  </dialog>
</template>

<script>
import pageBridge from '../pageBridge';
export default {
  mixins: [pageBridge],
  mounted() { this.page.modal = this.$refs.modal; }
};
</script>
