<template>
  <section class="project-management">
    <header class="project-heading">
      <div>
        <span class="eyebrow">{{ $t('projectManagement.eyebrow') }}</span>
        <h2>{{ $t('projectManagement.title') }}</h2>
        <p>{{ $t('projectManagement.subtitle') }}</p>
      </div>
      <button type="button" class="primary-btn" @click="openCreate">＋ {{ $t('projectManagement.add') }}</button>
    </header>

    <div class="project-metrics">
      <article><span>{{ $t('projectManagement.total') }}</span><strong>{{ summary.totalCount }}</strong><small>{{ $t('projectManagement.projectUnit') }}</small></article>
      <article><span>{{ $t('projectManagement.active') }}</span><strong class="good">{{ summary.activeCount }}</strong><small>{{ $t('projectManagement.availableHint') }}</small></article>
      <article><span>{{ $t('projectManagement.inactive') }}</span><strong>{{ summary.inactiveCount }}</strong><small>{{ $t('projectManagement.inactiveHint') }}</small></article>
      <article><span>{{ $t('projectManagement.units') }}</span><strong class="accent">{{ summary.unitCount }}</strong><small>{{ $t('projectManagement.unitsHint') }}</small></article>
    </div>

    <div class="project-toolbar">
      <label class="search-box"><Search :size="16" :stroke-width="2" aria-hidden="true" /><input v-model.trim="keyword" :placeholder="$t('projectManagement.search')" @keyup.enter="search"></label>
      <select v-model="status" @change="search">
        <option value="">{{ $t('projectManagement.allStatus') }}</option>
        <option value="active">{{ $t('projectManagement.active') }}</option>
        <option value="inactive">{{ $t('projectManagement.inactive') }}</option>
      </select>
      <button type="button" class="secondary-btn" @click="search">{{ $t('ui.search') }}</button>
      <button type="button" class="refresh-btn" :disabled="loading" @click="load(pager.page)">↻ {{ $t('projectManagement.refresh') }}</button>
    </div>

    <div class="project-list-card">
      <div class="card-heading">
        <div><h3>{{ $t('projectManagement.listTitle') }}</h3><p>{{ $t('projectManagement.listHint') }}</p></div>
        <span>{{ $t('projectManagement.records', { count: pager.totalRows }) }}</span>
      </div>

      <div v-if="loading" class="project-empty">{{ $t('ui.loading') }}</div>
      <div v-else-if="error" class="project-empty error">{{ $lt(error) }} <button type="button" @click="load(pager.page)">{{ $t('ui.retry') }}</button></div>
      <div v-else class="table-wrap">
        <table>
          <colgroup><col class="code-col"><col class="name-col"><col class="location-col"><col class="address-col"><col class="units-col"><col class="owners-col"><col class="status-col"><col class="actions-col"></colgroup>
          <thead><tr>
            <th>{{ $t('projectManagement.code') }}</th>
            <th>{{ $t('projectManagement.name') }}</th>
            <th>{{ $t('projectManagement.location') }}</th>
            <th>{{ $t('projectManagement.address') }}</th>
            <th>{{ $t('projectManagement.unitCount') }}</th>
            <th>{{ $t('projectManagement.ownerCount') }}</th>
            <th>{{ $t('projectManagement.status') }}</th>
            <th>{{ $t('projectManagement.actions') }}</th>
          </tr></thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id">
              <td><span class="project-code">{{ row.projectCode }}</span></td>
              <td><strong>{{ row.name }}</strong><small>{{ $t('projectManagement.updatedAt') }} {{ dateTime(row.updatedAt) }}</small></td>
              <td><strong>{{ row.city || '—' }}</strong><small>{{ [row.state, row.countryCode].filter(Boolean).join(' · ') }}</small></td>
              <td class="address-cell">{{ row.address || '—' }}</td>
              <td><span class="count-badge">{{ row.unitCount }}</span></td>
              <td>{{ row.ownerCount }}</td>
              <td><span class="status-pill" :class="row.status">{{ row.status === 'active' ? $t('projectManagement.active') : $t('projectManagement.inactive') }}</span></td>
              <td><div class="project-row-actions">
                <button type="button" @click="openEdit(row)">{{ $t('projectManagement.edit') }}</button>
                <button type="button" class="status-action" @click="toggleStatus(row)">{{ row.status === 'active' ? $t('projectManagement.disable') : $t('projectManagement.enable') }}</button>
                <button type="button" class="danger" :disabled="row.unitCount > 0" :title="row.unitCount > 0 ? $t('projectManagement.deleteBlocked') : ''" @click="remove(row)">{{ $t('projectManagement.delete') }}</button>
              </div></td>
            </tr>
            <tr v-if="!rows.length"><td colspan="8" class="project-empty">{{ $t('projectManagement.noRows') }}</td></tr>
          </tbody>
        </table>
      </div>

      <footer v-if="!loading && !error" class="project-pager">
        <span>{{ $t('projectManagement.records', { count: pager.totalRows }) }} · {{ $t('projectManagement.pageSize') }}</span>
        <div>
          <button type="button" :disabled="pager.page <= 1" @click="go(pager.page - 1)">‹</button>
          <span>{{ pager.page }} / {{ pager.totalPages }}</span>
          <button type="button" :disabled="pager.page >= pager.totalPages" @click="go(pager.page + 1)">›</button>
        </div>
      </footer>
    </div>

    <div v-if="dialogOpen" class="dialog-backdrop" @pointerdown.self="closeDialog">
      <form class="project-dialog" @submit.prevent="save">
        <header><div><span class="eyebrow">{{ $t('projectManagement.formEyebrow') }}</span><h3>{{ editing ? $t('projectManagement.editTitle') : $t('projectManagement.createTitle') }}</h3><p>{{ $t('projectManagement.formHint') }}</p></div><button type="button" class="close-btn" @click="closeDialog">×</button></header>
        <div class="project-form">
          <label><span>{{ $t('projectManagement.code') }} *</span><input v-model.trim="projectForm.projectCode" required maxlength="40" :placeholder="$t('projectManagement.codePlaceholder')"></label>
          <label><span>{{ $t('projectManagement.name') }} *</span><input v-model.trim="projectForm.name" required maxlength="160" :placeholder="$t('projectManagement.namePlaceholder')"></label>
          <label><span>{{ $t('projectManagement.state') }}</span><select v-model="projectForm.state" @change="onStateChange"><option value="">{{ $t('projectManagement.statePlaceholder') }}</option><option v-for="location in malaysiaLocations" :key="location.state" :value="location.state">{{ location.state }}</option></select></label>
          <label><span>{{ $t('projectManagement.city') }}</span><select v-model="projectForm.city" :disabled="!projectForm.state"><option value="">{{ $t('projectManagement.cityPlaceholder') }}</option><option v-if="projectForm.city && !areaOptions.includes(projectForm.city)" :value="projectForm.city">{{ projectForm.city }}</option><option v-for="area in areaOptions" :key="area" :value="area">{{ area }}</option></select></label>
          <label class="wide"><span>{{ $t('projectManagement.detailedAddress') }}</span><input v-model.trim="projectForm.address" maxlength="255" :placeholder="$t('projectManagement.addressPlaceholder')"></label>
          <label class="wide"><span>{{ $t('projectManagement.status') }}</span><select v-model="projectForm.status"><option value="active">{{ $t('projectManagement.active') }}</option><option value="inactive">{{ $t('projectManagement.inactive') }}</option></select></label>
        </div>
        <p v-if="formError" class="form-error">{{ $lt(formError) }}</p>
        <footer><button type="button" class="secondary-btn" @click="closeDialog">{{ $t('ui.cancel') }}</button><button type="submit" class="primary-btn" :disabled="saving">{{ saving ? $t('ui.saving') : $t('projectManagement.save') }}</button></footer>
      </form>
    </div>
  </section>
