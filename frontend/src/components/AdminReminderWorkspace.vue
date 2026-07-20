<template>
  <section class="reminder-layout">
    <div class="panel reminder-main">
      <div class="reminder-head">
        <div><h2>提醒規則與發送記錄</h2><span>資料庫即時資料 · 排程每小時執行</span></div>
        <div class="reminder-tabs">
          <button :class="{ active: activeTab === 'rules' }" @click="activeTab = 'rules'">提醒規則 <b>{{ rules.length }}</b></button>
          <button :class="{ active: activeTab === 'notifications' }" @click="activeTab = 'notifications'">通知記錄 <b>{{ notifications.length }}</b></button>
          <button :class="{ active: activeTab === 'deliveries' }" @click="activeTab = 'deliveries'">發送結果 <b>{{ deliveries.length }}</b></button>
        </div>
      </div>

      <div v-if="loading" class="reminder-state">正在載入自動提醒資料…</div>
      <div v-else-if="errorMessage" class="reminder-state error"><strong>提醒資料載入失敗</strong><span>{{ errorMessage }}</span><button @click="loadData">重新載入</button></div>

      <div v-else-if="activeTab === 'rules'" class="table-wrap reminder-table-wrap">
        <table>
          <thead><tr><th>規則名稱</th><th>提醒場景</th><th>觸發時間</th><th>通知方式</th><th>狀態</th><th>最近更新</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="rule in filteredRules" :key="rule.id" :class="{ selected: selectedRuleId === rule.id }" @click="selectedRuleId = rule.id">
              <td><strong>{{ rule.name }}</strong><small>{{ rule.code }}</small></td>
              <td>{{ eventLabel(rule.eventType) }}</td>
              <td>{{ triggerLabel(rule) }}</td>
              <td><span v-for="channel in rule.channels" :key="channel" class="channel-chip">{{ channelLabel(channel) }}</span></td>
              <td><span class="reminder-tag" :class="rule.enabled ? 'enabled' : 'disabled'">{{ rule.enabled ? '啟用' : '停用' }}</span></td>
              <td>{{ dateTime(rule.updatedAt) }}</td>
              <td class="row-buttons"><button @click.stop="openEdit(rule)">編輯</button><button :disabled="runningId === rule.id" @click.stop="runRule(rule)">{{ runningId === rule.id ? '執行中' : '立即執行' }}</button></td>
            </tr>
            <tr v-if="!filteredRules.length"><td colspan="7" class="empty-cell">尚未建立符合條件的提醒規則</td></tr>
          </tbody>
        </table>
      </div>

      <div v-else-if="activeTab === 'notifications'" class="table-wrap reminder-table-wrap">
        <table>
          <thead><tr><th>建立時間</th><th>提醒內容</th><th>收件人</th><th>規則</th><th>優先級</th><th>管道狀態</th><th>失敗原因</th></tr></thead>
          <tbody>
            <tr v-for="item in filteredNotifications" :key="item.id">
              <td>{{ dateTime(item.createdAt) }}</td><td><strong>{{ item.title }}</strong><small>{{ item.body }}</small></td>
              <td><strong>{{ item.recipientName || '未綁定業主' }}</strong><small>{{ item.recipientEmail || '無郵箱' }}</small></td>
              <td>{{ item.ruleName }}</td><td><span class="reminder-tag" :class="priorityClass(item.priority)">{{ priorityLabel(item.priority) }}</span></td>
              <td>{{ deliveryLabel(item.deliverySummary) }}</td><td class="failure-cell">{{ item.failureReason || '—' }}</td>
            </tr>
            <tr v-if="!filteredNotifications.length"><td colspan="7" class="empty-cell">尚無通知記錄</td></tr>
          </tbody>
        </table>
      </div>

      <div v-else class="table-wrap reminder-table-wrap">
        <table>
          <thead><tr><th>通知</th><th>收件人</th><th>管道</th><th>目的地</th><th>狀態</th><th>嘗試次數</th><th>處理時間／原因</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="delivery in filteredDeliveries" :key="delivery.id">
              <td><strong>{{ delivery.title }}</strong><small>{{ delivery.ruleName }}</small></td><td>{{ delivery.recipientName || '—' }}</td>
              <td>{{ channelLabel(delivery.channel) }}</td><td>{{ delivery.destination || '站內通知' }}</td>
              <td><span class="reminder-tag" :class="delivery.status">{{ deliveryStatusLabel(delivery.status) }}</span></td><td>{{ delivery.attemptCount }}</td>
              <td><strong>{{ dateTime(delivery.sentAt || delivery.failedAt) }}</strong><small class="failure-cell">{{ delivery.failureReason || '—' }}</small></td>
              <td><button v-if="delivery.status === 'failed' && delivery.channel === 'email'" class="retry-button" @click="retryDelivery(delivery)">重試</button><span v-else>—</span></td>
            </tr>
            <tr v-if="!filteredDeliveries.length"><td colspan="8" class="empty-cell">尚無發送結果</td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <aside class="panel reminder-detail">
      <template v-if="selectedRule">
        <div class="rule-title"><div class="rule-icon">{{ eventIcon(selectedRule.eventType) }}</div><div><h3>{{ selectedRule.name }}</h3><p>{{ selectedRule.code }}</p></div><span class="reminder-tag" :class="selectedRule.enabled ? 'enabled' : 'disabled'">{{ selectedRule.enabled ? '啟用' : '停用' }}</span></div>
        <div class="detail-actions"><button @click="openEdit(selectedRule)">編輯規則</button><button @click="toggleRule(selectedRule)">{{ selectedRule.enabled ? '停用' : '啟用' }}</button></div>
        <section class="rule-section"><h4>規則設定</h4><dl><div><dt>提醒場景</dt><dd>{{ eventLabel(selectedRule.eventType) }}</dd></div><div><dt>觸發條件</dt><dd>{{ triggerLabel(selectedRule) }}</dd></div><div><dt>接收對象</dt><dd>業主</dd></div><div><dt>通知方式</dt><dd>{{ selectedRule.channels.map(channelLabel).join('、') }}</dd></div></dl></section>
        <section class="rule-section"><h4>執行說明</h4><p>系統每小時掃描一次符合條件的房款、租金、租約、預備金或文件資料。同一規則及業務記錄不會重複建立到期通知。</p></section>
        <section class="rule-section danger-zone"><h4>規則管理</h4><button @click="deleteRule(selectedRule)">刪除未使用規則</button><small>已有通知歷史的規則只能停用，以保留稽核記錄。</small></section>
      </template>
      <div v-else class="reminder-state">選擇一條規則查看詳細設定</div>
    </aside>

    <dialog ref="ruleDialog" class="rule-dialog">
      <form @submit.prevent="saveRule">
        <header><div><h3>{{ editingId ? '編輯提醒規則' : '新增提醒規則' }}</h3><p>規則會直接儲存到資料庫並由後端排程執行。</p></div><button type="button" @click="closeDialog">×</button></header>
        <div class="rule-form">
          <label>規則名稱<input v-model.trim="form.name" maxlength="160" required placeholder="例如：房款到期前 7 天"></label>
          <label>規則代碼<input v-model.trim="form.code" maxlength="80" required pattern="[A-Za-z0-9_-]+" placeholder="PAYMENT_DUE_7D"></label>
          <div class="form-grid"><label>提醒場景<select v-model="form.eventType"><option v-for="option in eventOptions" :key="option.value" :value="option.value">{{ option.label }}</option></select></label><label>提前天數<input v-model.number="form.daysBefore" type="number" min="0" max="365" required></label></div>
          <fieldset><legend>通知方式</legend><label v-for="channel in channelOptions" :key="channel.value" class="check-label"><input v-model="form.channels" type="checkbox" :value="channel.value"><span><b>{{ channel.label }}</b><small>{{ channel.hint }}</small></span></label></fieldset>
          <label class="enabled-toggle"><input v-model="form.enabled" type="checkbox"><span><b>建立後立即啟用</b><small>停用規則仍會保留既有通知及發送記錄。</small></span></label>
          <p v-if="formError" class="form-error">{{ formError }}</p>
        </div>
        <menu><button type="button" @click="closeDialog">取消</button><button class="save-button" :disabled="saving">{{ saving ? '儲存中…' : '儲存規則' }}</button></menu>
      </form>
    </dialog>
  </section>
