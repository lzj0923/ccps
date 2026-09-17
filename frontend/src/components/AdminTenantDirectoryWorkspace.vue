<template>
  <section class="tenant-directory">
    <header class="directory-heading">
      <div><h2>{{ $t('tenantDirectory.title') }}</h2><p>{{ $t('tenantDirectory.subtitle') }}</p></div>
      <button type="button" class="primary-btn" @click="openCreate"><Plus :size="17" aria-hidden="true" />{{ $t('tenantDirectory.add') }}</button>
    </header>

    <div class="directory-metrics">
      <article><span>{{ $t('tenantDirectory.all') }}</span><strong>{{ summary.totalCount }}</strong></article>
      <article><span>{{ $t('tenantDirectory.active') }}</span><strong class="good">{{ summary.activeCount }}</strong></article>
      <article><span>{{ $t('tenantDirectory.inactive') }}</span><strong>{{ summary.inactiveCount }}</strong></article>
      <article><span>{{ $t('tenantDirectory.withLease') }}</span><strong class="accent">{{ summary.activeLeaseTenantCount }}</strong></article>
    </div>

    <div class="directory-filter">
      <label class="tenant-directory-search"><Search :size="17" aria-hidden="true" /><input v-model.trim="keyword" class="tenant-directory-search-input" :placeholder="$t('tenantDirectory.search')" @keyup.enter="search"></label>
      <select v-model="status" @change="search"><option value="">{{ $t('tenantDirectory.allStatus') }}</option><option value="active">{{ $t('tenantDirectory.active') }}</option><option value="inactive">{{ $t('tenantDirectory.inactive') }}</option></select>
      <button type="button" class="ghost-btn" @click="search">{{ $t('ui.search') }}</button>
    </div>

    <div class="directory-card">
      <div v-if="loading" class="directory-empty">{{ $t('ui.loading') }}</div>
      <div v-else-if="error" class="directory-empty error">{{ $lt(error) }} <button type="button" @click="load">{{ $t('ui.retry') }}</button></div>
      <template v-else>
        <div class="directory-table-wrap"><table>
          <colgroup><col class="tenant-col"><col class="identity-col"><col class="contact-col"><col class="lease-col"><col class="deposit-col"><col class="history-col"><col class="status-col"><col class="actions-col"></colgroup>
          <thead><tr><th scope="col">{{ $t('tenantDirectory.tenant') }}</th><th scope="col">{{ $t('tenantDirectory.identity') }}</th><th scope="col">{{ $t('tenantDirectory.contact') }}</th><th scope="col">{{ $t('tenantDirectory.currentLease') }}</th><th scope="col">{{ $t('tenantDirectory.depositBalance') }}</th><th scope="col">{{ $t('tenantDirectory.history') }}</th><th scope="col">{{ $t('tenantDirectory.status') }}</th><th scope="col">{{ $t('tenantDirectory.actions') }}</th></tr></thead>
          <tbody>
            <tr v-for="row in rows" :key="row.tenantId" :class="{ selected: selected?.tenantId === row.tenantId }" @click="selected = row">
              <td><div class="tenant-profile"><span class="tenant-avatar" aria-hidden="true"><UserRound :size="18" /></span><div><strong>{{ row.fullName }}</strong><small>{{ row.status === 'active' ? $t('tenantDirectory.active') : $t('tenantDirectory.inactive') }}</small></div></div></td>
              <td><span class="identity-value">{{ row.identityNo || '—' }}</span></td>
              <td><div class="directory-cell-stack"><span>{{ row.phone || '—' }}</span><small>{{ row.email || '—' }}</small><em class="whatsapp-state" :class="{ enabled: row.whatsappEnabled }">{{ row.whatsappEnabled ? $t('tenantDirectory.whatsappEnabledLabel') : $t('tenantDirectory.whatsappDisabledLabel') }}</em></div></td>
              <td><div v-if="row.currentLeaseNo" class="directory-cell-stack lease-cell"><strong>{{ row.currentLeaseNo }}</strong><small>{{ row.projectName }} · {{ row.unitNo }}</small></div><span v-else class="muted">{{ $t('tenantDirectory.noLease') }}</span></td>
              <td class="deposit-cell"><template v-if="row.currentLeaseNo"><strong>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(row.currentDepositBalance) }}</strong><small :class="`deposit-status ${row.currentDepositStatus || 'unrecorded'}`">{{ $lt(depositCollectionStatusLabel(row.currentDepositStatus)) }}</small></template><span v-else class="muted">—</span></td>
              <td>{{ $t('tenantDirectory.records', { count: row.leaseCount }) }}</td>
              <td><span class="status-pill" :class="row.status">{{ row.status === 'active' ? $t('tenantDirectory.active') : $t('tenantDirectory.inactive') }}</span></td>
              <td><div class="tenant-row-actions"><button type="button" class="detail-action" @click.stop="openDetails(row)">{{ $t('tenantDirectory.details') }}</button><button type="button" class="icon-action" :aria-label="$t('tenantDirectory.edit')" :title="$t('tenantDirectory.edit')" @click.stop="openEdit(row)"><Pencil :size="15" aria-hidden="true" /></button><button type="button" class="icon-action status-action" :aria-label="row.status === 'active' ? $t('tenantDirectory.disable') : $t('tenantDirectory.enable')" :title="row.status === 'active' ? $t('tenantDirectory.disable') : $t('tenantDirectory.enable')" @click.stop="toggleStatus(row)"><PauseCircle v-if="row.status === 'active'" :size="16" aria-hidden="true" /><PlayCircle v-else :size="16" aria-hidden="true" /></button><button type="button" class="icon-action danger" :aria-label="$t('tenantDirectory.delete')" :title="$t('tenantDirectory.delete')" @click.stop="remove(row)"><Trash2 :size="15" aria-hidden="true" /></button></div></td>
            </tr>
            <tr v-if="!rows.length"><td colspan="8" class="directory-empty">{{ $t('tenantDirectory.noRows') }}</td></tr>
          </tbody>
        </table></div>
        <footer class="directory-pager"><span>{{ $t('tenantDirectory.records', { count: pager.totalRows }) }}</span><div><button type="button" :aria-label="$t('tenantDirectory.previousPage')" :disabled="pager.page <= 1" @click="go(pager.page - 1)"><ChevronLeft :size="15" aria-hidden="true" /></button><span>{{ pager.page }} / {{ pager.totalPages }}</span><button type="button" :aria-label="$t('tenantDirectory.nextPage')" :disabled="pager.page >= pager.totalPages" @click="go(pager.page + 1)"><ChevronRight :size="15" aria-hidden="true" /></button></div></footer>
      </template>
    </div>

    <div v-if="dialogOpen" class="tenant-dialog-backdrop" @pointerdown.self="closeDialog"><form class="tenant-dialog" @submit.prevent="save">
      <header><div><h3>{{ editing ? $t('tenantDirectory.editTitle') : $t('tenantDirectory.createTitle') }}</h3><p>{{ $t('tenantDirectory.subtitle') }}</p></div><button type="button" class="close" :aria-label="$t('tenantDirectory.close')" @click="closeDialog"><X :size="18" aria-hidden="true" /></button></header>
      <div class="tenant-form"><label><span>{{ $t('tenantDirectory.fullName') }} *</span><input v-model.trim="form.fullName" required maxlength="160"></label><label><span>{{ $t('tenantDirectory.identity') }}</span><input v-model.trim="form.identityNo" maxlength="120"></label><label class="phone-field"><span>{{ $t('tenantDirectory.phone') }}</span><div class="phone-entry"><select v-model="form.phoneCountry" :aria-label="$t('tenantDirectory.countryRegion')" @change="validatePhone(true)"><option v-for="country in phoneCountries" :key="country.code" :value="country.code">{{ $regionName(country.code) }} {{ country.dialCode }}</option></select><input v-model.trim="form.phoneNational" type="tel" inputmode="tel" autocomplete="tel-national" maxlength="30" :placeholder="selectedPhoneCountry.example" @input="phoneError = ''" @blur="validatePhone(true)"></div><small>{{ $t('tenantDirectory.phoneHint') }}</small><small v-if="phoneError" class="phone-error" role="alert">{{ $lt(phoneError) }}</small></label><label><span>{{ $t('tenantDirectory.email') }}</span><input v-model.trim="form.email" type="email" maxlength="190"></label><label><span>{{ $t('tenantDirectory.status') }}</span><select v-model="form.status"><option value="active">{{ $t('tenantDirectory.active') }}</option><option value="inactive">{{ $t('tenantDirectory.inactive') }}</option></select></label><label class="whatsapp-toggle"><input v-model="form.whatsappEnabled" type="checkbox"><span><b>{{ $t('tenantDirectory.whatsappAutomation') }}</b><small>{{ $t('tenantDirectory.whatsappAutomationHint') }}</small></span></label><p v-if="form.whatsappEnabled" class="whatsapp-consent">{{ $t('tenantDirectory.whatsappConsent') }}</p></div>
      <footer><button type="button" class="ghost-btn" @click="closeDialog">{{ $t('tenantDirectory.cancel') }}</button><button type="submit" class="primary-btn" :disabled="saving">{{ saving ? (editing ? $t('tenantDirectory.saving') : $t('tenantDirectory.creating')) : $t('tenantDirectory.save') }}</button></footer>
    </form></div>

    <div v-if="detailOpen" class="tenant-dialog-backdrop" @pointerdown.self="closeDetails"><section class="tenant-dialog tenant-detail-dialog">
      <header><div><h3>{{ $t('tenantDirectory.detailTitle') }}</h3><p>{{ detailRow?.fullName }} · {{ detailRow?.identityNo || '—' }}</p></div><button type="button" class="close" :aria-label="$t('tenantDirectory.close')" @click="closeDetails"><X :size="18" aria-hidden="true" /></button></header>
      <div class="tenant-detail-body">
        <div class="tenant-overview-grid"><article class="attention"><span>{{ $t('tenantDirectory.unpaidRent') }}</span><strong>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(detailOverview.unpaidRent) }}</strong></article><article :class="{ attention: detailOverview.pendingMaintenanceCount }"><span>{{ $t('tenantDirectory.pendingMaintenance') }}</span><strong>{{ detailOverview.pendingMaintenanceCount }}</strong></article><article :class="{ attention: detailOverview.pendingSignatureCount }"><span>{{ $t('tenantDirectory.pendingSignature') }}</span><strong>{{ detailOverview.pendingSignatureCount }}</strong></article><article class="deposit-balance"><span>{{ $t('tenantDirectory.depositBalance') }}</span><strong>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(detailOverview.depositBalance) }}</strong></article></div>
        <h4>{{ $t('tenantDirectory.leaseDetails') }}</h4>
        <p v-if="detailLoading" class="detail-empty">{{ $t('ui.loading') }}</p>
        <p v-else-if="!leaseHistory.length" class="detail-empty">{{ $t('tenantDirectory.noLeaseHistory') }}</p>
