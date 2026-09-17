<template>
  <main class="owner-app-v2" :data-country="activeCountry" :data-detail="detailView || 'root'" :data-module="currentId">
    <header class="owner-app-bar">
      <button v-if="detailView" class="owner-app-back" type="button" :aria-label="$t('legacy.t_94c327416801')" @click="closeDetail"><ArrowLeft :size="20" /></button>
      <div class="owner-app-heading">
        <span>{{ detailView ? detailEyebrow : `CCPS · ${countryLabel}` }}</span>
        <h1>{{ detailView ? detailTitle : currentId === 'myProperties' ? $t('ownerApp.redesign.welcome', { name: ownerName }) : viewTitle }}</h1>
        <p>{{ detailView ? detailHint : viewHint }}</p>
      </div>
      <label v-if="!detailView && countries.length > 1" class="owner-country-switch"><Globe2 :size="15" /><select v-model="countryFilter" :aria-label="$t('legacy.t_db7fcb8f1c4e')"><option v-for="item in countries" :key="item" :value="item">{{ countryName(item) }}</option></select></label>
      <div v-else class="owner-currency-badge"><small>{{ $t('ownerApp.currency') }}</small><strong>{{ currency }}</strong></div>
    </header>
    <OwnerSignatureCenter :show-entry="currentId === 'ownerNotice' && !detailView" :blocked="Boolean(detailView || page.databaseLoading)" />

    <button v-if="ticker && !detailView" class="owner-app-ticker" type="button" @click="openNotice(ticker)">
      <Bell :size="16" aria-hidden="true" /><span>{{ ticker.title }}</span><ChevronRight :size="16" aria-hidden="true" />
    </button>

    <label v-if="!detailView && countryCurrencies.length > 1" class="owner-app-field"><span>{{ $t('ownerApp.currency') }}</span><select :value="portfolioCurrency" @change="currencyFilter = $event.target.value"><option v-for="code in countryCurrencies" :key="code" :value="code">{{ code }}</option></select></label>
    <section v-if="!detailView && page.databaseLoading" class="owner-app-state" aria-live="polite"><LoaderCircle class="spinning" :size="24" /><strong>{{ $t('legacy.t_03a17d236ff2') }}</strong></section>
    <section v-else-if="!detailView && page.databaseError" class="owner-app-state is-error" role="alert"><AlertCircle :size="24" /><p>{{ $lt(page.databaseError) }}</p><button type="button" @click="page.loadOwnerDashboard()">{{ $t('ownerApp.retry') }}</button></section>
    <section v-else-if="detailView" class="owner-detail-shell">
      <section class="owner-property-switch">
        <label><span>{{ $t('ownerApp.property') }}</span><select :value="selectedProperty?.ownerUnitId" @change="switchDetailProperty"><option v-for="item in visibleProperties" :key="item.ownerUnitId" :value="item.ownerUnitId">{{ item.projectName }} · {{ item.unitNo }}</option></select></label>
      </section>
      <nav class="owner-detail-tabs" :aria-label="$t('ownerApp.property')">
        <button v-for="tab in detailTabs" :key="tab" type="button" :aria-pressed="detailView === tab" @click="openPropertyDetail(selectedProperty, tab)">{{ $t('ownerApp.' + (tab === 'property' ? 'facts' : tab)) }}</button>
      </nav>
      <section v-if="detailLoading" class="owner-app-state"><LoaderCircle class="spinning" :size="24" /><strong>{{ $t('legacy.t_03a17d236ff2') }}</strong><span>{{ $t('legacy.t_d0c5a3c9b6b0') }}</span></section>
      <section v-else-if="detailError" class="owner-app-state is-error" role="alert"><AlertCircle :size="24" /><strong>{{ $t('legacy.t_0a7a0cc9e3f9') }}</strong><span>{{ $lt(detailError) }}</span><button type="button" @click="reloadDetail">{{ $t('legacy.t_5982c44c18df') }}</button></section>

      <template v-else-if="detailView === 'payment'">
        <section v-if="paymentInstallments.length" class="owner-stat-hero is-payment"><div class="owner-stat-figure"><strong>{{ paymentPercent(selectedProperty) }}</strong><span>%</span></div><div><span>{{ $t('legacy.t_1d3d8de5730f') }}</span><h2>{{ selectedProperty.projectName }} · {{ selectedProperty.unitNo }}</h2><p>{{ $t('legacy.t_390e249deb38') }} {{ money(paymentSummary.paidAmount) }}{{ $t('legacy.t_2120c3d0ad97') }} {{ money(paymentSummary.remainingAmount) }}</p></div></section>
        <section class="owner-amount-strip"><div><span>{{ $t('legacy.t_dd55a2cc5bc6') }}</span><strong>{{ money(paymentInstallments.length ? paymentSummary.purchasePrice : selectedProperty.purchasePrice) }}</strong></div><div v-if="paymentInstallments.length"><span>{{ $t('legacy.t_f3bb802a266e') }}</span><strong>{{ money(paymentSummary.nextDueAmount) }}</strong><small>{{ paymentSummary.nextDueDate ? formatDate(paymentSummary.nextDueDate) : $t('legacy.t_474b1f2a2dc4') }}</small></div></section>
        <SectionHeading :eyebrow="$t('legacy.t_3863f9a8f8ea')" :title="$t('legacy.t_a368d881d03b')" :count="$t('ui.records', { count: paymentInstallments.length })" />
