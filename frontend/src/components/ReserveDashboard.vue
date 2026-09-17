<template>
  <section class="reserve-dashboard-page">
    <div class="reserve-summary-grid">
      <article v-for="card in summaryCards" :key="card.label" class="reserve-summary-card">
        <div class="reserve-summary-icon" v-html="icons[card.icon]"></div>
        <div class="reserve-summary-copy">
          <span>{{ $lt(card.label) }} <i v-if="card.info">{{ $t('legacy.t_042dc4512fa3') }}</i></span>
          <strong :class="card.tone">{{ card.value }}</strong>
          <small v-if="card.badge" class="reserve-balance-badge" :class="{ low: summary.lowBalanceCount }"><b></b>{{ $lt(card.badge) }}</small>
          <small v-else>{{ $lt(card.note) }}</small>
        </div>
        <b class="reserve-card-arrow">›</b>
      </article>
    </div>

    <div v-if="errorMessage" class="reserve-api-message">{{ $lt(errorMessage) }}</div>
    <div class="reserve-layout">
      <main class="reserve-main-column">
        <section class="reserve-warning-banner" :class="{ sufficient: !summary.lowBalanceCount }">
          <div class="reserve-warning-icon">{{ summary.lowBalanceCount ? '!' : '✓' }}</div>
          <div>
            <h2>{{ summary.lowBalanceCount ? $t('legacy.t_27bdafce315f') : $t('legacy.t_439fd2b5313d') }}</h2>
            <p v-if="summary.lowBalanceCount">{{ $t('legacy.t_0f340e08806e') }} {{ summary.lowBalanceCount }} {{ $t('legacy.t_d9426f782a2f') }} {{ money(summary.totalBalance) }}{{ $t('legacy.t_cbb68380d87f') }} {{ money(summary.minimumBalance) }}。</p>
            <p v-else>{{ $t('legacy.t_ded74936b690') }} {{ summary.accountCount || 0 }} {{ $t('legacy.t_9db5cca53e79') }} {{ money(summary.totalBalance) }}。</p>
          </div>
          <button type="button" @click="openTopup"><span v-html="icons.wallet"></span>{{ $t('legacy.t_53c021518f38') }}</button>
        </section>

        <section class="reserve-account-strip">
          <article v-for="account in accounts" :key="account.id" :class="{ low: account.balanceStatus === 'low' }">
            <div><strong>{{ account.projectName }}</strong><small>{{ account.unitNo }}</small></div>
            <div><span>{{ $t('legacy.t_d8a9444716a3') }}</span><b>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(account.currentBalance) }}</b><small>{{ $t('legacy.t_1b374662a41f') }}</small></div>
            <div><span>{{ $t('legacy.t_4e5885f13d62') }}</span><b>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(account.accountingBalance) }}</b><small>{{ $t('legacy.t_e0beeaf302fa') }}</small></div>
            <div><span>{{ $t('legacy.t_c7e82f3b6404') }}</span><b>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(account.minimumBalance) }}</b></div>
            <em>{{ account.balanceStatus === 'low' ? $t('legacy.t_8533f5f6638b') : $t('legacy.t_bb8cb07eec1c') }}</em>
          </article>
        </section>

        <section class="reserve-table-card">
          <header class="reserve-table-toolbar">
            <h2>{{ $t('legacy.t_4bfe4bf89699') }}</h2>
            <div>
              <select v-model="draftFilters.projectId" :aria-label="$t('legacy.t_598a81bde0d4')"><option value="">{{ $t('legacy.t_9d65de4d5c85') }}</option><option v-for="property in properties" :key="property.id" :value="String(property.id)">{{ property.name }}</option></select>
              <div class="reserve-date-fields"><input v-model="draftFilters.startDate" type="date" :aria-label="$t('legacy.t_dca01748b387')"><span>{{ $t('legacy.t_43401e739ef4') }}</span><input v-model="draftFilters.endDate" type="date" :aria-label="$t('legacy.t_1bdd372b56ce')"></div>
              <select v-model="draftFilters.type" :aria-label="$t('legacy.t_ba735143bbd2')"><option value="">{{ $t('legacy.t_e60f5834f6e8') }}</option><option value="topup">{{ $t('legacy.t_fa03c4625313') }}</option><option value="debit">{{ $t('legacy.t_9942375f2604') }}</option><option value="adjustment">{{ $t('legacy.t_91c52ab89104') }}</option><option value="transfer_in">{{ $t('legacy.t_0ccaac91ae82') }}</option><option value="transfer_out">{{ $t('legacy.t_b7fd1c02a7ca') }}</option><option value="transfer_reverse_in">{{ $t('legacy.t_7e1e15885686') }}</option><option value="transfer_reverse_out">{{ $t('legacy.t_f8cc6c312e20') }}</option></select>
              <button type="button" :disabled="loading" @click="applyFilters">{{ loading ? $t('legacy.t_850f6f41e95c') : $t('legacy.t_505ba2176546') }}</button>
              <button type="button" @click="exportDetailsCsv"><span v-html="icons.download"></span>{{ $t('legacy.t_1e3fc8309205') }}</button>
            </div>
          </header>
          <div class="reserve-table-wrap">
            <table class="reserve-transaction-table">
              <thead><tr><th>{{ $t('legacy.t_b6fed9af8313') }}</th><th>{{ $t('legacy.t_d8efa35edf9e') }}</th><th>{{ $t('legacy.t_9b6c1b038aa5') }}</th><th>{{ $t('legacy.t_97deeaee4600') }}</th><th>{{ $t('legacy.t_a0d14fb92288') }}</th><th>{{ $t('legacy.t_45293595eae3') }}</th><th>{{ $t('legacy.t_7ce59565a333') }}</th></tr></thead>
              <tbody>
                <tr v-for="record in pagedRecords" :key="`${record.id}-${record.status}`">
                  <td>{{ formatDateTime(record.occurredAt) }}</td>
                  <td><span class="reserve-type-icon" :class="direction(record.transactionType)" v-html="icons[typeIcon(record.transactionType)]"></span>{{ $lt(typeLabel(record.transactionType)) }}<small>{{ record.projectName }} · {{ record.unitNo }}</small></td>
                  <td :title="record.description">{{ record.description }}</td>
                  <td :class="direction(record.transactionType) === 'in' ? 'reserve-money-in' : 'reserve-money-out'">{{ direction(record.transactionType) === 'in' ? '+' : '-' }} {{ $t('legacy.t_5e7b60c626a4') }} {{ money(record.amount) }}</td>
                  <td>{{ record.status === 'pending' ? '—' : `RM ${money(record.balanceAfter)}` }}</td>
                  <td><span class="reserve-transaction-status" :class="record.status">{{ $lt(statusLabel(record.status)) }}</span></td>
                  <td><strong>{{ record.confirmationStatus === 'confirmed' ? formatDateTime(record.occurredAt) : statusLabel(record.confirmationStatus) }}</strong><small v-if="record.attachmentCount">{{ $t('legacy.t_7279a9c87573') }} {{ record.attachmentCount }} {{ $t('legacy.t_e6e6501b229c') }}</small><small v-else>{{ record.confirmationStatus === 'confirmed' ? $t('legacy.t_ce94edf456b9') : $t('legacy.t_7888cad33170') }}</small></td>
                </tr>
                <tr v-if="loading"><td colspan="7" class="reserve-empty">{{ $t('legacy.t_f1cc1a3941d6') }}</td></tr>
                <tr v-else-if="!pagedRecords.length"><td colspan="7" class="reserve-empty">{{ $t('legacy.t_6a239b589fa8') }}</td></tr>
              </tbody>
            </table>
          </div>
          <footer class="reserve-table-footer">
            <span>{{ $t('legacy.t_3b6ef811b85a') }} {{ transactions.length }} {{ $t('legacy.t_c845e49258c0') }}</span>
            <div><button :disabled="currentPage === 1" @click="currentPage--">‹</button><button v-for="number in totalPages" :key="number" :class="{ active: currentPage === number }" @click="currentPage = number">{{ number }}</button><button :disabled="currentPage === totalPages" @click="currentPage++">›</button><select v-model.number="pageSize"><option :value="8">{{ $t('legacy.t_91b727dc5054') }}</option><option :value="10">{{ $t('legacy.t_fc6da0e815a1') }}</option><option :value="20">{{ $t('legacy.t_93d673672fa5') }}</option></select></div>
          </footer>
        </section>
      </main>

      <aside class="reserve-side-panel">
        <div class="reserve-side-tabs"><button :class="{ active: sideTab === 'notice' }" @click="sideTab = 'notice'">{{ $t('legacy.t_d3d59c09bd0b') }}</button><button :class="{ active: sideTab === 'documents' }" @click="sideTab = 'documents'">{{ $t('legacy.t_2aa75bb6f2c7') }}</button></div>
        <section v-if="sideTab === 'notice'" class="reserve-notice-section">
          <header><h2>{{ $t('legacy.t_acb7cfdd2636') }}</h2><span>{{ $t('legacy.t_3b6ef811b85a') }} {{ notifications.length }} {{ $t('legacy.t_6e38a9adcb74') }}</span></header>
          <div class="reserve-notice-list">
            <article v-for="notice in notifications" :key="notice.id">
              <i :class="noticeTone(notice)" v-html="icons[noticeIcon(notice)]"></i>
              <div><strong>{{ notice.title }}</strong><p>{{ notice.body }}</p></div><b v-if="notice.status === 'unread'"></b>
            </article>
            <p v-if="!notifications.length" class="reserve-side-empty">{{ $t('legacy.t_902c5bc76a4f') }}</p>
          </div>
        </section>
        <section v-if="sideTab === 'documents'" class="reserve-document-section standalone">
          <header><h2>{{ $t('legacy.t_2aa75bb6f2c7') }}</h2><span>{{ $t('legacy.t_3b6ef811b85a') }} {{ documents.length }} {{ $t('legacy.t_aa9f1ad4f91c') }}</span></header>
          <div class="reserve-document-list">
            <article v-for="document in documents" :key="document.id">
              <i :class="document.mimeType === 'application/pdf' ? 'red' : 'green'" v-html="icons.file"></i>
              <div><strong>{{ document.name }}</strong><small>{{ $lt(documentStatusLabel(document.status)) }} · {{ formatSize(document.size) }}</small></div>
              <button type="button" @click="viewDocument(document)">{{ $t('legacy.t_9b4ffb6eff0f') }}</button><button type="button" :aria-label="$t('legacy.t_33e3f60d0c5b')" @click="downloadDocument(document)" v-html="icons.download"></button>
            </article>
            <p v-if="!documents.length" class="reserve-side-empty">{{ $t('legacy.t_94e19936f1fe') }}</p>
          </div>
        </section>
      </aside>
    </div>

    <div v-if="topupVisible" class="reserve-topup-overlay" @pointerdown.self="closeTopup">
      <section class="reserve-topup-modal" role="dialog" aria-modal="true" :aria-label="$t('legacy.t_945911f21066')">
        <header><div><h2>{{ $t('legacy.t_74d47aed3d9f') }}</h2><p>{{ $t('legacy.t_6692b754f72c') }}</p></div><button type="button" :aria-label="$t('legacy.t_ddc05404b0d6')" @click="closeTopup">×</button></header>
        <form @submit.prevent="submitTopup">
          <div class="reserve-topup-form">
            <label class="wide"><span>{{ $t('legacy.t_1a6d8ab93637') }}</span><select v-model="topupForm.reserveAccountId" required><option value="" disabled>{{ $t('legacy.t_06dd9be6b9c7') }}</option><option v-for="account in accounts" :key="account.id" :value="String(account.id)">{{ account.projectName }} · {{ account.unitNo }}{{ $t('legacy.t_830c573ec72d') }} {{ money(account.currentBalance) }}）</option></select></label>
            <label><span>{{ $t('legacy.t_a55e79ad9ac4') }}</span><input v-model="topupForm.amount" type="number" min="0.01" max="1000000" step="0.01" required></label>
            <label><span>{{ $t('legacy.t_058f511c98cf') }}</span><input v-model="topupForm.paymentDate" type="date" :max="today" required></label>
            <label><span>{{ $t('legacy.t_c6b9a8cfdb21') }}</span><select v-model="topupForm.paymentMethod" required><option value="bank_transfer">{{ $t('legacy.t_60157e26d953') }}</option><option value="online_payment">{{ $t('legacy.t_6e249e45b116') }}</option></select></label>
            <label><span>{{ $t('legacy.t_608ae116f54d') }}</span><input v-model.trim="topupForm.payerName" maxlength="160" required></label>
            <label><span>{{ $t('legacy.t_1e007f0fc278') }}</span><input v-model.trim="topupForm.bankName" maxlength="60" required></label>
            <label><span>{{ $t('legacy.t_3d9f09604ecc') }}</span><input v-model.trim="topupForm.reference" maxlength="60" required></label>
            <label class="wide"><span>{{ $t('legacy.t_3ae9ab7d020d') }}</span><textarea v-model.trim="topupForm.note" maxlength="200" rows="2"></textarea></label>
            <label class="wide reserve-topup-files"><span>{{ $t('legacy.t_3df90a318c8f') }}</span><input type="file" accept="image/jpeg,image/png,application/pdf" multiple required @change="selectTopupFiles"><small v-if="topupFiles.length">{{ $t('legacy.t_aeec0b67da9d') }} {{ topupFiles.length }} {{ $t('legacy.t_d6d14543f900') }}{{ topupFiles.map(file => file.name).join('、') }}</small></label>
          </div>
          <p v-if="topupError" class="reserve-topup-error">{{ $lt(topupError) }}</p>
          <footer><button type="button" @click="closeTopup">{{ $t('legacy.t_4d0b4688c787') }}</button><button class="primary" type="submit" :disabled="submitting">{{ submitting ? $t('legacy.t_17e519c5a6bd') : $t('legacy.t_b090888b14f3') }}</button></footer>
        </form>
      </section>
    </div>
  </section>
