<template>
  <section class="expense-review-layout">
    <div class="panel expense-review-list">
      <div class="panel-head"><div><h2>{{ listTitle }}</h2><span>{{ totalRows }} {{ $t('legacy.t_404544c51ffe') }}</span></div><div class="expense-batch-actions"><span v-if="selectedIds.length">{{ $t('legacy.t_743aaf951e5d') }} {{ selectedIds.length }} {{ $t('legacy.t_f4d0aeab9772') }}</span><span v-if="loading">{{ $t('legacy.t_6ce3778a43cd') }}</span><template v-if="history"><button type="button" :disabled="!selectedIds.length || batchBusy" @click="batchDownload('invoice')">{{ $t('legacy.t_5793ea8f30e0') }}</button><button type="button" :disabled="!selectedIds.length || batchBusy" @click="batchDownload('receipt')">{{ $t('legacy.t_ec97150b081d') }}</button><button type="button" class="danger" :disabled="!selectedReopenIds.length || saving" @click="openBatchReopen">{{ $t('legacy.t_9d42644f9014') }}</button></template></div></div>
      <div v-if="errorMessage" class="admin-owner-state error"><strong>{{ $t('legacy.t_2c932aa16c9e') }}</strong><span>{{ $lt(errorMessage) }}</span><button @click="loadData">{{ $t('legacy.t_0c9157b5bfac') }}</button></div>
      <div v-else class="table-wrap"><table><thead><tr><th class="finance-check-cell"><input type="checkbox" :checked="allSelectableSelected" :disabled="!selectableRows.length" :aria-label="$t('legacy.t_e71c7a3e8777')" @change="toggleSelectablePage"></th><th>{{ $t('legacy.t_32e88cf956f6') }}</th><th>{{ $t('legacy.t_114246450ff0') }}</th><th>{{ $t('legacy.t_c5f5ac884039') }}</th><th>{{ $t('legacy.t_23a48c5c22c1') }}</th><th>{{ $t('legacy.t_380086757011') }}</th><th>{{ $t('legacy.t_b6fed9af8313') }}</th><th>{{ $t('legacy.t_45293595eae3') }}</th><th>{{ $t('legacy.t_f3ea6d345e2a') }}</th></tr></thead><tbody>
        <tr v-for="row in rows" :key="row.id" :class="{ selected: row.id===selectedId }"><td class="finance-check-cell"><input v-if="history || row.confirmationStatus==='pending'" v-model="selectedIds" type="checkbox" :value="row.id" @click.stop></td><td><strong>{{ row.transactionNo }}</strong></td><td>{{ row.projectName }}<small>{{ row.unitNo }}</small></td><td>{{ $lt(categoryLabel(row.receiptNo)) }}</td><td>{{ row.milestone || '—' }}</td><td><b>{{ row.currency || 'MYR' }} {{ money(row.amount) }}</b></td><td>{{ displayDate(row.transactionDate) }}</td><td><span class="tag" :class="statusClass(row.confirmationStatus)">{{ $lt(statusLabel(row.confirmationStatus)) }}</span></td><td><button class="finance-detail-button" type="button" @click="openDetails(row)">{{ $t('legacy.t_faea8c1db9cc') }}</button></td></tr>
        <tr v-if="!loading&&!rows.length"><td colspan="9" class="admin-owner-empty">{{ $t('legacy.t_8b08ebcb0c11') }}{{ sectionName }}</td></tr>
      </tbody></table></div>
      <div class="pager"><span>{{ $t('legacy.t_3b6ef811b85a') }} {{ totalRows }} {{ $t('legacy.t_86761b63a7bd') }}</span><div class="admin-building-pager"><button :disabled="pageNumber<=1" @click="goPage(pageNumber-1)">&lt;</button><button class="active">{{ pageNumber }}</button><button :disabled="pageNumber>=totalPages" @click="goPage(pageNumber+1)">&gt;</button><select v-model.number="pageSize"><option :value="5">{{ $t('legacy.t_f2d45e9d539d') }}</option><option :value="10">{{ $t('legacy.t_cb79efdcac1a') }}</option><option :value="20">{{ $t('legacy.t_8f7267417423') }}</option></select></div></div>
    </div>
    <dialog ref="details" class="modal expense-detail-dialog"><template v-if="selectedRow"><div class="modal-head"><div><h3>{{ detailTitle }}</h3><small>{{ selectedRow.transactionNo }}</small></div><button type="button" class="icon-close" @click="closeDetails">×</button></div><div class="expense-detail-body"><div class="expense-title"><span>{{ sectionIcon }}</span><div><h3>{{ $lt(categoryLabel(selectedRow.receiptNo)) }}</h3><p>{{ selectedRow.projectName }} · {{ selectedRow.unitNo }}</p></div><em class="tag" :class="statusClass(selectedRow.confirmationStatus)">{{ $lt(statusLabel(selectedRow.confirmationStatus)) }}</em></div><div class="expense-amount">{{ selectedRow.currency || 'MYR' }} {{ money(selectedRow.amount) }}</div><dl><dt>{{ $t('legacy.t_5684c8ab84a8') }}</dt><dd>{{ selectedRow.milestone || '—' }}</dd><dt>{{ $t('legacy.t_34ccfa907c1b') }}</dt><dd>{{ displayDate(selectedRow.transactionDate) }}</dd><dt>{{ $t('legacy.t_32e88cf956f6') }}</dt><dd>{{ selectedRow.transactionNo }}</dd><dt>{{ $t('legacy.t_1c1e89a30c41') }}</dt><dd>{{ selectedRow.confirmedByName || '—' }}</dd></dl><FinanceAllocationNoteEditor :record="selectedRow" @saved="loadData" /><div class="finance-document-detail-actions"><template v-if="history"><a :href="documentUrl(selectedRow, 'invoice')" download>{{ $t('legacy.t_c78fa9626abb') }}</a><a :href="documentUrl(selectedRow, 'receipt')" download>{{ $t('legacy.t_3f584f4c386e') }}</a><button v-if="selectedRow.confirmationStatus==='confirmed'" class="danger" type="button" @click="reopenFromDetails">{{ $t('legacy.t_2954008c3811') }}</button></template><template v-else><button class="confirm" type="button" @click="decisionFromDetails(true)">{{ confirmLabel }}</button><button class="danger" type="button" @click="decisionFromDetails(false)">{{ $t('legacy.t_c9a603cbf3ff') }}</button></template></div></div></template></dialog>
    <dialog ref="decision" class="modal admin-finance-decision-dialog"><form method="dialog" @submit.prevent="submitDecision"><div class="modal-head"><div><h3>{{ approving?$t('legacy.t_1aa94bf77754'):$t('legacy.t_2a9ef86ee366') }}</h3><small>{{ selectedRow?.milestone }}</small></div><button type="button" class="icon-close" @click="$refs.decision.close()">×</button></div><div class="finance-decision-body"><label v-if="approving">{{ $t('legacy.t_8c3f70d58ba3') }}<input v-model="decisionDate" type="date" :max="today" required><small>{{ $t('legacy.t_c10cc7d59a07') }}</small></label><label>{{ $t('legacy.t_098de965a383') }}<textarea v-model.trim="note" maxlength="500" required></textarea></label><p v-if="actionError" class="admin-property-error">{{ $lt(actionError) }}</p></div><menu><button type="button" @click="$refs.decision.close()">{{ $t('legacy.t_4d0b4688c787') }}</button><button class="primary-btn" :class="{reject:!approving}" :disabled="saving">{{ saving?$t('legacy.t_1e038f9b55ec'):approving?$t('legacy.t_86a07295c547'):$t('legacy.t_607cd976d2ca') }}</button></menu></form></dialog>
    <dialog ref="reopen" class="modal admin-finance-decision-dialog"><form method="dialog" @submit.prevent="submitReopen"><div class="modal-head"><div><h3>{{ $t('finance.reopenTitle') }}</h3><small>{{ selectedRow?.transactionNo }}</small></div><button type="button" class="icon-close" @click="$refs.reopen.close()">×</button></div><div class="finance-decision-body"><div class="finance-decision-warning"><strong>{{ $t('finance.reopen') }}</strong><span>{{ $t('finance.reopenHint') }}</span></div><label>{{ $t('finance.reopenNote') }}<textarea v-model.trim="reopenNote" maxlength="500" required :placeholder="$t('finance.reopenNotePlaceholder')"></textarea></label><p v-if="actionError" class="admin-property-error">{{ $lt(actionError) }}</p></div><menu><button type="button" @click="$refs.reopen.close()">{{ $t('legacy.t_4d0b4688c787') }}</button><button class="primary-btn reject" :disabled="saving">{{ saving?$t('legacy.t_1e038f9b55ec'):$t('finance.reopen') }}</button></menu></form></dialog>
    <dialog ref="batchReopen" class="modal admin-finance-decision-dialog"><form method="dialog" @submit.prevent="submitBatchReopen"><div class="modal-head"><div><h3>{{ $t('legacy.t_920e42140bdb') }}</h3><small>{{ $t('legacy.t_839ee29d9fec') }} {{ selectedReopenIds.length }} {{ $t('legacy.t_17ffd89fc824') }}</small></div><button type="button" class="icon-close" @click="closeBatchReopen">×</button></div><div class="finance-decision-body"><div class="finance-decision-warning"><strong>{{ $t('legacy.t_bf0ff597c4cc') }}</strong><span>{{ $t('legacy.t_87584c1e3fd8') }}</span></div><label>{{ $t('legacy.t_2fe161329598') }}<textarea v-model.trim="batchReopenNote" maxlength="500" required :placeholder="$t('legacy.t_e75387a2842c')"></textarea></label><p v-if="actionError" class="admin-property-error">{{ $lt(actionError) }}</p></div><menu><button type="button" @click="closeBatchReopen">{{ $t('legacy.t_4d0b4688c787') }}</button><button class="primary-btn reject" :disabled="saving">{{ saving ? $t('legacy.t_1cac8ac7f58f') : $t('ui.returnCount', { count: selectedReopenIds.length }) }}</button></menu></form></dialog>
    <dialog ref="batch" class="modal admin-finance-decision-dialog"><form method="dialog" @submit.prevent="submitBatch"><div class="modal-head"><div><h3>{{ $t('legacy.t_695d585707d5') }}{{ sectionName }}</h3><small>{{ $t('legacy.t_9daf73e3505e') }} {{ selectedIds.length }} {{ $t('legacy.t_c5b1282af54c') }}</small></div><button type="button" class="icon-close" @click="$refs.batch.close()">×</button></div><div class="finance-decision-body"><div class="finance-batch-overview"><span>{{ $t('legacy.t_78255fc5ff85') }}<b>{{ batchRows.length }} {{ $t('legacy.t_f4d0aeab9772') }}</b></span><span>{{ $t('legacy.t_9f5a58fc5586') }}<b>{{ $t('legacy.t_e98e2e0c8957') }} {{ money(batchTotal) }}</b></span></div><div class="finance-decision-warning success"><strong>{{ $t('legacy.t_78f6285d6f63') }}</strong><span>{{ $t('legacy.t_a5d0d6b2f2a9') }}</span></div><label>{{ $t('legacy.t_8c3f70d58ba3') }}<input v-model="batchDate" type="date" :max="today" required></label><label>{{ $t('legacy.t_9fc15be169de') }}<textarea v-model.trim="batchNote" maxlength="500" required></textarea></label><label>{{ $t('legacy.t_6a2f40215cfa') }}<input v-model.trim="batchReference" maxlength="120" :placeholder="$t('legacy.t_d1f0b4018693')"></label><p v-if="actionError" class="admin-property-error">{{ $lt(actionError) }}</p></div><menu><button type="button" @click="$refs.batch.close()">{{ $t('legacy.t_4d0b4688c787') }}</button><button class="primary-btn" :disabled="saving">{{ saving ? $t('legacy.t_1cac8ac7f58f') : $t('ui.batchConfirmCount', { count: selectedIds.length }) }}</button></menu></form></dialog>
  </section>
