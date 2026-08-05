<template>
  <section class="expense-review-layout">
    <div class="panel expense-review-list">
      <div class="panel-head"><div><h2>{{ history ? $t('legacy.t_4c17ca4c2f92') : $t('legacy.t_d673e6a61eaa') }}</h2><span>{{ totalRows }} {{ $t('legacy.t_f8c3a124a8a0') }}</span></div><span v-if="loading">{{ $t('legacy.t_6ce3778a43cd') }}</span></div>
      <div v-if="errorMessage" class="admin-owner-state error"><strong>{{ $t('legacy.t_2c932aa16c9e') }}</strong><span>{{ errorMessage }}</span><button @click="loadData">{{ $t('legacy.t_0c9157b5bfac') }}</button></div>
      <div v-else class="table-wrap"><table><thead><tr><th>{{ $t('legacy.t_32e88cf956f6') }}</th><th>{{ $t('legacy.t_114246450ff0') }}</th><th>{{ $t('legacy.t_c5f5ac884039') }}</th><th>{{ $t('legacy.t_23a48c5c22c1') }}</th><th>{{ $t('legacy.t_380086757011') }}</th><th>{{ $t('legacy.t_b6fed9af8313') }}</th><th>{{ $t('legacy.t_45293595eae3') }}</th><th>{{ $t('legacy.t_f3ea6d345e2a') }}</th></tr></thead><tbody>
        <tr v-for="row in rows" :key="row.id" :class="{ selected: row.id===selectedId }" @click="selectedId=row.id"><td><strong>{{ row.transactionNo }}</strong></td><td>{{ row.projectName }}<small>{{ row.unitNo }}</small></td><td>{{ categoryLabel(row.receiptNo) }}</td><td>{{ row.milestone || '—' }}</td><td><b>{{ row.currency || 'MYR' }} {{ money(row.amount) }}</b></td><td>{{ row.transactionDate }}</td><td><span class="tag" :class="statusClass(row.confirmationStatus)">{{ statusLabel(row.confirmationStatus) }}</span></td><td><div v-if="history" class="finance-document-actions"><a :href="documentUrl(row, 'invoice')" download @click.stop>Invoice / 发票</a><a :href="documentUrl(row, 'receipt')" download @click.stop>Official Receipt / 收据</a></div><button v-else class="row-actions" @click.stop="selectedId=row.id">…</button></td></tr>
        <tr v-if="!loading&&!rows.length"><td colspan="8" class="admin-owner-empty">{{ $t('legacy.t_2047f2c147d6') }}</td></tr>
      </tbody></table></div>
      <div class="pager"><span>{{ $t('legacy.t_3b6ef811b85a') }} {{ totalRows }} {{ $t('legacy.t_86761b63a7bd') }}</span><div class="admin-building-pager"><button :disabled="pageNumber<=1" @click="goPage(pageNumber-1)">&lt;</button><button class="active">{{ pageNumber }}</button><button :disabled="pageNumber>=totalPages" @click="goPage(pageNumber+1)">&gt;</button><select v-model.number="pageSize"><option :value="10">{{ $t('legacy.t_fc6da0e815a1') }}</option><option :value="20">{{ $t('legacy.t_93d673672fa5') }}</option></select></div></div>
    </div>
    <aside class="panel expense-review-detail"><template v-if="selectedRow"><div class="expense-title"><span>{{ $t('legacy.t_841534d30dbd') }}</span><div><h3>{{ categoryLabel(selectedRow.receiptNo) }}</h3><p>{{ selectedRow.projectName }} · {{ selectedRow.unitNo }}</p></div><em class="tag" :class="statusClass(selectedRow.confirmationStatus)">{{ statusLabel(selectedRow.confirmationStatus) }}</em></div><div class="expense-amount">{{ selectedRow.currency || 'MYR' }} {{ money(selectedRow.amount) }}</div><dl><dt>{{ $t('legacy.t_5684c8ab84a8') }}</dt><dd>{{ selectedRow.milestone }}</dd><dt>{{ $t('legacy.t_34ccfa907c1b') }}</dt><dd>{{ selectedRow.transactionDate }}</dd><dt>{{ $t('legacy.t_32e88cf956f6') }}</dt><dd>{{ selectedRow.transactionNo }}</dd><dt>{{ $t('legacy.t_1c1e89a30c41') }}</dt><dd>{{ selectedRow.confirmedByName || '—' }}</dd></dl><div v-if="history" class="finance-document-detail-actions"><a :href="documentUrl(selectedRow, 'invoice')" download>Invoice / 发票</a><a :href="documentUrl(selectedRow, 'receipt')" download>Official Receipt / 收据</a></div><div v-if="selectedRow.confirmationStatus==='pending'" class="expense-actions"><button class="confirm" @click="openDecision(true)">{{ $t('legacy.t_c678ab5851c4') }}</button><button class="reject" @click="openDecision(false)">{{ $t('legacy.t_607cd976d2ca') }}</button></div></template><div v-else class="admin-owner-empty">{{ $t('legacy.t_932a8456c2e2') }}</div></aside>
    <dialog ref="decision" class="modal admin-finance-decision-dialog"><form method="dialog" @submit.prevent="submitDecision"><div class="modal-head"><div><h3>{{ approving?$t('legacy.t_1aa94bf77754'):$t('legacy.t_2a9ef86ee366') }}</h3><small>{{ selectedRow?.milestone }}</small></div><button type="button" class="icon-close" @click="$refs.decision.close()">×</button></div><div class="finance-decision-body"><label>{{ $t('legacy.t_098de965a383') }}<textarea v-model.trim="note" maxlength="500" required></textarea></label><p v-if="actionError" class="admin-property-error">{{ actionError }}</p></div><menu><button type="button" @click="$refs.decision.close()">{{ $t('legacy.t_4d0b4688c787') }}</button><button class="primary-btn" :class="{reject:!approving}" :disabled="saving">{{ saving?$t('legacy.t_1e038f9b55ec'):approving?$t('legacy.t_86a07295c547'):$t('legacy.t_607cd976d2ca') }}</button></menu></form></dialog>
  </section>