</template>

<script>
import { createAdminReminderRule, deleteAdminReminderRule, fetchAdminReminders, retryAdminReminderDelivery, runAdminReminderRule, runAllAdminReminderRules, toggleAdminReminderRule, updateAdminReminderRule } from '../services/propertyApi';

const blankForm = () => ({ code: '', name: '', eventType: 'payment_due', daysBefore: 7, channels: ['in_app'], recipientRole: 'owner', enabled: true });

export default {
  inject: ['page'],
  data() {
    return {
      activeTab: 'rules', loading: true, errorMessage: '', rules: [], notifications: [], deliveries: [], selectedRuleId: null,
      editingId: null, form: blankForm(), formError: '', saving: false, runningId: null,
      eventOptions: [{ value: 'payment_due', label: '房款到期' }, { value: 'rent_due', label: '租金到期' }, { value: 'lease_expiry', label: '租約到期' }, { value: 'reserve_low', label: '預備金不足' }, { value: 'document_expiry', label: '文件到期' }],
      channelOptions: [{ value: 'in_app', label: '站內通知', hint: '顯示於業主通知中心' }, { value: 'email', label: 'Email', hint: '需業主完成郵箱驗證' }, { value: 'line', label: 'LINE', hint: '尚未設定時會記錄失敗原因' }]
    };
  },
  computed: {
    selectedRule() { return this.rules.find(rule => rule.id === this.selectedRuleId) || this.filteredRules[0] || null; },
    keyword() { return String(this.page.globalSearch || this.page.moduleSearch || '').trim().toLowerCase(); },
    filteredRules() { const status = this.page.statusFilter; return this.rules.filter(rule => (!this.keyword || `${rule.name} ${rule.code} ${this.eventLabel(rule.eventType)}`.toLowerCase().includes(this.keyword)) && (!status || status.includes('全部') || (status === '啟用' && rule.enabled) || (status === '停用' && !rule.enabled))); },
    filteredNotifications() { return this.notifications.filter(item => !this.keyword || `${item.title} ${item.body} ${item.recipientName || ''} ${item.ruleName || ''}`.toLowerCase().includes(this.keyword)); },
    filteredDeliveries() { const status = this.page.statusFilter; return this.deliveries.filter(item => (!this.keyword || `${item.title} ${item.recipientName || ''} ${item.destination || ''}`.toLowerCase().includes(this.keyword)) && (!status || status.includes('全部') || (status === '待發送' && item.status === 'pending') || (status === '發送失敗' && item.status === 'failed'))); },
    createNonce() { return this.page.adminReminderCreateNonce; }, runNonce() { return this.page.adminReminderRunNonce; }
  },
  watch: {
    createNonce(value, previous) { if (value > previous) this.openCreate(); },
    runNonce(value, previous) { if (value > previous) this.runAll(); },
    filteredRules(rows) { if (!rows.some(row => row.id === this.selectedRuleId)) this.selectedRuleId = rows[0]?.id || null; },
    activeTab(value) { if (value === 'deliveries' && this.page.statusFilter === '啟用') this.page.statusFilter = '全部狀態'; }
  },
  mounted() { this.page.projectFilter = '全部提醒'; this.page.statusFilter = '全部狀態'; this.loadData(); },
  methods: {
    async loadData() {
      this.loading = true; this.errorMessage = '';
      try {
        const response = await fetchAdminReminders();
        this.rules = response.rules || []; this.notifications = response.notifications || []; this.deliveries = response.deliveries || [];
        if (!this.rules.some(rule => rule.id === this.selectedRuleId)) this.selectedRuleId = this.rules[0]?.id || null;
        this.setMetrics(response.summary || {}); this.setExportRows();
      } catch (error) { this.errorMessage = error.message || '自動提醒資料讀取失敗'; this.page.adminReminderMetrics = null; }
      finally { this.loading = false; }
    },
    setMetrics(summary) {
      this.page.adminReminderMetrics = [
        { label: '提醒規則', value: `${Number(summary.ruleCount || 0)} 條`, delta: `${Number(summary.enabledRuleCount || 0)} 條啟用`, trend: 'up' },
        { label: '已建立通知', value: `${Number(summary.notificationCount || 0)} 則`, delta: '資料庫通知記錄', trend: 'up' },
        { label: '待發送', value: `${Number(summary.pendingDeliveryCount || 0)} 則`, delta: '等待管道處理', trend: Number(summary.pendingDeliveryCount) ? 'down' : 'up' },
        { label: '發送失敗', value: `${Number(summary.failedDeliveryCount || 0)} 則`, delta: '可查看原因或重試', trend: Number(summary.failedDeliveryCount) ? 'down' : 'up' }
      ];
    },
    setExportRows() { this.page.adminReminderExportHeaders = ['時間', '規則', '標題', '收件人', '郵箱', '優先級', '通知狀態', '管道狀態', '失敗原因']; this.page.adminReminderExportRows = this.notifications.map(item => [this.dateTime(item.createdAt), item.ruleName, item.title, item.recipientName || '', item.recipientEmail || '', this.priorityLabel(item.priority), item.noticeStatus, this.deliveryLabel(item.deliverySummary), item.failureReason || '']); },
    openCreate() { this.editingId = null; this.form = blankForm(); this.formError = ''; this.$refs.ruleDialog?.showModal(); },
    openEdit(rule) { this.editingId = rule.id; this.form = { code: rule.code, name: rule.name, eventType: rule.eventType, daysBefore: rule.daysBefore, channels: [...rule.channels], recipientRole: rule.recipientRole || 'owner', enabled: rule.enabled }; this.formError = ''; this.$refs.ruleDialog?.showModal(); },
    closeDialog() { if (!this.saving) this.$refs.ruleDialog?.close(); },
    async saveRule() {
      if (!this.form.channels.length) { this.formError = '請至少選擇一種通知方式'; return; }
      this.saving = true; this.formError = '';
      try { const result = this.editingId ? await updateAdminReminderRule(this.editingId, this.form) : await createAdminReminderRule(this.form); this.selectedRuleId = result.id; this.$refs.ruleDialog?.close(); await this.loadData(); this.page.showToast(this.editingId ? '提醒規則已更新' : '提醒規則已建立'); }
      catch (error) { this.formError = error.message || '提醒規則儲存失敗'; }
      finally { this.saving = false; }
    },
    async toggleRule(rule) { try { await toggleAdminReminderRule(rule.id, !rule.enabled); await this.loadData(); this.page.showToast(rule.enabled ? '提醒規則已停用' : '提醒規則已啟用'); } catch (error) { this.page.showToast(error.message || '規則狀態更新失敗'); } },
    async deleteRule(rule) { if (!window.confirm(`確定刪除「${rule.name}」？`)) return; try { await deleteAdminReminderRule(rule.id); await this.loadData(); this.page.showToast('提醒規則已刪除'); } catch (error) { this.page.showToast(error.message || '提醒規則刪除失敗'); } },
    async runRule(rule) { this.runningId = rule.id; try { const result = await runAdminReminderRule(rule.id); await this.loadData(); this.page.showToast(`已建立 ${Number(result.createdCount || 0)} 則通知`); } catch (error) { this.page.showToast(error.message || '提醒規則執行失敗'); } finally { this.runningId = null; } },
    async runAll() { try { const result = await runAllAdminReminderRules(); await this.loadData(); this.page.showToast(`全部規則執行完成，建立 ${Number(result.createdCount || 0)} 則通知`); } catch (error) { this.page.showToast(error.message || '批量執行失敗'); } },
    async retryDelivery(delivery) { try { await retryAdminReminderDelivery(delivery.id); await this.loadData(); this.page.showToast('郵件已加入重試佇列'); } catch (error) { this.page.showToast(error.message || '重試失敗'); } },
    eventLabel(value) { return ({ payment_due: '房款到期', rent_due: '租金到期', lease_expiry: '租約到期', reserve_low: '預備金不足', document_expiry: '文件到期' })[value] || value; },
    eventIcon(value) { return ({ payment_due: '款', rent_due: '租', lease_expiry: '約', reserve_low: '金', document_expiry: '件' })[value] || '醒'; },
    triggerLabel(rule) { return rule.eventType === 'reserve_low' ? '低於最低標準時' : rule.daysBefore ? `到期前 ${rule.daysBefore} 天` : '到期當天／已逾期'; },
    channelLabel(value) { return ({ in_app: '站內', email: 'Email', line: 'LINE' })[value] || value; },
    priorityLabel(value) { return ({ urgent: '緊急', high: '高', normal: '一般', low: '低' })[value] || value; },
    priorityClass(value) { return value === 'urgent' ? 'failed' : value === 'high' ? 'pending' : 'sent'; },
    deliveryStatusLabel(value) { return ({ sent: '已發送', pending: '待發送', failed: '失敗' })[value] || value; },
    deliveryLabel(value) { return String(value || 'in_app:sent').split(',').map(part => { const [channel, status] = part.split(':'); return `${this.channelLabel(channel)} ${this.deliveryStatusLabel(status)}`; }).join('、'); },
    dateTime(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '—'; }
  }
};
</script>

