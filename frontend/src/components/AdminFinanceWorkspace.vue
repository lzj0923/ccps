<template>
  <div class="admin-finance-shell">
    <div class="admin-finance-navigation">
      <nav class="admin-finance-type-tabs" :aria-label="$t('legacy.t_05f0d1143af2')">
        <button type="button" :class="{ active: financeType === 'rent' }" :aria-pressed="financeType === 'rent'" @click="switchFinanceType('rent')"><span>{{ $t('legacy.t_5d00753a306a') }}</span><b>{{ $t('legacy.t_5f3d34a1947f') }}</b><small>{{ $t('legacy.t_81a0964fde12') }}</small></button>
        <button type="button" :class="{ active: financeType === 'tenant_deposit' }" :aria-pressed="financeType === 'tenant_deposit'" @click="switchFinanceType('tenant_deposit')"><span>押</span><b>{{ $t('finance.tenantDepositTab') }}</b><small>{{ $t('finance.tenantDepositTabHint') }}</small></button>
        <button type="button" :class="{ active: financeType === 'expense' }" :aria-pressed="financeType === 'expense'" @click="switchFinanceType('expense')"><span>租</span><b>租客费用</b><small>租客应承担费用</small></button>
        <button type="button" :class="{ active: financeType === 'cashflow_maintenance' }" :aria-pressed="financeType === 'cashflow_maintenance'" @click="switchFinanceType('cashflow_maintenance')"><span>维</span><b>收支与维修</b><small>收支记录及维修付款</small></button>
        <button type="button" :class="{ active: financeType === 'reserve' }" :aria-pressed="financeType === 'reserve'" @click="switchFinanceType('reserve')"><span>{{ $t('legacy.t_aab9b3921100') }}</span><b>{{ $t('legacy.t_34aa642a0856') }}</b><small>{{ $t('legacy.t_2ab4575a1aac') }}</small></button>
        <button type="button" :class="{ active: financeType === 'reserve_refund' }" :aria-pressed="financeType === 'reserve_refund'" @click="switchFinanceType('reserve_refund')"><span>返</span><b>{{ $t('finance.reserveRefundTab') }}</b><small>{{ $t('finance.reserveRefundTabHint') }}</small></button>
        <button type="button" :class="{ active: financeType === 'property' }" :aria-pressed="financeType === 'property'" @click="switchFinanceType('property')"><span>{{ $t('legacy.t_510cf918d2af') }}</span><b>{{ $t('legacy.t_6c1975ad4c66') }}</b><small>{{ $t('legacy.t_883bf403fa13') }}</small></button>
      </nav>
      <button v-if="viewMode === 'pending'" type="button" class="finance-history-button" @click="switchViewMode('history')">{{ $t('legacy.t_98556ff264a5') }}</button>
      <button v-if="viewMode === 'history'" type="button" class="finance-history-button back" @click="switchViewMode('pending')"><span aria-hidden="true">←</span> {{ $t('legacy.t_aef2b015b09f') }}</button>
    </div>
  <ModuleToolbar />
  <section v-if="financeType === 'property'" class="content-grid admin-finance-workspace">
    <div class="panel table-panel finance-review-panel">
      <div class="panel-head">
        <div><h2>{{ viewMode === 'pending' ? $t('legacy.t_20098003f2ed') : $t('legacy.t_157301c893f6') }}</h2><span>{{ totalRows }} {{ $t('legacy.t_86761b63a7bd') }}</span></div>
        <div class="finance-list-actions">
          <span v-if="selectedIds.length">{{ $t('legacy.t_06fb19973447') }} {{ selectedIds.length }} {{ $t('legacy.t_d0bb9b2b8ea7') }}</span><span v-if="loading">{{ $t('legacy.t_6ce3778a43cd') }}</span>
          <template v-if="viewMode === 'history'">
            <button type="button" class="finance-list-batch-button" :disabled="!selectedIds.length || batchDocumentBusy" @click="batchDownloadDocuments('invoice')">批量下载发票</button>
            <button type="button" class="finance-list-batch-button" :disabled="!selectedIds.length || batchDocumentBusy" @click="batchDownloadDocuments('receipt')">批量下载收据</button>
            <button type="button" class="finance-list-batch-button danger" :disabled="!selectedReopenIds.length || actionSaving" @click="openBatchReopen">批量退回</button>
          </template>
        </div>
      </div>

      <div v-if="errorMessage" class="admin-owner-state error" role="alert">
        <strong>{{ $t('legacy.t_9d56180a3aa0') }}</strong><span>{{ errorMessage }}</span><button type="button" @click="loadData">{{ $t('legacy.t_0c9157b5bfac') }}</button>
      </div>
      <div v-else class="table-wrap finance-review-table-wrap">
        <table>
          <thead><tr><th class="finance-check-cell"><input type="checkbox" :checked="allSelectableSelected" :disabled="!selectablePageRows.length" :aria-label="$t('legacy.t_b944d0833969')" @change="toggleSelectablePage"></th><th>{{ $t('legacy.t_32e88cf956f6') }}</th><th>{{ $t('legacy.t_114246450ff0') }}</th><th>{{ $t('legacy.t_d61f1eba334a') }}</th><th>{{ $t('legacy.t_293b17eefc95') }}</th><th>{{ $t('legacy.t_058f511c98cf') }}</th><th>{{ $t('legacy.t_e2339796356e') }}</th><th>{{ $t('legacy.t_4feb65fa9683') }}</th><th>{{ $t('legacy.t_b041d1d11b87') }}</th><th>{{ $t('legacy.t_f3ea6d345e2a') }}</th></tr></thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id" :class="{ selected: row.id === selectedId }" @click="selectRow(row)">
              <td class="finance-check-cell"><input v-if="viewMode === 'history' || row.confirmationStatus === 'pending'" v-model="selectedIds" type="checkbox" :value="row.id" :aria-label="$t('finance.selectTransaction', { transactionNo: row.transactionNo })" @click.stop></td>
              <td><strong>{{ row.transactionNo }}</strong><small>{{ row.receiptNo || $t('legacy.t_655670295796') }}</small></td>
              <td>{{ row.projectName }}<small>{{ row.unitNo }} {{ $t('legacy.t_39a26997f3e2') }} {{ row.installmentNo }} {{ $t('legacy.t_fc73601f2012') }}</small></td>
              <td>{{ row.payerName || '—' }}</td>
              <td><b>{{ row.currency }} {{ money(row.amount) }}</b></td>
              <td>{{ displayDate(row.transactionDate) }}</td>
              <td><button type="button" class="finance-proof-link" :disabled="!row.proofDocumentId" @click.stop="selectAndOpenProof(row)">{{ row.proofDocumentId ? $t('legacy.t_16194d3fdaa8') : $t('legacy.t_08f9d55c4a07') }}</button></td>
              <td><span class="tag" :class="confirmationClass(row.confirmationStatus)">{{ confirmationLabel(row.confirmationStatus) }}</span></td>
              <td><span class="tag" :class="syncClass(row.syncStatus)">{{ syncLabel(row.syncStatus) }}</span></td>
              <td><button type="button" class="finance-detail-button" @click.stop="openDetails(row)">查看详情</button></td>
            </tr>
            <tr v-if="!loading && !rows.length"><td colspan="10" class="admin-owner-empty">{{ $t('legacy.t_249035ec5788') }}</td></tr>
          </tbody>
        </table>
      </div>
      <div class="pager"><span>{{ $t('legacy.t_3b6ef811b85a') }} {{ totalRows }} {{ $t('legacy.t_86761b63a7bd') }}</span><div class="admin-building-pager"><button type="button" :disabled="pageNumber <= 1 || loading" @click="goToPage(pageNumber - 1)">&lt;</button><button v-for="number in visiblePages" :key="number" type="button" :class="{ active: number === pageNumber }" :disabled="loading" @click="goToPage(number)">{{ number }}</button><button type="button" :disabled="pageNumber >= totalPages || loading" @click="goToPage(pageNumber + 1)">&gt;</button><select v-model.number="pageSize" :disabled="loading" :aria-label="$t('legacy.t_7650a7a51895')"><option :value="10">{{ $t('legacy.t_fc6da0e815a1') }}</option><option :value="20">{{ $t('legacy.t_93d673672fa5') }}</option><option :value="50">{{ $t('legacy.t_529930b886d2') }}</option></select></div></div>
    </div>

    <dialog ref="detailDialog" class="modal finance-record-detail-dialog">
      <template v-if="selectedRow">
        <div class="modal-head"><div><h3>房款确认详情</h3><small>{{ selectedRow.transactionNo }}</small></div><button type="button" class="icon-close" @click="closeDetails">×</button></div>
        <div class="finance-record-detail-body detail-card">
        <div class="profile finance-review-profile">
          <div class="big-avatar">{{ $t('legacy.t_72d148251eae') }}</div>
          <div class="finance-review-profile-copy">
            <h3 :title="selectedRow.transactionNo">{{ selectedRow.transactionNo }}</h3>
            <p>{{ selectedRow.projectName }}<span>·</span>{{ selectedRow.unitNo }}</p>
          </div>
          <span class="tag finance-review-status" :class="confirmationClass(selectedRow.confirmationStatus)">{{ confirmationLabel(selectedRow.confirmationStatus) }}</span>
        </div>
        <div class="detail-actions finance-review-actions">
          <button :disabled="!selectedRow.proofDocumentId" @click="openProofFromDetails">{{ $t('legacy.t_96bd7ca72147') }}</button>
          <a v-if="viewMode === 'history'" class="finance-document-button" :href="documentUrl(selectedRow, 'invoice')" download>Invoice / 发票</a>
          <a v-if="viewMode === 'history'" class="finance-document-button" :href="documentUrl(selectedRow, 'receipt')" download>Official Receipt / 收据</a>
          <button v-if="viewMode === 'history' && selectedRow.confirmationStatus === 'confirmed'" class="reopen" @click="openReopenFromDetails">{{ $t('finance.reopen') }}</button>
          <button v-if="selectedRow.confirmationStatus === 'pending'" class="confirm" @click="openDecisionFromDetails('confirm')">{{ $t('legacy.t_a042dbbff199') }}</button>
          <button v-if="selectedRow.confirmationStatus === 'pending'" class="reject" @click="openDecisionFromDetails('reject')">{{ $t('legacy.t_579798368137') }}</button>
          <button @click="goToInstallmentFromDetails">{{ $t('legacy.t_367ff02e0e0d') }}</button>
        </div>
        <div class="detail-section"><h4><span class="num">{{ $t('legacy.t_356a192b7913') }}</span>{{ $t('legacy.t_a7110c6a0220') }}</h4><div class="kv"><span>{{ $t('legacy.t_d61f1eba334a') }}</span><b>{{ selectedRow.payerName || '—' }}</b></div><div class="kv"><span>{{ $t('legacy.t_c6b9a8cfdb21') }}</span><b>{{ paymentMethodLabel(selectedRow.paymentMethod) }}</b></div><div class="kv"><span>{{ $t('legacy.t_bf8928021bb1') }}</span><b>{{ selectedRow.bankReference || '—' }}</b></div><div class="kv"><span>{{ $t('legacy.t_293b17eefc95') }}</span><b>{{ selectedRow.currency }} {{ money(selectedRow.amount) }}</b></div><div class="kv"><span>{{ $t('legacy.t_058f511c98cf') }}</span><b>{{ displayDate(selectedRow.transactionDate) }}</b></div></div>
        <div class="detail-section"><h4><span class="num">{{ $t('legacy.t_da4b9237bacc') }}</span>{{ $t('legacy.t_e46046d63d0d') }}</h4><div class="kv"><span>{{ $t('legacy.t_845f73689cac') }}</span><b>{{ selectedRow.installmentNo }}. {{ selectedRow.milestone || $t('legacy.t_8fa4c4e6ba7b') }}</b></div><div class="kv"><span>{{ $t('legacy.t_fdeda5befee2') }}</span><b>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(selectedRow.installmentAmount) }}</b></div><div class="kv"><span>{{ $t('legacy.t_25369a5ff28e') }}</span><b>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(selectedRow.installmentPaid) }}</b></div><div class="kv"><span>{{ $t('legacy.t_894b94156d36') }}</span><b>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(selectedRow.allocatedAmount) }}</b></div><div class="progress"><i :style="{ width: installmentProgress + '%' }"></i></div></div>
        <div class="detail-section"><h4><span class="num">{{ $t('legacy.t_77de68daecd8') }}</span>{{ $t('legacy.t_a89be4ee9d42') }}</h4><div class="kv"><span>{{ $t('legacy.t_4feb65fa9683') }}</span><b>{{ confirmationLabel(selectedRow.confirmationStatus) }}</b></div><div class="kv"><span>{{ $t('legacy.t_bffa37080afa') }}</span><b>{{ selectedRow.confirmedByName || '—' }}</b></div><div class="kv"><span>{{ $t('legacy.t_2d36c3ab208f') }}</span><b>{{ formatDateTime(selectedRow.confirmedAt) }}</b></div><div class="kv"><span>{{ $t('legacy.t_098de965a383') }}</span><b>{{ selectedRow.reviewNote || '—' }}</b></div><div class="kv"><span>{{ $t('legacy.t_94007fdd6e4e') }}</span><b>{{ syncLabel(selectedRow.syncStatus) }}</b></div></div>
        <FinanceAllocationNoteEditor :record="selectedRow" @saved="loadData" />
        </div>
      </template>
    </dialog>

    <dialog ref="decisionDialog" class="modal admin-finance-decision-dialog">
      <form method="dialog" @submit.prevent="submitDecision">
        <div class="modal-head"><div><h3>{{ decisionMode === 'confirm' ? $t('legacy.t_a042dbbff199') : $t('legacy.t_579798368137') }}</h3><small>{{ selectedRow?.transactionNo }} · {{ selectedRow?.currency }} {{ money(selectedRow?.amount) }}</small></div><button type="button" class="icon-close" @click="closeDecision">×</button></div>
        <div class="finance-decision-body">
          <div v-if="decisionMode === 'confirm'" class="finance-decision-warning success"><strong>{{ $t('legacy.t_269da3e1382b') }}</strong><span>{{ $t('legacy.t_0732a583bac6') }} {{ selectedRow?.installmentNo }} {{ $t('legacy.t_49ae7153d444') }}</span></div>
          <div v-else class="finance-decision-warning"><strong>{{ $t('legacy.t_88b8017997d0') }}</strong><span>{{ $t('legacy.t_803f1e0a838e') }}</span></div>
          <label v-if="decisionMode === 'confirm'">实际收款日期<input v-model="decisionReceiptDate" type="date" :max="today" required></label>
          <label v-if="decisionMode === 'confirm'">财务入账日期<input v-model="decisionDate" type="date" :max="today" required><small>入账日期用于 CCPS 余额、报表与关联流水。</small></label>
          <label>{{ $t('legacy.t_098de965a383') }}<textarea v-model.trim="decisionNote" maxlength="500" required :placeholder="decisionMode === 'confirm' ? $t('legacy.t_c333960469a4') : $t('legacy.t_b57961bc9a8a')"></textarea></label>
          <p v-if="actionError" class="admin-property-error">{{ actionError }}</p>
        </div>
        <menu><button type="button" @click="closeDecision">{{ $t('legacy.t_4d0b4688c787') }}</button><button type="submit" class="primary-btn" :class="{ reject: decisionMode === 'reject' }" :disabled="actionSaving">{{ actionSaving ? $t('legacy.t_1e038f9b55ec') : decisionMode === 'confirm' ? $t('legacy.t_c6f613106a48') : $t('legacy.t_559f15fd1a6a') }}</button></menu>
      </form>
    </dialog>

    <dialog ref="reopenDialog" class="modal admin-finance-decision-dialog">
      <form method="dialog" @submit.prevent="submitReopen">
        <div class="modal-head"><div><h3>{{ $t('finance.reopenTitle') }}</h3><small>{{ selectedRow?.transactionNo }} · {{ selectedRow?.currency }} {{ money(selectedRow?.amount) }}</small></div><button type="button" class="icon-close" @click="closeReopen">×</button></div>
        <div class="finance-decision-body"><div class="finance-decision-warning"><strong>{{ $t('finance.reopen') }}</strong><span>{{ $t('finance.reopenHint') }}</span></div><label>{{ $t('finance.reopenNote') }}<textarea v-model.trim="reopenNote" maxlength="500" required :placeholder="$t('finance.reopenNotePlaceholder')"></textarea></label><p v-if="actionError" class="admin-property-error">{{ actionError }}</p></div>
        <menu><button type="button" @click="closeReopen">{{ $t('legacy.t_4d0b4688c787') }}</button><button type="submit" class="primary-btn reject" :disabled="actionSaving">{{ actionSaving ? $t('legacy.t_1e038f9b55ec') : $t('finance.reopen') }}</button></menu>
      </form>
    </dialog>

    <dialog ref="batchReopenDialog" class="modal admin-finance-decision-dialog">
      <form method="dialog" @submit.prevent="submitBatchReopen">
        <div class="modal-head"><div><h3>批量退回待确认</h3><small>本次将退回 {{ selectedReopenIds.length }} 笔已确认记录</small></div><button type="button" class="icon-close" @click="closeBatchReopen">×</button></div>
        <div class="finance-decision-body"><div class="finance-decision-warning"><strong>批量退回后可重新核对</strong><span>若其中一笔无法退回，本次操作将整体取消，不会只处理一部分。</span></div><label>退回原因<textarea v-model.trim="batchReopenNote" maxlength="500" required placeholder="请填写本次批量退回原因"></textarea></label><p v-if="actionError" class="admin-property-error">{{ actionError }}</p></div>
        <menu><button type="button" @click="closeBatchReopen">取消</button><button type="submit" class="primary-btn reject" :disabled="actionSaving">{{ actionSaving ? '处理中…' : `退回 ${selectedReopenIds.length} 笔` }}</button></menu>
      </form>
    </dialog>

    <dialog ref="batchDialog" class="modal admin-finance-decision-dialog">
      <form method="dialog" @submit.prevent="submitBatch">
        <div class="modal-head"><div><h3>{{ $t('legacy.t_fa73e61d9cbe') }}</h3><small>{{ $t('legacy.t_aeec0b67da9d') }} {{ selectedIds.length }} {{ $t('legacy.t_02819cf567a4') }}</small></div><button type="button" class="icon-close" @click="closeBatch">×</button></div>
        <div class="finance-decision-body"><div class="finance-batch-overview"><span>确认记录<b>{{ batchRows.length }} 笔</b></span><span>合计金额<b>MYR {{ money(batchTotal) }}</b></span></div><div class="finance-decision-warning success"><strong>当前列表已自动带入</strong><span>无需逐笔勾选；如需只处理部分记录，可关闭后再勾选指定记录。</span></div><label>实际收款日期<input v-model="batchReceiptDate" type="date" :max="today" required></label><label>财务入账日期<input v-model="batchDate" type="date" :max="today" required><small>本批记录使用同一入账日期。</small></label><label>{{ $t('finance.batchReferenceNo') }}<input v-model.trim="batchReference" maxlength="120" :placeholder="$t('finance.batchReferencePlaceholder')"></label><label>{{ $t('legacy.t_c5dc4a6f6bc8') }}<textarea v-model.trim="batchNote" maxlength="500" required></textarea></label><p v-if="actionError" class="admin-property-error">{{ actionError }}</p></div>
        <menu><button type="button" @click="closeBatch">{{ $t('legacy.t_4d0b4688c787') }}</button><button type="submit" class="primary-btn" :disabled="actionSaving">{{ actionSaving ? $t('legacy.t_03fc655d6daf') : $t('finance.confirmSelected', { count: selectedIds.length }) }}</button></menu>
      </form>
    </dialog>

    <dialog ref="proofDialog" class="modal admin-finance-proof-dialog">
      <div class="modal-head"><div><h3>{{ $t('legacy.t_41ee43593eb7') }}</h3><small>{{ selectedRow?.proofName || selectedRow?.transactionNo }}</small></div><button type="button" class="icon-close" @click="closeProof">×</button></div>
      <div class="finance-proof-viewer"><div v-if="proofLoading" class="admin-owner-state">{{ $t('legacy.t_ef3161c22125') }}</div><img v-else-if="proofUrl && selectedRow?.proofMimeType?.startsWith('image/')" :src="proofUrl" :alt="$t('legacy.t_41ee43593eb7')"><iframe v-else-if="proofUrl && selectedRow?.proofMimeType === 'application/pdf'" :src="proofUrl" :title="$t('legacy.t_44472697225e')"></iframe><div v-else class="admin-owner-state">{{ proofError || $t('legacy.t_410b7e2a33ff') }}</div></div>
      <menu><button type="button" @click="closeProof">{{ $t('legacy.t_ddc05404b0d6') }}</button><button type="button" class="primary-btn" :disabled="!selectedRow?.proofDocumentId" @click="downloadProof">{{ $t('legacy.t_fb332e3c9ee6') }}</button></menu>
    </dialog>
  </section>
  <AdminRentCollectionWorkspace v-else-if="financeType === 'rent' && viewMode === 'pending'" ref="rentFinance" />
  <AdminRentFinanceWorkspace v-else-if="financeType === 'rent'" ref="rentFinanceHistory" />
  <AdminReserveFinanceWorkspace v-else-if="financeType === 'reserve'" ref="reserveFinance" />
  <AdminExpenseFinanceWorkspace v-else :key="financeType" ref="expenseFinance" :review-type="financeType" />
  </div>
