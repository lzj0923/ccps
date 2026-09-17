<template>
  <section class="expense-maintenance-page">
    <div class="expense-summary-grid">
      <article v-for="card in summaryCards" :key="card.label" class="expense-summary-card">
        <div class="expense-summary-icon" v-html="icons[card.icon]"></div>
        <div>
          <span>{{ $lt(card.label) }}</span>
          <strong>{{ card.value }}</strong>
          <small :class="card.tone">{{ $lt(card.note) }} <b v-if="card.delta">{{ card.delta }}</b></small>
        </div>
      </article>
    </div>

    <div v-if="errorMessage" class="expense-api-message error">{{ $lt(errorMessage) }}</div>
    <div class="expense-workspace" :class="{ 'detail-hidden': !detailVisible || !maintenanceDetail }">
      <div class="expense-main-column">
        <form class="expense-filter-card" @submit.prevent="applyFilters">
          <label><span>{{ $t('legacy.t_598a81bde0d4') }}</span><select v-model="draftFilters.projectId"><option value="">{{ $t('legacy.t_9d65de4d5c85') }}</option><option v-for="item in properties" :key="item.id" :value="String(item.id)">{{ item.name }}</option></select></label>
          <label><span>{{ $t('legacy.t_0cf468db12ee') }}</span><select v-model="draftFilters.category"><option value="">{{ $t('legacy.t_19817baeee19') }}</option><option v-for="item in categories" :key="item" :value="item">{{ $lt(categoryLabel(item)) }}</option></select></label>
          <label class="expense-date-filter"><span>{{ $t('legacy.t_55f16d9c2319') }}</span><div class="expense-date-range"><input v-model="draftFilters.startDate" type="date" :aria-label="$t('legacy.t_7fd7a227e9ce')"><em>{{ $t('legacy.t_43401e739ef4') }}</em><input v-model="draftFilters.endDate" type="date" :aria-label="$t('legacy.t_27eefa5237a0')"></div></label>
          <label><span>{{ $t('legacy.t_45293595eae3') }}</span><select v-model="draftFilters.status"><option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ $lt(item.label) }}</option></select></label>
          <div class="expense-filter-actions"><button class="expense-query-button" type="submit" :disabled="loading">{{ loading ? $t('legacy.t_850f6f41e95c') : $t('legacy.t_505ba2176546') }}</button><button type="button" @click="resetFilters">{{ $t('legacy.t_3d81345303ab') }}</button></div>
          <button class="expense-export-button" type="button" @click="exportRecords"><span v-html="icons.download"></span>{{ $t('legacy.t_894aa2f237cc') }}</button>
        </form>

        <section class="expense-table-card">
          <div class="expense-tabs">
            <button v-for="tab in tabs" :key="tab.key" :class="{ active: activeTab === tab.key }" @click="changeTab(tab.key)">{{ $lt(tab.label) }}</button>
          </div>
          <div class="expense-table-wrap">
            <table class="expense-record-table">
              <thead><tr><th></th><th>{{ $t('legacy.t_b6fed9af8313') }}</th><th>{{ $t('legacy.t_070276a68f73') }}</th><th>{{ $t('legacy.t_16ed763edcda') }}</th><th>{{ $t('legacy.t_e354c289aab2') }}</th><th>{{ $t('legacy.t_57de68258b51') }}</th><th>{{ $t('legacy.t_5c0ec3674a79') }}</th><th>{{ $t('legacy.t_99f6fe6c41ad') }}</th><th>{{ $t('legacy.t_e81e9a4a92a1') }}</th></tr></thead>
              <tbody>
                <tr v-for="record in pagedRecords" :key="record.key" :class="{ selected: selectedKey === record.key }" @click="selectRecord(record)">
                  <td><span class="expense-radio" :class="{ checked: selectedKey === record.key }"><i></i></span></td>
