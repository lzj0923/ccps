<template>
  <section class="rental-signing-page">
    <header class="rental-signing-header">
      <div><span>{{ $t('rentalFiles.kicker') }}</span><h2>{{ $t('rentalFiles.title') }}</h2><p>{{ $t('rentalFiles.description') }}</p></div>
      <div class="rental-signing-total"><strong>{{ completedSigningSteps }} / 5</strong><small>{{ $t('rentalFiles.signingProgress') }}</small></div>
    </header>

    <div class="rental-signing-layout">
      <aside class="rental-signing-property-picker">
        <div class="rental-signing-picker-head"><strong>{{ $t('rentalFiles.selectProperty') }}</strong><span>{{ properties.length }}</span></div>
        <input v-model.trim="propertySearch" type="search" :placeholder="$t('rentalFiles.propertySearch')">
        <div v-if="loading" class="rental-signing-state">{{ $t('rentalFiles.loadingProperties') }}</div>
        <div v-else-if="loadError" class="rental-signing-state error"><span>{{ loadError }}</span><button type="button" @click="loadProperties">{{ $t('rentalFiles.retry') }}</button></div>
        <div v-else-if="!filteredProperties.length" class="rental-signing-state">{{ $t('rentalFiles.noProperties') }}</div>
        <div v-else class="rental-signing-property-list">
          <button v-for="property in filteredProperties" :key="property.rowKey" type="button" :class="{ active: selectedPropertyKey === property.rowKey }" @click="selectProperty(property)">
            <span class="rental-signing-property-icon">⌂</span><span><strong>{{ property.projectName || $t('rentalFiles.unsetProject') }}</strong><small>{{ property.unitNo || $t('rentalFiles.unsetUnit') }} · {{ property.ownerName || $t('rentalFiles.unsetOwner') }}</small></span><i>›</i>
          </button>
        </div>
      </aside>

      <main class="rental-signing-main">
        <div v-if="workspaceLoading" class="rental-signing-empty">{{ $t('rentalFiles.loadingFiles') }}</div>
        <div v-else-if="workspaceError" class="rental-signing-empty error"><strong>{{ $t('rentalFiles.loadFailed') }}</strong><span>{{ workspaceError }}</span><button type="button" @click="loadSigningWorkspace">{{ $t('rentalFiles.retry') }}</button></div>
        <div v-else-if="!selectedProperty" class="rental-signing-empty"><strong>{{ $t('rentalFiles.chooseProperty') }}</strong><span>{{ $t('rentalFiles.choosePropertyHint') }}</span></div>
        <template v-else>
          <div class="rental-signing-property-head">
            <div><span>{{ $t('rentalFiles.currentProperty') }}</span><h3>{{ propertyTitle }}</h3><p>{{ selectedProperty.ownerName || $t('rentalFiles.unsetOwner') }} · {{ currentMandate?.mandateNo || $t('rentalFiles.noMandate') }}</p></div>
            <strong :class="{ historical: selectedCycleIsHistorical }">{{ currentMandate ? (selectedCycleIsHistorical ? $t('rentalFiles.historyRental') : $t('rentalFiles.currentRental')) : $t('rentalFiles.waitingRental') }}</strong>
          </div>

          <section v-if="rentalCycles.length" class="rental-cycle-selector">
            <div><span>{{ $t('rentalFiles.rentalCycle') }}</span><strong>{{ selectedCycleIsHistorical ? $t('rentalFiles.historyCycleTitle') : $t('rentalFiles.currentCycleTitle') }}</strong><small>{{ $t(selectedCycleIsHistorical ? 'rentalFiles.historyCycleHint' : 'rentalFiles.currentCycleHint') }}</small></div>
            <select v-model="selectedCycleId" @change="selectCycle"><option v-for="cycle in rentalCycles" :key="cycle.id" :value="String(cycle.id)">{{ cycleOptionLabel(cycle) }}</option></select>
          </section>
          <div v-if="selectedCycleIsHistorical" class="rental-cycle-readonly">{{ $t('rentalFiles.historyReadOnly') }}</div>

          <section class="rental-signing-actions">
            <div class="rental-signing-section-head"><div><h4>{{ $t('rentalFiles.signingTasks') }}</h4><p>{{ $t('rentalFiles.signingTasksHint') }}</p></div></div>
            <div class="rental-signing-card-grid">
              <article v-for="task in signingTasks" :key="task.key" :class="['rental-signing-card', `is-${task.status}`, { highlighted: task.actions.includes(requestedAction) }]">
                <div class="rental-signing-card-head"><span>{{ task.icon }}</span><div><strong>{{ task.title }}</strong><small>{{ task.description }}</small></div><em>{{ task.statusLabel }}</em></div>
                <dl><div><dt>{{ $t('rentalFiles.document') }}</dt><dd>{{ task.fileName || '—' }}</dd></div><div><dt>{{ $t('rentalFiles.latestStatus') }}</dt><dd>{{ task.detail }}</dd></div></dl>
                <div class="rental-signing-card-actions">
                  <button v-if="task.action" type="button" :disabled="task.disabled || actionBusy" @click="runSigningTask(task)">{{ task.actionLabel }}</button>
                  <button v-if="task.canRegenerate" type="button" class="secondary" :disabled="actionBusy" @click="regenerateTask(task)">{{ $t('rentalFiles.regenerateFile') }}</button>
                  <template v-if="task.templateType">
                    <input :ref="`template-${task.templateType}`" class="rental-template-input" type="file" accept="application/pdf" @change="replaceTemplate(task.templateType, $event)">
                    <button type="button" class="secondary" :disabled="actionBusy" @click="chooseTemplate(task.templateType)">{{ $t('rentalFiles.replaceTemplate') }}</button>
                    <small>{{ $t('rentalFiles.templateVersion') }} {{ templateVersions[task.templateType]?.version || 'v1' }}</small>
                  </template>
                </div>
              </article>
            </div>
          </section>

          <section v-if="signingPanel" class="rental-signing-panel">
            <header><div><span>{{ $t('rentalFiles.signingPanelKicker') }}</span><h4>{{ signingPanelTitle }}</h4><p>{{ signingPanelHint }}</p></div><button type="button" @click="closeSigningPanel">×</button></header>
            <div v-if="signingPanel.endsWith('Status')" class="rental-signing-status-detail">
              <div><span>{{ $t('rentalFiles.signerName') }}</span><strong>{{ activeSigning.signerName || '—' }}</strong></div>
              <div><span>{{ $t('rentalFiles.signerEmail') }}</span><strong>{{ activeSigning.signerEmail || '—' }}</strong></div>
              <div><span>{{ $t('rentalFiles.requestedAt') }}</span><strong>{{ formatDate(activeSigning.requestedAt) }}</strong></div>
              <div><span>{{ $t('rentalFiles.expiresAt') }}</span><strong>{{ formatDate(activeSigning.expiresAt) }}</strong></div>
            </div>
            <form v-else class="rental-signing-form" @submit.prevent="submitSigning">
              <label>{{ $t('rentalFiles.signerName') }}<input v-model.trim="signerForm.name" required></label>
              <label>{{ $t('rentalFiles.signerEmail') }}<input v-model.trim="signerForm.email" type="email" required></label>
              <div v-if="activeSignerRole" class="rental-signer-role"><span>{{ $t('rentalFiles.signerRole') }}</span><strong>{{ signerRoleLabel(activeSignerRole) }}</strong></div>
              <p v-if="actionError">{{ actionError }}</p>
              <div><button type="button" class="secondary" @click="closeSigningPanel">{{ $t('rentalFiles.cancel') }}</button><button type="submit" class="primary" :disabled="actionBusy">{{ actionBusy ? $t('rentalFiles.processing') : $t('rentalFiles.startSigning') }}</button></div>
            </form>
          </section>

          <section class="rental-generated-files">
            <div class="rental-signing-section-head"><div><h4>{{ $t('rentalFiles.generatedFiles') }}</h4><p>{{ $t('rentalFiles.generatedFilesHint') }}</p></div><span>{{ filteredFiles.length }} {{ $t('rentalFiles.files') }}</span></div>
            <div class="rental-files-toolbar"><input v-model.trim="fileSearch" type="search" :placeholder="$t('rentalFiles.fileSearch')"><select v-model="categoryFilter"><option value="">{{ $t('rentalFiles.allCategories') }}</option><option v-for="category in categories" :key="category" :value="category">{{ categoryLabel(category) }}</option></select></div>
            <div v-if="filteredFiles.length" class="rental-files-table-wrap">
              <table class="rental-files-table"><thead><tr><th>{{ $t('rentalFiles.fileName') }}</th><th>{{ $t('rentalFiles.category') }}</th><th>{{ $t('rentalFiles.rentalNo') }}</th><th>{{ $t('rentalFiles.status') }}</th><th>{{ $t('rentalFiles.createdAt') }}</th><th>{{ $t('rentalFiles.action') }}</th></tr></thead><tbody>
                <tr v-for="file in filteredFiles" :key="file.key"><td><div class="rental-file-name"><strong :title="file.name">{{ file.name }}</strong><small>{{ file.reference || '—' }}</small></div></td><td><span class="rental-file-category">{{ categoryLabel(file.category) }}</span></td><td><span class="rental-file-mandate">{{ file.mandateNo || '—' }}</span></td><td><span class="rental-file-status" :class="fileStatusClass(file)">{{ fileStatusLabel(file) }}</span></td><td><time>{{ formatDate(file.date) }}</time></td><td><button type="button" class="rental-file-download" :disabled="downloadingKey === file.key" @click="downloadFile(file)">{{ downloadingKey === file.key ? $t('rentalFiles.downloading') : $t('rentalFiles.download') }}</button></td></tr>
              </tbody></table>
            </div>
            <div v-else class="rental-signing-empty compact">{{ $t('rentalFiles.noFiles') }}</div>
            <p v-if="actionError && !signingPanel" class="rental-signing-inline-error">{{ actionError }}</p>
          </section>

          <section v-if="propertyFiles.length" class="rental-property-files">
            <div class="rental-signing-section-head"><div><h4>{{ $t('rentalFiles.propertyFiles') }}</h4><p>{{ $t('rentalFiles.propertyFilesHint') }}</p></div><span>{{ propertyFiles.length }} {{ $t('rentalFiles.files') }}</span></div>
            <div class="rental-property-file-list"><article v-for="file in propertyFiles" :key="file.key"><div><strong :title="file.name">{{ file.name }}</strong><small>{{ file.reference || '—' }}</small></div><span>{{ formatDate(file.date) }}</span><button type="button" class="rental-file-download" :disabled="downloadingKey === file.key" @click="downloadFile(file)">{{ downloadingKey === file.key ? $t('rentalFiles.downloading') : $t('rentalFiles.download') }}</button></article></div>
          </section>
        </template>
      </main>
    </div>

    <div v-if="pmaFormOpen" class="pma-form-backdrop" @click.self="closePmaForm">
      <form class="pma-form-dialog" @submit.prevent="generatePma">
        <header>
          <div><span>{{ $t('rentalFiles.pmaFormKicker') }}</span><h3>{{ $t('rentalFiles.pmaFormTitle') }}</h3><p>{{ $t('rentalFiles.pmaFormHint') }}</p></div>
          <button type="button" :aria-label="$t('rentalFiles.cancel')" @click="closePmaForm">×</button>
        </header>
        <div class="pma-form-body">
          <section>
            <h4>{{ $t('rentalFiles.pmaOwnerSection') }}</h4>
            <div class="pma-form-grid">
              <label>{{ $t('rentalFiles.pmaOwnerName') }}<input v-model.trim="pmaForm.landlordName" required></label>
              <label>{{ $t('rentalFiles.pmaOwnerIdentity') }}<input v-model.trim="pmaForm.landlordIdentity" required></label>
              <label class="wide">{{ $t('rentalFiles.pmaPropertyAddress') }}<textarea v-model.trim="pmaForm.propertyAddress" rows="2" required></textarea></label>
              <label class="wide">{{ $t('rentalFiles.pmaOwnerAddress') }}<textarea v-model.trim="pmaForm.ownerAddress" rows="2" required></textarea></label>
              <label>{{ $t('rentalFiles.pmaOwnerEmail') }}<input v-model.trim="pmaForm.ownerEmail" type="email" required></label>
              <label>{{ $t('rentalFiles.pmaOwnerPhone') }}<input v-model.trim="pmaForm.ownerPhone" required></label>
            </div>
          </section>
          <section>
            <h4>{{ $t('rentalFiles.pmaAgreementSection') }}</h4>
            <div class="pma-form-grid dates">
              <label>{{ $t('rentalFiles.pmaAgreementDate') }}<input v-model="pmaForm.agreementDate" type="date" required></label>
              <label>{{ $t('rentalFiles.pmaStartDate') }}<input v-model="pmaForm.startDate" type="date" required></label>
              <label>{{ $t('rentalFiles.pmaEndDate') }}<input v-model="pmaForm.endDate" type="date" required></label>
            </div>
          </section>
          <section>
            <h4>{{ $t('rentalFiles.pmaBankSection') }}</h4>
            <div class="pma-form-grid">
              <label>{{ $t('rentalFiles.pmaPayeeName') }}<input v-model.trim="pmaForm.bankPayeeName" required></label>
              <label>{{ $t('rentalFiles.pmaBankName') }}<input v-model.trim="pmaForm.bankName" required></label>
              <label class="wide">{{ $t('rentalFiles.pmaBankAddress') }}<textarea v-model.trim="pmaForm.bankAddress" rows="2" required></textarea></label>
              <label>{{ $t('rentalFiles.pmaBranchCode') }}<input v-model.trim="pmaForm.bankBranchCode" required></label>
              <label>{{ $t('rentalFiles.pmaAccountNo') }}<input v-model.trim="pmaForm.bankAccountNo" required></label>
              <label>{{ $t('rentalFiles.pmaSwiftCode') }}<input v-model.trim="pmaForm.bankSwiftCode" required></label>
            </div>
          </section>
        </div>
        <p v-if="actionError" class="pma-form-error">{{ actionError }}</p>
        <footer><button type="button" class="secondary" @click="closePmaForm">{{ $t('rentalFiles.cancel') }}</button><button type="submit" class="primary" :disabled="actionBusy">{{ actionBusy ? $t('rentalFiles.processing') : $t('rentalFiles.confirmGeneratePma') }}</button></footer>
      </form>
    </div>

  </section>
