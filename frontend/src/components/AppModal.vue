<template>
  <dialog ref="modal" class="modal">
    <form method="dialog">
      <div class="modal-head"><h3>{{ modalTitle }}</h3><button value="cancel" class="icon-close">X</button></div>
      <div v-if="modalMode === 'alerts'" class="notice-list">
        <button v-for="item in alertItems" :key="item.title" class="notice-item" @click="selectAlert(item)">
          <span class="tag" :class="tagClass(item.status)">{{ item.status }}</span><strong>{{ item.title }}</strong><small>{{ item.detail }}</small>
        </button>
      </div>
      <div v-else class="form-grid">
        <label>Name<input v-model="form.name"></label>
        <label>Phone<input v-model="form.phone"></label>
        <label>Project<select v-model="form.project"><option>Pavilion Square</option><option>CCP Residence</option></select></label>
        <label>Status<select v-model="form.status"><option>Active</option><option>Pending</option></select></label>
        <label class="wide">Notes<textarea v-model="form.note"></textarea></label>
      </div>
      <menu><button value="cancel" class="ghost-btn">Cancel</button><button value="default" class="primary-btn" @click="saveForm">{{ modalConfirmText }}</button></menu>
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