</template>

<script>
import { Search } from '@lucide/vue';
import pageBridge from '../pageBridge';
import { findStateByArea, malaysiaLocations } from '../data/malaysiaLocations';
import { createAdminProject, deleteAdminProject, fetchAdminProjects, updateAdminProject } from '../services/propertyApi';

const emptyForm = () => ({ projectCode: '', name: '', address: '', state: '', city: '', countryCode: 'MY', status: 'active' });

export default {
  components: { Search },
  mixins: [pageBridge],
  data() {
    return {
      malaysiaLocations,
      keyword: '', status: '', rows: [], loading: false, error: '', saving: false,
      summary: { totalCount: 0, activeCount: 0, inactiveCount: 0, unitCount: 0 },
      pager: { totalRows: 0, page: 1, pageSize: 5, totalPages: 1 },
      dialogOpen: false, editing: null, projectForm: emptyForm(), formError: '', requestSerial: 0
    };
  },
  computed: {
    areaOptions() {
      return this.malaysiaLocations.find(location => location.state === this.projectForm.state)?.areas || [];
    }
  },
  mounted() { this.load(1); },
  methods: {
    async load(page = 1) {
      const serial = ++this.requestSerial;
      this.loading = true; this.error = '';
      try {
        const data = await fetchAdminProjects({ page, pageSize: 5, keyword: this.keyword, status: this.status });
        if (serial !== this.requestSerial) return;
        this.rows = data.rows || [];
        this.summary = data.summary || this.summary;
        this.pager = data.page || this.pager;
      } catch (error) {
        if (serial === this.requestSerial) this.error = error?.message || this.$t('projectManagement.loadFailed');
      } finally {
        if (serial === this.requestSerial) this.loading = false;
      }
    },
    search() { this.load(1); },
    go(page) { if (page >= 1 && page <= this.pager.totalPages) this.load(page); },
    openCreate() { this.editing = null; this.projectForm = emptyForm(); this.formError = ''; this.dialogOpen = true; },
    openEdit(row) { this.editing = row; this.projectForm = { projectCode: row.projectCode, name: row.name, address: row.address || '', state: row.state || findStateByArea(row.city), city: row.city || '', countryCode: row.countryCode || 'MY', status: row.status || 'active' }; this.formError = ''; this.dialogOpen = true; },
    closeDialog() { if (!this.saving) this.dialogOpen = false; },
    onStateChange() { this.projectForm.city = ''; },
    payload(source = this.projectForm) { return { projectCode: source.projectCode.trim().toUpperCase(), name: source.name.trim(), address: source.address?.trim() || null, state: source.state?.trim() || null, city: source.city?.trim() || null, countryCode: source.countryCode.trim().toUpperCase(), status: source.status }; },
    async save() {
      this.saving = true; this.formError = '';
      try {
        const payload = this.payload();
        if (this.editing) await updateAdminProject(this.editing.id, payload); else await createAdminProject(payload);
        this.dialogOpen = false;
        this.showToast(this.$t(this.editing ? 'projectManagement.updated' : 'projectManagement.created'));
        await this.load(this.editing ? this.pager.page : 1);
      } catch (error) {
        this.formError = error?.message || this.$t('projectManagement.saveFailed');
      } finally { this.saving = false; }
    },
    async toggleStatus(row) {
      try {
        await updateAdminProject(row.id, this.payload({ ...row, status: row.status === 'active' ? 'inactive' : 'active' }));
        this.showToast(this.$t('projectManagement.statusUpdated'));
        await this.load(this.pager.page);
      } catch (error) { this.showToast(error?.message || this.$t('projectManagement.saveFailed')); }
    },
    async remove(row) {
      if (row.unitCount > 0) { this.showToast(this.$t('projectManagement.deleteBlocked')); return; }
      if (!window.confirm(this.$t('projectManagement.deleteConfirm', { name: row.name }))) return;
      try {
        await deleteAdminProject(row.id);
        this.showToast(this.$t('projectManagement.deleted'));
        await this.load(this.rows.length === 1 && this.pager.page > 1 ? this.pager.page - 1 : this.pager.page);
      } catch (error) { this.showToast(error?.message || this.$t('projectManagement.deleteFailed')); }
    },
    dateTime(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '—'; }
  }
};
</script>

