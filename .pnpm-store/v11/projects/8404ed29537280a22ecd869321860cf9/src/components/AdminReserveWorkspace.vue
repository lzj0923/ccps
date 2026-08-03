<template>
  <section class="reserve-layout">
    <div class="panel reserve-list-panel">
      <div class="panel-head">
        <div><h2>{{ $t('legacy.t_99d4291ac8d9') }}</h2><span>{{ filteredAccounts.length }} {{ $t('legacy.t_98b834166518') }}</span></div>
        <span v-if="loading" class="loading-text">{{ $t('legacy.t_6ce3778a43cd') }}</span>
      </div>

      <div v-if="errorMessage" class="reserve-state error" role="alert">
        <strong>{{ $t('legacy.t_88285dfdea7a') }}</strong>
        <span>{{ errorMessage }}</span>
        <button type="button" @click="loadData">{{ $t('legacy.t_0c9157b5bfac') }}</button>
      </div>
      <div v-else class="table-wrap reserve-table-wrap">
        <table>
          <thead>
            <tr><th>{{ $t('legacy.t_7860540047a1') }}</th><th>{{ $t('legacy.t_0c764992bf09') }}</th><th>{{ $t('legacy.t_c7e82f3b6404') }}</th><th>{{ $t('legacy.t_caf5b68402e9') }}</th><th>{{ $t('legacy.t_e9fc5541a8d1') }}</th><th>{{ $t('legacy.t_06db262791f7') }}</th><th>{{ $t('legacy.t_41d9205a68c9') }}</th><th>{{ $t('legacy.t_45293595eae3') }}</th><th>{{ $t('legacy.t_ecfe4930d2c5') }}</th><th>{{ $t('legacy.t_f3ea6d345e2a') }}</th></tr>
          </thead>
          <tbody>
            <tr v-for="account in pagedAccounts" :key="account.id" :class="{ selected: account.id === selectedId }" @click="selectedId = account.id">
              <td><strong>{{ account.ownerName }}</strong><small>{{ account.projectName }} · {{ account.unitNo }}</small></td>
              <td><b :class="{ danger: account.balanceStatus === 'low' }">{{ $t('legacy.t_5e7b60c626a4') }} {{ money(account.currentBalance) }}</b></td>
              <td>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(account.minimumBalance) }}</td>
              <td><b :class="{ danger: Number(account.shortageAmount) > 0 }">{{ $t('legacy.t_5e7b60c626a4') }} {{ money(account.shortageAmount) }}</b></td>
              <td class="positive">{{ $t('legacy.t_5e7b60c626a4') }} {{ money(account.totalTopups) }}</td>
              <td>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(account.totalDebits) }}</td>
              <td>{{ dateTime(account.lastMovementAt) }}</td>
              <td><span class="reserve-tag" :class="account.balanceStatus === 'low' ? 'low' : 'normal'">{{ account.balanceStatus === 'low' ? $t('legacy.t_f0f271b3fb70') : $t('legacy.t_f78d037abccd') }}</span></td>
              <td><span v-if="account.pendingTopupCount" class="reserve-tag pending">{{ account.pendingTopupCount }} {{ $t('legacy.t_94605a3a9af2') }} {{ money(account.pendingTopupAmount) }}</span><span v-else>—</span></td>
              <td><button type="button" class="detail-button" @click.stop="selectedId = account.id">{{ $t('legacy.t_f7acefd2d4cd') }}</button></td>
            </tr>
            <tr v-if="!loading && !filteredAccounts.length"><td colspan="10" class="empty-cell">{{ $t('legacy.t_0fb8878489de') }}</td></tr>
          </tbody>
        </table>
      </div>
      <AdminListPager :page="pageNumber" :page-size="pageSize" :total="filteredAccounts.length" @update:page="pageNumber=$event" @update:page-size="pageSize=$event;pageNumber=1" />
    </div>

    <aside class="panel reserve-detail-panel">
      <template v-if="selectedAccount">
        <div class="account-profile">
          <div class="reserve-avatar">{{ $t('legacy.t_aab9b3921100') }}</div>
          <div><h3>{{ selectedAccount.ownerName }}</h3><p>{{ selectedAccount.projectName }} · {{ selectedAccount.unitNo }}</p></div>
          <span class="reserve-tag" :class="selectedAccount.balanceStatus === 'low' ? 'low' : 'normal'">{{ selectedAccount.balanceStatus === 'low' ? $t('legacy.t_f0f271b3fb70') : $t('legacy.t_f78d037abccd') }}</span>
        </div>
        <div class="detail-actions">
          <button type="button" @click="openSettings(selectedAccount.id)">{{ $t('legacy.t_4d52819cedc6') }}</button>
          <button type="button" @click="openDirectTopup(selectedAccount.id)">{{ $t('legacy.t_b58d8cab2ede') }}</button>
          <button type="button" @click="openRefund(selectedAccount.id)">{{ $t('legacy.t_ccd1ed1a8ce8') }}</button>
          <button type="button" @click="goFinance">{{ $t('legacy.t_8e1062639263') }}</button>
          <button type="button" @click="goMaintenance">{{ $t('legacy.t_962ccbb1ec76') }}</button>
        </div>

        <div class="detail-section">
          <h4><span>{{ $t('legacy.t_356a192b7913') }}</span>{{ $t('legacy.t_8d6d9d0ee687') }}</h4>
          <div class="balance-hero"><small>{{ $t('legacy.t_11421e48dfaf') }}</small><strong>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(selectedAccount.currentBalance) }}</strong></div>
          <div class="balance-track"><i :class="{ low: selectedAccount.balanceStatus === 'low' }" :style="{ width: balanceProgress + '%' }"></i></div>
          <div class="kv"><span>{{ $t('legacy.t_c7e82f3b6404') }}</span><b>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(selectedAccount.minimumBalance) }}</b></div>
          <div class="kv"><span>{{ $t('legacy.t_bfa813347bf2') }}</span><b :class="{ danger: Number(selectedAccount.shortageAmount) > 0 }">{{ $t('legacy.t_5e7b60c626a4') }} {{ money(selectedAccount.shortageAmount) }}</b></div>
          <div class="kv"><span>{{ $t('legacy.t_f076129d286e') }}</span><b>{{ selectedAccount.lowBalanceAlertEnabled ? $t('legacy.t_e05c5ea82fba') : $t('legacy.t_b2a555e14aa5') }}</b></div>
        </div>

        <div class="detail-section">
          <h4><span>{{ $t('legacy.t_da4b9237bacc') }}</span>{{ $t('legacy.t_4ee8e9c9289e') }}</h4>
          <div class="kv"><span>{{ $t('legacy.t_e9fc5541a8d1') }}</span><b class="positive">{{ $t('legacy.t_5e7b60c626a4') }} {{ money(selectedAccount.totalTopups) }}</b></div>
          <div class="kv"><span>{{ $t('legacy.t_06db262791f7') }}</span><b>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(selectedAccount.totalDebits) }}</b></div>
          <div class="kv"><span>{{ $t('legacy.t_8c61b1d8f499') }}</span><b>{{ selectedAccount.pendingTopupCount }} {{ $t('legacy.t_d0bb9b2b8ea7') }}</b></div>
          <div class="kv"><span>{{ $t('legacy.t_5b7e8c6bcab4') }}</span><b class="pending-text">{{ $t('legacy.t_5e7b60c626a4') }} {{ money(selectedAccount.pendingTopupAmount) }}</b></div>
        </div>

        <div class="detail-section transaction-section">
          <h4><span>{{ $t('legacy.t_77de68daecd8') }}</span>{{ $t('legacy.t_ffdc8ecbaeb9') }}</h4>
          <div v-for="item in selectedTransactions" :key="item.id" class="transaction-item">
            <div><b>{{ transactionLabel(item.transactionType) }}</b><small>{{ item.transactionNo || item.workOrderNo || $t('legacy.t_f748a781d56c') }}</small></div>
            <div><strong :class="item.transactionType === 'topup' ? 'positive' : 'danger'">{{ item.transactionType === 'topup' ? '+' : '-' }} {{ $t('legacy.t_5e7b60c626a4') }} {{ money(item.amount) }}</strong><small>{{ dateTime(item.occurredAt) }}</small></div>
          </div>
          <p v-if="!selectedTransactions.length" class="no-transactions">{{ $t('legacy.t_2df027c1a592') }}</p>
        </div>
      </template>
      <div v-else class="reserve-state">{{ $t('legacy.t_e4c7cb02ac64') }}</div>
    </aside>

    <dialog ref="settingsDialog" class="reserve-settings-dialog">
      <form @submit.prevent="saveSettings">
        <header><div><h3>{{ $t('legacy.t_4d52819cedc6') }}</h3><p>{{ $t('legacy.t_6ea525cc9609') }}</p></div><button type="button" :aria-label="$t('legacy.t_ddc05404b0d6')" @click="closeSettings">×</button></header>
        <div class="settings-body">
          <label>{{ $t('legacy.t_7860540047a1') }} <select v-model.number="settings.accountId" required @change="syncSettingsForm">
              <option v-for="account in accounts" :key="account.id" :value="account.id">{{ account.ownerName }} · {{ account.projectName }}／{{ account.unitNo }}</option>
            </select>
          </label>
          <div v-if="settingsAccount" class="settings-balance-note">
            <span>{{ $t('legacy.t_0c764992bf09') }}</span><b>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(settingsAccount.currentBalance) }}</b>
            <span>{{ $t('legacy.t_a0fb3613066e') }}</span><b>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(settingsAccount.minimumBalance) }}</b>
          </div>
          <label>{{ $t('legacy.t_4e6f15c21b45') }} <input v-model="settings.minimumBalance" type="number" min="0" max="9999999999999999.99" step="0.01" required>
            <small>{{ $t('legacy.t_98cf5d678f9a') }}</small>
          </label>
          <label class="alert-toggle"><input v-model="settings.lowBalanceAlertEnabled" type="checkbox"><span><b>{{ $t('legacy.t_14f8eaebe1e7') }}</b><small>{{ $t('legacy.t_3325e83390c3') }}</small></span></label>
          <p v-if="settingsError" class="settings-error">{{ settingsError }}</p>
        </div>
        <menu><button type="button" @click="closeSettings">{{ $t('legacy.t_4d0b4688c787') }}</button><button type="submit" class="save-button" :disabled="settingsSaving">{{ settingsSaving ? $t('legacy.t_8488ea2522af') : $t('legacy.t_bcc070ffd6eb') }}</button></menu>
      </form>
    </dialog>

    <dialog ref="directTopupDialog" class="reserve-settings-dialog direct-topup-dialog">
      <form @submit.prevent="saveDirectTopup">
        <header><div><h3>{{ $t('legacy.t_a4874b3db809') }}</h3><p>{{ $t('legacy.t_5d8265e410fd') }}</p></div><button type="button" :aria-label="$t('legacy.t_ddc05404b0d6')" @click="closeDirectTopup">×</button></header>
        <div class="settings-body direct-topup-body">
          <label>{{ $t('legacy.t_7860540047a1') }} <select v-model.number="topup.accountId" required @change="syncTopupAccount">
              <option v-for="account in accounts" :key="account.id" :value="account.id">{{ account.ownerName }} · {{ account.projectName }}／{{ account.unitNo }}</option>
            </select>
          </label>
          <div v-if="topupAccount" class="settings-balance-note"><span>{{ $t('legacy.t_e73fc0f99c59') }}</span><b>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(topupAccount.currentBalance) }}</b><span>{{ $t('legacy.t_fa393329002d') }}</span><b class="positive">{{ $t('legacy.t_5e7b60c626a4') }} {{ money(topupBalanceAfter) }}</b></div>
          <div class="form-grid">
            <label>{{ $t('legacy.t_eb78b4962199') }}<input v-model="topup.amount" type="number" min="0.01" max="9999999999999999.99" step="0.01" required></label>
            <label>{{ $t('legacy.t_c5769e5a26fc') }}<input v-model="topup.paymentDate" type="date" :max="todayDate" required></label>
          </div>
          <div class="form-grid">
            <label>{{ $t('legacy.t_c6b9a8cfdb21') }}<select v-model="topup.paymentMethod" required><option value="bank_transfer">{{ $t('legacy.t_789957b63e04') }}</option><option value="online_payment">{{ $t('legacy.t_6e249e45b116') }}</option><option value="cash">{{ $t('legacy.t_e3ca5905c270') }}</option><option value="cheque">{{ $t('legacy.t_61b73b219228') }}</option></select></label>
            <label>{{ $t('legacy.t_d61f1eba334a') }}<input v-model.trim="topup.payerName" maxlength="160" required></label>
          </div>
          <label>{{ $t('legacy.t_91cdcd88db53') }}<input v-model.trim="topup.bankReference" maxlength="120" :placeholder="$t('legacy.t_3d562f054029')"></label>
          <label>{{ $t('legacy.t_0f5d56d5a8ca') }}<textarea v-model.trim="topup.note" maxlength="500" rows="3" :placeholder="$t('legacy.t_db2042727391')"></textarea></label>
          <div class="direct-topup-warning"><b>{{ $t('legacy.t_a8a12b1a166c') }}</b><span>{{ $t('legacy.t_5903e5280a96') }}</span></div>
          <p v-if="topupError" class="settings-error">{{ topupError }}</p>
        </div>
        <menu><button type="button" @click="closeDirectTopup">{{ $t('legacy.t_4d0b4688c787') }}</button><button type="submit" class="save-button" :disabled="topupSaving">{{ topupSaving ? $t('legacy.t_6a9b39fafbcc') : $t('legacy.t_cd13c78ecada') }}</button></menu>
      </form>
    </dialog>
    <dialog ref="refundDialog" class="reserve-settings-dialog">
      <form @submit.prevent="saveRefund"><header><div><h3>{{ $t('legacy.t_510aa89fca67') }}</h3><p>{{ $t('legacy.t_c903d11ef138') }}</p></div><button type="button" :aria-label="$t('legacy.t_ddc05404b0d6')" @click="closeRefund">×</button></header><div class="settings-body"><label>{{ $t('legacy.t_7860540047a1') }}<select v-model.number="refund.accountId" required><option v-for="account in accounts" :key="account.id" :value="account.id">{{ account.ownerName }} · {{ account.projectName }}／{{ account.unitNo }}</option></select></label><div v-if="refundAccount" class="settings-balance-note"><span>{{ $t('legacy.t_fda91d281d4c') }}</span><b>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(refundAccount.currentBalance) }}</b><span>{{ $t('legacy.t_c5aae8e63380') }}</span><b class="danger">{{ $t('legacy.t_5e7b60c626a4') }} {{ money(refundBalanceAfter) }}</b></div><div class="form-grid"><label>{{ $t('legacy.t_3f28e52fccf2') }}<input v-model="refund.amount" type="number" min="0.01" :max="refundAccount?.currentBalance" step="0.01" required></label><label>{{ $t('legacy.t_c6b9a8cfdb21') }}<select v-model="refund.paymentMethod"><option value="bank_transfer">{{ $t('legacy.t_789957b63e04') }}</option><option value="cheque">{{ $t('legacy.t_61b73b219228') }}</option><option value="cash">{{ $t('legacy.t_e3ca5905c270') }}</option><option value="other">{{ $t('legacy.t_1a26edf94a81') }}</option></select></label></div><label>{{ $t('legacy.t_ccd7b7ae45f2') }}<textarea v-model.trim="refund.note" maxlength="500" rows="3" :placeholder="$t('legacy.t_29e858bbf36b')"></textarea></label><p v-if="refundError" class="settings-error">{{ refundError }}</p></div><menu><button type="button" @click="closeRefund">{{ $t('legacy.t_4d0b4688c787') }}</button><button type="submit" class="save-button" :disabled="refundSaving">{{ refundSaving ? $t('legacy.t_2cd5496ec548') : $t('legacy.t_18b251b1dab9') }}</button></menu></form>
    </dialog>
  </section>