<div v-else class="tenant-lease-list"><article v-for="lease in leaseHistory" :key="lease.leaseId"><div class="lease-head"><strong>{{ lease.leaseNo }}</strong><span class="status-pill" :class="lease.status === 'active' ? 'active' : 'inactive'">{{ $lt(leaseStatusLabel(lease.status)) }}</span></div><p>{{ lease.projectName }} · {{ lease.unitNo }}</p><div class="lease-workflow"><span class="workflow-label">{{ $t('tenantDirectory.workflow') }}</span><strong>{{ $lt(workflowLabel(lease.workflowStep)) }}</strong><small>{{ $lt(signatureLabel(lease.signatureStatus)) }}</small></div><dl><div><dt>{{ $t('tenantDirectory.leasePeriod') }}</dt><dd>{{ displayDate(lease.startDate) }} ~ {{ displayDate(lease.endDate) }}</dd></div><div><dt>{{ $t('tenantDirectory.rent') }}</dt><dd>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(lease.monthlyRent) }}</dd></div><div><dt>{{ $t('tenantDirectory.unpaidRent') }}</dt><dd :class="{ overdue: lease.unpaidRent > 0 }">{{ $t('legacy.t_5e7b60c626a4') }} {{ money(lease.unpaidRent) }}</dd></div><div><dt>{{ $t('tenantDirectory.pendingMaintenance') }}</dt><dd :class="{ overdue: lease.pendingMaintenanceCount > 0 }">{{ lease.pendingMaintenanceCount }}</dd></div></dl><div class="lease-card-actions"><button type="button" class="lease-workbench-btn" @click="openLeaseWorkbench(lease)">{{ $t('tenantDirectory.openLeaseWorkbench') }}</button></div></article></div>
      </div>
      <footer><button type="button" class="ghost-btn" @click="closeDetails">{{ $t('tenantDirectory.cancel') }}</button></footer>
    </section></div>

  </section>
