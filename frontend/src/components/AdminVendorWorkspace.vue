<template>
  <section class="vendor-workspace">
    <div class="panel vendor-panel">
      <div class="vendor-head">
        <div><h2>{{ $t('legacy.t_80a60d4fdc17') }}</h2><span>{{ $t('legacy.t_772495582ea7') }}</span></div>
        <div class="vendor-head-actions"><button class="vendor-secondary" type="button" @click="backToRepairReport">{{ $t('legacy.t_cbb2950b97be') }}</button><button class="vendor-primary" type="button" @click="openCreate">{{ $t('legacy.t_225a47881756') }}</button></div>
      </div>
      <div class="vendor-toolbar"><input v-model.trim="search" type="search" :placeholder="$t('legacy.t_717a62b642bd')"><select v-model="statusFilter"><option value="all">{{ $t('legacy.t_026ed0343be6') }}</option><option value="active">{{ $t('legacy.t_ce6c3dc32674') }}</option><option value="inactive">{{ $t('legacy.t_d989e55188c9') }}</option></select><span>{{ $t('legacy.t_3b6ef811b85a') }} {{ filteredVendors.length }} {{ $t('legacy.t_7de8177ce74b') }}</span></div>
      <div v-if="loading" class="vendor-state">{{ $t('legacy.t_e19553d29228') }}</div>
      <div v-else-if="errorMessage" class="vendor-state error"><strong>{{ $t('legacy.t_d30360451be9') }}</strong><span>{{ $lt(errorMessage) }}</span><button type="button" @click="loadVendors">{{ $t('legacy.t_0c9157b5bfac') }}</button></div>
      <div v-else class="table-wrap vendor-table-wrap">
        <table>
          <thead><tr><th>{{ $t('legacy.t_63b5592477dc') }}</th><th>{{ $t('legacy.t_3c71a1c2cfaf') }}</th><th>{{ $t('legacy.t_8dcb070de196') }}</th><th>{{ $t('legacy.t_7177787c6e84') }}</th><th>{{ $t('legacy.t_84add5b29527') }}</th><th>{{ $t('legacy.t_45293595eae3') }}</th><th>{{ $t('legacy.t_f3ea6d345e2a') }}</th></tr></thead>
          <tbody>
            <tr v-for="vendor in filteredVendors" :key="vendor.id">
              <td>{{ vendor.vendorCode }}</td><td><strong>{{ vendor.name }}</strong></td><td>{{ vendor.contactName || '—' }}</td><td>{{ vendor.phone || '—' }}</td><td>{{ vendor.email || '—' }}</td>
              <td><span class="vendor-status" :class="vendor.status">{{ $lt(statusLabel(vendor.status)) }}</span></td>
              <td class="vendor-actions"><button type="button" @click="openEdit(vendor)">{{ $t('legacy.t_bad46aea44dc') }}</button><button v-if="vendor.status === 'active'" type="button" class="danger" @click="deactivate(vendor)">{{ $t('legacy.t_d989e55188c9') }}</button><button v-else type="button" @click="reactivate(vendor)">{{ $t('legacy.t_ce6c3dc32674') }}</button></td>
            </tr>
            <tr v-if="!filteredVendors.length"><td colspan="7" class="empty-cell">{{ $t('legacy.t_49c456bed84b') }}</td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="dialogOpen" class="vendor-dialog-backdrop" @pointerdown.self="closeDialog">
      <form class="vendor-dialog" @submit.prevent="saveVendor">
        <header><div><h3>{{ editingId ? $t('legacy.t_3055abc1b013') : $t('legacy.t_225a47881756') }}</h3><p>{{ $t('legacy.t_e986bbbf8bd8') }}</p></div><button type="button" class="vendor-close" @click="closeDialog">×</button></header>
        <div class="vendor-form-grid">
          <label class="wide">{{ $t('legacy.t_3c71a1c2cfaf') }}<input v-model.trim="form.name" maxlength="160" required :placeholder="$t('legacy.t_78ae0bc7ee32')"></label>
          <label>{{ $t('legacy.t_8dcb070de196') }}<input v-model.trim="form.contactName" maxlength="120" :placeholder="$t('legacy.t_0da627916bd7')"></label>
          <label>{{ $t('legacy.t_7177787c6e84') }}<input v-model.trim="form.phone" maxlength="40" :placeholder="$t('legacy.t_8c2286a406ae')"></label>
          <label class="wide">{{ $t('legacy.t_84add5b29527') }}<input v-model.trim="form.email" type="email" maxlength="190" :placeholder="$t('legacy.t_89bfe788b0c3')"></label>
          <label v-if="editingId">{{ $t('legacy.t_45293595eae3') }}<select v-model="form.status"><option value="active">{{ $t('legacy.t_ce6c3dc32674') }}</option><option value="inactive">{{ $t('legacy.t_d989e55188c9') }}</option></select></label>
        </div>
        <p v-if="formError" class="vendor-form-error">{{ $lt(formError) }}</p>
        <footer><button type="button" class="vendor-secondary" @click="closeDialog">{{ $t('legacy.t_4d0b4688c787') }}</button><button class="vendor-primary" type="submit" :disabled="saving">{{ saving ? $t('legacy.t_8488ea2522af') : $t('legacy.t_e7ffb1ee5ee7') }}</button></footer>
      </form>
    </div>
  </section>
