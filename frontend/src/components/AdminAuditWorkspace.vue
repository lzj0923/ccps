<template>
  <section class="audit-workspace">
    <div class="panel audit-panel">
      <header class="audit-head">
        <div><h2>{{ $t('legacy.t_2d1f7e34bbb4') }}</h2><p>{{ $t('legacy.t_d4dccdf4e913') }}</p></div>
        <button class="audit-refresh" type="button" :disabled="loading" @click="loadRecords">{{ $t('legacy.t_0c9157b5bfac') }}</button>
      </header>

      <form class="audit-filters" @submit.prevent="applyFilters">
        <label>{{ $t('legacy.t_7fd7a227e9ce') }}<input v-model="filters.startDate" type="date"></label>
        <label>{{ $t('legacy.t_27eefa5237a0') }}<input v-model="filters.endDate" type="date"></label>
        <label>{{ $t('legacy.t_00b52dc2284c') }}<select v-model="filters.actorId"><option value="">{{ $t('legacy.t_5da3e85f74ef') }}</option><option v-for="actor in options.actors" :key="actor.id" :value="String(actor.id)">{{ actor.name }}</option></select></label>
        <label>{{ $t('legacy.t_4bb42f343241') }}<select v-model="filters.action"><option value="">{{ $t('legacy.t_1e97126f5862') }}</option><option v-for="action in options.actions" :key="action" :value="action">{{ actionLabel(action) }}</option></select></label>
        <label class="keyword">{{ $t('legacy.t_2990e991a238') }}<input v-model.trim="filters.keyword" type="search" :placeholder="$t('legacy.t_f166a87b37e6')"></label>
        <div class="audit-filter-actions"><button class="audit-primary" type="submit">{{ $t('legacy.t_505ba2176546') }}</button><button class="audit-secondary" type="button" @click="resetFilters">{{ $t('legacy.t_7b15e5e8e7bd') }}</button></div>
      </form>

      <div v-if="errorMessage" class="audit-state error"><strong>{{ $t('legacy.t_4a5a291d9b7e') }}</strong><span>{{ errorMessage }}</span></div>
      <div v-else-if="loading" class="audit-state">{{ $t('legacy.t_5d5be43e065a') }}</div>
      <div v-else class="audit-table-wrap">
        <table>
          <thead><tr><th>{{ $t('legacy.t_4aa6a9cca401') }}</th><th>{{ $t('legacy.t_00b52dc2284c') }}</th><th>{{ $t('legacy.t_ddbe15e49ead') }}</th><th>{{ $t('legacy.t_c5e60f20ce3b') }}</th><th>{{ $t('legacy.t_c43a7f541fa6') }}</th><th>{{ $t('legacy.t_5b42873f8d8a') }}</th></tr></thead>
          <tbody>
            <template v-for="item in records" :key="item.id">
              <tr :class="{ selected: expandedId === item.id }">
                <td>{{ formatDateTime(item.createdAt) }}</td><td>{{ item.actorName || $t('legacy.t_4a4dae52e9fa') }}</td><td><span class="audit-action">{{ actionLabel(item.action) }}</span></td><td>{{ entityLabel(item.entityType) }}</td><td>{{ item.entityId || '—' }}</td>
                <td><button class="audit-detail" type="button" @click="toggleDetail(item.id)">{{ expandedId === item.id ? $t('legacy.t_5d5815647c76') : $t('legacy.t_f7acefd2d4cd') }}</button></td>
              </tr>
              <tr v-if="expandedId === item.id" class="audit-detail-row"><td colspan="6"><div class="audit-detail-grid"><div><h3>{{ $t('legacy.t_c9a559451d96') }}</h3><dl><template v-for="field in snapshotFields(item.beforeData)" :key="field.key"><dt>{{ field.label }}</dt><dd>{{ field.value }}</dd></template><dd v-if="!snapshotFields(item.beforeData).length">{{ $t('legacy.t_eb848bc1b8e6') }}</dd></dl></div><div><h3>{{ $t('legacy.t_377ea3bf938a') }}</h3><dl><template v-for="field in snapshotFields(item.afterData)" :key="field.key"><dt>{{ field.label }}</dt><dd>{{ field.value }}</dd></template><dd v-if="!snapshotFields(item.afterData).length">{{ $t('legacy.t_ba1a35dd3d86') }}</dd></dl></div></div></td></tr>
            </template>
            <tr v-if="!records.length"><td colspan="6" class="audit-empty">{{ $t('legacy.t_d47596765994') }}</td></tr>
          </tbody>
        </table>
      </div>
      <footer v-if="page.total > page.pageSize" class="audit-pager"><span>{{ $t('legacy.t_3b6ef811b85a') }} {{ page.total }} {{ $t('legacy.t_d0bb9b2b8ea7') }}</span><div><button class="audit-secondary" type="button" :disabled="page.page <= 1" @click="goPage(page.page - 1)">{{ $t('legacy.t_d1bf40b0d2ce') }}</button><b>{{ page.page }} / {{ page.totalPages }}</b><button class="audit-secondary" type="button" :disabled="page.page >= page.totalPages" @click="goPage(page.page + 1)">{{ $t('legacy.t_d44baa5198d4') }}</button></div></footer>
    </div>
  </section>
