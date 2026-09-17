<template>
  <dialog ref="modal" class="modal app-modal" @close="dateFilterError = ''">
    <form @submit.prevent="confirmModal">
      <div class="modal-head"><h3>{{ modalTitle }}</h3><button type="button" class="icon-close" @click="closeModal">{{ $t('legacy.t_c032adc1ff62') }}</button></div>
      <div v-if="modalMode === 'date'" class="date-filter-form">
        <label class="date-filter-preset">{{ $t('common.datePreset') }}
          <select v-model="datePreset" @change="applyDatePreset">
            <option value="year">{{ $t('common.currentYear') }}</option>
            <option value="h1">{{ $t('common.firstHalf') }}</option>
            <option value="h2">{{ $t('common.secondHalf') }}</option>
            <option value="month">{{ $t('common.currentMonth') }}</option>
            <option value="custom">{{ $t('common.customRange') }}</option>
          </select>
        </label>
        <div class="date-filter-grid">
          <label>{{ $t('common.dateStart') }}<input v-model="dateDraftStart" type="date" :max="dateDraftEnd || undefined" @input="datePreset = 'custom'; dateFilterError = ''"></label>
          <span aria-hidden="true">→</span>
          <label>{{ $t('common.dateEnd') }}<input v-model="dateDraftEnd" type="date" :min="dateDraftStart || undefined" @input="datePreset = 'custom'; dateFilterError = ''"></label>
        </div>
        <p class="date-filter-note">{{ $t('common.dateFilterHint') }}</p>
        <p v-if="dateFilterError" class="date-filter-error" role="alert">{{ $lt(dateFilterError) }}</p>
      </div>
      <div v-else-if="modalMode === 'alerts'" class="notice-list">
        <div v-if="alertLoading" class="notice-state">{{ $t('common.notificationsLoading') }}</div>
        <div v-else-if="alertError" class="notice-state error"><span>{{ $lt(alertError) }}</span><button type="button" @click="refreshAdminAlerts">{{ $t('common.retry') }}</button></div>
        <template v-else>
          <button v-for="item in alertItems" :key="item.id" type="button" class="notice-item" @click="selectAlert(item)">
            <span class="tag" :class="item.tone">{{ $lt(item.status) }}</span><strong>{{ $lt(item.title) }}</strong><small>{{ $lt(item.detail) }}</small><time>{{ item.time }}</time>
          </button>
        </template>
        <div v-if="!alertLoading && !alertError && !alertItems.length" class="notice-state">{{ $t('common.noNotifications') }}</div>
      </div>
      <div v-else class="form-grid">
        <label>{{ $t('legacy.t_709a23220f2c') }}<input v-model="form.name"></label>
        <label>{{ $t('legacy.t_77064d526523') }}<input v-model="form.phone"></label>
        <label>{{ $t('legacy.t_f6f4da8d93e8') }}<select v-model="form.project"><option>{{ $t('legacy.t_fcbc7a96a87f') }}</option><option>{{ $t('legacy.t_5a00a9ab139b') }}</option></select></label>
        <label>{{ $t('legacy.t_bae7d5be7082') }}<select v-model="form.status"><option>{{ $t('legacy.t_a733b809d2f1') }}</option><option>{{ $t('legacy.t_96f608c16cef') }}</option></select></label>
        <label class="wide">{{ $t('legacy.t_70440046a3dc') }}<textarea v-model="form.note"></textarea></label>
      </div>
      <menu><button type="button" class="ghost-btn" @click="closeModal">{{ $t('legacy.t_77dfd2135f4d') }}</button><button type="submit" class="primary-btn">{{ modalConfirmText }}</button></menu>
    </form>
  </dialog>
</template>

<script>
import pageBridge from '../pageBridge';
export default {
  mixins: [pageBridge],
  mounted() { this.page.modal = this.$refs.modal; },
  methods: { closeModal() { this.$refs.modal?.close(); } }
};
</script>
