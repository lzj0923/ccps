<template>
  <section class="content-grid admin-finance-workspace admin-rent-finance-workspace">
    <div class="panel table-panel finance-review-list-panel">
      <div class="panel-head"><div><h2>租金收款歷史</h2><span>{{ totalRows }} records · 已完成或已退回的記錄</span></div><button class="primary-btn" :disabled="!selectedRow || proofBusy" @click="chooseProof">{{ selectedRow?.proofDocumentId ? '更換憑證' : '補上憑證' }}</button></div>
      <input ref="proofInput" type="file" accept="application/pdf,image/jpeg,image/png" style="display:none" @change="uploadSelectedProof">
      <div v-if="errorMessage" class="admin-owner-state error"><strong>租金憑證資料載入失敗</strong><span>{{ errorMessage }}</span><button @click="loadData">重新載入</button></div>
      <div v-else class="table-wrap finance-review-table-wrap">
        <table>
          <thead><tr><th>交易編號</th><th>租客</th><th>建案／單位</th><th>帳單月份</th><th>收款金額</th><th>收款日期</th><th>付款憑證</th><th>確認狀態</th><th>SQL 同步</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id" :class="{ selected: row.id === selectedRow?.id }" @click="selectRow(row)">
              <td><strong>{{ row.transactionNo }}</strong><small>{{ paymentMethodLabel(row.paymentMethod) }}</small></td>
              <td>{{ row.tenantName }}<small>{{ row.leaseNo }}</small></td>
              <td>{{ row.projectName }}<small>{{ row.unitNo }}</small></td>
              <td>{{ monthLabel(row.billingMonth) }}<small>到期 {{ row.dueDate || '—' }}</small></td>
              <td><b>{{ row.currency || 'MYR' }} {{ money(row.amount) }}</b></td>
              <td>{{ row.transactionDate || '—' }}</td>
              <td><button class="finance-proof-link" :class="{ missing: !row.proofDocumentId }" @click.stop="proofAction(row)">{{ row.proofDocumentId ? '查看憑證' : '補上憑證' }}</button></td>
              <td><span class="tag" :class="confirmationClass(row.confirmationStatus)">{{ confirmationLabel(row.confirmationStatus) }}</span></td>
              <td><span class="tag" :class="syncClass(row.syncStatus)">{{ syncLabel(row.syncStatus) }}</span></td>
              <td><button class="row-actions" @click.stop="selectRow(row)">…</button></td>
            </tr>
            <tr v-if="!loading && !rows.length"><td colspan="10" class="admin-owner-empty">目前沒有符合條件的租金歷史記錄</td></tr>
          </tbody>
        </table>
      </div>
      <div class="pager"><span>共 {{ totalRows }} records</span><div class="admin-building-pager"><button :disabled="pageNumber <= 1" @click="goPage(pageNumber - 1)">&lt;</button><button v-for="n in visiblePages" :key="n" :class="{ active: n === pageNumber }" @click="goPage(n)">{{ n }}</button><button :disabled="pageNumber >= totalPages" @click="goPage(pageNumber + 1)">&gt;</button><select v-model.number="pageSize"><option :value="10">10 條/頁</option><option :value="20">20 條/頁</option><option :value="50">50 條/頁</option></select></div></div>
    </div>

    <aside class="panel detail-panel finance-review-detail">
      <div v-if="selectedRow" class="detail-card">
        <div class="profile finance-review-profile"><div class="big-avatar">租</div><div class="finance-review-profile-copy"><h3>{{ selectedRow.transactionNo }}</h3><p>{{ selectedRow.tenantName }} · {{ selectedRow.projectName }} / {{ selectedRow.unitNo }}</p></div><span class="tag" :class="confirmationClass(selectedRow.confirmationStatus)">{{ confirmationLabel(selectedRow.confirmationStatus) }}</span></div>
        <div class="detail-actions finance-review-actions">
          <button v-if="selectedRow.proofDocumentId" :disabled="proofBusy" @click="openProof">查看憑證</button>
          <button v-if="selectedRow.proofDocumentId" :disabled="proofBusy" @click="downloadProof">下載憑證</button>
          <button :disabled="proofBusy" @click="chooseProof">{{ proofBusy ? '處理中…' : selectedRow.proofDocumentId ? '更換憑證' : '補上憑證' }}</button>
          <button @click="goToTenancy">前往租客與租金</button>
        </div>
        <div class="detail-section"><h4><span class="num">1</span>線下收款記錄</h4><div class="kv"><span>租客</span><b>{{ selectedRow.tenantName }}</b></div><div class="kv"><span>付款方式</span><b>{{ paymentMethodLabel(selectedRow.paymentMethod) }}</b></div><div class="kv"><span>收款日期</span><b>{{ selectedRow.transactionDate || '—' }}</b></div><div class="kv"><span>收款金額</span><b>{{ selectedRow.currency || 'MYR' }} {{ money(selectedRow.amount) }}</b></div></div>
        <div class="detail-section"><h4><span class="num">2</span>關聯租金帳單</h4><div class="kv"><span>租約</span><b>{{ selectedRow.leaseNo }}</b></div><div class="kv"><span>帳單月份</span><b>{{ monthLabel(selectedRow.billingMonth) }}</b></div><div class="kv"><span>本期應收</span><b>RM {{ money(selectedRow.invoiceAmount) }}</b></div><div class="kv"><span>已入帳金額</span><b>RM {{ money(selectedRow.invoicePaid) }}</b></div><div class="progress"><i :style="{ width: invoiceProgress + '%' }"></i></div></div>
        <div class="detail-section"><h4><span class="num">3</span>憑證與同步</h4><div class="kv"><span>憑證文件</span><b>{{ selectedRow.proofName || '尚未上傳' }}</b></div><div class="kv"><span>文件大小</span><b>{{ fileSize(selectedRow.proofSize) }}</b></div><div class="kv"><span>SQL 同步</span><b>{{ syncLabel(selectedRow.syncStatus) }}</b></div><div class="kv"><span>記錄時間</span><b>{{ formatDateTime(selectedRow.submittedAt) }}</b></div></div>
      </div>
      <div v-else class="admin-owner-empty">目前沒有可查看的租金收款記錄</div>
    </aside>

    <dialog ref="proofDialog" class="modal admin-finance-proof-dialog"><div class="modal-head"><div><h3>租金付款憑證</h3><small>{{ selectedRow?.proofName || selectedRow?.transactionNo }}</small></div><button type="button" class="icon-close" @click="closeProof">×</button></div><div class="finance-proof-viewer"><div v-if="proofLoading" class="admin-owner-state">正在載入租金憑證…</div><img v-else-if="proofUrl && selectedRow?.proofMimeType?.startsWith('image/')" :src="proofUrl" alt="租金付款憑證"><iframe v-else-if="proofUrl && selectedRow?.proofMimeType === 'application/pdf'" :src="proofUrl" title="租金付款憑證 PDF"></iframe><div v-else class="admin-owner-state">{{ proofError || '無法預覽此憑證格式' }}</div></div><menu><button @click="closeProof">關閉</button><button class="primary-btn" :disabled="!selectedRow?.proofDocumentId" @click="downloadProof">下載原始文件</button></menu></dialog>
  </section>
