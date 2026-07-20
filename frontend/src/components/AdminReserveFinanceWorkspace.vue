<template>
  <section class="content-grid admin-finance-workspace admin-reserve-finance-workspace">
    <div class="panel table-panel finance-review-panel">
      <div class="panel-head"><div><h2>{{ viewMode === 'pending' ? '待確認預備金充值' : '預備金充值歷史' }}</h2><span>{{ totalRows }} records · {{ viewMode === 'pending' ? '確認後才增加賬戶餘額' : '已完成或已退回的記錄' }}</span></div><div class="finance-list-actions"><span v-if="selectedIds.length">已選 {{ selectedIds.length }} 筆</span><span v-if="loading">正在載入…</span></div></div>
      <div v-if="errorMessage" class="admin-owner-state error"><strong>預備金充值資料載入失敗</strong><span>{{ errorMessage }}</span><button @click="loadData">重新載入</button></div>
      <div v-else class="table-wrap finance-review-table-wrap">
        <table>
          <thead><tr><th class="finance-check-cell"><input type="checkbox" :checked="allPendingSelected" :disabled="!pendingRows.length" @change="togglePendingPage"></th><th>交易編號</th><th>建案／單位</th><th>付款人</th><th>充值金額</th><th>付款日期</th><th>付款憑證</th><th>確認狀態</th><th>SQL 同步</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id" :class="{ selected: row.id === selectedId }" @click="selectRow(row)">
              <td class="finance-check-cell"><input v-if="row.confirmationStatus === 'pending'" v-model="selectedIds" type="checkbox" :value="row.id" @click.stop></td>
              <td><strong>{{ row.transactionNo }}</strong><small>{{ row.receiptNo || '充值申請' }}</small></td>
              <td>{{ row.projectName }}<small>{{ row.unitNo }}</small></td>
              <td>{{ row.payerName || '—' }}<small>{{ row.bankReference || '—' }}</small></td>
              <td><b>{{ row.currency || 'MYR' }} {{ money(row.amount) }}</b></td>
              <td>{{ row.transactionDate || '—' }}</td>
              <td><button class="finance-proof-link" :disabled="!row.proofDocumentId" @click.stop="selectAndOpenProof(row)">{{ row.proofDocumentId ? '查看憑證' : '缺少憑證' }}</button></td>
              <td><span class="tag" :class="confirmationClass(row.confirmationStatus)">{{ confirmationLabel(row.confirmationStatus) }}</span></td>
              <td><span class="tag" :class="syncClass(row.syncStatus)">{{ syncLabel(row.syncStatus) }}</span></td>
              <td><button class="row-actions" @click.stop="selectRow(row)">…</button></td>
            </tr>
            <tr v-if="!loading && !rows.length"><td colspan="10" class="admin-owner-empty">目前沒有符合條件的預備金充值申請</td></tr>
          </tbody>
        </table>
      </div>
      <div class="pager"><span>共 {{ totalRows }} records</span><div class="admin-building-pager"><button :disabled="pageNumber <= 1" @click="goPage(pageNumber - 1)">&lt;</button><button v-for="n in visiblePages" :key="n" :class="{ active: n === pageNumber }" @click="goPage(n)">{{ n }}</button><button :disabled="pageNumber >= totalPages" @click="goPage(pageNumber + 1)">&gt;</button><select v-model.number="pageSize"><option :value="10">10 條/頁</option><option :value="20">20 條/頁</option><option :value="50">50 條/頁</option></select></div></div>
    </div>

    <aside class="panel detail-panel finance-review-detail">
      <div v-if="selectedRow" class="detail-card">
        <div class="profile finance-review-profile"><div class="big-avatar">備</div><div class="finance-review-profile-copy"><h3>{{ selectedRow.transactionNo }}</h3><p>{{ selectedRow.projectName }} · {{ selectedRow.unitNo }}</p></div><span class="tag" :class="confirmationClass(selectedRow.confirmationStatus)">{{ confirmationLabel(selectedRow.confirmationStatus) }}</span></div>
        <div class="detail-actions finance-review-actions">
          <button :disabled="!selectedRow.proofDocumentId" @click="openProof">查看付款憑證</button>
          <button v-if="selectedRow.confirmationStatus === 'pending'" class="confirm" @click="openDecision(true)">確認充值</button>
          <button v-if="selectedRow.confirmationStatus === 'pending'" class="reject" @click="openDecision(false)">退回申請</button>
        </div>
        <div class="detail-section"><h4><span class="num">1</span>充值資料</h4><div class="kv"><span>付款人</span><b>{{ selectedRow.payerName || '—' }}</b></div><div class="kv"><span>付款方式</span><b>{{ paymentMethodLabel(selectedRow.paymentMethod) }}</b></div><div class="kv"><span>銀行參考</span><b>{{ selectedRow.bankReference || '—' }}</b></div><div class="kv"><span>充值金額</span><b>{{ selectedRow.currency }} {{ money(selectedRow.amount) }}</b></div></div>
        <div class="detail-section"><h4><span class="num">2</span>預備金賬戶</h4><div class="kv"><span>當前餘額</span><b>RM {{ money(selectedRow.accountBalance) }}</b></div><div class="kv"><span>最低標準</span><b>RM {{ money(selectedRow.accountMinimumBalance) }}</b></div><div class="kv"><span>確認後餘額</span><b class="reserve-after">RM {{ money(Number(selectedRow.accountBalance || 0) + (selectedRow.confirmationStatus === 'pending' ? Number(selectedRow.amount || 0) : 0)) }}</b></div></div>
        <div class="detail-section"><h4><span class="num">3</span>審核資料</h4><div class="kv"><span>憑證</span><b>{{ selectedRow.proofName || '缺少憑證' }}</b></div><div class="kv"><span>審核人</span><b>{{ selectedRow.confirmedByName || '—' }}</b></div><div class="kv"><span>審核時間</span><b>{{ dateTime(selectedRow.confirmedAt) }}</b></div><div class="kv"><span>審核備註</span><b>{{ selectedRow.reviewNote || '—' }}</b></div></div>
      </div>
      <div v-else class="admin-owner-empty">目前沒有可查看的充值申請</div>
    </aside>

    <dialog ref="decisionDialog" class="modal admin-finance-decision-dialog"><form method="dialog" @submit.prevent="submitDecision"><div class="modal-head"><div><h3>{{ approving ? '確認預備金充值' : '退回充值申請' }}</h3><small>{{ selectedRow?.transactionNo }} · RM {{ money(selectedRow?.amount) }}</small></div><button type="button" class="icon-close" @click="closeDecision">×</button></div><div class="finance-decision-body"><div :class="['finance-decision-warning', { success: approving }]"><strong>{{ approving ? '確認後將立即增加預備金餘額' : '本次充值不會入賬' }}</strong><span>{{ approving ? `賬戶餘額將增加 RM ${money(selectedRow?.amount)}` : '退回原因會保留在充值申請中，業主可重新提交。' }}</span></div><label>審核備註<textarea v-model.trim="decisionNote" maxlength="500" required :placeholder="approving ? '填寫銀行入賬核對結果' : '填寫退回原因'"></textarea></label><p v-if="actionError" class="admin-property-error">{{ actionError }}</p></div><menu><button type="button" @click="closeDecision">取消</button><button class="primary-btn" :class="{ reject: !approving }" :disabled="actionSaving">{{ actionSaving ? '處理中…' : approving ? '確認並入賬' : '確認退回' }}</button></menu></form></dialog>

    <dialog ref="batchDialog" class="modal admin-finance-decision-dialog"><form method="dialog" @submit.prevent="submitBatch"><div class="modal-head"><div><h3>批量確認預備金充值</h3><small>已選擇 {{ selectedIds.length }} 筆申請</small></div><button type="button" class="icon-close" @click="closeBatch">×</button></div><div class="finance-decision-body"><div class="finance-decision-warning success"><strong>整批交易將在同一事務入賬</strong><span>任何一筆校驗失敗，整批都不會修改預備金餘額。</span></div><label>批量審核備註<textarea v-model.trim="batchNote" maxlength="500" required></textarea></label><p v-if="actionError" class="admin-property-error">{{ actionError }}</p></div><menu><button type="button" @click="closeBatch">取消</button><button class="primary-btn" :disabled="actionSaving">{{ actionSaving ? '確認中…' : `確認 ${selectedIds.length} 筆充值` }}</button></menu></form></dialog>

    <dialog ref="proofDialog" class="modal admin-finance-proof-dialog"><div class="modal-head"><div><h3>預備金充值憑證</h3><small>{{ selectedRow?.proofName || selectedRow?.transactionNo }}</small></div><button type="button" class="icon-close" @click="closeProof">×</button></div><div class="finance-proof-viewer"><div v-if="proofLoading" class="admin-owner-state">正在載入憑證…</div><img v-else-if="proofUrl && selectedRow?.proofMimeType?.startsWith('image/')" :src="proofUrl" alt="預備金充值憑證"><iframe v-else-if="proofUrl && selectedRow?.proofMimeType === 'application/pdf'" :src="proofUrl" title="預備金充值憑證 PDF"></iframe><div v-else class="admin-owner-state">{{ proofError || '無法預覽此憑證格式' }}</div></div><menu><button @click="closeProof">關閉</button><button class="primary-btn" :disabled="!selectedRow?.proofDocumentId" @click="downloadProof">下載原始文件</button></menu></dialog>
  </section>