<td>{{ displayDate(record.date) }}</td>
                  <td><strong class="expense-unit-project">{{ record.project }}</strong><small class="expense-unit-no">{{ record.unit }}</small></td>
                  <td><span class="expense-category-icon" :class="record.tone" v-html="icons[record.icon]"></span>{{ record.category }}</td>
                  <td :title="record.description">{{ record.description }}</td>
                  <td>{{ record.amount }}</td>
                  <td><span class="expense-deduct" :class="record.deduct ? 'yes' : 'no'">{{ record.deduct ? '✓' : '×' }}</span>{{ record.deduct ? $t('ui.reserveDeductedAmount', { amount: record.reserveAmount }) : $t('legacy.t_8bf5c10ad937') }}</td>
                  <td><button v-if="record.attachmentCount" type="button" class="expense-attachment-button" :title="$t('ui.attachmentCountTitle', { count: record.attachmentCount })" @click.stop="selectRecord(record)"><span v-html="icons.image"></span>{{ $t('legacy.t_76a786dd72cb') }}{{ record.attachmentCount }}）</button><span v-else class="expense-no-attachment">—</span></td>
                  <td><span class="expense-status" :class="statusClass(record.status)">{{ $lt(record.status) }}</span></td>
                </tr>
                <tr v-if="loading"><td colspan="9" class="expense-empty">{{ $t('legacy.t_f1cc1a3941d6') }}</td></tr>
                <tr v-else-if="!pagedRecords.length"><td colspan="9" class="expense-empty">{{ $t('legacy.t_59e737cd9720') }}</td></tr>
              </tbody>
            </table>
          </div>
          <footer class="expense-table-footer">
            <span>{{ $t('legacy.t_3b6ef811b85a') }} {{ filteredRecords.length }} {{ $t('legacy.t_c845e49258c0') }}</span>
            <div><button :disabled="currentPage === 1" @click="currentPage--">‹</button><button v-for="number in totalPages" :key="number" :class="{ active: currentPage === number }" @click="currentPage = number">{{ number }}</button><button :disabled="currentPage === totalPages" @click="currentPage++">›</button><select v-model.number="pageSize"><option :value="8">{{ $t('legacy.t_91b727dc5054') }}</option><option :value="10">{{ $t('legacy.t_fc6da0e815a1') }}</option><option :value="20">{{ $t('legacy.t_93d673672fa5') }}</option></select></div>
          </footer>
        </section>
      </div>

      <aside v-if="detailVisible && maintenanceDetail" class="maintenance-detail-panel">
        <header><h2>{{ $t('legacy.t_8a30b68ec405') }}</h2><button :aria-label="$t('legacy.t_937920c85654')" @click="detailVisible = false">×</button></header>
        <div class="maintenance-detail-body">
          <div class="maintenance-detail-title"><span v-html="icons.wrench"></span><h3>{{ maintenanceDetail.title }}</h3><b :class="statusClass(statusLabel(maintenanceDetail.status))">{{ $lt(statusLabel(maintenanceDetail.status)) }}</b></div>
          <dl class="maintenance-info-grid">
            <div><dt>{{ $t('legacy.t_bd238174d47b') }}</dt><dd>{{ formatDateTime(maintenanceDetail.requestedAt) }}</dd></div>
            <div><dt>{{ $t('legacy.t_070276a68f73') }}</dt><dd>{{ maintenanceDetail.projectName }}<br>{{ maintenanceDetail.unitNo }}</dd></div>
            <div><dt>{{ $t('legacy.t_89f374f708cf') }}</dt><dd>{{ maintenanceDetail.workOrderNo }}</dd></div>
            <div><dt>{{ $t('legacy.t_f2ec90c7d802') }}</dt><dd>{{ $lt(categoryLabel(maintenanceDetail.category)) }}<br>{{ maintenanceDetail.vendorName || $t('legacy.t_4e4ceafa8ee3') }}</dd></div>
          </dl>

          <section class="maintenance-progress-section">
            <h4>{{ $t('legacy.t_6c4c34e8852d') }}</h4>
            <div class="maintenance-progress-track">
              <div v-for="(step, index) in progressSteps" :key="step.status" :class="{ done: isStepDone(index), current: isCurrentStep(index) }">
                <i>{{ isStepDone(index) ? '✓' : index + 1 }}</i><strong>{{ $lt(step.label) }}</strong><small>{{ step.time }}</small>
              </div>
            </div>
          </section>

          <section class="maintenance-invoice-section">
            <h4>{{ $t('legacy.t_9bddaa9e3bbf') }}</h4>
            <button v-for="invoice in invoiceAttachments" :key="invoice.documentId" type="button" @click="downloadAttachment(invoice)"><span v-html="icons.file"></span><div><strong>{{ invoice.name }}</strong><small>{{ formatSize(invoice.size) }} · {{ formatDateTime(invoice.uploadedAt) }}</small></div><i v-html="icons.download"></i></button>
            <p v-if="maintenancePaymentCompleted && !invoiceAttachments.length" class="maintenance-invoice-note">{{ $t('legacy.t_33c396bdb799') }}</p>
            <button v-if="!maintenancePaymentCompleted" class="maintenance-invoice-upload" type="button" :disabled="uploading" @click="$refs.invoiceFile.click()"><span v-html="icons.file"></span><div><strong>{{ $t('legacy.t_43f71953ccac') }}</strong><small>{{ $t('legacy.t_347a38c49643') }}</small></div><i>＋</i></button>
            <input v-if="!maintenancePaymentCompleted" ref="invoiceFile" class="maintenance-file-input" type="file" accept="image/jpeg,image/png,application/pdf" multiple @change="uploadFiles('invoice', $event)">
          </section>

          <section class="maintenance-finance-section">
            <h4>{{ $t('legacy.t_824cf414dfa3') }}</h4>
            <div class="maintenance-finance-grid">
              <dl><div><dt>{{ $t('legacy.t_6ad972081fc5') }}</dt><dd>{{ $t('legacy.t_5e7b60c626a4') }} {{ formatMoney(maintenanceAmount) }}</dd></div><div><dt>{{ $t('legacy.t_02ef3b2c49a3') }}</dt><dd>{{ Number(maintenanceDetail.reserveDeductedAmount) > 0 ? $t('legacy.t_30160a21b92a') : $t('legacy.t_8bf5c10ad937') }}</dd></div><div><dt>{{ $t('legacy.t_727af713738d') }}</dt><dd>{{ $t('legacy.t_5e7b60c626a4') }} {{ formatMoney(maintenanceDetail.reserveDeductedAmount) }}</dd></div></dl>