</template>

<script>
import { createAdminVendor, deactivateAdminVendor, fetchAdminVendors, updateAdminVendor } from '../services/propertyApi';
export default {
  name: 'AdminVendorWorkspace',
  emits: ['back'],
  data() { return { vendors: [], loading: false, saving: false, errorMessage: '', formError: '', search: '', statusFilter: 'all', dialogOpen: false, editingId: null, form: this.emptyForm() }; },
  computed: { filteredVendors() { const query = this.search.toLowerCase(); return this.vendors.filter(vendor => { const matchesStatus = this.statusFilter === 'all' || vendor.status === this.statusFilter; const matchesSearch = !query || [vendor.vendorCode, vendor.name, vendor.contactName, vendor.phone, vendor.email].some(value => String(value || '').toLowerCase().includes(query)); return matchesStatus && matchesSearch; }); } },
  mounted() { this.loadVendors(); },
  methods: {
    emptyForm() { return { name: '', contactName: '', phone: '', email: '', status: 'active' }; },
    statusLabel(status) { return status === 'active' ? '啟用' : '停用'; },
    backToRepairReport() { this.$emit('back'); },
    async loadVendors() { this.loading = true; this.errorMessage = ''; try { this.vendors = await fetchAdminVendors(); } catch (error) { this.errorMessage = error.message || '無法讀取服務商資料'; } finally { this.loading = false; } },
    openCreate() { this.editingId = null; this.form = this.emptyForm(); this.formError = ''; this.dialogOpen = true; },
    openEdit(vendor) { this.editingId = vendor.id; this.form = { name: vendor.name || '', contactName: vendor.contactName || '', phone: vendor.phone || '', email: vendor.email || '', status: vendor.status || 'active' }; this.formError = ''; this.dialogOpen = true; },
    closeDialog() { if (!this.saving) { this.dialogOpen = false; this.formError = ''; } },
    async saveVendor() { if (!this.form.name.trim()) { this.formError = '請輸入服務商名稱'; return; } this.saving = true; this.formError = ''; try { const saved = this.editingId ? await updateAdminVendor(this.editingId, this.form) : await createAdminVendor(this.form); const index = this.vendors.findIndex(item => item.id === saved.id); if (index >= 0) this.vendors.splice(index, 1, saved); else this.vendors.unshift(saved); this.closeDialog(); } catch (error) { this.formError = error.message || '服務商保存失敗'; } finally { this.saving = false; } },
    async deactivate(vendor) { if (!window.confirm(this.$ltf`確定停用「${vendor.name}」嗎？歷史維修記錄會保留。`)) return; try { await deactivateAdminVendor(vendor.id); vendor.status = 'inactive'; } catch (error) { this.errorMessage = error.message || '服務商停用失敗'; } },
    async reactivate(vendor) { try { const saved = await updateAdminVendor(vendor.id, { name: vendor.name, contactName: vendor.contactName, phone: vendor.phone, email: vendor.email, status: 'active' }); const index = this.vendors.findIndex(item => item.id === vendor.id); if (index >= 0) this.vendors.splice(index, 1, saved); } catch (error) { this.errorMessage = error.message || '服務商啟用失敗'; } }
  }
};
</script>

