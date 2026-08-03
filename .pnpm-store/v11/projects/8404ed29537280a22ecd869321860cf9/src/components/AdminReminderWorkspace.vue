<template>
  <section class="reminder-layout">
    <div class="panel reminder-main">
      <div class="reminder-head">
        <div><h2>{{ $t('ui.reminderRulesAndHistory') }}</h2><span>{{ $t('ui.liveDatabaseHourly') }}</span></div>
        <div class="reminder-tabs">
          <button :class="{ active: activeTab === 'rules' }" @click="activeTab = 'rules'">{{ $t('ui.reminderRules') }} <b>{{ rules.length }}</b></button>
          <button :class="{ active: activeTab === 'notifications' }" @click="activeTab = 'notifications'">{{ $t('ui.notificationRecords') }} <b>{{ notifications.length }}</b></button>
          <button :class="{ active: activeTab === 'deliveries' }" @click="activeTab = 'deliveries'">{{ $t('ui.deliveryResults') }} <b>{{ deliveries.length }}</b></button>
        </div>
      </div>

      <div v-if="loading" class="reminder-state">{{ $t('ui.loadingReminders') }}</div>
      <div v-else-if="errorMessage" class="reminder-state error"><strong>{{ $t('ui.loadRemindersFailed') }}</strong><span>{{ errorMessage }}</span><button @click="loadData">{{ $t('ui.reload') }}</button></div>

      <div v-else-if="activeTab === 'rules'" class="table-wrap reminder-table-wrap">
        <table>
          <thead><tr><th>{{ $t('ui.ruleName') }}</th><th>{{ $t('ui.reminderScenario') }}</th><th>{{ $t('ui.triggerTime') }}</th><th>{{ $t('ui.notificationMethod') }}</th><th>{{ $t('ui.status') }}</th><th>{{ $t('ui.latestUpdate') }}</th><th>{{ $t('ui.actions') }}</th></tr></thead>
          <tbody>
            <tr v-for="rule in pagedRules" :key="rule.id" :class="{ selected: selectedRuleId === rule.id }" @click="selectedRuleId = rule.id">
              <td><strong>{{ ruleDisplayName(rule) }}</strong><small>{{ rule.code }}</small></td>
              <td>{{ eventLabel(rule.eventType) }}</td>
              <td>{{ triggerLabel(rule) }}</td>
              <td><span v-for="channel in rule.channels" :key="channel" class="channel-chip">{{ channelLabel(channel) }}</span></td>
              <td><span class="reminder-tag" :class="rule.enabled ? 'enabled' : 'disabled'">{{ rule.enabled ? $t('ui.enabled') : $t('ui.disabled') }}</span></td>
              <td>{{ dateTime(rule.updatedAt) }}</td>
              <td class="row-buttons"><button @click.stop="openEdit(rule)">{{ $t('ui.edit') }}</button><button :disabled="runningId === rule.id" @click.stop="runRule(rule)">{{ runningId === rule.id ? $t('ui.running') : $t('ui.runNow') }}</button></td>
            </tr>
            <tr v-if="!filteredRules.length"><td colspan="7" class="empty-cell">{{ $t('ui.noMatchingRules') }}</td></tr>
          </tbody>
        </table>
      </div>

      <div v-else-if="activeTab === 'notifications'" class="table-wrap reminder-table-wrap">
        <table>
          <thead><tr><th>{{ $t('legacy.t_a4cc514f1044') }}</th><th>{{ $t('legacy.t_ab230661307e') }}</th><th>{{ $t('legacy.t_529414cfe547') }}</th><th>{{ $t('legacy.t_21608b504e75') }}</th><th>{{ $t('legacy.t_e3c782502b8b') }}</th><th>{{ $t('legacy.t_522d73c189ab') }}</th><th>{{ $t('legacy.t_9a4880538bea') }}</th></tr></thead>
          <tbody>
            <tr v-for="item in pagedNotifications" :key="item.id">
              <td>{{ dateTime(item.createdAt) }}</td><td><strong>{{ item.title }}</strong><small>{{ item.body }}</small></td>
              <td><strong>{{ item.recipientName || $t('legacy.t_0cf29eb7bc67') }}</strong><small>{{ item.recipientEmail || $t('legacy.t_cd40f9f840e1') }}</small></td>
              <td>{{ item.ruleName }}</td><td><span class="reminder-tag" :class="priorityClass(item.priority)">{{ priorityLabel(item.priority) }}</span></td>
              <td>{{ deliveryLabel(item.deliverySummary) }}</td><td class="failure-cell">{{ item.failureReason || '—' }}</td>
            </tr>
            <tr v-if="!filteredNotifications.length"><td colspan="7" class="empty-cell">{{ $t('legacy.t_b9dc18cab8c1') }}</td></tr>
          </tbody>
        </table>
      </div>

      <div v-else class="table-wrap reminder-table-wrap">
        <table>
          <thead><tr><th>{{ $t('legacy.t_7a66c0d03631') }}</th><th>{{ $t('legacy.t_529414cfe547') }}</th><th>{{ $t('legacy.t_ad30d9f116fe') }}</th><th>{{ $t('legacy.t_e203a4c76038') }}</th><th>{{ $t('legacy.t_45293595eae3') }}</th><th>{{ $t('legacy.t_34f13573f23d') }}</th><th>{{ $t('legacy.t_7e76ce4d1414') }}</th><th>{{ $t('legacy.t_f3ea6d345e2a') }}</th></tr></thead>
          <tbody>
            <tr v-for="delivery in pagedDeliveries" :key="delivery.id">
              <td><strong>{{ delivery.title }}</strong><small>{{ delivery.ruleName }}</small></td><td>{{ delivery.recipientName || '—' }}</td>
              <td>{{ channelLabel(delivery.channel) }}</td><td>{{ delivery.destination || $t('legacy.t_1be94bed7b3f') }}</td>
              <td><span class="reminder-tag" :class="delivery.status">{{ deliveryStatusLabel(delivery.status) }}</span></td><td>{{ delivery.attemptCount }}</td>
              <td><strong>{{ dateTime(delivery.sentAt || delivery.failedAt) }}</strong><small class="failure-cell">{{ delivery.failureReason || '—' }}</small></td>
              <td><button v-if="delivery.status === 'failed' && delivery.channel === 'email'" class="retry-button" @click="retryDelivery(delivery)">{{ $t('legacy.t_a7d12db24f8a') }}</button><span v-else>—</span></td>
            </tr>
            <tr v-if="!filteredDeliveries.length"><td colspan="8" class="empty-cell">{{ $t('legacy.t_93b56b4d2a01') }}</td></tr>
          </tbody>
        </table>
      </div>
      <AdminListPager :page="pageNumber" :page-size="pageSize" :total="activeTotal" @update:page="pageNumber=$event" @update:page-size="pageSize=$event;pageNumber=1" />
    </div>

    <aside class="panel reminder-detail">
      <template v-if="selectedRule">
        <div class="rule-title"><div class="rule-icon">{{ eventIcon(selectedRule.eventType) }}</div><div><h3>{{ selectedRule.name }}</h3><p>{{ selectedRule.code }}</p></div><span class="reminder-tag" :class="selectedRule.enabled ? 'enabled' : 'disabled'">{{ selectedRule.enabled ? $t('legacy.t_ce6c3dc32674') : $t('legacy.t_d989e55188c9') }}</span></div>
        <div class="detail-actions"><button @click="openEdit(selectedRule)">{{ $t('legacy.t_b369492d74d3') }}</button><button @click="toggleRule(selectedRule)">{{ selectedRule.enabled ? $t('legacy.t_d989e55188c9') : $t('legacy.t_ce6c3dc32674') }}</button></div>
        <section class="rule-section"><h4>{{ $t('legacy.t_a467b6df2ddd') }}</h4><dl><div><dt>{{ $t('legacy.t_fd233f798e10') }}</dt><dd>{{ eventLabel(selectedRule.eventType) }}</dd></div><div><dt>{{ $t('legacy.t_b0f7fc5e2c79') }}</dt><dd>{{ triggerLabel(selectedRule) }}</dd></div><div><dt>{{ $t('legacy.t_bc14276958f3') }}</dt><dd>{{ $t('legacy.t_ad6da4dc7810') }}</dd></div><div><dt>{{ $t('legacy.t_836eeef0f9bd') }}</dt><dd>{{ selectedRule.channels.map(channelLabel).join('、') }}</dd></div></dl></section>
        <section class="rule-section"><h4>{{ $t('legacy.t_f2dcb420fb91') }}</h4><p>{{ $t('legacy.t_8fc4a3f9f22f') }}</p></section>
        <section class="rule-section danger-zone"><h4>{{ $t('legacy.t_3035b7f6a754') }}</h4><button @click="deleteRule(selectedRule)">{{ $t('legacy.t_96c9d0d1c4e8') }}</button><small>{{ $t('legacy.t_00c8deb77f2c') }}</small></section>
      </template>
      <div v-else class="reminder-state">{{ $t('legacy.t_60ee16d27951') }}</div>
    </aside>

    <dialog ref="ruleDialog" class="rule-dialog">
      <form @submit.prevent="saveRule">
        <header><div><h3>{{ editingId ? $t('legacy.t_6ee58b4a9359') : $t('legacy.t_8c51c72f17bb') }}</h3><p>{{ $t('legacy.t_60234fb4e41b') }}</p></div><button type="button" @click="closeDialog">×</button></header>
        <div class="rule-form">
          <label>{{ $t('legacy.t_aa2f116157c2') }}<input v-model.trim="form.name" maxlength="160" required :placeholder="$t('legacy.t_442a7148de23')"></label>
          <label>{{ $t('legacy.t_43856d492b9a') }}<input v-model.trim="form.code" maxlength="80" required pattern="[A-Za-z0-9_-]+" :placeholder="$t('legacy.t_6853ca2926a3')"></label>
          <div class="form-grid"><label>{{ $t('legacy.t_fd233f798e10') }}<select v-model="form.eventType"><option v-for="option in eventOptions" :key="option.value" :value="option.value">{{ option.label }}</option></select></label><label>{{ $t('legacy.t_1c53bb9b196e') }}<input v-model.number="form.daysBefore" type="number" min="0" max="365" required></label></div>
          <fieldset><legend>{{ $t('legacy.t_836eeef0f9bd') }}</legend><label v-for="channel in channelOptions" :key="channel.value" class="check-label"><input v-model="form.channels" type="checkbox" :value="channel.value"><span><b>{{ channel.label }}</b><small>{{ channel.hint }}</small></span></label></fieldset>
          <label class="enabled-toggle"><input v-model="form.enabled" type="checkbox"><span><b>{{ $t('legacy.t_39eed6f3c5ff') }}</b><small>{{ $t('legacy.t_1c81d29b1f50') }}</small></span></label>
          <p v-if="formError" class="form-error">{{ formError }}</p>
        </div>
        <menu><button type="button" @click="closeDialog">{{ $t('legacy.t_4d0b4688c787') }}</button><button class="save-button" :disabled="saving">{{ saving ? $t('legacy.t_8488ea2522af') : $t('legacy.t_e3cd424012ba') }}</button></menu>
      </form>
    </dialog>
  </section>