<div v-if="paymentInstallments.length" class="owner-timeline"><article v-for="item in paymentInstallments" :key="item.id" :class="`is-${item.status}`"><i><Check v-if="item.status === 'paid'" :size="15" /><Clock3 v-else :size="15" /></i><div><header><strong>{{ item.milestone || $t('ui.installmentNumber', { number: item.installmentNo }) }}</strong><b>{{ installmentStatus(item.status) }}</b></header><p>{{ item.dueDate ? formatDate(item.dueDate) : $t('legacy.t_6081bd847ad8') }}</p><dl><div><dt>{{ $t('legacy.t_97bab1676714') }}</dt><dd>{{ money(item.amountDue) }}</dd></div><div><dt>{{ $t('legacy.t_390e249deb38') }}</dt><dd>{{ money(item.amountPaid) }}</dd></div></dl><div v-if="item.proofDocumentIds?.length" class="owner-linked-files"><button v-for="(id, index) in item.proofDocumentIds" :key="id" type="button" @click="openLinkedDocument(id)">{{ $t('ownerApp.attachmentNumber', { n: index + 1 }) }}</button></div><span v-else class="owner-no-file">{{ $t('legacy.t_dfa87cfe3c0c') }}</span></div></article></div>
        <EmptyState v-else icon="file" :title="$t('legacy.t_ccd4489c1dcf')" :text="$t('legacy.t_ba628c0a438b')" compact />
        <OwnerProofSubmission v-if="selectedProperty.assetStage === 'PRE_HANDOVER' && paymentInstallments.length" :key="'payment-' + selectedProperty.ownerUnitId" kind="payment" :property="selectedProperty" :installments="paymentInstallments" :owner-name="ownerName" @submitted="documentsLoaded = false" />
      </template>

      <template v-else-if="detailView === 'cashflow'">
        <OwnerProofSubmission :key="'reserve-' + selectedProperty.ownerUnitId" kind="reserve" :property="selectedProperty" :owner-name="ownerName" @submitted="documentsLoaded = false" />
        <section class="owner-detail-toolbar"><button type="button" @click="cashflowYear--"><ChevronLeft :size="18" />{{ $t('legacy.t_cbd823c1d838') }}</button><strong>{{ cashflowYear }}</strong><button type="button" :disabled="cashflowYear >= currentYear" @click="cashflowYear++">{{ $t('legacy.t_743fc07e032d') }}<ChevronRight :size="18" /></button></section>
        <section class="owner-stat-hero is-rental"><div class="owner-stat-figure is-money"><span>{{ currency }}</span><strong>{{ compactMoney(cashflowBalance) }}</strong></div><div><span>{{ $t('ownerApp.annual') }}</span><h2>{{ selectedProperty.projectName }} · {{ selectedProperty.unitNo }}</h2><p>{{ $t('ownerApp.income') }} {{ money(cashflowIncome) }} · {{ $t('ownerApp.expense') }} {{ money(cashflowExpense) }}</p></div></section>
        <section class="owner-amount-strip"><div><span>{{ $t('ownerApp.reserve') }}</span><strong>{{ money(selectedProperty.reserveBalance) }}</strong></div><div><span>{{ $t('ownerApp.deposit') }}</span><strong>{{ money(selectedProperty.tenantDepositAmount) }}</strong></div></section>
        <section class="owner-bar-chart" :aria-label="$t('legacy.t_854cc704d83b')"><div v-for="bar in monthlyBars" :key="bar.month" role="img" :aria-label="monthLabel(bar.month) + ': ' + $t('ownerApp.income') + ' ' + money(bar.income) + ', ' + $t('ownerApp.expense') + ' ' + money(bar.expense)"><span><i class="income" :style="{ height: bar.incomeHeight + '%' }"></i><i class="expense" :style="{ height: bar.expenseHeight + '%' }"></i></span><small>{{ bar.month }}</small></div></section>
        <div class="owner-chart-legend"><span class="income">{{ $t('ownerApp.income') }}</span><span class="expense">{{ $t('ownerApp.expense') }}</span></div>
        <details class="owner-advanced-filters">
          <summary>{{ $t('ownerApp.category') }} · {{ $t('ownerApp.month') }}</summary>
          <div class="owner-filter-grid">
            <label class="owner-app-field"><span>{{ $t('ownerApp.month') }}</span><select v-model="cashflowMonth"><option value="">{{ $t('ownerApp.all') }}</option><option v-for="month in 12" :key="month" :value="String(month)">{{ monthLabel(month) }}</option></select></label>
            <label class="owner-app-field"><span>{{ $t('ownerApp.direction') }}</span><select v-model="cashflowType"><option value="all">{{ $t('ownerApp.all') }}</option><option value="income">{{ $t('ownerApp.income') }}</option><option value="expense">{{ $t('ownerApp.expense') }}</option></select></label>
            <label class="owner-app-field"><span>{{ $t('ownerApp.category') }}</span><select v-model="cashflowCategoryFilter"><option value="all">{{ $t('ownerApp.all') }}</option><option v-for="category in cashflowCategories" :key="category" :value="category">{{ $lt(categoryLabel(category)) }}</option></select></label>
            <label class="owner-app-field"><span>{{ $t('ownerApp.searchRecords') }}</span><input v-model="cashflowSearch" type="search" :placeholder="$t('ownerApp.searchRecords')"></label>
          </div>
        </details>
        <div class="owner-filter-actions"><button type="button" @click="resetCashflowFilters">{{ $t('ownerApp.reset') }}</button><button type="button" :disabled="!filteredCashflows.length" @click="exportCashflow"><FileSpreadsheet :size="17" />{{ $t('legacy.t_c7f85bf27614') }}</button></div>
        <div class="owner-selection-summary"><span>{{ $t('ownerApp.selectionNet') }}</span><strong>{{ money(selectionTotals.net) }}</strong><small>{{ $t('ui.records', { count: filteredCashflows.length }) }}</small></div>
        <nav class="owner-segment" :aria-label="$t('legacy.t_8179ff1a7945')"><button type="button" :class="{ active: cashflowMode === 'month' }" :aria-pressed="cashflowMode === 'month'" @click="cashflowMode = 'month'">{{ $t('legacy.t_2e72896dd7d3') }}</button><button type="button" :class="{ active: cashflowMode === 'records' }" :aria-pressed="cashflowMode === 'records'" @click="cashflowMode = 'records'">{{ $t('legacy.t_dc57b55ce515') }}</button></nav>
        <p class="owner-context-note">{{ $t('ownerApp.balanceHint') }}</p>
        <div v-if="cashflowMode === 'month'" class="owner-month-list">
          <article v-for="month in monthGroups" :key="month.key">
            <button type="button" :aria-expanded="expandedMonth === month.key" @click="expandedMonth = expandedMonth === month.key ? '' : month.key"><span><strong>{{ monthLabel(month.month) }}</strong><small>{{ $t('ui.records', { count: month.records.length }) }}</small></span><b :class="{ negative: month.net < 0 }">{{ signedMoney(month.net) }}</b><ChevronDown :class="{ rotated: expandedMonth === month.key }" :size="18" /></button>
            <div v-if="expandedMonth === month.key">
              <dl class="owner-month-categories"><div v-for="category in month.categories" :key="category.category"><dt>{{ $lt(categoryLabel(category.category)) }}</dt><dd>{{ signedMoney(category.net) }}</dd></div><div><dt>{{ $t('ownerApp.income') }}</dt><dd>{{ money(month.income) }}</dd></div><div><dt>{{ $t('ownerApp.expense') }}</dt><dd>{{ money(month.expense) }}</dd></div></dl>
              <div class="owner-transaction-list"><OwnerTransactionRow v-for="item in month.records" :key="item.key" :item="item" :currency="currency" @open-document="openLinkedDocument" /></div>
            </div>
          </article>
        </div>
        <div v-else-if="filteredCashflows.length" class="owner-transaction-list is-standalone"><OwnerTransactionRow v-for="item in filteredCashflows" :key="item.key" :item="item" :currency="currency" @open-document="openLinkedDocument" /></div>
        <EmptyState v-else icon="chart" :title="$t('legacy.t_8a9caa185292')" :text="$t('legacy.t_ee0e62e30835')" compact />
      </template>

      <template v-else-if="detailView === 'tenant'">
        <label class="owner-app-field"><span>{{ $t('legacy.t_e7b1f5be0761') }}</span><select v-model="selectedLeaseId"><option value="">{{ $t('ownerApp.currentLease') }}</option><option v-for="lease in propertyDetail.leases || []" :key="lease.id" :value="String(lease.id)">{{ lease.tenantName }} · {{ lease.leaseNo || formatDate(lease.startDate) }}</option></select></label>
        <section v-if="activeLease" class="owner-tenant-card">
          <header><div class="owner-tenant-avatar"><UserRound :size="24" /></div><div><span>{{ $t('ownerApp.tenant') }}</span><h2>{{ activeLease.tenantName }}</h2><p>{{ activeLease.leaseNo || '—' }}</p></div><b>{{ leaseStatus(activeLease.status) }}</b></header>
          <dl><div><dt>{{ $t('legacy.t_101a8e3682b8') }}</dt><dd>{{ money(activeLease.monthlyRent) }}</dd></div><div><dt>{{ $t('ownerApp.deposit') }}</dt><dd>{{ money(activeLease.depositAmount) }}</dd></div><div><dt>{{ $t('legacy.t_e4b3e401a42e') }}</dt><dd>{{ formatDate(activeLease.startDate) }} — {{ formatDate(activeLease.endDate) }}</dd></div><div><dt>{{ $t('legacy.t_e732638998ba') }}</dt><dd>{{ activeLease.leaseNo || '—' }}</dd></div></dl>
          <button type="button" :disabled="!activeLease.contractDocumentId" @click="openLinkedDocument(activeLease.contractDocumentId)"><FileText :size="17" />{{ $t('ownerApp.contract') }}</button>
        </section>
        <EmptyState v-else icon="history" :title="$t('ownerApp.noCurrentLease')" :text="$t('legacy.t_edfec6540588')" compact />
        <OwnerPhotoGallery :photos="detailPhotos" :leases="propertyDetail.leases || []" :selected-lease-id="activeLease?.id" @open="openDocument" />
      </template>

      <template v-else-if="detailView === 'files' || detailView === 'property'">
        <OwnerPropertyCover v-if="detailView === 'property'" class="owner-facts-cover" :property="selectedProperty" />
        <section v-if="detailView === 'property'" class="owner-property-facts"><dl><div><dt>{{ $t('legacy.t_dd55a2cc5bc6') }}</dt><dd>{{ money(selectedProperty.purchasePrice) }}</dd></div><div><dt>{{ $t('legacy.t_1434a7e90d2b') }}</dt><dd>{{ selectedProperty.areaSqm ? `${selectedProperty.areaSqm} m²` : '—' }}</dd></div><div><dt>{{ $t('legacy.t_5a3522e07891') }}</dt><dd>{{ selectedProperty.unitNo || '—' }}</dd></div><div><dt>{{ $t('legacy.t_aa78dfd39fde') }}</dt><dd>{{ assetStage(selectedProperty.assetStage) }}</dd></div><div><dt>{{ $t('legacy.t_c9ae7a023acd') }}</dt><dd>{{ serviceStatus('RENTAL') }}</dd></div><div><dt>{{ $t('legacy.t_d16932df4520') }}</dt><dd>{{ serviceStatus('MANAGEMENT') }}</dd></div></dl><section><span>{{ $t('legacy.t_50d624c775f4') }}</span><h3>{{ propertyDetail.bankAccount?.bankName || $t('legacy.t_6369653ea047') }}</h3><p v-if="propertyDetail.bankAccount">{{ propertyDetail.bankAccount.accountName }} · {{ propertyDetail.bankAccount.maskedAccountNo }}</p><p v-else>{{ $t('legacy.t_2eb489750c1a') }}</p></section><section v-if="propertyDetail.mandates?.length" class="owner-mandate-list"><span>{{ $t('legacy.t_f1ced494c6d8') }}</span><article v-for="mandate in propertyDetail.mandates" :key="mandate.id"><div><h3>{{ mandateType(mandate.mandateType) }}</h3><p>{{ formatDate(mandate.startDate) }} {{ $t('legacy.t_43401e739ef4') }} {{ formatDate(mandate.endDate) }}</p></div><b>{{ leaseStatus(mandate.status) }}</b></article></section></section>
        <OwnerPhotoGallery v-if="detailView === 'property'" :photos="detailPhotos" :leases="propertyDetail.leases || []" @open="openDocument" />
        <div class="owner-filter-actions"><span>{{ $t('ownerApp.fileCount', { count: filteredDocuments.length, total: propertyDocuments.length }) }}</span><button type="button" @click="refreshDocuments">{{ $t('ownerApp.refresh') }}</button></div>
        <label class="owner-app-field"><span>{{ $t('ownerApp.searchFiles') }}</span><input v-model="documentSearch" type="search" :placeholder="$t('ownerApp.searchFiles')"></label>
        <div class="owner-filter-row is-files"><label><span>{{ $t('legacy.t_b618af8b44f4') }}</span><select v-model="documentType"><option v-for="key in ownerDocumentCategories" :key="key" :value="key">{{ $t('ownerApp.documentCategories.' + key) }}</option></select></label></div>
        <div v-if="filteredDocuments.length" class="owner-document-list"><article v-for="file in filteredDocuments"  :key="ownerDocumentKey(file)"><span><FileText :size="20" /></span><div><strong>{{ file.name }}</strong><small>{{ $lt(file.typeLabel) }} · {{ formatDateTime(file.createdAt) }}</small></div><b>{{ $lt(file.status) }}</b><button type="button" :disabled="!file.downloadable" @click="openDocument(file)"><Eye :size="18" />{{ $t('legacy.t_de61aa8e1cbc') }}</button></article></div>
        <EmptyState v-else icon="file" :title="$t('legacy.t_5d7415bdca6a')" :text="$t('legacy.t_cb25e3ef1229')" compact />
      </template>
    </section>

    <template v-else-if="currentId === 'ownerProjects'">
      <OwnerEditorialBanner kind="architecture" :country="activeCountry" />
      <section class="owner-portfolio-board" :aria-label="$t('legacy.t_28f3bd666e74')">
      <header class="owner-board-heading"><div><span>{{ portfolioMonth }}</span><h2>{{ $t('ownerApp.redesign.assetValue') }}</h2><p>{{ countryLabel }} · {{ $t('ui.properties', { count: visibleProperties.length }) }}</p></div><div class="owner-stat-figure is-money"><span>{{ currency }}</span><strong>{{ compactMoney(totalAssetValue) }}</strong></div></header>
      <section class="owner-composition-card"><div class="owner-composition-ring" :style="assetRingStyle"><span>{{ visibleProperties.length }}</span><small>{{ $t('legacy.t_ba764234ee64') }}</small></div><div><h2>{{ $t('legacy.t_28f3bd666e74') }}</h2><p><i class="is-operating"></i>{{ $t('legacy.t_80315a92e528') }} {{ assetCounts.operating }} {{ $t('legacy.t_032231d845f8') }}</p><p><i class="is-pre"></i>{{ $t('legacy.t_76e5d6e50d9e') }} {{ assetCounts.preHandover }} {{ $t('legacy.t_032231d845f8') }}</p></div><div v-if="paymentRecordsComplete"><span>{{ $t('legacy.t_7add24e919ce') }}</span><strong>{{ compactMoney(totalPaidAmount) }}</strong><small>{{ portfolioPaymentPercent }}%</small></div><div v-else><span>{{ $t('ownerApp.paymentIncomplete') }}</span></div></section>
      </section>
      <SectionHeading :eyebrow="$t('legacy.t_b54ae62e75ca')" :title="$t('legacy.t_efc348384e21')" :count="$t('ui.properties', { count: visibleProperties.length })" />
      <div class="owner-filter-grid owner-asset-filters"><label class="owner-app-field"><span>{{ $t('ownerApp.redesign.searchAssets') }}</span><input v-model="propertySearch" type="search" :placeholder="$t('ownerApp.search')"></label><label class="owner-app-field"><span>{{ $t('legacy.t_aa78dfd39fde') }}</span><select v-model="propertyStage"><option value="all">{{ $t('ownerApp.all') }}</option><option value="PRE_HANDOVER">{{ assetStage('PRE_HANDOVER') }}</option><option value="OPERATING">{{ assetStage('OPERATING') }}</option></select></label></div>
      <OwnerPropertyCards v-if="searchedProperties.length" :properties="searchedProperties" :currency="currency" @open="openPropertyDetail" />
      <EmptyState v-else icon="building" :title="$t('ownerApp.noMatches')" text="" compact />
    </template>

    <template v-else-if="currentId === 'ownerNotice'">
      <section class="owner-notice-summary"><div><strong>{{ notificationSummary.unreadCount || 0 }}</strong><span>{{ $t('legacy.t_2847d84d9f82') }}</span></div><div><strong>{{ notificationSummary.importantCount || 0 }}</strong><span>{{ $t('legacy.t_66a359fec422') }}</span></div><button type="button" :disabled="!notificationSummary.unreadCount" @click="readAllNotices"><CheckCheck :size="17" />{{ $t('legacy.t_ac210d6e7dbb') }}</button></section>
      <div class="owner-notice-search"><label class="owner-app-field"><span>{{ $t('ownerApp.searchNotices') }}</span><input v-model="noticeSearch" type="search" :placeholder="$t('ownerApp.searchNotices')"></label><label class="owner-unread-toggle"><input v-model="unreadOnly" type="checkbox">{{ $t('ownerApp.unreadOnly') }}</label></div>
      <nav class="owner-notice-tabs" :aria-label="$t('legacy.t_4f80c1ef9207')"><button v-for="tab in noticeTabs" :key="tab.key" type="button" :class="{ active: noticeCategory === tab.key }" :aria-pressed="noticeCategory === tab.key" @click="noticeCategory = tab.key">{{ $lt(tab.label) }}<b>{{ tab.count }}</b></button></nav>
      <div v-if="notificationsLoading" class="owner-card-skeletons"><i v-for="n in 4" :key="n"></i></div>
      <div v-else-if="visibleNotices.length" class="owner-notice-list"><article v-for="item in visibleNotices" :key="item.id" :class="{ unread: item.status !== 'read', important: ['important','high'].includes(item.priority) }" @click="openNotice(item)" role="button" tabindex="0" @keydown.enter.prevent="openNotice(item)" @keydown.space.prevent="openNotice(item)"><span><AlertCircle v-if="['important','high'].includes(item.priority)" :size="19" /><Bell v-else :size="19" /></span><div><header><strong>{{ item.title }}</strong><b v-if="['important','high'].includes(item.priority)">{{ $t('legacy.t_b7f46707527b') }}</b></header><p>{{ item.body }}</p><small>{{ item.projectName || $t('legacy.t_b7575d6f5557') }}<template v-if="item.unitNo"> · {{ item.unitNo }}</template> · {{ formatDateTime(item.createdAt) }}</small></div><ChevronRight :size="17" /></article></div>
      <EmptyState v-else icon="bell" :title="$t('legacy.t_66ada7597e41')" :text="$t('legacy.t_26eabcf680fd')" />
    </template>

    <template v-else-if="currentId === 'myProperties'">
      <OwnerEditorialBanner :country="activeCountry" />
      <section class="owner-home-dashboard" :aria-label="$t('ownerApp.redesign.overview')">
      <section class="owner-stat-hero is-assets owner-home-overview">
        <div class="owner-overview-caption"><span>{{ portfolioMonth }}</span><h2>{{ $t('ownerApp.redesign.assetValue') }}</h2></div>
        <div class="owner-stat-figure is-money"><span>{{ currency }}</span><strong>{{ compactMoney(totalAssetValue) }}</strong></div>
        <div class="owner-home-estate"><Building2 :size="48" :stroke-width="1.1" aria-hidden="true" /><span><strong>{{ visibleProperties.length }}</strong> {{ $t('legacy.t_ba764234ee64') }}</span></div>
        <div class="owner-overview-legend"><span><i></i>{{ $t('legacy.t_80315a92e528') }} {{ assetCounts.operating }}</span><span><i class="is-pre"></i>{{ $t('legacy.t_76e5d6e50d9e') }} {{ assetCounts.preHandover }}</span></div>
      </section>
      <section class="owner-home-key-metrics" :aria-label="$t('legacy.t_08a1d24ee0c5')"><div><span>{{ $t('legacy.t_3dae5acc41fa') }}</span><strong>{{ currency }} {{ compactMoney(monthlyRentIncome) }}</strong></div><div><span>{{ $t('legacy.t_ce8617621e8e') }}</span><strong :class="{ negative: reserveBalance < 0 }">{{ currency }} {{ compactMoney(reserveBalance) }}</strong></div><div><span>{{ $t('legacy.t_1d3d8de5730f') }}</span><strong>{{ paymentRecordsComplete ? `${portfolioPaymentPercent}%` : '—' }}</strong><small v-if="!paymentRecordsComplete">{{ $t('ownerApp.paymentIncomplete') }}</small></div></section>
      </section>
      <nav class="owner-quick-grid owner-home-actions" :aria-label="$t('legacy.t_618e88a30498')"><button type="button" @click="page.selectModule('ownerProjects')"><Building2 :size="24" aria-hidden="true" /><span>{{ $t('ownerApp.shortcuts.assets') }}</span></button><button type="button" @click="page.selectModule('ownerRentalHub')"><KeyRound :size="24" aria-hidden="true" /><span>{{ $t('ownerApp.shortcuts.rentals') }}</span></button><button type="button" @click="page.selectModule('ownerNotice')"><Bell :size="24" aria-hidden="true" /><span>{{ $t('ownerApp.shortcuts.notices') }}</span></button><button type="button" @click="openFirstProperty('files')"><Files :size="24" aria-hidden="true" /><span>{{ $t('ownerApp.shortcuts.documents') }}</span></button></nav>
      <SectionHeading :eyebrow="$t('legacy.t_f6ce832f3d10')" :title="$t('legacy.t_7df745bb0e32')" :count="$t('ui.properties', { count: visibleProperties.length })" />
      <OwnerPropertyCards :properties="visibleProperties.slice(0, 2)" :currency="currency" @open="openPropertyDetail" />
      <button v-if="visibleProperties.length > 2" class="owner-home-more" type="button" @click="page.selectModule('ownerProjects')">{{ $t('legacy.t_feaa9d6bf566') }}<ChevronRight :size="17" /></button>
    </template>

    <template v-else-if="currentId === 'ownerRentalHub'">
      <OwnerEditorialBanner kind="living" :country="activeCountry" />
      <section class="owner-portfolio-board owner-rental-board" :aria-label="$t('ownerApp.redesign.rentalOverview')">
      <header class="owner-board-heading"><div><span>{{ portfolioMonth }}</span><h2>{{ $t('ownerApp.redesign.rentIncome') }}</h2><p>{{ $t('ownerApp.managedCount', { count: rentalProperties.length }) }}</p></div><div class="owner-stat-figure is-money"><span>{{ currency }}</span><strong>{{ compactMoney(monthlyRentIncome) }}</strong></div></header>
      <div class="owner-board-reserve"><ShieldCheck :size="16" aria-hidden="true" /><span>{{ $t('ownerApp.reserve') }}</span><strong>{{ money(reserveBalance) }}</strong></div>
      <section class="owner-composition-card is-rental"><div class="owner-composition-ring" :style="rentRingStyle"><span>{{ rentCollectionPercent }}%</span><small>{{ $t('legacy.t_fd6b131998e0') }}</small></div><div><h2>{{ $t('legacy.t_8f871a860456') }}</h2><p><i class="is-operating"></i>{{ $t('legacy.t_2eb877f1cc1d') }} {{ compactMoney(totalRentPaid) }}</p><p><i class="is-pre"></i>{{ $t('legacy.t_6ccf7ee4e2ae') }} {{ compactMoney(totalRentOutstanding) }}</p></div><div><span>{{ $t('ownerApp.monthlyNet') }}</span><strong>{{ compactMoney(totalUnitBalance) }}</strong><small>{{ currency }}</small></div></section>
      </section>
      <nav class="owner-quick-grid" :aria-label="$t('legacy.t_17c34f15cc95')"><button type="button" @click="openFirstRental('cashflow')"><ChartNoAxesColumnIncreasing :size="24" aria-hidden="true" /><span>{{ $t('ownerApp.shortcuts.cashflow') }}</span></button><button type="button" @click="openFirstRental('tenant')"><UsersRound :size="24" aria-hidden="true" /><span>{{ $t('ownerApp.shortcuts.tenants') }}</span></button><button type="button" @click="openFirstRental('files')"><Files :size="24" aria-hidden="true" /><span>{{ $t('ownerApp.shortcuts.documents') }}</span></button><button type="button" @click="openFirstRental('cashflow')"><ShieldCheck :size="24" aria-hidden="true" /><span>{{ $t('ownerApp.shortcuts.reserve') }}</span></button></nav>
      <SectionHeading :eyebrow="$t('legacy.t_97d426e9abcc')" :title="$t('legacy.t_f84195f63948')" :count="$t('ui.properties', { count: rentalProperties.length })" />
      <label class="owner-app-field"><span>{{ $t('ownerApp.search') }}</span><input v-model="propertySearch" type="search" :placeholder="$t('ownerApp.search')"></label>
      <div v-if="searchedRentals.length" class="owner-rental-list"><article v-for="property in searchedRentals" :key="property.ownerUnitId"><header><OwnerPropertyCover :property="property" /><div><span>{{ property.projectName }}</span><h2>{{ property.unitNo }}</h2><p>{{ property.areaSqm ? `${property.areaSqm} m²` : property.city || '—' }}</p></div><b>{{ $t('ownerApp.' + (property.tenantName ? 'occupied' : 'noCurrentLease')) }}</b></header><dl><div><dt>{{ $t('legacy.t_101a8e3682b8') }}</dt><dd>{{ money(property.monthlyRent, property.currency) }}</dd></div><div><dt>{{ $t('ownerApp.leaseEnd') }}</dt><dd>{{ formatDate(property.leaseEndDate) }}</dd></div><div><dt>{{ $t('ownerApp.income') }}</dt><dd>{{ money(property.currentMonthRentPaid, property.currency) }}</dd></div><div><dt>{{ $t('legacy.t_819c8bd23823') }}</dt><dd>{{ money(property.tenantDepositAmount, property.currency) }}</dd></div><div><dt>{{ $t('legacy.t_94d47e6a57f2') }}</dt><dd>{{ money(property.reserveBalance, property.currency) }}</dd></div><div><dt>{{ $t('ownerApp.monthlyNet') }}</dt><dd>{{ money(unitBalance(property), property.currency) }}</dd></div></dl><footer><span><UserRound :size="15" />{{ property.tenantName || $t('legacy.t_2d678e8e585e') }}</span><button type="button" @click="openPropertyDetail(property, 'tenant')">{{ $t('legacy.t_325c7006df2c') }}<ChevronRight :size="16" /></button></footer></article></div>
      <EmptyState v-else icon="key" :title="$t('legacy.t_2ad26d8d696a')" :text="$t('legacy.t_e14c69e4a6ef')" />
    </template>

    <template v-else>
      <div class="owner-profile-panel">
      <OwnerBrandMasthead />
      <section class="owner-profile-card"><div class="owner-app-avatar is-large"><strong>{{ ownerInitial }}</strong><small>{{ currency }}</small></div><div><span>{{ $t('legacy.t_832e06a25eac') }}</span><h2>{{ ownerName }}</h2><p>{{ countryLabel }} · {{ visibleProperties.length }} {{ $t('legacy.t_ba764234ee64') }}</p></div><ShieldCheck :size="24" aria-hidden="true" /></section>
      </div>
      <OwnerAccountSettings @password-changed="page.handleLogout()" />
      <SectionHeading :eyebrow="$t('legacy.t_221bae3ef7ac')" :title="$t('legacy.t_c202f1476f7a')" />
      <div class="owner-service-menu"><button type="button" @click="openFirstProperty('payment')"><CircleDollarSign :size="21" /><div><strong>{{ $t('legacy.t_034f523e2a94') }}</strong><small>{{ $t('legacy.t_a00189645159') }}</small></div><ChevronRight :size="18" /></button><button type="button" @click="openFirstRental('cashflow')"><ChartNoAxesColumnIncreasing :size="21" /><div><strong>{{ $t('legacy.t_4a5f440bbe4b') }}</strong><small>{{ $t('legacy.t_ac58fb6dd99c') }}</small></div><ChevronRight :size="18" /></button><button type="button" @click="openFirstRental('tenant')"><UsersRound :size="21" /><div><strong>{{ $t('legacy.t_bce0d26a4bfd') }}</strong><small>{{ $t('legacy.t_73a2678e7ab6') }}</small></div><ChevronRight :size="18" /></button><button type="button" @click="openFirstProperty('property')"><Landmark :size="21" /><div><strong>{{ $t('legacy.t_49e63012b6a1') }}</strong><small>{{ $t('legacy.t_666f738dc8ef') }}</small></div><ChevronRight :size="18" /></button><button type="button" @click="openFirstProperty('files')"><Files :size="21" /><div><strong>{{ $t('legacy.t_2bf9f46cae52') }}</strong><small>{{ $t('legacy.t_66e6b230b2fa') }}</small></div><ChevronRight :size="18" /></button></div>
      <SectionHeading :eyebrow="$t('legacy.t_ac505fb5ef2f')" :title="$t('legacy.t_c24913edcabb')" />
      <EmptyState icon="building" :title="$t('ownerApp.projectsUnavailable')" :text="$t('ownerApp.projectsUnavailableHint')" compact />
      <section class="owner-readonly-note"><ShieldCheck :size="20" /><div><strong>{{ $t('ownerApp.account.title') }}</strong><p>{{ $t('ownerApp.selfServiceHint') }}</p></div></section>
      <button class="owner-signout" type="button" @click="page.handleLogout()"><LogOut :size="18" aria-hidden="true" />{{ $t('legacy.t_feecb1e6adec') }}</button>
    </template>
    <dialog ref="noticeDialog" class="owner-notice-dialog" :aria-label="$t('ownerApp.notice')" @pointerdown.self="$refs.noticeDialog.close()">
      <article v-if="activeNotice"><header><span>{{ $t('ownerApp.notice') }}</span><button type="button" @click="$refs.noticeDialog.close()">{{ $t('ownerApp.close') }}</button></header><h2>{{ activeNotice.title }}</h2><small>{{ activeNotice.projectName }} {{ activeNotice.unitNo }} · {{ formatDateTime(activeNotice.createdAt) }}</small><p>{{ activeNotice.body }}</p><button v-if="relatedNoticeProperty" type="button" @click="openRelatedNotice">{{ $t('ownerApp.relatedProperty') }}<ChevronRight :size="18" /></button></article>
    </dialog>
    <OwnerDocumentPreview ref="documentPreview" @toast="page.showToast" />
  </main>
