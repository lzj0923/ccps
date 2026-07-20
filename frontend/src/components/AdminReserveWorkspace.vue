<template>
  <section class="reserve-layout">
    <div class="panel reserve-list-panel">
      <div class="panel-head">
        <div><h2>預備金帳戶列表</h2><span>{{ filteredAccounts.length }} 個帳戶 · 資料庫即時資料</span></div>
        <span v-if="loading" class="loading-text">正在載入…</span>
      </div>

      <div v-if="errorMessage" class="reserve-state error" role="alert">
        <strong>預備金資料載入失敗</strong>
        <span>{{ errorMessage }}</span>
        <button type="button" @click="loadData">重新載入</button>
      </div>
      <div v-else class="table-wrap reserve-table-wrap">
        <table>
          <thead>
            <tr><th>業主／單位</th><th>目前餘額</th><th>最低標準</th><th>不足金額</th><th>累計充值</th><th>累計扣款</th><th>最近變動</th><th>狀態</th><th>待確認</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="account in filteredAccounts" :key="account.id" :class="{ selected: account.id === selectedId }" @click="selectedId = account.id">
              <td><strong>{{ account.ownerName }}</strong><small>{{ account.projectName }} · {{ account.unitNo }}</small></td>
              <td><b :class="{ danger: account.balanceStatus === 'low' }">RM {{ money(account.currentBalance) }}</b></td>
              <td>RM {{ money(account.minimumBalance) }}</td>
              <td><b :class="{ danger: Number(account.shortageAmount) > 0 }">RM {{ money(account.shortageAmount) }}</b></td>
              <td class="positive">RM {{ money(account.totalTopups) }}</td>
              <td>RM {{ money(account.totalDebits) }}</td>
              <td>{{ dateTime(account.lastMovementAt) }}</td>
              <td><span class="reserve-tag" :class="account.balanceStatus === 'low' ? 'low' : 'normal'">{{ account.balanceStatus === 'low' ? '餘額不足' : '正常' }}</span></td>
              <td><span v-if="account.pendingTopupCount" class="reserve-tag pending">{{ account.pendingTopupCount }} 筆 · RM {{ money(account.pendingTopupAmount) }}</span><span v-else>—</span></td>
              <td><button type="button" class="detail-button" @click.stop="selectedId = account.id">查看</button></td>
            </tr>
            <tr v-if="!loading && !filteredAccounts.length"><td colspan="10" class="empty-cell">目前沒有符合條件的預備金帳戶</td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <aside class="panel reserve-detail-panel">
      <template v-if="selectedAccount">
        <div class="account-profile">
          <div class="reserve-avatar">備</div>
          <div><h3>{{ selectedAccount.ownerName }}</h3><p>{{ selectedAccount.projectName }} · {{ selectedAccount.unitNo }}</p></div>
          <span class="reserve-tag" :class="selectedAccount.balanceStatus === 'low' ? 'low' : 'normal'">{{ selectedAccount.balanceStatus === 'low' ? '餘額不足' : '正常' }}</span>
        </div>
        <div class="detail-actions">
          <button type="button" @click="openSettings(selectedAccount.id)">設定預備金</button>
          <button type="button" @click="openDirectTopup(selectedAccount.id)">直接充值</button>
          <button type="button" @click="goFinance">前往充值確認</button>
          <button type="button" @click="goMaintenance">查看收支維修</button>
        </div>

        <div class="detail-section">
          <h4><span>1</span>帳戶餘額</h4>
          <div class="balance-hero"><small>目前可用餘額</small><strong>RM {{ money(selectedAccount.currentBalance) }}</strong></div>
          <div class="balance-track"><i :class="{ low: selectedAccount.balanceStatus === 'low' }" :style="{ width: balanceProgress + '%' }"></i></div>
          <div class="kv"><span>最低標準</span><b>RM {{ money(selectedAccount.minimumBalance) }}</b></div>
          <div class="kv"><span>尚需補足</span><b :class="{ danger: Number(selectedAccount.shortageAmount) > 0 }">RM {{ money(selectedAccount.shortageAmount) }}</b></div>
          <div class="kv"><span>低餘額提醒</span><b>{{ selectedAccount.lowBalanceAlertEnabled ? '已啟用' : '未啟用' }}</b></div>
        </div>

        <div class="detail-section">
          <h4><span>2</span>資金摘要</h4>
          <div class="kv"><span>累計充值</span><b class="positive">RM {{ money(selectedAccount.totalTopups) }}</b></div>
          <div class="kv"><span>累計扣款</span><b>RM {{ money(selectedAccount.totalDebits) }}</b></div>
          <div class="kv"><span>待財務確認</span><b>{{ selectedAccount.pendingTopupCount }} 筆</b></div>
          <div class="kv"><span>待確認金額</span><b class="pending-text">RM {{ money(selectedAccount.pendingTopupAmount) }}</b></div>
        </div>

        <div class="detail-section transaction-section">
          <h4><span>3</span>最近交易</h4>
          <div v-for="item in selectedTransactions" :key="item.id" class="transaction-item">
            <div><b>{{ transactionLabel(item.transactionType) }}</b><small>{{ item.transactionNo || item.workOrderNo || '預備金調整' }}</small></div>
            <div><strong :class="item.transactionType === 'topup' ? 'positive' : 'danger'">{{ item.transactionType === 'topup' ? '+' : '-' }} RM {{ money(item.amount) }}</strong><small>{{ dateTime(item.occurredAt) }}</small></div>
          </div>
          <p v-if="!selectedTransactions.length" class="no-transactions">尚無交易記錄</p>
        </div>
      </template>
      <div v-else class="reserve-state">請從左側選擇預備金帳戶</div>
    </aside>

    <dialog ref="settingsDialog" class="reserve-settings-dialog">
      <form @submit.prevent="saveSettings">
        <header><div><h3>設定預備金</h3><p>設定指定業主單位的最低預備金標準與不足提醒。</p></div><button type="button" aria-label="關閉" @click="closeSettings">×</button></header>
        <div class="settings-body">
          <label>業主／單位
            <select v-model.number="settings.accountId" required @change="syncSettingsForm">
              <option v-for="account in accounts" :key="account.id" :value="account.id">{{ account.ownerName }} · {{ account.projectName }}／{{ account.unitNo }}</option>
            </select>
          </label>
          <div v-if="settingsAccount" class="settings-balance-note">
            <span>目前餘額</span><b>RM {{ money(settingsAccount.currentBalance) }}</b>
            <span>現行最低標準</span><b>RM {{ money(settingsAccount.minimumBalance) }}</b>
          </div>
          <label>最低預備金標準（RM）
            <input v-model="settings.minimumBalance" type="number" min="0" max="9999999999999999.99" step="0.01" required>
            <small>當目前餘額低於此金額時，帳戶會標記為「餘額不足」。</small>
          </label>
          <label class="alert-toggle"><input v-model="settings.lowBalanceAlertEnabled" type="checkbox"><span><b>啟用低餘額提醒</b><small>餘額低於最低標準時顯示提醒狀態。</small></span></label>
          <p v-if="settingsError" class="settings-error">{{ settingsError }}</p>
        </div>
        <menu><button type="button" @click="closeSettings">取消</button><button type="submit" class="save-button" :disabled="settingsSaving">{{ settingsSaving ? '儲存中…' : '儲存設定' }}</button></menu>
      </form>
    </dialog>

    <dialog ref="directTopupDialog" class="reserve-settings-dialog direct-topup-dialog">
      <form @submit.prevent="saveDirectTopup">
        <header><div><h3>直接充值預備金</h3><p>管理員確認線下收款後直接入帳，不經待確認流程。</p></div><button type="button" aria-label="關閉" @click="closeDirectTopup">×</button></header>
        <div class="settings-body direct-topup-body">
          <label>業主／單位
            <select v-model.number="topup.accountId" required @change="syncTopupAccount">
              <option v-for="account in accounts" :key="account.id" :value="account.id">{{ account.ownerName }} · {{ account.projectName }}／{{ account.unitNo }}</option>
            </select>
          </label>
          <div v-if="topupAccount" class="settings-balance-note"><span>充值前餘額</span><b>RM {{ money(topupAccount.currentBalance) }}</b><span>充值後餘額</span><b class="positive">RM {{ money(topupBalanceAfter) }}</b></div>
          <div class="form-grid">
            <label>充值金額（RM）<input v-model="topup.amount" type="number" min="0.01" max="9999999999999999.99" step="0.01" required></label>
            <label>入帳日期<input v-model="topup.paymentDate" type="date" :max="todayDate" required></label>
          </div>
          <div class="form-grid">
            <label>付款方式<select v-model="topup.paymentMethod" required><option value="bank_transfer">銀行轉帳</option><option value="online_payment">線上支付</option><option value="cash">現金</option><option value="cheque">支票</option></select></label>
            <label>付款人<input v-model.trim="topup.payerName" maxlength="160" required></label>
          </div>
          <label>銀行／付款參考<input v-model.trim="topup.bankReference" maxlength="120" placeholder="現金充值可留空"></label>
          <label>入帳備註<textarea v-model.trim="topup.note" maxlength="500" rows="3" placeholder="例如：管理處櫃檯已核對收款"></textarea></label>
          <div class="direct-topup-warning"><b>提交後會立即入帳</b><span>系統將同步建立已確認財務記錄、預備金流水及審計記錄。</span></div>
          <p v-if="topupError" class="settings-error">{{ topupError }}</p>
        </div>
        <menu><button type="button" @click="closeDirectTopup">取消</button><button type="submit" class="save-button" :disabled="topupSaving">{{ topupSaving ? '入帳中…' : '確認充值並入帳' }}</button></menu>
      </form>
    </dialog>
  </section>