<style scoped>
.vendor-workspace{padding:0 2px 28px}.vendor-panel{overflow:hidden}.vendor-head{display:flex;align-items:center;justify-content:space-between;gap:18px;padding:22px 26px;border-bottom:1px solid #e5edf4}.vendor-head h2{margin:0;color:#153f70;font-size:22px}.vendor-head span,.vendor-head p{color:#71859a;font-size:13px}.vendor-head-actions,.vendor-toolbar,.vendor-actions{display:flex;align-items:center;gap:10px}.vendor-primary,.vendor-secondary,.vendor-actions button,.vendor-state button{border-radius:8px;padding:9px 15px;border:1px solid #c8d8e8;background:#fff;color:#144579;cursor:pointer}.vendor-primary{background:#f3b500;border-color:#e2a500;color:#fff}.vendor-primary:disabled{opacity:.6;cursor:wait}.vendor-toolbar{padding:16px 26px;background:#f7fafc}.vendor-toolbar input,.vendor-toolbar select,.vendor-form-grid input,.vendor-form-grid select{border:1px solid #c8d8e8;border-radius:7px;padding:10px 12px;background:#fff;color:#23415f}.vendor-toolbar input{width:340px}.vendor-toolbar span{margin-left:auto;color:#71859a;font-size:13px}.vendor-table-wrap{padding:0 18px 18px}.vendor-table-wrap table{width:100%;border-collapse:collapse}.vendor-table-wrap th,.vendor-table-wrap td{padding:13px 10px;text-align:left;border-bottom:1px solid #e6eef5;font-size:13px}.vendor-table-wrap th{color:#6e8398;background:#fbfdff}.vendor-status{display:inline-block;border-radius:999px;padding:4px 10px;font-size:12px}.vendor-status.active{color:#14754d;background:#e5f8ee}.vendor-status.inactive{color:#8b5d22;background:#fff3d6}.vendor-actions button{padding:6px 10px;font-size:12px}.vendor-actions .danger{color:#aa3e3e}.vendor-state{padding:55px;text-align:center;color:#71859a}.vendor-state.error,.vendor-form-error{color:#b64040}.vendor-state button{margin-left:12px}.empty-cell{text-align:center;color:#8b9bae;padding:36px!important}.vendor-dialog-backdrop{position:fixed;inset:0;z-index:30;display:grid;place-items:center;padding:20px;background:rgba(15,35,55,.42)}.vendor-dialog{width:min(560px,100%);background:#fff;border-radius:14px;box-shadow:0 22px 70px rgba(18,53,83,.25);padding:22px}.vendor-dialog header,.vendor-dialog footer{display:flex;align-items:center;justify-content:space-between;gap:14px}.vendor-dialog h3{margin:0;color:#153f70}.vendor-dialog p{margin:5px 0 0;color:#71859a;font-size:13px}.vendor-close{border:0;background:none;font-size:24px;color:#71859a;cursor:pointer}.vendor-form-grid{display:grid;grid-template-columns:1fr 1fr;gap:14px;margin:22px 0}.vendor-form-grid label{display:grid;gap:6px;color:#49627a;font-size:13px}.vendor-form-grid .wide{grid-column:1/-1}.vendor-form-error{margin:0 0 16px}.vendor-dialog footer{justify-content:flex-end;border-top:1px solid #edf2f7;padding-top:16px}@media(max-width:700px){.vendor-head{align-items:flex-start;flex-direction:column}.vendor-toolbar{align-items:stretch;flex-direction:column}.vendor-toolbar input{width:auto}.vendor-toolbar span{margin-left:0}.vendor-table-wrap{overflow:auto}.vendor-form-grid{grid-template-columns:1fr}.vendor-form-grid .wide{grid-column:auto}}
</style>