</template>

<script>
import { formatDate, formatDateTime, formatMonth } from '../../utils/dateFormat';
import './owner-mobile-enhancements.css';
import './owner-mobile-visual.css';
import { ownerNoticeTabs, hasCompleteOwnerPaymentRecords, cashflowCategories, cashflowCategory, filterCashflows, groupCashflowMonths, searchOwnerProperties, summarizeCashflows } from '../../utils/ownerPortfolio';
import OwnerPhotoGallery from './OwnerPhotoGallery.vue';
import OwnerSignatureCenter from './OwnerSignatureCenter.vue';
import OwnerEditorialBanner from './OwnerEditorialBanner.vue';
import OwnerBrandMasthead from './OwnerBrandMasthead.vue';
import OwnerAccountSettings from './OwnerAccountSettings.vue';
import OwnerProofSubmission from './OwnerProofSubmission.vue';
import * as XLSX from 'xlsx';
import pageBridge from '../../pageBridge';
import { fetchOwnerDocuments, fetchOwnerNotifications, fetchOwnerPropertyCashflows, fetchOwnerPropertyDetail, fetchPaymentProgress, markAllOwnerNotificationsRead, markOwnerNotificationRead } from '../../services/propertyApi';
import { decodeMimeFilename } from '../../utils/mimeFilename';
import OwnerDocumentPreview from './OwnerDocumentPreview.vue';
import OwnerPropertyCover from './OwnerPropertyCover.vue';
import OwnerPropertyCards from './OwnerPropertyCards.vue';
import { ownerDocumentKey, matchesOwnerDocumentProperty, matchesOwnerDocumentCategory, ownerDocumentCategories } from '../../utils/ownerDocuments';
import OwnerTransactionRow from './OwnerTransactionRow.vue';
import SectionHeading from './SectionHeading.vue';
import EmptyState from './EmptyState.vue';
import { AlertCircle, ArrowLeft, ArrowUpRight, Bell, Building2, ChartNoAxesColumnIncreasing, Check, CheckCheck, ChevronDown, ChevronLeft, ChevronRight, CircleDollarSign, Clock3, Eye, FileSpreadsheet, FileText, Files, Globe2, Image as ImageIcon, KeyRound, Landmark, LoaderCircle, LogOut, ShieldCheck, UserRound, UsersRound } from '@lucide/vue';