</template>

<script>
import { createAdminReserveDirectTopup, fetchAdminReserveOverview, updateAdminReserveSettings } from '../services/propertyApi';

const today = () => {
  const value = new Date();
  value.setMinutes(value.getMinutes() - value.getTimezoneOffset());
  return value.toISOString().slice(0, 10);
};

export default {
  inject: ['page'],
  data() {
    return {
      accounts: [], transactions: [], selectedId: null, loading: false, errorMessage: '', requestSerial: 0,
      settings: { accountId: null, minimumBalance: '', lowBalanceAlertEnabled: true }, settingsSaving: false, settingsError: '',
      todayDate: today(), topup: { accountId: null, amount: '', paymentDate: today(), paymentMethod: 'bank_transfer', payerName: '', bankReference: '', note: '' }, topupSaving: false, topupError: ''
    };
  },
  computed: {
    filteredAccounts() {
      const keyword = String(this.page.globalSearch || this.page.moduleSearch || '').trim().toLowerCase();
      const project = String(this.page.projectFilter || '');
      const status = String(this.page.statusFilter || '');
      return this.accounts.filter(account => {
        const text = [account.ownerName, account.projectName, account.unitNo].join(' ').toLowerCase();
        const projectMatches = !project || project.includes('全部') || account.projectName === project;
        let statusMatches = !status || status.includes('全部');
        if (status === '正常') statusMatches = account.balanceStatus === 'normal';
        if (status === '餘額不足') statusMatches = account.balanceStatus === 'low';
        if (status === '待財務確認') statusMatches = Number(account.pendingTopupCount) > 0;
        return (!keyword || text.includes(keyword)) && projectMatches && statusMatches;
      });
    },
    selectedAccount() { return this.accounts.find(account => account.id === this.selectedId) || this.filteredAccounts[0] || null; },
    selectedTransactions() { return this.transactions.filter(item => item.reserveAccountId === this.selectedAccount?.id).slice(0, 8); },
    balanceProgress() {
      if (!this.selectedAccount) return 0;
      const minimum = Number(this.selectedAccount.minimumBalance || 0);
      return minimum ? Math.min(100, Math.round(Number(this.selectedAccount.currentBalance || 0) / minimum * 100)) : 100;
    },
    refreshNonce() { return this.page.adminReserveRefreshNonce; }
    ,settingsNonce() { return this.page.adminReserveSettingsNonce; }
    ,settingsAccount() { return this.accounts.find(account => account.id === this.settings.accountId) || null; }
    ,directTopupNonce() { return this.page.adminReserveDirectTopupNonce; }
    ,topupAccount() { return this.accounts.find(account => account.id === this.topup.accountId) || null; }
    ,topupBalanceAfter() { return Number(this.topupAccount?.currentBalance || 0) + Number(this.topup.amount || 0); }
  },
  watch: {
    refreshNonce(value, previous) { if (value > previous) this.loadData(); },
    settingsNonce(value, previous) { if (value > previous) this.openSettings(this.selectedAccount?.id); },
    directTopupNonce(value, previous) { if (value > previous) this.openDirectTopup(this.selectedAccount?.id); },
    filteredAccounts(rows) { if (!rows.some(row => row.id === this.selectedId)) this.selectedId = rows[0]?.id || null; }
  },
  mounted() {
    this.page.projectFilter = '全部建案';
    this.page.statusFilter = '全部狀態';
    this.loadData();
  },
  methods: {
    async loadData() {
      const serial = ++this.requestSerial;
      const preferredId = this.selectedId;
      this.loading = true;
      this.errorMessage = '';
      try {
        const response = await fetchAdminReserveOverview();
        if (serial !== this.requestSerial) return;
        this.accounts = response.accounts || [];
        this.transactions = response.transactions || [];
        this.selectedId = this.accounts.some(row => row.id === preferredId) ? preferredId : this.accounts[0]?.id || null;
        this.page.adminReserveProjects = response.projects || [];
        this.page.adminReserveMetrics = this.metrics(response.summary || {});
        this.setExportRows();
      } catch (error) {
        if (serial !== this.requestSerial) return;
        this.accounts = [];
        this.transactions = [];
        this.page.adminReserveMetrics = null;
        this.page.adminReserveProjects = [];
        this.errorMessage = error.message || 'API request failed';
      } finally {
        if (serial === this.requestSerial) this.loading = false;
      }
    },
    metrics(summary) {
      return [
        { label: '預備金總餘額', value: `RM ${this.money(summary.totalBalance)}`, delta: `${Number(summary.accountCount || 0)} 個有效帳戶`, trend: 'up' },
        { label: '餘額不足帳戶', value: `${Number(summary.lowBalanceCount || 0)} 個`, delta: `最低標準 RM ${this.money(summary.minimumBalance)}`, trend: Number(summary.lowBalanceCount) ? 'down' : 'up' },
        { label: '本月充值', value: `RM ${this.money(summary.monthlyTopups)}`, delta: '已入帳交易', trend: 'up' },
        { label: '本月扣款', value: `RM ${this.money(summary.monthlyDebits)}`, delta: '維修及支出扣款', trend: Number(summary.monthlyDebits) ? 'down' : '' },
        { label: '待確認充值', value: `${Number(summary.pendingTopupCount || 0)} 筆`, delta: `RM ${this.money(summary.pendingTopupAmount)}`, trend: Number(summary.pendingTopupCount) ? 'down' : 'up' },
        { label: '預備金帳戶', value: `${Number(summary.accountCount || 0)} 個`, delta: '資料庫有效帳戶', trend: 'up' }
      ];
    },
    setExportRows() {
      this.page.adminReserveExportHeaders = ['業主', '建案', '單位', '目前餘額', '最低標準', '不足金額', '累計充值', '累計扣款', '最近變動', '狀態', '待確認筆數', '待確認金額'];
      this.page.adminReserveExportRows = this.accounts.map(row => [row.ownerName, row.projectName, row.unitNo, this.money(row.currentBalance), this.money(row.minimumBalance), this.money(row.shortageAmount), this.money(row.totalTopups), this.money(row.totalDebits), this.dateTime(row.lastMovementAt), row.balanceStatus === 'low' ? '餘額不足' : '正常', row.pendingTopupCount, this.money(row.pendingTopupAmount)]);
    },
    goFinance() { this.page.adminFinanceMode = 'reserve'; this.page.selectModule('adminFinance'); },
    goMaintenance() { this.page.selectModule('adminMaintenance'); },
    openSettings(accountId) {
      if (!this.accounts.length) return;
      this.settings.accountId = accountId || this.selectedAccount?.id || this.accounts[0].id;
      this.settingsError = '';
      this.syncSettingsForm();
      this.$refs.settingsDialog?.showModal();
    },
    syncSettingsForm() {
      const account = this.settingsAccount;
      if (!account) return;
      this.settings.minimumBalance = Number(account.minimumBalance || 0).toFixed(2);
      this.settings.lowBalanceAlertEnabled = Boolean(account.lowBalanceAlertEnabled);
    },
    closeSettings() { if (!this.settingsSaving) this.$refs.settingsDialog?.close(); },
    async saveSettings() {
      const amount = Number(this.settings.minimumBalance);
      if (!Number.isFinite(amount) || amount < 0) { this.settingsError = '最低預備金標準不能小於 0。'; return; }
      this.settingsSaving = true;
      this.settingsError = '';
      try {
        await updateAdminReserveSettings(this.settings.accountId, { minimumBalance: amount.toFixed(2), lowBalanceAlertEnabled: this.settings.lowBalanceAlertEnabled });
        this.$refs.settingsDialog?.close();
        this.selectedId = this.settings.accountId;
        await this.loadData();
        this.page.showToast('預備金設定已更新');
      } catch (error) { this.settingsError = error.message || '預備金設定儲存失敗'; }
      finally { this.settingsSaving = false; }
    },
    openDirectTopup(accountId) {
      if (!this.accounts.length) return;
      this.topup = { accountId: accountId || this.selectedAccount?.id || this.accounts[0].id, amount: '', paymentDate: today(), paymentMethod: 'bank_transfer', payerName: '', bankReference: '', note: '' };
      this.topupError = '';
      this.syncTopupAccount();
      this.$refs.directTopupDialog?.showModal();
    },
    syncTopupAccount() { if (this.topupAccount) this.topup.payerName = this.topupAccount.ownerName || ''; },
    closeDirectTopup() { if (!this.topupSaving) this.$refs.directTopupDialog?.close(); },
    async saveDirectTopup() {
      const amount = Number(this.topup.amount);
      if (!Number.isFinite(amount) || amount <= 0) { this.topupError = '充值金額必須大於 0。'; return; }
      if (this.topup.paymentMethod !== 'cash' && !this.topup.bankReference) { this.topupError = '非現金充值必須填寫銀行或付款參考。'; return; }
      this.topupSaving = true;
      this.topupError = '';
      try {
        const result = await createAdminReserveDirectTopup(this.topup.accountId, { amount: amount.toFixed(2), paymentDate: this.topup.paymentDate, paymentMethod: this.topup.paymentMethod, payerName: this.topup.payerName, bankReference: this.topup.bankReference || null, note: this.topup.note || null });
        this.$refs.directTopupDialog?.close();
        this.selectedId = this.topup.accountId;
        await this.loadData();
        this.page.showToast(`充值已入帳：${result.referenceNo}`);
      } catch (error) { this.topupError = error.message || '預備金充值失敗'; }
      finally { this.topupSaving = false; }
    },
    money(value) { return Number(value || 0).toLocaleString('en-MY', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); },
    dateTime(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '—'; },
    transactionLabel(type) { return ({ topup: '充值入帳', debit: '預備金扣款', adjustment: '餘額調整' })[type] || type || '預備金交易'; }
  }
};
</script>