</template>

<script>
import { fetchAdminAudit, fetchAdminAuditOptions } from '../services/propertyApi';
const fieldLabels = { vendorCode: '服務商編號', name: '名稱', contactName: '聯絡人', phone: '電話', email: 'Email', status: '狀態', leaseCode: '租約編號', startDate: '租期開始', endDate: '租期結束', monthlyRent: '月租', tenantName: '租客', unitNo: '單位', amount: '金額', paymentDate: '收款日期' };
const actionLabels = {
  admin_direct_reserve_topup: '新增預備金充值', complete_electronic_signature: '完成電子簽署', complete_handover: '完成交屋報告', complete_maintenance: '完成維修訂單',
  confirm_property_payment: '確認房產付款', confirm_rent_collection: '確認租金收款', confirm_reserve_refund: '確認預備金退款', confirm_reserve_topup: '確認預備金充值',
  create: '新增資料', create_accounting_export: '新增會計匯出', create_expense: '新增房產費用', create_maintenance: '新增維修訂單', create_reminder_rule: '新增提醒規則',
  create_reserve_refund: '新增預備金退款', delete: '刪除資料', generate_report: '產生報表', generate_test_lease_contract: '產生測試租約合同',
  recalculate_daily_prorated_rent: '重算按日折算租金', reject_property_payment: '退回房產付款', start_electronic_signature: '發起電子簽署',
  update_lease: '修改租約', transfer_lease: '轉租', create_vendor: '新增服務商', update_vendor: '修改服務商', deactivate_vendor: '停用服務商',
  upload_rent_payment_proof: '上傳付款憑證', delete_rent_payment_proof: '刪除付款憑證', update_property: '修改房產資料', update_owner: '修改業主資料',
  create_cashflow: '新增收支記錄', update_cashflow: '修改收支記錄', delete_cashflow: '刪除收支記錄', create_contract: '新增合同', update_contract: '修改合同', delete_contract: '刪除合同'
};
const entityLabels = { lease: '租約', vendor: '服務商', rent_invoice: '租金帳單', rent_payment: '租金收款', payment_proof: '付款憑證' };
export default {
  name: 'AdminAuditWorkspace',
  data() { return { records: [], options: { actors: [], actions: [] }, filters: { startDate: '', endDate: '', actorId: '', action: '', keyword: '' }, page: { total: 0, page: 1, pageSize: 20, totalPages: 1 }, loading: false, errorMessage: '', expandedId: null }; },
  mounted() { this.loadOptions(); this.loadRecords(); },
  methods: {
    actionLabel(action) { return actionLabels[action] || (action ? `其他操作：${String(action).replaceAll('_', ' ')}` : '系統操作'); },
    entityLabel(type) { return entityLabels[type] || type || '—'; },
    formatDateTime(value) { if (!value) return '—'; return String(value).replace('T', ' ').slice(0, 16); },
    snapshotFields(raw) { if (!raw) return []; try { const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw; if (!parsed || typeof parsed !== 'object') return [{ key: 'raw', label: '內容', value: String(raw) }]; return Object.entries(parsed).filter(([, value]) => value !== null && value !== undefined && value !== '').map(([key, value]) => ({ key, label: fieldLabels[key] || key, value: this.valueLabel(key, value) })); } catch { return [{ key: 'raw', label: '內容', value: String(raw) }]; } },
    valueLabel(key, value) { if (key === 'status') return value === 'active' ? '啟用' : value === 'inactive' ? '停用' : String(value); return String(value); },
    async loadOptions() { try { this.options = await fetchAdminAuditOptions(); } catch { this.options = { actors: [], actions: [] }; } },
    async loadRecords() { this.loading = true; this.errorMessage = ''; try { const data = await fetchAdminAudit({ ...this.filters, page: this.page.page, pageSize: this.page.pageSize }); this.records = data.items || []; this.page = data.page || this.page; this.expandedId = null; } catch (error) { this.errorMessage = error.message || '無法讀取操作紀錄'; } finally { this.loading = false; } },
    applyFilters() { this.page.page = 1; this.loadRecords(); },
    resetFilters() { this.filters = { startDate: '', endDate: '', actorId: '', action: '', keyword: '' }; this.applyFilters(); },
    goPage(nextPage) { this.page.page = nextPage; this.loadRecords(); },
    toggleDetail(id) { this.expandedId = this.expandedId === id ? null : id; }
  }
};
</script>

