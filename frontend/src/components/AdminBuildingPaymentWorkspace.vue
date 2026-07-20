<template>
  <section class="content-grid admin-building-workspace">
    <div class="panel table-panel">
      <div class="panel-head">
        <div><h2>建築與房款列表</h2><span>{{ filteredRows.length }} records</span></div>
        <span v-if="loading">正在從資料庫載入…</span>
      </div>

      <div v-if="errorMessage" class="admin-owner-state error" role="alert">
        <strong>建築與房款資料載入失敗</strong><span>{{ errorMessage }}</span><button type="button" @click="loadData">重新載入</button>
      </div>
      <div v-else class="table-wrap">
        <table>
          <thead><tr><th>期數／工程階段</th><th>到期日</th><th>應收金額</th><th>已收金額</th><th>未收金額</th><th>付款日期</th><th>狀態</th><th>憑證</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="row in filteredRows" :key="row.id" :class="{ selected: row.id === selectedId }" @click="selectRow(row)">
              <td><strong>{{ row.installmentNo }}. {{ row.milestone || row.planName || '付款期數' }}</strong><small>{{ row.projectName }} · {{ row.unitNo }}</small></td>
              <td>{{ row.dueDate || '—' }}</td>
              <td>{{ money(row.amountDue) }}</td>
              <td :class="{ 'money-green': Number(row.amountPaid) > 0 }">{{ money(row.amountPaid) }}</td>
              <td :class="{ 'money-red': Number(row.unpaidAmount) > 0, 'money-green': Number(row.unpaidAmount) === 0 }">{{ money(row.unpaidAmount) }}</td>
              <td>{{ row.paymentDate || '—' }}</td>
              <td><span class="tag" :class="statusClass(row.status)">{{ statusLabel(row.status) }}</span></td>
              <td>{{ row.receiptNo || '—' }}</td>
              <td><button type="button" class="row-actions" title="查看詳情" @click.stop="selectRow(row)">…</button></td>
            </tr>
            <tr v-if="!loading && !filteredRows.length"><td colspan="9" class="admin-owner-empty">目前沒有符合條件的付款資料</td></tr>
          </tbody>
        </table>
      </div>
      <div class="pager"><span>共 {{ totalRows }} records</span><div class="admin-building-pager"><button type="button" :disabled="pageNumber <= 1 || loading" @click="goToPage(pageNumber - 1)">&lt;</button><button v-for="number in visiblePages" :key="number" type="button" :class="{ active: number === pageNumber }" :disabled="loading" @click="goToPage(number)">{{ number }}</button><button type="button" :disabled="pageNumber >= totalPages || loading" @click="goToPage(pageNumber + 1)">&gt;</button><select v-model.number="pageSize" :disabled="loading" aria-label="每頁記錄數"><option :value="10">10 條/頁</option><option :value="20">20 條/頁</option><option :value="50">50 條/頁</option></select></div></div>
    </div>

    <aside class="panel detail-panel">
      <div v-if="selectedRow" class="detail-card">
        <div class="profile"><div class="big-avatar">{{ selectedRow.installmentNo }}</div><div><h3>{{ selectedRow.milestone || selectedRow.planName || '付款期數' }}</h3><p>{{ selectedRow.projectName }} · {{ selectedRow.unitNo }}</p></div><span class="tag" :class="statusClass(selectedRow.status)">{{ statusLabel(selectedRow.status) }}</span></div>
        <div class="detail-actions installment-actions">
          <button v-if="canEditSelected" @click="openInstallmentEdit">編輯本期</button>
          <button v-if="canRemindSelected" :disabled="reminderSending" @click="sendReminder">{{ reminderSending ? '發送中…' : reminderLabel }}</button>
          <button :disabled="!selectedRow.receiptNo" @click="openPaymentRecord">{{ selectedRow.status === 'paid' ? '查看收據' : '查看付款記錄' }}</button>
          <button v-if="selectedRow.confirmationStatus === 'pending'" @click="goToFinanceReview">前往財務確認</button>
        </div>
        <div class="detail-section"><h4><span class="num">1</span>資料資訊</h4><div class="kv"><span>業主</span><b>{{ selectedRow.ownerName || '—' }}</b></div><div class="kv"><span>合約編號</span><b>{{ selectedRow.contractNo || '—' }}</b></div><div class="kv"><span>付款計劃</span><b>{{ selectedRow.planName || '—' }}</b></div></div>
        <div class="detail-section"><h4><span class="num">2</span>付款狀態</h4><div class="kv"><span>應收金額</span><b>RM {{ money(selectedRow.amountDue) }}</b></div><div class="kv"><span>已收金額</span><b>RM {{ money(selectedRow.amountPaid) }}</b></div><div class="kv"><span>未收金額</span><b class="money-red">RM {{ money(selectedRow.unpaidAmount) }}</b></div></div>
        <div class="detail-section"><h4><span class="num">3</span>Payment progress</h4><div class="kv"><span>Due date</span><b>{{ selectedRow.dueDate || '—' }}</b></div><div class="kv"><span>Payment date</span><b>{{ selectedRow.paymentDate || '—' }}</b></div><div class="progress"><i :style="{ width: paymentProgress + '%' }"></i></div></div>
      </div>
      <div v-else class="admin-owner-empty">目前沒有可查看的付款資料</div>
    </aside>

    <dialog ref="projectDialog" class="modal admin-building-dialog">
      <form method="dialog" @submit.prevent="saveProject">
        <div class="modal-head"><div><h3>新增建案</h3><small>建立後可在業主管理新增單位與房產</small></div><button class="icon-close" type="button" @click="closeProjectDialog">×</button></div>
        <div class="form-grid">
          <label>建案編碼<input v-model.trim="projectForm.projectCode" maxlength="40" required placeholder="例如：CCPS-01"></label>
          <label>建案名稱<input v-model.trim="projectForm.name" maxlength="160" required placeholder="請輸入建案名稱"></label>
          <label class="wide">地址<input v-model.trim="projectForm.address" maxlength="255" placeholder="建案地址"></label>
          <label>城市<input v-model.trim="projectForm.city" maxlength="100" placeholder="例如：Kuala Lumpur"></label>
          <label>國家代碼<input v-model.trim="projectForm.countryCode" maxlength="2" required></label>
          <label>狀態<select v-model="projectForm.status"><option value="active">啟用</option><option value="inactive">停用</option></select></label>
          <p v-if="projectFormError" class="admin-property-error wide">{{ projectFormError }}</p>
        </div>
        <menu><button type="button" @click="closeProjectDialog">取消</button><button type="submit" class="primary-btn" :disabled="projectSaving">{{ projectSaving ? '建立中…' : '確認新增' }}</button></menu>
      </form>
    </dialog>

    <dialog ref="paymentPlanDialog" class="modal admin-payment-plan-dialog">
      <form method="dialog" @submit.prevent="savePaymentPlan">
        <div class="modal-head"><div><h3>新增房款</h3><small>建立購房合約的付款計劃與應收分期</small></div><button class="icon-close" type="button" @click="closePaymentPlanDialog">×</button></div>
        <div v-if="contractsLoading" class="admin-owner-state">正在載入可設定房款的合約…</div>
        <div v-else class="payment-plan-form">
          <div v-if="!eligibleContracts.length" class="admin-owner-state">
            目前沒有可新增房款的合約。房產必須為未交房，且不能已有啟用中的付款計劃。
          </div>
          <template v-else>
            <div class="form-grid">
              <label class="wide">購房合約<select v-model.number="paymentPlanForm.purchaseContractId" required><option disabled value="">請選擇建案、單位與業主</option><option v-for="contract in eligibleContracts" :key="contract.contractId" :value="contract.contractId">{{ contract.projectName }} · {{ contract.unitNo }} · {{ contract.ownerName }} · {{ contract.contractNo }}</option></select></label>
              <label>計劃名稱<input v-model.trim="paymentPlanForm.planName" maxlength="120" required placeholder="例如：工程進度付款計劃"></label>
              <label>生效日期<input v-model="paymentPlanForm.startDate" type="date"></label>
            </div>

            <div v-if="selectedContract" class="payment-contract-summary">
              <span><small>業主／單位</small><strong>{{ selectedContract.ownerName }} · {{ selectedContract.unitNo }}</strong></span>
              <span><small>購房合約</small><strong>{{ selectedContract.contractNo }}</strong></span>
              <span><small>合約總價</small><strong>{{ selectedContract.currency }} {{ money(selectedContract.purchasePrice) }}</strong></span>
            </div>

            <section class="payment-installment-editor">
              <div class="payment-installment-head"><div><h4>付款分期</h4><small>到期日必須依序遞增，合計必須等於合約總價</small></div><div><button type="button" @click="splitEvenly" :disabled="!selectedContract">平均分期</button><button type="button" class="primary-btn" @click="addInstallment">＋ 增加一期</button></div></div>
              <div class="payment-installment-row payment-installment-labels"><span>期數</span><span>工程階段／說明</span><span>到期日</span><span>應收金額</span><span></span></div>
              <div v-for="(installment, index) in paymentPlanForm.installments" :key="installment.key" class="payment-installment-row">
                <b>{{ index + 1 }}</b>
                <input v-model.trim="installment.milestone" maxlength="160" :placeholder="`第 ${index + 1} 期`">
                <input v-model="installment.dueDate" type="date" required>
                <input v-model="installment.amountDue" type="number" min="0.01" step="0.01" required>
                <button type="button" title="刪除此期" :disabled="paymentPlanForm.installments.length === 1" @click="removeInstallment(index)">×</button>
              </div>
            </section>

            <div class="payment-plan-totals">
              <span>合約總價 <b>{{ money(selectedContract?.purchasePrice) }}</b></span>
              <span>分期合計 <b>{{ money(planTotal) }}</b></span>
              <span :class="{ mismatch: planDifferenceCents !== 0, matched: planDifferenceCents === 0 }">差額 <b>{{ money(Math.abs(planDifferenceCents) / 100) }}</b></span>
            </div>
          </template>
          <p v-if="paymentPlanError" class="admin-property-error">{{ paymentPlanError }}</p>
        </div>
        <menu><button type="button" @click="closePaymentPlanDialog">取消</button><button type="submit" class="primary-btn" :disabled="paymentPlanSaving || contractsLoading || !eligibleContracts.length">{{ paymentPlanSaving ? '建立中…' : '確認建立房款' }}</button></menu>
      </form>
    </dialog>

    <dialog ref="installmentEditDialog" class="modal admin-installment-dialog">
      <form method="dialog" @submit.prevent="saveInstallment">
        <div class="modal-head"><div><h3>編輯本期房款</h3><small>{{ selectedRow?.projectName }} · {{ selectedRow?.unitNo }} · 第 {{ selectedRow?.installmentNo }} 期</small></div><button class="icon-close" type="button" @click="closeInstallmentEdit">×</button></div>
        <div class="form-grid">
          <label class="wide">工程階段／說明<input v-model.trim="installmentEditForm.milestone" maxlength="160" placeholder="例如：地基工程完成"></label>
          <label>到期日<input v-model="installmentEditForm.dueDate" type="date" required></label>
          <label>應收金額（RM）<input :value="money(selectedRow?.amountDue)" disabled><small>金額屬於整份付款計劃，不能在單一期數直接調整</small></label>
          <p v-if="installmentActionError" class="admin-property-error wide">{{ installmentActionError }}</p>
        </div>
        <menu><button type="button" @click="closeInstallmentEdit">取消</button><button type="submit" class="primary-btn" :disabled="installmentSaving">{{ installmentSaving ? '儲存中…' : '儲存本期' }}</button></menu>
      </form>
    </dialog>

    <dialog ref="paymentRecordDialog" class="modal admin-installment-dialog">
      <form method="dialog" @submit.prevent>
        <div class="modal-head"><div><h3>本期付款記錄</h3><small>{{ selectedRow?.projectName }} · {{ selectedRow?.unitNo }} · 第 {{ selectedRow?.installmentNo }} 期</small></div><button class="icon-close" type="button" @click="closePaymentRecord">×</button></div>
        <div class="installment-record-grid">
          <div><span>收據編號</span><b>{{ selectedRow?.receiptNo || '—' }}</b></div>
          <div><span>付款日期</span><b>{{ selectedRow?.paymentDate || '—' }}</b></div>
          <div><span>付款方式</span><b>{{ paymentMethodLabel(selectedRow?.paymentMethod) }}</b></div>
          <div><span>財務確認</span><b><i class="tag" :class="confirmationClass(selectedRow?.confirmationStatus)">{{ confirmationLabel(selectedRow?.confirmationStatus) }}</i></b></div>
          <div class="wide"><span>銀行／交易參考</span><b>{{ selectedRow?.bankReference || '—' }}</b></div>
          <div class="wide"><span>提交備註</span><b>{{ selectedRow?.submissionNote || '—' }}</b></div>
          <div><span>本次提交金額</span><b>RM {{ money(selectedRow?.submittedAmount) }}</b></div>
          <div><span>已確認收款</span><b>RM {{ money(selectedRow?.amountPaid) }}</b></div>
          <div><span>憑證文件</span><b>{{ selectedRow?.proofDocumentId ? `文件 #${selectedRow.proofDocumentId}` : '—' }}</b></div>
        </div>
        <menu><button type="button" @click="closePaymentRecord">關閉</button><button v-if="selectedRow?.confirmationStatus === 'pending'" type="button" class="primary-btn" @click="goToFinanceReview">前往財務確認</button></menu>
      </form>
    </dialog>
  </section>
