<template>
  <div ref="root" class="owner-staff-multi-select">
    <button
      class="owner-staff-multi-trigger"
      type="button"
      :aria-label="label"
      :aria-expanded="open"
      aria-haspopup="listbox"
      @click="open = !open"
    >
      <span :class="{ placeholder: !selectedOptions.length }">{{ summary }}</span>
      <ChevronDown :size="16" :class="{ rotated: open }" aria-hidden="true" />
    </button>
    <div v-if="open" class="owner-staff-multi-menu" role="listbox" :aria-label="label" aria-multiselectable="true">
      <p v-if="!options.length" class="owner-staff-multi-empty">{{ emptyText }}</p>
      <label v-for="option in options" :key="option.id" class="owner-staff-multi-option" role="option" :aria-selected="isSelected(option.id)">
        <input type="checkbox" :checked="isSelected(option.id)" @change="toggle(option.id)">
        <span><strong>{{ option.displayName || option.username }}</strong><small>{{ option.username }}</small></span>
      </label>
    </div>
  </div>
</template>

<script>
import { ChevronDown } from '@lucide/vue';

export default {
  name: 'OwnerStaffMultiSelect',
  components: { ChevronDown },
  props: {
    modelValue: { type: Array, default: () => [] },
    options: { type: Array, default: () => [] },
    label: { type: String, default: '所属工作人员' },
    placeholder: { type: String, default: '请选择工作人员' },
    emptyText: { type: String, default: '暂无可选管理员账号' }
  },
  emits: ['update:modelValue'],
  data() { return { open: false }; },
  computed: {
    selectedOptions() {
      const selected = new Set(this.modelValue.map(Number));
      return this.options.filter(option => selected.has(Number(option.id)));
    },
    summary() {
      if (!this.selectedOptions.length) return this.placeholder;
      if (this.selectedOptions.length <= 2) return this.selectedOptions.map(option => option.displayName || option.username).join('、');
      return `${this.selectedOptions[0].displayName || this.selectedOptions[0].username} 等 ${this.selectedOptions.length} 人`;
    }
  },
  mounted() { document.addEventListener('pointerdown', this.closeFromOutside); },
  beforeUnmount() { document.removeEventListener('pointerdown', this.closeFromOutside); },
  methods: {
    isSelected(id) { return this.modelValue.map(Number).includes(Number(id)); },
    toggle(id) {
      const value = Number(id);
      const selected = this.modelValue.map(Number);
      this.$emit('update:modelValue', selected.includes(value) ? selected.filter(item => item !== value) : [...selected, value]);
    },
    closeFromOutside(event) {
      if (!this.$refs.root?.contains(event.target)) this.open = false;
    }
  }
};
</script>

<style scoped>
.owner-staff-multi-select{position:relative;margin-top:6px}.owner-staff-multi-trigger{display:flex;align-items:center;justify-content:space-between;gap:12px;width:100%;min-height:40px;padding:8px 11px;border:1px solid #c8d8e4;border-radius:9px;background:#fff;color:#173b59;text-align:left}.owner-staff-multi-trigger .placeholder{color:#8191a2}.owner-staff-multi-trigger svg{flex:none;transition:transform .16s ease}.owner-staff-multi-trigger svg.rotated{transform:rotate(180deg)}.owner-staff-multi-menu{position:absolute;z-index:30;top:calc(100% + 6px);right:0;left:0;max-height:230px;overflow:auto;padding:6px;border:1px solid #c8d8e4;border-radius:10px;background:#fff;box-shadow:0 14px 34px rgba(20,50,75,.16)}.owner-staff-multi-option{display:flex!important;align-items:center;gap:10px;margin:0!important;padding:9px 10px;border-radius:7px;cursor:pointer}.owner-staff-multi-option:hover{background:#eff8fa}.owner-staff-multi-option input{width:16px!important;height:16px!important;margin:0!important}.owner-staff-multi-option span{display:grid;gap:2px}.owner-staff-multi-option strong{font-size:13px}.owner-staff-multi-option small{color:#71889f;font-size:11px}.owner-staff-multi-empty{margin:0;padding:14px;color:#71889f;text-align:center}
</style>