</template>

<script>
import pageBridge from '../pageBridge';
import { fetchOwnerReserve, fetchReserveDocument, submitReserveTopup } from '../services/propertyApi';
import { downloadCsv } from '../utils/csvExporter';
import { formatDateTime as displayDateTime } from '../utils/dateFormat';

const localDate = value => `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}`;
const defaultFilters = () => {
  const now = new Date();
  return { projectId: '', type: '', startDate: localDate(new Date(now.getFullYear(), now.getMonth() - 5, 1)), endDate: localDate(now) };
};
const defaultTopup = () => ({ reserveAccountId: '', amount: '', paymentDate: localDate(new Date()), paymentMethod: 'bank_transfer', bankName: '', reference: '', payerName: '', note: '' });

export default {
  mixins: [pageBridge],
  data() {
    return {
      response: { summary: {}, accounts: [], properties: [], transactions: [], notifications: [], documents: [] },
      draftFilters: defaultFilters(), filters: defaultFilters(), currentPage: 1, pageSize: 10,
      sideTab: 'notice', loading: false, errorMessage: '', topupVisible: false,
      topupForm: defaultTopup(), topupFiles: [], topupError: '', submitting: false,
      today: localDate(new Date()),
      icons: {
        shield: '<svg viewBox="0 0 24 24" fill="none"><path d="M12 3 4 6v6c0 5 3.4 8 8 10 4.6-2 8-5 8-10V6l-8-3Z"/><path d="m8.5 12 2.2 2.2 4.8-5"/></svg>',
        clipboard: '<svg viewBox="0 0 24 24" fill="none"><rect x="5" y="4" width="14" height="17" rx="2"/><path d="M9 4V2h6v2M8 9h8M8 13h8M8 17h5"/></svg>',
        chart: '<svg viewBox="0 0 24 24" fill="none"><path d="M4 20V11M10 20V7M16 20v-5M22 20V4"/><path d="m4 8 5-4 5 5 7-7"/></svg>',
        trendDown: '<svg viewBox="0 0 24 24" fill="none"><path d="M4 5v5h5M20 19v-5h-5M5 9l5 5 4-4 5 5"/><path d="M4 19h16"/></svg>',
        wallet: '<svg viewBox="0 0 24 24" fill="none"><path d="M4 7h15a2 2 0 0 1 2 2v10H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h13"/><path d="M16 11h5v4h-5a2 2 0 0 1 0-4Z"/></svg>',
        download: '<svg viewBox="0 0 24 24" fill="none"><path d="M12 3v12m0 0 4-4m-4 4-4-4M4 17v3h16v-3"/></svg>',
        arrowUp: '<svg viewBox="0 0 24 24" fill="none"><path d="M12 20V5m0 0-5 5m5-5 5 5"/></svg>',
        arrowDown: '<svg viewBox="0 0 24 24" fill="none"><path d="M12 4v15m0 0-5-5m5 5 5-5"/></svg>',
        adjust: '<svg viewBox="0 0 24 24" fill="none"><path d="M4 7h10M18 7h2M4 17h2M10 17h10"/><circle cx="16" cy="7" r="2"/><circle cx="8" cy="17" r="2"/></svg>',
        bell: '<svg viewBox="0 0 24 24" fill="none"><path d="M18 9a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9ZM10 21h4"/></svg>',
        warning: '<svg viewBox="0 0 24 24" fill="none"><path d="m12 3 10 18H2L12 3Z"/><path d="M12 9v5M12 18h.1"/></svg>',
        building: '<svg viewBox="0 0 24 24" fill="none"><path d="M3 21h18M5 18V8h14v10M8 18v-6M12 18v-6M16 18v-6M4 8l8-5 8 5"/></svg>',
        file: '<svg viewBox="0 0 24 24" fill="none"><path d="M6 2h8l4 4v16H6V2Z"/><path d="M14 2v5h5M9 15h6M9 18h4"/></svg>'
      }
    };
  },
  computed: {
    summary() { return this.response.summary || {}; }, accounts() { return this.response.accounts || []; },
    properties() { return this.response.properties || []; }, transactions() { return this.response.transactions || []; },
    notifications() { return this.response.notifications || []; }, documents() { return this.response.documents || []; },
    summaryCards() { return [
      { icon: 'shield', label: '业主账单余额', value: `RM ${this.money(this.summary.totalBalance)}`, badge: this.summary.lowBalanceCount ? `${this.summary.lowBalanceCount} 个账户不足` : '按入账日期', info: true },
      { icon: 'clipboard', label: '会计余额', value: `RM ${this.money(this.summary.accountingBalance)}`, note: '按实际收款日期', info: true },
      { icon: 'clipboard', label: '最低預備金標準', value: `RM ${this.money(this.summary.minimumBalance)}`, note: `${this.summary.accountCount || 0} 個預備金賬戶`, info: true },
      { icon: 'chart', label: '累計充值', value: `RM ${this.money(this.summary.totalTopups)}`, note: `共 ${this.summary.topupCount || 0} 筆已確認充值`, tone: 'positive', info: true },
      { icon: 'trendDown', label: '累計扣款', value: `RM ${this.money(this.summary.totalDebits)}`, note: `共 ${this.summary.debitCount || 0} 筆扣款`, tone: 'negative', info: true }
    ]; },
    totalPages() { return Math.max(1, Math.ceil(this.transactions.length / this.pageSize)); },
    pagedRecords() { return this.transactions.slice((this.currentPage - 1) * this.pageSize, this.currentPage * this.pageSize); }
  },
  watch: { pageSize() { this.currentPage = 1; } },
  mounted() { this.loadData(); },
  methods: {
    async loadData(notify = false) { this.loading = true; this.errorMessage = ''; try { this.response = await fetchOwnerReserve(this.filters); if (notify) this.showToast(this.$ltf`已查詢 ${this.transactions.length} 條預備金記錄`); } catch (error) { this.errorMessage = error.message || '預備金數據讀取失敗'; } finally { this.loading = false; } },
    async applyFilters() { this.filters = { ...this.draftFilters }; this.currentPage = 1; await this.loadData(true); },
    openTopup() { if (!this.accounts.length) { this.showToast('目前沒有可充值的預備金賬戶'); return; } this.topupForm = { ...defaultTopup(), reserveAccountId: String(this.accounts.find(item => item.balanceStatus === 'low')?.id || this.accounts[0].id) }; this.topupFiles = []; this.topupError = ''; this.topupVisible = true; },
    closeTopup() { if (!this.submitting) this.topupVisible = false; },
    selectTopupFiles(event) { this.topupFiles = Array.from(event.target.files || []).slice(0, 4); },
    async submitTopup() { if (!this.topupFiles.length) { this.topupError = '請上傳至少一個付款憑證'; return; } this.submitting = true; this.topupError = ''; try { await submitReserveTopup(this.topupForm, this.topupFiles); this.topupVisible = false; await this.loadData(); this.showToast('充值申請已提交，等待財務審核'); } catch (error) { this.topupError = error.message || '充值申請提交失敗'; } finally { this.submitting = false; } },
    async viewDocument(document) { try { const { blob } = await fetchReserveDocument(document.id); const url = URL.createObjectURL(blob); window.open(url, '_blank', 'noopener'); setTimeout(() => URL.revokeObjectURL(url), 60000); } catch (error) { this.showToast(error.message || '文件預覽失敗'); } },
    async downloadDocument(document) { try { const { blob } = await fetchReserveDocument(document.id, true); const url = URL.createObjectURL(blob); const link = document.createElement('a'); link.href = url; link.download = document.name; link.click(); URL.revokeObjectURL(url); } catch (error) { this.showToast(error.message || '文件下載失敗'); } },
    money(value) { return Number(value || 0).toLocaleString('en-MY', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); },
    direction(type) { return ['debit','transfer_out','transfer_reverse_out'].includes(type) ? 'out' : 'in'; }, typeIcon(type) { return this.direction(type) === 'out' ? 'arrowDown' : type === 'adjustment' ? 'adjust' : 'arrowUp'; },
    typeLabel(type) { return { topup: '充值', debit: '扣款', adjustment: '人工調整', transfer_in: '内部调拨转入', transfer_out: '内部调拨转出', transfer_reverse_in: '调拨冲正转入', transfer_reverse_out: '调拨冲正转出' }[type] || type; },
    statusLabel(status) { return { confirmed: '已確認', pending: '待審核', rejected: '已拒絕', active: '可用' }[status] || status || '-'; },
    documentStatusLabel(status) { return { pending_review: '待審核', active: '已確認', rejected: '已拒絕' }[status] || status; },
    formatDateTime(value) { return displayDateTime(value, '-'); },
    formatSize(value) { const bytes = Number(value || 0); return bytes >= 1048576 ? `${(bytes / 1048576).toFixed(1)} MB` : `${Math.max(1, Math.round(bytes / 1024))} KB`; },
    noticeIcon(notice) { const text = `${notice.title} ${notice.body}`; return text.includes('預備金') ? 'warning' : text.includes('維修') ? 'building' : 'bell'; },
    noticeTone(notice) { return notice.priority === 'high' ? 'orange' : notice.status === 'unread' ? 'gold' : 'blue'; },
    exportDetails() { const header = ['日期', '房產', '單位', '類型', '說明', '變動金額', '餘額', '狀態']; const body = this.transactions.map(item => [this.formatDateTime(item.occurredAt), item.projectName, item.unitNo, this.typeLabel(item.transactionType), item.description, `${this.direction(item.transactionType) === 'in' ? '+' : '-'} RM ${this.money(item.amount)}`, item.status === 'pending' ? '-' : `RM ${this.money(item.balanceAfter)}`, this.statusLabel(item.status)]); const csv = [header, ...body].map(row => row.map(value => `"${String(value).replaceAll('"', '""')}"`).join(',')).join('\n'); const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' }); const link = document.createElement('a'); link.href = URL.createObjectURL(blob); link.download = '預備金交易明細.csv'; link.click(); URL.revokeObjectURL(link.href); this.showToast('預備金交易明細已匯出'); }
    ,
    exportDetailsCsv() {
      const headers = ['日期', '房產', '單位', '類型', '說明', '變動金額', '餘額', '狀態'];
      const rows = this.transactions.map((item) => [this.formatDateTime(item.occurredAt), item.projectName, item.unitNo, this.typeLabel(item.transactionType), item.description, `${this.direction(item.transactionType) === 'in' ? '+' : '-'} RM ${this.money(item.amount)}`, item.status === 'pending' ? '-' : `RM ${this.money(item.balanceAfter)}`, this.statusLabel(item.status)]);
      downloadCsv('預備金交易明細.csv', headers, rows);
      this.showToast('預備金交易明細已匯出');
    },
  }
};
</script>