</template>

<script>
import {
  downloadAdminPropertyAttachment,
  downloadAdminPropertyContractRecord,
  downloadAdminPropertyHandoverReport,
  downloadAdminRentalMandateDocument,
  fetchAdminProperties,
  fetchAdminPropertyAttachments,
  fetchAdminPropertyHandoverReports,
  fetchAdminPropertyWorkspaceById,
  fetchAdminRentalMandateDocuments,
  fetchAdminRentalMandates,
  fetchAdminContractTemplateVersion,
  generateAdminContractTemplate,
  replaceAdminContractTemplate,
  startAdminLeaseSignature,
  startAdminMandateDocumentSignature,
  uploadAdminLeaseContract,
  uploadAdminRentalMandateDocument,
} from '../services/propertyApi';
import { reportBelongsToMandate, selectCurrentRentalMandate } from '../utils/rentalCycleWorkbench';

const CATEGORY_ORDER = ['management', 'appointment', 'authorization', 'contract', 'handover'];
const COMPLETE_CONTRACT_STATUSES = new Set(['signed', 'completed', 'active', 'approved']);
const EDITABLE_MANDATE_STATUSES = new Set(['draft', 'pending_review', 'active', 'suspended']);
const emptyPmaForm = () => ({ landlordName: '', landlordIdentity: '', propertyAddress: '', ownerAddress: '', ownerEmail: '', ownerPhone: '', agreementDate: '', startDate: '', endDate: '', bankPayeeName: '', bankName: '', bankAddress: '', bankBranchCode: '', bankAccountNo: '', bankSwiftCode: '' });