<dl><div><dt>{{ $t('legacy.t_607b3e1024c4') }}</dt><dd>{{ $lt(paymentStatusLabel(maintenanceDetail.paymentStatus, maintenanceDetail.confirmationStatus, maintenanceDetail.paymentMethod)) }}</dd></div><div><dt>{{ $t('legacy.t_058f511c98cf') }}</dt><dd>{{ displayDate(maintenanceDetail.paymentDate) }}</dd></div><div><dt>{{ $t('legacy.t_c6b9a8cfdb21') }}</dt><dd>{{ $lt(paymentMethodLabel(maintenanceDetail.paymentMethod)) }}</dd></div></dl>
            </div>
          </section>
        </div>
      </aside>
      <button v-else-if="maintenanceDetail" class="maintenance-detail-reopen" @click="detailVisible = true"><span v-html="icons.wrench"></span>{{ $t('legacy.t_a6b85e77ecb7') }}</button>
    </div>
  </section>
</template>

<script>
import { formatDate } from '../utils/dateFormat';
import pageBridge from '../pageBridge';
import { fetchMaintenanceAttachment, fetchMaintenanceDetail, fetchOwnerExpenses, uploadMaintenanceAttachments } from '../services/propertyApi';
import { downloadCsv } from '../utils/csvExporter';