</template>

<script>
import { ChevronLeft, ChevronRight, PauseCircle, Pencil, PlayCircle, Plus, Search, Trash2, UserRound, X } from '@lucide/vue';
import { formatDate } from '../utils/dateFormat';
import { navigate } from '../router';
import { createAdminTenant, deleteAdminTenant, fetchAdminTenantDetail, fetchAdminTenantDirectory, updateAdminTenant, updateAdminTenantStatus } from '../services/propertyApi';
import { splitTenantPhone, TENANT_PHONE_COUNTRIES, tenantPhoneCountry, validateTenantPhone } from '../utils/tenantPhone';
const blank = () => ({ fullName: '', identityNo: '', phoneCountry: 'MY', phoneNational: '', email: '', status: 'active', whatsappEnabled: true });
export default {
  components: { ChevronLeft, ChevronRight, PauseCircle, Pencil, PlayCircle, Plus, Search, Trash2, UserRound, X },
  inject: ['page'],
  data() { return { rows: [], summary: { totalCount: 0, activeCount: 0, inactiveCount: 0, activeLeaseTenantCount: 0 }, pager: { page: 1, pageSize: 5, totalRows: 0, totalPages: 1 }, keyword: '', status: '', loading: false, error: '', selected: null, dialogOpen: false, editing: null, form: blank(), phoneCountries: TENANT_PHONE_COUNTRIES, phoneError: '', saving: false, detailOpen: false, detailRow: null, detailLoading: false, detailOverview: { unpaidRent: 0, pendingMaintenanceCount: 0, pendingSignatureCount: 0, depositBalance: 0 }, leaseHistory: [] }; },
  computed: { selectedPhoneCountry() { return tenantPhoneCountry(this.form.phoneCountry); } },
  mounted() { this.applyProcessRoute(); },
  methods: {
    displayDate(value) { return formatDate(value); },
    async applyProcessRoute() { const params = new URLSearchParams(window.location.search); const workflow = params.get('workflow'); const tenantId = params.get('tenantId'); if (workflow === 'edit' && params.get('keyword')) this.keyword = params.get('keyword'); await this.load(1); if (workflow !== 'edit' || !tenantId) return; let tenant = this.rows.find(row => String(row.tenantId) === String(tenantId)); if (!tenant) { const data = await fetchAdminTenantDirectory({ page: 1, pageSize: 200, keyword: this.keyword, status: this.status }); tenant = (data.rows || []).find(row => String(row.tenantId) === String(tenantId)); } if (tenant) this.openEdit(tenant); },
    async load(page = this.pager.page) { this.loading = true; this.error = ''; try { const data = await fetchAdminTenantDirectory({ page, pageSize: this.pager.pageSize, keyword: this.keyword, status: this.status }); this.rows = data.rows || []; this.summary = data.summary || this.summary; this.pager = data.page || this.pager; this.selected = this.rows.find((item) => item.tenantId === this.selected?.tenantId) || this.rows[0] || null; } catch (error) { this.error = error?.message || this.$t('tenantDirectory.loadFailed'); } finally { this.loading = false; } },
    search() { this.load(1); }, go(page) { this.load(page); },
    async openDetails(row) { this.detailOpen = true; this.detailRow = row; this.leaseHistory = []; this.detailOverview = { unpaidRent: 0, pendingMaintenanceCount: 0, pendingSignatureCount: 0, depositBalance: 0 }; this.detailLoading = true; try { const data = await fetchAdminTenantDetail(row.tenantId); this.leaseHistory = data.leases || []; this.detailOverview = data.overview || this.detailOverview; } catch (error) { this.page?.showToast?.(error?.message || this.$t('tenantDirectory.loadFailed'), 'error'); } finally { this.detailLoading = false; } },
    closeDetails() { this.detailOpen = false; this.detailRow = null; this.leaseHistory = []; },
    openCreate() { this.editing = null; this.form = blank(); this.phoneError = ''; this.dialogOpen = true; },
    openEdit(row) { const phone = splitTenantPhone(row.phone || ''); this.editing = row; this.form = { fullName: row.fullName || '', identityNo: row.identityNo || '', phoneCountry: phone.country, phoneNational: phone.nationalNumber, email: row.email || '', status: row.status || 'active', whatsappEnabled: Boolean(row.whatsappEnabled) }; this.phoneError = ''; this.dialogOpen = true; },
    closeDialog() { if (!this.saving) this.dialogOpen = false; },
    validatePhone(showError = false) { const result = validateTenantPhone(this.form.phoneNational, this.form.phoneCountry, false); if (showError) { if (result.reason === 'required') this.phoneError = this.$t('tenantDirectory.phoneRequired'); else if (result.reason === 'country_mismatch') this.phoneError = this.$t('tenantDirectory.phoneCountryMismatch', { country: this.selectedPhoneCountry.label }); else if (result.reason === 'invalid') this.phoneError = this.$t('tenantDirectory.phoneInvalid', { country: this.selectedPhoneCountry.label, example: this.selectedPhoneCountry.example }); else this.phoneError = ''; } else if (result.valid) this.phoneError = ''; return result; },
    async save() { const phone = this.validatePhone(true); if (!phone.valid) return; this.saving = true; try { const tenantPayload = { fullName: this.form.fullName, identityNo: this.form.identityNo, phone: phone.e164, email: this.form.email, status: this.form.status, whatsappEnabled: !this.editing || this.form.whatsappEnabled !== Boolean(this.editing.whatsappEnabled) ? this.form.whatsappEnabled : null }; if (this.editing) await updateAdminTenant(this.editing.tenantId, tenantPayload); else await createAdminTenant(tenantPayload); this.dialogOpen = false; this.page?.showToast?.(this.$t('tenantDirectory.saved')); await this.load(this.editing ? this.pager.page : 1); } catch (error) { this.page?.showToast?.(error?.message || this.$t('tenantDirectory.loadFailed'), 'error'); } finally { this.saving = false; } },
    async toggleStatus(row) { try { await updateAdminTenantStatus(row.tenantId, row.status === 'active' ? 'inactive' : 'active'); await this.load(); } catch (error) { this.page?.showToast?.(error?.message || this.$t('tenantDirectory.loadFailed'), 'error'); } },
    async remove(row) { if (row.leaseCount > 0) { this.page?.showToast?.(this.$t('tenantDirectory.deleteBlocked'), 'error'); return; } if (!window.confirm(this.$t('tenantDirectory.deleteConfirm', { name: row.fullName }))) return; try { await deleteAdminTenant(row.tenantId); this.page?.showToast?.(this.$t('tenantDirectory.deleted')); await this.load(this.rows.length === 1 && this.pager.page > 1 ? this.pager.page - 1 : this.pager.page); } catch (error) { this.page?.showToast?.(error?.message || this.$t('tenantDirectory.deleteFailed'), 'error'); } },
    money(value) { return Number(value || 0).toLocaleString('en-MY', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); },
    workflowLabel(value) { return this.$t(`tenantDirectory.workflow_${value || 'contract_pending'}`); },
    leaseStatusLabel(value) { return this.$t(`tenantDirectory.leaseStatus_${value || 'active'}`); },
    openLeaseWorkbench(lease) { if (!lease?.unitId || !lease?.leaseId) return; navigate(`/admin/properties/${encodeURIComponent(lease.unitId)}?tab=rentalManagement&rentalTab=lease&leaseId=${encodeURIComponent(lease.leaseId)}`); },
    signatureLabel(value) { return this.$t(`tenantDirectory.signature_${value || 'not_generated'}`); },
    depositCollectionStatusLabel(value) { return this.$t(`tenantDirectory.deposit_${value || 'unrecorded'}`); },
  }
};
</script>

