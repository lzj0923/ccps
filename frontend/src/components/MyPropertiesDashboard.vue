<template>
  <section class="my-properties-dashboard">
    <section class="owner-summary-grid lifecycle-summary">
      <article v-for="card in summaryCards" :key="card.label" class="owner-summary-card">
        <div class="summary-icon">{{ card.icon }}</div>
        <div class="summary-copy">
          <span>{{ $lt(card.label) }}</span>
          <strong>{{ card.value }}</strong>
          <small>{{ $lt(card.note) }}</small>
        </div>
        <span class="summary-mark">{{ card.mark }}</span>
      </article>
    </section>

    <section class="my-properties-layout">
      <div class="property-list-panel">
        <div class="property-toolbar">
          <label class="property-search">
            <span>⌕</span>
            <input v-model.trim="search" :placeholder="$t('legacy.t_6d40090d73d7')" />
          </label>
          <select v-model="project">
            <option value="全部建案">{{ $t('legacy.t_4502f4a3a4aa') }}</option>
            <option v-for="item in projectOptions" :key="item" :value="item">{{ item }}</option>
          </select>
          <select v-model="status">
            <option value="全部狀態">{{ $t('legacy.t_026ed0343be6') }}</option>
            <option value="未交房">{{ $t('legacy.t_8fbf8d463812') }}</option>
            <option value="出租中">{{ $t('legacy.t_2ba7bdb71038') }}</option>
            <option value="正常繳費中">{{ $t('legacy.t_7358d27ba2bb') }}</option>
            <option value="已繳清">{{ $t('legacy.t_d1fbe08d95e8') }}</option>
            <option value="即將到期">{{ $t('legacy.t_63079a4c84c3') }}</option>
            <option value="已逾期">{{ $t('legacy.t_bf6fc7bf226d') }}</option>
            <option value="待補繳款資料">{{ $t('legacy.t_af136eaa1dab') }}</option>
          </select>
          <button class="property-query" @click="appliedSearch = search">{{ $t('legacy.t_505ba2176546') }}</button>
          <button class="property-reset" @click="resetFilters">{{ $t('legacy.t_3d81345303ab') }}</button>
          <div class="property-view-toggle">
            <button :class="{ active: viewMode === 'list' }" @click="viewMode = 'list'">▦</button>
            <button :class="{ active: viewMode === 'compact' }" @click="viewMode = 'compact'">▤</button>
          </div>
        </div>

        <div class="property-list-head">
          <strong>{{ $t('legacy.t_0776391efeac') }}</strong>
          <span>{{ $t('legacy.t_3b6ef811b85a') }} {{ filteredProperties.length }} {{ $t('legacy.t_20c65f89b612') }}</span>
        </div>
        <div class="property-stage-tabs" :aria-label="$t('legacy.t_860cfcc6a38b')">
          <button :class="{ active: stageFilter === 'ALL' }" @click="stageFilter = 'ALL'">{{ $t('legacy.t_9d65de4d5c85') }} <b>{{ stageCounts.all }}</b></button>
          <button :class="{ active: stageFilter === 'PRE_HANDOVER' }" @click="stageFilter = 'PRE_HANDOVER'">{{ $t('legacy.t_8fbf8d463812') }} <b>{{ stageCounts.preHandover }}</b></button>
          <button :class="{ active: stageFilter === 'OPERATING' }" @click="stageFilter = 'OPERATING'">{{ $t('legacy.t_2ba7bdb71038') }} <b>{{ stageCounts.operating }}</b></button>
        </div>

        <div v-if="page.databaseLoading" class="property-data-state">{{ $t('legacy.t_56f3ac475242') }}</div>
        <div v-else-if="page.databaseError" class="property-data-state error" role="alert">
          <strong>{{ $t('legacy.t_36c4e79dae5c') }}</strong>
          <span>{{ $lt(page.databaseError) }}</span>
          <button @click="page.loadOwnerDashboard()">{{ $t('legacy.t_0c9157b5bfac') }}</button>
        </div>
        <div v-else class="property-list" :class="{ compact: viewMode === 'compact' }">
          <article v-for="(property, index) in pagedProperties" :key="property.key" class="property-row lifecycle-property-row" :class="{ 'operating-property': !property.isPreHandover }">
            <div class="property-photo" :class="`photo-${index % 3}`"><span>{{ property.initials }}</span></div>
            <div class="property-identity">
              <h3>{{ property.projectName }}</h3>
              <span class="property-location">⌖ {{ property.city || $t('legacy.t_4516716cc6bb') }}</span>
              <div class="property-identity-tags">
                <b :class="property.isPreHandover ? 'pre' : 'operating'">{{ property.lifecycle }}</b>
                <span>{{ property.unit }}</span>
              </div>
            </div>
            <div v-if="property.isPreHandover" class="property-metric-grid pre-handover-metrics">
              <div><span>{{ $t('legacy.t_bbc3a9494a03') }}</span><strong>{{ property.total }}</strong></div>
              <div><span>{{ $t('legacy.t_929fe9d67675') }}</span><strong class="green-value">{{ property.paid }}</strong></div>
              <div><span>{{ $t('legacy.t_1c1c1a7ddc8a') }}</span><strong class="gold-value">{{ property.remaining }}</strong></div>
              <div><span>{{ $t('legacy.t_41319c3504fe') }}</span><strong>{{ property.progress }}</strong></div>
              <div><span>{{ $t('legacy.t_328f69d15811') }}</span><strong>{{ property.nextDue }}</strong></div>
              <div><span>{{ $t('legacy.t_61b693084754') }}</span><strong>{{ property.expectedHandover }}</strong></div>
              <div class="metric-status"><span>{{ $t('legacy.t_607b3e1024c4') }}</span><b class="property-status" :class="property.statusClass"><i></i>{{ $lt(property.status) }}</b></div>
            </div>
            <div v-else class="property-operating-summary">
              <div class="operating-service-tags">
                <span v-for="service in property.serviceBadges" :key="service.code" :class="service.code.toLowerCase()">{{ $lt(service.label) }}</span>
                <span v-if="!property.serviceBadges.length" class="empty">{{ $t('legacy.t_92fd34a9ca85') }}</span>
              </div>
              <div class="property-metric-grid operating-metrics">
                <template v-if="property.hasRental">
                  <div><span>{{ $t('legacy.t_f73ddf8aaf4a') }}</span><strong>{{ property.tenantName }}</strong></div>
                  <div><span>{{ $t('legacy.t_35231ac50ed8') }}</span><strong>{{ property.monthlyRentText }}</strong></div>
                  <div><span>{{ tenantDepositLabel }}</span><strong>{{ property.tenantDepositText }}</strong></div>
                  <div><span>{{ $t('legacy.t_75a9b0272f59') }}</span><strong :class="{ 'gold-value': property.hasRentOutstanding }">{{ property.monthRentProgress }}</strong></div>
                  <div><span>{{ $t('legacy.t_4c2e681c838a') }}</span><strong>{{ property.leaseEndDateText }}</strong></div>
                </template>
                <template v-if="property.hasManagement">
                  <div><span>{{ $t('legacy.t_ba4eb373df77') }}</span><strong :class="property.reserveLow ? 'gold-value' : 'green-value'">{{ property.reserveBalanceText }}</strong></div>
                  <div><span>{{ $t('legacy.t_e9aff9439a39') }}</span><strong :class="property.monthlyNetValue < 0 ? 'negative-value' : 'green-value'">{{ property.monthlyNetText }}</strong></div>
                  <div><span>{{ $t('legacy.t_8707473743ab') }}</span><strong>{{ property.pendingMaintenanceCount }} {{ $t('legacy.t_9cc6467c8ca1') }}</strong></div>
                </template>
                <div v-if="property.hasResale"><span>{{ $t('legacy.t_b99cea7ca335') }}</span><strong class="green-value">{{ $t('legacy.t_0b71ced27be5') }}</strong></div>
                <div v-if="!property.serviceBadges.length"><span>{{ $t('legacy.t_5e6ad3f8b374') }}</span><strong>{{ property.actualHandover }}</strong></div>
              </div>
            </div>
            <div class="property-actions">
              <button @click="showProperty(property)">{{ $t('legacy.t_db4452b17eab') }}</button>
              <button v-if="property.isPreHandover" class="upload" @click="openPaymentProofs(property)">{{ $t('legacy.t_9bd53a59b648') }}</button>
              <button v-else class="upload" @click="openServiceStatusEditor(property)">{{ $t('legacy.t_549316ca7bb8') }}</button>
            </div>
          </article>
          <div v-if="!filteredProperties.length" class="property-empty">{{ $t('legacy.t_e6b7ac4bd2c8') }}</div>
        </div>

        <div v-if="!page.databaseLoading && !page.databaseError && filteredProperties.length" class="property-pagination">
          <span>{{ $t('legacy.t_15671929a694') }} {{ pageStart + 1 }}-{{ pageEnd }}{{ $t('legacy.t_e6d397553786') }} {{ filteredProperties.length }} {{ $t('legacy.t_20c65f89b612') }}</span>
          <div>
            <button :disabled="propertyPage === 1" @click="propertyPage--">‹</button>
            <button v-for="pageNumber in propertyTotalPages" :key="pageNumber" :class="{ active: propertyPage === pageNumber }" @click="propertyPage = pageNumber">{{ pageNumber }}</button>
            <button :disabled="propertyPage === propertyTotalPages" @click="propertyPage++">›</button>
          </div>
        </div>
      </div>

      <aside class="owner-side-column">
        <section class="owner-side-card">
          <div class="side-card-head"><h2>{{ $t('legacy.t_acb7cfdd2636') }}</h2><button @click="page.selectModule('ownerNotice')">{{ $t('legacy.t_ed2172fd7894') }}</button></div>
          <div class="notice-list-new">
            <article v-for="item in notices" :key="item.id || item.title">
              <span class="notice-symbol" :class="item.tone">{{ item.icon }}</span>
              <div><strong>{{ $lt(item.title) }}</strong><p>{{ $lt(item.detail) }}</p></div>
