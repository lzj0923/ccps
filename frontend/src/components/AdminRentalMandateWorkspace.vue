<template>
  <section class="mandate-workspace" :class="{ 'workflow-pending': workflowPending }">
    <div class="workspace-head">
      <div><div class="eyebrow">{{ $t('legacy.t_ebadce743e9e') }}</div><h2>{{ $t('legacy.t_b5f563d732aa') }}</h2><p>{{ $t('legacy.t_66a0bcb52807') }}</p></div>
      <button class="primary-btn primary-btn-lg" type="button" @click="openCreate"><Plus :size="18" :stroke-width="2.2" />{{ $t('legacy.t_42bf6a51df77') }}</button>
    </div>
    <div class="summary-grid"><div v-for="card in summaryCards" :key="card.key" class="summary-card" :class="`summary-${card.key}`"><span class="summary-icon"><component :is="card.icon" :size="21" :stroke-width="1.9" /></span><div class="summary-copy"><span>{{ $lt(card.label) }}</span><strong>{{ summary[card.key] || 0 }}</strong><small>{{ $t('legacy.t_01992f531941') }}</small></div></div></div>
    <div class="toolbar"><div class="search-field"><Search :size="17" :stroke-width="1.9" /><input v-model="keyword" :placeholder="$t('legacy.t_6f8bff9a98be')" @keyup.enter="resetAndLoad" /></div><select v-model="status" @change="resetAndLoad"><option value="">{{ $t('legacy.t_026ed0343be6') }}</option><option value="active">{{ $t('legacy.t_ce6c3dc32674') }}</option><option value="suspended">{{ $t('legacy.t_37676bdf08e6') }}</option><option value="terminated">{{ $t('legacy.t_7ba87aca8368') }}</option><option value="expired">{{ $t('tenancy.expiredStatus') }}</option></select><button class="secondary-btn reload-btn" type="button" @click="load"><RefreshCw :size="16" :stroke-width="1.9" />{{ $t('legacy.t_0c9157b5bfac') }}</button></div>
    <div class="table-card"><div class="table-card-head"><div><strong>{{ $t('legacy.t_db00f68ac7c2') }}</strong><small>{{ $t('legacy.t_cc64c3eeb4d6') }}</small></div><span class="result-count">{{ $t('legacy.t_3b6ef811b85a') }} {{ totalRows }} {{ $t('legacy.t_d0bb9b2b8ea7') }}</span></div><div class="table-wrap"><table><thead><tr><th>{{ $t('legacy.t_d4723a4b8844') }}</th><th>{{ $t('legacy.t_6614e6f71020') }}</th><th>{{ $t('legacy.t_0e996dca5673') }}</th><th>{{ $t('legacy.t_edcfdcdbfa8c') }}</th><th>{{ $t('legacy.t_45293595eae3') }}</th><th>{{ $t('legacy.t_f3ea6d345e2a') }}</th></tr></thead><tbody><tr v-for="item in items" :key="item.id" :class="{ selected: selected?.id === item.id }" @click="select(item)"><td><strong>{{ item.mandateNo }}</strong></td><td>{{ item.ownerName }}<br /><small>{{ item.projectName }} · {{ item.unitNo }}</small></td><td>{{ displayDate(item.startDate) }}<br />{{ item.endDate ? displayDate(item.endDate) : $t('legacy.t_79aacc634aa7') }}</td><td>{{ item.responsibleUserName || $t('legacy.t_863a3c74cfd9') }}</td><td><span class="status" :class="item.status"><i></i>{{ $lt(statusLabel(item.status)) }}</span></td><td class="actions"><button v-if="item.status === 'active'" class="secondary-btn compact-btn" @click.stop="changeStatus(item, 'suspended')">{{ $t('legacy.t_37676bdf08e6') }}</button><button v-if="item.status === 'suspended'" class="secondary-btn compact-btn" @click.stop="changeStatus(item, 'active')">{{ $t('legacy.t_ca33657147e6') }}</button><button v-if="['active','suspended'].includes(item.status)" class="danger-btn compact-btn" @click.stop="terminate(item)">{{ $t('legacy.t_11a1d4ecf8bb') }}</button></td></tr><tr v-if="!items.length"><td colspan="6" class="empty">{{ $t('legacy.t_a18d4c65acd9') }}</td></tr></tbody></table></div></div>
    <AdminListPager v-if="totalRows" :page="pageNumber" :page-size="pageSize" :total="totalRows" @update:page="goPage" @update:page-size="changePageSize" />
    <aside v-if="selected" class="detail-panel"><div class="detail-head"><div><div class="eyebrow">{{ $t('legacy.t_1acb9dd33ebd') }}</div><h3>{{ selected.mandateNo }}</h3></div><button class="icon-btn" type="button" :aria-label="$t('legacy.t_48b47156015e')" @click="selected = null">×</button></div><div class="detail-hero"><strong>{{ selected.ownerName }}</strong><span>{{ selected.projectName }} · {{ selected.unitNo }}</span><span class="status" :class="selected.status"><i></i>{{ $lt(statusLabel(selected.status)) }}</span></div><div class="detail-grid"><div><small>{{ $t('legacy.t_0e996dca5673') }}</small><strong>{{ displayDate(selected.startDate) }} {{ $t('legacy.t_43401e739ef4') }} {{ selected.endDate ? displayDate(selected.endDate) : $t('legacy.t_79aacc634aa7') }}</strong></div><div><small>{{ $t('legacy.t_a178daac2527') }}</small><strong>{{ selected.managementFee || 0 }}</strong></div><div><small>{{ $t('legacy.t_93a98f5e6002') }}</small><strong>{{ selected.commissionPercent || 0 }}%</strong></div></div><div><div v-if="['active','suspended'].includes(selected.status)" class="detail-actions"><button class="secondary-btn full-btn" type="button" @click="openHandover"><span>⌂</span>{{ handover ? $t('legacy.t_3d082ac64340') : $t('legacy.t_7311f7b91045') }}</button></div><p v-if="handover" class="handover-state"><i></i>{{ $t('legacy.t_494a40854fe7') }}{{ handover.status === 'completed' ? $t('legacy.t_e99b48a29bdf') : $t('legacy.t_0f436818c0b4') }} · {{ displayDate(handover.handoverDate) }}</p><section class="detail-section"><div class="section-title"><h4>{{ $t('legacy.t_2cb33d0a43c5') }}</h4><span>{{ documentsLoading ? $t('tenancy.loadingDocuments') : documents.length }} {{ $t('legacy.t_aa9f1ad4f91c') }}</span></div><p class="authorization-hint">{{ $t('tenancy.signedAuthorizationUploadHint') }}</p><div class="upload-row"><select v-model="documentType"><option value="mandate_document">{{ $t('legacy.t_01cbac7e5bc6') }}</option><option value="authorization">{{ $t('legacy.t_5ed1fcbc5cd9') }}</option><option value="handover_photo">{{ $t('legacy.t_91594df5910f') }}</option><option value="inventory">{{ $t('legacy.t_c8159b903b04') }}</option></select><label class="upload-btn"><span>↑</span>{{ $t('legacy.t_cc44896ba419') }}<input type="file" @change="uploadDocument" /></label></div><p v-if="documentsLoading" class="muted">{{ $t('tenancy.loadingDocuments') }}</p><p v-else-if="documentLoadError" class="admin-property-error">{{ $lt(documentLoadError) }}</p><ul v-else class="detail-list"><li v-for="doc in documents" :key="doc.id"><div><button class="link-btn" type="button" @click="downloadDocument(doc)">{{ doc.originalName }}</button><small>{{ $lt(documentTypeLabel(doc.relationType)) }} · {{ dateTime(doc.createdAt) }}</small></div><button v-if="doc.mimeType === 'application/pdf' && doc.relationType !== 'signed_contract'" class="text-action" type="button" @click="startDocumentSigning(doc)">{{ $t('legacy.t_403f901d0b71') }}</button></li><li v-if="!documents.length" class="muted">{{ $t('legacy.t_6007668430ab') }}</li></ul></section><section class="detail-section"><div class="section-title"><h4>{{ $t('legacy.t_04dcf3bd904a') }}</h4><span>{{ history.length }} {{ $t('legacy.t_d0bb9b2b8ea7') }}</span></div><ul class="history-list"><li v-for="entry in history" :key="entry.id"><span class="history-dot"></span><div><strong>{{ $lt(statusLabel(entry.fromStatus)) }} → {{ $lt(statusLabel(entry.toStatus)) }}</strong><small>{{ dateTime(entry.changedAt) }}<template v-if="entry.reason"> · {{ entry.reason }}</template></small></div></li></ul></section></div></aside>

    <div v-if="authorizationWorkspaceOpen" class="modal-backdrop" @pointerdown.self="closeAuthorizationWorkspace"><div class="modal modal-authorization-workspace"><div class="modal-head"><div><div class="eyebrow">{{ $t('tenancy.authorizationWorkspaceEyebrow') }}</div><h3>{{ $t('tenancy.authorizationWorkspaceTitle') }}</h3><p>{{ selected && selected.mandateNo }} · {{ selected && selected.ownerName }}</p></div><button type="button" class="icon-btn" @click="closeAuthorizationWorkspace">×</button></div><div class="authorization-steps"><span class="active">{{ $t('legacy.t_dcf4ef6622d9') }} {{ $t('tenancy.authorizationStepGenerate') }}</span><span :class="{ active: authorizationDraft }">{{ $t('legacy.t_e97031e5b027') }} {{ $t('tenancy.authorizationStepSign') }}</span><span :class="{ active: hasAuthorization }">{{ $t('legacy.t_9902d381a017') }} {{ $t('tenancy.authorizationStepComplete') }}</span></div><p v-if="!authorizationDraft && !hasAuthorization" class="authorization-hint">{{ $t('tenancy.authorizationWorkspaceHint') }}</p><button v-if="!authorizationDraft && !hasAuthorization" class="primary-btn" type="button" @click="openAuthorizationForm">{{ $t('tenancy.generateAuthorization') }}</button><div v-if="authorizationDraft" class="authorization-document-row"><div class="authorization-document-info"><strong>{{ authorizationDraft.originalName }}</strong><small>{{ $t('tenancy.authorizationDraftLabel') }}</small></div><div class="signing-recipient-fields"><label>{{ $t('legacy.t_a6c87526ef43') }}<input v-model.trim="signingForm.signerName" type="text" autocomplete="name"></label><label>{{ $t('legacy.t_7659e49d822a') }}<input v-model.trim="signingForm.signerEmail" type="email" autocomplete="email" required></label></div><button class="primary-btn compact-btn" type="button" @click="startDocumentSigning(authorizationDraft)" :disabled="signingBusy || !signingForm.signerName.trim() || !signingForm.signerEmail.trim()">{{ signingBusy ? $t('legacy.t_c1b894480de0') : $t('tenancy.startOnlineSigning') }}</button></div><p v-if="signingError" class="admin-property-error">{{ $lt(signingError) }}</p><p v-if="hasAuthorization" class="authorization-ready">{{ $t('tenancy.authorizationUploaded') }}</p><div class="modal-actions"><button class="secondary-btn" type="button" @click="closeAuthorizationWorkspace">{{ $t('tenancy.close') }}</button></div></div></div>
    <div v-if="authorizationOpen" class="modal-backdrop" @pointerdown.self="closeAuthorizationDialog"><div class="modal mandate-authorization-dialog"><form @submit.prevent="generateAuthorization"><div class="modal-head"><div><h3>{{ $t('tenancy.generateAuthorizationTitle') }}</h3><small>{{ $t('tenancy.templateHint') }}</small></div><button type="button" class="icon-btn" @click="closeAuthorizationDialog">×</button></div><div class="form-grid"><label>{{ $t('tenancy.caseNo') }}<input v-model.trim="authorizationForm.caseNo" required></label><label>{{ $t('tenancy.propertyAddress') }}<input v-model.trim="authorizationForm.propertyAddress" required></label><label>{{ $t('tenancy.ownerName') }}<input v-model.trim="authorizationForm.landlordName"></label><label>{{ $t('tenancy.ownerIdentity') }}<input v-model.trim="authorizationForm.landlordIdentity"></label><label>{{ $t('tenancy.earnestDepositRm') }}<input v-model.trim="authorizationForm.earnestDeposit"></label><label>{{ $t('tenancy.commissionMonths') }}<input v-model.trim="authorizationForm.commission"></label><label>{{ $t('tenancy.gstSst') }}<input v-model.trim="authorizationForm.securityDepositMonths"></label><label>{{ $t('tenancy.agencyFeeRm') }}<input v-model.trim="authorizationForm.utilityDepositMonths"></label><label>{{ $t('tenancy.totalAgencyFeeRm') }}<input v-model.trim="authorizationForm.totalBeforeKeys"></label><label>{{ $t('tenancy.mandateStartDate') }}<input v-model.trim="authorizationForm.startDate" type="date"></label><label>{{ $t('tenancy.mandateEndDate') }}<input v-model.trim="authorizationForm.commencementDate" type="date"></label><label class="wide">{{ $t('tenancy.otherConditions') }}<input v-model.trim="authorizationForm.otherConditions"></label></div><p v-if="authorizationError" class="admin-property-error">{{ $lt(authorizationError) }}</p><div class="modal-actions"><button class="secondary-btn" type="button" @click="closeAuthorizationDialog">{{ $t('tenancy.cancel') }}</button><button class="primary-btn" type="submit" :disabled="authorizationBusy">{{ authorizationBusy ? $t('tenancy.generating') : $t('tenancy.generateDownloadPdf') }}</button></div></form></div></div>
    <div v-if="showCreate" class="modal-backdrop"><div class="modal"><div class="modal-head"><div><div class="eyebrow">{{ $t('legacy.t_1d6e5efa9017') }}</div><h3>{{ editingMandateId ? $t('rentalMandates.editDetails') : $t('legacy.t_fd996f9aad00') }}</h3><p>{{ editingMandateId ? $t('rentalMandates.editDetailsHint') : $t('legacy.t_5eadd0312f61') }}</p></div><button class="icon-btn" type="button" @click="showCreate = false">×</button></div><label>{{ $t('legacy.t_805febdfe9ae') }}<select v-model="form.ownerUnitId" :disabled="Boolean(editingMandateId)"><option :value="null">{{ $t('legacy.t_9d72e0df80ca') }}</option><option v-for="unit in options.units" :key="unit.id" :value="unit.id">{{ unit.projectName }} · {{ unit.unitNo }} · {{ unit.ownerName }}</option></select></label><label>{{ $t('legacy.t_ca209c5c82b3') }}<select v-model="form.mandateType"><option value="management">{{ $t('legacy.t_884e447b954b') }}</option><option value="exclusive">{{ $t('legacy.t_5e9343efd79f') }}</option><option value="non_exclusive">{{ $t('legacy.t_7da9307fbbf8') }}</option></select></label><div class="form-grid"><label>{{ $t('legacy.t_7fd7a227e9ce') }}<input v-model="form.startDate" type="date" /></label><label>{{ $t('legacy.t_27eefa5237a0') }}<input v-model="form.endDate" type="date" /></label><label>{{ $t('legacy.t_a178daac2527') }}<input v-model="form.managementFee" type="number" min="0" step="0.01" /></label><label>{{ $t('legacy.t_93a98f5e6002') }}<input v-model="form.commissionPercent" type="number" min="0" max="100" step="0.01" /></label></div><label>{{ $t('legacy.t_edcfdcdbfa8c') }}<select v-model="form.responsibleUserId"><option :value="null">{{ $t('legacy.t_863a3c74cfd9') }}</option><option v-for="user in options.users" :key="user.id" :value="user.id">{{ user.name || user.displayName }}</option></select></label><div class="modal-actions"><button class="secondary-btn" type="button" @click="showCreate = false">{{ $t('legacy.t_4d0b4688c787') }}</button><button class="primary-btn" type="button" @click="create" :disabled="saving">{{ editingMandateId ? $t('rentalMandates.saveDetails') : $t('legacy.t_cfd822039051') }}</button></div></div></div>
    <div v-if="showHandover" class="modal-backdrop"><div class="modal modal-handover"><div class="modal-head"><div><div class="eyebrow">{{ $t('legacy.t_23be3a658c7c') }}</div><h3>{{ $t('legacy.t_aab015938018') }}</h3><p>{{ selected && selected.mandateNo }} {{ $t('legacy.t_0c584f0f9531') }}</p></div><button class="icon-btn" type="button" @click="showHandover = false">×</button></div><div class="form-grid"><label>{{ $t('legacy.t_d8931bb4e442') }}<input v-model="handoverForm.handoverDate" type="date" /></label><label>{{ $t('legacy.t_b965a5e3c3ab') }}<input v-model="handoverForm.receivedBy" /></label><label>{{ $t('legacy.t_c5e2b373b4c3') }}<input v-model.number="handoverForm.keyCount" type="number" min="0" /></label><label>{{ $t('legacy.t_aa972d904421') }}<input v-model.number="handoverForm.accessCardCount" type="number" min="0" /></label><label>{{ $t('legacy.t_534de2712bb7') }}<input v-model="handoverForm.waterMeter" /></label><label>{{ $t('legacy.t_163ca92ef2a1') }}<input v-model="handoverForm.electricityMeter" /></label></div><label>{{ $t('legacy.t_68b4214b4c85') }}<textarea v-model="handoverForm.conditionSummary"></textarea></label><label>{{ $t('legacy.t_4cefde7bdce6') }}<textarea v-model="handoverForm.inventory"></textarea></label><label>{{ $t('legacy.t_3ae9ab7d020d') }}<textarea v-model="handoverForm.notes"></textarea></label><div class="modal-actions"><button class="secondary-btn" type="button" @click="showHandover = false">{{ $t('legacy.t_4d0b4688c787') }}</button><button class="secondary-btn" type="button" @click="saveHandover(false)">{{ $t('legacy.t_cfd822039051') }}</button><button class="primary-btn" type="button" @click="saveHandover(true)">{{ $t('legacy.t_ca1ed59e4728') }}</button></div></div></div>
  </section>