<style scoped>
/* Hallmark · pre-emit critique: P5 H5 E5 S5 R5 V4 · genre: modern-minimal · macrostructure: operational directory · tokens: CCPS admin · contrast: pass (40–41) */
.tenant-directory {
  padding: var(--space-admin-md) var(--space-admin-lg) var(--space-admin-xl);
  color: var(--color-admin-ink);
}

.directory-heading,
.directory-filter,
.directory-pager,
.directory-pager > div,
.tenant-dialog header,
.tenant-dialog footer,
.lease-head,
.lease-card-actions {
  display: flex;
  align-items: center;
}

.directory-heading,
.directory-pager,
.tenant-dialog header,
.tenant-dialog footer,
.lease-head {
  justify-content: space-between;
}

.directory-heading,
.directory-filter,
.directory-pager,
.tenant-dialog header,
.tenant-dialog footer {
  gap: var(--space-admin-sm);
}

.directory-heading h2,
.tenant-dialog h3 {
  margin: 0;
  color: var(--color-admin-ink-strong);
  font-family: var(--font-admin-display);
}

.directory-heading h2 {
  font-size: var(--text-admin-title);
  line-height: 1.2;
}

.directory-heading p,
.tenant-dialog p {
  margin: var(--space-admin-xs) 0 0;
  color: var(--color-admin-muted);
  font-size: var(--text-admin-caption);
}