</template>

<script>
import { fetchAdminRentFinanceProjects, fetchAdminRentFinanceReviews, fetchAdminRentProof, uploadAdminRentProof } from '../services/propertyApi';

export default {
  inject: ['page'],
  data() { return { rows: [], selectedId: null, pageNumber: 1, pageSize: 10, totalRows: 0, totalPages: 1, loading: false, errorMessage: '', requestSerial: 0, proofBusy: false, proofLoading: false, proofError: '', proofUrl: '' }; },
  computed: {
    selectedRow() { return this.rows.find(row => row.id === this.selectedId) || this.rows[0] || null; },
    visiblePages() { const start = Math.max(1, Math.min(this.pageNumber - 2, this.totalPages - 4)); return Array.from({ length: Math.min(5, this.totalPages) }, (_, i) => start + i); },
    invoiceProgress() { return !Number(this.selectedRow?.invoiceAmount) ? 0 : Math.min(100, Math.round(Number(this.selectedRow.invoicePaid || 0) / Number(this.selectedRow.invoiceAmount) * 100)); },
    refreshNonce() { return this.page.adminFinanceRefreshNonce; },
    proofUploadNonce() { return this.page.adminRentProofUploadNonce; }
  },
  watch: {
    'page.moduleSearch'() { this.resetLoad(); }, 'page.globalSearch'() { this.resetLoad(); }, 'page.projectFilter'() { this.resetLoad(); }, 'page.statusFilter'() { this.resetLoad(); }, 'page.dateStart'() { this.resetLoad(); }, 'page.dateEnd'() { this.resetLoad(); },
    pageSize() { this.pageNumber = 1; this.loadData(); }, refreshNonce(value, previous) { if (value > previous) this.loadData(); }, proofUploadNonce(value, previous) { if (value > previous) this.chooseProof(); }, rows: { deep: true, handler(rows) { if (!rows.some(row => row.id === this.selectedId)) this.selectedId = rows[0]?.id || null; } }
  },
  mounted() { this.loadData(); }, beforeUnmount() { this.revokeProofUrl(); },
  methods: {
    async loadData() { const serial = ++this.requestSerial; const preferredId = this.selectedId; this.loading = true; this.errorMessage = ''; try { const [response, projects] = await Promise.all([fetchAdminRentFinanceReviews({ page: this.pageNumber, pageSize: this.pageSize, keyword: this.page.globalSearch || this.page.moduleSearch || '', projectName: String(this.page.projectFilter || '').includes('全部') ? '' : this.page.projectFilter, status: this.statusParam(this.page.statusFilter) || 'history', startDate: this.page.dateStart, endDate: this.page.dateEnd }), fetchAdminRentFinanceProjects()]); if (serial !== this.requestSerial) return; this.rows = response.rows || []; this.totalRows = response.page?.totalRows || 0; this.totalPages = response.page?.totalPages || 1; this.pageNumber = response.page?.page || 1; this.selectedId = this.rows.some(row => row.id === preferredId) ? preferredId : this.rows[0]?.id || null; this.page.adminFinanceProjects = projects || []; this.page.adminFinanceMetrics = this.metrics(response.summary || {}); } catch (e) { if (serial !== this.requestSerial) return; this.rows = []; this.errorMessage = e.message || 'API request failed'; this.page.adminFinanceMetrics = null; } finally { if (serial === this.requestSerial) this.loading = false; } },
    metrics(s) { return [{ label: '租金收款記錄', value: `${Number(s.totalCount || 0)} 筆`, delta: `RM ${this.money(s.totalAmount)}`, trend: 'up' }, { label: '已上傳憑證', value: `${Number(s.withProofCount || 0)} 筆`, delta: '線下審批憑證', trend: 'up' }, { label: '缺少憑證', value: `${Number(s.missingProofCount || 0)} 筆`, delta: '需要補上文件', trend: Number(s.missingProofCount) ? 'down' : 'up' }, { label: '待同步 SQL', value: `${Number(s.pendingSyncCount || 0)} 筆`, delta: '會計同步狀態', trend: Number(s.pendingSyncCount) ? 'down' : 'up' }, { label: '本月收款金額', value: `RM ${this.money(s.monthAmount)}`, delta: '按收款日期統計', trend: 'up' }]; },
    resetLoad() { this.pageNumber = 1; this.loadData(); }, goPage(n) { if (n >= 1 && n <= this.totalPages && n !== this.pageNumber) { this.pageNumber = n; this.loadData(); } }, selectRow(row) { this.selectedId = row.id; },
    proofAction(row) { this.selectRow(row); this.$nextTick(() => row.proofDocumentId ? this.openProof() : this.chooseProof()); }, chooseProof() { if (!this.selectedRow || this.proofBusy) return; this.$refs.proofInput.value = ''; this.$refs.proofInput.click(); },
    async uploadSelectedProof(event) { const file = event.target.files?.[0]; if (!file) return; if (!this.validProof(file)) { event.target.value = ''; return; } const replacing = Boolean(this.selectedRow.proofDocumentId); this.proofBusy = true; try { await uploadAdminRentProof(this.selectedRow.id, file); await this.loadData(); this.page.showToast(replacing ? '租金憑證已更換' : '租金憑證已補上'); } catch (e) { this.page.showToast(e.message || '租金憑證上傳失敗'); } finally { this.proofBusy = false; event.target.value = ''; } }, validProof(file) { if (!['application/pdf', 'image/jpeg', 'image/png'].includes(file.type) || file.size > 10 * 1024 * 1024) { this.page.showToast('只支援 10MB 以內的 PDF、JPG 或 PNG 憑證'); return false; } return true; },
    async openProof() { if (!this.selectedRow?.proofDocumentId) return; this.revokeProofUrl(); this.proofLoading = true; this.proofError = ''; this.$refs.proofDialog?.showModal(); try { const result = await fetchAdminRentProof(this.selectedRow.proofDocumentId); this.proofUrl = URL.createObjectURL(result.blob); } catch (e) { this.proofError = e.message || '租金憑證載入失敗'; } finally { this.proofLoading = false; } }, closeProof() { this.$refs.proofDialog?.close(); this.revokeProofUrl(); }, revokeProofUrl() { if (this.proofUrl) URL.revokeObjectURL(this.proofUrl); this.proofUrl = ''; }, async downloadProof() { try { const result = await fetchAdminRentProof(this.selectedRow.proofDocumentId, true); const url = URL.createObjectURL(result.blob); const link = document.createElement('a'); link.href = url; link.download = this.selectedRow.proofName || `rent-proof-${this.selectedRow.id}`; link.click(); URL.revokeObjectURL(url); } catch (e) { this.page.showToast(e.message || '租金憑證下載失敗'); } },
    goToTenancy() { const keyword = this.selectedRow?.tenantName || this.selectedRow?.unitNo || ''; this.page.selectModule('adminTenants'); this.$nextTick(() => { this.page.globalSearch = keyword; }); }, statusParam(v) { return ({ '已確認': 'confirmed', '已退回': 'rejected', '待同步': 'sync_pending', '同步失敗': 'sync_failed' })[v] || ''; }, confirmationLabel(v) { return ({ confirmed: '已確認', rejected: '已退回' })[v] || '已完成'; }, confirmationClass(v) { return v === 'rejected' ? 'red' : 'green'; }, syncLabel(v) { return ({ not_synced: '未同步', pending: '待同步', synced: '已同步', failed: '同步失敗' })[v] || '未同步'; }, syncClass(v) { return v === 'synced' ? 'green' : v === 'failed' ? 'red' : 'orange'; }, paymentMethodLabel(v) { return ({ bank_transfer: '銀行轉帳', online_transfer: '網上轉帳', cheque: '支票', cash: '現金', online_banking: '網上銀行' })[v] || v || '—'; }, monthLabel(v) { return v ? String(v).slice(0, 7) : '—'; }, formatDateTime(v) { return v ? String(v).replace('T', ' ').slice(0, 19) : '—'; }, fileSize(v) { if (!v) return '—'; return v >= 1024 * 1024 ? `${(v / 1024 / 1024).toFixed(1)} MB` : `${Math.ceil(v / 1024)} KB`; }, money(v) { return Number(v || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
  }
};
</script>