</template>

<script>
import { batchConfirmAdminReserveTopups, fetchAdminFinanceProjects, fetchAdminFinanceReviews, fetchAdminReserveProof, reviewAdminReserveTopup } from '../services/propertyApi';

export default {
  inject: ['page'],
  data() { return { rows: [], selectedId: null, selectedIds: [], pageNumber: 1, pageSize: 10, totalRows: 0, totalPages: 1, loading: false, errorMessage: '', serial: 0, approving: true, decisionNote: '', batchNote: '銀行入賬與充值憑證核對一致', actionSaving: false, actionError: '', proofLoading: false, proofError: '', proofUrl: '' }; },
  computed: {
    selectedRow() { return this.rows.find(row => row.id === this.selectedId) || this.rows[0] || null; },
    viewMode() { return this.page.adminFinanceViewMode === 'history' ? 'history' : 'pending'; },
    pendingRows() { return this.rows.filter(row => row.confirmationStatus === 'pending'); },
    allPendingSelected() { return this.pendingRows.length > 0 && this.pendingRows.every(row => this.selectedIds.includes(row.id)); },
    visiblePages() { const start = Math.max(1, Math.min(this.pageNumber - 2, this.totalPages - 4)); return Array.from({ length: Math.min(5, this.totalPages) }, (_, i) => start + i); },
    batchNonce() { return this.page.adminFinanceBatchNonce; }, refreshNonce() { return this.page.adminFinanceRefreshNonce; }
  },
  watch: {
    'page.moduleSearch'() { this.resetLoad(); }, 'page.globalSearch'() { this.resetLoad(); }, 'page.projectFilter'() { this.resetLoad(); }, 'page.statusFilter'() { this.resetLoad(); }, 'page.adminFinanceViewMode'() { this.resetLoad(); }, 'page.dateStart'() { this.resetLoad(); }, 'page.dateEnd'() { this.resetLoad(); }, pageSize() { this.resetLoad(); },
    batchNonce(v, old) { if (v > old) this.openBatch(); }, refreshNonce(v, old) { if (v > old) this.loadData(); },
    rows: { deep: true, handler(rows) { if (!rows.some(row => row.id === this.selectedId)) this.selectedId = rows[0]?.id || null; } }
  },
  mounted() { this.loadData(); }, beforeUnmount() { this.revokeProof(); },
  methods: {
    async loadData() { const serial = ++this.serial; this.loading = true; this.errorMessage = ''; try { const params = { type: 'reserve', page: this.pageNumber, pageSize: this.pageSize, keyword: this.page.globalSearch || this.page.moduleSearch || '', projectName: String(this.page.projectFilter || '').includes('全部') ? '' : this.page.projectFilter, status: this.requestStatus(), startDate: this.page.dateStart, endDate: this.page.dateEnd }; const [response, projects] = await Promise.all([fetchAdminFinanceReviews(params), fetchAdminFinanceProjects('reserve')]); if (serial !== this.serial) return; this.rows = response.rows || []; this.totalRows = response.page?.totalRows || 0; this.totalPages = response.page?.totalPages || 1; this.pageNumber = response.page?.page || 1; this.page.adminFinanceProjects = projects || []; this.page.adminFinanceMetrics = this.metrics(response.summary || {}); this.selectedIds = this.selectedIds.filter(id => this.pendingRows.some(row => row.id === id)); } catch (error) { if (serial !== this.serial) return; this.rows = []; this.errorMessage = error.message || 'API request failed'; this.page.adminFinanceMetrics = null; } finally { if (serial === this.serial) this.loading = false; } },
    metrics(s) { return [{ label: '待確認充值', value: `${Number(s.pendingCount || 0)} 筆`, delta: `RM ${this.money(s.pendingAmount)}`, trend: Number(s.pendingCount) ? 'down' : 'up' }, { label: '已確認充值', value: `${Number(s.confirmedCount || 0)} 筆`, delta: '已增加預備金', trend: 'up' }, { label: '已退回申請', value: `${Number(s.rejectedCount || 0)} 筆`, delta: '保留退回原因', trend: Number(s.rejectedCount) ? 'down' : 'up' }, { label: '待同步 SQL', value: `${Number(s.pendingSyncCount || 0)} 筆`, delta: '會計同步狀態', trend: Number(s.pendingSyncCount) ? 'down' : 'up' }, { label: '本月確認充值', value: `RM ${this.money(s.confirmedMonthAmount)}`, delta: '本月實際入賬', trend: 'up' }]; },
    resetLoad() { this.pageNumber = 1; this.selectedIds = []; this.loadData(); }, goPage(n) { if (n >= 1 && n <= this.totalPages && n !== this.pageNumber) { this.pageNumber = n; this.loadData(); } }, selectRow(row) { this.selectedId = row.id; },
    togglePendingPage(event) { const ids = this.pendingRows.map(row => row.id); this.selectedIds = event.target.checked ? [...new Set([...this.selectedIds, ...ids])] : this.selectedIds.filter(id => !ids.includes(id)); },
    openDecision(approved) { this.approving = approved; this.decisionNote = approved ? '銀行入賬與充值憑證核對一致' : ''; this.actionError = ''; this.$refs.decisionDialog?.showModal(); }, closeDecision() { this.$refs.decisionDialog?.close(); },
    async submitDecision() { if (!this.decisionNote) { this.actionError = '請填寫審核備註'; return; } this.actionSaving = true; this.actionError = ''; try { await reviewAdminReserveTopup(this.selectedRow.id, this.approving, this.decisionNote); this.closeDecision(); await this.loadData(); this.page.showToast(this.approving ? '預備金充值已確認並入賬' : '充值申請已退回'); } catch (error) { this.actionError = error.message || '充值審核失敗'; } finally { this.actionSaving = false; } },
    openBatch() { if (!this.selectedIds.length) { this.page.showToast('請先勾選待確認充值'); return; } this.actionError = ''; this.$refs.batchDialog?.showModal(); }, closeBatch() { this.$refs.batchDialog?.close(); }, async submitBatch() { if (!this.batchNote) return; this.actionSaving = true; this.actionError = ''; const count = this.selectedIds.length; try { await batchConfirmAdminReserveTopups(this.selectedIds, this.batchNote); this.closeBatch(); this.selectedIds = []; await this.loadData(); this.page.showToast(`已確認 ${count} 筆預備金充值`); } catch (error) { this.actionError = error.message || '批量確認失敗'; } finally { this.actionSaving = false; } },
    selectAndOpenProof(row) { this.selectRow(row); this.$nextTick(this.openProof); }, async openProof() { if (!this.selectedRow?.proofDocumentId) return; this.revokeProof(); this.proofLoading = true; this.proofError = ''; this.$refs.proofDialog?.showModal(); try { const result = await fetchAdminReserveProof(this.selectedRow.proofDocumentId); this.proofUrl = URL.createObjectURL(result.blob); } catch (error) { this.proofError = error.message || '憑證載入失敗'; } finally { this.proofLoading = false; } }, closeProof() { this.$refs.proofDialog?.close(); this.revokeProof(); }, revokeProof() { if (this.proofUrl) URL.revokeObjectURL(this.proofUrl); this.proofUrl = ''; }, async downloadProof() { try { const result = await fetchAdminReserveProof(this.selectedRow.proofDocumentId, true); const url = URL.createObjectURL(result.blob); const link = document.createElement('a'); link.href = url; link.download = this.selectedRow.proofName || `reserve-proof-${this.selectedRow.id}`; link.click(); URL.revokeObjectURL(url); } catch (error) { this.page.showToast(error.message || '憑證下載失敗'); } },
    statusParam(v) { return ({ '待確認': 'pending', '已確認': 'confirmed', '已退回': 'rejected', '待同步': 'sync_pending', '同步失敗': 'sync_failed' })[v] || ''; }, requestStatus() { if (this.viewMode === 'pending') return 'pending'; return this.statusParam(this.page.statusFilter) || 'history'; }, confirmationLabel(v) { return ({ pending: '待確認', confirmed: '已確認', rejected: '已退回' })[v] || '未知'; }, confirmationClass(v) { return v === 'confirmed' ? 'green' : v === 'rejected' ? 'red' : 'orange'; }, syncLabel(v) { return ({ not_synced: '未同步', pending: '待同步', synced: '已同步', failed: '同步失敗' })[v] || '未同步'; }, syncClass(v) { return v === 'synced' ? 'green' : v === 'failed' ? 'red' : 'orange'; }, paymentMethodLabel(v) { return ({ bank_transfer: '銀行轉賬', online_payment: '線上支付' })[v] || v || '—'; }, dateTime(v) { return v ? String(v).replace('T', ' ').slice(0, 19) : '—'; }, money(v) { return Number(v || 0).toLocaleString('en-MY', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
  }
};
</script>

<style scoped>
.admin-reserve-finance-workspace { align-items: stretch; }.admin-reserve-finance-workspace .finance-review-panel,.admin-reserve-finance-workspace .finance-review-detail { min-height: 590px; }.admin-reserve-finance-workspace td small { display: block; margin-top: 3px; color: #7b8798; font-size: 9px; }.reserve-after { color: #15803d !important; }
</style>