.primary-btn,
.ghost-btn,
.tenant-row-actions button,
.directory-pager button,
.close,
.lease-workbench-btn,
.directory-empty button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-admin-xs);
  min-height: var(--admin-control-height);
  border: 1px solid var(--color-admin-rule-strong);
  border-radius: var(--radius-admin-control);
  background: var(--color-admin-paper);
  color: var(--color-admin-ink);
  font: inherit;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
  cursor: pointer;
  transition: background-color var(--dur-admin-micro) var(--ease-admin-out), border-color var(--dur-admin-micro) var(--ease-admin-out), color var(--dur-admin-micro) var(--ease-admin-out);
}

.primary-btn {
  padding: 0 var(--space-admin-md);
  border-color: var(--color-admin-accent);
  background: var(--color-admin-accent);
  color: var(--color-admin-on-accent);
}

.primary-btn:hover,
.tenant-row-actions .detail-action:hover {
  border-color: var(--color-admin-accent-hover);
  background: var(--color-admin-accent-hover);
}

.ghost-btn:hover,
.tenant-row-actions button:hover,
.directory-pager button:hover,
.close:hover,
.lease-workbench-btn:hover,
.directory-empty button:hover {
  border-color: var(--color-admin-accent);
  background: var(--color-admin-accent-soft);
  color: var(--color-admin-accent-hover);
}

.primary-btn:focus-visible,
.ghost-btn:focus-visible,
.tenant-row-actions button:focus-visible,
.directory-pager button:focus-visible,
.close:focus-visible,
.lease-workbench-btn:focus-visible,
.directory-empty button:focus-visible,
.directory-filter select:focus-visible,
.tenant-form input:focus-visible,
.tenant-form select:focus-visible {
  outline: 2px solid var(--color-admin-focus);
  outline-offset: 1px;
}

.primary-btn:active,
.tenant-row-actions .detail-action:active {
  background: var(--color-admin-accent-hover);
}

.ghost-btn:active,
.tenant-row-actions button:active,
.directory-pager button:active,
.close:active,
.lease-workbench-btn:active,
.directory-empty button:active {
  background: var(--color-admin-surface-muted);
}

.primary-btn:disabled,
.ghost-btn:disabled,
.tenant-row-actions button:disabled,
.directory-pager button:disabled,
.close:disabled,
.lease-workbench-btn:disabled {
  opacity: .55;
  cursor: not-allowed;
}

.directory-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-admin-sm);
  margin: var(--space-admin-md) 0;
}

.directory-metrics article,
.directory-card {
  border: 1px solid var(--color-admin-rule);
  border-radius: var(--radius-admin-panel);
  background: var(--color-admin-paper);
  box-shadow: var(--shadow-admin-panel);
}

.directory-metrics article {
  min-width: 0;
  padding: var(--space-admin-sm) var(--space-admin-md);
}

