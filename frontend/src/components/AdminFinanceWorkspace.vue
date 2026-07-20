<template>
  <div class="admin-finance-shell">
    <div class="admin-finance-navigation">
      <nav class="admin-finance-type-tabs" aria-label="財務款項類型">
        <button type="button" :class="{ active: financeType === 'property' }" :aria-pressed="financeType === 'property'" @click="switchFinanceType('property')"><span>房</span><b>買房款</b><small>業主購房進度款</small></button>
        <button type="button" :class="{ active: financeType === 'rent' }" :aria-pressed="financeType === 'rent'" @click="switchFinanceType('rent')"><span>租</span><b>租金</b><small>租客每月租金</small></button>
        <button type="button" :class="{ active: financeType === 'reserve' }" :aria-pressed="financeType === 'reserve'" @click="switchFinanceType('reserve')"><span>備</span><b>預備金充值</b><small>確認後增加賬戶餘額</small></button>
      </nav>
      <button v-if="viewMode === 'pending'" type="button" class="finance-history-button" @click="switchViewMode('history')">歷史記錄</button>
      <button v-else type="button" class="finance-history-button back" @click="switchViewMode('pending')"><span aria-hidden="true">←</span> 返回財務確認</button>
    </div>
  <section v-if="financeType === 'property'" class="content-grid admin-finance-workspace">
    <div class="panel table-panel finance-review-panel">
      <div class="panel-head">
        <div><h2>{{ viewMode === 'pending' ? '待確認房款列表' : '房款確認歷史' }}</h2><span>{{ totalRows }} records</span></div>
        <div class="finance-list-actions"><span v-if="selectedIds.length">已選 {{ selectedIds.length }} 筆</span><span v-if="loading">正在載入…</span></div>
      </div>

      <div v-if="errorMessage" class="admin-owner-state error" role="alert">
        <strong>財務確認資料載入失敗</strong><span>{{ errorMessage }}</span><button type="button" @click="loadData">重新載入</button>
      </div>
      <div v-else class="table-wrap finance-review-table-wrap">
        <table>
          <thead><tr><th class="finance-check-cell"><input type="checkbox" :checked="allPendingSelected" :disabled="!pendingPageRows.length" aria-label="選擇本頁待確認交易" @change="togglePendingPage"></th><th>交易編號</th><th>建案／單位</th><th>付款人</th><th>提交金額</th><th>付款日期</th><th>憑證</th><th>確認狀態</th><th>同步狀態</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id" :class="{ selected: row.id === selectedId }" @click="selectRow(row)">
              <td class="finance-check-cell"><input v-if="row.confirmationStatus === 'pending'" v-model="selectedIds" type="checkbox" :value="row.id" :aria-label="`選擇 ${row.transactionNo}`" @click.stop></td>
              <td><strong>{{ row.transactionNo }}</strong><small>{{ row.receiptNo || '尚無收據編號' }}</small></td>
              <td>{{ row.projectName }}<small>{{ row.unitNo }} · 第 {{ row.installmentNo }} 期</small></td>
              <td>{{ row.payerName || '—' }}</td>
              <td><b>{{ row.currency }} {{ money(row.amount) }}</b></td>
              <td>{{ row.transactionDate || '—' }}</td>
              <td><button type="button" class="finance-proof-link" :disabled="!row.proofDocumentId" @click.stop="selectAndOpenProof(row)">{{ row.proofDocumentId ? '查看憑證' : '缺少憑證' }}</button></td>
              <td><span class="tag" :class="confirmationClass(row.confirmationStatus)">{{ confirmationLabel(row.confirmationStatus) }}</span></td>
              <td><span class="tag" :class="syncClass(row.syncStatus)">{{ syncLabel(row.syncStatus) }}</span></td>
              <td><button type="button" class="row-actions" title="查看詳情" @click.stop="selectRow(row)">…</button></td>
            </tr>
            <tr v-if="!loading && !rows.length"><td colspan="10" class="admin-owner-empty">目前沒有符合條件的房款審核記錄</td></tr>
          </tbody>
        </table>
      </div>
      <div class="pager"><span>共 {{ totalRows }} records</span><div class="admin-building-pager"><button type="button" :disabled="pageNumber <= 1 || loading" @click="goToPage(pageNumber - 1)">&lt;</button><button v-for="number in visiblePages" :key="number" type="button" :class="{ active: number === pageNumber }" :disabled="loading" @click="goToPage(number)">{{ number }}</button><button type="button" :disabled="pageNumber >= totalPages || loading" @click="goToPage(pageNumber + 1)">&gt;</button><select v-model.number="pageSize" :disabled="loading" aria-label="每頁記錄數"><option :value="10">10 條/頁</option><option :value="20">20 條/頁</option><option :value="50">50 條/頁</option></select></div></div>
    </div>

    <aside class="panel detail-panel finance-review-detail">
      <div v-if="selectedRow" class="detail-card">
        <div class="profile finance-review-profile">
          <div class="big-avatar">財</div>
          <div class="finance-review-profile-copy">
            <h3 :title="selectedRow.transactionNo">{{ selectedRow.transactionNo }}</h3>
            <p>{{ selectedRow.projectName }}<span>·</span>{{ selectedRow.unitNo }}</p>
          </div>
          <span class="tag finance-review-status" :class="confirmationClass(selectedRow.confirmationStatus)">{{ confirmationLabel(selectedRow.confirmationStatus) }}</span>
        </div>
        <div class="detail-actions finance-review-actions">
          <button :disabled="!selectedRow.proofDocumentId" @click="openProof">查看付款憑證</button>
          <button v-if="selectedRow.confirmationStatus === 'pending'" class="confirm" @click="openDecision('confirm')">確認收款</button>
          <button v-if="selectedRow.confirmationStatus === 'pending'" class="reject" @click="openDecision('reject')">退回補件</button>
          <button @click="goToInstallment">前往關聯房款</button>
        </div>
        <div class="detail-section"><h4><span class="num">1</span>付款資訊</h4><div class="kv"><span>付款人</span><b>{{ selectedRow.payerName || '—' }}</b></div><div class="kv"><span>付款方式</span><b>{{ paymentMethodLabel(selectedRow.paymentMethod) }}</b></div><div class="kv"><span>銀行參考</span><b>{{ selectedRow.bankReference || '—' }}</b></div><div class="kv"><span>提交金額</span><b>{{ selectedRow.currency }} {{ money(selectedRow.amount) }}</b></div><div class="kv"><span>付款日期</span><b>{{ selectedRow.transactionDate || '—' }}</b></div></div>
        <div class="detail-section"><h4><span class="num">2</span>關聯房款</h4><div class="kv"><span>工程階段</span><b>{{ selectedRow.installmentNo }}. {{ selectedRow.milestone || '付款期數' }}</b></div><div class="kv"><span>本期應收</span><b>RM {{ money(selectedRow.installmentAmount) }}</b></div><div class="kv"><span>已確認收款</span><b>RM {{ money(selectedRow.installmentPaid) }}</b></div><div class="kv"><span>本次分配</span><b>RM {{ money(selectedRow.allocatedAmount) }}</b></div><div class="progress"><i :style="{ width: installmentProgress + '%' }"></i></div></div>
        <div class="detail-section"><h4><span class="num">3</span>審核與同步</h4><div class="kv"><span>確認狀態</span><b>{{ confirmationLabel(selectedRow.confirmationStatus) }}</b></div><div class="kv"><span>審核人</span><b>{{ selectedRow.confirmedByName || '—' }}</b></div><div class="kv"><span>審核時間</span><b>{{ formatDateTime(selectedRow.confirmedAt) }}</b></div><div class="kv"><span>審核備註</span><b>{{ selectedRow.reviewNote || '—' }}</b></div><div class="kv"><span>SQL 同步</span><b>{{ syncLabel(selectedRow.syncStatus) }}</b></div></div>
      </div>
      <div v-else class="admin-owner-empty">目前沒有可查看的財務記錄</div>
    </aside>

    <dialog ref="decisionDialog" class="modal admin-finance-decision-dialog">
      <form method="dialog" @submit.prevent="submitDecision">
        <div class="modal-head"><div><h3>{{ decisionMode === 'confirm' ? '確認收款' : '退回補件' }}</h3><small>{{ selectedRow?.transactionNo }} · {{ selectedRow?.currency }} {{ money(selectedRow?.amount) }}</small></div><button type="button" class="icon-close" @click="closeDecision">×</button></div>
        <div class="finance-decision-body">
          <div v-if="decisionMode === 'confirm'" class="finance-decision-warning success"><strong>確認後將立即入帳</strong><span>本次金額會累加至第 {{ selectedRow?.installmentNo }} 期房款，並進入待同步會計狀態。</span></div>
          <div v-else class="finance-decision-warning"><strong>業主需要重新提交憑證</strong><span>本次金額不會計入已收款，退回原因會發送至業主通知中心。</span></div>
          <label>審核備註<textarea v-model.trim="decisionNote" maxlength="500" required :placeholder="decisionMode === 'confirm' ? '填寫銀行入賬核對結果' : '說明需要補充或修正的內容'"></textarea></label>
          <p v-if="actionError" class="admin-property-error">{{ actionError }}</p>
        </div>
        <menu><button type="button" @click="closeDecision">取消</button><button type="submit" class="primary-btn" :class="{ reject: decisionMode === 'reject' }" :disabled="actionSaving">{{ actionSaving ? '處理中…' : decisionMode === 'confirm' ? '確認並入帳' : '確認退回' }}</button></menu>
      </form>
    </dialog>

    <dialog ref="batchDialog" class="modal admin-finance-decision-dialog">
      <form method="dialog" @submit.prevent="submitBatch">
        <div class="modal-head"><div><h3>批量確認收款</h3><small>已選擇 {{ selectedIds.length }} 筆待確認交易</small></div><button type="button" class="icon-close" @click="closeBatch">×</button></div>
        <div class="finance-decision-body"><div class="finance-decision-warning success"><strong>整批交易會以同一事務確認</strong><span>任何一筆資料校驗失敗，整批都不會入帳。</span></div><label>批量審核備註<textarea v-model.trim="batchNote" maxlength="500" required></textarea></label><p v-if="actionError" class="admin-property-error">{{ actionError }}</p></div>
        <menu><button type="button" @click="closeBatch">取消</button><button type="submit" class="primary-btn" :disabled="actionSaving">{{ actionSaving ? '確認中…' : `確認 ${selectedIds.length} 筆收款` }}</button></menu>
      </form>
    </dialog>

    <dialog ref="proofDialog" class="modal admin-finance-proof-dialog">
      <div class="modal-head"><div><h3>付款憑證</h3><small>{{ selectedRow?.proofName || selectedRow?.transactionNo }}</small></div><button type="button" class="icon-close" @click="closeProof">×</button></div>
      <div class="finance-proof-viewer"><div v-if="proofLoading" class="admin-owner-state">正在載入付款憑證…</div><img v-else-if="proofUrl && selectedRow?.proofMimeType?.startsWith('image/')" :src="proofUrl" alt="付款憑證"><iframe v-else-if="proofUrl && selectedRow?.proofMimeType === 'application/pdf'" :src="proofUrl" title="付款憑證 PDF"></iframe><div v-else class="admin-owner-state">{{ proofError || '無法預覽此憑證格式' }}</div></div>
      <menu><button type="button" @click="closeProof">關閉</button><button type="button" class="primary-btn" :disabled="!selectedRow?.proofDocumentId" @click="downloadProof">下載原始文件</button></menu>
    </dialog>
  </section>
  <AdminRentCollectionWorkspace v-else-if="financeType === 'rent' && viewMode === 'pending'" ref="rentFinance" />
  <AdminRentFinanceWorkspace v-else-if="financeType === 'rent'" ref="rentFinanceHistory" />
  <AdminReserveFinanceWorkspace v-else />
  </div>
