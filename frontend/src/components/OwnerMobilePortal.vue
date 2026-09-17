<template>
  <section class="owner-mobile-portal" :data-view="currentId">
    <header class="owner-mobile-titlebar">
      <div>
        <span>{{ $t('legacy.t_6100e56dd4b7') }} {{ countryLabel }}</span>
        <h1>{{ viewTitle }}</h1>
        <p>{{ viewHint }}</p>
      </div>
      <div class="owner-mobile-profile" :aria-label="ownerName">
        <strong>{{ ownerInitial }}</strong>
        <small>{{ currency }}</small>
      </div>
    </header>

    <section v-if="currentId === 'ownerProjects'" class="owner-mobile-surface owner-projects-empty">
      <Building2 :size="28" aria-hidden="true" />
      <span>{{ $t('legacy.t_87ec085a2d8d') }}</span>
      <h2>{{ $t('legacy.t_9e43e092921a') }}</h2>
      <p>{{ $t('legacy.t_0310c020f422') }}</p>
      <button type="button" @click="selectModule('ownerNotice')">{{ $t('legacy.t_2e71bc3716c4') }}</button>
    </section>

    <template v-else-if="currentId === 'myProperties'">
      <section class="owner-mobile-portfolio owner-mobile-surface">
        <div class="owner-mobile-ring" :style="assetRingStyle" role="img" :aria-label="`${$t('legacy.t_2e81e7e6f359')} ${currency} ${money(totalAssetValue)}`">
          <div><small>{{ portfolioMonth }}</small><strong>{{ currency }}</strong><b>{{ compactMoney(totalAssetValue) }}</b></div>
        </div>
        <div class="owner-mobile-portfolio-copy">
          <span>{{ $t('legacy.t_2e81e7e6f359') }}</span>
          <h2>{{ currency }} {{ money(totalAssetValue) }}</h2>
          <p>{{ ownerName }} · {{ countryLabel }} {{ $t('legacy.t_0e9fac850d11') }} {{ properties.length }} {{ $t('legacy.t_ba764234ee64') }}</p>
          <div class="owner-mobile-legend">
            <span><i class="operating"></i>{{ $t('legacy.t_80315a92e528') }} {{ assetCounts.operating }}</span>
            <span><i class="pre"></i>{{ $t('legacy.t_76e5d6e50d9e') }} {{ assetCounts.preHandover }}</span>
          </div>
        </div>
      </section>

      <section class="owner-mobile-section-head">
        <div><span>{{ $t('legacy.t_b54ae62e75ca') }}</span><h2>{{ $t('legacy.t_efc348384e21') }}</h2></div>
        <small>{{ properties.length }} {{ $t('legacy.t_032231d845f8') }}</small>
      </section>

      <div v-if="page.databaseLoading" class="owner-mobile-state"><LoaderCircle class="spinning" :size="22" aria-hidden="true" /><span>{{ $t('legacy.t_7921b0176c93') }}</span></div>
      <div v-else-if="page.databaseError" class="owner-mobile-state error"><AlertCircle :size="22" aria-hidden="true" /><strong>{{ $t('legacy.t_4a6503aaa9fc') }}</strong><span>{{ $lt(page.databaseError) }}</span><button type="button" @click="page.loadOwnerDashboard()">{{ $t('legacy.t_5982c44c18df') }}</button></div>
      <div v-else-if="!properties.length" class="owner-mobile-state"><Building2 :size="24" aria-hidden="true" /><strong>{{ $t('legacy.t_d9810ccbb682') }}</strong><span>{{ $t('legacy.t_f683eed468ac') }}</span></div>
      <div v-else class="owner-mobile-property-list">
        <article v-for="property in properties" :key="property.ownerUnitId" class="owner-mobile-property-card">
          <header>
            <div class="owner-mobile-property-mark">{{ initials(property.projectName) }}</div>
            <div><span>{{ property.city || countryLabel }}</span><h3>{{ property.projectName || $t('legacy.t_efc349204510') }}</h3><p>{{ property.unitNo || '—' }}<template v-if="property.areaSqm"> · {{ property.areaSqm }} {{ $t('legacy.t_6f6f0f6a0fb3') }}</template></p></div>
            <b :class="property.assetStage === 'PRE_HANDOVER' ? 'pre' : 'operating'">{{ assetStage(property.assetStage) }}</b>
          </header>
          <dl class="owner-mobile-money-grid">
            <div><dt>{{ $t('legacy.t_90aab4d3f61e') }}</dt><dd>{{ propertyCurrency(property) }} {{ money(property.purchasePrice) }}</dd></div>
            <div><dt>{{ $t('legacy.t_ee023ed023f9') }}</dt><dd>{{ formatMonth(property.purchaseDate) }}</dd></div>
            <div><dt>{{ $t('legacy.t_b0a5b93d3722') }}</dt><dd>{{ propertyCurrency(property) }} {{ money(property.paidAmount) }}</dd><small>{{ paymentPercent(property) }}%</small></div>
            <div><dt>{{ $t('legacy.t_96b4a1fd550b') }}</dt><dd>{{ propertyCurrency(property) }} {{ money(property.remainingAmount) }}</dd><small>{{ 100 - paymentPercent(property) }}%</small></div>
          </dl>
          <div class="owner-mobile-progress" :aria-label="`${$t('legacy.t_390e249deb38')} ${paymentPercent(property)}%`"><i :style="{ width: `${paymentPercent(property)}%` }"></i></div>
          <div v-if="expandedAsset === property.ownerUnitId" class="owner-mobile-inline-details">
            <dl>
              <div><dt>{{ $t('legacy.t_aa78dfd39fde') }}</dt><dd>{{ assetStage(property.assetStage) }}</dd></div>
              <div><dt>{{ $t('legacy.t_034f523e2a94') }}</dt><dd>{{ property.paidInstallmentCount || 0 }} / {{ property.totalInstallmentCount || 0 }} {{ $t('legacy.t_fc73601f2012') }}</dd></div>
              <div><dt>{{ $t('legacy.t_5d57312f51ad') }}</dt><dd>{{ formatDate(property.nextDueDate) }}</dd></div>
              <div><dt>{{ $t('legacy.t_0cf68ec966d9') }}</dt><dd>{{ servicesText(property.services) }}</dd></div>
            </dl>
            <div class="owner-mobile-detail-actions">
              <button type="button" @click="selectModule('ownerPayment')">{{ $t('legacy.t_d913a4bb7136') }}</button>
              <button type="button" @click="selectModule('ownerDocuments')">{{ $t('legacy.t_b317595eee44') }}</button>
            </div>
          </div>
          <button class="owner-mobile-row-action" type="button" :aria-expanded="expandedAsset === property.ownerUnitId" @click="toggleAsset(property.ownerUnitId)">
            {{ expandedAsset === property.ownerUnitId ? $t('legacy.t_b792d8a6b3d5') : $t('legacy.t_f00a24ea7d55') }}<ChevronDown :class="{ rotated: expandedAsset === property.ownerUnitId }" :size="18" aria-hidden="true" />
          </button>
        </article>
      </div>
    </template>

    <template v-else-if="currentId === 'ownerRentalHub'">
      <section class="owner-mobile-portfolio owner-mobile-surface rental">
        <div class="owner-mobile-ring rent" :style="rentRingStyle" role="img" :aria-label="`${$t('legacy.t_3dae5acc41fa')} ${currency} ${money(monthlyRentIncome)}`">
          <div><small>{{ portfolioMonth }}</small><strong>{{ $t('legacy.t_3dae5acc41fa') }}</strong><b>{{ compactMoney(monthlyRentIncome) }}</b></div>
        </div>
        <div class="owner-mobile-portfolio-copy">
          <span>{{ $t('legacy.t_83ea55a69162') }}</span>
          <h2>{{ currency }} {{ money(monthlyRentIncome) }}</h2>
          <p>{{ rentalProperties.length }} {{ $t('legacy.t_542b555e23b5') }} {{ currency }} {{ compactMoney(reserveBalance) }}</p>
          <button type="button" @click="selectModule('rentIncome')">{{ $t('legacy.t_b45b83edc57c') }}<ChevronRight :size="16" aria-hidden="true" /></button>
        </div>
      </section>

      <nav class="owner-mobile-chip-nav" :aria-label="$t('legacy.t_2e98f28508c3')">
        <button type="button" @click="selectModule('rentIncome')"><WalletCards :size="17" />{{ $t('legacy.t_5f3d34a1947f') }}</button>
        <button type="button" @click="selectModule('ownerExpenses')"><ChartNoAxesColumnIncreasing :size="17" />{{ $t('legacy.t_1024a9d92a44') }}</button>
        <button type="button" @click="selectModule('ownerReserve')"><ShieldCheck :size="17" />{{ $t('legacy.t_94d47e6a57f2') }}</button>
        <button type="button" @click="selectModule('ownerDocuments')"><Files :size="17" />{{ $t('legacy.t_59404d408887') }}</button>
      </nav>

      <section class="owner-mobile-section-head"><div><span>{{ $t('legacy.t_97d426e9abcc') }}</span><h2>{{ $t('legacy.t_bff5821ed4b8') }}</h2></div><small>{{ rentalProperties.length }} {{ $t('legacy.t_032231d845f8') }}</small></section>
      <div v-if="!rentalProperties.length" class="owner-mobile-state"><KeyRound :size="24" /><strong>{{ $t('legacy.t_2ad26d8d696a') }}</strong><span>{{ $t('legacy.t_8e82c648fb41') }}</span></div>
      <div v-else class="owner-mobile-property-list">
        <article v-for="property in rentalProperties" :key="property.ownerUnitId" class="owner-mobile-property-card rental-card">
          <header><div class="owner-mobile-property-mark"><KeyRound :size="20" /></div><div><span>{{ property.projectName }}</span><h3>{{ property.unitNo || '—' }}</h3><p>{{ property.areaSqm ? `${property.areaSqm} m²` : property.city || '—' }}</p></div><b class="operating">{{ $t('legacy.t_2ba7bdb71038') }}</b></header>
          <dl class="owner-mobile-rent-grid">
            <div><dt>{{ $t('legacy.t_101a8e3682b8') }}</dt><dd>{{ propertyCurrency(property) }} {{ money(property.monthlyRent) }}</dd></div>
            <div><dt>{{ $t('legacy.t_819c8bd23823') }}</dt><dd>{{ propertyCurrency(property) }} {{ money(property.tenantDepositAmount) }}</dd></div>
            <div><dt>{{ $t('legacy.t_870c0ab88bd9') }}</dt><dd>{{ propertyCurrency(property) }} {{ money(property.reserveBalance) }}</dd></div>
            <div><dt>{{ $t('legacy.t_98d28c24fa30') }}</dt><dd>{{ propertyCurrency(property) }} {{ money(unitBalance(property)) }}</dd></div>
          </dl>
          <button class="owner-mobile-row-action" type="button" :aria-expanded="expandedRental === property.ownerUnitId" @click="toggleRental(property.ownerUnitId)">
            {{ property.tenantName || $t('legacy.t_2d678e8e585e') }}<span>{{ formatDate(property.leaseEndDate) }} {{ $t('legacy.t_8066e0f41f1f') }}</span><ChevronDown :class="{ rotated: expandedRental === property.ownerUnitId }" :size="18" />
          </button>
          <div v-if="expandedRental === property.ownerUnitId" class="owner-mobile-inline-details tenant">
            <div class="owner-mobile-tenant-title"><UserRound :size="18" /><div><span>{{ $t('legacy.t_f2c025910b37') }}</span><strong>{{ property.tenantName || $t('legacy.t_2d678e8e585e') }}</strong></div><b v-if="property.tenantName">{{ $t('legacy.t_66aabd91fc41') }}</b></div>
            <dl>
              <div><dt>{{ $t('legacy.t_e732638998ba') }}</dt><dd>{{ property.leaseNo || '—' }}</dd></div>
              <div><dt>{{ $t('legacy.t_3e79de10ae35') }}</dt><dd>{{ formatDate(property.leaseStartDate) }} {{ $t('legacy.t_43401e739ef4') }} {{ formatDate(property.leaseEndDate) }}</dd></div>
              <div><dt>{{ $t('legacy.t_d0b57aede288') }}</dt><dd>{{ propertyCurrency(property) }} {{ money(property.currentMonthRentDue) }}</dd></div>
              <div><dt>{{ $t('legacy.t_6ccf7ee4e2ae') }}</dt><dd>{{ propertyCurrency(property) }} {{ money(property.currentMonthRentOutstanding) }}</dd></div>
            </dl>
            <div class="owner-mobile-detail-actions"><button type="button" @click="selectModule('ownerDocuments')">{{ $t('legacy.t_acb90d55414c') }}</button><button type="button" @click="selectModule('ownerExpenses')">{{ $t('legacy.t_6d38d88df4c7') }}</button></div>
          </div>
        </article>
      </div>
    </template>

    <template v-else>
      <section class="owner-mobile-surface owner-more-summary">
        <div><span>{{ $t('legacy.t_a39a3f21f732') }}</span><h2>{{ ownerName }}</h2><p>{{ countryLabel }} {{ $t('legacy.t_e6a9202bca51') }} {{ currency }}</p></div>
        <button type="button" @click="page.handleLogout()"><LogOut :size="17" />{{ $t('legacy.t_094774b4a77b') }}</button>
      </section>
      <section class="owner-mobile-section-head"><div><span>{{ $t('legacy.t_36e135d47637') }}</span><h2>{{ $t('legacy.t_c202f1476f7a') }}</h2></div></section>
      <div class="owner-mobile-service-list">
        <button type="button" @click="selectModule('ownerPayment')"><span><CircleDollarSign :size="21" /></span><div><strong>{{ $t('legacy.t_034f523e2a94') }}</strong><small>{{ $t('legacy.t_efaa423ef6ff') }}</small></div><ChevronRight :size="18" /></button>
        <button type="button" @click="selectModule('ownerDocuments')"><span><Files :size="21" /></span><div><strong>{{ $t('legacy.t_73fd7c1459e9') }}</strong><small>{{ $t('legacy.t_286cf5a5d188') }}</small></div><ChevronRight :size="18" /></button>
        <button type="button" @click="selectModule('ownerFinance')"><span><Landmark :size="21" /></span><div><strong>{{ $t('legacy.t_c75da5657804') }}</strong><small>{{ $t('legacy.t_2743385d6819') }}</small></div><ChevronRight :size="18" /></button>
        <button type="button" @click="selectModule('ownerReserve')"><span><ShieldCheck :size="21" /></span><div><strong>{{ $t('legacy.t_94d47e6a57f2') }}</strong><small>{{ $t('legacy.t_8ea611a3b29d') }}</small></div><ChevronRight :size="18" /></button>
      </div>
      <section class="owner-mobile-surface owner-mobile-readonly-note"><ShieldCheck :size="20" /><div><strong>{{ $t('legacy.t_891a05da2662') }}</strong><p>{{ $t('legacy.t_9a01af4b40b0') }}</p></div></section>
    </template>
  </section>