.directory-metrics span {
  display: block;
  color: var(--color-admin-muted);
  font-size: var(--text-admin-caption);
}

.directory-metrics strong {
  display: block;
  margin-top: var(--space-admin-xs);
  color: var(--color-admin-ink-strong);
  font-size: var(--text-admin-title);
  line-height: 1;
}

.directory-metrics .good { color: var(--color-admin-success); }
.directory-metrics .accent { color: var(--color-admin-alert); }

.directory-filter {
  justify-content: flex-start;
  margin-bottom: var(--space-admin-sm);
}

.directory-filter label {
  display: flex;
  align-items: center;
  gap: var(--space-admin-xs);
  width: min(32rem, 100%);
  min-height: var(--admin-control-height);
  padding: 0 var(--space-admin-sm);
  border: 1px solid var(--color-admin-rule-strong);
  border-radius: var(--radius-admin-control);
  background: var(--color-admin-paper);
  color: var(--color-admin-muted);
}

.directory-filter label:focus-within {
  border-color: var(--color-admin-focus);
  outline: 2px solid var(--color-admin-focus);
  outline-offset: 1px;
}

.directory-filter input,
.directory-filter select,
.tenant-form input,
.tenant-form select {
  box-sizing: border-box;
  width: 100%;
  min-height: var(--admin-control-height);
  border: 1px solid var(--color-admin-rule-strong);
  border-radius: var(--radius-admin-control);
  background: var(--color-admin-paper);
  color: var(--color-admin-ink);
  font: inherit;
}

.tenant-directory .directory-filter > .tenant-directory-search > .tenant-directory-search-input {
  min-width: 0;
  min-height: 0;
  padding: 0;
  border: 0 !important;
  border-radius: 0 !important;
  background: transparent !important;
  box-shadow: none !important;
  outline: 0;
}

.directory-filter select {
  width: 13rem;
  padding: 0 var(--space-admin-sm);
}

.directory-card {
  max-width: 100%;
  overflow: hidden;
}

.directory-table-wrap {
  max-width: 100%;
  overflow-x: auto;
}

.directory-card table {
  width: 100%;
  min-width: 73rem;
  border-collapse: collapse;
  table-layout: fixed;
  font-size: var(--text-admin-body);
}

.tenant-col { width: 10.5rem; }
.identity-col { width: 10rem; }
.contact-col { width: 15.5rem; }
.lease-col { width: 16rem; }
.deposit-col { width: 10.5rem; }
.history-col { width: 6.5rem; }
.status-col { width: 6.5rem; }
.actions-col { width: 11rem; }

.directory-card th {
  height: var(--admin-control-height);
  padding: 0 var(--space-admin-sm);
  border-bottom: 1px solid var(--color-admin-rule);
  background: var(--color-admin-surface);
  color: var(--color-admin-muted);
  font-size: var(--text-admin-caption);
  font-weight: 800;
  text-align: left;
  white-space: nowrap;
}

.directory-card td {
  height: 4.75rem;
  padding: var(--space-admin-sm);
  border-bottom: 1px solid var(--color-admin-rule);
  color: var(--color-admin-ink);
  vertical-align: middle;
}

.directory-card tbody tr {
  cursor: pointer;
  transition: background-color var(--dur-admin-micro) var(--ease-admin-out);
}

.directory-card tbody tr:hover { background: var(--color-admin-surface); }
.directory-card tbody tr.selected { background: var(--color-admin-accent-soft); }
.directory-card tbody tr:last-child td { border-bottom: 0; }
.directory-card tbody tr.selected .tenant-profile strong { color: var(--color-admin-accent-hover); }

.tenant-profile {
  display: flex;
  align-items: center;
  gap: var(--space-admin-sm);
  min-width: 0;
}

.tenant-profile > div,
.directory-cell-stack {
  min-width: 0;
}

.tenant-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.25rem;
  height: 2.25rem;
  flex: 0 0 2.25rem;
  border-radius: var(--radius-admin-control);
  background: var(--color-admin-accent-soft);
  color: var(--color-admin-accent);
}

.directory-card strong,
.directory-card small,
.directory-cell-stack > span {
  display: block;
}

.tenant-profile strong,
.lease-cell strong,
.deposit-cell strong,
.identity-value,
.directory-cell-stack > span,
.directory-cell-stack small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tenant-profile strong,
.lease-cell strong {
  color: var(--color-admin-ink-strong);
  font-weight: 800;
}

.directory-card small,
.muted {
  margin-top: var(--space-admin-2xs, .25rem);
  color: var(--color-admin-muted);
  font-size: var(--text-admin-caption);
}

.identity-value { color: var(--color-admin-muted); }