const monthFilters = () => {
  const now = new Date();
  const first = new Date(now.getFullYear(), now.getMonth(), 1);
  const last = new Date(now.getFullYear(), now.getMonth() + 1, 0);
  const localDate = value => `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}`;
  return { projectId: '', category: '', startDate: localDate(first), endDate: localDate(last), status: '' };
};

const CATEGORY_LABELS = { utilities: '水電費', management: '管理費', cleaning: '清潔費', maintenance: '維修費', plumbing: '管道維修', air_conditioning: '冷氣維修', electrical: '電器維修', other: '其他支出' };
const STATUS_LABELS = { submitted: '待處理', assigned: '已指派', in_progress: '處理中', inspection: '待驗收', completed: '已完成', cancelled: '已取消' };
const STATUS_ORDER = ['submitted', 'assigned', 'in_progress', 'inspection', 'completed'];

export default {
  mixins: [pageBridge],
  data() {
    return {
      response: { summary: {}, properties: [], categories: [], expenses: [], maintenance: [] },
      maintenanceDetail: null,
      selectedKey: '',
      activeTab: 'expense',
      detailVisible: true,
      currentPage: 1,
      pageSize: 8,
      draftFilters: monthFilters(),
      filters: monthFilters(),
      loading: false,
      uploading: false,
      errorMessage: '',
      tabs: [{ key: 'expense', label: '支出記錄' }, { key: 'maintenance', label: '維修記錄' }],
      icons: {
        wallet: '<svg viewBox="0 0 24 24" fill="none"><path d="M4 7h15a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h13"/><path d="M16 12h5v4h-5a2 2 0 0 1 0-4Z"/></svg>',
        wrench: '<svg viewBox="0 0 24 24" fill="none"><path d="M14 6a5 5 0 0 0-6.5 6.5L3 17l4 4 4.5-4.5A5 5 0 0 0 18 10l-3 3-4-4 3-3Z"/></svg>',
        shield: '<svg viewBox="0 0 24 24" fill="none"><path d="M12 3 4 6v6c0 5 3.4 8 8 10 4.6-2 8-5 8-10V6l-8-3Z"/><path d="m8.5 12 2.2 2.2 4.8-5"/></svg>',
        bell: '<svg viewBox="0 0 24 24" fill="none"><path d="M18 9a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9ZM10 21h4"/></svg>',
        water: '<svg viewBox="0 0 24 24" fill="none"><path d="M12 3s6 6.7 6 11a6 6 0 1 1-12 0c0-4.3 6-11 6-11Z"/></svg>',
        users: '<svg viewBox="0 0 24 24" fill="none"><circle cx="12" cy="8" r="3"/><path d="M6 20v-2a6 6 0 0 1 12 0v2M5 9a2.5 2.5 0 0 0 0 5M19 9a2.5 2.5 0 0 1 0 5"/></svg>',
        clean: '<svg viewBox="0 0 24 24" fill="none"><path d="m9 9 6-6M11 7l6 6M7 21h12l-2-8H9l-2 8Z"/></svg>',
        more: '<svg viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="9"/><path d="M8 12h.1M12 12h.1M16 12h.1"/></svg>',
        file: '<svg viewBox="0 0 24 24" fill="none"><path d="M6 2h8l4 4v16H6V2Z"/><path d="M14 2v5h5M9 15h6M9 18h4"/></svg>',
        image: '<svg viewBox="0 0 24 24" fill="none"><rect x="3" y="4" width="18" height="16" rx="2"/><circle cx="9" cy="9" r="2"/><path d="m5 18 5-5 3 3 2-2 4 4"/></svg>',
        download: '<svg viewBox="0 0 24 24" fill="none"><path d="M12 3v12m0 0 4-4m-4 4-4-4M4 17v3h16v-3"/></svg>'
      }
    };
  },
  computed: {
    properties() { return this.response.properties || []; },
    categories() {
      if (this.activeTab === 'expense') return this.response.categories || [];
      return [...new Set((this.response.maintenance || []).map(item => item.category))].sort();
    },
    statusOptions() {
      return this.activeTab === 'maintenance'
        ? [{ value: '', label: '全部狀態' }, ...STATUS_ORDER.map(value => ({ value, label: STATUS_LABELS[value] })), { value: 'cancelled', label: '已取消' }]
        : [{ value: '', label: '全部狀態' }, { value: 'completed', label: '已完成' }, { value: 'processing', label: '處理中' }, { value: 'pending', label: '待處理' }];
    },
    summaryCards() {
      const summary = this.response.summary || {};
      return [
        this.summaryCard('wallet', '本月支出', summary.monthlyExpense, summary.expenseChangePercent),
        this.summaryCard('wrench', '本月維修費用', summary.monthlyMaintenanceExpense, summary.maintenanceChangePercent),
        { icon: 'shield', label: '從預備金扣除金額', value: `RM ${this.formatMoney(summary.reserveDeductedAmount)}`, note: `本月共 ${summary.reserveDebitCount || 0} 筆扣除`, delta: '', tone: '' },
        { icon: 'bell', label: '待處理維修事項', value: `${summary.pendingMaintenanceCount || 0} 項`, note: '需及時跟進處理', delta: '', tone: '' }
      ];
    },
    expenseRecords() { return (this.response.expenses || []).map(item => this.mapExpense(item)); },
    maintenanceRecords() { return (this.response.maintenance || []).map(item => this.mapMaintenance(item)); },
    records() { return this.activeTab === 'expense' ? this.expenseRecords : this.maintenanceRecords; },
    filteredRecords() {
      if (this.activeTab !== 'expense' || !this.filters.status) return this.records;
      return this.records.filter(record => this.statusClass(record.status) === this.filters.status);
    },
    totalPages() { return Math.max(1, Math.ceil(this.filteredRecords.length / this.pageSize)); },
    pagedRecords() { return this.filteredRecords.slice((this.currentPage - 1) * this.pageSize, this.currentPage * this.pageSize); },
    invoiceAttachments() { return (this.maintenanceDetail?.attachments || []).filter(item => item.relationType === 'invoice'); },
    maintenancePaymentCompleted() {
      const detail = this.maintenanceDetail;
      return detail?.paymentStatus === 'paid' && detail?.confirmationStatus === 'confirmed';
    },
    maintenanceAmount() { return this.maintenanceDetail?.actualAmount ?? 0; },
    progressSteps() {
      const history = this.maintenanceDetail?.history || [];
      return STATUS_ORDER.map(status => {
        const event = history.find(item => item.status === status);
        return { status, label: STATUS_LABELS[status], time: event ? this.formatStepTime(event.occurredAt) : '' };
      });
    },
    progressIndex() { return Math.max(0, STATUS_ORDER.indexOf(this.maintenanceDetail?.status)); }
  },
  watch: {
    pageSize() { this.currentPage = 1; }
  },
  mounted() { this.loadData(); },
  methods: {
    displayDate(value) { return formatDate(value, '-'); },
    async loadData(notify = false) {
      this.loading = true;
      this.errorMessage = '';
      try {
        const query = { projectId: this.filters.projectId, category: this.filters.category, startDate: this.filters.startDate, endDate: this.filters.endDate };
        if (this.activeTab === 'maintenance') query.status = this.filters.status;
        this.response = await fetchOwnerExpenses(query);
        const availableMaintenanceIds = new Set((this.response.maintenance || []).map(item => item.id));
        if (this.maintenanceDetail && !availableMaintenanceIds.has(this.maintenanceDetail.id)) {
          this.clearMaintenanceDetail();
        }
        if (this.activeTab === 'maintenance' && !this.maintenanceDetail && this.response.maintenance?.length) {
          await this.loadMaintenance(this.response.maintenance[0].id);
        }
        if (notify) this.showToast(this.$ltf`已查詢 ${this.records.length} 條記錄`);
      } catch (error) {
        this.errorMessage = error.message || '支出與維修數據讀取失敗';
      } finally {
        this.loading = false;
      }
    },
    async loadMaintenance(workOrderId) {
      try {
        this.maintenanceDetail = await fetchMaintenanceDetail(workOrderId);
        this.selectedKey = `m-${workOrderId}`;
        this.detailVisible = true;
      } catch (error) {
        this.errorMessage = error.message || '維修詳情讀取失敗';
      }
    },
    selectRecord(record) {
      this.selectedKey = record.key;
      if (record.workOrderId) this.loadMaintenance(record.workOrderId);
      else {
        this.clearMaintenanceDetail();
        this.showToast('此支出沒有關聯維修單');
      }
    },
    async changeTab(tab) {
      this.activeTab = tab;
      this.draftFilters.category = '';
      this.draftFilters.status = '';
      this.filters.category = '';
      this.filters.status = '';
      this.currentPage = 1;
      this.clearMaintenanceDetail();
      await this.loadData();
    },
    async applyFilters() {
      this.filters = { ...this.draftFilters };
      this.currentPage = 1;
      await this.loadData(true);
    },
    async resetFilters() {
      this.draftFilters = monthFilters();
      this.filters = monthFilters();
      this.currentPage = 1;
      await this.loadData();
      this.showToast('支出篩選已重置');
    },
    clearMaintenanceDetail() {
      this.maintenanceDetail = null;
      this.detailVisible = false;
    },
    async uploadFiles(relationType, event) {
      const files = Array.from(event.target.files || []);
      event.target.value = '';
      if (!files.length || !this.maintenanceDetail) return;
      this.uploading = true;
      try {
        if (relationType !== 'invoice') return;
        const attachments = await uploadMaintenanceAttachments(this.maintenanceDetail.id, relationType, files);
        this.maintenanceDetail = { ...this.maintenanceDetail, attachments };
        const maintenance = (this.response.maintenance || []).map(item => item.id === this.maintenanceDetail.id ? { ...item, attachmentCount: attachments.length } : item);
        const expenses = (this.response.expenses || []).map(item => item.workOrderId === this.maintenanceDetail.id ? { ...item, attachmentCount: attachments.length } : item);
        this.response = { ...this.response, maintenance, expenses };
        this.showToast('附件已上傳');
      } catch (error) {
        this.showToast(error.message || '附件上傳失敗');
      } finally {
        this.uploading = false;
      }
    },
    async downloadAttachment(attachment) {
      try {
        const { blob } = await fetchMaintenanceAttachment(attachment.documentId, true);
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a'); link.href = url; link.download = attachment.name; link.click(); URL.revokeObjectURL(url);
      } catch (error) { this.showToast(error.message || '附件下載失敗'); }
    },
    mapExpense(item) {
      const status = item.paymentStatus === 'paid' && item.confirmationStatus === 'confirmed' ? '已完成' : item.paymentStatus === 'paid' || item.paymentStatus === 'partial' ? '處理中' : '待處理';
      return { key: `e-${item.id}`, workOrderId: item.workOrderId, date: item.occurredOn, project: item.projectName, unit: item.unitNo, category: this.categoryLabel(item.category), description: item.description, amount: this.formatMoney(item.amount), reserveAmount: this.formatMoney(item.reserveDeductedAmount), deduct: Number(item.reserveDeductedAmount) > 0, attachmentCount: item.attachmentCount || 0, status, icon: this.categoryIcon(item.category), tone: this.categoryTone(item.category) };
    },
    mapMaintenance(item) {
      return { key: `m-${item.id}`, workOrderId: item.id, date: (item.requestedAt || '').slice(0, 10), project: item.projectName, unit: item.unitNo, category: this.categoryLabel(item.category), description: item.title, amount: this.formatMoney(item.amount), reserveAmount: this.formatMoney(item.reserveDeductedAmount), deduct: Number(item.reserveDeductedAmount) > 0, attachmentCount: item.attachmentCount || 0, status: this.statusLabel(item.status), icon: 'wrench', tone: 'blue' };
    },
    summaryCard(icon, label, value, change) {
      const number = Number(change || 0);
      return { icon, label, value: `RM ${this.formatMoney(value)}`, note: '較上月', delta: `${number > 0 ? '↑' : number < 0 ? '↓' : '—'} ${Math.abs(number).toFixed(2)}%`, tone: number <= 0 ? 'positive' : 'negative' };
    },
    statusLabel(value) { return STATUS_LABELS[value] || value || '待處理'; },
    statusClass(status) { return { '已完成': 'completed', '處理中': 'processing', '已指派': 'processing', '待驗收': 'processing', '待處理': 'pending', '已取消': 'pending' }[status] || 'pending'; },
    categoryLabel(value) { return CATEGORY_LABELS[value] || value || '其他支出'; },
    categoryIcon(value) { return { utilities: 'water', management: 'users', cleaning: 'clean', maintenance: 'wrench', plumbing: 'wrench', air_conditioning: 'wrench' }[value] || 'more'; },
    categoryTone(value) { return { management: 'orange', cleaning: 'green', other: 'gray' }[value] || 'blue'; },
    isStepDone(index) { return this.maintenanceDetail?.status === 'completed' || index < this.progressIndex; },
    isCurrentStep(index) { return this.maintenanceDetail?.status !== 'completed' && index === this.progressIndex; },
    formatMoney(value) { return Number(value || 0).toLocaleString('en-MY', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); },
    formatDateTime(value) { return value ? value.replace('T', ' ').slice(0, 16) : '-'; },
    formatStepTime(value) { return value ? value.slice(5, 16).replace('T', ' ') : ''; },
    formatSize(value) { const bytes = Number(value || 0); return bytes >= 1024 * 1024 ? `${(bytes / 1024 / 1024).toFixed(1)} MB` : `${Math.max(1, Math.round(bytes / 1024))} KB`; },
    paymentStatusLabel(payment, confirmation, method) { if (payment === 'paid' && confirmation === 'confirmed' && method === 'reserve_account') return '預備金已扣'; if (payment === 'paid' && confirmation === 'confirmed') return '已付款'; if (payment === 'paid') return '待審核'; if (payment === 'partial') return '部分付款'; return '待付款'; },
    paymentMethodLabel(value) { return { bank_transfer: '銀行轉賬', online_payment: '線上支付', reserve_account: '預備金', cash: '現金' }[value] || value || '-'; },
    exportRecordsLegacy() {
      const header = ['日期', '房產', '單位', '類別', '項目說明', '金額', '預備金扣除', '處理狀態'];
      const body = this.filteredRecords.map(record => [record.date, record.project, record.unit, record.category, record.description, record.amount, record.deduct ? record.reserveAmount : '否', record.status]);
      const csv = [header, ...body].map(row => row.map(value => `"${String(value).replaceAll('"', '""')}"`).join(',')).join('\n');
      const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' });
      const link = document.createElement('a'); link.href = URL.createObjectURL(blob); link.download = '支出維修記錄.csv'; link.click(); URL.revokeObjectURL(link.href);
      this.showToast('支出維修記錄已匯出');
    },
    exportRecords() {
      const headers = ['日期', '房產', '單位', '類別', '項目說明', '金額', '預備金扣除', '處理狀態'];
      const rows = this.filteredRecords.map((record) => [record.date, record.project, record.unit, record.category, record.description, record.amount, record.deduct ? record.reserveAmount : '否', record.status]);
      downloadCsv('維修支出明細.csv', headers, rows);
    }
  }
};
</script>