<style scoped>
.project-management{padding:20px 28px 32px;color:#123e70}.project-heading,.project-toolbar,.card-heading,.project-pager,.project-dialog header,.project-dialog footer{display:flex;align-items:center;justify-content:space-between;gap:14px}.project-heading h2{margin:3px 0 0;color:#073c78;font-size:24px;line-height:1.25}.project-heading p,.project-dialog header p{margin:6px 0 0;color:#6f86a1;font-size:13px}.eyebrow{color:#098c95;font-size:11px;font-weight:800;letter-spacing:.11em;text-transform:uppercase}.primary-btn,.secondary-btn,.refresh-btn{min-height:42px;padding:0 18px;border:1px solid #d1dfec;border-radius:8px;background:#fff;color:#123f70;font:inherit;font-weight:700;cursor:pointer}.primary-btn{border-color:#e5a600;background:linear-gradient(135deg,#f4bb09,#e6a000);color:#fff;box-shadow:0 8px 18px rgba(219,151,0,.18)}.project-metrics{display:grid;grid-template-columns:repeat(4,minmax(150px,1fr));gap:14px;margin:20px 0}.project-metrics article{position:relative;overflow:hidden;padding:17px 19px;background:#fff;border:1px solid #d8e5f0;border-radius:12px;box-shadow:0 8px 24px rgba(22,65,98,.05)}.project-metrics article::before{content:"";position:absolute;inset:0 auto 0 0;width:4px;background:#0b9da3}.project-metrics span,.project-metrics small{display:block;color:#516f8d;font-size:12px}.project-metrics strong{display:block;margin:5px 0 2px;color:#073d77;font-size:27px}.project-metrics .good{color:#14945a}.project-metrics .accent{color:#d58c00}.project-toolbar{justify-content:flex-start;padding:13px 14px;margin-bottom:14px;background:#fff;border:1px solid #d8e5f0;border-radius:11px}.search-box{display:flex;align-items:center;gap:8px;flex:1;min-width:260px;padding:0 13px;border:1px solid #cfddec;border-radius:8px}.search-box input{width:100%;padding:11px 0;border:0;outline:0;color:#103e70;font:inherit}.project-toolbar select{width:220px;padding:11px;border:1px solid #cfddec;border-radius:8px;background:#fff;color:#103e70;font:inherit}.refresh-btn{margin-left:auto}.project-list-card{overflow:hidden;background:#fff;border:1px solid #d8e5f0;border-radius:12px;box-shadow:0 12px 28px rgba(22,65,98,.06)}.card-heading{padding:15px 18px;background:#fff;border-bottom:1px solid #dce7f1}.card-heading h3{margin:0;color:#073f76;font-size:17px}.card-heading p{margin:4px 0 0;color:#587590;font-size:12px}.card-heading>span{color:#506d89;font-size:13px}.table-wrap{overflow-x:auto;background:#fff}.project-list-card table{width:100%;min-width:1120px;table-layout:fixed;border-collapse:collapse;background:#fff;font-size:14px}.project-list-card .code-col{width:10%}.project-list-card .name-col{width:18%}.project-list-card .location-col{width:15%}.project-list-card .address-col{width:19%}.project-list-card .units-col,.project-list-card .owners-col{width:8%}.project-list-card .status-col{width:7%}.project-list-card .actions-col{width:15%}.project-list-card th{padding:13px 14px;text-align:left;white-space:nowrap;background:#eef5f8;color:#294d68;font-size:12px;font-weight:800}.project-list-card td{min-width:0;padding:13px 14px;border-top:1px solid #e3ebf3;color:#173f6c;vertical-align:middle}.project-list-card tbody tr:hover{background:#fff9eb}.project-list-card td strong,.project-list-card td small{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.project-list-card td small{margin-top:4px;color:#6d849c;font-size:11px}.project-code{display:inline-block;max-width:100%;padding:5px 9px;overflow:hidden;border-radius:7px;background:#edf7f8;color:#087d84;font-weight:800;letter-spacing:.02em;text-overflow:ellipsis;white-space:nowrap}.address-cell{max-width:0;overflow:hidden;color:#58728f!important;text-overflow:ellipsis;white-space:nowrap}.count-badge{display:inline-grid;place-items:center;min-width:26px;height:26px;padding:0 6px;border-radius:13px;background:#eef5ff;color:#1461a0;font-weight:800}.status-pill{display:inline-block;padding:5px 10px;border-radius:999px;font-size:12px;font-weight:700}.status-pill.active{background:#e7f8ee;color:#14834c}.status-pill.inactive{background:#f0f3f6;color:#6c7e90}.project-list-card th:last-child,.project-list-card td:last-child{min-width:174px;text-align:right}.project-row-actions{display:flex;align-items:center;justify-content:flex-end;gap:6px;width:100%;height:auto;min-width:0;padding:0;border:0;background:transparent;box-shadow:none;white-space:nowrap}.project-row-actions button{flex:0 0 auto;min-width:0;padding:6px 8px;border:1px solid #c9dbea;border-radius:6px;background:#fff;color:#0c4b82;cursor:pointer}.project-row-actions .status-action{border-color:#ecd08a;color:#9c6900;background:#fffaf0}.project-row-actions .danger{border-color:#f0c6c6;color:#d14747}.project-row-actions button:disabled{cursor:not-allowed;opacity:.42}.project-empty{padding:64px 20px;background:#fff;text-align:center;color:#4e6b86}.project-empty.error{color:#c84a48}.project-empty button{margin-left:8px}.project-pager{padding:12px 15px;background:#fbfdfd;border-top:1px solid #dce7f1;color:#496982;font-size:13px}.project-pager div{display:flex;align-items:center;gap:9px}.project-pager button{width:36px;height:34px;border:1px solid #cfdeeb;border-radius:7px;background:#fff;color:#124b7e;cursor:pointer}.project-pager button:disabled,.primary-btn:disabled,.refresh-btn:disabled{opacity:.5;cursor:not-allowed}.dialog-backdrop{position:fixed;inset:0;z-index:40;display:grid;place-items:center;padding:18px;background:rgba(8,31,53,.45);backdrop-filter:blur(2px)}.project-dialog{width:min(720px,100%);overflow:hidden;background:#fff;border-radius:14px;box-shadow:0 24px 70px rgba(3,27,51,.28)}.project-dialog header{padding:20px 24px;border-bottom:1px solid #dee8f1}.project-dialog h3{margin:4px 0 0;color:#073c78;font-size:21px}.close-btn{align-self:flex-start;width:36px;height:36px;border:1px solid #cbdbea;border-radius:8px;background:#fff;color:#164b7b;font-size:23px;cursor:pointer}.project-form{display:grid;grid-template-columns:1fr 1fr;gap:17px;padding:24px}.project-form label{display:grid;gap:7px;color:#345a7a;font-size:13px;font-weight:700}.project-form label small{color:#7c91a5;font-size:11px;font-weight:400}.project-form .wide{grid-column:1/-1}.project-form input,.project-form select{width:100%;box-sizing:border-box;padding:11px 12px;border:1px solid #cadbea;border-radius:8px;background:#fff;color:#123f70;font:inherit}.project-form input:focus,.project-form select:focus{outline:2px solid rgba(12,151,158,.16);border-color:#0c979e}.form-error{margin:-8px 24px 18px;padding:10px 12px;border-radius:7px;background:#fff0f0;color:#c84040;font-size:13px}.project-dialog footer{justify-content:flex-end;padding:15px 24px;border-top:1px solid #dee8f1}@media(max-width:900px){.project-management{padding:15px}.project-metrics{grid-template-columns:1fr 1fr}.project-toolbar{flex-wrap:wrap}.search-box{flex-basis:100%}.refresh-btn{margin-left:0}.project-heading{align-items:flex-start}}@media(max-width:600px){.project-metrics,.project-form{grid-template-columns:1fr}.project-form .wide{grid-column:auto}.project-heading{flex-direction:column}.project-heading .primary-btn{width:100%}}
</style>