.whatsapp-state {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  max-width: 100%;
  margin-top: var(--space-admin-xs);
  padding: .25rem .5rem;
  overflow: hidden;
  border-radius: var(--radius-native-pill);
  background: var(--color-admin-surface-muted);
  color: var(--color-admin-muted);
  font-size: var(--text-admin-caption);
  font-style: normal;
  line-height: 1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.whatsapp-state.enabled {
  background: var(--color-admin-success-soft);
  color: var(--color-admin-success);
}

.deposit-cell { white-space: nowrap; }
.deposit-cell strong { color: var(--color-admin-accent); }
.deposit-status.pending { color: var(--color-admin-alert); }
.deposit-status.confirmed,
.deposit-status.posted { color: var(--color-admin-success); }
.deposit-status.rejected { color: var(--color-admin-danger); }

.status-pill {
  display: inline-flex;
  align-items: center;
  padding: .25rem .5rem;
  border-radius: var(--radius-native-pill);
  font-size: var(--text-admin-caption);
  line-height: 1.25;
  white-space: nowrap;
}

.status-pill.active {
  background: var(--color-admin-success-soft);
  color: var(--color-admin-success);
}

.status-pill.inactive {
  background: var(--color-admin-surface-muted);
  color: var(--color-admin-muted);
}

.tenant-row-actions {
  display: flex;
  align-items: center;
  gap: var(--space-admin-xs);
  min-width: 0;
  border: 0 !important;
  background: transparent;
  box-shadow: none !important;
  white-space: nowrap;
}

.tenant-row-actions button {
  min-width: 2.25rem;
  min-height: 2.25rem;
  padding: 0 var(--space-admin-xs);
  flex: 0 0 auto;
}

.tenant-row-actions .detail-action {
  padding-inline: var(--space-admin-sm);
  border-color: var(--color-admin-accent);
  background: var(--color-admin-accent);
  color: var(--color-admin-on-accent);
}

.tenant-row-actions .status-action {
  border-color: var(--color-admin-alert);
  background: var(--color-admin-alert-soft);
  color: var(--color-admin-alert);
}

.tenant-row-actions .danger {
  border-color: var(--color-admin-danger-soft);
  background: var(--color-admin-danger-soft);
  color: var(--color-admin-danger);
}

.directory-empty {
  padding: var(--space-admin-xl);
  color: var(--color-admin-muted);
  text-align: center;
}

.directory-empty.error { color: var(--color-admin-danger); }
.directory-empty button { margin-left: var(--space-admin-xs); }

.directory-pager {
  min-height: 3.5rem;
  padding: var(--space-admin-sm) var(--space-admin-md);
  border-top: 1px solid var(--color-admin-rule);
  color: var(--color-admin-muted);
  font-size: var(--text-admin-caption);
}

.directory-pager > div { gap: var(--space-admin-xs); }

.directory-pager button {
  width: 2.25rem;
  min-width: 2.25rem;
  min-height: 2.25rem;
  padding: 0;
}

.tenant-dialog-backdrop {
  position: fixed;
  inset: 0;
  z-index: var(--z-admin-overlay);
  display: grid;
  place-items: center;
  padding: var(--space-admin-md);
  background: var(--color-admin-overlay);
}

.tenant-dialog {
  width: min(38.75rem, calc(100vw - 2rem));
  overflow: hidden;
  border: 1px solid var(--color-admin-rule);
  border-radius: var(--radius-admin-dialog);
  background: var(--color-admin-paper);
  box-shadow: var(--shadow-admin-dialog);
}

.tenant-dialog header {
  padding: var(--space-admin-md) var(--space-admin-lg);
  border-bottom: 1px solid var(--color-admin-rule);
}

.tenant-dialog h3 { font-size: var(--text-admin-heading); }

.close {
  width: var(--admin-control-height);
  min-width: var(--admin-control-height);
  padding: 0;
}

.tenant-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-admin-md);
  padding: var(--space-admin-lg);
}

.tenant-form label {
  display: grid;
  gap: var(--space-admin-xs);
  min-width: 0;
  color: var(--color-admin-ink);
  font-size: var(--text-admin-caption);
}

.tenant-form input,
.tenant-form select { padding: 0 var(--space-admin-sm); }

.tenant-dialog footer {
  justify-content: flex-end;
  padding: var(--space-admin-sm) var(--space-admin-lg);
  border-top: 1px solid var(--color-admin-rule);
}

.tenant-form .phone-field { grid-column: 1 / -1; }

.phone-entry {
  display: grid;
  grid-template-columns: 12rem minmax(0, 1fr);
  gap: var(--space-admin-xs);
}

.tenant-form .whatsapp-toggle,
.tenant-form .whatsapp-consent {
  display: flex;
  grid-column: 1 / -1;
  align-items: flex-start;
  gap: var(--space-admin-sm);
  padding: var(--space-admin-sm);
  border: 1px solid var(--color-admin-rule);
  border-radius: var(--radius-admin-control);
  background: var(--color-admin-success-soft);
}

.tenant-form .whatsapp-consent { background: var(--color-admin-alert-soft); }

.tenant-form .whatsapp-toggle input,
.tenant-form .whatsapp-consent input {
  width: auto;
  min-height: auto;
  margin-top: .125rem;
}

.tenant-form .whatsapp-toggle b,
.tenant-form .whatsapp-toggle small { display: block; }