<style scoped>
.reserve-layout{display:grid;grid-template-columns:minmax(0,1fr) 360px;gap:14px;min-height:560px}.reserve-list-panel,.reserve-detail-panel{min-width:0}.panel-head{display:flex;align-items:center;justify-content:space-between}.panel-head h2{margin:0 0 5px;font-size:18px}.panel-head span,.loading-text{color:#64748b;font-size:12px}.reserve-table-wrap{overflow:auto}.reserve-table-wrap table{min-width:1180px}.reserve-table-wrap tbody tr{cursor:pointer}.reserve-table-wrap tbody tr.selected{background:#fff8e7}.reserve-table-wrap td strong,.reserve-table-wrap td small{display:block}.reserve-table-wrap td small{margin-top:4px;color:#64748b;font-size:11px}.reserve-tag{display:inline-flex;align-items:center;border:1px solid;border-radius:6px;padding:4px 8px;font-size:12px;white-space:nowrap}.reserve-tag.normal{color:#168546;background:#edf9f1;border-color:#b8e6c7}.reserve-tag.low{color:#d12f35;background:#fff1f1;border-color:#ffc5c7}.reserve-tag.pending{color:#b56b00;background:#fff7e5;border-color:#ffd68a}.positive{color:#16934b!important}.danger{color:#e3343e!important}.pending-text{color:#c77900}.detail-button{border:1px solid #d7e1ed;background:#fff;color:#0b4b83;border-radius:6px;padding:5px 11px}.reserve-detail-panel{padding:16px}.account-profile{display:grid;grid-template-columns:48px minmax(0,1fr) auto;align-items:center;gap:12px;padding-bottom:16px;border-bottom:1px solid #e3e9f1}.reserve-avatar{display:grid;place-items:center;width:48px;height:48px;border-radius:50%;background:#3787f5;color:#fff;font-size:20px;font-weight:700}.account-profile h3{margin:0 0 5px;font-size:19px}.account-profile p{margin:0;color:#42566e}.detail-actions{display:grid;grid-template-columns:1fr 1fr;gap:8px;padding:14px 0}.detail-actions button{border:1px solid #d8e1ec;border-radius:6px;background:#fff;padding:9px;color:#0a315f}.detail-actions button:first-child{background:#092f63;color:#fff;border-color:#092f63}.detail-section{padding:15px 0;border-top:1px solid #e3e9f1}.detail-section h4{display:flex;align-items:center;gap:8px;margin:0 0 14px}.detail-section h4>span{display:grid;place-items:center;width:20px;height:20px;border-radius:50%;background:#06346d;color:#fff;font-size:12px}.balance-hero{display:flex;align-items:flex-end;justify-content:space-between;margin-bottom:10px}.balance-hero small{color:#64748b}.balance-hero strong{font-size:22px;color:#0a315f}.balance-track{height:7px;border-radius:8px;background:#e1e7ef;overflow:hidden;margin-bottom:13px}.balance-track i{display:block;height:100%;background:#24a35a;border-radius:inherit}.balance-track i.low{background:#e3a000}.kv{display:flex;justify-content:space-between;gap:16px;padding:5px 0;color:#5b6c80;font-size:13px}.kv b{color:#081b36;text-align:right}.transaction-section{max-height:280px;overflow:auto}.transaction-item{display:flex;justify-content:space-between;gap:12px;padding:9px 0;border-bottom:1px dashed #e1e7ee}.transaction-item>div:last-child{text-align:right}.transaction-item b,.transaction-item strong,.transaction-item small{display:block}.transaction-item small{margin-top:3px;color:#718096;font-size:11px}.no-transactions,.empty-cell{text-align:center;color:#718096}.reserve-state{display:flex;flex-direction:column;align-items:center;justify-content:center;gap:8px;min-height:220px;color:#64748b}.reserve-state.error strong{color:#c93038}.reserve-state button{border:0;border-radius:6px;background:#0a376d;color:#fff;padding:8px 16px}.reserve-settings-dialog{width:min(520px,calc(100vw - 32px));padding:0;border:0;border-radius:12px;box-shadow:0 22px 70px #10233b42}.reserve-settings-dialog::backdrop{background:#0a172a80}.reserve-settings-dialog form{margin:0}.reserve-settings-dialog header{display:flex;justify-content:space-between;gap:16px;padding:20px 22px;border-bottom:1px solid #e2e8f0}.reserve-settings-dialog h3{margin:0 0 5px;font-size:20px}.reserve-settings-dialog header p{margin:0;color:#64748b;font-size:13px}.reserve-settings-dialog header button{border:0;background:transparent;font-size:26px;color:#64748b}.settings-body{display:grid;gap:16px;padding:20px 22px}.settings-body label{display:grid;gap:7px;color:#20364f;font-size:13px;font-weight:600}.settings-body select,.settings-body input:not([type=checkbox]),.settings-body textarea{width:100%;box-sizing:border-box;border:1px solid #ccd7e4;border-radius:7px;padding:10px 11px;background:#fff;color:#10243d;font:inherit}.settings-body textarea{resize:vertical}.settings-body label>small,.alert-toggle small{color:#718096;font-weight:400}.settings-balance-note{display:grid;grid-template-columns:1fr auto;gap:7px 16px;padding:12px 14px;border-radius:8px;background:#f6f8fb;color:#5d6d80;font-size:13px}.settings-balance-note b{color:#10243d}.alert-toggle{grid-template-columns:auto 1fr!important;align-items:start;padding:12px 14px;border:1px solid #dce4ed;border-radius:8px}.alert-toggle input{margin-top:3px}.alert-toggle span,.alert-toggle b,.alert-toggle small{display:block}.alert-toggle small{margin-top:4px}.settings-error{margin:0;color:#d32f3a;font-size:13px}.reserve-settings-dialog menu{display:flex;justify-content:flex-end;gap:9px;margin:0;padding:14px 22px;background:#f7f9fb}.reserve-settings-dialog menu button{border:1px solid #ced8e4;border-radius:7px;background:#fff;padding:9px 18px}.reserve-settings-dialog menu .save-button{background:#d89100;border-color:#d89100;color:#fff}.reserve-settings-dialog menu button:disabled{opacity:.6}.direct-topup-dialog{width:min(620px,calc(100vw - 32px))}.direct-topup-body{max-height:65vh;overflow:auto}.form-grid{display:grid;grid-template-columns:1fr 1fr;gap:12px}.direct-topup-warning{display:flex;flex-direction:column;gap:4px;padding:11px 13px;border:1px solid #f1cd7c;border-radius:8px;background:#fff8e8;color:#825900;font-size:12px}@media(max-width:1250px){.reserve-layout{grid-template-columns:1fr}.reserve-detail-panel{min-height:auto}}@media(max-width:560px){.form-grid{grid-template-columns:1fr}}
</style>
