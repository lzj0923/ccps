<template>
  <section class="content-grid admin-building-workspace">
    <div class="panel table-panel">
      <div class="panel-head">
        <div><h2>{{ $t('building.listTitle') }}</h2><span>{{ $t('ui.records', { count: filteredRows.length }) }}</span></div>
        <span v-if="loading">{{ $t('ui.loadingFromDatabase') }}</span>
      </div>

      <div v-if="errorMessage" class="admin-owner-state error" role="alert">
        <strong>{{ $t('building.paymentDataLoadFailed') }}</strong><span>{{ errorMessage }}</span><button type="button" @click="loadData">{{ $t('ui.reload') }}</button>
      </div>
      <div v-else class="table-wrap">
        <table>
          <thead><tr><th>{{ $t('building.installmentMilestone') }}</th><th>{{ $t('building.dueDate') }}</th><th>{{ $t('building.amountDue') }}</th><th>{{ $t('building.amountPaid') }}</th><th>{{ $t('building.amountUnpaid') }}</th><th>{{ $t('building.paymentDate') }}</th><th>{{ $t('ui.status') }}</th><th>{{ $t('building.proof') }}</th><th>{{ $t('ui.actions') }}</th></tr></thead>
          <tbody>
            <tr v-for="row in filteredRows" :key="row.id" :class="{ selected: row.id === selectedId }" @click="selectRow(row)">
              <td><strong>{{ row.installmentNo }}. {{ row.milestone || row.planName || $t('building.paymentInstallment') }}</strong><small>{{ row.projectName }} · {{ row.unitNo }}</small></td>
              <td>{{ row.dueDate || '—' }}</td>
              <td>{{ money(row.amountDue) }}</td>
              <td :class="{ 'money-green': Number(row.amountPaid) > 0 }">{{ money(row.amountPaid) }}</td>
              <td :class="{ 'money-red': Number(row.unpaidAmount) > 0, 'money-green': Number(row.unpaidAmount) === 0 }">{{ money(row.unpaidAmount) }}</td>
              <td>{{ row.paymentDate || '—' }}</td>
              <td><span class="tag" :class="statusClass(row.status)">{{ statusLabel(row.status) }}</span></td>
              <td>{{ row.receiptNo || '—' }}</td>
              <td><button type="button" class="row-actions" :title="$t('building.viewDetails')" @click.stop="selectRow(row)">…</button></td>
            </tr>
            <tr v-if="!loading && !filteredRows.length"><td colspan="9" class="admin-owner-empty">{{ $t('building.noMatchingPayments') }}</td></tr>
          </tbody>
        </table>
      </div>
      <div class="pager"><span>{{ $t('ui.records', { count: totalRows }) }}</span><div class="admin-building-pager"><button type="button" :disabled="pageNumber <= 1 || loading" @click="goToPage(pageNumber - 1)">&lt;</button><button v-for="number in visiblePages" :key="number" type="button" :class="{ active: number === pageNumber }" :disabled="loading" @click="goToPage(number)">{{ number }}</button><button type="button" :disabled="pageNumber >= totalPages || loading" @click="goToPage(pageNumber + 1)">&gt;</button><select v-model.number="pageSize" :disabled="loading" :aria-label="$t('building.recordsPerPage', { count: pageSize })"><option :value="10">{{ $t('building.recordsPerPage', { count: 10 }) }}</option><option :value="20">{{ $t('building.recordsPerPage', { count: 20 }) }}</option><option :value="50">{{ $t('building.recordsPerPage', { count: 50 }) }}</option></select></div></div>
    </div>

    <aside class="panel detail-panel">
      <div v-if="selectedRow" class="detail-card">
        <div class="profile"><div class="big-avatar">{{ selectedRow.installmentNo }}</div><div><h3>{{ selectedRow.milestone || selectedRow.planName || $t('building.paymentInstallment') }}</h3><p>{{ selectedRow.projectName }} · {{ selectedRow.unitNo }}</p></div><span class="tag" :class="statusClass(selectedRow.status)">{{ statusLabel(selectedRow.status) }}</span></div>
        <div class="detail-actions installment-actions">
          <button v-if="canEditSelected" @click="openInstallmentEdit">{{ $t('building.editInstallment') }}</button>
          <button v-if="canRemindSelected" :disabled="reminderSending" @click="sendReminder">{{ reminderSending ? $t('building.sending') : reminderLabel }}</button>
          <button :disabled="!selectedRow.receiptNo" @click="openPaymentRecord">{{ selectedRow.status === 'paid' ? $t('building.viewReceipt') : $t('building.viewPaymentRecord') }}</button>
          <button v-if="selectedRow.confirmationStatus === 'pending'" @click="goToFinanceReview">{{ $t('building.goToFinanceReview') }}</button>
        </div>
        <div class="detail-section"><h4><span class="num">{{ $t('legacy.t_356a192b7913') }}</span>{{ $t('building.information') }}</h4><div class="kv"><span>{{ $t('ui.owner') }}</span><b>{{ selectedRow.ownerName || '—' }}</b></div><div class="kv"><span>{{ $t('building.contractNo') }}</span><b>{{ selectedRow.contractNo || '—' }}</b></div><div class="kv"><span>{{ $t('building.paymentPlan') }}</span><b>{{ selectedRow.planName || '—' }}</b></div></div>
        <div class="detail-section"><h4><span class="num">{{ $t('legacy.t_da4b9237bacc') }}</span>{{ $t('building.paymentStatus') }}</h4><div class="kv"><span>{{ $t('building.amountDue') }}</span><b>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(selectedRow.amountDue) }}</b></div><div class="kv"><span>{{ $t('building.amountPaid') }}</span><b>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(selectedRow.amountPaid) }}</b></div><div class="kv"><span>{{ $t('building.amountUnpaid') }}</span><b class="money-red">{{ $t('legacy.t_5e7b60c626a4') }} {{ money(selectedRow.unpaidAmount) }}</b></div></div>
        <div class="detail-section"><h4><span class="num">{{ $t('legacy.t_77de68daecd8') }}</span>{{ $t('building.paymentProgress') }}</h4><div class="kv"><span>{{ $t('building.dueDate') }}</span><b>{{ selectedRow.dueDate || '—' }}</b></div><div class="kv"><span>{{ $t('building.paymentDate') }}</span><b>{{ selectedRow.paymentDate || '—' }}</b></div><div class="progress"><i :style="{ width: paymentProgress + '%' }"></i></div></div>
      </div>
      <div v-else class="admin-owner-empty">{{ $t('building.noPaymentData') }}</div>
    </aside>

    <dialog ref="projectDialog" class="modal admin-building-dialog">
      <form method="dialog" @submit.prevent="saveProject">
        <div class="modal-head"><div><h3>{{ $t('building.createProject') }}</h3><small>{{ $t('building.projectCreatedHint') }}</small></div><button class="icon-close" type="button" @click="closeProjectDialog">×</button></div>
        <div class="form-grid">
          <label>{{ $t('building.projectCode') }}<input v-model.trim="projectForm.projectCode" maxlength="40" required :placeholder="$t('building.projectCodeExample')"></label>
          <label>{{ $t('building.projectName') }}<input v-model.trim="projectForm.name" maxlength="160" required :placeholder="$t('building.projectName')"></label>
          <label class="wide">{{ $t('building.address') }}<input v-model.trim="projectForm.address" maxlength="255" :placeholder="$t('building.address')"></label>
          <label>{{ $t('building.city') }}<input v-model.trim="projectForm.city" maxlength="100" :placeholder="$t('building.cityExample')"></label>
          <label>{{ $t('building.countryCode') }}<input v-model.trim="projectForm.countryCode" maxlength="2" required></label>
          <label>{{ $t('ui.status') }}<select v-model="projectForm.status"><option value="active">{{ $t('ui.enabled') }}</option><option value="inactive">{{ $t('ui.disabled') }}</option></select></label>
          <p v-if="projectFormError" class="admin-property-error wide">{{ projectFormError }}</p>
        </div>
        <menu><button type="button" @click="closeProjectDialog">{{ $t('ui.cancel') }}</button><button type="submit" class="primary-btn" :disabled="projectSaving">{{ projectSaving ? $t('building.creating') : $t('building.confirmCreate') }}</button></menu>
      </form>
    </dialog>

    <dialog ref="paymentPlanDialog" class="modal admin-payment-plan-dialog">
      <form method="dialog" @submit.prevent="savePaymentPlan">
        <div class="modal-head"><div><h3>{{ $t('building.createPaymentPlan') }}</h3><small>{{ $t('building.paymentPlanHint') }}</small></div><button class="icon-close" type="button" @click="closePaymentPlanDialog">×</button></div>
        <div v-if="contractsLoading" class="admin-owner-state">{{ $t('building.loadingEligibleContracts') }}</div>
        <div v-else class="payment-plan-form">
          <div v-if="!eligibleContracts.length" class="admin-owner-state">
            {{ $t('building.noEligibleContracts') }}
          </div>
          <template v-else>
            <div class="form-grid">
              <label class="wide">{{ $t('building.purchaseContract') }}<select v-model.number="paymentPlanForm.purchaseContractId" required><option disabled value="">{{ $t('building.selectProjectUnitOwner') }}</option><option v-for="contract in eligibleContracts" :key="contract.contractId" :value="contract.contractId">{{ contract.projectName }} · {{ contract.unitNo }} · {{ contract.ownerName }} · {{ contract.contractNo }}</option></select></label>
              <label>{{ $t('building.planName') }}<input v-model.trim="paymentPlanForm.planName" maxlength="120" required :placeholder="$t('building.planName')"></label>
              <label>{{ $t('building.effectiveDate') }}<input v-model="paymentPlanForm.startDate" type="date"></label>
            </div>

            <div v-if="selectedContract" class="payment-contract-summary">
              <span><small>{{ $t('building.ownerUnit') }}</small><strong>{{ selectedContract.ownerName }} · {{ selectedContract.unitNo }}</strong></span>
              <span><small>{{ $t('building.purchaseContract') }}</small><strong>{{ selectedContract.contractNo }}</strong></span>
              <span><small>{{ $t('building.contractTotal') }}</small><strong>{{ selectedContract.currency }} {{ money(selectedContract.purchasePrice) }}</strong></span>
            </div>

            <section class="payment-installment-editor">
              <div class="payment-installment-head"><div><h4>{{ $t('building.paymentInstallments') }}</h4><small>{{ $t('building.installmentHint') }}</small></div><div><button type="button" @click="splitEvenly" :disabled="!selectedContract">{{ $t('building.splitEvenly') }}</button><button type="button" class="primary-btn" @click="addInstallment">＋ {{ $t('building.addInstallment') }}</button></div></div>
              <div class="payment-installment-row payment-installment-labels"><span>{{ $t('building.paymentInstallment') }}</span><span>{{ $t('building.milestoneDescription') }}</span><span>{{ $t('building.dueDate') }}</span><span>{{ $t('building.amountDue') }}</span><span></span></div>
              <div v-for="(installment, index) in paymentPlanForm.installments" :key="installment.key" class="payment-installment-row">
                <b>{{ index + 1 }}</b>
                <input v-model.trim="installment.milestone" maxlength="160" :placeholder="$t('building.installments', { count: index + 1 })">
                <input v-model="installment.dueDate" type="date" required>
                <input v-model="installment.amountDue" type="number" min="0.01" step="0.01" required>
                <button type="button" :title="$t('building.removeInstallment')" :disabled="paymentPlanForm.installments.length === 1" @click="removeInstallment(index)">×</button>
              </div>
            </section>

            <div class="payment-plan-totals">
              <span>{{ $t('building.contractTotal') }} <b>{{ money(selectedContract?.purchasePrice) }}</b></span>
              <span>{{ $t('building.installmentTotal') }} <b>{{ money(planTotal) }}</b></span>
              <span :class="{ mismatch: planDifferenceCents !== 0, matched: planDifferenceCents === 0 }">{{ $t('building.difference') }} <b>{{ money(Math.abs(planDifferenceCents) / 100) }}</b></span>
            </div>
          </template>
          <p v-if="paymentPlanError" class="admin-property-error">{{ paymentPlanError }}</p>
        </div>
        <menu><button type="button" @click="closePaymentPlanDialog">{{ $t('ui.cancel') }}</button><button type="submit" class="primary-btn" :disabled="paymentPlanSaving || contractsLoading || !eligibleContracts.length">{{ paymentPlanSaving ? $t('building.creating') : $t('building.confirmCreatePaymentPlan') }}</button></menu>
      </form>
    </dialog>

    <dialog ref="installmentEditDialog" class="modal admin-installment-dialog">
      <form method="dialog" @submit.prevent="saveInstallment">
        <div class="modal-head"><div><h3>{{ $t('building.editInstallmentPayment') }}</h3><small>{{ selectedRow?.projectName }} · {{ selectedRow?.unitNo }} · {{ $t('building.installments', { count: selectedRow?.installmentNo }) }}</small></div><button class="icon-close" type="button" @click="closeInstallmentEdit">×</button></div>
        <div class="form-grid">
          <label class="wide">{{ $t('building.milestoneDescription') }}<input v-model.trim="installmentEditForm.milestone" maxlength="160" :placeholder="$t('building.milestoneDescription')"></label>
          <label>{{ $t('building.dueDate') }}<input v-model="installmentEditForm.dueDate" type="date" required></label>
          <label>{{ $t('building.amountDue') }} {{ $t('building.currencyMyr') }}<input :value="money(selectedRow?.amountDue)" disabled><small>{{ $t('building.amountLockedHint') }}</small></label>
          <p v-if="installmentActionError" class="admin-property-error wide">{{ installmentActionError }}</p>
        </div>
        <menu><button type="button" @click="closeInstallmentEdit">{{ $t('ui.cancel') }}</button><button type="submit" class="primary-btn" :disabled="installmentSaving">{{ installmentSaving ? $t('ui.saving') : $t('building.saveInstallment') }}</button></menu>
      </form>
    </dialog>

    <dialog ref="paymentRecordDialog" class="modal admin-installment-dialog">
      <form method="dialog" @submit.prevent>
        <div class="modal-head"><div><h3>{{ $t('building.paymentRecord') }}</h3><small>{{ selectedRow?.projectName }} · {{ selectedRow?.unitNo }} · {{ $t('building.installments', { count: selectedRow?.installmentNo }) }}</small></div><button class="icon-close" type="button" @click="closePaymentRecord">×</button></div>
        <div class="installment-record-grid">
          <div><span>{{ $t('building.receiptNo') }}</span><b>{{ selectedRow?.receiptNo || '—' }}</b></div>
          <div><span>{{ $t('building.paymentDate') }}</span><b>{{ selectedRow?.paymentDate || '—' }}</b></div>
          <div><span>{{ $t('building.paymentMethod') }}</span><b>{{ paymentMethodLabel(selectedRow?.paymentMethod) }}</b></div>
          <div><span>{{ $t('building.financeConfirmation') }}</span><b><i class="tag" :class="confirmationClass(selectedRow?.confirmationStatus)">{{ confirmationLabel(selectedRow?.confirmationStatus) }}</i></b></div>
          <div class="wide"><span>{{ $t('building.bankReference') }}</span><b>{{ selectedRow?.bankReference || '—' }}</b></div>
          <div class="wide"><span>{{ $t('building.submissionNote') }}</span><b>{{ selectedRow?.submissionNote || '—' }}</b></div>
          <div><span>{{ $t('building.submittedAmount') }}</span><b>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(selectedRow?.submittedAmount) }}</b></div>
          <div><span>{{ $t('building.amountPaid') }}</span><b>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(selectedRow?.amountPaid) }}</b></div>
          <div><span>{{ $t('building.proofDocument') }}</span><b>{{ selectedRow?.proofDocumentId ? `${$t('building.proofDocument')} #${selectedRow.proofDocumentId}` : '—' }}</b></div>
        </div>
        <menu><button type="button" @click="closePaymentRecord">{{ $t('building.close') }}</button><button v-if="selectedRow?.confirmationStatus === 'pending'" type="button" class="primary-btn" @click="goToFinanceReview">{{ $t('building.goToFinanceReview') }}</button></menu>
      </form>
    </dialog>
  </section>