.tenant-form .whatsapp-toggle small,
.tenant-form label > small {
  min-height: 1lh;
  margin-top: .25rem;
  color: var(--color-admin-muted);
  font-size: var(--text-admin-caption);
}

.tenant-form .phone-error {
  color: var(--color-admin-danger);
  font-weight: 700;
}

.phone-entry input:invalid { border-color: var(--color-admin-danger); }

.tenant-detail-body {
  max-height: 65vh;
  padding: var(--space-admin-lg);
  overflow: auto;
}

.tenant-overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-admin-xs);
}

.tenant-overview-grid article,
.tenant-lease-list article {
  border: 1px solid var(--color-admin-rule);
  border-radius: var(--radius-admin-control);
  background: var(--color-admin-surface);
}

.tenant-overview-grid article {
  display: grid;
  gap: var(--space-admin-xs);
  min-width: 0;
  padding: var(--space-admin-sm);
}

.tenant-overview-grid span,
.tenant-lease-list dt,
.workflow-label {
  color: var(--color-admin-muted);
  font-size: var(--text-admin-caption);
}

.tenant-overview-grid strong {
  overflow: hidden;
  color: var(--color-admin-ink-strong);
  font-size: var(--text-admin-heading);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tenant-overview-grid article.attention { background: var(--color-admin-alert-soft); }
.tenant-overview-grid article.attention strong { color: var(--color-admin-alert); }
.tenant-overview-grid article.deposit-balance { background: var(--color-admin-accent-soft); }
.tenant-overview-grid article.deposit-balance strong { color: var(--color-admin-accent); }

.tenant-detail-body h4 {
  margin: var(--space-admin-lg) 0 var(--space-admin-sm);
  color: var(--color-admin-ink-strong);
}

.tenant-lease-list {
  display: grid;
  gap: var(--space-admin-sm);
}

.tenant-lease-list article { padding: var(--space-admin-sm); }
.lease-head { gap: var(--space-admin-xs); }
.lease-head strong { color: var(--color-admin-ink-strong); }

.tenant-lease-list p {
  margin: .25rem 0 var(--space-admin-sm);
  color: var(--color-admin-muted);
  font-size: var(--text-admin-caption);
}

.lease-workflow {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: .25rem var(--space-admin-xs);
  margin: 0 0 var(--space-admin-sm);
  padding: var(--space-admin-xs) var(--space-admin-sm);
  border: 1px solid var(--color-admin-rule);
  border-radius: var(--radius-admin-control);
  background: var(--color-admin-accent-soft);
}

.lease-workflow strong {
  color: var(--color-admin-accent-hover);
  font-size: var(--text-admin-caption);
}

.lease-workflow small {
  grid-column: 2;
  color: var(--color-admin-muted);
  font-size: var(--text-admin-caption);
}

.tenant-lease-list dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-admin-xs);
  margin: 0;
}

.tenant-lease-list dl div { display: grid; gap: .25rem; }

.tenant-lease-list dd {
  margin: 0;
  color: var(--color-admin-ink);
  font-size: var(--text-admin-caption);
}

.tenant-lease-list dd.overdue {
  color: var(--color-admin-danger);
  font-weight: 800;
}

.lease-card-actions { gap: var(--space-admin-xs); }

.lease-workbench-btn {
  flex: 1;
  margin-top: var(--space-admin-sm);
  padding: 0 var(--space-admin-sm);
  color: var(--color-admin-accent);
}

.detail-empty {
  padding: var(--space-admin-lg) 0;
  color: var(--color-admin-muted);
  text-align: center;
}

@media (max-width: 50rem) {
  .tenant-directory { padding: var(--space-admin-sm); }
  .directory-heading { align-items: flex-start; }
  .directory-filter { flex-wrap: wrap; }
  .directory-filter label { flex: 1 1 100%; }
  .directory-filter select { flex: 1 1 12rem; }
  .tenant-form,
  .tenant-overview-grid,
  .tenant-lease-list dl,
  .phone-entry { grid-template-columns: minmax(0, 1fr); }
  .tenant-row-actions button,
  .directory-pager button { min-width: var(--admin-control-height); min-height: var(--admin-control-height); }
}

@media (max-width: 32rem) {
  .directory-heading { flex-direction: column; }
  .directory-heading .primary-btn,
  .directory-filter select,
  .directory-filter > button { width: 100%; }
  .directory-metrics { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .tenant-form { grid-template-columns: minmax(0, 1fr); padding: var(--space-admin-md); }
  .tenant-dialog header,
  .tenant-dialog footer { padding-inline: var(--space-admin-md); }
  .tenant-dialog footer { flex-wrap: wrap; }
  .tenant-dialog footer button { flex: 1; }
}

@media (prefers-reduced-motion: reduce) {
  .primary-btn,
  .ghost-btn,
  .tenant-row-actions button,
  .directory-pager button,
  .close,
  .lease-workbench-btn,
  .directory-empty button,
  .directory-card tbody tr { transition-duration: 0ms; }
}
</style>