export default {
  name: 'AdminRentalSigningWorkspace',
  inject: ['page'],
  data() {
    return {
      properties: [], mandates: [], selectedProperty: null, selectedPropertyKey: '', selectedCycleId: '', propertySearch: '',
      workspace: null, mandateDocuments: [], propertyAttachments: [], handoverReports: [], files: [],
      fileSearch: '', categoryFilter: '', loading: false, loadError: '', workspaceLoading: false, workspaceError: '',
      actionBusy: false, actionError: '', signingPanel: '', signerForm: { name: '', email: '' }, downloadingKey: '',
      pmaFormOpen: false, pmaForm: emptyPmaForm(),
      templateVersions: {},
      requestedAction: new URLSearchParams(window.location.search).get('action') || '',
    };
  },
  computed: {
    filteredProperties() { const query = this.propertySearch.toLowerCase(); return this.properties.filter(property => !query || [property.projectName, property.unitNo, property.ownerName].some(value => String(value || '').toLowerCase().includes(query))); },
    propertyTitle() { return `${this.selectedProperty?.projectName || this.$t('rentalFiles.unsetProject')} · ${this.selectedProperty?.unitNo || this.$t('rentalFiles.unsetUnit')}`; },
    propertyMandates() { return this.mandates.filter(item => String(item.ownerUnitId) === String(this.selectedProperty?.ownerUnitId)); },
    activeCycle() {
      return selectCurrentRentalMandate({ property: this.selectedProperty || {}, mandates: this.propertyMandates })
        || [...this.propertyMandates]
          .filter(item => EDITABLE_MANDATE_STATUSES.has(String(item.status || '').toLowerCase()))
          .sort((left, right) => String(right.startDate || right.createdAt || '').localeCompare(String(left.startDate || left.createdAt || '')) || Number(right.id || 0) - Number(left.id || 0))[0]
        || null;
    },
    rentalCycles() { return [...this.propertyMandates].sort((left, right) => Number(String(right.id) === String(this.activeCycle?.id)) - Number(String(left.id) === String(this.activeCycle?.id)) || String(right.startDate || right.createdAt || '').localeCompare(String(left.startDate || left.createdAt || '')) || Number(right.id || 0) - Number(left.id || 0)); },
    currentMandate() { return this.rentalCycles.find(item => String(item.id) === String(this.selectedCycleId)) || this.activeCycle || this.rentalCycles[0] || null; },
    selectedCycleIsHistorical() { return Boolean(this.currentMandate && (!this.activeCycle || String(this.currentMandate.id) !== String(this.activeCycle.id) || !EDITABLE_MANDATE_STATUSES.has(String(this.currentMandate.status || '').toLowerCase()))); },
    cycleLeases() { return (this.workspace?.leases || []).filter(lease => String(lease.rentalMandateId || lease.mandateId) === String(this.currentMandate?.id)).sort((left, right) => String(right.startDate || '').localeCompare(String(left.startDate || '')) || Number(right.id || right.leaseId || 0) - Number(left.id || left.leaseId || 0)); },
    currentLease() { return this.cycleLeases.find(lease => String(lease.status || '').toLowerCase() === 'active') || this.cycleLeases[0] || null; },
    pmaDocument() { return this.mandateDocuments.find(item => String(item.mandateId) === String(this.currentMandate?.id) && String(item.relationType || '').toLowerCase() === 'property_management_agreement_draft') || null; },
    pmaSigned() { return this.mandateDocuments.some(item => String(item.mandateId) === String(this.currentMandate?.id) && String(item.relationType || '').toLowerCase() === 'property_management_agreement_signed'); },
    pmaSigning() { const document = this.pmaDocument || {}; return { status: this.pmaSigned ? 'signed' : String(document.signatureStatus || 'not_started').toLowerCase(), signerRole: document.signatureSignerRole || '', signingOrder: Number(document.signatureSigningOrder || 0), signerName: document.signatureSignerName || '', signerEmail: document.signatureSignerEmail || '', requestedAt: document.signatureRequestedAt || null, expiresAt: document.signatureExpiresAt || null }; },
    nextPmaSignerRole() { if (!this.pmaDocument || this.pmaSigning.signingOrder < 1) return 'owner'; if (this.pmaSigning.status !== 'signed') return this.pmaSigning.signerRole || 'owner'; if (this.pmaSigning.signingOrder === 1) return 'company'; if (this.pmaSigning.signingOrder === 2) return 'customer_service'; return ''; },
    rentalAppointmentDocument() { return this.mandateDocuments.find(item => String(item.mandateId) === String(this.currentMandate?.id) && ['rental_appointment_draft', 'authorization_draft'].includes(String(item.relationType || '').toLowerCase())) || null; },
    rentalAppointmentSigning() { const document = this.rentalAppointmentDocument || {}; return { status: this.rentalAppointmentSigned ? 'signed' : String(document.signatureStatus || 'not_started').toLowerCase(), signerName: document.signatureSignerName || '', signerEmail: document.signatureSignerEmail || '', requestedAt: document.signatureRequestedAt || null, expiresAt: document.signatureExpiresAt || null }; },
    rentalAppointmentSigned() { return this.mandateDocuments.some(item => String(item.mandateId) === String(this.currentMandate?.id) && String(item.relationType || '').toLowerCase() === 'rental_appointment_signed') || String(this.rentalAppointmentDocument?.signatureStatus || '').toLowerCase() === 'signed'; },
    authorizationSigning() { const document = this.authorizationDocument || {}; const status = this.authorizationSigned ? 'signed' : String(document.signatureStatus || 'not_started').toLowerCase(); return { status, documentId: document.id || null, signerName: document.signatureSignerName || '', signerEmail: document.signatureSignerEmail || '', requestedAt: document.signatureRequestedAt || null, expiresAt: document.signatureExpiresAt || null, signedAt: document.signatureSignedAt || null }; },
    authorizationDocument() { return this.mandateDocuments.find(item => String(item.mandateId) === String(this.currentMandate?.id) && ['management_authorization_draft', 'authorization_draft', 'authorization'].includes(String(item.relationType || '').toLowerCase())) || null; },
    authorizationSigned() { return this.mandateDocuments.some(item => String(item.mandateId) === String(this.currentMandate?.id) && String(item.relationType || '').toLowerCase() === 'management_authorization_signed') || String(this.authorizationDocument?.signatureStatus || '').toLowerCase() === 'signed'; },
    currentLeaseContracts() { const leaseId = this.currentLease?.id || this.currentLease?.leaseId; return (this.workspace?.contracts || []).filter(item => String(item.leaseId) === String(leaseId)); },
    otrContract() {
      const mandateOtr = this.mandateDocuments.find(item => String(item.mandateId) === String(this.currentMandate?.id) && ['otr', 'otr_document'].includes(String(item.relationType || item.documentType || '').toLowerCase()));
      if (mandateOtr) return mandateOtr;
      const contracts = this.workspace?.contracts || [];
      const mandateReference = String(this.currentMandate?.mandateNo || this.currentMandate?.id || '');
      return this.currentLeaseContracts.find(item => item.contractType === 'O_LEASE_RESERVATION')
        || contracts.find(item => item.contractType === 'O_LEASE_RESERVATION' && mandateReference && String(item.contractNo || '').includes(mandateReference))
        || null;
    },
    leaseContract() { return this.currentLeaseContracts.find(item => item.contractType === 'L_LEASE') || null; },
    leaseSignatureStatus() { return String(this.currentLease?.signatureStatus || (this.leaseContractComplete ? 'signed' : this.leaseContract ? 'ready_to_sign' : 'not_generated')).toLowerCase(); },
    otrComplete() { return Boolean(this.otrContract && (['otr', 'otr_document'].includes(String(this.otrContract.relationType || this.otrContract.documentType || '').toLowerCase()) || this.contractComplete(this.otrContract))); },
    leaseContractComplete() { return this.contractComplete(this.leaseContract); },
    completedSigningSteps() { return [this.pmaSigned, this.rentalAppointmentSigned, this.authorizationSigned, this.otrComplete, this.leaseContractComplete].filter(Boolean).length; },
    signingTasks() {
      const noMandate = !this.currentMandate;
      const noLease = !this.currentLease;
      const readOnly = this.selectedCycleIsHistorical;
      const authorizationPending = ['pending', 'sent', 'viewed'].includes(String(this.authorizationSigning.status || '').toLowerCase());
      const rentalAppointmentPending = ['pending', 'sent', 'viewed'].includes(String(this.rentalAppointmentSigning.status || '').toLowerCase());
      const pmaPending = ['pending', 'sent', 'viewed'].includes(String(this.pmaSigning.status || '').toLowerCase());
      const leaseSigningStarted = ['pending', 'sent', 'viewed', 'signed'].includes(this.leaseSignatureStatus);
      return [
        { key: 'pma', icon: '管', title: this.$t('rentalFiles.pmaTitle'), description: this.$t('rentalFiles.pmaHint'), fileName: this.pmaDocument?.originalName, actions: ['generate_pma', 'start_pma_signing', 'view_pma_signing_status'], templateType: 'property-management-agreement',
          status: this.pmaSigned ? 'complete' : pmaPending ? 'pending' : noMandate ? 'unavailable' : 'todo', statusLabel: this.pmaSigned ? this.$t('rentalFiles.statusComplete') : pmaPending ? this.$t('rentalFiles.statusPending') : this.$t('rentalFiles.statusNotStarted'),
          detail: noMandate ? this.$t('rentalFiles.createMandateFirst') : this.pmaSigned ? this.$t('rentalFiles.pmaSigned') : pmaPending ? `${this.signerRoleLabel(this.pmaSigning.signerRole)}${this.$t('rentalFiles.awaitingSignature')}` : this.pmaDocument ? `${this.$t('rentalFiles.nextSigner')}：${this.signerRoleLabel(this.nextPmaSignerRole)}` : this.$t('rentalFiles.notGenerated'),
          action: !readOnly && !noMandate && !this.pmaSigned, actionLabel: pmaPending ? this.$t('rentalFiles.viewSigningStatus') : this.pmaDocument ? this.$t('rentalFiles.continueSigning') : this.$t('rentalFiles.generatePma'), canRegenerate: !readOnly && Boolean(this.pmaDocument) && !this.pmaSigned && !pmaPending, disabled: noMandate || readOnly },
        { key: 'rentalAppointment', icon: '任', title: this.$t('rentalFiles.rentalAppointmentTitle'), description: this.$t('rentalFiles.rentalAppointmentHint'), fileName: this.rentalAppointmentDocument?.originalName, actions: ['generate_rental_appointment', 'start_rental_appointment_signing'],
          status: this.rentalAppointmentSigned ? 'complete' : rentalAppointmentPending ? 'pending' : noMandate ? 'unavailable' : 'todo', statusLabel: this.rentalAppointmentSigned ? this.$t('rentalFiles.statusComplete') : rentalAppointmentPending ? this.$t('rentalFiles.statusPending') : this.$t('rentalFiles.statusNotStarted'),
          detail: noMandate ? this.$t('rentalFiles.createMandateFirst') : this.rentalAppointmentSigned ? this.$t('rentalFiles.rentalAppointmentSigned') : rentalAppointmentPending ? this.$t('rentalFiles.rentalAppointmentAwaiting') : this.rentalAppointmentDocument ? this.$t('rentalFiles.readyToSign') : this.$t('rentalFiles.notGenerated'),
          action: !readOnly && !noMandate && !this.rentalAppointmentSigned, actionLabel: rentalAppointmentPending ? this.$t('rentalFiles.viewSigningStatus') : this.rentalAppointmentDocument ? this.$t('rentalFiles.startSigning') : this.$t('rentalFiles.generateRentalAppointment'), canRegenerate: !readOnly && Boolean(this.rentalAppointmentDocument) && !this.rentalAppointmentSigned && !rentalAppointmentPending, disabled: noMandate || readOnly },
        { key: 'authorization', icon: '授', title: this.$t('rentalFiles.authorizationTitle'), description: this.$t('rentalFiles.authorizationHint'), fileName: this.authorizationDocument?.originalName, actions: ['generate_authorization', 'start_authorization_signing', 'view_signing_status'],
          templateType: 'management-authorization',
          status: this.authorizationSigned ? 'complete' : authorizationPending ? 'pending' : noMandate ? 'unavailable' : 'todo', statusLabel: this.authorizationSigned ? this.$t('rentalFiles.statusComplete') : authorizationPending ? this.$t('rentalFiles.statusPending') : this.$t('rentalFiles.statusNotStarted'),
          detail: noMandate ? this.$t('rentalFiles.createMandateFirst') : this.authorizationSigned ? this.$t('rentalFiles.authorizationSigned') : authorizationPending ? this.$t('rentalFiles.authorizationAwaiting') : this.authorizationDocument ? this.$t('rentalFiles.readyToSign') : this.$t('rentalFiles.notGenerated'),
          action: !readOnly && !noMandate && !this.authorizationSigned, actionLabel: authorizationPending ? this.$t('rentalFiles.viewSigningStatus') : this.authorizationDocument ? this.$t('rentalFiles.startSigning') : this.$t('rentalFiles.generateAuthorization'), canRegenerate: !readOnly && Boolean(this.authorizationDocument) && !this.authorizationSigned && !authorizationPending, disabled: noMandate || readOnly },
        { key: 'otr', icon: 'O', title: this.$t('rentalFiles.otrTitle'), description: this.$t('rentalFiles.otrHint'), fileName: this.otrContract?.originalName, actions: ['generate_otr'], status: this.otrComplete ? 'complete' : noMandate ? 'unavailable' : 'todo', statusLabel: this.otrComplete ? this.$t('rentalFiles.statusComplete') : this.$t('rentalFiles.statusNotStarted'), detail: noMandate ? this.$t('rentalFiles.createMandateFirst') : this.otrComplete ? this.$t('rentalFiles.generatedAndStored') : this.$t('rentalFiles.notGenerated'), action: !readOnly && !noMandate && !this.otrComplete, actionLabel: this.$t('rentalFiles.generateOtr'), canRegenerate: !readOnly && Boolean(this.otrContract), disabled: noMandate || readOnly },
        { key: 'lease', icon: '签', title: this.$t('rentalFiles.leaseTitle'), description: this.$t('rentalFiles.leaseHint'), fileName: this.leaseContract?.originalName, actions: ['sign_lease_contract'], status: this.leaseContractComplete ? 'complete' : noLease ? 'unavailable' : this.leaseContract ? 'pending' : 'todo', statusLabel: this.leaseContractComplete ? this.$t('rentalFiles.statusComplete') : this.leaseContract ? this.$t('rentalFiles.statusPending') : this.$t('rentalFiles.statusNotStarted'), detail: noLease ? this.$t('rentalFiles.createLeaseFirst') : this.leaseContractComplete ? this.$t('rentalFiles.leaseSigned') : this.leaseContract ? this.$t('rentalFiles.leaseAwaiting') : this.$t('rentalFiles.notGenerated'), action: !readOnly && !noLease && !this.leaseContractComplete, actionLabel: this.$t('rentalFiles.generateAndSignLease'), canRegenerate: !readOnly && Boolean(this.leaseContract) && !leaseSigningStarted, disabled: noLease || readOnly },
      ];
    },
    activeSignerRole() { return this.signingPanel === 'pma' ? this.nextPmaSignerRole : ['rentalAppointment', 'authorization'].includes(this.signingPanel) ? 'owner' : ''; },
    activeSigning() { return this.signingPanel.startsWith('pma') ? this.pmaSigning : this.signingPanel.startsWith('rentalAppointment') ? this.rentalAppointmentSigning : this.authorizationSigning; },
    signingPanelTitle() { return this.signingPanel.endsWith('Status') ? this.$t('rentalFiles.viewSigningStatus') : this.signingPanel === 'pma' ? this.$t('rentalFiles.pmaSigningTitle') : this.signingPanel === 'rentalAppointment' ? this.$t('rentalFiles.rentalAppointmentSigningTitle') : this.signingPanel === 'authorization' ? this.$t('rentalFiles.authorizationSigningTitle') : this.$t('rentalFiles.leaseSigningTitle'); },
    signingPanelHint() { return this.signingPanel.endsWith('Status') ? this.$t('rentalFiles.authorizationAwaiting') : this.$t('rentalFiles.signingPanelHint'); },
    categories() { return CATEGORY_ORDER.filter(category => this.files.some(file => file.category === category)); },
    filteredFiles() { const query = this.fileSearch.toLowerCase(); return this.files.filter(file => (!this.categoryFilter || file.category === this.categoryFilter) && (!query || [file.name, file.reference, file.mandateNo, this.categoryLabel(file.category)].some(value => String(value || '').toLowerCase().includes(query)))); },
    propertyFiles() { const ownerId = Number(this.selectedProperty?.ownerId); const ownerUnitId = Number(this.selectedProperty?.ownerUnitId); return (this.propertyAttachments || []).map(attachment => ({ key: `property-${attachment.id}`, kind: 'property', category: 'property', id: attachment.id, ownerId, ownerUnitId, name: attachment.originalName || attachment.title || this.$t('rentalFiles.unnamedFile'), reference: attachment.title || attachment.remarks, status: attachment.enabled === false ? 'disabled' : 'active', date: attachment.createdAt || attachment.updatedAt })).sort((left, right) => String(right.date || '').localeCompare(String(left.date || ''))); },
  },
  mounted() { this.loadProperties(); this.loadTemplateVersions(); },
  methods: {
    async loadProperties() {
      this.loading = true; this.loadError = '';
      try {
        const [propertyResponse, mandateResponse] = await Promise.all([fetchAdminProperties({ page: 1, pageSize: 500 }), fetchAdminRentalMandates({ page: 1, pageSize: 500 })]);
        this.mandates = mandateResponse?.rows || [];
        const rentalUnitIds = new Set(this.mandates.map(item => String(item.ownerUnitId)));
        this.properties = (propertyResponse?.rows || []).map(item => ({ ...item.property, ownerId: item.ownerId, ownerName: item.ownerName, rowKey: `${item.ownerId}-${item.property.ownerUnitId}` })).filter(property => rentalUnitIds.has(String(property.ownerUnitId)));
        const requestedUnitId = new URLSearchParams(window.location.search).get('ownerUnitId');
        const selected = this.properties.find(property => String(property.ownerUnitId) === String(requestedUnitId)) || this.properties.find(property => property.rowKey === this.selectedPropertyKey) || this.properties[0];
        if (selected) await this.selectProperty(selected); else { this.selectedProperty = null; this.files = []; }
      } catch (error) { this.loadError = error.message || this.$t('rentalFiles.loadFailed'); }
      finally { this.loading = false; }
    },
    async selectProperty(property) { this.selectedProperty = property; this.selectedPropertyKey = property.rowKey; this.selectedCycleId = ''; this.fileSearch = ''; this.categoryFilter = ''; this.signingPanel = ''; await this.loadSigningWorkspace(); },
    async loadSigningWorkspace() {
      if (!this.selectedProperty) return;
      this.workspaceLoading = true; this.workspaceError = ''; this.actionError = '';
      try {
        const property = this.selectedProperty; const ownerId = Number(property.ownerId); const ownerUnitId = Number(property.ownerUnitId); const unitId = property.unitId || property.id || property.ownerUnitId;
        const safe = (promise, fallback = []) => promise.catch(() => fallback);
        const groupsPromise = Promise.all(
          this.propertyMandates.map(mandate => safe(fetchAdminRentalMandateDocuments(mandate.id))
            .then(documents => documents.map(document => ({ ...document, mandateId: mandate.id, mandateNo: mandate.mandateNo })))),
        );
        const [workspace, documents, attachments, handovers] = await Promise.all([fetchAdminPropertyWorkspaceById(unitId), groupsPromise, safe(fetchAdminPropertyAttachments(ownerId, ownerUnitId)), safe(fetchAdminPropertyHandoverReports(ownerId, ownerUnitId))]);
        this.workspace = workspace || {}; this.mandateDocuments = documents.flat(); this.propertyAttachments = attachments; this.handoverReports = handovers;
        if (!this.rentalCycles.some(cycle => String(cycle.id) === String(this.selectedCycleId))) this.selectedCycleId = String(this.activeCycle?.id || this.rentalCycles[0]?.id || '');
        this.buildFiles();
        const signing = this.authorizationSigning; this.signerForm = { name: signing.signerName || this.currentMandate?.ownerName || property.ownerName || '', email: signing.signerEmail || this.currentMandate?.ownerEmail || '' };
      } catch (error) { this.workspaceError = error.message || this.$t('rentalFiles.loadFailed'); }
      finally { this.workspaceLoading = false; }
    },
    buildFiles() {
      const files = []; const mandate = this.currentMandate;
      if (!mandate) { this.files = []; return; }
      const ownerId = Number(this.selectedProperty.ownerId); const ownerUnitId = Number(this.selectedProperty.ownerUnitId); const mandateId = String(mandate.id); const mandateReference = String(mandate.mandateNo || mandate.id); const cycleLeaseIds = new Set(this.cycleLeases.map(lease => String(lease.id || lease.leaseId)));
      for (const document of this.mandateDocuments.filter(item => String(item.mandateId) === mandateId)) {
        const relation = String(document.relationType || document.documentType || '').toLowerCase();
        const category = relation.includes('property_management_agreement') ? 'management' : relation.includes('rental_appointment') || relation === 'authorization_draft' ? 'appointment' : ['otr', 'otr_document'].includes(relation) ? 'contract' : 'authorization';
        files.push({ key: `mandate-${document.mandateId}-${document.id}`, kind: 'mandate', category, id: document.id, mandateId: document.mandateId, mandateNo: document.mandateNo || mandate.mandateNo, name: document.originalName || document.documentNo || this.$t('rentalFiles.unnamedFile'), reference: document.documentNo || document.relationType, status: document.signatureStatus || document.status || document.relationType, date: document.createdAt || document.updatedAt });
      }
      for (const contract of (this.workspace?.contracts || []).filter(item => cycleLeaseIds.has(String(item.leaseId)) || (item.contractType === 'O_LEASE_RESERVATION' && mandateReference && String(item.contractNo || '').includes(mandateReference)))) files.push({ key: `contract-${contract.id}`, kind: 'contract', category: 'contract', id: contract.id, ownerId, ownerUnitId, mandateNo: mandate.mandateNo, name: contract.originalName || contract.contractNo || this.$t('rentalFiles.contractFile'), reference: contract.contractNo || contract.contractType, status: contract.status, date: contract.createdAt || contract.updatedAt || contract.signedDate });
      const mandateStartedAt = mandate.createdAt || mandate.startDate; const nextMandate = this.propertyMandates.filter(item => String(item.id) !== mandateId && String(item.createdAt || item.startDate || '') > String(mandateStartedAt || '')).sort((left, right) => String(left.createdAt || left.startDate || '').localeCompare(String(right.createdAt || right.startDate || '')))[0] || null;
      for (const report of this.handoverReports.filter(item => reportBelongsToMandate(item, mandate, nextMandate))) files.push({ key: `handover-${report.id}`, kind: 'handover', category: 'handover', id: report.id, ownerId, ownerUnitId, mandateNo: mandate.mandateNo, name: report.originalName || report.title || this.$t('rentalFiles.handoverFile'), reference: report.reportNo || report.title, status: report.completed ? 'completed' : report.status, date: report.createdAt || report.reportDate });
      this.files = files.sort((left, right) => String(right.date || '').localeCompare(String(left.date || '')));
    },
    selectCycle() {
      this.fileSearch = ''; this.categoryFilter = ''; this.signingPanel = ''; this.actionError = ''; this.buildFiles();
      const signing = this.authorizationSigning; this.signerForm = { name: signing.signerName || this.currentMandate?.ownerName || this.selectedProperty?.ownerName || '', email: signing.signerEmail || this.currentMandate?.ownerEmail || '' };
    },
    cycleOptionLabel(cycle) {
      const current = String(cycle.id) === String(this.activeCycle?.id); const type = this.$t(current ? 'rentalFiles.currentCycleOption' : 'rentalFiles.historyCycleOption'); const period = [cycle.startDate, cycle.endDate].filter(Boolean).join(' ~ ');
      return `${type} · ${cycle.mandateNo || `RM-${cycle.id}`}${period ? ` · ${period}` : ''}`;
    },
    contractComplete(contract) { return Boolean(contract && (COMPLETE_CONTRACT_STATUSES.has(String(contract.status || '').toLowerCase()) || contract.signedDate)); },
    async runSigningTask(task) {
      if (this.selectedCycleIsHistorical) return;
      this.actionError = '';
      if (task.key === 'pma') {
        if (['pending', 'sent', 'viewed'].includes(String(this.pmaSigning.status || '').toLowerCase())) { this.signingPanel = 'pmaStatus'; return; }
        if (this.pmaDocument) { this.signerForm = { name: this.nextPmaSignerRole === 'owner' ? (this.currentMandate?.ownerName || this.selectedProperty?.ownerName || '') : '', email: this.nextPmaSignerRole === 'owner' ? (this.currentMandate?.ownerEmail || '') : '' }; this.signingPanel = 'pma'; return; }
        this.openPmaForm(); return;
      }
      if (task.key === 'rentalAppointment') {
        if (['pending', 'sent', 'viewed'].includes(String(this.rentalAppointmentSigning.status || '').toLowerCase())) { this.signingPanel = 'rentalAppointmentStatus'; return; }
        if (this.rentalAppointmentDocument) { this.signerForm = { name: this.currentMandate?.ownerName || this.selectedProperty?.ownerName || '', email: this.currentMandate?.ownerEmail || '' }; this.signingPanel = 'rentalAppointment'; return; }
        await this.generateRentalAppointment(); return;
      }
      if (task.key === 'authorization') {
        if (['pending', 'sent', 'viewed'].includes(String(this.authorizationSigning.status || '').toLowerCase())) { this.signingPanel = 'authorizationStatus'; return; }
        if (this.authorizationDocument) { this.signingPanel = 'authorization'; return; }
        await this.generateAuthorization(); return;
      }
      if (task.key === 'otr') { await this.generateOtr(); return; }
      if (task.key === 'lease') this.signingPanel = 'lease';
    },
    async regenerateTask(task) {
      if (this.selectedCycleIsHistorical || !task?.canRegenerate || this.actionBusy) return;
      if (task.key === 'pma') { this.openPmaForm(); return; }
      if (task.key === 'rentalAppointment') { await this.generateRentalAppointment(); return; }
      if (task.key === 'authorization') { await this.generateAuthorization(); return; }
      if (task.key === 'otr') { await this.generateOtr(); return; }
      if (task.key === 'lease') await this.generateLeaseDraft();
    },
    templateFields() {
      const mandate = this.currentMandate || {}; const property = this.selectedProperty || {};
      return {
        caseNo: mandate.mandateNo || (mandate.id ? `RM-${mandate.id}` : ''),
        projectName: property.projectName || '', unitNo: property.unitNo || '', propertyAddress: property.address || property.fullAddress || this.propertyTitle,
        landlordName: mandate.ownerName || property.ownerName || '',
        landlordIdentity: mandate.ownerIdentityNo || mandate.ownerIdentity || property.ownerIdentityNo || '',
        ownerEmail: mandate.ownerEmail || property.ownerEmail || '', ownerPhone: mandate.ownerPhone || property.ownerPhone || '',
        ownerAddress: mandate.ownerAddress || property.ownerAddress || '', agreementDate: mandate.startDate || new Date().toISOString().slice(0, 10),
        startDate: mandate.startDate || '', endDate: mandate.endDate || '', managementOffice: property.managementOffice || '',
        bankPayeeName: mandate.bankPayeeName || '', bankName: mandate.bankName || '', bankAccountNo: mandate.bankAccountNo || '', bankSwiftCode: mandate.bankSwiftCode || '',
      };
    },
    openPmaForm() {
      if (!this.currentMandate?.id || this.selectedCycleIsHistorical) return;
      const fields = this.templateFields();
      this.pmaForm = {
        ...emptyPmaForm(), ...fields,
        agreementDate: fields.agreementDate || new Date().toISOString().slice(0, 10),
        bankAddress: this.currentMandate.bankAddress || '',
        bankBranchCode: this.currentMandate.bankBranchCode || '',
      };
      this.actionError = '';
      this.pmaFormOpen = true;
    },
    closePmaForm() { if (!this.actionBusy) { this.pmaFormOpen = false; this.actionError = ''; } },
    async generatePma() {
      if (!this.currentMandate?.id || this.selectedCycleIsHistorical || this.actionBusy) return;
      this.actionBusy = true; this.actionError = '';
      try {
        const result = await generateAdminContractTemplate('property-management-agreement', { ...this.templateFields(), ...this.pmaForm });
        const file = new File([result.blob], result.filename, { type: 'application/pdf' });
        await uploadAdminRentalMandateDocument(this.currentMandate.id, 'property_management_agreement_draft', file);
        this.pmaFormOpen = false; await this.loadSigningWorkspace(); this.page?.showToast?.(this.$t('rentalFiles.pmaGenerated'));
      } catch (error) { this.actionError = error.message || this.$t('rentalFiles.actionFailed'); }
      finally { this.actionBusy = false; }
    },
    async generateRentalAppointment() {
      if (!this.currentMandate?.id || this.selectedCycleIsHistorical || this.actionBusy) return;
      this.actionBusy = true; this.actionError = '';
      try {
        const mandate = this.currentMandate; const fields = this.templateFields();
        const result = await generateAdminContractTemplate('authorization', { ...fields, commission: mandate.commissionPercent ? `${mandate.commissionPercent}%` : '', commencementDate: mandate.endDate || '' });
        const file = new File([result.blob], result.filename, { type: 'application/pdf' });
        await uploadAdminRentalMandateDocument(mandate.id, 'rental_appointment_draft', file);
        await this.loadSigningWorkspace(); this.page?.showToast?.(this.$t('rentalFiles.rentalAppointmentGenerated'));
      } catch (error) { this.actionError = error.message || this.$t('rentalFiles.actionFailed'); }
      finally { this.actionBusy = false; }
    },
    async generateAuthorization() {
      if (!this.currentMandate?.id || this.selectedCycleIsHistorical || this.actionBusy) return;
      this.actionBusy = true; this.actionError = '';
      try {
        const mandate = this.currentMandate; const result = await generateAdminContractTemplate('management-authorization', this.templateFields());
        const file = new File([result.blob], result.filename, { type: 'application/pdf' }); await uploadAdminRentalMandateDocument(mandate.id, 'management_authorization_draft', file); await this.loadSigningWorkspace(); this.page?.showToast?.(this.$t('rentalFiles.authorizationGenerated'));
      } catch (error) { this.actionError = error.message || this.$t('rentalFiles.actionFailed'); }
      finally { this.actionBusy = false; }
    },
    async generateOtr() {
      const lease = this.currentLease; const mandate = this.currentMandate; if (!mandate?.id || this.selectedCycleIsHistorical || this.actionBusy) return;
      this.actionBusy = true; this.actionError = '';
      try {
        const reference = mandate.mandateNo || String(mandate.id); const validFrom = lease?.startDate || mandate.startDate || null; const validTo = lease?.endDate || mandate.endDate || null;
        const result = await generateAdminContractTemplate('otr', { caseNo: lease?.leaseNo || reference, projectName: this.selectedProperty.projectName || '', unitNo: this.selectedProperty.unitNo || '', propertyAddress: this.propertyTitle, landlordName: this.selectedProperty.ownerName || '', tenantName: lease?.tenantName || '', monthlyRent: lease?.monthlyRent || '', startDate: validFrom || '', commencementDate: validFrom || '' });
        const file = new File([result.blob], result.filename, { type: 'application/pdf' }); await uploadAdminRentalMandateDocument(mandate.id, 'otr', file); await this.loadSigningWorkspace(); this.page?.showToast?.(this.$t('rentalFiles.otrGenerated'));
      } catch (error) { this.actionError = error.message || this.$t('rentalFiles.actionFailed'); }
      finally { this.actionBusy = false; }
    },
    async generateLeaseDraft() {
      const lease = this.currentLease; const leaseId = lease?.id || lease?.leaseId;
      if (!leaseId || this.selectedCycleIsHistorical || this.actionBusy || ['pending', 'sent', 'viewed', 'signed'].includes(this.leaseSignatureStatus)) return;
      this.actionBusy = true; this.actionError = '';
      try {
        const result = await generateAdminContractTemplate('tenancy-agreement', { caseNo: lease.leaseNo || `LEASE-${leaseId}`, projectName: this.selectedProperty.projectName || '', unitNo: this.selectedProperty.unitNo || '', propertyAddress: this.propertyTitle, landlordName: this.selectedProperty.ownerName || '', tenantName: lease.tenantName || '', tenantEmail: lease.tenantEmail || '', monthlyRent: lease.monthlyRent || '', leaseStart: lease.startDate || '', leaseEnd: lease.endDate || '' });
        const file = new File([result.blob], result.filename, { type: 'application/pdf' });
        await uploadAdminLeaseContract(leaseId, file); await this.loadSigningWorkspace(); this.page?.showToast?.(this.$t('rentalFiles.leaseRegenerated'));
      } catch (error) { this.actionError = error.message || this.$t('rentalFiles.actionFailed'); }
      finally { this.actionBusy = false; }
    },
    async submitSigning() {
      if (this.selectedCycleIsHistorical || this.actionBusy || !this.signingPanel) return;
      this.actionBusy = true; this.actionError = '';
      try {
        if (this.signingPanel === 'pma') {
          if (!this.currentMandate?.id || !this.pmaDocument?.id) throw new Error(this.$t('rentalFiles.notGenerated'));
          await startAdminMandateDocumentSignature(this.currentMandate.id, this.pmaDocument.id, { signerName: this.signerForm.name, signerEmail: this.signerForm.email, expiresInDays: 7, signerRole: this.nextPmaSignerRole });
        } else if (this.signingPanel === 'rentalAppointment') {
          if (!this.currentMandate?.id || !this.rentalAppointmentDocument?.id) throw new Error(this.$t('rentalFiles.notGenerated'));
          await startAdminMandateDocumentSignature(this.currentMandate.id, this.rentalAppointmentDocument.id, { signerName: this.signerForm.name, signerEmail: this.signerForm.email, expiresInDays: 7, signerRole: 'owner' });
        } else if (this.signingPanel === 'authorization') {
          if (!this.currentMandate?.id || !this.authorizationDocument?.id) throw new Error(this.$t('rentalFiles.notGenerated'));
          await startAdminMandateDocumentSignature(this.currentMandate.id, this.authorizationDocument.id, { signerName: this.signerForm.name, signerEmail: this.signerForm.email, expiresInDays: 7, signerRole: 'owner' });
        } else if (this.signingPanel === 'lease') {
          const lease = this.currentLease; const leaseId = lease?.id || lease?.leaseId; if (!leaseId) throw new Error(this.$t('rentalFiles.createLeaseFirst'));
          if (!this.leaseContract) {
            const result = await generateAdminContractTemplate('tenancy-agreement', { caseNo: lease.leaseNo || `LEASE-${leaseId}`, projectName: this.selectedProperty.projectName || '', unitNo: this.selectedProperty.unitNo || '', propertyAddress: this.propertyTitle, landlordName: this.selectedProperty.ownerName || '', tenantName: lease.tenantName || '', tenantEmail: this.signerForm.email, monthlyRent: lease.monthlyRent || '', leaseStart: lease.startDate || '', leaseEnd: lease.endDate || '' });
            const file = new File([result.blob], result.filename, { type: 'application/pdf' }); await uploadAdminLeaseContract(leaseId, file);
          }
          await startAdminLeaseSignature(leaseId, { signerName: this.signerForm.name, signerEmail: this.signerForm.email, expiresInDays: 7 });
        }
        this.closeSigningPanel(); await this.loadSigningWorkspace(); this.page?.showToast?.(this.$t('rentalFiles.signingStarted'));
      } catch (error) { this.actionError = error.message || this.$t('rentalFiles.actionFailed'); }
      finally { this.actionBusy = false; }
    },
    async loadTemplateVersions() {
      const types = ['property-management-agreement', 'management-authorization'];
      const results = await Promise.all(types.map(type => fetchAdminContractTemplateVersion(type).catch(() => null)));
      this.templateVersions = Object.fromEntries(types.map((type, index) => [type, results[index]]));
    },
    chooseTemplate(type) { const input = this.$refs[`template-${type}`]; (Array.isArray(input) ? input[0] : input)?.click(); },
    async replaceTemplate(type, event) {
      const file = event.target.files?.[0]; event.target.value = ''; if (!file || this.actionBusy) return;
      this.actionBusy = true; this.actionError = '';
      try { const version = await replaceAdminContractTemplate(type, file); this.templateVersions = { ...this.templateVersions, [type]: version }; this.page?.showToast?.(this.$t('rentalFiles.templateReplaced')); }
      catch (error) { this.actionError = error.message || this.$t('rentalFiles.actionFailed'); }
      finally { this.actionBusy = false; }
    },
    signerRoleLabel(role) { const keys = { owner: 'signerRoleOwner', company: 'signerRoleCompany', customer_service: 'signerRoleCustomerService', tenant: 'signerRoleTenant' }; return this.$t(`rentalFiles.${keys[role] || keys.owner}`); },
    closeSigningPanel() { this.signingPanel = ''; this.actionError = ''; },
    categoryLabel(category) { return this.$t(`rentalFiles.categories.${category}`); },
    fileStatusLabel(file) { const status = String(file.status || '').toLowerCase(); if (['signed', 'completed', 'approved', 'active'].includes(status)) return this.$t('rentalFiles.statusComplete'); if (['authorization_draft', 'draft', 'ready'].includes(status)) return this.$t('rentalFiles.statusReady'); if (['pending', 'pending_review', 'sent', 'viewed', 'in_progress'].includes(status)) return this.$t('rentalFiles.statusPending'); if (status === 'disabled') return this.$t('rentalFiles.statusDisabled'); return this.$t('rentalFiles.statusStored'); },
    fileStatusClass(file) { const status = String(file.status || '').toLowerCase(); if (['signed', 'completed', 'approved', 'active'].includes(status)) return 'complete'; if (['authorization_draft', 'draft', 'ready'].includes(status)) return 'ready'; if (status === 'disabled') return 'disabled'; return 'pending'; },
    formatDate(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '—'; },
    async downloadFile(file) {
      this.downloadingKey = file.key; this.actionError = '';
      try { let result; if (file.kind === 'mandate') result = await downloadAdminRentalMandateDocument(file.mandateId, file.id); else if (file.kind === 'contract') result = await downloadAdminPropertyContractRecord(file.ownerId, file.ownerUnitId, file.id); else if (file.kind === 'handover') result = await downloadAdminPropertyHandoverReport(file.ownerId, file.ownerUnitId, file.id); else if (file.kind === 'property') result = await downloadAdminPropertyAttachment(file.ownerId, file.ownerUnitId, file.id); if (!result?.blob) return; const url = URL.createObjectURL(result.blob); const link = document.createElement('a'); link.href = url; link.download = file.name; document.body.appendChild(link); link.click(); link.remove(); setTimeout(() => URL.revokeObjectURL(url), 1000); }
      catch (error) { this.actionError = error.message || this.$t('rentalFiles.downloadFailed'); }
      finally { this.downloadingKey = ''; }
    },
  },
};
</script>