<style scoped>
.reminder-layout{display:grid;grid-template-columns:minmax(0,1fr) 340px;gap:14px;min-height:570px}.reminder-main,.reminder-detail{min-width:0}.reminder-head{display:flex;align-items:flex-end;justify-content:space-between;gap:18px;padding:16px 18px 0}.reminder-head h2{margin:0 0 5px;font-size:18px}.reminder-head>div>span{color:#718096;font-size:12px}.reminder-tabs{display:flex;gap:4px}.reminder-tabs button{border:0;border-bottom:2px solid transparent;background:transparent;padding:9px 10px;color:#64748b}.reminder-tabs button.active{border-color:#d59200;color:#0b315f;font-weight:700}.reminder-tabs b{display:inline-grid;place-items:center;min-width:18px;height:18px;margin-left:4px;border-radius:10px;background:#eef2f7;font-size:10px}.reminder-table-wrap{margin-top:10px;overflow:auto}.reminder-table-wrap table{min-width:930px}.reminder-table-wrap tbody tr{cursor:pointer}.reminder-table-wrap tbody tr.selected{background:#fff8e8}.reminder-table-wrap td strong,.reminder-table-wrap td small{display:block}.reminder-table-wrap td small{max-width:420px;margin-top:3px;color:#718096;font-size:11px;white-space:normal}.channel-chip{display:inline-block;margin:2px;padding:3px 7px;border-radius:10px;background:#edf3fa;color:#24557e;font-size:11px}.reminder-tag{display:inline-flex;padding:4px 8px;border:1px solid;border-radius:6px;font-size:11px;white-space:nowrap}.reminder-tag.enabled,.reminder-tag.sent{color:#18864a;background:#eef9f2;border-color:#bde6cb}.reminder-tag.disabled{color:#64748b;background:#f4f6f8;border-color:#dce2e8}.reminder-tag.pending{color:#aa6800;background:#fff7e6;border-color:#f0d18e}.reminder-tag.failed{color:#c9303a;background:#fff0f1;border-color:#f4bdc1}.row-buttons{white-space:nowrap}.row-buttons button,.retry-button{margin-right:5px;border:1px solid #ccd8e5;border-radius:6px;background:#fff;padding:5px 8px;color:#0b4a7e}.row-buttons button:disabled{opacity:.55}.failure-cell{max-width:260px;color:#b32f38!important;white-space:normal!important}.empty-cell{text-align:center;color:#718096;padding:36px!important}.reminder-state{display:flex;flex-direction:column;align-items:center;justify-content:center;gap:8px;min-height:300px;color:#718096}.reminder-state.error strong{color:#c93038}.reminder-state button{border:0;border-radius:6px;background:#0a376d;color:#fff;padding:8px 16px}.reminder-detail{padding:17px}.rule-title{display:grid;grid-template-columns:46px minmax(0,1fr) auto;align-items:center;gap:11px;padding-bottom:16px}.rule-icon{display:grid;place-items:center;width:46px;height:46px;border-radius:12px;background:#0b3d73;color:#fff;font-size:20px;font-weight:700}.rule-title h3{margin:0 0 4px;font-size:17px}.rule-title p{margin:0;color:#718096;font-size:12px}.detail-actions{display:grid;grid-template-columns:1fr 1fr;gap:8px;padding:13px 0;border-top:1px solid #e3e9ef}.detail-actions button{border:1px solid #d4dee9;border-radius:6px;background:#fff;padding:9px;color:#0a396c}.detail-actions button:first-child{background:#0a396c;color:#fff}.rule-section{padding:15px 0;border-top:1px solid #e3e9ef}.rule-section h4{margin:0 0 12px;font-size:14px}.rule-section dl{margin:0}.rule-section dl div{display:flex;justify-content:space-between;gap:12px;padding:6px 0}.rule-section dt{color:#718096}.rule-section dd{margin:0;text-align:right;font-weight:600}.rule-section p{margin:0;color:#5f7084;font-size:12px;line-height:1.7}.danger-zone button{border:1px solid #efb4b8;border-radius:6px;background:#fff4f4;color:#c62e38;padding:7px 10px}.danger-zone small{display:block;margin-top:8px;color:#8a6a6c;line-height:1.5}.rule-dialog{width:min(580px,calc(100vw - 32px));padding:0;border:0;border-radius:12px;box-shadow:0 22px 70px #10233b42}.rule-dialog::backdrop{background:#0a172a80}.rule-dialog form{margin:0}.rule-dialog header{display:flex;justify-content:space-between;gap:15px;padding:20px 22px;border-bottom:1px solid #e1e7ee}.rule-dialog h3{margin:0 0 5px;font-size:20px}.rule-dialog header p{margin:0;color:#718096;font-size:12px}.rule-dialog header button{border:0;background:transparent;color:#718096;font-size:26px}.rule-form{display:grid;gap:14px;max-height:65vh;padding:20px 22px;overflow:auto}.rule-form>label,.form-grid label{display:grid;gap:6px;color:#253b54;font-size:13px;font-weight:600}.rule-form input:not([type=checkbox]),.rule-form select{box-sizing:border-box;width:100%;border:1px solid #cbd7e4;border-radius:7px;padding:10px;background:#fff;font:inherit}.form-grid{display:grid;grid-template-columns:1fr 1fr;gap:12px}.rule-form fieldset{display:grid;gap:8px;border:1px solid #dce4ed;border-radius:8px;padding:12px}.rule-form legend{padding:0 6px;color:#253b54;font-size:13px;font-weight:700}.check-label,.enabled-toggle{display:grid!important;grid-template-columns:auto 1fr;align-items:start;gap:9px!important;padding:5px;font-weight:400!important}.check-label input,.enabled-toggle input{margin-top:3px}.check-label b,.check-label small,.enabled-toggle b,.enabled-toggle small{display:block}.check-label small,.enabled-toggle small{margin-top:3px;color:#718096;font-weight:400}.enabled-toggle{padding:11px!important;border-radius:8px;background:#f5f8fb}.form-error{margin:0;color:#c93038;font-size:12px}.rule-dialog menu{display:flex;justify-content:flex-end;gap:8px;margin:0;padding:14px 22px;background:#f6f8fb}.rule-dialog menu button{border:1px solid #ccd7e3;border-radius:7px;background:#fff;padding:9px 17px}.rule-dialog menu .save-button{border-color:#d08a00;background:#d08a00;color:#fff}.rule-dialog menu button:disabled{opacity:.6}@media(max-width:1200px){.reminder-layout{grid-template-columns:1fr}}@media(max-width:620px){.form-grid{grid-template-columns:1fr}.reminder-head{align-items:flex-start;flex-direction:column}.reminder-tabs{overflow:auto;width:100%}}
</style>