</template>

<script>
import { batchConfirmAdminFinanceReviews, batchReopenAdminFinanceReviews, confirmAdminFinanceReview, downloadAdminFinanceDocuments, fetchAdminFinanceProjects, fetchAdminFinanceProof, fetchAdminFinanceReviews, getAdminFinanceDocumentUrl, rejectAdminFinanceReview, reopenAdminFinanceReview } from '../services/propertyApi';
import AdminRentCollectionWorkspace from './AdminRentCollectionWorkspace.vue';
import AdminRentFinanceWorkspace from './AdminRentFinanceWorkspace.vue';
import AdminReserveFinanceWorkspace from './AdminReserveFinanceWorkspace.vue';
import AdminExpenseFinanceWorkspace from './AdminExpenseFinanceWorkspace.vue';
import { formatDate, formatDateTime as displayDateTime, todayIsoDate } from '../utils/dateFormat';
import ModuleToolbar from './ModuleToolbar.vue';
import FinanceAllocationNoteEditor from './FinanceAllocationNoteEditor.vue';

export default {
  components: { AdminRentCollectionWorkspace, AdminRentFinanceWorkspace, AdminReserveFinanceWorkspace, AdminExpenseFinanceWorkspace, ModuleToolbar, FinanceAllocationNoteEditor },
  inject: ['page'],
  data() {
    return {
      financeType: 'property', rows: [], selectedId: null, selectedIds: [], loading: false, errorMessage: '', requestSerial: 0,
      pageNumber: 1, pageSize: 5, totalRows: 0, totalPages: 1,
      decisionMode: 'confirm', decisionDate: todayIsoDate(), decisionReceiptDate: todayIsoDate(), batchDate: todayIsoDate(), batchReceiptDate: todayIsoDate(), today: todayIsoDate(), decisionNote: '', reopenNote: '', batchReopenNote: '', batchNote: '批量核對付款憑證與銀行入賬資料一致', batchReference: '', actionSaving: false, actionError: '',
      proofLoading: false, proofError: '', proofUrl: '', batchDocumentBusy: false
    };
  },
  computed: {
    selectedRow() { return this.rows.find(row => row.id === this.selectedId) || this.rows[0] || null; },
    viewMode() { return this.page.adminFinanceViewMode === 'history' ? 'history' : 'pending'; },
    pendingPageRows() { return this.rows.filter(row => row.confirmationStatus === 'pending'); },
    selectablePageRows() { return this.viewMode === 'history' ? this.rows : this.pendingPageRows; },
    batchRows() { return this.rows.filter(row => this.selectedIds.includes(row.id)); },
    batchTotal() { return this.batchRows.reduce((sum, row) => sum + Number(row.amount || 0), 0); },
    allSelectableSelected() { return this.selectablePageRows.length > 0 && this.selectablePageRows.every(row => this.selectedIds.includes(row.id)); },
    selectedReopenIds() { return this.rows.filter(row => this.selectedIds.includes(row.id) && row.confirmationStatus === 'confirmed').map(row => row.id); },
    visiblePages() { const start = Math.max(1, Math.min(this.pageNumber - 2, this.totalPages - 4)); const end = Math.min(this.totalPages, start + 4); return Array.from({ length: Math.max(1, end - start + 1) }, (_, index) => start + index); },
    installmentProgress() { return !Number(this.selectedRow?.installmentAmount) ? 0 : Math.min(100, Math.round(Number(this.selectedRow.installmentPaid || 0) / Number(this.selectedRow.installmentAmount) * 100)); },
    refreshNonce() { return this.page.adminFinanceRefreshNonce; },
    batchNonce() { return this.page.adminFinanceBatchNonce; }
  },
  watch: {
    'page.moduleSearch'() { this.resetAndLoad(); }, 'page.globalSearch'() { this.resetAndLoad(); },
    'page.projectFilter'() { this.resetAndLoad(); }, 'page.statusFilter'() { this.resetAndLoad(); },
    'page.adminFinanceViewMode'() { this.resetAndLoad(); },
    'page.dateStart'() { this.resetAndLoad(); }, 'page.dateEnd'() { this.resetAndLoad(); },
    pageSize() { this.pageNumber = 1; this.loadData(); },
    refreshNonce(value, previous) { if (value > previous) this.loadData(); },
    batchNonce(value, previous) { if (value > previous && this.financeType === 'property') this.openBatch(); },
    rows: { deep: true, immediate: true, handler(rows) { if (!rows.some(row => row.id === this.selectedId)) this.selectedId = rows[0]?.id || null; } }
  },
  mounted() {
    const requestedType = this.page.adminFinanceMode === 'tenant_charge' ? 'expense' : this.page.adminFinanceMode;
    this.financeType = ['property', 'rent', 'reserve', 'expense', 'cashflow_maintenance', 'reserve_refund', 'tenant_deposit'].includes(requestedType) ? requestedType : 'property';
    this.page.adminFinanceMode = this.financeType;
    this.page.adminFinanceViewMode = 'pending';
    this.page.projectFilter = '全部建案';
    this.page.statusFilter = this.pendingStatusLabel(this.financeType);
    if (this.financeType === 'property') this.loadData();
  },
  beforeUnmount() { this.revokeProofUrl(); },
  methods: {
    async loadData() {
      if (this.financeType !== 'property') return;
      const serial = ++this.requestSerial; const preferredId = this.selectedId; this.loading = true; this.errorMessage = '';
      try {
        const [response, projects] = await Promise.all([fetchAdminFinanceReviews({ page: this.pageNumber, pageSize: this.pageSize, keyword: this.page.globalSearch || this.page.moduleSearch || '', projectName: String(this.page.projectFilter || '').includes('全部') ? '' : this.page.projectFilter, status: this.requestStatus(), startDate: this.page.dateStart, endDate: this.page.dateEnd }), fetchAdminFinanceProjects()]);
        if (serial !== this.requestSerial || this.financeType !== 'property') return;
        this.rows = response.rows || []; this.totalRows = response.page?.totalRows || 0; this.totalPages = response.page?.totalPages || 1; this.pageNumber = response.page?.page || this.pageNumber;
        this.page.adminFinanceProjects = projects || []; this.page.adminFinanceMetrics = this.toMetrics(response.summary || {});
        this.selectedIds = this.selectedIds.filter(id => this.rows.some(row => row.id === id));
        this.selectedId = this.rows.some(row => row.id === preferredId) ? preferredId : this.rows[0]?.id || null;
      } catch (error) { if (serial !== this.requestSerial) return; this.rows = []; this.totalRows = 0; this.page.adminFinanceProjects = []; this.page.adminFinanceMetrics = null; this.errorMessage = error.message || this.$t('legacy.t_9d56180a3aa0'); }
      finally { if (serial === this.requestSerial) this.loading = false; }
    },
    toMetrics(summary) { return [
       { label: this.$t('finance.pendingReceipts'), value: this.$t('finance.records', { count: Number(summary.pendingCount || 0) }), delta: `RM ${this.money(summary.pendingAmount)}`, trend: Number(summary.pendingCount) ? 'down' : 'up' },
       { label: this.$t('finance.confirmedReceipts'), value: this.$t('finance.records', { count: Number(summary.confirmedCount || 0) }), delta: this.$t('finance.propertyPaymentTotal'), trend: 'up' },
       { label: this.$t('finance.returnedProof'), value: this.$t('finance.records', { count: Number(summary.rejectedCount || 0) }), delta: this.$t('finance.awaitingResubmission'), trend: Number(summary.rejectedCount) ? 'down' : '' },
       { label: this.$t('finance.pendingSyncSql'), value: this.$t('finance.records', { count: Number(summary.pendingSyncCount || 0) }), delta: this.$t('finance.confirmedAwaitingSync'), trend: Number(summary.pendingSyncCount) ? 'down' : 'up' },
       { label: this.$t('finance.currentMonthConfirmed'), value: `RM ${this.money(summary.confirmedMonthAmount)}`, delta: this.$t('finance.monthCompleted'), trend: 'up' }
    ]; },
    switchFinanceType(type) { if (type === this.financeType) return; this.financeType = type; this.page.adminFinanceMode = type; this.page.projectFilter = '全部建案'; this.page.statusFilter = this.viewMode === 'history' ? '全部歷史' : this.pendingStatusLabel(type); this.page.globalSearch = ''; this.page.moduleSearch = ''; this.selectedIds = []; if (type === 'property') this.$nextTick(this.loadData); },
    switchViewMode(mode) { if (mode === this.viewMode) return; this.page.adminFinanceViewMode = mode; this.page.statusFilter = mode === 'pending' ? this.pendingStatusLabel(this.financeType) : '全部歷史'; this.selectedIds = []; },
    pendingStatusLabel(type) { return type === 'rent' ? '全部租金狀態' : '全部狀態'; },
    resetAndLoad() { if (this.financeType !== 'property') return; this.pageNumber = 1; this.selectedIds = []; this.loadData(); },
    async goToPage(page) { if (page < 1 || page > this.totalPages || page === this.pageNumber) return; this.pageNumber = page; await this.loadData(); },
    selectRow(row) { this.selectedId = row.id; },
    openDetails(row) { this.selectRow(row); this.$nextTick(() => this.$refs.detailDialog?.showModal()); },
    closeDetails() { this.$refs.detailDialog?.close(); },
    openProofFromDetails() { this.closeDetails(); this.$nextTick(this.openProof); },
    openDecisionFromDetails(mode) { this.closeDetails(); this.$nextTick(() => this.openDecision(mode)); },
    openReopenFromDetails() { this.closeDetails(); this.$nextTick(this.openReopen); },
    goToInstallmentFromDetails() { this.closeDetails(); this.goToInstallment(); },
    toggleSelectablePage(event) { const ids = this.selectablePageRows.map(row => row.id); this.selectedIds = event.target.checked ? [...new Set([...this.selectedIds, ...ids])] : this.selectedIds.filter(id => !ids.includes(id)); },
    selectAndOpenProof(row) { this.selectRow(row); this.$nextTick(this.openProof); },
     openDecision(mode) { this.decisionMode = mode; this.decisionDate = todayIsoDate(); this.decisionReceiptDate = todayIsoDate(); this.decisionNote = mode === 'confirm' ? this.$t('finance.reviewNoteDefault') : ''; this.actionError = ''; this.$refs.decisionDialog?.showModal(); },
    closeDecision() { this.$refs.decisionDialog?.close(); },
     async submitDecision() { if (!this.decisionNote || (this.decisionMode === 'confirm' && (!this.decisionDate || !this.decisionReceiptDate))) { this.actionError = this.$t('finance.reviewNoteRequired'); return; } this.actionSaving = true; this.actionError = ''; const id = this.selectedRow.id; try { if (this.decisionMode === 'confirm') await confirmAdminFinanceReview(id, this.decisionDate, this.decisionNote, this.decisionReceiptDate); else await rejectAdminFinanceReview(id, this.decisionNote); this.closeDecision(); await this.loadData(); this.page.showToast(this.decisionMode === 'confirm' ? this.$t('finance.collectionConfirmed') : this.$t('finance.proofReturned')); } catch (error) { this.actionError = error.message || this.$t('finance.reviewOperationFailed'); } finally { this.actionSaving = false; } },
     openReopen() { this.reopenNote = ''; this.actionError = ''; this.$refs.reopenDialog?.showModal(); },
     closeReopen() { this.$refs.reopenDialog?.close(); },
     async submitReopen() { if (!this.reopenNote) { this.actionError = this.$t('finance.reviewNoteRequired'); return; } this.actionSaving = true; this.actionError = ''; try { await reopenAdminFinanceReview(this.selectedRow.id, this.reopenNote); this.closeReopen(); await this.loadData(); this.page.showToast(this.$t('finance.reopenSuccess')); } catch (error) { this.actionError = error.message || this.$t('finance.reopenFailed'); } finally { this.actionSaving = false; } },
     openBatchReopen() { if (!this.selectedReopenIds.length) { this.page.showToast('请先勾选已确认记录'); return; } this.batchReopenNote = ''; this.actionError = ''; this.$refs.batchReopenDialog?.showModal(); },
     closeBatchReopen() { this.$refs.batchReopenDialog?.close(); },
     async submitBatchReopen() { if (!this.batchReopenNote) { this.actionError = '请填写退回原因'; return; } const ids = [...this.selectedReopenIds]; this.actionSaving = true; this.actionError = ''; try { await batchReopenAdminFinanceReviews(ids, this.batchReopenNote); this.closeBatchReopen(); this.selectedIds = []; await this.loadData(); this.page.showToast(`已批量退回 ${ids.length} 笔记录`); } catch (error) { this.actionError = error.message || '批量退回失败'; } finally { this.actionSaving = false; } },
     openBatch() { if (!this.selectedIds.length) this.selectedIds = this.pendingPageRows.map(row => row.id); if (!this.selectedIds.length) { this.page.showToast('当前列表没有可确认记录'); return; } this.batchDate = todayIsoDate(); this.batchReceiptDate = todayIsoDate(); this.batchNote = this.$t('finance.batchNoteDefault'); this.batchReference = ''; this.actionError = ''; this.$refs.batchDialog?.showModal(); },
    closeBatch() { this.$refs.batchDialog?.close(); },
     async submitBatch() { if (!this.batchNote || !this.batchDate || !this.batchReceiptDate) { this.actionError = this.$t('finance.batchNoteRequired'); return; } this.actionSaving = true; this.actionError = ''; const count = this.selectedIds.length; try { await batchConfirmAdminFinanceReviews(this.selectedIds, this.batchDate, this.batchNote, this.batchReference, this.batchReceiptDate); this.closeBatch(); this.selectedIds = []; await this.loadData(); this.page.showToast(this.$t('finance.batchConfirmed', { count })); } catch (error) { this.actionError = error.message || this.$t('finance.batchConfirmFailed'); } finally { this.actionSaving = false; } },
     displayDate(value) { return formatDate(value); },
     async openProof() { if (!this.selectedRow?.proofDocumentId) return; this.revokeProofUrl(); this.proofLoading = true; this.proofError = ''; this.$refs.proofDialog?.showModal(); try { const result = await fetchAdminFinanceProof(this.selectedRow.proofDocumentId); this.proofUrl = URL.createObjectURL(result.blob); } catch (error) { this.proofError = error.message || this.$t('finance.proofLoadFailed'); } finally { this.proofLoading = false; } },
    closeProof() { this.$refs.proofDialog?.close(); this.revokeProofUrl(); },
    revokeProofUrl() { if (this.proofUrl) URL.revokeObjectURL(this.proofUrl); this.proofUrl = ''; },
     async downloadProof() { if (!this.selectedRow?.proofDocumentId) return; try { const result = await fetchAdminFinanceProof(this.selectedRow.proofDocumentId, true); const url = URL.createObjectURL(result.blob); const link = document.createElement('a'); link.href = url; link.download = this.selectedRow.proofName || `payment-proof-${this.selectedRow.proofDocumentId}`; link.click(); URL.revokeObjectURL(url); } catch (error) { this.page.showToast(error.message || this.$t('finance.proofDownloadFailed')); } },
    goToInstallment() { const keyword = this.selectedRow?.receiptNo || this.selectedRow?.unitNo || ''; this.page.selectModule('adminData'); this.$nextTick(() => { this.page.globalSearch = keyword; }); },
    documentUrl(row, type) { return row?.id ? getAdminFinanceDocumentUrl(row.id, type) : '#'; },
    async batchDownloadDocuments(type) {
      if (this.batchDocumentBusy) return;
      if (this.financeType !== 'property') {
        const child = this.financeType === 'rent' ? this.$refs.rentFinanceHistory : this.financeType === 'reserve' ? this.$refs.reserveFinance : this.$refs.expenseFinance;
        if (child?.batchDownload) return child.batchDownload(type);
      }
       const ids = [...this.selectedIds];
       if (!ids.length) { this.page.showToast('请先勾选需要下载的历史记录'); return; }
      this.batchDocumentBusy = true;
      try {
        const result = await downloadAdminFinanceDocuments(ids, type);
        const url = URL.createObjectURL(result.blob); const link = document.createElement('a'); link.href = url; link.download = result.filename; link.click();
        window.setTimeout(() => URL.revokeObjectURL(url), 1000);
        this.page.showToast(`已生成 ${ids.length} 份${type === 'invoice' ? '发票' : '收据'}`);
      } catch (error) { this.page.showToast(error.message || '批量下载失败'); }
      finally { this.batchDocumentBusy = false; }
    },
    statusParam(value) { return ({ '待確認': 'pending', '已確認': 'confirmed', '已退回': 'rejected', '待同步': 'sync_pending', '同步失敗': 'sync_failed' })[value] || ''; },
    requestStatus() { if (this.viewMode === 'pending') return 'pending'; return this.statusParam(this.page.statusFilter) || 'history'; },
     confirmationLabel(value) { return ({ pending: this.$t('finance.pending'), confirmed: this.$t('finance.confirmed'), rejected: this.$t('finance.rejected') })[value] || this.$t('finance.unknownStatus'); },
    confirmationClass(value) { return value === 'confirmed' ? 'green' : value === 'rejected' ? 'red' : 'orange'; },
     syncLabel(value) { return ({ not_synced: this.$t('finance.notSynced'), pending: this.$t('finance.syncing'), synced: this.$t('finance.synced'), failed: this.$t('finance.syncFailed') })[value] || this.$t('finance.notSynced'); },
    syncClass(value) { return value === 'synced' ? 'green' : value === 'failed' ? 'red' : 'orange'; },
     paymentMethodLabel(value) { return ({ bank_transfer: this.$t('finance.bankTransfer'), cheque: this.$t('finance.cheque'), cash: this.$t('finance.cash'), online_banking: this.$t('finance.onlineBanking') })[value] || value || '—'; },
    formatDateTime(value) { return displayDateTime(value); },
    money(value) { return Number(value || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
  }
};
</script>

<style scoped>
.admin-finance-shell { display: grid; gap: 12px; }
.admin-finance-navigation { display: flex; align-items: center; gap: 12px; margin: 0 28px; }
.admin-finance-type-tabs {
  display: grid;
  grid-template-columns: repeat(7, minmax(124px, 1fr));
  width: min(calc(100% - 56px), 1320px);
  margin: 0;
  padding: 4px;
  border: 1px solid #d9e2ee;
  border-radius: 12px;
  background: #eef3f8;
  box-shadow: 0 2px 8px rgba(15, 45, 80, .06);
}
.admin-finance-type-tabs button {
  position: relative;
  display: grid;
  grid-template-columns: 34px auto;
  grid-template-rows: auto auto;
  column-gap: 10px;
  align-items: center;
  min-height: 50px;
  padding: 7px 12px;
  border: 0;
  border-radius: 9px;
  background: transparent;
  color: #334155;
  text-align: left;
  cursor: pointer;
  transition: background-color .18s ease, color .18s ease, box-shadow .18s ease, transform .18s ease;
}
.admin-finance-type-tabs button:hover:not(.active) { background: rgba(255,255,255,.72); color: #0b3768; }
.admin-finance-type-tabs button:focus-visible { outline: 2px solid #d99a00; outline-offset: 2px; }
.admin-finance-type-tabs button > span {
  grid-row: 1 / 3;
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: 1px solid #d7e2ef;
  border-radius: 10px;
  background: #fff;
  color: #174578;
  font-size: 13px;
  font-weight: 900;
  box-shadow: 0 1px 3px rgba(15,45,80,.06);
}
.admin-finance-type-tabs button > b { align-self: end; font-size: 13px; line-height: 1.15; letter-spacing: .01em; }
.admin-finance-type-tabs button > small { align-self: start; margin-top: 2px; color: #75849a; font-size: 10px; line-height: 1.15; }
.admin-finance-type-tabs button.active {
  background: linear-gradient(135deg, #0a3769 0%, #0d4c86 100%);
  color: #fff;
  box-shadow: 0 5px 12px rgba(10,55,105,.2);
}
.admin-finance-type-tabs button.active::after {
  content: '';
  position: absolute;
  right: 11px;
  top: 10px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #e0a100;
  box-shadow: 0 0 0 3px rgba(224,161,0,.18);
}
.admin-finance-type-tabs button.active > span { border-color: #e0a100; background: #e0a100; color: #fff; box-shadow: none; }
.admin-finance-type-tabs button.active > small { color: #cbd9e9; }
.finance-history-button { flex: 0 0 auto; min-height: 42px; padding: 0 18px; border: 1px solid #cbd8e7; border-radius: 9px; background: #fff; color: #123e6d; font-size: 13px; font-weight: 800; cursor: pointer; box-shadow: 0 2px 6px rgba(15,45,80,.05); }
.finance-history-button:hover { border-color: #0b4a83; background: #f4f8fc; }
.finance-history-button.back { display: inline-flex; align-items: center; gap: 7px; }
.finance-document-actions { display: grid; gap: 4px; min-width: 106px; }
.finance-document-actions a, .finance-document-button { display: inline-flex; align-items: center; justify-content: center; min-height: 26px; padding: 0 7px; border: 1px solid #b7cfee; border-radius: 6px; background: #f7fbff; color: #185a97; font-size: 10px; text-decoration: none; white-space: nowrap; }
.finance-document-actions a:hover, .finance-document-button:hover { background: #eaf3ff; border-color: #6f9fda; }
.finance-batch-actions { display: flex; gap: 8px; flex-wrap: wrap; margin-left: auto; }
.finance-batch-button { min-height: 36px; padding: 0 12px; border: 1px solid #d3a100; border-radius: 8px; background: linear-gradient(180deg,#f9c21b,#e9aa00); color: #fff; font-weight: 700; cursor: pointer; }
.finance-batch-button.receipt { border-color: #24588f; background: #fff; color: #185a97; }
.finance-batch-button:disabled { opacity: .55; cursor: not-allowed; }
.finance-list-actions { display: flex; align-items: center; justify-content: flex-end; gap: 8px; flex-wrap: wrap; }
.finance-list-batch-button { min-height: 34px; padding: 0 12px; border: 1px solid #9bc9d0; border-radius: 8px; background: #fff; color: #087078; font-size: 12px; font-weight: 700; cursor: pointer; }
.finance-list-batch-button:hover:not(:disabled) { background: #eff9f8; border-color: #55aaae; }
.finance-list-batch-button.danger { border-color: #efb6bb; background: #fff7f7; color: #bf2f3b; }
.finance-list-batch-button:disabled { opacity: .45; cursor: not-allowed; }
.finance-batch-overview{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}.finance-batch-overview span{display:grid;gap:5px;padding:13px;border:1px solid #cfe4e5;border-radius:9px;background:#f1faf9;color:#66788b;font-size:12px}.finance-batch-overview b{color:#073b67;font-size:18px}
@media (max-width: 720px) {
  .admin-finance-navigation { align-items: stretch; flex-direction: column; margin-inline: 14px; }
  .admin-finance-type-tabs { width: 100%; grid-template-columns: 1fr; }
  .admin-finance-type-tabs button { grid-template-columns: 30px auto; column-gap: 8px; padding-inline: 9px; }
  .admin-finance-type-tabs button > span { width: 30px; height: 30px; }
  .finance-history-button { width: 100%; }
}
</style>