export default {
  mixins: [pageBridge],
  // Local editorial photography never replaces property archive images.
  components: { OwnerSignatureCenter, OwnerEditorialBanner, OwnerBrandMasthead, OwnerAccountSettings, OwnerProofSubmission, OwnerPropertyCover, OwnerPhotoGallery, OwnerDocumentPreview, OwnerPropertyCards, OwnerTransactionRow, SectionHeading, EmptyState, AlertCircle, ArrowLeft, ArrowUpRight, Bell, Building2, ChartNoAxesColumnIncreasing, Check, CheckCheck, ChevronDown, ChevronLeft, ChevronRight, CircleDollarSign, Clock3, Eye, FileSpreadsheet, FileText, Files, Globe2, ImageIcon, KeyRound, Landmark, LoaderCircle, LogOut, ShieldCheck, UserRound, UsersRound },
  data: () => ({ ownerDocumentCategories, currencyFilter: '', propertySearch: '', propertyStage: 'all', cashflowMonth: '', cashflowCategoryFilter: 'all', cashflowSearch: '', documentSearch: '', noticeSearch: '', unreadOnly: false, activeNotice: null, selectedLeaseId: '', detailRequest: 0, cashflowCategories, detailView: '', selectedProperty: null, detailLoading: false, detailError: '', paymentDetails: null, cashflowRecords: [], cashflowYear: new Date().getFullYear(), cashflowMode: 'month', cashflowType: 'all', expandedMonth: '', documents: [], documentsLoaded: false, documentType: 'all', propertyDetail: {}, notifications: [], notificationSummary: {}, noticeTabs: [{ key: 'all', label: '全部', count: 0 }], noticeCategory: 'all', notificationsLoading: false, countryFilter: '' }),
  computed: {
    paymentRecordsComplete() { return hasCompleteOwnerPaymentRecords(this.visibleProperties); },
    properties() { return this.page.ownerDashboard?.properties || []; }, summary() { return this.page.ownerDashboard?.summary || {}; },
    countries() { return [...new Set(this.properties.map(item => item.countryCode || 'MY'))]; }, activeCountry() { return this.countryFilter || this.countries[0] || 'MY'; },
    countryProperties() { return this.properties.filter(item => (item.countryCode || 'MY') === this.activeCountry); },
    countryCurrencies() { return [...new Set(this.countryProperties.map(item => item.currency || (this.activeCountry === 'TH' ? 'THB' : 'MYR')))]; },
    portfolioCurrency() { return this.countryCurrencies.includes(this.currencyFilter) ? this.currencyFilter : this.countryCurrencies[0] || (this.activeCountry === 'TH' ? 'THB' : 'MYR'); },
    visibleProperties() { return this.countryProperties.filter(item => (item.currency || (this.activeCountry === 'TH' ? 'THB' : 'MYR')) === this.portfolioCurrency); }, rentalProperties() { return this.visibleProperties.filter(item => item.assetStage === 'OPERATING' && ((item.services || []).includes('RENTAL') || item.tenantName)); },
    totalAssetValue() { return this.visibleProperties.reduce((sum, item) => sum + Number(item.purchasePrice || 0), 0); }, totalPaidAmount() { return this.visibleProperties.reduce((sum, item) => sum + Number(item.paidAmount || 0), 0); }, portfolioPaymentPercent() { return this.totalAssetValue ? Math.round(this.totalPaidAmount / this.totalAssetValue * 100) : 0; },
    monthlyRentIncome() { return this.visibleProperties.reduce((sum, item) => sum + Number(item.currentMonthRentPaid || 0), 0); }, reserveBalance() { return this.visibleProperties.reduce((sum, item) => sum + Number(item.reserveBalance || 0), 0); }, assetCounts() { return { operating: this.visibleProperties.filter(item => item.assetStage === 'OPERATING').length, preHandover: this.visibleProperties.filter(item => item.assetStage === 'PRE_HANDOVER').length }; },
    currency() { if (this.selectedProperty?.currency) return this.selectedProperty.currency; return this.portfolioCurrency; }, countryLabel() { return this.countryName(this.activeCountry); }, ownerName() { return this.page.currentUser?.displayName || this.page.currentUser?.username || this.$lt('业主'); }, ownerInitial() { return String(this.ownerName).trim().slice(0, 1).toUpperCase() || 'CC'; }, portfolioMonth() { return new Intl.DateTimeFormat(this.$i18n.locale, { year: 'numeric', month: 'long' }).format(new Date()); }, currentYear() { return new Date().getFullYear(); },
    viewTitle() { return this.$lt({ ownerProjects: '我的资产', ownerNotice: '消息中心', myProperties: '首页', ownerRentalHub: '租务总览', ownerMore: '我的' }[this.currentId] || '业主服务'); }, viewHint() { return this.$lt({ ownerProjects: '名下房产、房款与资产状态', ownerNotice: '房款、租金、文件与预备金提醒', myProperties: '资产、租务与消息的快速入口', ownerRentalHub: '租金、收支、租客与合同', ownerMore: '房产资料、全部凭证与精选建案' }[this.currentId] || '业主房产查询中心'); },
    detailTitle() { return this.$lt({ payment: '房款进度', cashflow: '房产收支', tenant: '租客资讯', files: '合同与凭证', property: '房产资料' }[this.detailView] || '房产详情'); }, detailEyebrow() { return this.selectedProperty ? `${this.selectedProperty.projectName} · ${this.selectedProperty.unitNo}` : this.$lt('房产查询'); }, detailHint() { return this.$lt({ payment: '分期、已缴、待缴与付款凭证', cashflow: '年度趋势、每月统计与交易明细', tenant: '租客、合同、押金与现场照片', files: 'PDF、合同、发票、收据与转账凭证', property: '资产、银行、委租委管和房屋现况' }[this.detailView] || ''); },
    ticker() { return this.notifications.find(item => item.status !== 'read') || this.page.ownerDashboard?.notifications?.[0]; }, assetRingStyle() { const percent = this.visibleProperties.length ? Math.round(this.assetCounts.operating / this.visibleProperties.length * 100) : 0; return { '--ring-progress': `${percent}%` }; },
    totalRentPaid() { return this.rentalProperties.reduce((sum, item) => sum + Number(item.currentMonthRentPaid || 0), 0); }, totalRentDue() { return this.rentalProperties.reduce((sum, item) => sum + Number(item.currentMonthRentDue || 0), 0); }, totalRentOutstanding() { return this.rentalProperties.reduce((sum, item) => sum + Number(item.currentMonthRentOutstanding || 0), 0); }, rentCollectionPercent() { return this.totalRentDue ? Math.min(100, Math.round(this.totalRentPaid / this.totalRentDue * 100)) : 0; }, rentRingStyle() { return { '--ring-progress': `${this.rentCollectionPercent}%` }; }, totalUnitBalance() { return this.rentalProperties.reduce((sum, item) => sum + this.unitBalance(item), 0); },
    paymentSummary() { return this.paymentDetails?.summary || { purchasePrice: this.selectedProperty?.purchasePrice, paidAmount: this.selectedProperty?.paidAmount, remainingAmount: this.selectedProperty?.remainingAmount }; }, paymentInstallments() { return this.paymentDetails?.installments || []; },
    searchedProperties() { return searchOwnerProperties(this.visibleProperties, this.propertySearch, this.propertyStage); },
    searchedRentals() { return searchOwnerProperties(this.rentalProperties, this.propertySearch); },
    detailTabs() { return ['cashflow', 'tenant', 'property', 'payment', 'files']; },
    annualCashflows() { return filterCashflows(this.cashflowRecords, { year: this.cashflowYear }); },
    filteredCashflows() { return filterCashflows(this.cashflowRecords, { year: this.cashflowYear, month: this.cashflowMonth, direction: this.cashflowType, category: this.cashflowCategoryFilter, search: this.cashflowSearch }); },
    cashflowIncome() { return summarizeCashflows(this.annualCashflows).income; },
    cashflowExpense() { return summarizeCashflows(this.annualCashflows).expense; },
    cashflowBalance() { return this.cashflowIncome - this.cashflowExpense; },
    selectionTotals() { return summarizeCashflows(this.filteredCashflows); },
    monthlyBars() { const totals = groupCashflowMonths(this.cashflowRecords, this.cashflowYear); const max = Math.max(1, ...totals.flatMap(item => [item.income, item.expense])); return totals.map(item => ({ ...item, incomeHeight: item.income / max * 100, expenseHeight: item.expense / max * 100 })); },
    monthGroups() { return groupCashflowMonths(this.filteredCashflows, this.cashflowYear).filter(item => !this.cashflowMonth || item.month === Number(this.cashflowMonth)).reverse(); },
    activeLease() { const leases = this.propertyDetail.leases || []; return leases.find(item => String(item.id) === String(this.selectedLeaseId)) || leases.find(item => item.leaseNo === this.selectedProperty?.leaseNo) || leases.find(item => String(item.status).toUpperCase() === 'ACTIVE') || null; },
    relatedNoticeProperty() { return this.properties.find(item => item.projectName === this.activeNotice?.projectName && item.unitNo === this.activeNotice?.unitNo); },
    propertyDocuments() { return this.documents.filter(item => this.documentMatchesProperty(item, this.selectedProperty)); }, filteredDocuments() { return this.propertyDocuments.filter(item => matchesOwnerDocumentCategory(item, this.documentType) && (!this.documentSearch.trim() || String(item.name || '').toLocaleLowerCase().includes(this.documentSearch.trim().toLocaleLowerCase()))); }, leaseDocuments() { return this.propertyDocuments.filter(item => item.category === 'lease' || String(item.typeLabel || '').includes('租')); }, photoDocuments() { return this.propertyDocuments.filter(item => String(item.mimeType || '').startsWith('image/')); }, historyLeases() { const currentLeaseNo = this.selectedProperty?.leaseNo; return (this.propertyDetail.leases || []).filter(item => item.leaseNo !== currentLeaseNo && String(item.status || '').toUpperCase() !== 'ACTIVE'); }, detailPhotos() { const apiPhotos = (this.propertyDetail.photos || []).filter(item => item.documentId).map(item => ({ ...item, key: `property-photo-${item.id}`, id: item.documentId, name: item.title || item.fileName || '房屋现场照片', downloadable: true, stageLabel: this.photoStage(item.rentalStage) })); const seen = new Set(); return apiPhotos.filter(item => { const key = Number(item.id); if (!key || seen.has(key)) return false; seen.add(key); return true; }); }, visibleNotices() { return this.notifications.filter(item => (this.noticeCategory === 'all' || item.category === this.noticeCategory) && (!this.unreadOnly || item.status !== 'read') && (!this.noticeSearch.trim() || [item.title, item.body, item.projectName, item.unitNo].join(' ').toLocaleLowerCase().includes(this.noticeSearch.trim().toLocaleLowerCase()))); }
  },
  watch: {
    countries: { immediate: true, handler(value) { if (!value.includes(this.countryFilter) && value.length) this.countryFilter = value.includes('MY') ? 'MY' : value[0]; } },
    currentId: {
      immediate: true,
      handler(value) {
        this.detailRequest++;
        this.detailView = '';
        this.selectedProperty = null;
        if (value === 'ownerNotice') this.loadNotifications();
        this.routeLegacyModule(value);
        // All owner tabs share the document scroller. Reset after Vue replaces
        // the page so scroll anchoring cannot carry the old offset into it.
        this.$nextTick(() => {
          if (this.currentId === value) window.scrollTo({ top: 0, left: 0, behavior: 'instant' });
        });
      }
    }
  },
  mounted() { this.loadNotifications(true); },
  beforeUnmount() { this.detailRequest++; },
  methods: {
    formatDate, formatDateTime, formatMonth,
    routeLegacyModule(id) { const map = { ownerPayment: 'payment', ownerFinance: 'property', rentIncome: 'cashflow', ownerExpenses: 'cashflow', ownerReserve: 'property', ownerDocuments: 'files' }; if (!map[id]) return; const property = id === 'ownerPayment' ? this.properties.find(item => item.assetStage === 'PRE_HANDOVER') : this.properties[0]; if (property) this.openPropertyDetail(property, map[id]); },
    countryName(code) { try { return new Intl.DisplayNames([this.$i18n.locale], { type: 'region' }).of(code) || code; } catch { return code || this.$lt('其他地区'); } }, initials(value) { return String(value || 'CC').split(/\s+/).map(word => word[0]).join('').slice(0, 2).toUpperCase(); }, compactMoney(value) { return new Intl.NumberFormat(this.$i18n.locale, { notation: 'compact', maximumFractionDigits: 1 }).format(Number(value || 0)); }, amount(value) { return Number(value || 0).toLocaleString(this.$i18n.locale, { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }, money(value, currency = this.currency) { return `${currency || 'MYR'} ${this.amount(value)}`; }, signedMoney(value) { const number = Number(value || 0); return `${number >= 0 ? '+' : '-'}${this.money(Math.abs(number))}`; },  paymentPercent(property) { const total = Number(this.paymentSummary.purchasePrice ?? property?.purchasePrice ?? 0); return total ? Math.min(100, Math.max(0, Math.round(Number(this.paymentSummary.paidAmount ?? property?.paidAmount ?? 0) / total * 100))) : 0; }, unitBalance(property) { return Number(property?.monthlyIncome || 0) - Number(property?.monthlyExpense || 0); }, assetStage(value) { return this.$lt(value === 'PRE_HANDOVER' ? '预售／未交房' : value === 'OPERATING' ? '已交房' : '状态待确认'); }, installmentStatus(value) { return this.$lt({ paid: '已完成', current: '当前应缴', overdue: '已逾期', pending: '未到期' }[value] || value || '未到期'); }, leaseStatus(value) { return this.$lt({ ACTIVE: '生效中', EXPIRED: '已到期', TERMINATED: '已终止', PENDING: '待生效', SIGNED: '已签署' }[String(value || '').toUpperCase()] || value || '状态待确认'); }, mandateType(value) { return this.$lt({ RENTAL: '委租授权', MANAGEMENT: '委管授权', RENTAL_MANAGEMENT: '委租委管授权' }[String(value || '').toUpperCase()] || value || '授权委托'); }, photoStage(value) { return this.$lt({ BEFORE: '出租前', MOVE_IN: '入住时', DURING: '租赁中', MOVE_OUT: '退租后', AFTER: '退租后' }[String(value || '').toUpperCase()] || '房屋现况'); }, serviceStatus(type) { return this.$lt((this.selectedProperty?.services || []).includes(type) ? '已启用' : '尚未启用'); },
    async loadNotifications(silent = false) { if (this.notificationsLoading) return; this.notificationsLoading = !silent; try { const data = await fetchOwnerNotifications(); this.notifications = data?.notifications || []; this.notificationSummary = data?.summary || {}; this.page.ownerNotificationUnreadCount = Number(this.notificationSummary.unreadCount || 0); this.noticeTabs = ownerNoticeTabs(data?.categories || [], this.notifications.length); } catch { if (!silent) this.notifications = this.page.ownerDashboard?.notifications || []; } finally { this.notificationsLoading = false; } },
    async openNotice(item) { if (item?.id && item.status !== 'read') { try { await markOwnerNotificationRead(item.id); item.status = 'read'; this.notificationSummary.unreadCount = Math.max(0, Number(this.notificationSummary.unreadCount || 0) - 1); this.page.ownerNotificationUnreadCount = this.notificationSummary.unreadCount; } catch { /* 读取失败不阻断查看 */ } } this.activeNotice = item; await this.$nextTick(); this.$refs.noticeDialog?.showModal(); }, async readAllNotices() { try { await markAllOwnerNotificationsRead(); this.notifications.forEach(item => { item.status = 'read'; }); this.notificationSummary.unreadCount = 0; this.page.ownerNotificationUnreadCount = 0; } catch (error) { this.page.showToast(error.message || '暂时无法更新已读状态'); } },
    ownerDocumentKey, matchesOwnerDocumentCategory,
    documentMatchesProperty: matchesOwnerDocumentProperty, propertyDocumentCount(property) { return this.documents.filter(item => this.documentMatchesProperty(item, property)).length; }, findPropertyWithDocuments() { return this.visibleProperties.find(property => this.propertyDocumentCount(property) > 0); },
    async openFirstProperty(view) { if (view === 'files') { try { await this.loadDocuments(); } catch { /* 详情页会显示真实接口错误 */ } } const property = view === 'payment' ? (this.visibleProperties.find(item => item.assetStage === 'PRE_HANDOVER') || this.visibleProperties[0]) : view === 'files' ? this.findPropertyWithDocuments() || this.visibleProperties[0] : this.visibleProperties[0]; property ? this.openPropertyDetail(property, view) : this.page.showToast('目前没有可查询的房产'); }, openFirstRental(view) { this.rentalProperties[0] ? this.openPropertyDetail(this.rentalProperties[0], view) : this.page.showToast('目前没有出租中的房产'); },
    async switchDetailProperty(event) { const property = this.visibleProperties.find(item => String(item.ownerUnitId) === String(event.target.value)); if (property) await this.openPropertyDetail(property, this.detailView); },
    async openPropertyDetail(property, view) { if (String(this.selectedProperty?.ownerUnitId) !== String(property.ownerUnitId)) { this.selectedLeaseId = ''; this.resetCashflowFilters(); this.documentSearch = ''; this.documentType = 'all'; } this.countryFilter = property.countryCode || 'MY'; this.currencyFilter = property.currency || (this.countryFilter === 'TH' ? 'THB' : 'MYR'); this.selectedProperty = property; this.detailView = view; this.detailError = ''; window.scrollTo({ top: 0, behavior: 'auto' }); await this.reloadDetail(); }, closeDetail() { this.detailRequest++; this.detailView = ''; this.selectedProperty = null; this.detailError = ''; window.scrollTo({ top: 0, behavior: 'auto' }); },
    async reloadDetail() {
      if (!this.selectedProperty || !this.detailView) return;
      const request = ++this.detailRequest;
      const view = this.detailView;
      const ownerUnitId = this.selectedProperty.ownerUnitId;
      this.detailLoading = true; this.detailError = ''; this.propertyDetail = {}; this.paymentDetails = null; this.cashflowRecords = [];
      try {
        if (view === 'payment') { const data = await fetchPaymentProgress(ownerUnitId); if (request !== this.detailRequest) return; this.paymentDetails = data; }
        if (view === 'cashflow') { const data = await fetchOwnerPropertyCashflows(ownerUnitId); if (request !== this.detailRequest) return; this.cashflowRecords = data?.records || []; }
        if (['files', 'property'].includes(view)) { await this.loadDocuments(); if (request !== this.detailRequest) return; }
        if (['tenant', 'property'].includes(view)) { const data = await fetchOwnerPropertyDetail(ownerUnitId); if (request !== this.detailRequest) return; this.propertyDetail = data; }
      } catch (error) { if (request === this.detailRequest) this.detailError = error.message || '资料读取失败'; }
      finally { if (request === this.detailRequest) this.detailLoading = false; }
    },
    resetCashflowFilters() { this.cashflowMonth = ''; this.cashflowCategoryFilter = 'all'; this.cashflowType = 'all'; this.cashflowSearch = ''; },
    categoryLabel(category, direction) { return this.$t('ownerApp.categories.' + cashflowCategory(category, direction)); },
    monthLabel(month) { return new Intl.DateTimeFormat(this.$i18n.locale, { month: 'short' }).format(new Date(this.cashflowYear, month - 1, 1)); },
    async openLinkedDocument(id) {
      try {
        await this.loadDocuments();
        const file = this.documents.find(item => (item.source || 'document') === 'document' && String(item.id) === String(id));
        if (file) this.openDocument(file);
        else this.openDocument({ id, downloadable: true, name: this.$t('ownerApp.attachments') });
      } catch (error) { this.page.showToast(error.message || this.$t('ownerApp.proofUnavailable')); }
    },
    openRelatedNotice() { const property = this.relatedNoticeProperty; this.$refs.noticeDialog?.close(); if (property) this.openPropertyDetail(property, 'property'); },
    async refreshDocuments() { this.documentsLoaded = false; await this.reloadDetail(); },
    async loadDocuments() { if (this.documentsLoaded) return; const data = await fetchOwnerDocuments(); this.documents = (data?.documents || []).map(item => ({ ...item, name: decodeMimeFilename(item.name) })); this.documentsLoaded = true; }, openDocument(file) { if (!file?.downloadable) { this.page.showToast('文件记录存在，但源文件暂不可用'); return; } this.$refs.documentPreview?.open({ ...file, name: decodeMimeFilename(file.name) }); },

    exportCashflow() {
      try {
        const rows = this.filteredCashflows.map(item => ({
          [this.$t('ownerApp.date')]: item.occurredOn,
          [this.$t('ownerApp.description')]: item.description,
          [this.$t('ownerApp.category')]: this.categoryLabel(item.category, item.direction),
          [this.$t('ownerApp.direction')]: this.$t('ownerApp.' + item.direction),
          [this.$t('ownerApp.currency')]: this.currency,
          [this.$t('ownerApp.income')]: item.direction === 'income' ? Number(item.amount || 0) : 0,
          [this.$t('ownerApp.expense')]: item.direction === 'expense' ? Number(item.amount || 0) : 0,
          [this.$t('ownerApp.balance')]: item.balanceAfter ?? ''
        }));
        const sheet = XLSX.utils.json_to_sheet(rows);
        const book = XLSX.utils.book_new();
        XLSX.utils.book_append_sheet(book, sheet, this.$t('ownerApp.cashflow'));
        XLSX.writeFile(book, `${this.selectedProperty?.projectName || 'CCPS'}-${this.selectedProperty?.unitNo || ''}-${this.cashflowYear}.xlsx`);
      } catch { this.page.showToast(this.$t('ownerApp.exportFailed')); }
    }
  }
};
</script>