</template>

<script>
import { batchConfirmAdminFinanceReviews, confirmAdminFinanceReview, fetchAdminFinanceProjects, fetchAdminFinanceProof, fetchAdminFinanceReviews, rejectAdminFinanceReview } from '../services/propertyApi';
import AdminRentCollectionWorkspace from './AdminRentCollectionWorkspace.vue';
import AdminRentFinanceWorkspace from './AdminRentFinanceWorkspace.vue';
import AdminReserveFinanceWorkspace from './AdminReserveFinanceWorkspace.vue';

export default {
  components: { AdminRentCollectionWorkspace, AdminRentFinanceWorkspace, AdminReserveFinanceWorkspace },
  inject: ['page'],
  data() {
    return {
      financeType: 'property', rows: [], selectedId: null, selectedIds: [], loading: false, errorMessage: '', requestSerial: 0,
      pageNumber: 1, pageSize: 10, totalRows: 0, totalPages: 1,
      decisionMode: 'confirm', decisionNote: '', batchNote: '批量核對付款憑證與銀行入賬資料一致', actionSaving: false, actionError: '',
      proofLoading: false, proofError: '', proofUrl: ''
    };
  },
  computed: {
    selectedRow() { return this.rows.find(row => row.id === this.selectedId) || this.rows[0] || null; },
    viewMode() { return this.page.adminFinanceViewMode === 'history' ? 'history' : 'pending'; },
    pendingPageRows() { return this.rows.filter(row => row.confirmationStatus === 'pending'); },
    allPendingSelected() { return this.pendingPageRows.length > 0 && this.pendingPageRows.every(row => this.selectedIds.includes(row.id)); },
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
    this.financeType = ['property', 'rent', 'reserve'].includes(this.page.adminFinanceMode) ? this.page.adminFinanceMode : 'property';
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
        this.selectedIds = this.selectedIds.filter(id => this.rows.some(row => row.id === id && row.confirmationStatus === 'pending'));
        this.selectedId = this.rows.some(row => row.id === preferredId) ? preferredId : this.rows[0]?.id || null;
      } catch (error) { if (serial !== this.requestSerial) return; this.rows = []; this.totalRows = 0; this.page.adminFinanceProjects = []; this.page.adminFinanceMetrics = null; this.errorMessage = error.message || 'API request failed'; }
      finally { if (serial === this.requestSerial) this.loading = false; }
    },
    toMetrics(summary) { return [
      { label: '待確認收款', value: `${Number(summary.pendingCount || 0)} 筆`, delta: `RM ${this.money(summary.pendingAmount)}`, trend: Number(summary.pendingCount) ? 'down' : 'up' },
      { label: '已確認收款', value: `${Number(summary.confirmedCount || 0)} 筆`, delta: '房款累計', trend: 'up' },
      { label: '已退回補件', value: `${Number(summary.rejectedCount || 0)} 筆`, delta: '等待重新提交', trend: Number(summary.rejectedCount) ? 'down' : '' },
      { label: '待同步 SQL', value: `${Number(summary.pendingSyncCount || 0)} 筆`, delta: '已確認待同步', trend: Number(summary.pendingSyncCount) ? 'down' : 'up' },
      { label: '本月確認金額', value: `RM ${this.money(summary.confirmedMonthAmount)}`, delta: '本月完成', trend: 'up' }
    ]; },
    switchFinanceType(type) { if (type === this.financeType) return; this.financeType = type; this.page.adminFinanceMode = type; this.page.adminFinanceViewMode = 'pending'; this.page.projectFilter = '全部建案'; this.page.statusFilter = this.pendingStatusLabel(type); this.page.globalSearch = ''; this.page.moduleSearch = ''; this.selectedIds = []; if (type === 'property') this.$nextTick(this.loadData); },
    switchViewMode(mode) { if (mode === this.viewMode) return; this.page.adminFinanceViewMode = mode; this.page.statusFilter = mode === 'pending' ? this.pendingStatusLabel(this.financeType) : '全部歷史'; this.selectedIds = []; },
    pendingStatusLabel(type) { return type === 'rent' ? '全部租金狀態' : '全部狀態'; },
    resetAndLoad() { if (this.financeType !== 'property') return; this.pageNumber = 1; this.selectedIds = []; this.loadData(); },
    async goToPage(page) { if (page < 1 || page > this.totalPages || page === this.pageNumber) return; this.pageNumber = page; await this.loadData(); },
    selectRow(row) { this.selectedId = row.id; },
    togglePendingPage(event) { const ids = this.pendingPageRows.map(row => row.id); this.selectedIds = event.target.checked ? [...new Set([...this.selectedIds, ...ids])] : this.selectedIds.filter(id => !ids.includes(id)); },
    selectAndOpenProof(row) { this.selectRow(row); this.$nextTick(this.openProof); },
    openDecision(mode) { this.decisionMode = mode; this.decisionNote = mode === 'confirm' ? '銀行入賬與付款憑證核對一致' : ''; this.actionError = ''; this.$refs.decisionDialog?.showModal(); },
    closeDecision() { this.$refs.decisionDialog?.close(); },
    async submitDecision() { if (!this.decisionNote) { this.actionError = '請填寫審核備註'; return; } this.actionSaving = true; this.actionError = ''; const id = this.selectedRow.id; try { if (this.decisionMode === 'confirm') await confirmAdminFinanceReview(id, this.decisionNote); else await rejectAdminFinanceReview(id, this.decisionNote); this.closeDecision(); await this.loadData(); this.page.showToast(this.decisionMode === 'confirm' ? '收款已確認並計入房款' : '憑證已退回業主補件'); } catch (error) { this.actionError = error.message || '審核操作失敗'; } finally { this.actionSaving = false; } },
    openBatch() { if (!this.selectedIds.length) { this.page.showToast('請先勾選待確認交易'); return; } this.batchNote = '批量核對付款憑證與銀行入賬資料一致'; this.actionError = ''; this.$refs.batchDialog?.showModal(); },
    closeBatch() { this.$refs.batchDialog?.close(); },
    async submitBatch() { if (!this.batchNote) { this.actionError = '請填寫批量審核備註'; return; } this.actionSaving = true; this.actionError = ''; const count = this.selectedIds.length; try { await batchConfirmAdminFinanceReviews(this.selectedIds, this.batchNote); this.closeBatch(); this.selectedIds = []; await this.loadData(); this.page.showToast(`已確認 ${count} 筆收款`); } catch (error) { this.actionError = error.message || '批量確認失敗'; } finally { this.actionSaving = false; } },
    async openProof() { if (!this.selectedRow?.proofDocumentId) return; this.revokeProofUrl(); this.proofLoading = true; this.proofError = ''; this.$refs.proofDialog?.showModal(); try { const result = await fetchAdminFinanceProof(this.selectedRow.proofDocumentId); this.proofUrl = URL.createObjectURL(result.blob); } catch (error) { this.proofError = error.message || '付款憑證載入失敗'; } finally { this.proofLoading = false; } },
    closeProof() { this.$refs.proofDialog?.close(); this.revokeProofUrl(); },
    revokeProofUrl() { if (this.proofUrl) URL.revokeObjectURL(this.proofUrl); this.proofUrl = ''; },
    async downloadProof() { if (!this.selectedRow?.proofDocumentId) return; try { const result = await fetchAdminFinanceProof(this.selectedRow.proofDocumentId, true); const url = URL.createObjectURL(result.blob); const link = document.createElement('a'); link.href = url; link.download = this.selectedRow.proofName || `payment-proof-${this.selectedRow.proofDocumentId}`; link.click(); URL.revokeObjectURL(url); } catch (error) { this.page.showToast(error.message || '憑證下載失敗'); } },
    goToInstallment() { const keyword = this.selectedRow?.receiptNo || this.selectedRow?.unitNo || ''; this.page.selectModule('adminData'); this.$nextTick(() => { this.page.globalSearch = keyword; }); },
    statusParam(value) { return ({ '待確認': 'pending', '已確認': 'confirmed', '已退回': 'rejected', '待同步': 'sync_pending', '同步失敗': 'sync_failed' })[value] || ''; },
    requestStatus() { if (this.viewMode === 'pending') return 'pending'; return this.statusParam(this.page.statusFilter) || 'history'; },
    confirmationLabel(value) { return ({ pending: '待確認', confirmed: '已確認', rejected: '已退回' })[value] || '未知'; },
    confirmationClass(value) { return value === 'confirmed' ? 'green' : value === 'rejected' ? 'red' : 'orange'; },
    syncLabel(value) { return ({ not_synced: '未同步', pending: '待同步', synced: '已同步', failed: '同步失敗' })[value] || '未同步'; },
    syncClass(value) { return value === 'synced' ? 'green' : value === 'failed' ? 'red' : 'orange'; },
    paymentMethodLabel(value) { return ({ bank_transfer: '銀行轉帳', cheque: '支票', cash: '現金', online_banking: '網上銀行' })[value] || value || '—'; },
    formatDateTime(value) { return value ? String(value).replace('T', ' ').slice(0, 19) : '—'; },
    money(value) { return Number(value || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
  }
};
</script>

<style scoped>
.admin-finance-shell { display: grid; gap: 12px; }
.admin-finance-navigation { display: flex; align-items: center; gap: 12px; margin: 0 28px; }
.admin-finance-type-tabs {
  display: grid;
  grid-template-columns: repeat(3, minmax(180px, 1fr));
  width: min(calc(100% - 56px), 800px);
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
@media (max-width: 720px) {
  .admin-finance-navigation { align-items: stretch; flex-direction: column; margin-inline: 14px; }
  .admin-finance-type-tabs { width: 100%; grid-template-columns: 1fr; }
  .admin-finance-type-tabs button { grid-template-columns: 30px auto; column-gap: 8px; padding-inline: 9px; }
  .admin-finance-type-tabs button > span { width: 30px; height: 30px; }
  .finance-history-button { width: 100%; }
}
</style>