</template>

<script>
import pageBridge from '../pageBridge';
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
    projectCreateRequestNonce() { return this.page.adminBuildingProjectCreateNonce; },
    paymentPlanCreateRequestNonce() { return this.page.adminPaymentPlanCreateNonce; },
    eligibleContracts() { return this.paymentContracts.filter(contract => !contract.hasActivePlan); },
    selectedContract() { return this.paymentContracts.find(contract => contract.contractId === this.paymentPlanForm.purchaseContractId) || null; },
    planTotalCents() { return this.paymentPlanForm.installments.reduce((sum, item) => sum + this.toCents(item.amountDue), 0); },
    planTotal() { return this.planTotalCents / 100; },
    planDifferenceCents() { return this.planTotalCents - this.toCents(this.selectedContract?.purchasePrice); },
    canEditSelected() { return this.selectedRow && this.selectedRow.status !== 'paid'; },
    canRemindSelected() { return this.selectedRow && Number(this.selectedRow.unpaidAmount || 0) > 0; },
    reminderLabel() { return this.selectedRow?.status === 'overdue' ? '發送逾期提醒' : this.selectedRow?.status === 'partial' ? '發送餘款提醒' : '發送付款提醒'; }
  },
  watch: {
    rows: { immediate: true, deep: true, handler(rows) { if (!rows.some(row => row.id === this.selectedId)) this.selectedId = rows[0]?.id || null; } },
    pageSize() { this.pageNumber = 1; this.loadData(); },
    moduleSearch() { this.resetAndLoad(); },
    globalSearch() { this.resetAndLoad(); },
    projectFilter() { this.resetAndLoad(); },
    statusFilter() { this.resetAndLoad(); },
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
      if (!this.selectedRow || !this.installmentEditForm.dueDate) { this.installmentActionError = '請填寫到期日'; return; }
      const selectedId = this.selectedRow.id; this.installmentSaving = true; this.installmentActionError = '';
      try {
        await updateAdminPaymentInstallment(selectedId, { milestone: this.installmentEditForm.milestone || null, dueDate: this.installmentEditForm.dueDate });
        await this.loadData(); if (this.rows.some(row => row.id === selectedId)) this.selectedId = selectedId;
        this.closeInstallmentEdit(); this.showToast('本期房款資料已更新');
      } catch (error) { this.installmentActionError = error.message || '本期房款更新失敗'; }
      finally { this.installmentSaving = false; }
    },
    async sendReminder() {
      if (!this.canRemindSelected || this.reminderSending) return;
      this.reminderSending = true;
      try { await sendAdminPaymentReminder(this.selectedRow.id); this.showToast('付款提醒已發送至業主通知中心'); }
      catch (error) { this.showToast(error.message || '付款提醒發送失敗'); }
      finally { this.reminderSending = false; }
    },
    openPaymentRecord() { if (this.selectedRow?.receiptNo) this.$refs.paymentRecordDialog?.showModal(); },
    closePaymentRecord() { this.$refs.paymentRecordDialog?.close(); },
    goToFinanceReview() {
      const receiptNo = this.selectedRow?.receiptNo || '';
      this.closePaymentRecord(); this.selectModule('adminFinance');
      this.$nextTick(() => { this.page.globalSearch = receiptNo; });
    },
    paymentMethodLabel(value) { return ({ bank_transfer: '銀行轉帳', cheque: '支票', cash: '現金', online_banking: '網上銀行' })[value] || value || '—'; },
    confirmationLabel(value) { return ({ pending: '待財務確認', confirmed: '已確認', rejected: '已拒絕' })[value] || '尚未提交'; },
    confirmationClass(value) { return value === 'confirmed' ? 'green' : value === 'rejected' ? 'red' : 'orange'; },
    openProjectCreate() {
      this.projectForm = { projectCode: '', name: '', address: '', city: '', countryCode: 'MY', status: 'active' };
      this.projectFormError = ''; this.$refs.projectDialog?.showModal();
    },
    closeProjectDialog() { this.$refs.projectDialog?.close(); },
    async saveProject() {
      if (!this.projectForm.projectCode || !this.projectForm.name) { this.projectFormError = '請填寫建案編碼與名稱'; return; }
      this.projectSaving = true; this.projectFormError = '';
      try {
        const project = await createAdminBuildingProject({ ...this.projectForm, countryCode: this.projectForm.countryCode.toUpperCase() });
        this.page.adminBuildingProjects = [...new Set([...(this.page.adminBuildingProjects || []), project.name])].sort((a, b) => a.localeCompare(b));
        this.closeProjectDialog(); this.showToast('建案已建立，可前往業主管理新增單位');
      } catch (error) { this.projectFormError = error.message || '新增建案失敗'; }
      finally { this.projectSaving = false; }
    },
    emptyInstallment(index = 0) {
      const date = new Date(); date.setMonth(date.getMonth() + index + 1);
      return { key: this.installmentKey++, milestone: '', dueDate: date.toISOString().slice(0, 10), amountDue: '' };
    },
    async openPaymentPlanCreate() {
      this.paymentPlanForm = { purchaseContractId: '', planName: '工程進度付款計劃', startDate: new Date().toISOString().slice(0, 10), installments: [this.emptyInstallment()] };
      this.paymentPlanError = ''; this.paymentContracts = []; this.$refs.paymentPlanDialog?.showModal(); this.contractsLoading = true;
      try {
        this.paymentContracts = await fetchAdminPaymentContracts();
        if (this.eligibleContracts.length === 1) this.paymentPlanForm.purchaseContractId = this.eligibleContracts[0].contractId;
      } catch (error) { this.paymentPlanError = error.message || '購房合約載入失敗'; }
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
      if (!this.selectedContract) { this.paymentPlanError = '請選擇購房合約'; return; }
      if (!this.paymentPlanForm.planName) { this.paymentPlanError = '請填寫付款計劃名稱'; return; }
      if (this.paymentPlanForm.installments.some(item => !item.dueDate || this.toCents(item.amountDue) <= 0)) { this.paymentPlanError = '每一期都需要填寫到期日與大於零的應收金額'; return; }
      for (let index = 1; index < this.paymentPlanForm.installments.length; index++) {
        if (this.paymentPlanForm.installments[index].dueDate <= this.paymentPlanForm.installments[index - 1].dueDate) { this.paymentPlanError = '每一期到期日必須依序遞增'; return; }
      }
      if (this.planDifferenceCents !== 0) { this.paymentPlanError = '分期合計必須等於購房合約總價'; return; }
      this.paymentPlanSaving = true; this.paymentPlanError = '';
      try {
        await createAdminPaymentPlan({
          purchaseContractId: this.paymentPlanForm.purchaseContractId,
          planName: this.paymentPlanForm.planName,
          startDate: this.paymentPlanForm.startDate || null,
          installments: this.paymentPlanForm.installments.map(item => ({ milestone: item.milestone || null, dueDate: item.dueDate, amountDue: (this.toCents(item.amountDue) / 100).toFixed(2) }))
        });
        this.closePaymentPlanDialog(); this.pageNumber = 1; await this.loadData(); this.showToast('房款計劃與分期已建立');
      } catch (error) { this.paymentPlanError = error.message || '新增房款失敗'; }
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
        { icon: 'building', label: '房產總價', value: moneyValue(summary.propertyTotal), delta: '資料庫即時統計', trend: '' },
        { icon: 'paid', label: '已繳總金額', value: moneyValue(summary.paidTotal), delta: '付款期數累計', trend: 'up' },
        { icon: 'unpaid', label: '未繳總金額', value: moneyValue(summary.unpaidTotal), delta: '待追蹤款項', trend: summary.unpaidTotal > 0 ? 'down' : 'up' },
        { icon: 'total', label: '總繳費期數', value: `${summary.totalInstallments} 期`, delta: '目前付款計劃', trend: '' },
        { icon: 'paidCount', label: '已繳期數', value: `${summary.paidInstallments} 期`, delta: '已完成付款', trend: 'up' },
        { icon: 'remaining', label: '剩餘期數', value: `${summary.remainingInstallments} 期`, delta: '待付款期數', trend: summary.remainingInstallments ? 'down' : 'up' },
        { icon: 'next', label: '下一期金額', value: moneyValue(summary.nextAmount), delta: '未繳期數中最近一筆', trend: 'down' },
        { icon: 'date', label: '下一期到期日', value: summary.nextDueDate || '—', delta: '資料庫即時統計', trend: 'down' }
      ];
    },
    selectRow(row) { this.selectedId = row.id; },
    statusParam(status) { return ({ '已完成': 'paid', '部分付款': 'partial', '待付款': 'pending', '逾期': 'overdue' })[status] || ''; },
    statusLabel(status) { return ({ paid: '已完成', partial: '部分付款', pending: '待付款', overdue: '逾期' })[status] || '待付款'; },
    statusClass(status) { return status === 'paid' ? 'green' : status === 'overdue' ? 'red' : 'orange'; },
    money(value) { return Number(value || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
  }
};
</script>