<style scoped>
.rental-signing-page{display:grid;min-width:0;overflow-x:hidden;gap:18px;padding:16px 20px 28px}.rental-signing-header{display:flex;align-items:flex-start;justify-content:space-between;gap:20px;padding:4px 2px}.rental-signing-header>div:first-child{display:grid;gap:6px}.rental-signing-header span,.rental-signing-property-head span,.rental-signing-panel header span{color:#0b8f96;font-size:10px;font-weight:800;letter-spacing:1.4px}.rental-signing-header h2{margin:0;color:#12375f;font-size:24px}.rental-signing-header p{margin:0;color:#6c7f8e;font-size:13px}.rental-signing-total{display:grid;justify-items:end;gap:2px;padding:10px 16px;border:1px solid #d6e5e8;border-radius:10px;background:#f3fafb;color:#0a737b}.rental-signing-total strong{font-size:20px}.rental-signing-total small{font-size:11px}.rental-signing-layout{display:grid;grid-template-columns:minmax(240px,290px) minmax(0,1fr);width:100%;min-width:0;gap:16px;height:calc(100vh - 190px);min-height:0}.rental-signing-property-picker,.rental-signing-main{min-width:0;border:1px solid #dce7ed;border-radius:12px;background:#fff}.rental-signing-property-picker{display:grid;grid-template-rows:auto auto minmax(0,1fr);align-self:start;max-height:100%;gap:11px;padding:16px 0;overflow:hidden}.rental-signing-picker-head{display:flex;align-items:center;justify-content:space-between;margin:0 16px;color:#173f5c}.rental-signing-picker-head span{padding:3px 8px;border-radius:999px;background:#edf6f7;color:#08717a;font-size:11px}.rental-signing-property-picker>input{box-sizing:border-box;width:calc(100% - 32px);height:38px;margin:0 16px;border:1px solid #cad8e3;border-radius:7px;padding:0 10px}.rental-signing-property-list{min-width:0;min-height:0;overflow-y:auto;padding:0 16px}.rental-signing-property-list button{display:grid;grid-template-columns:30px minmax(0,1fr) 22px;align-items:center;gap:8px;width:100%;border:1px solid transparent;border-radius:9px;background:#fff;padding:9px;text-align:left;color:#35576d;cursor:pointer;transition:background .18s,border-color .18s}.rental-signing-property-list button:hover{background:#f4f9fa}.rental-signing-property-list button.active{border-color:#72c9ce;background:#eaf9f9;color:#075e68;box-shadow:inset 3px 0 #0b979f}.rental-signing-property-list button>span:nth-child(2){display:grid;gap:3px;min-width:0}.rental-signing-property-list strong,.rental-signing-property-list small{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.rental-signing-property-list strong{font-size:12px}.rental-signing-property-list small{color:#7b8d99;font-size:10px}.rental-signing-property-list i{display:grid;place-items:center;width:20px;height:20px;border-radius:999px;font-style:normal}.rental-signing-property-list button.active i{background:#d5f1f2;color:#06747c}.rental-signing-property-icon{display:grid;place-items:center;width:30px;height:30px;border-radius:8px;background:#eef5f6;color:#19737a}.rental-signing-main{height:100%;box-sizing:border-box;padding:20px;overflow-x:hidden;overflow-y:auto}.rental-signing-property-head{display:flex;align-items:flex-start;justify-content:space-between;gap:16px;padding:4px 0 18px;border-bottom:1px solid #e8eef2}.rental-signing-property-head>div{display:grid;min-width:0;gap:4px}.rental-signing-property-head h3{overflow:hidden;margin:0;color:#12375f;font-size:20px;text-overflow:ellipsis;white-space:nowrap}.rental-signing-property-head p{overflow:hidden;margin:0;color:#718595;font-size:12px;text-overflow:ellipsis;white-space:nowrap}.rental-signing-property-head>strong{flex:0 0 auto;padding:5px 9px;border-radius:999px;background:#edf6f7;color:#08717a;font-size:10px}.rental-signing-actions,.rental-generated-files{min-width:0}.rental-signing-section-head{display:flex;align-items:flex-start;justify-content:space-between;gap:12px;margin:18px 0 12px}.rental-signing-section-head h4{margin:0;color:#173f5c;font-size:15px}.rental-signing-section-head p{margin:3px 0 0;color:#7a8d9b;font-size:11px}.rental-signing-section-head>span{flex:0 0 auto;margin:0;padding:4px 9px;border-radius:999px;background:#f1f6f7;color:#687f8d;font-size:10px}.rental-signing-card-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px}.rental-signing-card{display:grid;align-content:start;gap:13px;min-width:0;padding:15px;border:1px solid #dce7ed;border-radius:11px;background:#fbfdfe}.rental-signing-card.highlighted{box-shadow:0 0 0 3px rgba(11,143,150,.12)}.rental-signing-card.is-complete{border-color:#b9dfc7;background:#f5fcf7}.rental-signing-card.is-pending{border-color:#e5ce88;background:#fffbf1}.rental-signing-card.is-unavailable{opacity:.68}.rental-signing-card-head{display:grid;grid-template-columns:34px minmax(0,1fr) auto;align-items:start;gap:9px}.rental-signing-card-head>span{display:grid;place-items:center;width:32px;height:32px;border-radius:9px;background:#eaf9f9;color:#0b8f96;font-weight:800}.rental-signing-card-head>div{display:grid;gap:4px;min-width:0}.rental-signing-card-head strong{color:#173f5c;font-size:13px}.rental-signing-card-head small{color:#718595;font-size:10px;line-height:1.4}.rental-signing-card-head em{padding:4px 7px;border-radius:999px;background:#edf2f4;color:#718391;font-size:9px;font-style:normal;white-space:nowrap}.is-complete .rental-signing-card-head em{background:#e4f8ed;color:#168047}.is-pending .rental-signing-card-head em{background:#fff0c7;color:#9b6900}.rental-signing-card dl{display:grid;gap:7px;margin:0}.rental-signing-card dl>div{display:grid;gap:3px}.rental-signing-card dt{color:#8a9aa4;font-size:9px}.rental-signing-card dd{overflow:hidden;margin:0;color:#547083;font-size:10px;text-overflow:ellipsis;white-space:nowrap}.rental-signing-card>button{justify-self:start;height:32px;border:1px solid #0b8f96;border-radius:7px;background:#0b8f96;padding:0 11px;color:#fff;font-size:10px}.rental-signing-card>button:disabled{opacity:.55}.rental-signing-panel{display:grid;gap:14px;margin-top:16px;padding:16px 18px;border:1px solid #a8d8db;border-radius:11px;background:#f5fbfb}.rental-signing-panel header{display:flex;align-items:flex-start;justify-content:space-between;gap:12px}.rental-signing-panel header>div{display:grid;gap:4px}.rental-signing-panel h4{margin:0;color:#173f5c}.rental-signing-panel header p{margin:0;color:#718595;font-size:11px}.rental-signing-panel header>button{width:28px;height:28px;border:1px solid #c7dfe2;border-radius:7px;background:#fff;color:#547083;font-size:18px}.rental-signing-form{display:grid;grid-template-columns:1fr 1fr;gap:12px}.rental-signing-form label{display:grid;gap:6px;color:#31556b;font-size:11px;font-weight:700}.rental-signing-form input{box-sizing:border-box;width:100%;min-width:0;height:36px;border:1px solid #c9dce2;border-radius:7px;padding:0 9px}.rental-signing-form p{grid-column:1/-1;margin:0;color:#a13f2d;font-size:11px}.rental-signing-form>div{display:flex;grid-column:1/-1;justify-content:flex-end;gap:8px}.rental-signing-form button{min-height:34px;border-radius:7px;padding:0 13px}.rental-signing-form .secondary{border:1px solid #c7dfe2;background:#fff;color:#31556b}.rental-signing-form .primary{border:1px solid #0b8f96;background:#0b8f96;color:#fff}.rental-signing-status-detail{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:10px}.rental-signing-status-detail>div{display:grid;gap:5px;padding:11px;border:1px solid #dce7ed;border-radius:8px;background:#fff}.rental-signing-status-detail span{color:#718595;font-size:9px}.rental-signing-status-detail strong{overflow-wrap:anywhere;color:#29475f;font-size:11px}.rental-files-toolbar{display:grid;grid-template-columns:minmax(0,1fr) minmax(150px,190px);width:100%;min-width:0;gap:10px;margin-bottom:12px}.rental-files-toolbar input,.rental-files-toolbar select{box-sizing:border-box;width:100%;min-width:0;height:38px;border:1px solid #cad8e3;border-radius:7px;background:#fff;padding:0 10px}.rental-files-table-wrap{max-width:100%;overflow:auto;border:1px solid #dbe7ec;border-radius:10px}.rental-files-table{width:100%;min-width:900px;border-collapse:collapse}.rental-files-table th{padding:10px 11px;background:#f1f7f8;color:#496377;font-size:11px;text-align:left}.rental-files-table td{padding:12px 11px;border-top:1px solid #e4ecef;color:#29475f;font-size:12px}.rental-files-table td:first-child{display:grid;gap:4px;max-width:280px}.rental-files-table td:first-child strong,.rental-files-table td:first-child small{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.rental-files-table td:first-child small{color:#84949f;font-size:10px}.rental-file-category,.rental-file-status{display:inline-flex;padding:4px 8px;border-radius:999px;background:#edf3f6;color:#547083;font-size:10px;white-space:nowrap}.rental-file-status.complete{background:#e4f8ed;color:#168047}.rental-file-status.pending{background:#fff3d5;color:#9b6900}.rental-file-status.disabled{background:#eef1f2;color:#76858e}.rental-file-download{height:30px;border:1px solid #9fd5d8;border-radius:7px;background:#fff;padding:0 11px;color:#08717a}.rental-signing-state,.rental-signing-empty{display:grid;place-items:center;align-content:center;gap:8px;min-height:180px;color:#718595;font-size:12px;text-align:center}.rental-signing-state{min-height:120px;padding:14px}.rental-signing-state.error,.rental-signing-empty.error,.rental-signing-inline-error{color:#c43a3a}.rental-signing-state button,.rental-signing-empty button{border:1px solid #cbdde2;border-radius:7px;background:#fff;padding:7px 11px;color:#176f78}.rental-signing-empty.compact{min-height:150px;border:1px dashed #c9dfe3;border-radius:10px;background:#fbfdfe}.rental-signing-inline-error{margin:10px 0 0;padding:9px 10px;border-radius:7px;background:#fff1f2;font-size:11px}@media(max-width:1200px){.rental-signing-layout{grid-template-columns:250px minmax(0,1fr)}.rental-signing-card-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.rental-signing-card:last-child{grid-column:1/-1}}@media(max-width:900px){.rental-signing-layout{grid-template-columns:1fr;height:auto}.rental-signing-property-list{max-height:250px}.rental-signing-main{min-height:500px}.rental-signing-card:last-child{grid-column:auto}}@media(max-width:620px){.rental-signing-page{padding:12px}.rental-signing-header,.rental-signing-property-head{flex-direction:column}.rental-signing-total{justify-items:start}.rental-signing-card-grid,.rental-signing-form,.rental-files-toolbar,.rental-signing-status-detail{grid-template-columns:1fr}}
.rental-files-table{table-layout:fixed;border-collapse:separate;border-spacing:0}.rental-files-table th{padding:11px 14px;font-weight:700;white-space:nowrap}.rental-files-table th:first-child{width:24%}.rental-files-table th:nth-child(2){width:15%}.rental-files-table th:nth-child(3){width:22%}.rental-files-table th:nth-child(4){width:14%}.rental-files-table th:nth-child(5){width:16%}.rental-files-table th:last-child{width:90px}.rental-files-table td{height:58px;padding:10px 14px;background:#fff;vertical-align:middle}.rental-files-table td:first-child{display:table-cell;max-width:none}.rental-files-table tbody tr:hover td{background:#f8fcfc}.rental-file-name{display:grid;min-width:0;gap:4px}.rental-file-name strong,.rental-file-name small,.rental-file-mandate{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.rental-file-name strong{color:#173f5c;font-size:12px}.rental-file-name small{color:#84949f;font-size:10px}.rental-file-mandate{color:#355b73}.rental-files-table time{color:#607a8d;white-space:nowrap}.rental-file-category,.rental-file-status{padding:5px 9px;font-weight:600}.rental-file-status.ready{background:#e7f6fb;color:#147b98}.rental-file-download{border-color:#84cbd0;padding:0 12px;font-weight:700;cursor:pointer}.rental-file-download:hover:not(:disabled){background:#eaf9f9}.rental-file-download:disabled{opacity:.55;cursor:default}
.rental-signing-card-actions{display:flex;align-items:center;flex-wrap:wrap;gap:7px}.rental-signing-card-actions>button{height:32px;border:1px solid #0b8f96;border-radius:7px;background:#0b8f96;padding:0 11px;color:#fff;font-size:10px;cursor:pointer}.rental-signing-card-actions>button.secondary{border-color:#b8d7da;background:#fff;color:#08717a}.rental-signing-card-actions>small{width:100%;color:#80919b;font-size:9px}.rental-template-input{display:none}.rental-signer-role{display:grid;grid-column:1/-1;grid-template-columns:auto 1fr;align-items:center;gap:10px;padding:10px 12px;border:1px solid #d8e7ea;border-radius:8px;background:#fff}.rental-signer-role span{color:#758995;font-size:10px}.rental-signer-role strong{color:#0a737b;font-size:12px}
.pma-form-backdrop{position:fixed;z-index:1200;inset:0;display:grid;place-items:center;padding:24px;background:rgba(13,36,47,.58)}.pma-form-dialog{display:grid;grid-template-rows:auto minmax(0,1fr) auto auto;width:min(900px,calc(100vw - 40px));max-height:calc(100vh - 48px);overflow:hidden;border-radius:14px;background:#fff;box-shadow:0 24px 70px rgba(8,34,47,.28)}.pma-form-dialog>header{display:flex;align-items:flex-start;justify-content:space-between;gap:20px;padding:20px 24px 16px;border-bottom:1px solid #e2ebee}.pma-form-dialog>header>div{display:grid;gap:4px}.pma-form-dialog>header span{color:#0b8f96;font-size:10px;font-weight:800;letter-spacing:1.2px}.pma-form-dialog>header h3{margin:0;color:#173f5c;font-size:20px}.pma-form-dialog>header p{margin:0;color:#718595;font-size:11px}.pma-form-dialog>header button{width:30px;height:30px;border:1px solid #c9dce2;border-radius:8px;background:#fff;color:#47677b;font-size:20px}.pma-form-body{display:grid;gap:18px;padding:18px 24px;overflow-y:auto}.pma-form-body section{display:grid;gap:11px}.pma-form-body h4{margin:0;padding-bottom:8px;border-bottom:1px solid #edf1f3;color:#1b5069;font-size:13px}.pma-form-grid{display:grid;grid-template-columns:1fr 1fr;gap:12px}.pma-form-grid.dates{grid-template-columns:repeat(3,1fr)}.pma-form-grid label{display:grid;gap:6px;color:#31556b;font-size:11px;font-weight:700}.pma-form-grid label.wide{grid-column:1/-1}.pma-form-grid input,.pma-form-grid textarea{box-sizing:border-box;width:100%;min-width:0;border:1px solid #c8dbe1;border-radius:7px;background:#fff;padding:9px 10px;color:#173f5c;font:inherit}.pma-form-grid input{height:38px}.pma-form-grid textarea{resize:vertical;line-height:1.45}.pma-form-error{margin:0 24px;padding:9px 11px;border-radius:7px;background:#fff1f2;color:#bd3333;font-size:11px}.pma-form-dialog>footer{display:flex;justify-content:flex-end;gap:9px;padding:14px 24px 18px;border-top:1px solid #e5edef}.pma-form-dialog>footer button{min-height:38px;border-radius:8px;padding:0 16px}.pma-form-dialog>footer .secondary{border:1px solid #c7dce1;background:#fff;color:#31556b}.pma-form-dialog>footer .primary{border:1px solid #0b8f96;background:#0b8f96;color:#fff}@media(max-width:700px){.pma-form-backdrop{padding:10px}.pma-form-dialog{width:calc(100vw - 20px);max-height:calc(100vh - 20px)}.pma-form-grid,.pma-form-grid.dates{grid-template-columns:1fr}.pma-form-grid label.wide{grid-column:auto}}
.rental-signing-property-head>strong.historical{background:#f1f3f5;color:#687986}.rental-cycle-selector{display:grid;grid-template-columns:minmax(0,1fr) minmax(270px,390px);align-items:center;gap:18px;margin-top:14px;padding:13px 15px;border:1px solid #dce8eb;border-radius:10px;background:#f8fbfc}.rental-cycle-selector>div{display:grid;gap:3px;min-width:0}.rental-cycle-selector span{color:#0b8f96;font-size:9px;font-weight:800;letter-spacing:1px}.rental-cycle-selector strong{color:#173f5c;font-size:13px}.rental-cycle-selector small{color:#758995;font-size:10px}.rental-cycle-selector select{box-sizing:border-box;width:100%;min-width:0;height:38px;border:1px solid #bfd5dc;border-radius:8px;background:#fff;padding:0 10px;color:#294c63}.rental-cycle-readonly{margin-top:10px;padding:9px 12px;border:1px solid #e2e7ea;border-radius:8px;background:#f6f7f8;color:#667985;font-size:11px}.rental-property-files{min-width:0;margin-top:18px;padding-top:1px;border-top:1px solid #edf1f3}.rental-property-file-list{display:grid;border:1px solid #dbe7ec;border-radius:10px;overflow:hidden}.rental-property-file-list article{display:grid;grid-template-columns:minmax(0,1fr) 145px auto;align-items:center;gap:14px;padding:11px 13px;background:#fbfdfe}.rental-property-file-list article+article{border-top:1px solid #e4ecef}.rental-property-file-list article>div{display:grid;min-width:0;gap:3px}.rental-property-file-list strong,.rental-property-file-list small{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.rental-property-file-list strong{color:#29475f;font-size:11px}.rental-property-file-list small,.rental-property-file-list article>span{color:#7d8f9b;font-size:10px}@media(max-width:900px){.rental-cycle-selector{grid-template-columns:1fr}.rental-property-file-list article{grid-template-columns:minmax(0,1fr) auto}.rental-property-file-list article>span{display:none}}@media(max-width:620px){.rental-cycle-selector{padding:12px}.rental-property-file-list article{grid-template-columns:1fr}.rental-property-file-list .rental-file-download{justify-self:start}}
</style>