</template>

<script>
import { createAdminReserveDirectTopup, createAdminReserveRefund, fetchAdminReserveOverview, updateAdminReserveSettings } from '../services/propertyApi';
import AdminListPager from './AdminListPager.vue';

const today = () => {
  const value = new Date();
  value.setMinutes(value.getMinutes() - value.getTimezoneOffset());
  return value.toISOString().slice(0, 10);
};

export default {
  components:{AdminListPager},
  inject: ['page'],
  data() {
    return {
      accounts: [], transactions: [], selectedId: null, loading: false, errorMessage: '', requestSerial: 0,pageNumber:1,pageSize:10,
      settings: { accountId: null, minimumBalance: '', lowBalanceAlertEnabled: true }, settingsSaving: false, settingsError: '',
      todayDate: today(), topup: { accountId: null, amount: '', paymentDate: today(), paymentMethod: 'bank_transfer', payerName: '', bankReference: '', note: '' }, topupSaving: false, topupError: '', refund: { accountId: null, amount: '', paymentMethod: 'bank_transfer', note: '' }, refundSaving: false, refundError: ''
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
    pagedAccounts(){const start=(this.pageNumber-1)*this.pageSize;return this.filteredAccounts.slice(start,start+this.pageSize)},
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
    ,refundNonce() { return this.page.adminReserveRefundNonce; }
    ,topupAccount() { return this.accounts.find(account => account.id === this.topup.accountId) || null; }
    ,topupBalanceAfter() { return Number(this.topupAccount?.currentBalance || 0) + Number(this.topup.amount || 0); }
    ,refundAccount() { return this.accounts.find(account => account.id === this.refund.accountId) || null; }
    ,refundBalanceAfter() { return Math.max(0, Number(this.refundAccount?.currentBalance || 0) - Number(this.refund.amount || 0)); }
  },
  watch: {
    refreshNonce(value, previous) { if (value > previous) this.loadData(); },
    settingsNonce(value, previous) { if (value > previous) this.openSettings(this.selectedAccount?.id); },
    directTopupNonce(value, previous) { if (value > previous) this.openDirectTopup(this.selectedAccount?.id); },
    refundNonce(value, previous) { if (value > previous) this.openRefund(this.selectedAccount?.id); },
    filteredAccounts(rows) { if (!rows.some(row => row.id === this.selectedId)) this.selectedId = rows[0]?.id || null; },'page.globalSearch'(){this.pageNumber=1},'page.moduleSearch'(){this.pageNumber=1},'page.projectFilter'(){this.pageNumber=1},'page.statusFilter'(){this.pageNumber=1}
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
      const t = (key, params) => this.$t(`reserve.${key}`, params);
      return [
        { label: t('totalBalance'), value: `RM ${this.money(summary.totalBalance)}`, delta: t('activeAccountValue', { count: Number(summary.accountCount || 0) }), trend: 'up' },
        { label: t('lowBalanceAccounts'), value: t('accountCountValue', { count: Number(summary.lowBalanceCount || 0) }), delta: t('minimumStandard', { amount: this.money(summary.minimumBalance) }), trend: Number(summary.lowBalanceCount) ? 'down' : 'up' },
        { label: t('monthlyTopups'), value: `RM ${this.money(summary.monthlyTopups)}`, delta: t('postedTransactions'), trend: 'up' },
        { label: t('monthlyDebits'), value: `RM ${this.money(summary.monthlyDebits)}`, delta: t('maintenanceDebits'), trend: Number(summary.monthlyDebits) ? 'down' : '' },
        { label: t('pendingTopups'), value: t('countValue', { count: Number(summary.pendingTopupCount || 0) }), delta: t('pendingAmount', { amount: this.money(summary.pendingTopupAmount) }), trend: Number(summary.pendingTopupCount) ? 'down' : 'up' },
        { label: t('reserveAccounts'), value: t('accountCountValue', { count: Number(summary.accountCount || 0) }), delta: t('validDatabaseAccounts'), trend: 'up' }
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
    openRefund(accountId) { if (!this.accounts.length) return; this.refund = { accountId: accountId || this.selectedAccount?.id || this.accounts[0].id, amount: '', paymentMethod: 'bank_transfer', note: '' }; this.refundError = ''; this.$refs.refundDialog?.showModal(); },
    closeRefund() { if (!this.refundSaving) this.$refs.refundDialog?.close(); },
    async saveRefund() { const amount = Number(this.refund.amount); if (!Number.isFinite(amount) || amount <= 0) { this.refundError = '返還金額必須大於 0。'; return; } if (amount > Number(this.refundAccount?.currentBalance || 0)) { this.refundError = '返還金額不能高於目前預備金餘額。'; return; } this.refundSaving = true; this.refundError = ''; try { const result = await createAdminReserveRefund(this.refund.accountId, { amount: amount.toFixed(2), paymentMethod: this.refund.paymentMethod, note: this.refund.note || null }); this.$refs.refundDialog?.close(); this.selectedId = this.refund.accountId; await this.loadData(); this.page.showToast(`已建立待財務付款返還：${result.referenceNo}`); } catch (error) { this.refundError = error.message || '預備金返還建立失敗'; } finally { this.refundSaving = false; } },
    money(value) { return Number(value || 0).toLocaleString('en-MY', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); },
    dateTime(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '—'; },
    transactionLabel(type) { return ({ topup: '充值入帳', debit: '預備金扣款', adjustment: '餘額調整' })[type] || type || '預備金交易'; }
  }
};
</script>

<style scoped>
.reserve-layout{display:grid;grid-template-columns:minmax(0,1fr) 360px;gap:14px;min-height:560px}.reserve-list-panel,.reserve-detail-panel{min-width:0}.panel-head{display:flex;align-items:center;justify-content:space-between}.panel-head h2{margin:0 0 5px;font-size:18px}.panel-head span,.loading-text{color:#64748b;font-size:12px}.reserve-table-wrap{overflow:auto}.reserve-table-wrap table{min-width:1180px}.reserve-table-wrap tbody tr{cursor:pointer}.reserve-table-wrap tbody tr.selected{background:#fff8e7}.reserve-table-wrap td strong,.reserve-table-wrap td small{display:block}.reserve-table-wrap td small{margin-top:4px;color:#64748b;font-size:11px}.reserve-tag{display:inline-flex;align-items:center;border:1px solid;border-radius:6px;padding:4px 8px;font-size:12px;white-space:nowrap}.reserve-tag.normal{color:#168546;background:#edf9f1;border-color:#b8e6c7}.reserve-tag.low{color:#d12f35;background:#fff1f1;border-color:#ffc5c7}.reserve-tag.pending{color:#b56b00;background:#fff7e5;border-color:#ffd68a}.positive{color:#16934b!important}.danger{color:#e3343e!important}.pending-text{color:#c77900}.detail-button{border:1px solid #d7e1ed;background:#fff;color:#0b4b83;border-radius:6px;padding:5px 11px}.reserve-detail-panel{padding:16px}.account-profile{display:grid;grid-template-columns:48px minmax(0,1fr) auto;align-items:center;gap:12px;padding-bottom:16px;border-bottom:1px solid #e3e9f1}.reserve-avatar{display:grid;place-items:center;width:48px;height:48px;border-radius:50%;background:#3787f5;color:#fff;font-size:20px;font-weight:700}.account-profile h3{margin:0 0 5px;font-size:19px}.account-profile p{margin:0;color:#42566e}.detail-actions{display:grid;grid-template-columns:1fr 1fr;gap:8px;padding:14px 0}.detail-actions button{border:1px solid #d8e1ec;border-radius:6px;background:#fff;padding:9px;color:#0a315f}.detail-actions button:first-child{background:#092f63;color:#fff;border-color:#092f63}.detail-section{padding:15px 0;border-top:1px solid #e3e9f1}.detail-section h4{display:flex;align-items:center;gap:8px;margin:0 0 14px}.detail-section h4>span{display:grid;place-items:center;width:20px;height:20px;border-radius:50%;background:#06346d;color:#fff;font-size:12px}.balance-hero{display:flex;align-items:flex-end;justify-content:space-between;margin-bottom:10px}.balance-hero small{color:#64748b}.balance-hero strong{font-size:22px;color:#0a315f}.balance-track{height:7px;border-radius:8px;background:#e1e7ef;overflow:hidden;margin-bottom:13px}.balance-track i{display:block;height:100%;background:#24a35a;border-radius:inherit}.balance-track i.low{background:#e3a000}.kv{display:flex;justify-content:space-between;gap:16px;padding:5px 0;color:#5b6c80;font-size:13px}.kv b{color:#081b36;text-align:right}.transaction-section{max-height:280px;overflow:auto}.transaction-item{display:flex;justify-content:space-between;gap:12px;padding:9px 0;border-bottom:1px dashed #e1e7ee}.transaction-item>div:last-child{text-align:right}.transaction-item b,.transaction-item strong,.transaction-item small{display:block}.transaction-item small{margin-top:3px;color:#718096;font-size:11px}.no-transactions,.empty-cell{text-align:center;color:#718096}.reserve-state{display:flex;flex-direction:column;align-items:center;justify-content:center;gap:8px;min-height:220px;color:#64748b}.reserve-state.error strong{color:#c93038}.reserve-state button{border:0;border-radius:6px;background:#0a376d;color:#fff;padding:8px 16px}.reserve-settings-dialog{width:min(520px,calc(100vw - 32px));padding:0;border:0;border-radius:12px;box-shadow:0 22px 70px #10233b42}.reserve-settings-dialog::backdrop{background:#0a172a80}.reserve-settings-dialog form{margin:0}.reserve-settings-dialog header{display:flex;justify-content:space-between;gap:16px;padding:20px 22px;border-bottom:1px solid #e2e8f0}.reserve-settings-dialog h3{margin:0 0 5px;font-size:20px}.reserve-settings-dialog header p{margin:0;color:#64748b;font-size:13px}.reserve-settings-dialog header button{border:0;background:transparent;font-size:26px;color:#64748b}.settings-body{display:grid;gap:16px;padding:20px 22px}.settings-body label{display:grid;gap:7px;color:#20364f;font-size:13px;font-weight:600}.settings-body select,.settings-body input:not([type=checkbox]),.settings-body textarea{width:100%;box-sizing:border-box;border:1px solid #ccd7e4;border-radius:7px;padding:10px 11px;background:#fff;color:#10243d;font:inherit}.settings-body textarea{resize:vertical}.settings-body label>small,.alert-toggle small{color:#718096;font-weight:400}.settings-balance-note{display:grid;grid-template-columns:1fr auto;gap:7px 16px;padding:12px 14px;border-radius:8px;background:#f6f8fb;color:#5d6d80;font-size:13px}.settings-balance-note b{color:#10243d}.alert-toggle{grid-template-columns:auto 1fr!important;align-items:start;padding:12px 14px;border:1px solid #dce4ed;border-radius:8px}.alert-toggle input{margin-top:3px}.alert-toggle span,.alert-toggle b,.alert-toggle small{display:block}.alert-toggle small{margin-top:4px}.settings-error{margin:0;color:#d32f3a;font-size:13px}.reserve-settings-dialog menu{display:flex;justify-content:flex-end;gap:9px;margin:0;padding:14px 22px;background:#f7f9fb}.reserve-settings-dialog menu button{border:1px solid #ced8e4;border-radius:7px;background:#fff;padding:9px 18px}.reserve-settings-dialog menu .save-button{background:#d89100;border-color:#d89100;color:#fff}.reserve-settings-dialog menu button:disabled{opacity:.6}.direct-topup-dialog{width:min(620px,calc(100vw - 32px))}.direct-topup-body{max-height:65vh;overflow:auto}.form-grid{display:grid;grid-template-columns:1fr 1fr;gap:12px}.direct-topup-warning{display:flex;flex-direction:column;gap:4px;padding:11px 13px;border:1px solid #f1cd7c;border-radius:8px;background:#fff8e8;color:#825900;font-size:12px}@media(max-width:1250px){.reserve-layout{grid-template-columns:1fr}.reserve-detail-panel{min-height:auto}}@media(max-width:560px){.form-grid{grid-template-columns:1fr}}
</style>