<time>{{ displayDate(item.date) }}</time><i></i>
            </article>
            <div v-if="!notices.length" class="side-card-empty">{{ $t('legacy.t_df56fa06bad2') }}</div>
          </div>
          <button class="side-card-more" @click="page.selectModule('ownerNotice')">{{ $t('legacy.t_35cf739e48a0') }}</button>
        </section>

        <section class="owner-side-card pending-card">
          <div class="side-card-head"><h2>{{ $t('legacy.t_6f277e1de0bd') }}</h2><button @click="page.selectModule('ownerDocuments')">{{ $t('legacy.t_ed2172fd7894') }}</button></div>
          <div class="pending-list">
            <article v-for="item in pendingItems" :key="item.type">
              <span class="pending-symbol">{{ item.icon }}</span>
              <div><strong>{{ $lt(item.title) }}</strong><p>{{ $lt(item.detail) }}</p></div>
              <b>{{ item.count }}</b>
            </article>
            <div v-if="!pendingItems.length" class="side-card-empty">{{ $t('legacy.t_ccafc8486914') }}</div>
          </div>
          <button class="side-card-more" @click="page.selectModule('ownerDocuments')">{{ $t('legacy.t_d1d259f10a29') }}</button>
        </section>
      </aside>
    </section>

    <div v-if="selectedProperty" class="property-detail-overlay" @pointerdown.self="selectedProperty = null">
      <section class="property-detail-dialog" role="dialog" aria-modal="true" :aria-label="$t('legacy.t_c2119703b8d0')">
        <header><div><span>{{ $t('legacy.t_c2119703b8d0') }}</span><h2>{{ selectedProperty.projectName }} · {{ selectedProperty.unit }}</h2></div><button @click="selectedProperty = null">×</button></header>
        <div class="property-detail-grid">
          <div><span>{{ $t('legacy.t_590157b8d4d7') }}</span><strong>{{ selectedProperty.city || '—' }}</strong></div>
          <div><span>{{ $t('legacy.t_ad9e95e81c53') }}</span><strong>{{ selectedProperty.unitType || '—' }}</strong></div>
          <div><span>{{ $t('legacy.t_e89998bd95c2') }}</span><strong>{{ selectedProperty.area }}</strong></div>
          <div><span>{{ $t('legacy.t_bbc3a9494a03') }}</span><strong>{{ selectedProperty.total }}</strong></div>
          <div><span>{{ $t('legacy.t_a1feb767c3c4') }}</span><strong>{{ selectedProperty.lifecycle }}</strong></div>
          <template v-if="selectedProperty.isPreHandover">
            <div><span>{{ $t('legacy.t_929fe9d67675') }}</span><strong>{{ selectedProperty.paid }}</strong></div>
            <div><span>{{ $t('legacy.t_1c1c1a7ddc8a') }}</span><strong>{{ selectedProperty.remaining }}</strong></div>
            <div><span>{{ $t('legacy.t_41319c3504fe') }}</span><strong>{{ selectedProperty.progress }}</strong></div>
            <div><span>{{ $t('legacy.t_b20d15a073af') }}</span><strong>{{ selectedProperty.nextDue }}</strong></div>
            <div><span>{{ $t('legacy.t_bc44c4426292') }}</span><strong>{{ selectedProperty.expectedHandover }}</strong></div>
          </template>
          <template v-else>
            <div><span>{{ $t('legacy.t_5e6ad3f8b374') }}</span><strong>{{ selectedProperty.actualHandover }}</strong></div>
            <div><span>{{ $t('legacy.t_2edc6eafc14e') }}</span><strong>{{ selectedProperty.servicesText }}</strong></div>
          </template>
        </div>
        <section v-if="!selectedProperty.isPreHandover" class="property-service-details">
          <article v-if="selectedProperty.hasRental" class="rental">
            <header><div><b>{{ $t('legacy.t_82b48101dd74') }}</b><span>{{ selectedProperty.rentStatus }}</span></div><button @click="openServiceModule('rentIncome')">{{ $t('legacy.t_38f9f8594490') }}</button></header>
            <dl>
              <div><dt>{{ $t('legacy.t_f73ddf8aaf4a') }}</dt><dd>{{ selectedProperty.tenantName }}</dd></div>
              <div><dt>{{ $t('legacy.t_35231ac50ed8') }}</dt><dd>{{ selectedProperty.monthlyRentText }}</dd></div>
              <div><dt>{{ tenantDepositLabel }}</dt><dd>{{ selectedProperty.tenantDepositText }}</dd></div>
              <div><dt>{{ $t('legacy.t_72bd77e1d37d') }}</dt><dd>{{ selectedProperty.currentMonthRentPaidText }}</dd></div>
              <div><dt>{{ $t('legacy.t_c08daf0f9b3a') }}</dt><dd>{{ selectedProperty.currentMonthRentOutstandingText }}</dd></div>
              <div><dt>{{ $t('legacy.t_4c2e681c838a') }}</dt><dd>{{ selectedProperty.leaseEndDateText }}</dd></div>
            </dl>
          </article>
          <article v-if="selectedProperty.hasManagement" class="management">
            <header><div><b>{{ $t('legacy.t_4eaef1731e38') }}</b><span>{{ selectedProperty.pendingMaintenanceCount ? $t('ui.maintenancePendingCount', { count: selectedProperty.pendingMaintenanceCount }) : $t('legacy.t_46491a1a1e7c') }}</span></div><button @click="openServiceModule('ownerReserve')">{{ $t('legacy.t_f087734e0cf8') }}</button></header>
            <dl>
              <div><dt>{{ $t('legacy.t_ba4eb373df77') }}</dt><dd>{{ selectedProperty.reserveBalanceText }}</dd></div>
              <div><dt>{{ $t('legacy.t_0b73c01aa04e') }}</dt><dd>{{ selectedProperty.reserveMinimumText }}</dd></div>
              <div><dt>{{ $t('legacy.t_0da153c696cb') }}</dt><dd>{{ selectedProperty.monthlyIncomeText }}</dd></div>
              <div><dt>{{ $t('legacy.t_1c58f3dcbf5f') }}</dt><dd>{{ selectedProperty.monthlyExpenseText }}</dd></div>
              <div><dt>{{ $t('legacy.t_e9aff9439a39') }}</dt><dd>{{ selectedProperty.monthlyNetText }}</dd></div>
            </dl>
            <button class="service-secondary-action" @click="openPropertyCashflow(selectedProperty)">{{ $t('legacy.t_3fe6a3311d65') }}</button>
          </article>
          <article v-if="selectedProperty.hasResale" class="resale">
            <header><div><b>{{ $t('legacy.t_cd5a943f140a') }}</b><span>{{ $t('legacy.t_bb1a3917e52b') }}</span></div></header>
            <p>{{ $t('legacy.t_7ab8ef6e51e0') }}</p>
          </article>
          <div v-if="!selectedProperty.serviceBadges.length" class="property-no-service">{{ $t('legacy.t_277c82fa25ae') }}</div>
        </section>
      </section>
    </div>

    <div v-if="serviceDialogProperty" class="property-detail-overlay" @pointerdown.self="closeServiceStatusEditor">
      <section class="property-detail-dialog service-status-dialog" role="dialog" aria-modal="true" :aria-label="$t('legacy.t_1511fb7564fa')">
        <header>
          <div><span>{{ $t('legacy.t_ae5c8302f547') }}</span><h2>{{ serviceDialogProperty.projectName }} · {{ serviceDialogProperty.unit }}</h2></div>
          <button :disabled="serviceSaving" @click="closeServiceStatusEditor">×</button>
        </header>
        <p class="service-status-hint">{{ $t('legacy.t_cdfc1f456acd') }}</p>
        <div class="service-status-options">
          <label v-for="option in serviceOptions" :key="option.code" :class="{ selected: serviceSelection.includes(option.code) }">
            <input v-model="serviceSelection" type="checkbox" :value="option.code" :disabled="serviceSaving" />
            <span>{{ option.icon }}</span>
            <div><strong>{{ $lt(option.label) }}</strong><small>{{ $lt(option.description) }}</small></div>
            <b>{{ serviceSelection.includes(option.code) ? $t('legacy.t_aeec0b67da9d') : $t('legacy.t_3fcc786925cc') }}</b>
          </label>
        </div>
        <p v-if="serviceError" class="service-status-error">{{ $lt(serviceError) }}</p>
        <footer class="service-status-actions">
          <button :disabled="serviceSaving" @click="closeServiceStatusEditor">{{ $t('legacy.t_4d0b4688c787') }}</button>
          <button class="save" :disabled="serviceSaving || !serviceSelection.length" @click="saveServiceStatus">
            {{ serviceSaving ? $t('legacy.t_6644f06197a4') : $t('legacy.t_2ae79050a2a6') }}
          </button>
        </footer>
      </section>
    </div>
  </section>
