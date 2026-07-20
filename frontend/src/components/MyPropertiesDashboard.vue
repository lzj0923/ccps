<template>
  <section class="my-properties-dashboard">
    <section class="owner-summary-grid lifecycle-summary">
      <article v-for="card in summaryCards" :key="card.label" class="owner-summary-card">
        <div class="summary-icon">{{ card.icon }}</div>
        <div class="summary-copy">
          <span>{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
          <small>{{ card.note }}</small>
        </div>
        <span class="summary-mark">{{ card.mark }}</span>
      </article>
    </section>

    <section class="my-properties-layout">
      <div class="property-list-panel">
        <div class="property-toolbar">
          <label class="property-search">
            <span>⌕</span>
            <input v-model.trim="search" placeholder="搜索建案名稱 / 單位編號" />
          </label>
          <select v-model="project">
            <option value="全部建案">全部建案</option>
            <option v-for="item in projectOptions" :key="item" :value="item">{{ item }}</option>
          </select>
          <select v-model="status">
            <option value="全部狀態">全部狀態</option>
            <option value="未交房">未交房</option>
            <option value="出租中">出租中</option>
            <option value="正常繳費中">正常繳費中</option>
            <option value="已繳清">已繳清</option>
            <option value="即將到期">即將到期</option>
            <option value="已逾期">已逾期</option>
            <option value="待補繳款資料">待補繳款資料</option>
          </select>
          <button class="property-query" @click="appliedSearch = search">查詢</button>
          <button class="property-reset" @click="resetFilters">重置</button>
          <div class="property-view-toggle">
            <button :class="{ active: viewMode === 'list' }" @click="viewMode = 'list'">▦</button>
            <button :class="{ active: viewMode === 'compact' }" @click="viewMode = 'compact'">▤</button>
          </div>
        </div>

        <div class="property-list-head">
          <strong>我的房產</strong>
          <span>共 {{ filteredProperties.length }} 項房產</span>
        </div>
        <div class="property-stage-tabs" aria-label="房產類型篩選">
          <button :class="{ active: stageFilter === 'ALL' }" @click="stageFilter = 'ALL'">全部房產 <b>{{ stageCounts.all }}</b></button>
          <button :class="{ active: stageFilter === 'PRE_HANDOVER' }" @click="stageFilter = 'PRE_HANDOVER'">未交房 <b>{{ stageCounts.preHandover }}</b></button>
          <button :class="{ active: stageFilter === 'OPERATING' }" @click="stageFilter = 'OPERATING'">出租中 <b>{{ stageCounts.operating }}</b></button>
        </div>

        <div v-if="page.databaseLoading" class="property-data-state">正在從資料庫載入房產資料…</div>
        <div v-else-if="page.databaseError" class="property-data-state error" role="alert">
          <strong>房產資料載入失敗</strong>
          <span>{{ page.databaseError }}</span>
          <button @click="page.loadOwnerDashboard()">重新載入</button>
        </div>
        <div v-else class="property-list" :class="{ compact: viewMode === 'compact' }">
          <article v-for="(property, index) in pagedProperties" :key="property.key" class="property-row lifecycle-property-row" :class="{ 'operating-property': !property.isPreHandover }">
            <div class="property-photo" :class="`photo-${index % 3}`"><span>{{ property.initials }}</span></div>
            <div class="property-identity">
              <h3>{{ property.projectName }}</h3>
              <span class="property-location">⌖ {{ property.city || '城市未設定' }}</span>
              <div class="property-identity-tags">
                <b :class="property.isPreHandover ? 'pre' : 'operating'">{{ property.lifecycle }}</b>
                <span>{{ property.unit }}</span>
              </div>
            </div>
            <div v-if="property.isPreHandover" class="property-metric-grid pre-handover-metrics">
              <div><span>房產總價</span><strong>{{ property.total }}</strong></div>
              <div><span>已繳金額</span><strong class="green-value">{{ property.paid }}</strong></div>
              <div><span>剩餘金額</span><strong class="gold-value">{{ property.remaining }}</strong></div>
              <div><span>繳費進度</span><strong>{{ property.progress }}</strong></div>
              <div><span>下一期繳費</span><strong>{{ property.nextDue }}</strong></div>
              <div><span>預計交房</span><strong>{{ property.expectedHandover }}</strong></div>
              <div class="metric-status"><span>付款狀態</span><b class="property-status" :class="property.statusClass"><i></i>{{ property.status }}</b></div>
            </div>
            <div v-else class="property-operating-summary">
              <div class="operating-service-tags">
                <span v-for="service in property.serviceBadges" :key="service.code" :class="service.code.toLowerCase()">{{ service.label }}</span>
                <span v-if="!property.serviceBadges.length" class="empty">尚未啟用營運服務</span>
              </div>
              <div class="property-metric-grid operating-metrics">
                <template v-if="property.hasRental">
                  <div><span>目前租客</span><strong>{{ property.tenantName }}</strong></div>
                  <div><span>每月租金</span><strong>{{ property.monthlyRentText }}</strong></div>
                  <div><span>本月已收／應收</span><strong :class="{ 'gold-value': property.hasRentOutstanding }">{{ property.monthRentProgress }}</strong></div>
                  <div><span>租約到期</span><strong>{{ property.leaseEndDateText }}</strong></div>
                </template>
                <template v-if="property.hasManagement">
                  <div><span>預備金餘額</span><strong :class="property.reserveLow ? 'gold-value' : 'green-value'">{{ property.reserveBalanceText }}</strong></div>
                  <div><span>本月淨收支</span><strong :class="property.monthlyNetValue < 0 ? 'negative-value' : 'green-value'">{{ property.monthlyNetText }}</strong></div>
                  <div><span>待處理維修</span><strong>{{ property.pendingMaintenanceCount }} 項</strong></div>
                </template>
                <div v-if="property.hasResale"><span>代售狀態</span><strong class="green-value">服務已啟用</strong></div>
                <div v-if="!property.serviceBadges.length"><span>實際交房日期</span><strong>{{ property.actualHandover }}</strong></div>
              </div>
            </div>
            <div class="property-actions">
              <button @click="showProperty(property)">▣ &nbsp;查看詳情</button>
              <button v-if="property.isPreHandover" class="upload" @click="openPaymentProofs(property)">⇧ &nbsp;上傳憑證</button>
              <button v-else class="upload" @click="openServiceStatusEditor(property)">⌁ &nbsp;改變狀態</button>
            </div>
          </article>
          <div v-if="!filteredProperties.length" class="property-empty">目前沒有符合條件的房產資料</div>
        </div>

        <div v-if="!page.databaseLoading && !page.databaseError && filteredProperties.length" class="property-pagination">
          <span>顯示 {{ pageStart + 1 }}-{{ pageEnd }}，共 {{ filteredProperties.length }} 項房產</span>
          <div>
            <button :disabled="propertyPage === 1" @click="propertyPage--">‹</button>
            <button v-for="pageNumber in propertyTotalPages" :key="pageNumber" :class="{ active: propertyPage === pageNumber }" @click="propertyPage = pageNumber">{{ pageNumber }}</button>
            <button :disabled="propertyPage === propertyTotalPages" @click="propertyPage++">›</button>
          </div>
        </div>
      </div>

      <aside class="owner-side-column">
        <section class="owner-side-card">
          <div class="side-card-head"><h2>最新通知</h2><button @click="page.selectModule('ownerNotice')">查看全部</button></div>
          <div class="notice-list-new">
            <article v-for="item in notices" :key="item.id || item.title">
              <span class="notice-symbol" :class="item.tone">{{ item.icon }}</span>
              <div><strong>{{ item.title }}</strong><p>{{ item.detail }}</p></div>
              <time>{{ item.date }}</time><i></i>
            </article>
            <div v-if="!notices.length" class="side-card-empty">目前沒有通知</div>
          </div>
          <button class="side-card-more" @click="page.selectModule('ownerNotice')">查看全部通知&nbsp; →</button>
        </section>

        <section class="owner-side-card pending-card">
          <div class="side-card-head"><h2>待處理事項</h2><button @click="page.selectModule('ownerDocuments')">查看全部</button></div>
          <div class="pending-list">
            <article v-for="item in pendingItems" :key="item.type">
              <span class="pending-symbol">{{ item.icon }}</span>
              <div><strong>{{ item.title }}</strong><p>{{ item.detail }}</p></div>
              <b>{{ item.count }}</b>
            </article>
            <div v-if="!pendingItems.length" class="side-card-empty">目前沒有待處理事項</div>
          </div>
          <button class="side-card-more" @click="page.selectModule('ownerDocuments')">查看全部事項&nbsp; →</button>
        </section>
      </aside>
    </section>

    <div v-if="selectedProperty" class="property-detail-overlay" @click.self="selectedProperty = null">
      <section class="property-detail-dialog" role="dialog" aria-modal="true" aria-label="房產詳情">
        <header><div><span>房產詳情</span><h2>{{ selectedProperty.projectName }} · {{ selectedProperty.unit }}</h2></div><button @click="selectedProperty = null">×</button></header>
        <div class="property-detail-grid">
          <div><span>城市</span><strong>{{ selectedProperty.city || '—' }}</strong></div>
          <div><span>房型</span><strong>{{ selectedProperty.unitType || '—' }}</strong></div>
          <div><span>面積</span><strong>{{ selectedProperty.area }}</strong></div>
          <div><span>房產總價</span><strong>{{ selectedProperty.total }}</strong></div>
          <div><span>房產階段</span><strong>{{ selectedProperty.lifecycle }}</strong></div>
          <template v-if="selectedProperty.isPreHandover">
            <div><span>已繳金額</span><strong>{{ selectedProperty.paid }}</strong></div>
            <div><span>剩餘金額</span><strong>{{ selectedProperty.remaining }}</strong></div>
            <div><span>繳費進度</span><strong>{{ selectedProperty.progress }}</strong></div>
            <div><span>下一期日期</span><strong>{{ selectedProperty.nextDue }}</strong></div>
            <div><span>預計交房日期</span><strong>{{ selectedProperty.expectedHandover }}</strong></div>
          </template>
          <template v-else>
            <div><span>實際交房日期</span><strong>{{ selectedProperty.actualHandover }}</strong></div>
            <div><span>營運服務</span><strong>{{ selectedProperty.servicesText }}</strong></div>
          </template>
        </div>
        <section v-if="!selectedProperty.isPreHandover" class="property-service-details">
          <article v-if="selectedProperty.hasRental" class="rental">
            <header><div><b>出租</b><span>{{ selectedProperty.rentStatus }}</span></div><button @click="openServiceModule('rentIncome')">查看租金 →</button></header>
            <dl>
              <div><dt>目前租客</dt><dd>{{ selectedProperty.tenantName }}</dd></div>
              <div><dt>每月租金</dt><dd>{{ selectedProperty.monthlyRentText }}</dd></div>
              <div><dt>本月已收</dt><dd>{{ selectedProperty.currentMonthRentPaidText }}</dd></div>
              <div><dt>本月未收</dt><dd>{{ selectedProperty.currentMonthRentOutstandingText }}</dd></div>
              <div><dt>租約到期</dt><dd>{{ selectedProperty.leaseEndDateText }}</dd></div>
            </dl>
          </article>
          <article v-if="selectedProperty.hasManagement" class="management">
            <header><div><b>代管</b><span>{{ selectedProperty.pendingMaintenanceCount ? `${selectedProperty.pendingMaintenanceCount} 項維修待處理` : '目前運作正常' }}</span></div><button @click="openServiceModule('ownerReserve')">查看預備金 →</button></header>
            <dl>
              <div><dt>預備金餘額</dt><dd>{{ selectedProperty.reserveBalanceText }}</dd></div>
              <div><dt>最低預備金</dt><dd>{{ selectedProperty.reserveMinimumText }}</dd></div>
              <div><dt>本月收入</dt><dd>{{ selectedProperty.monthlyIncomeText }}</dd></div>
              <div><dt>本月支出</dt><dd>{{ selectedProperty.monthlyExpenseText }}</dd></div>
              <div><dt>本月淨收支</dt><dd>{{ selectedProperty.monthlyNetText }}</dd></div>
            </dl>
            <button class="service-secondary-action" @click="openServiceModule('ownerExpenses')">查看收支與維修</button>
          </article>
          <article v-if="selectedProperty.hasResale" class="resale">
            <header><div><b>代售</b><span>代售服務已啟用</span></div></header>
            <p>目前資料庫尚未建立售價、刊登平台與帶看紀錄；這裡只展示真實的服務啟用狀態。</p>
          </article>
          <div v-if="!selectedProperty.serviceBadges.length" class="property-no-service">這套房產尚未啟用出租、代售或代管服務。</div>
        </section>
      </section>
    </div>

    <div v-if="serviceDialogProperty" class="property-detail-overlay" @click.self="closeServiceStatusEditor">
      <section class="property-detail-dialog service-status-dialog" role="dialog" aria-modal="true" aria-label="變更房產服務狀態">
        <header>
          <div><span>變更服務狀態</span><h2>{{ serviceDialogProperty.projectName }} · {{ serviceDialogProperty.unit }}</h2></div>
          <button :disabled="serviceSaving" @click="closeServiceStatusEditor">×</button>
        </header>
        <p class="service-status-hint">可同時選擇多項服務，保存後立即套用到這套房產。</p>
        <div class="service-status-options">
          <label v-for="option in serviceOptions" :key="option.code" :class="{ selected: serviceSelection.includes(option.code) }">
            <input v-model="serviceSelection" type="checkbox" :value="option.code" :disabled="serviceSaving" />
            <span>{{ option.icon }}</span>
            <div><strong>{{ option.label }}</strong><small>{{ option.description }}</small></div>
            <b>{{ serviceSelection.includes(option.code) ? '已選擇' : '未選擇' }}</b>
          </label>
        </div>
        <p v-if="serviceError" class="service-status-error">{{ serviceError }}</p>
        <footer class="service-status-actions">
          <button :disabled="serviceSaving" @click="closeServiceStatusEditor">取消</button>
          <button class="save" :disabled="serviceSaving || !serviceSelection.length" @click="saveServiceStatus">
            {{ serviceSaving ? '保存中…' : '保存狀態' }}
          </button>
        </footer>
      </section>
    </div>
  </section>
</template>

<script>
import { updateOwnerPropertyServices } from '../services/propertyApi';

const paymentStates = {
  paying: { label: '正常繳費中', css: 'green' },
  paid: { label: '已繳清', css: 'paid' },
  due_soon: { label: '即將到期', css: 'blue' },
  overdue: { label: '已逾期', css: 'overdue' },
  not_configured: { label: '待補繳款資料', css: 'pending-data' },
  not_applicable: { label: '出租中', css: 'paid' }
};

const serviceNames = { RENTAL: '代租', RESALE: '代售', MANAGEMENT: '代管' };
const serviceOptions = [
  { code: 'RENTAL', label: '代租', icon: '⌂', description: '委託招租、租約及租金管理' },
  { code: 'RESALE', label: '代售', icon: '◇', description: '委託刊登、帶看及出售跟進' },
  { code: 'MANAGEMENT', label: '代管', icon: '⚒', description: '預備金、收支及維修管理' }
];

const pendingIcons = {
  payment_proof: '⇧',
  rent_confirmation: '✉',
  document_signature: '▣'
};

export default {
  inject: ['page'],
  data() {
    return {
      search: '',
      appliedSearch: '',
      project: '全部建案',
      status: '全部狀態',
      viewMode: 'list',
      propertyPage: 1,
      propertyPageSize: 3,
      stageFilter: 'ALL',
      selectedProperty: null,
      serviceDialogProperty: null,
      serviceSelection: [],
      serviceSaving: false,
      serviceError: ''
    };
  },
  computed: {
    sourceProperties() { return this.page.ownerDashboard?.properties || []; },
    serviceOptions() { return serviceOptions; },
    properties() {
      return this.sourceProperties.map((property) => {
        const state = paymentStates[property.paymentStatus] || paymentStates.paying;
        const isPreHandover = property.assetStage === 'PRE_HANDOVER';
        const paidInstallments = Number(property.paidInstallmentCount || 0);
        const totalInstallments = Number(property.totalInstallmentCount || 0);
        const services = property.services || [];
        const currentMonthRentDue = Number(property.currentMonthRentDue || 0);
        const currentMonthRentPaid = Number(property.currentMonthRentPaid || 0);
        const reserveBalance = Number(property.reserveBalance || 0);
        const reserveMinimum = Number(property.reserveMinimumBalance || 0);
        const monthlyIncome = Number(property.monthlyIncome || 0);
        const monthlyExpense = Number(property.monthlyExpense || 0);
        const monthlyNetValue = monthlyIncome - monthlyExpense;
        return {
          ...property,
          key: property.ownerUnitId || `${property.projectName}-${property.unitNo}`,
          projectName: property.projectName || '未設定建案',
          unit: property.unitNo || '—',
          city: property.city || '',
          unitType: property.unitType || '',
          area: property.areaSqm == null ? '—' : `${property.areaSqm} m²`,
          isPreHandover,
          lifecycle: isPreHandover ? '未交房' : property.assetStage === 'DISPOSED' ? '已處置' : '出租中',
          expectedHandover: property.expectedHandoverDate || '—',
          actualHandover: property.actualHandoverDate || '—',
          servicesText: services.map(service => serviceNames[service] || service).join('、') || '尚未啟用',
          serviceBadges: services.map(service => ({ code: service, label: serviceNames[service] || service })),
          hasRental: services.includes('RENTAL'),
          hasResale: services.includes('RESALE'),
          hasManagement: services.includes('MANAGEMENT'),
          tenantName: property.tenantName || '尚未出租',
          monthlyRentText: this.money(property.monthlyRent),
          currentMonthRentPaidText: this.money(currentMonthRentPaid),
          currentMonthRentOutstandingText: this.money(property.currentMonthRentOutstanding),
          monthRentProgress: `${this.money(currentMonthRentPaid)} / ${this.money(currentMonthRentDue)}`,
          hasRentOutstanding: Number(property.currentMonthRentOutstanding || 0) > 0,
          leaseEndDateText: property.leaseEndDate || '—',
          rentStatus: !property.tenantName ? '待出租' : Number(property.currentMonthRentOutstanding || 0) > 0 ? '本月租金未收齊' : currentMonthRentDue > 0 ? '本月租金已收' : '租約進行中',
          reserveBalanceText: this.money(reserveBalance),
          reserveMinimumText: this.money(reserveMinimum),
          reserveLow: reserveBalance < reserveMinimum,
          monthlyIncomeText: this.money(monthlyIncome),
          monthlyExpenseText: this.money(monthlyExpense),
          monthlyNetValue,
          monthlyNetText: this.signedMoney(monthlyNetValue),
          pendingMaintenanceCount: Number(property.pendingMaintenanceCount || 0),
          total: this.money(property.purchasePrice),
          paid: this.money(property.paidAmount),
          remaining: this.money(property.remainingAmount),
          progress: `${paidInstallments} / ${totalInstallments} 期`,
          nextDue: property.nextDueDate || '—',
          status: isPreHandover ? state.label : property.assetStage === 'DISPOSED' ? '已處置' : '出租中',
          statusClass: isPreHandover ? state.css : property.assetStage === 'DISPOSED' ? 'pending-data' : 'paid',
          initials: String(property.projectName || 'CC').slice(0, 2).toUpperCase()
        };
      });
    },
    projectOptions() { return [...new Set(this.properties.map(item => item.projectName))]; },
    stageCounts() {
      return {
        all: this.properties.length,
        preHandover: this.properties.filter(item => item.isPreHandover).length,
        operating: this.properties.filter(item => !item.isPreHandover && item.assetStage !== 'DISPOSED').length
      };
    },
    filteredProperties() {
      const keyword = this.appliedSearch.toLowerCase();
      return this.properties.filter(item =>
        (!keyword || `${item.projectName} ${item.unit}`.toLowerCase().includes(keyword))
        && (this.project === '全部建案' || item.projectName === this.project)
        && (this.stageFilter === 'ALL' || item.assetStage === this.stageFilter)
        && (this.status === '全部狀態' || item.status === this.status || item.lifecycle === this.status));
    },
    propertyTotalPages() { return Math.max(1, Math.ceil(this.filteredProperties.length / this.propertyPageSize)); },
    pagedProperties() {
      const start = (this.propertyPage - 1) * this.propertyPageSize;
      return this.filteredProperties.slice(start, start + this.propertyPageSize);
    },
    pageStart() { return (this.propertyPage - 1) * this.propertyPageSize; },
    pageEnd() { return Math.min(this.propertyPage * this.propertyPageSize, this.filteredProperties.length); },
    summaryCards() {
      const summary = this.page.ownerDashboard?.summary || {};
      return [
        { icon: '▥', mark: '♧', label: '名下房產數量', value: `${summary.propertyCount || 0}`, note: `${this.stageCounts.preHandover} 未交房 · ${this.stageCounts.operating} 出租中` },
        { icon: '▤', mark: '▱', label: '未交房待繳', value: this.money(summary.unpaidPropertyAmount), note: '僅計未交房房產' },
        { icon: '◉', mark: '⌁', label: '出租中本月租金', value: this.money(summary.monthlyRentIncome), note: '已付款並確認' },
        { icon: '♢', mark: '♢', label: '出租中預備金', value: this.money(summary.reserveBalance), note: '可用餘額' },
        { icon: '⚒', mark: '!', label: '待處理維修', value: `${summary.pendingMaintenanceCount || 0}`, note: '出租中房產' }
      ];
    },
    notices() {
      return (this.page.ownerDashboard?.notifications || []).map(item => ({
        id: item.id,
        icon: item.priority === 'high' || item.priority === 'urgent' ? '!' : '▣',
        tone: item.priority === 'high' || item.priority === 'urgent' ? 'purple' : 'gold',
        title: item.title,
        detail: item.body,
        date: item.createdDate || ''
      }));
    },
    pendingItems() {
      return (this.page.ownerDashboard?.pendingItems || []).map(item => ({
        ...item,
        icon: pendingIcons[item.type] || '•'
      }));
    }
  },
  watch: {
    search() { this.propertyPage = 1; },
    project() { this.propertyPage = 1; },
    status() { this.propertyPage = 1; },
    stageFilter() { this.propertyPage = 1; },
    filteredProperties() {
      if (this.propertyPage > this.propertyTotalPages) this.propertyPage = this.propertyTotalPages;
    }
  },
  methods: {
    money(value) {
      return `RM ${Number(value || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
    },
    signedMoney(value) {
      const amount = Number(value || 0);
      return `${amount < 0 ? '-' : ''}${this.money(Math.abs(amount))}`;
    },
    resetFilters() {
      this.search = '';
      this.appliedSearch = '';
      this.project = '全部建案';
      this.status = '全部狀態';
      this.stageFilter = 'ALL';
      this.propertyPage = 1;
    },
    showProperty(property) { this.selectedProperty = property; },
    openPaymentProofs(property) {
      this.page.selectModule('ownerDocuments');
      this.page.showToast(`請為 ${property.projectName} ${property.unit} 選擇並上傳繳費憑證`);
    },
    openServiceStatusEditor(property) {
      this.serviceDialogProperty = property;
      this.serviceSelection = serviceOptions
        .map(option => option.code)
        .filter(service => property.services.includes(service));
      this.serviceError = '';
    },
    closeServiceStatusEditor() {
      if (this.serviceSaving) return;
      this.serviceDialogProperty = null;
      this.serviceSelection = [];
      this.serviceError = '';
    },
    async saveServiceStatus() {
      if (!this.serviceDialogProperty || !this.serviceSelection.length || this.serviceSaving) return;
      this.serviceSaving = true;
      this.serviceError = '';
      try {
        await updateOwnerPropertyServices(this.serviceDialogProperty.ownerUnitId, this.serviceSelection);
        const unit = this.serviceDialogProperty.unit;
        this.serviceDialogProperty = null;
        this.serviceSelection = [];
        await this.page.loadOwnerDashboard();
        this.page.showToast(`${unit} 的服務狀態已更新`);
      } catch (error) {
        this.serviceError = error.message || '服務狀態更新失敗';
      } finally {
        this.serviceSaving = false;
      }
    },
    openServiceModule(moduleId) {
      this.selectedProperty = null;
      this.page.selectModule(moduleId);
    }
  }
};
</script>