</template>

<script>
import { fetchAdminRentalMandate, updateAdminRentalMandate, fetchAdminRentalMandates, fetchAdminRentalMandateOptions, createAdminRentalMandate, updateAdminRentalMandateStatus, fetchAdminRentalMandateHistory, fetchAdminPropertyHandover, saveAdminPropertyHandover, fetchAdminRentalMandateDocuments, uploadAdminRentalMandateDocument, downloadAdminRentalMandateDocument, startAdminMandateDocumentSignature, generateAdminContractTemplate } from '../services/propertyApi';
import AdminListPager from './AdminListPager.vue';
import { CircleCheck, CirclePause, Layers3, Plus, RefreshCw, Search } from '@lucide/vue';
import { formatDate, formatDateTime } from '../utils/dateFormat';
export default {
  name: 'AdminRentalMandateWorkspace',
  components: { AdminListPager, Plus, RefreshCw, Search },
  props: { page: { type: Object, default: () => ({}) } },
  data() { return { items: [], summary: {}, options: { units: [], users: [] }, optionsError: '', selected: null, history: [], handover: null, documents: [], documentsLoading: false, documentLoadError: '', documentType: 'mandate_document', showHandover: false, handoverForm: {}, managementOpen: false, workflowPending: false, authorizationOpen: false, authorizationWorkspaceOpen: false, authorizationBusy: false, authorizationError: '', authorizationForm: {}, signingForm: { signerName: '', signerEmail: '' }, signingBusy: false, signingError: '', documentRefreshTimer: null, refreshOnFocus: null, keyword: '', status: '', pageNumber: 1, pageSize: 5, totalRows: 0, showCreate: false, editingMandateId: null, saving: false, form: { ownerUnitId: null, mandateType: 'management', startDate: '', endDate: '', managementFee: 0, commissionPercent: 0, responsibleUserId: null } }; },
  computed: { hasAuthorization() { return this.documents.some(doc => ['authorization', 'signed_contract', 'authorization_signed'].includes(String(doc.relationType || '').toLowerCase())); }, authorizationDraft() { return this.documents.find(doc => doc.relationType === 'authorization_draft'); }, summaryCards() { return [{ key: 'total', label: this.$t('rentalMandates.all'), icon: Layers3 }, { key: 'active', label: this.$t('rentalMandates.active'), icon: CircleCheck }, { key: 'suspended', label: this.$t('legacy.t_37676bdf08e6'), icon: CirclePause }]; } },
  watch: { 'page.adminRentalMandateCreateNonce': 'openCreate', 'page.adminRentalMandateRefreshNonce': 'load', authorizationOpen(value) { if (value) this.authorizationWorkspaceOpen = false; }, authorizationWorkspaceOpen(value) { if (value) this.startDocumentRefresh(); else this.stopDocumentRefresh(); } },
  mounted() { this.load().then(() => this.applyProcessRoute()); this.loadOptions(); this.refreshOnFocus = () => this.refreshSelectedDocuments(); window.addEventListener('focus', this.refreshOnFocus); },
  beforeUnmount() { if (this.refreshOnFocus) window.removeEventListener('focus', this.refreshOnFocus); this.stopDocumentRefresh(); },
  methods: {
    displayDate(value) { return formatDate(value); },
    dateTime(value) { return formatDateTime(value); },
    async applyProcessRoute() {
      const params = new URLSearchParams(window.location.search);
      const workflow = params.get('workflow'); const ownerUnitId = params.get('ownerUnitId');
      if (!workflow || !ownerUnitId) return;
      try {
        if (workflow === 'create') { await this.openCreate(); this.form.ownerUnitId = Number(ownerUnitId); return; }
        const mandateId = params.get('mandateId');
        const item = mandateId ? await fetchAdminRentalMandate(mandateId) : null;
        if (!item || String(item.ownerUnitId) !== String(ownerUnitId)) { this.$emit('toast', this.$t('rentalMandates.targetMissing')); return; }
        await this.select(item);
        if (workflow === 'edit') { await this.openEdit(item); return; }
        if (workflow !== 'select') this.openAuthorizationWorkspace(item);
      } catch (error) { this.$emit('toast', error.message); }
    },
    async load() { try { const data = await fetchAdminRentalMandates({ keyword: this.keyword, status: this.status, page: this.pageNumber, pageSize: this.pageSize }); this.items = data.rows || data.items || []; this.summary = data.summary || {}; this.totalRows = Number(data.page?.totalRows || this.items.length); if (data.page?.page) this.pageNumber = Number(data.page.page); if (this.selected) this.select(this.items.find(item => item.id === this.selected.id) || null); } catch (error) { this.$emit('toast', error.message); } },
    resetAndLoad() { this.pageNumber = 1; return this.load(); },
    goPage(page) { if (page === this.pageNumber) return; this.pageNumber = page; this.load(); },
    changePageSize(size) { if (size === this.pageSize) return; this.pageSize = size; this.pageNumber = 1; this.load(); },
    async loadOptions() { try { this.optionsError = ''; this.options = await fetchAdminRentalMandateOptions(); } catch (error) { this.optionsError = error.message; this.$emit('toast', error.message); } },
    async openCreate() { this.editingMandateId = null; this.form = { ownerUnitId: null, mandateType: 'management', startDate: '', endDate: '', managementFee: 0, commissionPercent: 0, responsibleUserId: null }; await this.loadOptions(); this.showCreate = true; },
    async openEdit(item) { if (!['active', 'suspended'].includes(item.status)) return; await this.loadOptions(); this.editingMandateId = item.id; this.form = { ownerUnitId: item.ownerUnitId, mandateType: item.mandateType, startDate: item.startDate || '', endDate: item.endDate || '', managementFee: item.managementFee || 0, commissionPercent: item.commissionPercent || 0, responsibleUserId: item.responsibleUserId || null }; if (!this.options.units.some(unit => String(unit.id) === String(item.ownerUnitId))) this.options.units.push({ ...item, id: item.ownerUnitId }); this.showCreate = true; },
    async create() { if (this.saving) return; this.saving = true; try { const payload = { ...this.form, endDate: this.form.endDate || null, ownerUnitId: Number(this.form.ownerUnitId), responsibleUserId: this.form.responsibleUserId ? Number(this.form.responsibleUserId) : null }; if (this.editingMandateId) await updateAdminRentalMandate(this.editingMandateId, payload); else await createAdminRentalMandate(payload); this.showCreate = false; this.$emit('toast', this.editingMandateId ? this.$t('rentalMandates.detailsSaved') : this.$lt('委托已建立并启用')); await Promise.all([this.load(), this.loadOptions()]); } catch (error) { this.$emit('toast', error.message); } finally { this.saving = false; } },
    async changeStatus(item, nextStatus) { try { await updateAdminRentalMandateStatus(item.id, { status: nextStatus, reason: '' }); await Promise.all([this.load(), this.loadOptions()]); } catch (error) { this.$emit('toast', error.message); } },
    async terminate(item) { const reason = window.prompt(this.$lt('請輸入終止原因')); if (!reason) return; try { await updateAdminRentalMandateStatus(item.id, { status: 'terminated', reason }); await Promise.all([this.load(), this.loadOptions()]); this.$emit('toast', this.$lt('委託已終止，房產已重新開放建立委託')); } catch (error) { this.$emit('toast', error.message); } },
        async refreshSelectedDocuments() { if (!this.selected?.id || this.authorizationOpen) return; try { this.documents = (await fetchAdminRentalMandateDocuments(this.selected.id)) || []; } catch (error) { /* Keep the current attachment list while the server is unavailable. */ } },
        startDocumentRefresh() { this.stopDocumentRefresh(); if (!this.selected?.id) return; this.refreshSelectedDocuments(); this.documentRefreshTimer = window.setInterval(() => this.refreshSelectedDocuments(), 4000); },
        stopDocumentRefresh() { if (this.documentRefreshTimer) { window.clearInterval(this.documentRefreshTimer); this.documentRefreshTimer = null; } },
    async select(item) { this.selected = item; this.managementOpen = false; this.handover = null; this.documents = []; this.documentLoadError = ''; this.documentsLoading = Boolean(item); if (!item) return; try { const [history, handover, documents] = await Promise.all([fetchAdminRentalMandateHistory(item.id), fetchAdminPropertyHandover(item.id), fetchAdminRentalMandateDocuments(item.id)]); this.history = history || []; this.handover = handover; this.documents = documents || []; } catch (error) { this.documentLoadError = error.message || this.$t('tenancy.documentsLoadFailed'); } finally { this.documentsLoading = false; } },
    openHandover() { this.handoverForm = { handoverDate: this.handover?.handoverDate || new Date().toISOString().slice(0, 10), conditionSummary: this.handover?.conditionSummary || '', keyCount: this.handover?.keyCount || 0, accessCardCount: this.handover?.accessCardCount || 0, waterMeter: this.handover?.waterMeter || '', electricityMeter: this.handover?.electricityMeter || '', inventory: this.handover?.inventory || '{}', receivedBy: this.handover?.receivedBy || '', notes: this.handover?.notes || '' }; this.showHandover = true; },
    async saveHandover(completed) { try { this.handover = await saveAdminPropertyHandover(this.selected.id, { ...this.handoverForm, completed }); this.showHandover = false; this.$emit('toast', completed ? '接管資料已完成' : '接管草稿已儲存'); } catch (error) { this.$emit('toast', error.message); } },
    async uploadDocument(event) { const file = event.target.files?.[0]; if (!file || !this.selected) return; try { this.documents = await uploadAdminRentalMandateDocument(this.selected.id, this.documentType, file); this.$emit('toast', this.documentType === 'authorization' ? this.$t('tenancy.authorizationUploaded') : '附件已上傳'); } catch (error) { this.$emit('toast', error.message); } finally { event.target.value = ''; } },
    async downloadDocument(doc) { try { const result = await downloadAdminRentalMandateDocument(this.selected.id, doc.id); const url = URL.createObjectURL(result.blob); const link = document.createElement('a'); link.href = url; link.download = doc.originalName; link.click(); URL.revokeObjectURL(url); } catch (error) { this.$emit('toast', error.message); } },
    async startDocumentSigning(doc) { if (doc?.relationType === 'signed_contract') return; const signerName = this.signingForm.signerName.trim(); const signerEmail = this.signingForm.signerEmail.trim(); if (!signerName || !signerEmail) { this.signingError = '請填寫簽署人姓名與電郵'; return; } this.signingBusy = true; this.signingError = ''; try { await startAdminMandateDocumentSignature(this.selected.id, doc.id, { signerName, signerEmail, expiresInDays: 7 }); this.$emit('toast', this.$lt('簽署邀請已寄出')); } catch (error) { this.signingError = error.message || '簽署邀請寄送失敗'; } finally { this.signingBusy = false; } },
     openAuthorizationWorkspace(item) { if (!item || !['active', 'suspended'].includes(item.status)) return; this.workflowPending = true; this.selected = item; this.signingForm = { signerName: item.ownerName || '', signerEmail: item.ownerEmail || '' }; this.signingError = ''; this.authorizationForm = { caseNo: item.mandateNo || `RM-${item.id}`, propertyAddress: `${item.projectName || ''} ${item.unitNo || ''}`.trim(), landlordName: item.ownerName || '', landlordIdentity: item.ownerIdentity || '', earnestDeposit: '', commission: item.commissionPercent ? `${item.commissionPercent}%` : '', securityDepositMonths: '', utilityDepositMonths: '', totalBeforeKeys: '', startDate: item.startDate || '', commencementDate: item.endDate || '', otherConditions: '' }; this.authorizationError = ''; this.documentType = 'authorization_draft'; this.authorizationWorkspaceOpen = true; },
    openAuthorizationForm() { this.authorizationWorkspaceOpen = false; this.authorizationOpen = true; },
    closeAuthorizationWorkspace() { this.authorizationWorkspaceOpen = false; this.authorizationError = ''; this.workflowPending = false; this.$nextTick(() => { this.selected = null; }); },
    closeAuthorizationDialog() { this.authorizationOpen = false; this.authorizationBusy = false; this.authorizationError = ''; },
        withTimeout(promise, milliseconds, message) { let timer; const timeout = new Promise((_, reject) => { timer = window.setTimeout(() => reject(new Error(message)), milliseconds); }); return Promise.race([promise, timeout]).finally(() => window.clearTimeout(timer)); },
        async generateAuthorization() { if (!this.selected?.id || !['active', 'suspended'].includes(this.selected.status)) return; this.authorizationBusy = true; this.authorizationError = ''; try { const result = await this.withTimeout(generateAdminContractTemplate('authorization', this.authorizationForm), 20000, '生成授权委托书超时，请检查后端服务'); const file = new File([result.blob], result.filename, { type: 'application/pdf' }); this.documents = await this.withTimeout(uploadAdminRentalMandateDocument(this.selected.id, 'authorization_draft', file), 20000, '授权委托书上传超时，请检查后端服务'); this.closeAuthorizationDialog(); this.authorizationWorkspaceOpen = true; this.$emit('toast', this.$t('tenancy.authorizationGenerated')); } catch (error) { this.authorizationError = error.message || this.$t('tenancy.templateGenerateFailed'); } finally { this.authorizationBusy = false; } },
    documentTypeLabel(value) { const key = { mandate_document: 'legacy.t_01cbac7e5bc6', authorization: 'legacy.t_5ed1fcbc5cd9', handover_photo: 'legacy.t_91594df5910f', inventory: 'legacy.t_c8159b903b04', signed_contract: 'ui.documentTypeSigned' }[value]; return key ? this.$t(key) : value || '—'; },
    statusLabel(status) { const key = { active: 'rentalMandates.active', suspended: 'rentalMandates.suspended', terminated: 'rentalMandates.terminated', expired: 'tenancy.expiredStatus', draft: 'rentalMandates.draft', pending: 'rentalMandates.pending' }[status]; return key ? this.$t(key) : status || '—'; }
  }
};
</script>