</template>

<script>
import { updateOwnerPropertyServices } from '../services/propertyApi';
import { navigate } from '../router';
import { formatDate } from '../utils/dateFormat';

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

const pendingCopyKeys = {
  payment_proof: 'ui.ownerPendingPaymentProof',
  rent_confirmation: 'ui.ownerPendingRentConfirmation',
  document_signature: 'ui.ownerPendingDocumentSignature'
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
    tenantDepositLabel() { return this.$i18n.locale === 'zh-TW' ? '租客押金' : this.$i18n.locale === 'en' ? 'Tenant deposit' : '租客押金'; },
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
          tenantDepositText: this.money(property.tenantDepositAmount),
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
        { icon: '♢', mark: '♢', label: this.tenantDepositLabel, value: this.money(summary.tenantDepositAmount), note: '有效租约押金合计' },
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
        ...this.pendingItemCopy(item),
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
    pendingItemCopy(item) {
      const count = Number(item.count || 0);
      return {
        title: pendingCopyKeys[item.type] ? this.$t(pendingCopyKeys[item.type]) : this.$lt(item.title),
        detail: count > 0 ? this.$t('ui.ownerPendingCount', { count }) : this.$t('ui.ownerPendingEmpty')
      };
    },
    displayDate(value) { return formatDate(value); },
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
      this.page.showToast(this.$ltf`請為 ${property.projectName} ${property.unit} 選擇並上傳繳費憑證`);
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
        this.page.showToast(this.$ltf`${unit} 的服務狀態已更新`);
      } catch (error) {
        this.serviceError = error.message || '服務狀態更新失敗';
      } finally {
        this.serviceSaving = false;
      }
    },
    openServiceModule(moduleId) {
      this.selectedProperty = null;
      this.page.selectModule(moduleId);
    },
    openPropertyCashflow(property) {
      const ownerUnitId = property?.ownerUnitId;
      this.selectedProperty = null;
      if (!ownerUnitId) {
        this.page.selectModule('ownerFinance');
        return;
      }
      navigate(`/owner/finance?ownerUnitId=${encodeURIComponent(ownerUnitId)}&tab=cashflow`);
    }
  }
};
</script>