<style scoped>
.audit-workspace{padding:0 2px 28px}.audit-panel{overflow:hidden}.audit-head{display:flex;justify-content:space-between;align-items:center;gap:20px;padding:22px 26px;border-bottom:1px solid #e5edf4}.audit-head h2{margin:0;color:#153f70;font-size:22px}.audit-head p{margin:6px 0 0;color:#71859a;font-size:13px}.audit-filters{display:flex;flex-wrap:wrap;align-items:end;gap:12px;padding:16px 26px;background:#f7fafc}.audit-filters label{display:grid;gap:6px;color:#637c93;font-size:12px}.audit-filters input,.audit-filters select{min-width:134px;border:1px solid #c8d8e8;border-radius:7px;padding:9px 10px;color:#23415f;background:#fff}.audit-filters .keyword input{min-width:245px}.audit-filter-actions{display:flex;gap:8px}.audit-primary,.audit-secondary,.audit-refresh,.audit-detail{border:1px solid #c8d8e8;border-radius:8px;padding:9px 14px;background:#fff;color:#144579;cursor:pointer}.audit-primary{background:#f3b500;border-color:#e2a500;color:#fff}.audit-refresh:disabled,.audit-secondary:disabled{opacity:.5;cursor:not-allowed}.audit-state{padding:54px;text-align:center;color:#71859a}.audit-state.error{color:#b64040;display:grid;gap:6px}.audit-table-wrap{padding:0 18px;overflow:auto}.audit-table-wrap table{width:100%;border-collapse:collapse;min-width:760px}.audit-table-wrap th,.audit-table-wrap td{padding:13px 10px;text-align:left;border-bottom:1px solid #e6eef5;font-size:13px}.audit-table-wrap th{color:#6e8398;background:#fbfdff}.audit-table-wrap tbody tr.selected td{background:#fffaf0}.audit-action{display:inline-block;border-radius:999px;padding:4px 9px;color:#14558c;background:#eaf4ff;font-size:12px}.audit-detail{padding:6px 11px;font-size:12px}.audit-detail-row td{padding:0 10px 16px;background:#fffaf0}.audit-detail-grid{display:grid;grid-template-columns:1fr 1fr;gap:16px;padding:16px;border:1px solid #f2dfb7;border-radius:8px;background:#fff}.audit-detail-grid h3{margin:0 0 10px;color:#365d83;font-size:13px}.audit-detail-grid dl{display:grid;grid-template-columns:110px 1fr;gap:7px;margin:0;font-size:12px}.audit-detail-grid dt{color:#7990a5}.audit-detail-grid dd{margin:0;color:#233f5c;word-break:break-word}.audit-empty{text-align:center;color:#8b9bae;padding:40px!important}.audit-pager{display:flex;align-items:center;justify-content:space-between;padding:16px 26px;color:#71859a;font-size:13px}.audit-pager>div{display:flex;align-items:center;gap:10px}.audit-pager b{color:#345878}@media(max-width:760px){.audit-head{align-items:flex-start;flex-direction:column}.audit-filters{align-items:stretch}.audit-filters label,.audit-filters input,.audit-filters select,.audit-filters .keyword input{width:100%;min-width:0}.audit-detail-grid{grid-template-columns:1fr}.audit-pager{align-items:flex-start;flex-direction:column;gap:12px}}
</style>