</template>

<script>
import { createAdminReminderRule, deleteAdminReminderRule, fetchAdminReminders, retryAdminReminderDelivery, runAdminReminderRule, runAllAdminReminderRules, toggleAdminReminderRule, updateAdminReminderRule } from '../services/propertyApi';
import AdminListPager from './AdminListPager.vue';

const blankForm = () => ({ code: '', name: '', eventType: 'payment_due', daysBefore: 7, channels: ['in_app'], recipientRole: 'owner', enabled: true });

export default {
  components:{AdminListPager},
  inject: ['page'],
  data() {
    return {
      activeTab: 'rules', loading: true, errorMessage: '', rules: [], notifications: [], deliveries: [], selectedRuleId: null,pageNumber:1,pageSize:10,
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
    pagedRules(){return this.slicePage(this.filteredRules)},pagedNotifications(){return this.slicePage(this.filteredNotifications)},pagedDeliveries(){return this.slicePage(this.filteredDeliveries)},activeTotal(){return this.activeTab==='rules'?this.filteredRules.length:this.activeTab==='notifications'?this.filteredNotifications.length:this.filteredDeliveries.length},
    createNonce() { return this.page.adminReminderCreateNonce; }, runNonce() { return this.page.adminReminderRunNonce; }
  },
  watch: {
    createNonce(value, previous) { if (value > previous) this.openCreate(); },
    runNonce(value, previous) { if (value > previous) this.runAll(); },
    filteredRules(rows) { if (!rows.some(row => row.id === this.selectedRuleId)) this.selectedRuleId = rows[0]?.id || null; },
    activeTab(value) { this.pageNumber=1;if (value === 'deliveries' && this.page.statusFilter === '啟用') this.page.statusFilter = '全部狀態'; },keyword(){this.pageNumber=1},'page.statusFilter'(){this.pageNumber=1}
  },
  mounted() { this.page.projectFilter = '全部提醒'; this.page.statusFilter = '全部狀態'; this.loadData(); },
  methods: {
    slicePage(rows){const start=(this.pageNumber-1)*this.pageSize;return rows.slice(start,start+this.pageSize)},
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
        { label: this.$t('ui.reminderRules'), value: this.$t('ui.records', { count: Number(summary.ruleCount || 0) }), delta: this.$t('ui.enabledRules', { count: Number(summary.enabledRuleCount || 0) }), trend: 'up' },
        { label: this.$t('ui.notificationsCreated'), value: this.$t('ui.records', { count: Number(summary.notificationCount || 0) }), delta: this.$t('ui.databaseNotificationRecords'), trend: 'up' },
        { label: this.$t('ui.pending'), value: this.$t('ui.records', { count: Number(summary.pendingDeliveryCount || 0) }), delta: this.$t('ui.awaitingChannel'), trend: Number(summary.pendingDeliveryCount) ? 'down' : 'up' },
        { label: this.$t('ui.deliveryFailed'), value: this.$t('ui.records', { count: Number(summary.failedDeliveryCount || 0) }), delta: this.$t('ui.viewReasonRetry'), trend: Number(summary.failedDeliveryCount) ? 'down' : 'up' }
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
    ruleDisplayName(rule) { return ({ payment_due: this.$t('ui.paymentDue'), rent_due: this.$t('ui.rentDue'), lease_expiry: this.$t('ui.leaseExpiry'), reserve_low: this.$t('ui.reserveLow'), document_expiry: this.$t('ui.documentExpiry') })[rule.eventType] || rule.name; },
    eventLabel(value) { return ({ payment_due: this.$t('ui.paymentDue'), rent_due: this.$t('ui.rentDue'), lease_expiry: this.$t('ui.leaseExpiry'), reserve_low: this.$t('ui.reserveLow'), document_expiry: this.$t('ui.documentExpiry') })[value] || value; },
    eventIcon(value) { return ({ payment_due: '款', rent_due: '租', lease_expiry: '約', reserve_low: '金', document_expiry: '件' })[value] || '醒'; },
    triggerLabel(rule) { return rule.eventType === 'reserve_low' ? '低於最低標準時' : rule.daysBefore ? `到期前 ${rule.daysBefore} 天` : '到期當天／已逾期'; },
    channelLabel(value) { return ({ in_app: this.$t('ui.inApp'), email: this.$t('ui.email'), line: 'LINE' })[value] || value; },
    priorityLabel(value) { return ({ urgent: this.$t('ui.urgent'), high: this.$t('ui.high'), normal: this.$t('ui.normal'), low: this.$t('ui.low') })[value] || value; },
    priorityClass(value) { return value === 'urgent' ? 'failed' : value === 'high' ? 'pending' : 'sent'; },
    deliveryStatusLabel(value) { return ({ sent: this.$t('ui.sent'), pending: this.$t('ui.pending'), failed: this.$t('ui.failed') })[value] || value; },
    deliveryLabel(value) { return String(value || 'in_app:sent').split(',').map(part => { const [channel, status] = part.split(':'); return `${this.channelLabel(channel)} ${this.deliveryStatusLabel(status)}`; }).join('、'); },
    dateTime(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '—'; }
  }
};
</script>

<style scoped>
.reminder-layout{display:grid;grid-template-columns:minmax(0,1fr) 340px;gap:14px;min-height:570px}.reminder-main,.reminder-detail{min-width:0}.reminder-head{display:flex;align-items:flex-end;justify-content:space-between;gap:18px;padding:16px 18px 0}.reminder-head h2{margin:0 0 5px;font-size:18px}.reminder-head>div>span{color:#718096;font-size:12px}.reminder-tabs{display:flex;gap:4px}.reminder-tabs button{border:0;border-bottom:2px solid transparent;background:transparent;padding:9px 10px;color:#64748b}.reminder-tabs button.active{border-color:#d59200;color:#0b315f;font-weight:700}.reminder-tabs b{display:inline-grid;place-items:center;min-width:18px;height:18px;margin-left:4px;border-radius:10px;background:#eef2f7;font-size:10px}.reminder-table-wrap{margin-top:10px;overflow:auto}.reminder-table-wrap table{min-width:930px}.reminder-table-wrap tbody tr{cursor:pointer}.reminder-table-wrap tbody tr.selected{background:#fff8e8}.reminder-table-wrap td strong,.reminder-table-wrap td small{display:block}.reminder-table-wrap td small{max-width:420px;margin-top:3px;color:#718096;font-size:11px;white-space:normal}.channel-chip{display:inline-block;margin:2px;padding:3px 7px;border-radius:10px;background:#edf3fa;color:#24557e;font-size:11px}.reminder-tag{display:inline-flex;padding:4px 8px;border:1px solid;border-radius:6px;font-size:11px;white-space:nowrap}.reminder-tag.enabled,.reminder-tag.sent{color:#18864a;background:#eef9f2;border-color:#bde6cb}.reminder-tag.disabled{color:#64748b;background:#f4f6f8;border-color:#dce2e8}.reminder-tag.pending{color:#aa6800;background:#fff7e6;border-color:#f0d18e}.reminder-tag.failed{color:#c9303a;background:#fff0f1;border-color:#f4bdc1}.row-buttons{white-space:nowrap}.row-buttons button,.retry-button{margin-right:5px;border:1px solid #ccd8e5;border-radius:6px;background:#fff;padding:5px 8px;color:#0b4a7e}.row-buttons button:disabled{opacity:.55}.failure-cell{max-width:260px;color:#b32f38!important;white-space:normal!important}.empty-cell{text-align:center;color:#718096;padding:36px!important}.reminder-state{display:flex;flex-direction:column;align-items:center;justify-content:center;gap:8px;min-height:300px;color:#718096}.reminder-state.error strong{color:#c93038}.reminder-state button{border:0;border-radius:6px;background:#0a376d;color:#fff;padding:8px 16px}.reminder-detail{padding:17px}.rule-title{display:grid;grid-template-columns:46px minmax(0,1fr) auto;align-items:center;gap:11px;padding-bottom:16px}.rule-icon{display:grid;place-items:center;width:46px;height:46px;border-radius:12px;background:#0b3d73;color:#fff;font-size:20px;font-weight:700}.rule-title h3{margin:0 0 4px;font-size:17px}.rule-title p{margin:0;color:#718096;font-size:12px}.detail-actions{display:grid;grid-template-columns:1fr 1fr;gap:8px;padding:13px 0;border-top:1px solid #e3e9ef}.detail-actions button{border:1px solid #d4dee9;border-radius:6px;background:#fff;padding:9px;color:#0a396c}.detail-actions button:first-child{background:#0a396c;color:#fff}.rule-section{padding:15px 0;border-top:1px solid #e3e9ef}.rule-section h4{margin:0 0 12px;font-size:14px}.rule-section dl{margin:0}.rule-section dl div{display:flex;justify-content:space-between;gap:12px;padding:6px 0}.rule-section dt{color:#718096}.rule-section dd{margin:0;text-align:right;font-weight:600}.rule-section p{margin:0;color:#5f7084;font-size:12px;line-height:1.7}.danger-zone button{border:1px solid #efb4b8;border-radius:6px;background:#fff4f4;color:#c62e38;padding:7px 10px}.danger-zone small{display:block;margin-top:8px;color:#8a6a6c;line-height:1.5}.rule-dialog{width:min(580px,calc(100vw - 32px));padding:0;border:0;border-radius:12px;box-shadow:0 22px 70px #10233b42}.rule-dialog::backdrop{background:#0a172a80}.rule-dialog form{margin:0}.rule-dialog header{display:flex;justify-content:space-between;gap:15px;padding:20px 22px;border-bottom:1px solid #e1e7ee}.rule-dialog h3{margin:0 0 5px;font-size:20px}.rule-dialog header p{margin:0;color:#718096;font-size:12px}.rule-dialog header button{border:0;background:transparent;color:#718096;font-size:26px}.rule-form{display:grid;gap:14px;max-height:65vh;padding:20px 22px;overflow:auto}.rule-form>label,.form-grid label{display:grid;gap:6px;color:#253b54;font-size:13px;font-weight:600}.rule-form input:not([type=checkbox]),.rule-form select{box-sizing:border-box;width:100%;border:1px solid #cbd7e4;border-radius:7px;padding:10px;background:#fff;font:inherit}.form-grid{display:grid;grid-template-columns:1fr 1fr;gap:12px}.rule-form fieldset{display:grid;gap:8px;border:1px solid #dce4ed;border-radius:8px;padding:12px}.rule-form legend{padding:0 6px;color:#253b54;font-size:13px;font-weight:700}.check-label,.enabled-toggle{display:grid!important;grid-template-columns:auto 1fr;align-items:start;gap:9px!important;padding:5px;font-weight:400!important}.check-label input,.enabled-toggle input{margin-top:3px}.check-label b,.check-label small,.enabled-toggle b,.enabled-toggle small{display:block}.check-label small,.enabled-toggle small{margin-top:3px;color:#718096;font-weight:400}.enabled-toggle{padding:11px!important;border-radius:8px;background:#f5f8fb}.form-error{margin:0;color:#c93038;font-size:12px}.rule-dialog menu{display:flex;justify-content:flex-end;gap:8px;margin:0;padding:14px 22px;background:#f6f8fb}.rule-dialog menu button{border:1px solid #ccd7e3;border-radius:7px;background:#fff;padding:9px 17px}.rule-dialog menu .save-button{border-color:#d08a00;background:#d08a00;color:#fff}.rule-dialog menu button:disabled{opacity:.6}@media(max-width:1200px){.reminder-layout{grid-template-columns:1fr}}@media(max-width:620px){.form-grid{grid-template-columns:1fr}.reminder-head{align-items:flex-start;flex-direction:column}.reminder-tabs{overflow:auto;width:100%}}
</style>