</template>

<script>
import { batchConfirmAdminFinanceReviews, batchReopenAdminFinanceReviews, confirmAdminFinanceReview, downloadAdminFinanceDocuments, fetchAdminFinanceProjects, fetchAdminFinanceReviews, getAdminFinanceDocumentUrl, rejectAdminFinanceReview, reopenAdminFinanceReview } from '../services/propertyApi';
import FinanceAllocationNoteEditor from './FinanceAllocationNoteEditor.vue';
import { formatDate, todayIsoDate } from '../utils/dateFormat';
export default {
  components: { FinanceAllocationNoteEditor },
  inject: ['page'],
  props: { reviewType: { type: String, default: 'expense' } },
  data() { return { rows: [], selectedId: null, selectedIds: [], pageNumber: 1, pageSize: 5, totalRows: 0, totalPages: 1, loading: false, errorMessage: '', serial: 0, approving: true, decisionDate: todayIsoDate(), batchDate: todayIsoDate(), today: todayIsoDate(), note: '', batchNote: '财务资料与付款凭证核对正确', batchReference: '', reopenNote: '', batchReopenNote: '', saving: false, batchBusy: false, actionError: '' }; },
  computed: {
    history() { return this.page.adminFinanceViewMode === 'history'; },
    selectedRow() { return this.rows.find(x => x.id === this.selectedId) || this.rows[0] || null; },
    pendingRows() { return this.rows.filter(row => row.confirmationStatus === 'pending'); },
    selectableRows() { return this.history ? this.rows : this.pendingRows; },
    batchRows() { return this.rows.filter(row => this.selectedIds.includes(row.id)); },
    batchTotal() { return this.batchRows.reduce((sum, row) => sum + Number(row.amount || 0), 0); },
    allSelectableSelected() { return this.selectableRows.length > 0 && this.selectableRows.every(row => this.selectedIds.includes(row.id)); },
    selectedReopenIds() { return this.rows.filter(row => this.selectedIds.includes(row.id) && row.confirmationStatus === 'confirmed').map(row => row.id); },
    batchNonce() { return this.page.adminFinanceBatchNonce; },
    sectionName() { return this.reviewType === 'reserve_refund' ? '预备金返还' : this.reviewType === 'tenant_deposit' ? '租客押金' : this.reviewType === 'cashflow_maintenance' ? '收支与维修' : '租客费用'; },
    sectionIcon() { return this.reviewType === 'reserve_refund' ? '返' : this.reviewType === 'tenant_deposit' ? '押' : this.reviewType === 'cashflow_maintenance' ? '维' : '费'; },
    listTitle() { return this.history ? `${this.sectionName}历史` : `待确认${this.sectionName}`; },
    detailTitle() { return `${this.sectionName}详情`; },
    confirmLabel() { return `确认${this.sectionName}`; }
  },
  watch: {
    'page.moduleSearch'() { this.reset(); }, 'page.globalSearch'() { this.reset(); }, 'page.projectFilter'() { this.reset(); }, 'page.statusFilter'() { this.reset(); }, 'page.adminFinanceViewMode'() { this.reset(); }, 'page.dateStart'() { this.reset(); }, 'page.dateEnd'() { this.reset(); }, pageSize() { this.reset(); }, batchNonce(v, p) { if (v > p) this.$nextTick(this.openBatch); }
  },
  mounted() { this.loadData(); },
  methods: {
    async loadData() {
      const serial = ++this.serial; this.loading = true; this.errorMessage = '';
      try {
        const params = { type: this.reviewType, page: this.pageNumber, pageSize: this.pageSize, keyword: this.page.globalSearch || this.page.moduleSearch || '', projectName: String(this.page.projectFilter || '').includes('全部') ? '' : this.page.projectFilter, status: this.history ? 'history' : 'pending', startDate: this.page.dateStart, endDate: this.page.dateEnd };
        const [response, projects] = await Promise.all([fetchAdminFinanceReviews(params), fetchAdminFinanceProjects(this.reviewType)]);
        if (serial !== this.serial) return;
        this.rows = response.rows || []; this.totalRows = response.page?.totalRows || 0; this.totalPages = response.page?.totalPages || 1; this.pageNumber = response.page?.page || 1; this.page.adminFinanceProjects = projects || []; this.page.adminFinanceMetrics = this.metrics(response.summary || {});
        if (!this.rows.some(x => x.id === this.selectedId)) this.selectedId = this.rows[0]?.id || null;
        this.selectedIds = this.selectedIds.filter(id => this.rows.some(row => row.id === id));
      } catch (e) { this.rows = []; this.errorMessage = e.message || 'API request failed'; }
      finally { if (serial === this.serial) this.loading = false; }
    },
    reset() { this.pageNumber = 1; this.selectedIds = []; this.loadData(); },
    goPage(n) { if (n >= 1 && n <= this.totalPages) { this.pageNumber = n; this.loadData(); } },
    documentUrl(row, type) { return row?.id ? getAdminFinanceDocumentUrl(row.id, type) : '#'; },
    async batchDownload(type) {
      const ids = [...this.selectedIds];
      if (!ids.length) { this.page.showToast('请先勾选需要下载的历史记录'); return; }
      this.batchBusy = true;
      try {
        const result = await downloadAdminFinanceDocuments(ids, type);
        const url = URL.createObjectURL(result.blob);
        const link = document.createElement('a'); link.href = url; link.download = result.filename; document.body.appendChild(link); link.click(); link.remove();
        window.setTimeout(() => URL.revokeObjectURL(url), 1000);
        this.page.showToast(this.$ltf`已生成 ${ids.length} 份${type === 'invoice' ? '发票' : '收据'}`);
      } catch (error) { this.page.showToast(error.message || '批量下载失败'); }
      finally { this.batchBusy = false; }
    },
    toggleSelectablePage(event) { const ids = this.selectableRows.map(row => row.id); this.selectedIds = event.target.checked ? [...new Set([...this.selectedIds, ...ids])] : this.selectedIds.filter(id => !ids.includes(id)); },
    openBatch() { if (!this.selectedIds.length) this.selectedIds = this.pendingRows.map(row => row.id); if (!this.selectedIds.length) { this.page.showToast('当前列表没有可确认记录'); return; } this.batchDate = todayIsoDate(); this.batchNote = '财务资料与付款凭证核对正确'; this.batchReference = ''; this.actionError = ''; this.$refs.batch?.showModal(); },
    async submitBatch() { if (!this.batchNote || !this.batchDate) return; this.saving = true; this.actionError = ''; const count = this.selectedIds.length; try { await batchConfirmAdminFinanceReviews(this.selectedIds, this.batchDate, this.batchNote, this.batchReference); this.$refs.batch.close(); this.selectedIds = []; await this.loadData(); this.page.showToast(this.$ltf`已批量确认 ${count} 笔财务记录`); } catch (e) { this.actionError = e.message || '批量确认失败'; } finally { this.saving = false; } },
    openRowDecision(row, ok) { this.selectedId = row.id; this.$nextTick(() => this.openDecision(ok)); },
    openDecision(ok) { this.approving = ok; this.decisionDate = todayIsoDate(); this.note = ok ? '費用資料核對正確' : ''; this.actionError = ''; this.$refs.decision?.showModal(); },
    openDetails(row) { this.selectedId = row.id; this.$nextTick(() => this.$refs.details?.showModal()); },
    closeDetails() { this.$refs.details?.close(); },
    decisionFromDetails(ok) { this.closeDetails(); this.$nextTick(() => this.openDecision(ok)); },
    reopenFromDetails() { this.closeDetails(); this.$nextTick(this.openReopen); },
    async submitDecision() {
      if (!this.note) return; this.saving = true;
      try { if (this.approving) await confirmAdminFinanceReview(this.selectedRow.id, this.decisionDate, this.note); else await rejectAdminFinanceReview(this.selectedRow.id, this.note); this.$refs.decision.close(); await this.loadData(); this.page.showToast(this.approving ? this.$ltf`${this.sectionName}已确认` : this.$ltf`${this.sectionName}已退回`); }
      catch (e) { this.actionError = e.message || '處理失敗'; } finally { this.saving = false; }
    },
    openRowReopen(row) { this.selectedId = row.id; this.$nextTick(this.openReopen); },
    openReopen() { this.reopenNote = ''; this.actionError = ''; this.$refs.reopen?.showModal(); },
    async submitReopen() { if (!this.reopenNote) return; this.saving = true; this.actionError = ''; try { await reopenAdminFinanceReview(this.selectedRow.id, this.reopenNote); this.$refs.reopen.close(); await this.loadData(); this.page.showToast(this.$t('finance.reopenSuccess')); } catch (e) { this.actionError = e.message || this.$t('finance.reopenFailed'); } finally { this.saving = false; } },
    openBatchReopen() { if (!this.selectedReopenIds.length) { this.page.showToast('请先勾选已确认费用'); return; } this.batchReopenNote = ''; this.actionError = ''; this.$refs.batchReopen?.showModal(); },
    closeBatchReopen() { this.$refs.batchReopen?.close(); },
    async submitBatchReopen() { if (!this.batchReopenNote) { this.actionError = '请填写退回原因'; return; } const ids = [...this.selectedReopenIds]; this.saving = true; this.actionError = ''; try { await batchReopenAdminFinanceReviews(ids, this.batchReopenNote); this.closeBatchReopen(); this.selectedIds = []; await this.loadData(); this.page.showToast(this.$ltf`已批量退回 ${ids.length} 笔费用`); } catch (e) { this.actionError = e.message || '批量退回失败'; } finally { this.saving = false; } },
    metrics(s) { return [{ label: `待确认${this.sectionName}`, value: `${Number(s.pendingCount || 0)} 笔`, delta: `RM ${this.money(s.pendingAmount)}`, trend: Number(s.pendingCount) ? 'down' : 'up' }, { label: `已确认${this.sectionName}`, value: `${Number(s.confirmedCount || 0)} 笔`, delta: '保留历史金额', trend: 'up' }, { label: `已退回${this.sectionName}`, value: `${Number(s.rejectedCount || 0)} 笔`, delta: '等待调整', trend: '' }, { label: '待同步 SQL', value: `${Number(s.pendingSyncCount || 0)} 笔`, delta: '会计同步', trend: '' }, { label: `本月确认${this.sectionName}`, value: `RM ${this.money(s.confirmedMonthAmount)}`, delta: '本月完成', trend: 'up' }]; },
    categoryLabel(v) { return ({ management: '管理费', utilities: '水电费', maintenance: '维修转收费', other: '其他杂费', service_fee: '服务费', insurance: '保险', tax: '税费', deposit: '租客押金', deposit_refund: '租客押金退款', deposit_forfeiture: '租客押金没收' })[v] || v || '其他费用'; },
    statusLabel(v) { return ({ pending: '待確認', confirmed: '已確認', rejected: '已退回' })[v] || v; },
    statusClass(v) { return v === 'confirmed' ? 'green' : v === 'rejected' ? 'red' : 'orange'; },
    displayDate(value) { return formatDate(value); },
    money(v) { return Number(v || 0).toLocaleString('en-MY', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
  }
};
</script>

<style scoped>
.expense-review-layout{display:block;margin:0 28px;min-height:560px}
.expense-review-list{width:100%;min-width:0}
.expense-review-list small{display:block;margin-top:3px;color:#718096}
.expense-review-list th{font-size:13px}.expense-review-list td{font-size:14px}.expense-review-list th:last-child,.expense-review-list td:last-child{width:112px;text-align:right}
.expense-batch-actions{display:flex;align-items:center;justify-content:flex-end;gap:8px;flex-wrap:wrap}.expense-batch-actions>span{color:#66788b;font-size:12px}.expense-batch-actions button{min-height:34px;padding:0 12px;border:1px solid #9bc9d0;border-radius:8px;background:#fff;color:#087078;font-size:12px;font-weight:700;cursor:pointer}.expense-batch-actions button.danger{border-color:#efb6bb;background:#fff7f7;color:#bf2f3b}.expense-batch-actions button:disabled{opacity:.45;cursor:not-allowed}
.expense-row-actions{display:flex;align-items:center;justify-content:flex-end;gap:6px;min-width:166px;white-space:nowrap}
.expense-row-actions button,.expense-row-actions a{display:inline-flex;align-items:center;justify-content:center;min-height:31px;padding:0 10px;border:1px solid #cfdee5;border-radius:8px;background:#fff;color:#31566d;font-size:11px;font-weight:700;text-decoration:none;cursor:pointer}
.expense-row-actions .confirm{border-color:#078b8d;background:#078b8d;color:#fff}
.expense-row-actions .confirm:hover{background:#06767a}
.expense-row-actions .reject{border-color:#efc8cc;background:#fff8f8;color:#c83d48}
.expense-row-actions .detail:hover{border-color:#77bfc2;background:#eff9f8;color:#087078}
.expense-row-actions a{border-color:#b7cfee;background:#f7fbff;color:#185a97}
.expense-title{display:flex;align-items:center;gap:12px}
.expense-title>span{display:grid;width:46px;height:46px;place-items:center;border-radius:14px;background:#e9f8f7;color:#078b8d;font-size:20px;font-weight:700}
.expense-title h3,.expense-title p{margin:0}
.expense-title em{margin-left:auto;font-style:normal}
.expense-amount{margin:24px 0;color:#073b67;font-size:28px;font-weight:700}
.expense-detail-dialog{width:min(620px,calc(100vw - 40px));padding:0;overflow:hidden}
.expense-detail-body{max-height:calc(100vh - 180px);padding:22px;overflow:auto}
.expense-detail-body dl{display:grid;grid-template-columns:100px minmax(0,1fr);gap:14px;margin:0;border-top:1px solid #e4ebf3;padding-top:18px}
.expense-detail-body dt{color:#708096}
.expense-detail-body dd{min-width:0;margin:0;overflow-wrap:anywhere;font-weight:600}
.finance-document-detail-actions{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px;margin-top:20px}
.finance-document-detail-actions a,.finance-document-detail-actions button{display:inline-flex;align-items:center;justify-content:center;min-height:42px;padding:0 14px;border:1px solid #b7cfee;border-radius:9px;background:#f7fbff;color:#185a97;font-size:13px;font-weight:700;text-decoration:none;cursor:pointer}.finance-document-detail-actions .confirm{border-color:#078b8d;background:#078b8d;color:#fff}.finance-document-detail-actions .danger{border-color:#efb6bb;background:#fff5f5;color:#bf2f3b}
@media(max-width:900px){.expense-review-layout{margin:0 14px}.expense-row-actions{min-width:0}.expense-review-list th:last-child,.expense-review-list td:last-child{width:auto}}
</style>
<style scoped>
.finance-document-button{display:inline-flex;align-items:center;justify-content:center;min-height:26px;padding:0 7px;border:1px solid #b7cfee;border-radius:6px;background:#f7fbff;color:#185a97;font-size:10px;text-decoration:none;white-space:nowrap}.finance-document-button:hover{background:#eaf3ff;border-color:#6f9fda}
.finance-batch-overview{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}.finance-batch-overview span{display:grid;gap:5px;padding:13px;border:1px solid #cfe4e5;border-radius:9px;background:#f1faf9;color:#66788b;font-size:12px}.finance-batch-overview b{color:#073b67;font-size:18px}
</style>