</template>

<script>
import { confirmAdminFinanceReview, downloadAdminFinanceDocuments, fetchAdminFinanceProjects, fetchAdminFinanceReviews, getAdminFinanceDocumentUrl, rejectAdminFinanceReview } from '../services/propertyApi';
export default {
  inject: ['page'],
  data() { return { rows: [], selectedId: null, pageNumber: 1, pageSize: 10, totalRows: 0, totalPages: 1, loading: false, errorMessage: '', serial: 0, approving: true, note: '', saving: false, actionError: '' }; },
  computed: {
    history() { return this.page.adminFinanceViewMode === 'history'; },
    selectedRow() { return this.rows.find(x => x.id === this.selectedId) || this.rows[0] || null; }
  },
  watch: {
    'page.moduleSearch'() { this.reset(); }, 'page.globalSearch'() { this.reset(); }, 'page.projectFilter'() { this.reset(); }, 'page.statusFilter'() { this.reset(); }, 'page.adminFinanceViewMode'() { this.reset(); }, 'page.dateStart'() { this.reset(); }, 'page.dateEnd'() { this.reset(); }, pageSize() { this.reset(); }
  },
  mounted() { this.loadData(); },
  methods: {
    async loadData() {
      const serial = ++this.serial; this.loading = true; this.errorMessage = '';
      try {
        const params = { type: 'expense', page: this.pageNumber, pageSize: this.pageSize, keyword: this.page.globalSearch || this.page.moduleSearch || '', projectName: String(this.page.projectFilter || '').includes('全部') ? '' : this.page.projectFilter, status: this.history ? 'history' : 'pending', startDate: this.page.dateStart, endDate: this.page.dateEnd };
        const [response, projects] = await Promise.all([fetchAdminFinanceReviews(params), fetchAdminFinanceProjects('expense')]);
        if (serial !== this.serial) return;
        this.rows = response.rows || []; this.totalRows = response.page?.totalRows || 0; this.totalPages = response.page?.totalPages || 1; this.pageNumber = response.page?.page || 1; this.page.adminFinanceProjects = projects || []; this.page.adminFinanceMetrics = this.metrics(response.summary || {});
        if (!this.rows.some(x => x.id === this.selectedId)) this.selectedId = this.rows[0]?.id || null;
      } catch (e) { this.rows = []; this.errorMessage = e.message || 'API request failed'; }
      finally { if (serial === this.serial) this.loading = false; }
    },
    reset() { this.pageNumber = 1; this.loadData(); },
    goPage(n) { if (n >= 1 && n <= this.totalPages) { this.pageNumber = n; this.loadData(); } },
    documentUrl(row, type) { return row?.id ? getAdminFinanceDocumentUrl(row.id, type) : '#'; },
    async batchDownload(type) {
      const ids = this.rows.map(row => row.id).filter(Boolean);
      if (!ids.length) { this.page.showToast('当前没有可下载的历史记录'); return; }
      try {
        const result = await downloadAdminFinanceDocuments(ids, type);
        const url = URL.createObjectURL(result.blob);
        const link = document.createElement('a'); link.href = url; link.download = result.filename; document.body.appendChild(link); link.click(); link.remove();
        window.setTimeout(() => URL.revokeObjectURL(url), 1000);
        this.page.showToast(`已生成 ${ids.length} 份${type === 'invoice' ? '发票' : '收据'}`);
      } catch (error) { this.page.showToast(error.message || '批量下载失败'); }
    },
    openDecision(ok) { this.approving = ok; this.note = ok ? '費用資料核對正確' : ''; this.actionError = ''; this.$refs.decision?.showModal(); },
    async submitDecision() {
      if (!this.note) return; this.saving = true;
      try { if (this.approving) await confirmAdminFinanceReview(this.selectedRow.id, this.note); else await rejectAdminFinanceReview(this.selectedRow.id, this.note); this.$refs.decision.close(); await this.loadData(); this.page.showToast(this.approving ? '房產費用已確認' : '房產費用已退回'); }
      catch (e) { this.actionError = e.message || '處理失敗'; } finally { this.saving = false; }
    },
    metrics(s) { return [{ label: '待確認費用', value: `${Number(s.pendingCount || 0)} 筆`, delta: `RM ${this.money(s.pendingAmount)}`, trend: Number(s.pendingCount) ? 'down' : 'up' }, { label: '已確認費用', value: `${Number(s.confirmedCount || 0)} 筆`, delta: '保留歷史金額', trend: 'up' }, { label: '已退回費用', value: `${Number(s.rejectedCount || 0)} 筆`, delta: '等待調整', trend: '' }, { label: '待同步 SQL', value: `${Number(s.pendingSyncCount || 0)} 筆`, delta: '會計同步', trend: '' }, { label: '本月確認費用', value: `RM ${this.money(s.confirmedMonthAmount)}`, delta: '本月完成', trend: 'up' }]; },
    categoryLabel(v) { return ({ management: '管理費', service_fee: '服務費', insurance: '保險', tax: '稅費', deposit: '租客押金' })[v] || v || '其他費用'; },
    statusLabel(v) { return ({ pending: '待確認', confirmed: '已確認', rejected: '已退回' })[v] || v; },
    statusClass(v) { return v === 'confirmed' ? 'green' : v === 'rejected' ? 'red' : 'orange'; },
    money(v) { return Number(v || 0).toLocaleString('en-MY', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
  }
};
</script>

<style scoped>
.expense-review-layout{display:grid;grid-template-columns:minmax(0,2.1fr) minmax(320px,1fr);gap:12px;margin:0 28px;min-height:560px}.expense-review-list,.expense-review-detail{min-width:0}.expense-review-list small{display:block;color:#718096;margin-top:3px}.expense-review-detail{padding:20px}.expense-title{display:flex;align-items:center;gap:12px}.expense-title>span{display:grid;place-items:center;width:46px;height:46px;border-radius:14px;background:#e9f8f7;color:#078b8d;font-size:20px;font-weight:700}.expense-title h3,.expense-title p{margin:0}.expense-title em{margin-left:auto;font-style:normal}.expense-amount{margin:24px 0;font-size:28px;font-weight:700;color:#073b67}.expense-review-detail dl{display:grid;grid-template-columns:90px 1fr;gap:14px;border-top:1px solid #e4ebf3;padding-top:18px}.expense-review-detail dt{color:#708096}.expense-review-detail dd{margin:0;font-weight:600}.expense-actions{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:24px}.expense-actions button{height:40px;border-radius:8px;border:1px solid #d5dfeb;background:#fff}.expense-actions .confirm{background:#078b8d;color:#fff;border-color:#078b8d}.expense-actions .reject{color:#dc2626}@media(max-width:1100px){.expense-review-layout{grid-template-columns:1fr}.expense-review-detail{display:none}}
</style>
<style scoped>
.finance-document-actions{display:grid;gap:4px;min-width:106px}.finance-document-actions a,.finance-document-button{display:inline-flex;align-items:center;justify-content:center;min-height:26px;padding:0 7px;border:1px solid #b7cfee;border-radius:6px;background:#f7fbff;color:#185a97;font-size:10px;text-decoration:none;white-space:nowrap}.finance-document-actions a:hover,.finance-document-button:hover{background:#eaf3ff;border-color:#6f9fda}
</style>