</template>

<script>
import { formatMonth } from '../utils/dateFormat';
import { formatDate } from '../utils/dateFormat';
import pageBridge from '../pageBridge';
import { AlertCircle, Building2, ChartNoAxesColumnIncreasing, ChevronDown, ChevronRight, CircleDollarSign, Files, KeyRound, Landmark, LoaderCircle, LogOut, ShieldCheck, UserRound, WalletCards } from '@lucide/vue';

export default {
  mixins: [pageBridge],
  components: { AlertCircle, Building2, ChartNoAxesColumnIncreasing, ChevronDown, ChevronRight, CircleDollarSign, Files, KeyRound, Landmark, LoaderCircle, LogOut, ShieldCheck, UserRound, WalletCards },
  data() { return { expandedAsset: null, expandedRental: null }; },
  computed: {
    properties() { return this.page.ownerDashboard?.properties || []; },
    summary() { return this.page.ownerDashboard?.summary || {}; },
    rentalProperties() { return this.properties.filter(item => item.assetStage === 'OPERATING' && ((item.services || []).includes('RENTAL') || item.tenantName)); },
    totalAssetValue() { return this.properties.reduce((sum, item) => sum + Number(item.purchasePrice || 0), 0); },
    monthlyRentIncome() { return Number(this.summary.monthlyRentIncome || 0); },
    reserveBalance() { return Number(this.summary.reserveBalance || 0); },
    assetCounts() { return { operating: this.properties.filter(item => item.assetStage === 'OPERATING').length, preHandover: this.properties.filter(item => item.assetStage === 'PRE_HANDOVER').length }; },
    currency() { return this.properties.find(item => item.currency)?.currency || 'MYR'; },
    countryCode() { return this.properties.find(item => item.countryCode)?.countryCode || 'MY'; },
    countryLabel() { try { return new Intl.DisplayNames([this.$i18n.locale], { type: 'region' }).of(this.countryCode) || this.countryCode; } catch { return this.countryCode; } },
    ownerName() { return this.page.currentUser?.displayName || this.page.currentUser?.username || this.$lt('业主'); },
    ownerInitial() { return String(this.ownerName).trim().slice(0, 1).toUpperCase() || 'CC'; },
    portfolioMonth() { return new Intl.DateTimeFormat(this.$i18n.locale, { year: 'numeric', month: 'long' }).format(new Date()); },
    viewTitle() { return this.$lt({ ownerProjects: '最新建案', myProperties: '资产总览', ownerRentalHub: '租管服务', ownerMore: '更多服务' }[this.currentId] || '业主服务'); },
    viewHint() { return this.$lt({ ownerProjects: '查看开放项目与讲座信息', myProperties: '资产价值、房款与房产状态', ownerRentalHub: '租金、租客、收支与合同', ownerMore: '文件、付款与账户服务' }[this.currentId] || ''); },
    assetRingStyle() { const operating = this.properties.length ? Math.round(this.assetCounts.operating / this.properties.length * 100) : 0; return { '--ring-progress': `${operating}%` }; },
    rentRingStyle() { const paid = this.properties.reduce((sum, item) => sum + Number(item.currentMonthRentPaid || 0), 0); const due = this.properties.reduce((sum, item) => sum + Number(item.currentMonthRentDue || 0), 0); return { '--ring-progress': `${due ? Math.min(100, Math.round(paid / due * 100)) : 0}%` }; }
  },
  mounted() { if (!this.properties.length && !this.page.databaseLoading) this.page.loadOwnerDashboard(); },
  methods: {
    initials(value) { return String(value || 'CC').split(/\s+/).map(word => word[0]).join('').slice(0, 2).toUpperCase(); },
    money(value) { return Number(value || 0).toLocaleString(this.$i18n.locale, { minimumFractionDigits: 2, maximumFractionDigits: 2 }); },
    compactMoney(value) { return new Intl.NumberFormat(this.$i18n.locale, { notation: 'compact', maximumFractionDigits: 1 }).format(Number(value || 0)); },
    propertyCurrency(property) { return property.currency || this.currency; },
    paymentPercent(property) { const total = Number(property.purchasePrice || 0); return total ? Math.min(100, Math.max(0, Math.round(Number(property.paidAmount || 0) / total * 100))) : 0; },
    unitBalance(property) { return Number(property.monthlyIncome || 0) - Number(property.monthlyExpense || 0); },
    formatDate,
    formatMonth,
    assetStage(value) { return this.$lt(value === 'PRE_HANDOVER' ? '预售／未交房' : value === 'OPERATING' ? '已交房' : '状态待确认'); },
    servicesText(services = []) { const labels = { RENTAL: '委租', MANAGEMENT: '委管', RESALE: '委售' }; return services.length ? services.map(item => this.$lt(labels[item] || item)).join(this.$i18n.locale === 'en' ? ', ' : '、') : this.$lt('尚未启用'); },
    toggleAsset(id) { this.expandedAsset = this.expandedAsset === id ? null : id; },
    toggleRental(id) { this.expandedRental = this.expandedRental === id ? null : id; }
  }
};
</script>