<style scoped>
.mandate-workspace{position:relative;color:#153b57}.workspace-head,.detail-head,.toolbar,.modal-actions{display:flex;align-items:center;justify-content:space-between;gap:16px}.workspace-head{margin-bottom:20px}.workspace-head h2{margin:3px 0 4px;font-size:24px;letter-spacing:-.02em}.workspace-head p{margin:0;color:#6a8294;font-size:13px}.eyebrow{color:#0c8d98;font-size:10px;font-weight:700;letter-spacing:.12em}.primary-btn,.secondary-btn,.danger-btn,.icon-btn,.compact-btn,.upload-btn{display:inline-flex;align-items:center;justify-content:center;gap:7px;border-radius:8px;font-weight:600;cursor:pointer;transition:all .18s ease}.primary-btn{background:#0b8f99;color:#fff;border:1px solid #0b8f99;padding:10px 16px;box-shadow:0 5px 12px #0b8f9930}.primary-btn:hover{background:#087782;transform:translateY(-1px)}.primary-btn-lg{min-height:44px;padding:0 20px}.button-icon{font-size:18px;line-height:1}.secondary-btn{background:#fff;color:#24516c;border:1px solid #c9dbe4;padding:9px 14px}.secondary-btn:hover{border-color:#0b8f99;color:#087782;background:#f3fbfb}.danger-btn{background:#fff5f5;color:#c13c45;border:1px solid #efc6ca;padding:9px 14px}.danger-btn:hover{background:#fde9ea;border-color:#de9aa0}.compact-btn{padding:6px 10px;font-size:11px;box-shadow:none}.icon-btn{width:34px;height:34px;padding:0;background:#f4f8fa;border:1px solid #d5e3e9;color:#4d687a;font-size:20px;line-height:1}.icon-btn:hover{background:#e6f5f5;color:#087782;border-color:#91d5d6}.summary-grid{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:12px;margin-bottom:18px}.summary-card{position:relative;padding:16px 18px;background:linear-gradient(135deg,#fff,#f5fafb);border:1px solid #dbe8ed;border-radius:12px;box-shadow:0 4px 14px #1f52690b}.summary-card span,.summary-card small{display:block;color:#7890a0;font-size:12px}.summary-card strong{display:block;margin:5px 0 1px;color:#153b57;font-size:26px;line-height:1}.summary-card small{font-size:11px;color:#9aadb9}.toolbar{justify-content:flex-start;padding:12px 14px;margin-bottom:14px;border:1px solid #dbe8ed;border-radius:12px;background:#f8fbfc}.search-field{display:flex;align-items:center;gap:8px;flex:1;min-width:220px;background:#fff;border:1px solid #c9dbe4;border-radius:8px;padding:0 11px;color:#7592a2}.search-field input{width:100%;border:0!important;outline:0;padding:10px 0!important;background:transparent!important}.toolbar select{min-width:150px}.toolbar select,.toolbar input,.modal input,.modal select,.modal textarea{border:1px solid #c9dbe4;border-radius:8px;padding:10px;color:#244b64;background:#fff;outline:none}.toolbar select:focus,.modal input:focus,.modal select:focus,.modal textarea:focus{border-color:#55b7ba;box-shadow:0 0 0 3px #0b8f9918}.table-card{border:1px solid #dbe8ed;border-radius:12px;background:#fff;overflow:hidden;box-shadow:0 5px 18px #1f52690b}.table-card-head{display:flex;align-items:center;justify-content:space-between;padding:14px 18px;border-bottom:1px solid #e7eff2}.table-card-head strong{display:block;font-size:15px}.table-card-head small{display:block;margin-top:3px;color:#8197a5;font-size:11px}.result-count{color:#718b9b;font-size:12px}.table-wrap{overflow:auto}table{width:100%;border-collapse:collapse}th,td{text-align:left;padding:12px 14px;border-bottom:1px solid #edf3f5;font-size:12px}th{background:#f6fafb;color:#668092;font-size:11px;font-weight:700;white-space:nowrap}tbody tr{cursor:pointer;transition:background .15s}tbody tr:hover{background:#f7fcfc}td strong{color:#174966}td small{color:#7d95a3;font-size:11px}.actions{white-space:nowrap}.actions button{margin:0 4px 0 0}.status{display:inline-flex;align-items:center;gap:6px;padding:5px 9px;border-radius:999px;font-size:11px;background:#eef3f5;color:#567080;white-space:nowrap}.status i,.handover-state i{width:6px;height:6px;border-radius:50%;background:currentColor}.status.active{background:#e4f8ed;color:#168047}.status.pending_review{background:#fff4d8;color:#986900}.status.suspended{background:#fff4d8;color:#986900}.status.terminated{background:#fee9ea;color:#ba3e46}.empty{text-align:center;color:#8299a7;padding:34px!important}.detail-panel{position:absolute;right:0;top:62px;width:min(380px,calc(100% - 24px));max-height:calc(100vh - 150px);overflow:auto;background:#fff;border:1px solid #cfe0e6;border-radius:14px;padding:20px;box-shadow:0 18px 42px #173f5228;z-index:2}.detail-head{align-items:flex-start}.detail-head h3{margin:3px 0 0;font-size:18px}.detail-hero{display:grid;gap:4px;padding:16px 0;border-bottom:1px solid #e5eef1}.detail-hero strong{font-size:16px}.detail-hero span:not(.status){color:#6f8998;font-size:12px}.detail-hero .status{justify-self:start;margin-top:7px}.detail-grid{display:grid;grid-template-columns:1.4fr .8fr .8fr;gap:10px;padding:16px 0;border-bottom:1px solid #e5eef1}.detail-grid div{display:grid;gap:4px}.detail-grid small,.section-title span{color:#8198a6;font-size:11px}.detail-grid strong{font-size:12px;color:#214b64}.detail-actions{padding:14px 0 4px}.full-btn{width:100%}.handover-state{display:flex;align-items:center;gap:7px;margin:12px 0 0;padding:9px 11px;border-radius:8px;background:#e9f8ef;color:#168047;font-size:12px}.detail-section{padding-top:18px}.section-title{display:flex;align-items:center;justify-content:space-between;margin-bottom:10px}.section-title h4{margin:0;font-size:13px}.detail-list,.history-list{list-style:none;margin:0;padding:0}.detail-list li{display:flex;align-items:center;justify-content:space-between;gap:8px;padding:9px 0;border-top:1px solid #edf3f5}.link-btn{border:0;background:none;color:#0b7f89;padding:0;cursor:pointer;text-align:left}.text-action{border:0;background:none;color:#b77900;font-size:11px;cursor:pointer;padding:3px}.detail-list li small,.history-list small{display:block;color:#8ba0ac;font-size:10px;margin-top:3px}.muted{color:#8ba0ac!important}.history-list li{display:flex;gap:9px;padding:8px 0;border-top:1px solid #edf3f5}.history-dot{width:7px;height:7px;margin-top:4px;border-radius:50%;background:#4bb4b6;flex:0 0 auto}.history-list strong{font-size:11px}.upload-row{display:flex;gap:8px;align-items:center}.upload-row select{flex:1;min-width:0}.upload-btn{position:relative;padding:9px 11px;background:#f0fafb;border:1px solid #b6dfe0;color:#087782;font-size:11px;white-space:nowrap}.upload-btn input{position:absolute;inset:0;opacity:0;cursor:pointer}.modal-backdrop{position:fixed;inset:0;background:#17334578;backdrop-filter:blur(3px);display:grid;place-items:center;padding:20px;z-index:10}.modal{background:#fff;border:1px solid #d8e6eb;border-radius:16px;padding:24px;width:min(620px,94vw);max-height:calc(100vh - 40px);overflow:auto;display:grid;gap:16px;box-shadow:0 22px 55px #122f3d38}.modal-small{width:min(470px,94vw)}.modal-handover{width:min(720px,94vw)}.modal-head{display:flex;align-items:flex-start;justify-content:space-between;gap:16px;padding-bottom:14px;border-bottom:1px solid #e6eff2}.modal-head h3{margin:3px 0 4px;font-size:19px}.modal-head p{margin:0;color:#7a919e;font-size:12px}.modal label{display:grid;gap:6px;color:#426478;font-size:12px;font-weight:600}.form-grid{display:grid;grid-template-columns:1fr 1fr;gap:13px}.modal textarea{min-height:100px;resize:vertical}.modal-actions{justify-content:flex-end;padding-top:6px}.modal-actions .danger-btn{margin-right:auto}@media(max-width:900px){.summary-grid{grid-template-columns:repeat(3,1fr)}.detail-panel{position:fixed;top:72px;right:12px;max-height:calc(100vh - 90px)}}@media(max-width:620px){.workspace-head{align-items:flex-start;flex-direction:column}.summary-grid{grid-template-columns:repeat(2,1fr)}.toolbar{align-items:stretch;flex-direction:column}.search-field{width:100%}.toolbar select,.toolbar button{width:100%}.form-grid{grid-template-columns:1fr}.modal-actions{flex-wrap:wrap}.modal-actions button{flex:1}.detail-grid{grid-template-columns:1fr}.detail-panel{width:calc(100% - 24px)}}
.authorization-status{margin:10px 0;padding:10px 12px;border:1px solid #f1c4c4;border-radius:8px;background:#fff5f5;color:#b33f46;font-weight:700;font-size:12px}.authorization-status.ready{border-color:#b6e2c7;background:#f0fbf4;color:#168047}.authorization-warning{margin:10px 0;padding:10px 12px;border:1px solid #f1c4c4;border-radius:8px;background:#fff5f5;color:#b33f46;font-size:12px;line-height:1.5}.authorization-ready{margin:10px 0;padding:10px 12px;border:1px solid #b6e2c7;border-radius:8px;background:#f0fbf4;color:#168047;font-size:12px;line-height:1.5}.draft-activation-warning{margin:10px 0;padding:10px 12px;border:1px solid #f4d59a;border-radius:8px;background:#fff8e8;color:#8b6200;font-size:12px;line-height:1.5}.authorization-hint{margin:8px 0;color:#6a8294;font-size:12px;line-height:1.5}.detail-actions{display:grid;gap:8px}.actions > .primary-btn:last-child:not(:only-child){background:#fff;color:#24516c;border-color:#b9d1e2;box-shadow:none}.actions > .primary-btn:last-child:not(:only-child):hover{background:#eef7fc;color:#0b527b;border-color:#78a9c8;transform:none}.mandate-authorization-dialog{width:min(760px,94vw)}.mandate-authorization-dialog .wide{grid-column:1/-1}
    .status.expired{background:#edf0f2;color:#5f6e78}.workflow-card{display:flex;align-items:center;justify-content:space-between;gap:16px;margin:14px 0;padding:16px 18px;border:1px solid #cfe0e6;border-left:4px solid #0b8f99;border-radius:12px;background:#f8fcfc}.workflow-card h3{margin:3px 0 4px;font-size:16px;color:#153b57}.workflow-card p{margin:0;color:#718b9b;font-size:12px}.workflow-actions{display:flex;gap:8px;flex-wrap:wrap;justify-content:flex-end}.modal-authorization-workspace{width:min(620px,94vw)}.authorization-steps{display:grid;grid-template-columns:repeat(3,1fr);gap:8px}.authorization-steps span{padding:9px 10px;border:1px solid #dbe8ed;border-radius:8px;color:#8197a5;font-size:11px;text-align:center}.authorization-steps span.active{border-color:#8ed1d2;background:#eefafa;color:#087782;font-weight:700}.authorization-document-row{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:12px;border:1px solid #dbe8ed;border-radius:10px;background:#f8fbfc}.authorization-document-row strong{display:block;color:#24516c;font-size:12px}.authorization-document-row small{display:block;margin-top:4px;color:#8197a5;font-size:11px}
    </style>
    <style scoped>
.authorization-document-row{display:grid;grid-template-columns:1fr;gap:12px}.authorization-document-row .signing-recipient-fields{margin-top:0;padding:0;border:0;background:transparent}.authorization-document-row>.primary-btn{justify-self:end}
    </style>
    <style>
.signing-recipient-fields{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:12px;padding:12px;border:1px solid #dbe8ed;border-radius:10px;background:#fbfdfe}.signing-recipient-fields label{display:grid;gap:6px;color:#426478;font-size:12px;font-weight:600}.signing-recipient-fields input{height:38px;border:1px solid #c9dbe4;border-radius:8px;padding:0 10px;color:#244b64;background:#fff;outline:none}.signing-recipient-fields input:focus{border-color:#0b8f99;box-shadow:0 0 0 3px #0b8f9918}
@media(max-width:620px){.signing-recipient-fields{grid-template-columns:1fr}}
.mandate-workspace:has(.modal-backdrop) .detail-panel { display: none; }
.mandate-workspace.workflow-pending .detail-panel { display: none; }
</style>
<style scoped>
.mandate-workspace {
  width: auto;
  min-width: 0;
  padding: 20px 28px 32px;
}

.workspace-head {
  min-height: 110px;
  margin: 0 0 18px;
  padding: 20px 22px;
  border: 1px solid #d6e5ea;
  border-radius: 16px;
  background:
    radial-gradient(circle at 88% 18%, rgba(18, 156, 160, .12), transparent 28%),
    linear-gradient(135deg, #ffffff 0%, #f3fbfa 100%);
  box-shadow: 0 10px 28px rgba(23, 63, 82, .08);
}

.workspace-head h2 {
  margin: 5px 0 6px;
  color: #123d58;
  font-size: 25px;
  line-height: 1.15;
}

.workspace-head p {
  color: #637e8e;
  font-size: 13px;
  line-height: 1.6;
}

.workspace-head .eyebrow {
  display: inline-flex;
  align-items: center;
  min-height: 23px;
  padding: 0 9px;
  border-radius: 999px;
  background: #e8f7f5;
  color: #087d84;
  letter-spacing: .08em;
}

.primary-btn-lg {
  flex: 0 0 auto;
  min-height: 44px;
  padding: 0 20px;
  border-color: #d99a00;
  border-radius: 10px;
  background: linear-gradient(135deg, #f4b400, #e69b00);
  box-shadow: 0 8px 18px rgba(218, 151, 0, .23);
  font-size: 14px;
}

.primary-btn-lg:hover {
  border-color: #c78900;
  background: linear-gradient(135deg, #eaaa00, #d89000);
}

.summary-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.summary-card {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  min-height: 108px;
  padding: 17px 16px;
  overflow: hidden;
  border-color: #d6e4ea;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(23, 63, 82, .07);
}

.summary-card::before {
  content: '';
  position: absolute;
  inset: 0 0 auto;
  height: 3px;
  background: #0d969b;
}

.summary-icon {
  display: grid !important;
  width: 42px;
  height: 42px;
  place-items: center;
  border: 1px solid #cfe3e8;
  border-radius: 12px;
  background: #eff8fa;
  color: #13748b !important;
}

.summary-copy {
  min-width: 0;
}

.summary-card .summary-copy > span {
  overflow: hidden;
  color: #4f6e80;
  font-size: 12px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-card strong {
  margin: 5px 0 3px;
  color: #0d3c5b;
  font-size: 28px;
  font-weight: 700;
}

.summary-card small {
  color: #8298a6;
  font-size: 11px;
}

.summary-draft::before { background: #7c91a1; }
.summary-draft .summary-icon { background: #f3f6f8; color: #60798a !important; }
.summary-pendingReview::before { background: #e3a000; }
.summary-pendingReview .summary-icon { border-color: #f0ddb0; background: #fff8e7; color: #af7600 !important; }
.summary-active::before { background: #1a9a61; }
.summary-active .summary-icon { border-color: #bfe6d1; background: #eefaf4; color: #168455 !important; }
.summary-suspended::before { background: #d07a45; }
.summary-suspended .summary-icon { border-color: #efd4c3; background: #fff4ee; color: #b86334 !important; }

.toolbar {
  min-height: 72px;
  margin: 0 0 18px;
  padding: 13px 14px;
  border-color: #d6e4ea;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 7px 20px rgba(23, 63, 82, .055);
}

.search-field {
  min-width: 260px;
  min-height: 44px;
  padding: 0 14px;
  border-color: #cbdce4;
  border-radius: 10px;
  color: #6a8797;
}

.search-field:focus-within {
  border-color: #45aeb0;
  box-shadow: 0 0 0 3px rgba(11, 143, 153, .1);
}

.toolbar select {
  width: 210px;
  min-height: 44px;
  border-radius: 10px;
  font-size: 13px;
}

.reload-btn {
  min-width: 116px;
  min-height: 44px;
  border-radius: 10px;
  font-size: 13px;
}

.table-card {
  border-color: #d4e3e9;
  border-radius: 15px;
  box-shadow: 0 10px 28px rgba(23, 63, 82, .075);
}

.table-card-head {
  min-height: 68px;
  padding: 15px 20px;
  background: linear-gradient(180deg, #fff, #fbfdfd);
}

.table-card-head strong {
  color: #123f5c;
  font-size: 16px;
}

.table-card-head small,
.result-count {
  color: #718b9b;
  font-size: 12px;
}

th,
td {
  padding: 14px 16px;
  font-size: 13px;
}

th {
  height: 46px;
  background: #f3f8fa;
  color: #42677d;
  font-size: 12px;
  letter-spacing: .01em;
}

tbody tr:hover,
tbody tr.selected {
  background: #f0f9f8;
}

tbody tr.selected td:first-child {
  box-shadow: inset 3px 0 #0e969b;
}

td strong {
  font-size: 13px;
  font-weight: 700;
}

td small {
  display: inline-block;
  margin-top: 4px;
  font-size: 11px;
}

.actions button {
  min-height: 32px;
  padding: 6px 11px;
  border-radius: 8px;
  font-size: 12px;
}

.status {
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 700;
}

:deep(.list-pager) {
  min-height: 58px;
  margin-top: 12px;
  padding: 10px 14px;
  border: 1px solid #d6e4ea;
  border-radius: 13px;
  background: #fff;
  box-shadow: 0 6px 18px rgba(23, 63, 82, .045);
  font-size: 12px;
}

.detail-panel {
  right: 28px;
  top: 150px;
  width: min(430px, calc(100% - 56px));
  padding: 22px;
  border-radius: 16px;
  box-shadow: 0 22px 52px rgba(18, 47, 61, .24);
}

@media (max-width: 1180px) {
  .mandate-workspace { padding: 18px 20px 28px; }
  .summary-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .detail-panel { right: 20px; width: min(430px, calc(100% - 40px)); }
}

@media (max-width: 760px) {
  .mandate-workspace { padding: 14px 12px 24px; }
  .workspace-head { align-items: flex-start; flex-direction: column; padding: 18px; }
  .workspace-head .primary-btn-lg { width: 100%; }
  .summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
  .toolbar { align-items: stretch; flex-direction: column; }
  .search-field,
  .toolbar select,
  .toolbar button { width: 100%; }
  .detail-panel { right: 12px; width: calc(100% - 24px); }
}

@media (max-width: 480px) {
  .summary-grid { grid-template-columns: 1fr; }
}
</style>