</template>

<script>
import pageBridge from '../pageBridge';
import { i18n } from '../i18n';
import { createAdminBuildingProject, createAdminPaymentPlan, fetchAdminBuildingPaymentProgress, fetchAdminPaymentContracts, sendAdminPaymentReminder, updateAdminPaymentInstallment } from '../services/propertyApi';

export default {
  mixins: [pageBridge],
  data() { return {
    rows: [], summary: null, selectedId: null, loading: false, errorMessage: '', pageNumber: 1, pageSize: 10, totalRows: 0, totalPages: 1, requestSerial: 0,
    projectSaving: false, projectFormError: '', projectForm: { projectCode: '', name: '', address: '', city: '', countryCode: 'MY', status: 'active' },
    paymentContracts: [], contractsLoading: false, paymentPlanSaving: false, paymentPlanError: '', installmentKey: 1,
    paymentPlanForm: { purchaseContractId: '', planName: '', startDate: '', installments: [] },
    installmentSaving: false, reminderSending: false, installmentActionError: '', installmentEditForm: { milestone: '', dueDate: '' }
  }; },
  computed: {
    filteredRows() {
      const keyword = String(this.page.globalSearch || this.page.moduleSearch || '').trim().toLowerCase();
      const project = String(this.page.projectFilter || '');
      const status = String(this.page.statusFilter || '');
      return this.rows.filter(row => {
        const text = `${row.projectName} ${row.unitNo} ${row.ownerName} ${row.milestone || ''} ${row.receiptNo || ''}`.toLowerCase();
        const matchesKeyword = !keyword || text.includes(keyword);
        const matchesProject = project.includes('全部') || row.projectName === project;
        const matchesStatus = status.includes('全部') || this.statusLabel(row.status) === status;
        return matchesKeyword && matchesProject && matchesStatus;
      });
    },
    selectedRow() { return this.rows.find(row => row.id === this.selectedId) || this.rows[0] || null; },
    visiblePages() {
      const start = Math.max(1, Math.min(this.pageNumber - 2, this.totalPages - 4));
      const end = Math.min(this.totalPages, start + 4);
      return Array.from({ length: Math.max(1, end - start + 1) }, (_, index) => start + index);
    },
    paymentProgress() {
      if (!this.selectedRow || !Number(this.selectedRow.amountDue)) return 0;
      return Math.min(100, Math.max(0, Math.round(Number(this.selectedRow.amountPaid || 0) / Number(this.selectedRow.amountDue) * 100)));
    },
    locale() { return i18n.global.locale.value; },
    projectCreateRequestNonce() { return this.page.adminBuildingProjectCreateNonce; },
    paymentPlanCreateRequestNonce() { return this.page.adminPaymentPlanCreateNonce; },
    eligibleContracts() { return this.paymentContracts.filter(contract => !contract.hasActivePlan); },
    selectedContract() { return this.paymentContracts.find(contract => contract.contractId === this.paymentPlanForm.purchaseContractId) || null; },
    planTotalCents() { return this.paymentPlanForm.installments.reduce((sum, item) => sum + this.toCents(item.amountDue), 0); },
    planTotal() { return this.planTotalCents / 100; },
    planDifferenceCents() { return this.planTotalCents - this.toCents(this.selectedContract?.purchasePrice); },
    canEditSelected() { return this.selectedRow && this.selectedRow.status !== 'paid'; },
    canRemindSelected() { return this.selectedRow && Number(this.selectedRow.unpaidAmount || 0) > 0; },
    reminderLabel() { return this.selectedRow?.status === 'overdue' ? this.$t('building.sendOverdueReminder') : this.selectedRow?.status === 'partial' ? this.$t('building.sendBalanceReminder') : this.$t('building.sendPaymentReminder'); }
  },
  watch: {
    rows: { immediate: true, deep: true, handler(rows) { if (!rows.some(row => row.id === this.selectedId)) this.selectedId = rows[0]?.id || null; } },
    pageSize() { this.pageNumber = 1; this.loadData(); },
    moduleSearch() { this.resetAndLoad(); },
    globalSearch() { this.resetAndLoad(); },
    projectFilter() { this.resetAndLoad(); },
    statusFilter() { this.resetAndLoad(); },
    locale() { if (this.summary) this.page.adminDataMetrics = this.toMetrics(this.summary); },
    projectCreateRequestNonce(value, previousValue) { if (value > previousValue) this.openProjectCreate(); },
    paymentPlanCreateRequestNonce(value, previousValue) { if (value > previousValue) this.openPaymentPlanCreate(); },
    selectedContract(contract) {
      if (contract && this.paymentPlanForm.installments.length === 1 && !this.paymentPlanForm.installments[0].amountDue) {
        this.paymentPlanForm.installments[0].amountDue = Number(contract.purchasePrice).toFixed(2);
      }
    }
  },
  mounted() { this.loadData(); },
  methods: {
    openInstallmentEdit() {
      if (!this.canEditSelected) return;
      this.installmentEditForm = { milestone: this.selectedRow.milestone || '', dueDate: this.selectedRow.dueDate || '' };
      this.installmentActionError = ''; this.$refs.installmentEditDialog?.showModal();
    },
    closeInstallmentEdit() { this.$refs.installmentEditDialog?.close(); },
    async saveInstallment() {
      if (!this.selectedRow || !this.installmentEditForm.dueDate) { this.installmentActionError = this.$t('building.dueDate'); return; }
      const selectedId = this.selectedRow.id; this.installmentSaving = true; this.installmentActionError = '';
      try {
        await updateAdminPaymentInstallment(selectedId, { milestone: this.installmentEditForm.milestone || null, dueDate: this.installmentEditForm.dueDate });
        await this.loadData(); if (this.rows.some(row => row.id === selectedId)) this.selectedId = selectedId;
        this.closeInstallmentEdit(); this.showToast(this.$t('building.paymentUpdated'));
      } catch (error) { this.installmentActionError = error.message || this.$t('building.paymentUpdateFailed'); }
      finally { this.installmentSaving = false; }
    },
    async sendReminder() {
      if (!this.canRemindSelected || this.reminderSending) return;
      this.reminderSending = true;
      try { await sendAdminPaymentReminder(this.selectedRow.id); this.showToast(this.$t('building.paymentReminderSent')); }
      catch (error) { this.showToast(error.message || this.$t('building.paymentReminderFailed')); }
      finally { this.reminderSending = false; }
    },
    openPaymentRecord() { if (this.selectedRow?.receiptNo) this.$refs.paymentRecordDialog?.showModal(); },
    closePaymentRecord() { this.$refs.paymentRecordDialog?.close(); },
    goToFinanceReview() {
      const receiptNo = this.selectedRow?.receiptNo || '';
      this.closePaymentRecord(); this.selectModule('adminFinance');
      this.$nextTick(() => { this.page.globalSearch = receiptNo; });
    },
    paymentMethodLabel(value) { return ({ bank_transfer: this.$t('building.bankTransfer'), cheque: this.$t('building.cheque'), cash: this.$t('building.cash'), online_banking: this.$t('building.onlineBanking') })[value] || value || '—'; },
    confirmationLabel(value) { return ({ pending: this.$t('building.pendingFinanceConfirmation'), confirmed: this.$t('building.confirmed'), rejected: this.$t('building.rejected') })[value] || this.$t('building.notSubmitted'); },
    confirmationClass(value) { return value === 'confirmed' ? 'green' : value === 'rejected' ? 'red' : 'orange'; },
    openProjectCreate() {
      this.projectForm = { projectCode: '', name: '', address: '', city: '', countryCode: 'MY', status: 'active' };
      this.projectFormError = ''; this.$refs.projectDialog?.showModal();
    },
    closeProjectDialog() { this.$refs.projectDialog?.close(); },
    async saveProject() {
      if (!this.projectForm.projectCode || !this.projectForm.name) { this.projectFormError = this.$t('building.projectRequired'); return; }
      this.projectSaving = true; this.projectFormError = '';
      try {
        const project = await createAdminBuildingProject({ ...this.projectForm, countryCode: this.projectForm.countryCode.toUpperCase() });
        this.page.adminBuildingProjects = [...new Set([...(this.page.adminBuildingProjects || []), project.name])].sort((a, b) => a.localeCompare(b));
        this.closeProjectDialog(); this.showToast(this.$t('building.projectCreated'));
      } catch (error) { this.projectFormError = error.message || this.$t('building.projectCreateFailed'); }
      finally { this.projectSaving = false; }
    },
    emptyInstallment(index = 0) {
      const date = new Date(); date.setMonth(date.getMonth() + index + 1);
      return { key: this.installmentKey++, milestone: '', dueDate: date.toISOString().slice(0, 10), amountDue: '' };
    },
    async openPaymentPlanCreate() {
      this.paymentPlanForm = { purchaseContractId: '', planName: this.$t('building.paymentPlan'), startDate: new Date().toISOString().slice(0, 10), installments: [this.emptyInstallment()] };
      this.paymentPlanError = ''; this.paymentContracts = []; this.$refs.paymentPlanDialog?.showModal(); this.contractsLoading = true;
      try {
        this.paymentContracts = await fetchAdminPaymentContracts();
        if (this.eligibleContracts.length === 1) this.paymentPlanForm.purchaseContractId = this.eligibleContracts[0].contractId;
      } catch (error) { this.paymentPlanError = error.message || this.$t('building.purchaseContractLoadFailed'); }
      finally { this.contractsLoading = false; }
    },
    closePaymentPlanDialog() { this.$refs.paymentPlanDialog?.close(); },
    addInstallment() { this.paymentPlanForm.installments.push(this.emptyInstallment(this.paymentPlanForm.installments.length)); },
    removeInstallment(index) { if (this.paymentPlanForm.installments.length > 1) this.paymentPlanForm.installments.splice(index, 1); },
    splitEvenly() {
      if (!this.selectedContract || !this.paymentPlanForm.installments.length) return;
      const totalCents = this.toCents(this.selectedContract.purchasePrice);
      const base = Math.floor(totalCents / this.paymentPlanForm.installments.length);
      let remainder = totalCents - base * this.paymentPlanForm.installments.length;
      this.paymentPlanForm.installments.forEach(item => { item.amountDue = ((base + (remainder-- > 0 ? 1 : 0)) / 100).toFixed(2); });
    },
    async savePaymentPlan() {
      if (!this.selectedContract) { this.paymentPlanError = this.$t('building.selectPurchaseContract'); return; }
      if (!this.paymentPlanForm.planName) { this.paymentPlanError = this.$t('building.paymentPlanNameRequired'); return; }
      if (this.paymentPlanForm.installments.some(item => !item.dueDate || this.toCents(item.amountDue) <= 0)) { this.paymentPlanError = this.$t('building.installmentValidation'); return; }
      for (let index = 1; index < this.paymentPlanForm.installments.length; index++) {
        if (this.paymentPlanForm.installments[index].dueDate <= this.paymentPlanForm.installments[index - 1].dueDate) { this.paymentPlanError = this.$t('building.dueDatesIncreasing'); return; }
      }
      if (this.planDifferenceCents !== 0) { this.paymentPlanError = this.$t('building.installmentTotalMismatch'); return; }
      this.paymentPlanSaving = true; this.paymentPlanError = '';
      try {
        await createAdminPaymentPlan({
          purchaseContractId: this.paymentPlanForm.purchaseContractId,
          planName: this.paymentPlanForm.planName,
          startDate: this.paymentPlanForm.startDate || null,
          installments: this.paymentPlanForm.installments.map(item => ({ milestone: item.milestone || null, dueDate: item.dueDate, amountDue: (this.toCents(item.amountDue) / 100).toFixed(2) }))
        });
        this.closePaymentPlanDialog(); this.pageNumber = 1; await this.loadData(); this.showToast(this.$t('building.paymentPlanCreated'));
      } catch (error) { this.paymentPlanError = error.message || this.$t('building.createPaymentPlanFailed'); }
      finally { this.paymentPlanSaving = false; }
    },
    toCents(value) { return Math.round(Number(value || 0) * 100); },
    resetAndLoad() { this.pageNumber = 1; this.loadData(); },
    async goToPage(page) {
      if (page < 1 || page > this.totalPages || page === this.pageNumber) return;
      this.pageNumber = page;
      await this.loadData();
    },
    async loadData() {
      const serial = ++this.requestSerial;
      this.loading = true; this.errorMessage = '';
      try {
        const response = await fetchAdminBuildingPaymentProgress({
          page: this.pageNumber,
          pageSize: this.pageSize,
          keyword: this.page.globalSearch || this.page.moduleSearch || '',
          projectName: String(this.page.projectFilter || '').includes('全部') ? '' : this.page.projectFilter,
          status: this.statusParam(this.page.statusFilter)
        });
        if (serial !== this.requestSerial) return;
        this.summary = response.summary || null;
        this.rows = response.rows || [];
        this.totalRows = response.page?.totalRows || 0;
        this.totalPages = response.page?.totalPages || 1;
        this.pageNumber = response.page?.page || this.pageNumber;
        this.page.adminBuildingProjects = [...new Set([...(this.page.adminBuildingProjects || []), ...this.rows.map(row => row.projectName).filter(Boolean)])].sort((a, b) => a.localeCompare(b));
        this.page.adminDataMetrics = this.toMetrics(this.summary);
        this.selectedId = this.rows[0]?.id || null;
      } catch (error) {
        if (serial !== this.requestSerial) return;
        this.rows = []; this.summary = null; this.page.adminBuildingProjects = []; this.page.adminDataMetrics = null; this.errorMessage = error.message || 'API request failed';
      } finally { this.loading = false; }
    },
    toMetrics(summary) {
      if (!summary) return [];
      const moneyValue = value => `RM ${this.money(value)}`;
      return [
        { icon: 'building', label: this.$t('building.contractTotal'), value: moneyValue(summary.propertyTotal), delta: this.$t('ui.liveDatabaseStatistics'), trend: '' },
        { icon: 'paid', label: this.$t('building.amountPaid'), value: moneyValue(summary.paidTotal), delta: this.$t('building.livePaymentPlan'), trend: 'up' },
        { icon: 'unpaid', label: this.$t('building.amountUnpaid'), value: moneyValue(summary.unpaidTotal), delta: this.$t('building.followUpPayments'), trend: summary.unpaidTotal > 0 ? 'down' : 'up' },
        { icon: 'total', label: this.$t('building.paymentInstallments'), value: this.$t('building.installments', { count: summary.totalInstallments }), delta: this.$t('building.currentPaymentPlan'), trend: '' },
        { icon: 'paidCount', label: this.$t('building.amountPaid'), value: this.$t('building.installments', { count: summary.paidInstallments }), delta: this.$t('building.paymentCompleted'), trend: 'up' },
        { icon: 'remaining', label: this.$t('building.amountUnpaid'), value: this.$t('building.installments', { count: summary.remainingInstallments }), delta: this.$t('building.pendingInstallments'), trend: summary.remainingInstallments ? 'down' : 'up' },
        { icon: 'next', label: this.$t('building.amountDue'), value: moneyValue(summary.nextAmount), delta: this.$t('building.nearestUnpaid'), trend: 'down' },
        { icon: 'date', label: this.$t('building.dueDate'), value: summary.nextDueDate || '—', delta: this.$t('ui.liveDatabaseStatistics'), trend: 'down' }
      ];
    },
    selectRow(row) { this.selectedId = row.id; },
    statusParam(status) { return ({ '已完成': 'paid', '部分付款': 'partial', '待付款': 'pending', '逾期': 'overdue' })[status] || ''; },
    statusLabel(status) { return ({ paid: this.$t('building.complete'), partial: this.$t('building.partialPayment'), pending: this.$t('building.pendingPayment'), overdue: this.$t('building.overdue') })[status] || this.$t('building.pendingPayment'); },
    statusClass(status) { return status === 'paid' ? 'green' : status === 'overdue' ? 'red' : 'orange'; },
    money(value) { return Number(value || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
  }
};
</script>
