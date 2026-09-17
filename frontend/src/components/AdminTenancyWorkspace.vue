<template>
  <section class="content-grid admin-tenancy-workspace">
    <div class="panel table-panel tenancy-list-panel">
      <div class="panel-head"><div><h2>{{ $t('legacy.t_d8a78b14bb2e') }}</h2><span>{{ totalRows }} {{ $t('legacy.t_86761b63a7bd') }}</span></div><span v-if="loading" class="tenancy-loading">{{ $t('legacy.t_6ce3778a43cd') }}</span></div>
      <div v-if="errorMessage" class="admin-owner-state error"><strong>{{ $t('legacy.t_f5ec447609a8') }}</strong><span>{{ $lt(errorMessage) }}</span><button @click="loadData">{{ $t('legacy.t_0c9157b5bfac') }}</button></div>
      <div v-else class="table-wrap tenancy-table-wrap">
        <table>
          <thead><tr><th>{{ $t('legacy.t_42c950a2ce7c') }}</th><th>{{ $t('legacy.t_114246450ff0') }}</th><th>{{ $t('legacy.t_bd95f007d6d0') }}</th><th>{{ $t('legacy.t_52661b02b6a2') }}</th><th>{{ $t('legacy.t_a7ff541f4978') }}</th><th>{{ $t('legacy.t_fdeda5befee2') }}</th><th>{{ $t('legacy.t_17973bd1a231') }}</th><th>{{ $t('legacy.t_84ae58f16b57') }}</th><th>{{ $t('legacy.t_53ebb183604b') }}</th><th>{{ $t('legacy.t_d71d167d09d9') }}</th><th class="tenancy-action-column">{{ $t('legacy.t_f3ea6d345e2a') }}</th></tr></thead>
          <tbody>
            <tr v-for="row in rows" :key="row.leaseId ? `lease-${row.leaseId}` : `tenant-${row.tenantId}`" class="tenancy-table-row" :class="{ selected: row === selectedRow }" @click="openTenantDetails(row)">
              <td><strong>{{ row.tenantName }}</strong><small>{{ row.phone || row.email || $t('legacy.t_33ebf8c2736b') }}</small></td>
              <td>{{ row.projectName || $t('legacy.t_bc6d72467bec') }}<small>{{ row.unitNo || '—' }}{{ row.rentalSpaceName ? ` · ${row.rentalSpaceName}` : '' }}</small></td>
              <td class="tenancy-lease-cell"><strong class="tenancy-lease-period">{{ row.leaseStart ? `${displayDate(row.leaseStart)} ~ ${displayDate(row.leaseEnd)}` : '—' }}</strong><small class="tenancy-lease-code">{{ row.leaseNo || '—' }}</small></td>
              <td><b class="tenancy-deposit">{{ money(row.depositAmount) }}</b></td>
              <td>{{ $lt(monthLabel(row.billingMonth)) }}</td>
              <td><b>{{ money(row.amountDue) }}</b></td>
              <td><span class="tenancy-paid">{{ money(row.amountPaid) }}</span><small class="tenancy-unpaid">{{ $t('legacy.t_58bb21f07dc4') }} {{ money(row.amountUnpaid) }}</small></td>
              <td><b class="money-red">{{ money(row.totalUnpaid) }}</b><small class="tenancy-unpaid">{{ $t('legacy.t_57dd08dd1420') }}</small></td>
              <td>{{ displayDate(row.dueDate) }}</td>
              <td><span class="tag" :class="rentClass(row.rentStatus)">{{ $lt(rentLabel(row.rentStatus)) }}</span></td>
              <td class="tenancy-row-action-cell"><button type="button" class="tenancy-row-action" @click.stop="openTenantDetails(row)">{{ $t('tenancy.viewDetails') }}</button></td>
            </tr>
            <tr v-if="!loading && !rows.length"><td colspan="11" class="admin-owner-empty">{{ $t('legacy.t_6d6a0646edc9') }}</td></tr>
          </tbody>
        </table>
      </div>
      <div class="pager"><span>{{ $t('legacy.t_3b6ef811b85a') }} {{ totalRows }} {{ $t('legacy.t_86761b63a7bd') }}</span><div class="admin-building-pager"><button :disabled="pageNumber <= 1" @click="goPage(pageNumber - 1)">&lt;</button><button v-for="n in visiblePages" :key="n" :class="{ active: n === pageNumber }" @click="goPage(n)">{{ n }}</button><button :disabled="pageNumber >= totalPages" @click="goPage(pageNumber + 1)">&gt;</button><select v-model.number="pageSize"><option :value="10">{{ $t('legacy.t_fc6da0e815a1') }}</option><option :value="20">{{ $t('legacy.t_93d673672fa5') }}</option><option :value="50">{{ $t('legacy.t_529930b886d2') }}</option></select></div></div>
    </div>

    <dialog ref="tenancyDetailDialog" class="tenancy-detail-dialog" @pointerdown.self="closeDialog('tenancyDetailDialog')">
      <article v-if="selectedRow" class="detail-card tenancy-detail-card">
        <header class="tenancy-detail-card-head">
          <div class="profile tenancy-profile"><div class="big-avatar">{{ initials(selectedRow.tenantName) }}</div><div class="tenancy-profile-copy"><h3>{{ selectedRow.tenantName }}</h3><p>{{ selectedRow.projectName || $t('legacy.t_bc6d72467bec') }} · {{ selectedRow.unitNo || '—' }}</p><small>{{ selectedRow.leaseNo || '—' }}</small></div><span class="tag" :class="rentClass(selectedRow.rentStatus)">{{ $lt(rentLabel(selectedRow.rentStatus)) }}</span></div>
          <button type="button" class="tenancy-detail-close" :aria-label="$t('tenancy.close')" @click="closeDialog('tenancyDetailDialog')">×</button>
        </header>
        <section v-if="page.canManageCurrentAdminModule" class="tenancy-action-toolbar" :aria-label="$t('legacy.t_f3ea6d345e2a')">
          <div class="tenancy-action-toolbar-head"><strong>{{ $t('legacy.t_f3ea6d345e2a') }}</strong><span>{{ selectedRow.leaseNo || '—' }}</span></div>
          <div class="detail-actions tenancy-actions">
            <button class="tenancy-action-primary" :disabled="selectedRow.leaseStatus !== 'active'" @click="openEditLease">{{ $t('legacy.t_f688a08a4a7e') }}</button>
            <button :disabled="selectedRow.leaseStatus !== 'active'" @click="openRenewalLease">{{ $t('processCenter.operationsRenewalButton') }}</button>
            <button :disabled="selectedRow.leaseStatus !== 'active'" @click="openTransferLease">{{ $t('legacy.t_40cd2e301851') }}</button>
            <button class="danger-action" :disabled="selectedRow.leaseStatus !== 'active'" @click="openManualClose">{{ $t('tenancy.manualTerminate') }}</button>
          </div>
        </section>
        <div class="tenancy-detail-body">
          <main class="tenancy-detail-main">
            <div class="tenancy-detail-summary">
              <div><span>{{ $t('legacy.t_4c32ed4c883c') }}</span><strong>{{ money(selectedRow.monthlyRent) }}</strong></div>
              <div><span>{{ $t('legacy.t_fdeda5befee2') }}</span><strong>{{ money(selectedRow.amountDue) }}</strong></div>
              <div><span>{{ $t('legacy.t_84ae58f16b57') }}</span><strong class="money-red">{{ money(selectedRow.totalUnpaid) }}</strong></div>
            </div>
            <div v-if="selectedRow.leaseStatus === 'active' && selectedRow.leaseEnd" class="tenancy-policy-row" :class="{ active: leaseInGracePeriod }">
              <span class="tenancy-policy-marker" aria-hidden="true"></span>
              <div class="tenancy-policy-copy"><b>{{ $t(leaseInGracePeriod ? 'tenancy.gracePeriodActive' : 'tenancy.gracePeriodPolicy') }}</b><span>{{ $t(leaseInGracePeriod ? 'tenancy.gracePeriodActiveHint' : 'tenancy.gracePeriodPolicyHint', { date: autoTerminationDate }) }}</span></div>
            </div>
            <div class="tenancy-detail-sections">
              <section class="detail-section tenancy-detail-section"><h4><span class="num">{{ $t('legacy.t_356a192b7913') }}</span>{{ $t('legacy.t_2c7d5d60a874') }}</h4><div class="kv"><span>{{ $t('legacy.t_3b08982cc350') }}</span><b>{{ selectedRow.identityNo || '—' }}</b></div><div class="kv"><span>{{ $t('legacy.t_7177787c6e84') }}</span><b>{{ selectedRow.phone || '—' }}</b></div><div class="kv"><span>{{ $t('legacy.t_84add5b29527') }}</span><b>{{ selectedRow.email || '—' }}</b></div><div class="kv"><span>{{ $t('legacy.t_45293595eae3') }}</span><b>{{ selectedRow.tenantStatus === 'active' ? $t('legacy.t_ce6c3dc32674') : $t('legacy.t_d989e55188c9') }}</b></div></section>
              <section class="detail-section tenancy-detail-section tenancy-detail-finance-section"><h4><span class="num">{{ $t('legacy.t_77de68daecd8') }}</span>{{ $t('legacy.t_edeb2ce5b99b') }}</h4><div class="kv"><span>{{ $t('legacy.t_a7ff541f4978') }}</span><b>{{ $lt(monthLabel(selectedRow.billingMonth)) }}</b></div><div class="kv"><span>{{ $t('legacy.t_60a54ee472f7') }}</span><b>{{ money(selectedRow.amountDue) }}</b></div><div class="kv"><span>{{ $t('legacy.t_109f4a1ff366') }}</span><b class="tenancy-paid">{{ money(selectedRow.amountPaid) }}</b></div><div class="kv"><span>{{ $t('legacy.t_c08daf0f9b3a') }}</span><b class="money-red">{{ money(selectedRow.amountUnpaid) }}</b></div><div class="kv"><span>{{ $t('legacy.t_84ae58f16b57') }}</span><b class="money-red">{{ money(selectedRow.totalUnpaid) }}</b></div><div class="kv"><span>{{ $t('tenancy.prepaidRentBalance') }}</span><b class="tenancy-paid">{{ money(selectedRow.prepaidRentBalance) }}</b></div><div class="tenancy-invoice-footer"><div class="progress"><i :style="{ width: rentProgress + '%' }"></i></div><button class="tenancy-invoice-link" :disabled="!selectedRow.leaseId" @click="openInvoiceDetails(selectedRow)">{{ $t('legacy.t_63e1e4b3444e') }}</button></div></section>
              <section class="detail-section tenancy-detail-section tenancy-detail-lease-section"><h4><span class="num">{{ $t('legacy.t_da4b9237bacc') }}</span>{{ $t('legacy.t_1a855f8f0f4f') }}</h4><div class="kv"><span>{{ $t('legacy.t_0c36300c2dae') }}</span><b>{{ selectedRow.rentalSpaceName || $t('legacy.t_2342e6b6433d') }}</b></div><div class="kv"><span>{{ $t('legacy.t_7275c423eae7') }}</span><b>{{ selectedRow.leaseNo || '—' }}</b></div><div class="kv"><span>{{ $t('legacy.t_87d24fa26e57') }}</span><b>{{ selectedRow.leaseStart ? `${displayDate(selectedRow.leaseStart)} ~ ${displayDate(selectedRow.leaseEnd)}` : '—' }}</b></div><div class="kv"><span>{{ $t('legacy.t_4c32ed4c883c') }}</span><b>{{ money(selectedRow.monthlyRent) }}</b></div><div class="kv"><span>{{ $t('legacy.t_986f8c25c20e') }}</span><b>{{ selectedRow.rentCalculationMethod === 'daily_prorated' ? $t('legacy.t_1e5825e68267') : $t('legacy.t_b9c6778f8e79') }}</b></div><div class="kv"><span>{{ $t('legacy.t_52661b02b6a2') }}</span><b>{{ money(selectedRow.depositAmount) }}</b></div><div class="kv"><span>{{ $t('legacy.t_59e1119803f6') }}</span><b>{{ selectedRow.paymentDay ? $t('tenancy.dayValue', { day: selectedRow.paymentDay }) : '—' }}</b></div><div class="kv"><span>{{ $t('legacy.t_56e5d9db491b') }}</span><b>{{ selectedRow.contractDocumentName || $t('legacy.t_3c5b845dce11') }}</b></div><div class="kv"><span>{{ $t('legacy.t_f365bb04b619') }}</span><b>{{ $lt(contractStatusLabel(selectedRow.contractStatus)) }}</b></div></section>
            </div>
          </main>
        </div>
      </article>
    </dialog>

    <dialog ref="currentUnpaidDialog" class="modal admin-tenancy-dialog current-unpaid-dialog">
      <div class="modal-head"><div><h3>{{ $t('tenancy.currentUnpaidDetails') }}</h3><small>{{ currentUnpaidMonth }} · {{ $t('tenancy.currentUnpaidBillCount', { count: currentUnpaidRows.length }) }}</small></div><button type="button" class="icon-close" @click="closeDialog('currentUnpaidDialog')">×</button></div>
      <div v-if="currentUnpaidLoading" class="admin-owner-empty">{{ $t('tenancy.currentUnpaidLoading') }}</div>
      <div v-else-if="currentUnpaidError" class="admin-property-error">{{ $lt(currentUnpaidError) }}</div>
      <div v-else-if="!currentUnpaidRows.length" class="admin-owner-empty">{{ $t('tenancy.currentUnpaidEmpty') }}</div>
      <template v-else>
        <div class="current-unpaid-summary"><span>{{ $t('tenancy.currentUnpaidTenantCount', { count: currentUnpaidTenantCount }) }}</span><strong>{{ $t('tenancy.currentUnpaidTotal') }} {{ money(currentUnpaidTotal) }}</strong></div>
        <div class="table-wrap current-unpaid-table-wrap"><table><thead><tr><th>{{ $t('legacy.t_42c950a2ce7c') }}</th><th>{{ $t('legacy.t_7d72a964480f') }}</th><th>{{ $t('legacy.t_308cac1bc3d6') }}</th><th>{{ $t('legacy.t_25f6c77dd124') }}</th><th>{{ $t('legacy.t_6cb2798516e3') }}</th><th>{{ $t('legacy.t_58bb21f07dc4') }}</th><th>{{ $t('legacy.t_efe272b774fe') }}</th><th>{{ $t('legacy.t_62e951a692ff') }}</th></tr></thead><tbody><tr v-for="row in currentUnpaidRows" :key="row.invoiceId"><td><strong>{{ row.tenantName }}</strong><small>{{ row.phone || row.email || '—' }}</small></td><td>{{ row.projectName || '—' }}<small>{{ row.unitNo || '—' }}{{ row.rentalSpaceName ? ` · ${row.rentalSpaceName}` : '' }}</small></td><td>{{ row.leaseNo || '—' }}</td><td>{{ money(row.amountDue) }}</td><td class="tenancy-paid">{{ money(row.amountPaid) }}</td><td class="money-red"><strong>{{ money(row.amountUnpaid) }}</strong></td><td>{{ displayDate(row.dueDate) }}</td><td><span class="tag" :class="rentClass(row.rentStatus)">{{ $lt(rentLabel(row.rentStatus)) }}</span></td></tr></tbody></table></div>
      </template>
    </dialog>

    <dialog ref="tenantDialog" class="modal admin-tenancy-dialog"><form method="dialog" @submit.prevent="submitTenant"><div class="modal-head"><div><h3>{{ $t('legacy.t_cae78ebbbaa6') }}</h3><small>{{ $t('legacy.t_478c437c8a9f') }}</small></div><button type="button" class="icon-close" @click="closeDialog('tenantDialog')">×</button></div><div class="form-grid tenancy-form"><label>{{ $t('legacy.t_235873c711c8') }}<input v-model.trim="tenantForm.fullName" required maxlength="160"></label><label>{{ $t('legacy.t_3b08982cc350') }}<input v-model.trim="tenantForm.identityNo" maxlength="120"></label><label>{{ $t('legacy.t_7177787c6e84') }}<input v-model.trim="tenantForm.phone" maxlength="40"></label><label>{{ $t('legacy.t_84add5b29527') }}<input v-model.trim="tenantForm.email" type="email" maxlength="190"></label><label>{{ $t('legacy.t_45293595eae3') }}<select v-model="tenantForm.status"><option value="active">{{ $t('legacy.t_ce6c3dc32674') }}</option><option value="inactive">{{ $t('legacy.t_d989e55188c9') }}</option></select></label><p v-if="actionError" class="admin-property-error">{{ $lt(actionError) }}</p></div><menu><button type="button" @click="closeDialog('tenantDialog')">{{ $t('legacy.t_4d0b4688c787') }}</button><button class="primary-btn" :disabled="saving">{{ saving ? $t('legacy.t_2cd5496ec548') : $t('legacy.t_30e6d69fa298') }}</button></menu></form></dialog>

    <dialog ref="leaseDialog" class="modal admin-tenancy-dialog">
      <form method="dialog" @submit.prevent="submitLease">
        <div class="modal-head"><div><h3>{{ $t('legacy.t_c6693d2dde44') }}</h3><small>{{ $t('legacy.t_fc71975fecdf') }}</small></div><button type="button" class="icon-close" @click="closeDialog('leaseDialog')">×</button></div>
        <div class="form-grid tenancy-form">
          <label>{{ $t('legacy.t_42c950a2ce7c') }}<select v-model.number="leaseForm.tenantId" required><option :value="null" disabled>{{ $t('legacy.t_6c362bcd7b18') }}</option><option v-for="t in options.tenants" :key="t.id" :value="t.id">{{ t.name }}</option></select></label>
          <label>{{ $t('legacy.t_53be6696e334') }}<select v-model.number="leaseForm.unitId" required @change="applyMandateDates"><option :value="null" disabled>{{ $t('legacy.t_e47eccacd8d5') }}</option><option v-for="u in options.units" :key="u.id" :value="u.id">{{ u.projectName }} · {{ u.unitNo }}{{ u.rentalMode === 'shared' ? $t('legacy.t_9866261f2ff3') : $t('legacy.t_af95d314418a') }}</option></select></label>
          <label>{{ $t('legacy.t_0c36300c2dae') }}<select v-model.number="leaseForm.rentalSpaceId" required><option :value="null" disabled>{{ $t('legacy.t_be63dfc8d0ab') }}</option><option v-for="space in availableLeaseSpaces" :key="space.id" :value="space.id">{{ space.spaceName }}{{ space.currentLeaseId ? $t('tenancy.occupiedUntil', { date: displayDate(space.currentLeaseEnd) }) : '' }}</option></select><small v-if="selectedLeaseUnit?.rentalMode === 'shared'">{{ $t('legacy.t_e5e7c9d0450e') }}</small></label>
          <label>{{ $t('legacy.t_7fd7a227e9ce') }}<input v-model="leaseForm.startDate" type="date" :min="selectedLeaseUnit?.mandateStartDate" :max="selectedLeaseUnit?.mandateEndDate || undefined" required></label>
          <label>{{ $t('legacy.t_27eefa5237a0') }}<input v-model="leaseForm.endDate" type="date" :min="leaseForm.startDate || selectedLeaseUnit?.mandateStartDate" :max="selectedLeaseUnit?.mandateEndDate || undefined" required></label>
          <label>{{ $t('legacy.t_2454bfeb3505') }}<input v-model.number="leaseForm.monthlyRent" type="number" min="0.01" step="0.01" required></label><label>{{ $t('legacy.t_40cf1ff4d13b') }}<input v-model.number="leaseForm.depositAmount" type="number" min="0" step="0.01" readonly><small>{{ $t('tenancy.depositAutoHint') }}</small></label><label>{{ $t('legacy.t_59e1119803f6') }}<input v-model.number="leaseForm.paymentDay" type="number" min="1" max="31" required></label><label>{{ $t('legacy.t_4611dc09098c') }}<select v-model="leaseForm.rentCalculationMethod"><option value="daily_prorated">{{ $t('legacy.t_1e5825e68267') }}</option></select></label>
          <label class="tenancy-contract-field">{{ $t('legacy.t_964d1e8f91d4') }}<input type="file" accept="application/pdf,image/jpeg,image/png" @change="selectNewLeaseContract"><small>{{ $t('legacy.t_f46a65c7a59f') }}</small></label><p v-if="!options.units.length" class="admin-property-error">{{ $t('legacy.t_526945c6d6cf') }}</p><p v-if="actionError" class="admin-property-error">{{ $lt(actionError) }}</p>
        </div>
        <menu><button type="button" @click="closeDialog('leaseDialog')">{{ $t('legacy.t_4d0b4688c787') }}</button><button class="primary-btn" :disabled="saving || !leaseForm.rentalSpaceId">{{ saving ? $t('legacy.t_2cd5496ec548') : $t('legacy.t_aced24f70328') }}</button></menu>
      </form>
    </dialog>

    <dialog ref="editLeaseDialog" class="modal admin-tenancy-dialog"><form method="dialog" @submit.prevent="submitEditLease"><div class="modal-head"><div><h3>{{ $t('legacy.t_f688a08a4a7e') }}</h3><small>{{ selectedRow?.leaseNo }} · {{ $t('tenancy.editLeaseHint') }}</small></div><button type="button" class="icon-close" @click="closeDialog('editLeaseDialog')">×</button></div><div class="form-grid tenancy-form"><label>{{ $t('legacy.t_42c950a2ce7c') }}<select v-model.number="editLeaseForm.tenantId" required><option :value="null" disabled>{{ $t('legacy.t_6c362bcd7b18') }}</option><option v-for="tenant in options.tenants" :key="tenant.id" :value="tenant.id">{{ tenant.name }}</option></select></label><label>{{ $t('legacy.t_53be6696e334') }}<select v-model.number="editLeaseForm.unitId" required><option :value="null" disabled>{{ $t('legacy.t_e47eccacd8d5') }}</option><option v-for="unit in editUnitOptions" :key="unit.id" :value="unit.id">{{ unit.projectName }} · {{ unit.unitNo }}{{ unit.current ? `（${$t('tenancy.currentUnit')}）` : '' }}</option></select></label><label>{{ $t('legacy.t_7fd7a227e9ce') }}<input v-model="editLeaseForm.startDate" type="date" required></label><label>{{ $t('legacy.t_27eefa5237a0') }}<input v-model="editLeaseForm.endDate" type="date" :min="todayDate" required></label><label>{{ $t('legacy.t_2454bfeb3505') }}<input v-model.number="editLeaseForm.monthlyRent" type="number" min="0.01" step="0.01" required></label><label>{{ $t('legacy.t_40cf1ff4d13b') }}<input v-model.number="editLeaseForm.depositAmount" type="number" min="0" step="0.01" required></label><label>{{ $t('legacy.t_59e1119803f6') }}<input v-model.number="editLeaseForm.paymentDay" type="number" min="1" max="31" required></label><label>{{ $t('legacy.t_4611dc09098c') }}<select v-model="editLeaseForm.rentCalculationMethod"><option value="monthly">{{ $t('legacy.t_b9c6778f8e79') }}</option><option value="daily_prorated">{{ $t('legacy.t_1e5825e68267') }}</option></select></label><div class="lease-change-note"><b>{{ $t('tenancy.editLeaseScopeTitle') }}</b><span>{{ $t('tenancy.editLeaseScopeHint') }}</span></div><div class="lease-change-note"><b>{{ $t('legacy.t_95d145ab0c65') }}</b><span>{{ $t('legacy.t_4e86700a46b3') }}</span></div><p v-if="actionError" class="admin-property-error">{{ $lt(actionError) }}</p></div><menu><button type="button" @click="closeDialog('editLeaseDialog')">{{ $t('legacy.t_4d0b4688c787') }}</button><button class="primary-btn" :disabled="saving || !editLeaseForm.tenantId || !editLeaseForm.unitId">{{ saving ? $t('legacy.t_8488ea2522af') : $t('legacy.t_a08367c3ad9b') }}</button></menu></form></dialog>

    <dialog ref="transferLeaseDialog" class="modal admin-tenancy-dialog"><form method="dialog" @submit.prevent="submitTransferLease"><div class="modal-head"><div><h3>{{ $t('legacy.t_40cd2e301851') }}</h3><small>{{ selectedRow?.projectName }} · {{ selectedRow?.unitNo }}</small></div><button type="button" class="icon-close" @click="closeDialog('transferLeaseDialog')">×</button></div><div class="form-grid tenancy-form"><label>{{ $t('legacy.t_3be587f9d5f3') }}<input :value="selectedRow?.tenantName" disabled></label><label>{{ $t('legacy.t_1d18a8d9157c') }}<select v-model.number="transferLeaseForm.newTenantId" required><option :value="null" disabled>{{ $t('legacy.t_d58d5216e6aa') }}</option><option v-for="tenant in transferTenantOptions" :key="tenant.id" :value="tenant.id">{{ tenant.name }}</option></select></label><label>{{ $t('legacy.t_48631b2f5f6d') }}<input v-model="transferLeaseForm.transferDate" type="date" :min="transferMinDate" :max="selectedRow?.leaseEnd" required></label><label>{{ $t('legacy.t_4f265263cb6b') }}<input v-model="transferLeaseForm.endDate" type="date" :min="transferLeaseForm.transferDate" required></label><label>{{ $t('legacy.t_3dd7ca3f56b6') }}<input v-model.number="transferLeaseForm.monthlyRent" type="number" min="0.01" step="0.01" required></label><label>{{ $t('legacy.t_c6e11bda27b7') }}<input v-model.number="transferLeaseForm.depositAmount" type="number" min="0" step="0.01" required></label><label>{{ $t('legacy.t_59e1119803f6') }}<input v-model.number="transferLeaseForm.paymentDay" type="number" min="1" max="31" required></label><label>{{ $t('legacy.t_4611dc09098c') }}<select v-model="transferLeaseForm.rentCalculationMethod"><option value="monthly">{{ $t('legacy.t_b9c6778f8e79') }}</option><option value="daily_prorated">{{ $t('legacy.t_1e5825e68267') }}</option></select></label><label class="tenancy-contract-field">{{ $t('legacy.t_19faa1d83b11') }}<input type="file" accept="application/pdf,image/jpeg,image/png" @change="selectTransferContract"><small>{{ $t('legacy.t_d459e4c4dfb1') }}</small></label><div class="lease-transfer-warning"><b>{{ $t('legacy.t_787486055d66') }}</b><span>{{ $t('legacy.t_9ea4c2af4433') }}</span></div><p v-if="!transferTenantOptions.length" class="admin-property-error">{{ $t('legacy.t_d0d5dd05c114') }}</p><p v-if="actionError" class="admin-property-error">{{ $lt(actionError) }}</p></div><menu><button type="button" @click="closeDialog('transferLeaseDialog')">{{ $t('legacy.t_4d0b4688c787') }}</button><button class="primary-btn" :disabled="saving || !transferTenantOptions.length">{{ saving ? $t('legacy.t_e42afa900cb7') : $t('legacy.t_a9e87ad0d750') }}</button></menu></form></dialog>

    <dialog ref="invoiceDetailsDialog" class="modal admin-tenancy-dialog tenancy-invoice-dialog"><div class="modal-head"><div><h3>{{ $t('legacy.t_6c6010c77d79') }}</h3><small>{{ invoiceDetailTitle }} {{ $t('legacy.t_6cc98689cde4') }}</small></div><button type="button" class="icon-close" @click="closeDialog('invoiceDetailsDialog')">×</button></div><div v-if="invoiceDetailsLoading" class="admin-owner-empty">{{ $t('legacy.t_46cbac9efcc4') }}</div><div v-else-if="invoiceDetailsError" class="admin-property-error">{{ $lt(invoiceDetailsError) }}</div><div v-else class="table-wrap"><table><thead><tr><th>{{ $t('legacy.t_a7ff541f4978') }}</th><th>{{ $t('legacy.t_11cb5e87977f') }}</th><th>{{ $t('legacy.t_60a54ee472f7') }}</th><th>{{ $t('legacy.t_6cb2798516e3') }}</th><th>{{ $t('legacy.t_58bb21f07dc4') }}</th><th>{{ $t('legacy.t_45293595eae3') }}</th></tr></thead><tbody><tr v-for="invoice in invoiceDetails" :key="invoice.invoiceId"><td>{{ $lt(monthLabel(invoice.billingMonth)) }}</td><td>{{ displayDate(invoice.dueDate) }}</td><td>{{ money(invoice.amountDue) }}</td><td class="tenancy-paid">{{ money(invoice.amountPaid) }}</td><td class="money-red">{{ money(invoice.amountUnpaid) }}</td><td><span class="tag" :class="rentClass(invoice.rentStatus)">{{ $lt(rentLabel(invoice.rentStatus)) }}</span></td></tr><tr v-if="!invoiceDetails.length"><td colspan="6" class="admin-owner-empty">{{ $t('legacy.t_6b0cbf3fea8e') }}</td></tr></tbody></table></div><menu><button type="button" @click="closeDialog('invoiceDetailsDialog')">{{ $t('legacy.t_ddc05404b0d6') }}</button></menu></dialog>
    <dialog ref="templateDialog" class="modal admin-tenancy-dialog contract-template-dialog">
      <form method="dialog" @submit.prevent="generateTemplate">
        <div class="modal-head"><div><h3>{{ templateForm.templateType === 'tenancy-agreement' ? $t('tenancy.generateTenancyAgreementTitle') : $t('tenancy.generateOtrTitle') }}</h3><small>{{ $t('tenancy.templateHint') }}</small></div><button type="button" class="icon-close" @click="closeDialog('templateDialog')">×</button></div>
        <div class="form-grid tenancy-form">
          <label>{{ $t('tenancy.caseNo') }}<input v-model.trim="templateForm.fields.caseNo" required></label>
          <label>{{ $t('tenancy.propertyAddress') }}<input v-model.trim="templateForm.fields.propertyAddress" required></label>
          <label>{{ $t('tenancy.ownerName') }}<input v-model.trim="templateForm.fields.landlordName" :required="templateForm.templateType === 'otr'"></label>
          <label>{{ $t('tenancy.ownerIdentity') }}<input v-model.trim="templateForm.fields.landlordIdentity" :required="templateForm.templateType === 'otr'"></label>
          <label v-if="templateForm.templateType === 'tenancy-agreement'">{{ $t('tenancy.ownerAddress') }}<input v-model.trim="templateForm.fields.landlordAddress"></label>
          <label v-if="templateForm.templateType === 'otr'">{{ $t('tenancy.tenantName') }}<input v-model.trim="templateForm.fields.tenantName" required></label>
          <label v-if="templateForm.templateType === 'otr'">{{ $t('tenancy.tenantIdentity') }}<input v-model.trim="templateForm.fields.tenantIdentity" required></label>
          <label v-if="templateForm.templateType === 'tenancy-agreement'">{{ $t('tenancy.tenantName') }}<input v-model.trim="templateForm.fields.tenantName" required></label>
          <label v-if="templateForm.templateType === 'tenancy-agreement'">{{ $t('tenancy.tenantIdentity') }}<input v-model.trim="templateForm.fields.tenantIdentity"></label>
          <label v-if="templateForm.templateType === 'tenancy-agreement'">{{ $t('tenancy.tenantPhone') }}<input v-model.trim="templateForm.fields.tenantPhone"></label>
          <label v-if="templateForm.templateType === 'tenancy-agreement'">{{ $t('tenancy.tenantAddress') }}<input v-model.trim="templateForm.fields.tenantAddress"></label>
          <label v-if="templateForm.templateType === 'tenancy-agreement'">{{ $t('tenancy.tenantEmail') }}<input v-model.trim="templateForm.fields.tenantEmail" type="email"></label>
          <label v-if="templateForm.templateType === 'otr'">{{ $t('tenancy.rentRm') }}<input v-model.trim="templateForm.fields.advanceRental" required></label>
          <label v-if="templateForm.templateType === 'tenancy-agreement'">{{ $t('tenancy.agreementDate') }}<input v-model.trim="templateForm.fields.agreementDate" type="date"></label>
          <label v-if="templateForm.templateType === 'tenancy-agreement'">{{ $t('tenancy.rentRm') }}<input v-model.trim="templateForm.fields.monthlyRent"></label>
          <label v-if="templateForm.templateType === 'otr'">{{ $t('tenancy.leaseStartDate') }}<input v-model.trim="templateForm.fields.commencementDate" type="date" required></label>
          <label v-if="templateForm.templateType === 'tenancy-agreement'">{{ $t('tenancy.leaseStartDate') }}<input v-model.trim="templateForm.fields.leaseStart" type="date"></label>
          <label v-if="templateForm.templateType === 'tenancy-agreement'">{{ $t('tenancy.leaseEndDate') }}<input v-model.trim="templateForm.fields.leaseEnd" type="date"></label>
          <label v-if="templateForm.templateType === 'tenancy-agreement'">{{ $t('tenancy.leaseYears') }}<input v-model.trim="templateForm.fields.termYears"></label>
          <label v-if="templateForm.templateType === 'tenancy-agreement'">{{ $t('tenancy.paymentDay') }}<input v-model.trim="templateForm.fields.paymentDay" type="number" min="1" max="31"></label>
          <label v-if="templateForm.templateType === 'tenancy-agreement'">{{ $t('tenancy.advanceRental') }}<input v-model.trim="templateForm.fields.advanceRental"></label>
          <label v-if="templateForm.templateType === 'tenancy-agreement'">{{ $t('tenancy.securityDeposit') }}<input v-model.trim="templateForm.fields.securityDeposit"></label>
          <label v-if="templateForm.templateType === 'tenancy-agreement'">{{ $t('tenancy.utilityDeposit') }}<input v-model.trim="templateForm.fields.utilityDeposit"></label>
          <label v-if="templateForm.templateType === 'tenancy-agreement'">{{ $t('tenancy.bankName') }}<input v-model.trim="templateForm.fields.bankName"></label>
          <label v-if="templateForm.templateType === 'tenancy-agreement'">{{ $t('tenancy.bankAccount') }}<input v-model.trim="templateForm.fields.bankAccount"></label>
          <label v-if="templateForm.templateType === 'tenancy-agreement'">{{ $t('tenancy.bankBranch') }}<input v-model.trim="templateForm.fields.bankBranch"></label>
          <label v-if="templateForm.templateType === 'tenancy-agreement'">{{ $t('tenancy.renewalOption') }}<input v-model.trim="templateForm.fields.renewalOption"></label>
          <label v-if="templateForm.templateType === 'tenancy-agreement'">{{ $t('tenancy.useOfPremises') }}<input v-model.trim="templateForm.fields.use"></label>
          <label v-if="templateForm.templateType === 'otr'">{{ $t('tenancy.leaseYears') }}<input v-model.trim="templateForm.fields.periodYears" required></label>
          <label v-if="templateForm.templateType === 'otr'">{{ $t('legacy.t_1dbeef29aa0d') }}<input v-model.trim="templateForm.fields.renewalYears" required></label>
          <label v-if="templateForm.templateType === 'otr'">{{ $t('tenancy.depositMonths') }}<input v-model.trim="templateForm.fields.securityDepositMonths" required></label>
          <label v-if="templateForm.templateType === 'otr'">{{ $t('tenancy.depositRm') }}<input v-model.trim="templateForm.fields.securityDeposit" required></label>
          <label v-if="templateForm.templateType === 'otr'">{{ $t('tenancy.utilityDepositMonths') }}<input v-model.trim="templateForm.fields.utilityDepositMonths" required></label>
          <label v-if="templateForm.templateType === 'otr'">{{ $t('tenancy.utilityDepositRm') }}<input v-model.trim="templateForm.fields.utilityDeposit" required></label>
          <label v-if="templateForm.templateType === 'otr'">{{ $t('tenancy.stampingFeesRm') }}<input v-model.trim="templateForm.fields.stampingFee" required></label>
          <label v-if="templateForm.templateType === 'otr'">{{ $t('tenancy.totalBeforeKeysRm') }}<input v-model.trim="templateForm.fields.totalBeforeKeys" required></label>
          <label>{{ $t('tenancy.earnestDepositRm') }}<input v-model.trim="templateForm.fields.earnestDeposit" :required="templateForm.templateType === 'otr'"></label>
          <template v-if="templateForm.templateType === 'otr'">
            <label>{{ $t('legacy.t_aec3a28562db') }}<input v-model.trim="templateForm.fields.tenantDate" type="date" required></label>
            <label>{{ $t('legacy.t_8f36120e808c') }}<input v-model.trim="templateForm.fields.landlordDate" type="date" required></label>
            <label>{{ $t('legacy.t_b6d8b6192a98') }}<input v-model.trim="templateForm.fields.tenantWitnessName" required></label>
            <label>{{ $t('legacy.t_a268aa195980') }}<input v-model.trim="templateForm.fields.tenantWitnessIdentity" required></label>
            <label>{{ $t('legacy.t_0044c77d01de') }}<input v-model.trim="templateForm.fields.tenantWitnessDate" type="date" required></label>
            <label>{{ $t('legacy.t_db2f8699d1b1') }}<input v-model.trim="templateForm.fields.landlordWitnessName" required></label>
            <label>{{ $t('legacy.t_1fde9e803301') }}<input v-model.trim="templateForm.fields.landlordWitnessIdentity" required></label>
            <label>{{ $t('legacy.t_9c4fbd931d11') }}<input v-model.trim="templateForm.fields.landlordWitnessDate" type="date" required></label>
          </template>
          <label class="wide">{{ templateForm.templateType === 'tenancy-agreement' ? $t('tenancy.specialConditions') : $t('tenancy.otherConditions') }}<input v-model.trim="templateForm.fields[templateForm.templateType === 'tenancy-agreement' ? 'specialConditions' : 'otherConditions']" :required="templateForm.templateType === 'otr'"></label>
          <p v-if="templateError" class="admin-property-error">{{ $lt(templateError) }}</p>
        </div>
        <menu><button type="button" @click="closeDialog('templateDialog')">{{ $t('tenancy.cancel') }}</button><button class="primary-btn" :disabled="templateBusy">{{ templateBusy ? $t('tenancy.generating') : (templateForm.signingMode ? $t('tenancy.generateContinueSigning') : $t('tenancy.generateDownloadPdf')) }}</button></menu>
      </form>
    </dialog>
    <dialog ref="leaseSigningDialog" class="modal admin-tenancy-dialog">
      <form method="dialog" @submit.prevent="startLeaseSigning">
        <div class="modal-head"><div><h3>{{ $t('legacy.t_d533a7b75659') }}</h3><small>{{ signatureLinks.length ? $t('rentalFiles.signingLinkDirectHint') : $t('rentalFiles.signerPackageHint') }}</small></div><button type="button" class="icon-close" @click="closeDialog('leaseSigningDialog')">×</button></div>
        <div v-if="signatureLinks.length" class="tenancy-signing-links">
          <div class="tenancy-signing-summary"><span>{{ $t('legacy.t_356a192b7913') }}</span><div><strong>{{ $t('rentalFiles.signingLinksReady') }}</strong><small>{{ $t('rentalFiles.signingLinkDirectHint') }}</small></div></div>
          <article v-for="link in signatureLinks" :key="link.requestId || link.signingUrl"><div><strong>{{ $lt(leaseSignerRoleLabel(link.signerRole)) }} · {{ link.signerName }}</strong><small>{{ $t('rentalFiles.emailOnlyWhenSending') }}</small></div><label><span>{{ $t('rentalFiles.signingLink') }}</span><input :value="link.signingUrl" readonly></label><label><span>{{ $t('rentalFiles.deliveryEmail') }}</span><input v-model.trim="link.deliveryEmail" type="email" maxlength="190" :placeholder="$t('rentalFiles.deliveryEmailHint')" :disabled="signatureEmailBusyId === link.requestId"></label><section><button type="button" :disabled="signatureEmailBusyId === link.requestId" :aria-busy="signatureEmailBusyId === link.requestId" :data-state="signatureEmailSentIds.includes(link.requestId) ? 'success' : signatureEmailBusyId === link.requestId ? 'loading' : 'default'" @click="sendLeaseSigningEmail(link)">{{ signatureEmailSentIds.includes(link.requestId) ? $t('rentalFiles.emailSent') : signatureEmailBusyId === link.requestId ? $t('rentalFiles.sendingEmail') : $t('rentalFiles.sendByEmail') }}</button><a :href="whatsAppLeaseSigningUrl(link)" target="_blank" rel="noopener noreferrer">{{ $t('rentalFiles.shareByWhatsApp') }}</a><button type="button" @click="copyLeaseSigningLink(link.signingUrl)">{{ $t('rentalFiles.copyLink') }}</button><a :href="link.signingUrl" target="_blank" rel="noopener noreferrer">{{ $t('rentalFiles.openSigningLink') }}</a></section></article>
          <p v-if="signatureDeliveryError" class="admin-property-error" role="alert">{{ $lt(signatureDeliveryError) }}</p>
        </div>
        <div v-else class="form-grid tenancy-form"><section v-for="signer in signatureForm.signers" :key="signer.signerRole"><strong>{{ $lt(leaseSignerRoleLabel(signer.signerRole)) }}</strong><label>{{ $t('legacy.t_a6c87526ef43') }}<input v-model.trim="signer.signerName" required maxlength="190"></label></section><label>{{ $t('legacy.t_1f156a7776ee') }}<input v-model.number="signatureForm.expiresInDays" required type="number" min="1" max="30"></label><p v-if="actionError" class="admin-property-error">{{ $lt(actionError) }}</p></div>
        <menu><button type="button" @click="closeDialog('leaseSigningDialog')">{{ signatureLinks.length ? $t('rentalFiles.done') : $t('legacy.t_4d0b4688c787') }}</button><button v-if="!signatureLinks.length" class="primary-btn" :disabled="signatureBusy">{{ signatureBusy ? $t('legacy.t_45c36a4dc86c') : $t('rentalFiles.startSigningPackage') }}</button></menu>
      </form>
    </dialog>

    <dialog ref="renewalLeaseDialog" class="modal admin-tenancy-dialog">
      <form method="dialog" @submit.prevent="submitRenewalLease">
        <div class="modal-head"><div><h3>{{ $t('processCenter.operationsRenewalTitle') }}</h3><small>{{ selectedRow?.tenantName }} · {{ selectedRow?.projectName }} {{ selectedRow?.unitNo }}</small></div><button type="button" class="icon-close" @click="closeDialog('renewalLeaseDialog')">×</button></div>
        <div class="form-grid tenancy-form">
          <p class="renewal-lease-hint">{{ $t('legacy.t_faa1a31cf4e9') }}</p>
<section v-if="renewalPeriods.length" class="renewal-period-history wide"><strong>{{ $t('legacy.t_80b21f41ce78') }}</strong><div v-for="period in renewalPeriods" :key="period.id" class="renewal-period-item"><span>{{ $t('legacy.t_dae828fe4fb7') }} {{ period.periodNo }} {{ $t('legacy.t_fc73601f2012') }}</span><span>{{ displayDate(period.startDate) }} {{ $t('legacy.t_43401e739ef4') }} {{ displayDate(period.endDate) }}</span><span>{{ $t('legacy.t_5e7b60c626a4') }} {{ Number(period.monthlyRent || 0).toFixed(2) }}{{ $t('legacy.t_7a3bee7c87ff') }}</span><button v-if="period.contractDocumentId" type="button" @click="downloadRenewalPeriod(period)">{{ $t('legacy.t_c51a011e7b2e') }}</button><em v-else>{{ period.periodNo === 1 ? $t('legacy.t_30f649d0e9c6') : $t('legacy.t_235fdf5b3eb9') }}</em></div></section>
          <label>{{ $t('processCenter.operationsRenewalStartDate') }}<input v-model="renewalLeaseForm.startDate" type="date" :min="renewalMinDate" readonly required></label>
          <label>{{ $t('processCenter.operationsRenewalEndDate') }}<input v-model="renewalLeaseForm.endDate" type="date" :min="renewalMinDate" required></label>
          <label>{{ $t('processCenter.operationsRenewalMonthlyRent') }}<input v-model.number="renewalLeaseForm.monthlyRent" type="number" min="0.01" step="0.01" required></label>
          <label>{{ $t('processCenter.operationsRenewalDeposit') }}<input v-model.number="renewalLeaseForm.depositAmount" type="number" min="0" step="0.01" required><small class="renewal-deposit-hint">{{ $t('tenancy.renewalDepositIncreaseHint', { current: money(selectedRow?.depositAmount) }) }}</small></label>
          <label>{{ $t('processCenter.operationsRenewalPaymentDay') }}<input v-model.number="renewalLeaseForm.paymentDay" type="number" min="1" max="31" required></label>
          <label>{{ $t('processCenter.operationsRenewalCalculationMethod') }}<select v-model="renewalLeaseForm.rentCalculationMethod"><option value="daily_prorated">{{ $t('legacy.t_1e5825e68267') }}</option></select></label>
          <label class="tenancy-contract-field">{{ $t('legacy.t_201145bfe365') }}<input type="file" accept="application/pdf,image/jpeg,image/png" @change="selectRenewalContract"><small>{{ $t('legacy.t_8dd12431ae06') }}</small></label>
          <p v-if="renewalError" class="admin-property-error">{{ $lt(renewalError) }}</p>
        </div>
        <menu><button type="button" @click="closeDialog('renewalLeaseDialog')">{{ $t('legacy.t_4d0b4688c787') }}</button><button class="primary-btn" :disabled="renewalSaving">{{ renewalSaving ? $t('legacy.t_2cd5496ec548') : $t('processCenter.operationsRenewalSubmit') }}</button></menu>
      </form>
    </dialog>

    <dialog ref="closeLeaseDialog" class="modal admin-tenancy-dialog">
      <form method="dialog" @submit.prevent="submitManualClose">
        <div class="modal-head"><div><h3>{{ $t('tenancy.manualTerminateTitle') }}</h3><small>{{ selectedRow?.leaseNo }} · {{ selectedRow?.tenantName }}</small></div><button type="button" class="icon-close" @click="closeDialog('closeLeaseDialog')">×</button></div>
        <div class="form-grid tenancy-form">
          <label>{{ $t('tenancy.manualTerminateDate') }}<input v-model="closeLeaseForm.endDate" type="date" :min="selectedRow?.leaseStart" :max="todayDate" required></label>
          <label>{{ $t('tenancy.manualTerminateReason') }}<select v-model="closeLeaseForm.reason" required><option value="normal_expiry">{{ $t('tenancy.manualTerminateNormal') }}</option><option value="early_termination">{{ $t('tenancy.manualTerminateEarly') }}</option></select></label>
          <label class="wide">{{ $t('tenancy.manualTerminateNotes') }}<textarea v-model.trim="closeLeaseForm.notes" rows="3"></textarea></label>
          <div class="lease-change-note"><b>{{ $t('tenancy.manualTerminateWarningTitle') }}</b><span>{{ $t('tenancy.manualTerminateWarning') }}</span></div>
          <p v-if="actionError" class="admin-property-error">{{ $lt(actionError) }}</p>
        </div>
        <menu><button type="button" @click="closeDialog('closeLeaseDialog')">{{ $t('legacy.t_4d0b4688c787') }}</button><button class="primary-btn danger-action" :disabled="saving">{{ saving ? $t('legacy.t_2cd5496ec548') : $t('tenancy.manualTerminateSubmit') }}</button></menu>
      </form>
    </dialog>
  </section>
</template>

<script>
import { closeAdminLease, createAdminLease, createAdminLeaseRenewal, createAdminTenant, fetchAdminLeasePeriods, fetchAdminLeaseRenewalContract, fetchAdminLeaseRentInvoices, fetchAdminLeaseSignatureParticipants, fetchAdminPropertyHandover, fetchAdminTenancy, fetchAdminTenancyOptions, generateAdminContractTemplate, sendAdminElectronicSignatureInvitation, startAdminLeaseSignaturePackage, transferAdminLease, updateAdminLease, uploadAdminGeneratedLeaseContract, uploadAdminLeaseContract, uploadAdminLeaseRenewalContract } from '../services/propertyApi';
import { calculateTenantDeposit } from '../utils/tenantDeposit';
import { formatDate, formatMonth } from '../utils/dateFormat';
const today = () => new Date().toISOString().slice(0, 10);
const nextYear = () => { const date = new Date(); date.setFullYear(date.getFullYear() + 1); return date.toISOString().slice(0, 10); };
const plusDays = (value, days) => { const [year, month, day] = String(value || '').slice(0, 10).split('-').map(Number); if (!year || !month || !day) return ''; const date = new Date(Date.UTC(year, month - 1, day)); date.setUTCDate(date.getUTCDate() + days); return date.toISOString().slice(0, 10); };
const plusCalendarMonths = (value, months) => { const [year, month, day] = String(value || '').slice(0, 10).split('-').map(Number); if (!year || !month || !day) return ''; const index = month - 1 + months; const targetYear = year + Math.floor(index / 12); const targetMonth = ((index % 12) + 12) % 12; const targetDay = Math.min(day, new Date(Date.UTC(targetYear, targetMonth + 1, 0)).getUTCDate()); return `${targetYear}-${String(targetMonth + 1).padStart(2, '0')}-${String(targetDay).padStart(2, '0')}`; };
export default {
  inject: ['page'],
  data() { return { rows: [], selectedId: null, selectedLeaseId: null, selectedInvoiceId: null, invoiceDetails: [], invoiceDetailsLoading: false, invoiceDetailsError: '', invoiceDetailTitle: '', currentUnpaidRows: [], currentUnpaidLoading: false, currentUnpaidError: '', totalRows: 0, totalPages: 1, pageNumber: 1, pageSize: 10, loading: false, errorMessage: '', saving: false, signatureBusy: false, signatureLinks: [], signatureEmailBusyId: null, signatureEmailSentIds: [], signatureDeliveryError: '', templateBusy: false, templateError: '', templateForm: { templateType: 'otr', signingMode: false, fields: {} }, signatureForm: { signers: [], expiresInDays: 7 }, leaseContractFile: null, transferContractFile: null, actionError: '', requestSerial: 0, todayDate: today(), options: { projects: [], tenants: [], units: [] }, tenantForm: { fullName: '', identityNo: '', phone: '', email: '', status: 'active' }, leaseForm: { tenantId: null, unitId: null, rentalMandateId: null, startDate: today(), endDate: nextYear(), monthlyRent: 0, depositAmount: 0, paymentDay: 1, rentCalculationMethod: 'monthly' }, editLeaseForm: { startDate: '', endDate: '', monthlyRent: 0, depositAmount: 0, paymentDay: 1, rentCalculationMethod: 'monthly' }, transferLeaseForm: { newTenantId: null, transferDate: today(), endDate: '', monthlyRent: 0, depositAmount: 0, paymentDay: 1, rentCalculationMethod: 'monthly' }, closeLeaseForm: { endDate: today(), reason: 'early_termination', notes: '' }, renewalLeaseForm: { startDate: '', endDate: '', monthlyRent: 0, depositAmount: 0, paymentDay: 1, rentCalculationMethod: 'daily_prorated' }, renewalPeriods: [], renewalContractFile: null, renewalSaving: false, renewalError: '' }; },
  computed: {
    selectedRow() { return this.rows.find(row => row.leaseId && row.leaseId === this.selectedLeaseId) || this.rows.find(row => !row.leaseId && row.tenantId === this.selectedId) || this.rows[0] || null; },
    visiblePages() { const start = Math.max(1, Math.min(this.pageNumber - 2, this.totalPages - 4)); return Array.from({ length: Math.min(5, this.totalPages) }, (_, i) => start + i); },
    rentProgress() { return !Number(this.selectedRow?.amountDue) ? 0 : Math.min(100, Math.round(Number(this.selectedRow.amountPaid || 0) / Number(this.selectedRow.amountDue) * 100)); },
    autoTerminationDate() { return plusCalendarMonths(this.selectedRow?.leaseEnd, 2); },
    leaseInGracePeriod() { return this.selectedRow?.leaseStatus === 'active' && Boolean(this.selectedRow?.leaseEnd) && this.selectedRow.leaseEnd < this.todayDate; },
    tenantNonce() { return this.page.adminTenantCreateNonce; }, leaseNonce() { return this.page.adminLeaseCreateNonce; },
    transferTenantOptions() { return (this.options.tenants || []).filter(tenant => tenant.id !== this.selectedRow?.tenantId); },
    editUnitOptions() {
      const current = this.selectedRow?.unitId ? {
        id: this.selectedRow.unitId,
        projectName: this.selectedRow.projectName,
        unitNo: this.selectedRow.unitNo,
        current: true
      } : null;
      const units = [...(this.options.units || [])];
      if (current && !units.some(unit => Number(unit.id) === Number(current.id))) units.unshift(current);
      return units;
    },
    selectedLeaseUnit() { return (this.options.units || []).find(unit => unit.id === this.leaseForm.unitId) || null; },
    availableLeaseSpaces() {
      const unit = this.selectedLeaseUnit;
      if (!unit) return [];
      return (this.options.rentalSpaces || []).filter(space => Number(space.unitId) === Number(unit.id)
        && (unit.rentalMode === 'shared' ? space.spaceType === 'room' : space.spaceType === 'whole_unit'));
    },
    transferMinDate() { if (!this.selectedRow?.leaseStart) return this.todayDate; return [this.todayDate, plusDays(this.selectedRow.leaseStart, 1)].sort().at(-1); },
    renewalMinDate() { if (!this.selectedRow?.leaseEnd) return this.todayDate; return plusDays(this.selectedRow.leaseEnd, 1); },
    currentUnpaidMonth() { return this.todayDate.slice(0, 7); },
    currentUnpaidTotal() { return this.currentUnpaidRows.reduce((sum, row) => sum + Number(row.amountUnpaid || 0), 0); },
    currentUnpaidTenantCount() { return new Set(this.currentUnpaidRows.map(row => row.tenantId)).size; }
  },
  watch: {
    'leaseForm.monthlyRent'(value) { this.leaseForm.depositAmount = calculateTenantDeposit(value); },
    'page.moduleSearch'() { this.resetLoad(); }, 'page.globalSearch'() { this.resetLoad(); }, 'page.projectFilter'() { this.resetLoad(); }, 'page.statusFilter'() { this.resetLoad(); }, 'page.dateStart'() { this.resetLoad(); }, 'page.dateEnd'() { this.resetLoad(); },
    pageSize() { this.pageNumber = 1; this.loadData(); }, tenantNonce(v, old) { if (v > old) this.openTenant(); }, leaseNonce(v, old) { if (v > old) this.openLease(); },
    rows: { deep: true, handler(rows) { const current = rows.find(row => row.leaseId && row.leaseId === this.selectedLeaseId) || rows.find(row => !row.leaseId && row.tenantId === this.selectedId) || rows[0] || null; this.selectedId = current?.tenantId || null; this.selectedLeaseId = current?.leaseId || null; this.selectedInvoiceId = current?.invoiceId || null; } }
  },
   mounted() { this.page.projectFilter = this.$t('ui.allProjects'); this.page.statusFilter = this.$t('ui.allStatus'); window.addEventListener('admin-tenancy-current-unpaid-details', this.openCurrentUnpaidDetails); this.loadData().then(() => this.applyProcessRoute()); },
  beforeUnmount() { window.removeEventListener('admin-tenancy-current-unpaid-details', this.openCurrentUnpaidDetails); },
  methods: {
    calculateTenantDeposit,
     async applyProcessRoute() { const params = new URLSearchParams(window.location.search); const workflow = params.get('workflow'); const unitId = params.get('unitId'); if (workflow === 'tenant-create') { this.openTenant(); return; } if (workflow === 'lease-create') { this.openLease(); if (unitId) { this.leaseForm.unitId = Number(unitId); this.leaseForm.rentalMandateId = params.get('mandateId') ? Number(params.get('mandateId')) : null; this.applyMandateDates(); } return; } const row = this.rows.find(item => String(item.unitId) === String(unitId)); if (!row) return; this.selectRow(row); if (workflow === 'otr') this.openTemplateGenerator('otr'); if (workflow === 'lease-contract') this.openTemplateGenerator('tenancy-agreement', true); if (workflow === 'invoice') await this.openInvoiceDetails(row); },
     async loadData() { const serial = ++this.requestSerial; this.loading = true; this.errorMessage = ''; try { const [response, options] = await Promise.all([fetchAdminTenancy({ page: this.pageNumber, pageSize: this.pageSize, keyword: this.page.globalSearch || this.page.moduleSearch || '', projectName: this.page.projectFilter === this.$t('ui.allProjects') ? '' : this.page.projectFilter, status: this.statusParam(this.page.statusFilter), startDate: this.page.dateStart, endDate: this.page.dateEnd }), fetchAdminTenancyOptions()]); if (serial !== this.requestSerial) return; this.rows = response.rows || []; this.totalRows = response.page?.totalRows || 0; this.totalPages = response.page?.totalPages || 1; this.pageNumber = response.page?.page || 1; this.options = options || { projects: [], tenants: [], units: [] }; this.page.adminTenancyProjects = this.options.projects || []; this.page.adminTenancyMetrics = this.metrics(response.summary || {}); this.page.adminTenancyExportHeaders = ['exportTenant', 'exportProject', 'exportUnit', 'exportLeaseNo', 'exportLeaseStart', 'exportLeaseEnd', 'exportMonthlyRent', 'exportBillingMonth', 'exportAmountDue', 'exportAmountPaid', 'exportAmountUnpaid', 'exportRentStatus'].map(key => this.$t(`tenancy.${key}`)); this.page.adminTenancyExportRows = this.rows.map(row => [row.tenantName, row.projectName || '', row.unitNo || '', row.leaseNo || '', this.displayDate(row.leaseStart, ''), this.displayDate(row.leaseEnd, ''), row.monthlyRent, row.billingMonth ? this.monthLabel(row.billingMonth) : '', row.amountDue, row.amountPaid, row.amountUnpaid, this.rentLabel(row.rentStatus)]); } catch (e) { if (serial !== this.requestSerial) return; this.rows = []; this.errorMessage = e.message || 'API request failed'; this.page.adminTenancyMetrics = null; this.page.adminTenancyExportRows = []; } finally { if (serial === this.requestSerial) this.loading = false; } },
    metrics(s) { const t = (key, params) => this.$t(`tenancy.${key}`, params); return [{ icon: t('tenantMetricIcon'), label: t('totalTenants'), value: t('tenantCountValue', { count: Number(s.tenantCount || 0) }), delta: t('activeLeaseValue', { count: Number(s.activeLeaseCount || 0) }), trend: 'up' }, { icon: t('rentMetricIcon'), label: t('currentDueRent'), value: this.money(s.currentDue), delta: t('liveCalculation'), trend: 'up' }, { icon: '✓', label: t('currentPaidRent'), value: this.money(s.currentPaid), delta: t('postedCollections'), trend: 'up' }, { icon: '!', label: t('currentUnpaidAmount'), value: this.money(s.currentUnpaid), delta: t('needsFollowUp'), trend: Number(s.currentUnpaid) ? 'down' : 'up', action: 'admin-tenancy-current-unpaid-details', actionLabel: t('viewDetails') }, { icon: t('balanceMetricIcon'), label: t('totalUnpaidAmount'), value: this.money(s.totalUnpaid), delta: t('pastMonthsIncluded'), trend: Number(s.totalUnpaid) ? 'down' : 'up' }, { icon: t('partialMetricIcon'), label: t('partialPaymentCount'), value: t('countValue', { count: Number(s.partialCount || 0) }), delta: t('currentMonthBills'), trend: Number(s.partialCount) ? 'down' : '' }, { icon: t('overdueMetricIcon'), label: t('overdueRentCount'), value: t('countValue', { count: Number(s.overdueCount || 0) }), delta: t('currentMonthBills'), trend: Number(s.overdueCount) ? 'down' : 'up' }]; },
    async openCurrentUnpaidDetails() { const monthStart = `${this.todayDate.slice(0, 7)}-01`; const monthEndDate = new Date(`${monthStart}T00:00:00Z`); monthEndDate.setUTCMonth(monthEndDate.getUTCMonth() + 1); monthEndDate.setUTCDate(0); const monthEnd = monthEndDate.toISOString().slice(0, 10); this.currentUnpaidRows = []; this.currentUnpaidError = ''; this.currentUnpaidLoading = true; this.$refs.currentUnpaidDialog?.showModal(); try { const first = await fetchAdminTenancy({ page: 1, pageSize: 100, startDate: monthStart, endDate: monthEnd }); const pages = Number(first.page?.totalPages || 1); const rest = pages > 1 ? await Promise.all(Array.from({ length: pages - 1 }, (_, index) => fetchAdminTenancy({ page: index + 2, pageSize: 100, startDate: monthStart, endDate: monthEnd }))) : []; this.currentUnpaidRows = [first, ...rest].flatMap(result => result.rows || []).filter(row => row.billingMonth?.slice(0, 7) === this.todayDate.slice(0, 7) && Number(row.amountUnpaid || 0) > 0).sort((a, b) => Number(b.amountUnpaid || 0) - Number(a.amountUnpaid || 0)); } catch (error) { this.currentUnpaidError = error.message || this.$t('tenancy.currentUnpaidLoadFailed'); } finally { this.currentUnpaidLoading = false; } },
    resetLoad() { this.pageNumber = 1; this.loadData(); }, goPage(n) { if (n >= 1 && n <= this.totalPages && n !== this.pageNumber) { this.pageNumber = n; this.loadData(); } },
    openTenant() { this.tenantForm = { fullName: '', identityNo: '', phone: '', email: '', status: 'active' }; this.actionError = ''; this.$refs.tenantDialog?.showModal(); },
    openLease() { this.leaseForm = { tenantId: this.selectedRow?.tenantId || null, unitId: null, rentalSpaceId: null, rentalMandateId: null, startDate: today(), endDate: nextYear(), monthlyRent: 0, depositAmount: 0, paymentDay: 1, rentCalculationMethod: 'daily_prorated' }; this.leaseContractFile = null; this.actionError = ''; this.$refs.leaseDialog?.showModal(); },
    applyMandateDates() { const unit = this.selectedLeaseUnit; if (!unit) return; this.leaseForm.startDate = unit.mandateStartDate || today(); this.leaseForm.endDate = unit.mandateEndDate || nextYear(); this.leaseForm.rentalSpaceId = null; this.$nextTick(() => { if (this.availableLeaseSpaces.length === 1) this.leaseForm.rentalSpaceId = this.availableLeaseSpaces[0].id; }); },
    closeDialog(ref) { this.$refs[ref]?.close(); this.actionError = ''; },
    async submitTenant() { this.saving = true; this.actionError = ''; try { await createAdminTenant(this.tenantForm); this.closeDialog('tenantDialog'); await this.loadData(); this.page.showToast(this.$t('tenancy.tenantCreated')); } catch (e) { this.actionError = e.message || this.$t('tenancy.tenantCreateFailed'); } finally { this.saving = false; } },
    async submitLease() { this.saving = true; this.actionError = ''; try { const created = await createAdminLease({ ...this.leaseForm, rentCalculationMethod: 'daily_prorated' }); this.selectedId = this.leaseForm.tenantId; this.selectedLeaseId = created.id; this.selectedInvoiceId = null; if (this.leaseContractFile) { try { await uploadAdminLeaseContract(created.id, this.leaseContractFile); } catch (uploadError) { this.closeDialog('leaseDialog'); await this.loadData(); this.page.showToast(this.$t('tenancy.leaseCreatedUploadFailed', { message: uploadError.message || '' })); return; } } this.closeDialog('leaseDialog'); await this.loadData(); this.page.showToast(this.$t(this.leaseContractFile ? 'tenancy.leaseCreatedWithDocument' : 'tenancy.leaseCreated')); } catch (e) { this.actionError = e.message || this.$t('tenancy.leaseCreateFailed'); } finally { this.saving = false; } },
    async openRenewalLease() {
      if (!this.selectedRow?.leaseId || this.selectedRow.leaseStatus !== 'active') return;
      this.renewalSaving = true; this.renewalError = '';
      try {
        this.renewalPeriods = await fetchAdminLeasePeriods(this.selectedRow.leaseId);
        const latest = this.renewalPeriods.at(-1) || null;
        const startDate = plusDays(latest?.endDate || this.selectedRow.leaseEnd, 1);
        const endDate = new Date(`${startDate}T00:00:00`); endDate.setFullYear(endDate.getFullYear() + 1);
        this.renewalLeaseForm = { startDate, endDate: endDate.toISOString().slice(0, 10), monthlyRent: Number(latest?.monthlyRent ?? this.selectedRow.monthlyRent ?? 0), depositAmount: Number(latest?.depositAmount ?? this.selectedRow.depositAmount ?? 0), paymentDay: Number(latest?.paymentDay ?? this.selectedRow.paymentDay ?? 1), rentCalculationMethod: latest?.rentCalculationMethod || this.selectedRow.rentCalculationMethod || 'daily_prorated' };
        this.renewalContractFile = null;
        this.$refs.renewalLeaseDialog?.showModal();
      } catch (error) { this.renewalError = error.message || this.$t('processCenter.loadFailed'); }
      finally { this.renewalSaving = false; }
    },
    selectRenewalContract(event) { const file = event.target.files?.[0] || null; if (!file) { this.renewalContractFile = null; return; } const validType = ['application/pdf', 'image/jpeg', 'image/png'].includes(file.type); if (!validType || file.size > 10 * 1024 * 1024) { this.renewalError = this.$t('processCenter.operationsRenewalContractInvalid'); event.target.value = ''; this.renewalContractFile = null; return; } this.renewalError = ''; this.renewalContractFile = file; },
    async submitRenewalLease() { if (this.renewalSaving) return; const form = this.renewalLeaseForm || {}; const latest = this.renewalPeriods.at(-1); const minimumStart = plusDays(latest?.endDate || this.selectedRow?.leaseEnd, 1); if (!form.startDate || form.startDate !== minimumStart || !form.endDate || form.endDate < form.startDate) { this.renewalError = this.$t('processCenter.operationsRenewalInvalidDates'); return; } this.renewalSaving = true; this.renewalError = ''; try { const period = await createAdminLeaseRenewal(this.selectedRow.leaseId, { startDate: form.startDate, endDate: form.endDate, monthlyRent: Number(form.monthlyRent), depositAmount: Number(form.depositAmount || 0), paymentDay: Number(form.paymentDay), rentCalculationMethod: form.rentCalculationMethod || 'daily_prorated' }); if (this.renewalContractFile) { try { await uploadAdminLeaseRenewalContract(this.selectedRow.leaseId, period.id, this.renewalContractFile); } catch (uploadError) { this.closeDialog('renewalLeaseDialog'); await this.loadData(); this.page.showToast(this.$t('processCenter.operationsRenewalCreatedButContractFailed', { message: uploadError.message || '' })); return; } } const leaseId = this.selectedRow.leaseId; this.closeDialog('renewalLeaseDialog'); await this.loadData(); this.selectedLeaseId = leaseId; this.page.showToast(`${this.$t('tenancy.renewalCreated', { period: period.periodNo })}${this.renewalContractFile ? this.$t('tenancy.renewalAttachmentSaved') : ''}`); } catch (error) { this.renewalError = error.message || this.$t('processCenter.loadFailed'); } finally { this.renewalSaving = false; } },
    async downloadRenewalPeriod(period) { try { const result = await fetchAdminLeaseRenewalContract(this.selectedRow.leaseId, period.id, true); const url = URL.createObjectURL(result.blob); const link = document.createElement('a'); link.href = url; link.download = period.contractDocumentName || this.$t('tenancy.renewalAttachmentName', { period: period.periodNo }); document.body.appendChild(link); link.click(); link.remove(); URL.revokeObjectURL(url); } catch (error) { this.renewalError = error.message || this.$t('tenancy.renewalAttachmentDownloadFailed'); } },
    openEditLease() { if (!this.selectedRow?.leaseId || this.selectedRow.leaseStatus !== 'active') return; this.editLeaseForm = { tenantId: Number(this.selectedRow.tenantId), unitId: Number(this.selectedRow.unitId), startDate: this.selectedRow.leaseStart, endDate: this.selectedRow.leaseEnd, monthlyRent: Number(this.selectedRow.monthlyRent), depositAmount: Number(this.selectedRow.depositAmount), paymentDay: Number(this.selectedRow.paymentDay), rentCalculationMethod: 'daily_prorated' }; this.actionError = ''; this.$refs.editLeaseDialog?.showModal(); },
    async submitEditLease() { this.saving = true; this.actionError = ''; try { await updateAdminLease(this.selectedRow.leaseId, { ...this.editLeaseForm, tenantId: Number(this.editLeaseForm.tenantId), unitId: Number(this.editLeaseForm.unitId), rentCalculationMethod: 'daily_prorated' }); this.closeDialog('editLeaseDialog'); await this.loadData(); this.page.showToast(this.$t('tenancy.leaseUpdated')); } catch (e) { this.actionError = e.message || this.$t('tenancy.leaseUpdateFailed'); } finally { this.saving = false; } },
    openManualClose() { if (!this.selectedRow?.leaseId || this.selectedRow.leaseStatus !== 'active') return; const ended = Boolean(this.selectedRow.leaseEnd && this.selectedRow.leaseEnd < this.todayDate); this.closeLeaseForm = { endDate: ended ? this.selectedRow.leaseEnd : this.todayDate, reason: ended ? 'normal_expiry' : 'early_termination', notes: '' }; this.actionError = ''; this.$refs.closeLeaseDialog?.showModal(); },
    async submitManualClose() { if (this.saving || !this.selectedRow?.leaseId) return; this.saving = true; this.actionError = ''; try { await closeAdminLease(this.selectedRow.leaseId, { endDate: this.closeLeaseForm.endDate, reason: this.closeLeaseForm.reason, notes: this.closeLeaseForm.notes || null }); this.closeDialog('closeLeaseDialog'); await this.loadData(); this.page.showToast(this.$t('tenancy.manualTerminateCompleted')); } catch (e) { this.actionError = e.message || this.$t('tenancy.manualTerminateFailed'); } finally { this.saving = false; } },
    openTransferLease() { if (!this.selectedRow?.leaseId || this.selectedRow.leaseStatus !== 'active') return; this.transferLeaseForm = { newTenantId: null, transferDate: this.transferMinDate, endDate: this.selectedRow.leaseEnd, monthlyRent: Number(this.selectedRow.monthlyRent), depositAmount: Number(this.selectedRow.depositAmount), paymentDay: Number(this.selectedRow.paymentDay), rentCalculationMethod: 'daily_prorated' }; this.transferContractFile = null; this.actionError = ''; this.$refs.transferLeaseDialog?.showModal(); },
    selectTransferContract(event) { const file = event.target.files?.[0] || null; if (file && !this.validContract(file)) { event.target.value = ''; return; } this.transferContractFile = file; },
    async submitTransferLease() { this.saving = true; this.actionError = ''; try { const oldLeaseId = this.selectedRow.leaseId; const newTenantId = this.transferLeaseForm.newTenantId; const created = await transferAdminLease(oldLeaseId, { ...this.transferLeaseForm, rentCalculationMethod: 'daily_prorated' }); this.selectedId = newTenantId; this.selectedLeaseId = created.id; this.selectedInvoiceId = null; if (this.transferContractFile) { try { await uploadAdminLeaseContract(created.id, this.transferContractFile); } catch (uploadError) { this.closeDialog('transferLeaseDialog'); await this.loadData(); this.page.showToast(this.$t('tenancy.transferUploadFailed', { message: uploadError.message || '' })); return; } } this.closeDialog('transferLeaseDialog'); await this.loadData(); this.page.showToast(this.$t(this.transferContractFile ? 'tenancy.transferWithDocumentCompleted' : 'tenancy.transferCompleted')); } catch (e) { this.actionError = e.message || this.$t('tenancy.transferFailed'); } finally { this.saving = false; } },
    selectNewLeaseContract(event) { const file = event.target.files?.[0] || null; if (file && !this.validContract(file)) { event.target.value = ''; return; } this.leaseContractFile = file; },
    openLeaseSigning() { if (!this.selectedRow?.leaseId) return; if (this.selectedRow.contractDocumentId) this.openLeaseSigningDialog(); else this.openTemplateGenerator('tenancy-agreement', true); },
    async openLeaseSigningDialog() {
      const defaults = [
        { signerRole: 'owner', signerName: this.selectedRow?.ownerName || '', signerEmail: '' },
        { signerRole: 'owner_witness', signerName: '', signerEmail: '' },
        { signerRole: 'tenant', signerName: this.selectedRow?.tenantName || '', signerEmail: this.selectedRow?.email || '' },
        { signerRole: 'tenant_witness', signerName: '', signerEmail: '' },
      ];
      this.signatureForm = { signers: defaults, expiresInDays: 7 }; this.signatureLinks = []; this.signatureEmailBusyId = null; this.signatureEmailSentIds = []; this.signatureDeliveryError = ''; this.actionError = '';
      this.$refs.leaseSigningDialog?.showModal();
      try {
        const saved = await fetchAdminLeaseSignatureParticipants(this.selectedRow.leaseId);
        const byRole = new Map((saved || []).map(signer => [signer.signerRole, signer]));
        this.signatureForm.signers = defaults.map(signer => ({ ...signer, ...(byRole.get(signer.signerRole) || {}) }));
      } catch (e) { this.actionError = e.message || this.$t('rentalFiles.signerPackageLoadFailed'); }
    },
    async startLeaseSigning() { this.signatureBusy = true; this.actionError = ''; try { const result = await startAdminLeaseSignaturePackage(this.selectedRow.leaseId, this.signatureForm); const links = result?.signingLinks || (result?.signingUrl ? [result] : []); this.signatureLinks = links.map(link => ({ ...link, deliveryEmail: link.signerEmail || '' })); this.signatureEmailSentIds = []; this.signatureDeliveryError = ''; this.page.showToast(this.$t('rentalFiles.signingLinksReady')); } catch (e) { this.actionError = e.message || this.$t('tenancy.signingInvitationFailed'); } finally { this.signatureBusy = false; } },
    async sendLeaseSigningEmail(link) { if (!link?.requestId || !link?.signingUrl || this.signatureEmailBusyId) return; const email = String(link.deliveryEmail || '').trim(); if (!email) { this.signatureDeliveryError = this.$t('rentalFiles.deliveryEmailRequired'); return; } if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) { this.signatureDeliveryError = this.$t('rentalFiles.deliveryEmailInvalid'); return; } this.signatureEmailBusyId = link.requestId; this.signatureDeliveryError = ''; try { await sendAdminElectronicSignatureInvitation(link.requestId, link.signingUrl, email); link.signerEmail = email; if (!this.signatureEmailSentIds.includes(link.requestId)) this.signatureEmailSentIds = [...this.signatureEmailSentIds, link.requestId]; } catch (e) { this.signatureDeliveryError = e.message || this.$t('rentalFiles.emailSendFailed'); } finally { this.signatureEmailBusyId = null; } },
    whatsAppLeaseSigningUrl(link) { const message = this.$t('rentalFiles.whatsAppSigningMessage', { name: link?.signerName || '', url: link?.signingUrl || '' }); return `https://wa.me/?text=${encodeURIComponent(message)}`; },
    async copyLeaseSigningLink(url) { if (!url) return; try { await navigator.clipboard.writeText(url); } catch { const input = document.createElement('textarea'); input.value = url; input.style.position = 'fixed'; input.style.opacity = '0'; document.body.appendChild(input); input.select(); document.execCommand('copy'); input.remove(); } this.page.showToast(this.$t('rentalFiles.linkCopied')); },
    leaseSignerRoleLabel(role) { const keys = { owner: 'signerRoleOwner', owner_witness: 'signerRoleOwnerWitness', tenant: 'signerRoleTenant', tenant_witness: 'signerRoleTenantWitness' }; return this.$t(`rentalFiles.${keys[role] || keys.owner}`); },
    openTemplateGenerator(type, signingMode = false) {
      const row = this.selectedRow;
      if (!row?.leaseId || !['otr', 'tenancy-agreement'].includes(type)) return;
      const propertyAddress = `${row.projectName || ''} ${row.unitNo || ''} ${row.rentalSpaceName || ''}`.trim();
      const years = row.leaseStart && row.leaseEnd
        ? String(Math.max(1, Math.round((new Date(row.leaseEnd) - new Date(row.leaseStart)) / (365 * 86400000)))) : '1';
      const monthlyRent = Number(row.monthlyRent || 0);
      const securityDeposit = Number(row.depositAmount || 0);
      const utilityDeposit = monthlyRent > 0 ? monthlyRent / 2 : 0;
      const stampingFee = 0;
      const amount = value => Number(value || 0).toFixed(2);
      const common = {
        caseNo: row.leaseNo || `LEASE-${row.leaseId}`,
        propertyAddress,
        landlordName: row.ownerName || '', landlordIdentity: row.ownerIdentity || '',
        tenantName: row.tenantName || '', tenantIdentity: row.identityNo || '',
        monthlyRent: amount(monthlyRent),
        advanceRental: amount(monthlyRent),
        securityDeposit: amount(securityDeposit),
        utilityDeposit: amount(utilityDeposit), otherConditions: 'Nil', specialConditions: '',
        commencementDate: row.leaseStart || '', periodYears: years,
        renewalYears: '1',
        securityDepositMonths: monthlyRent > 0 ? String(Number((securityDeposit / monthlyRent).toFixed(2))) : '0',
        utilityDepositMonths: monthlyRent > 0 ? String(Number((utilityDeposit / monthlyRent).toFixed(2))) : '0',
        stampingFee: amount(stampingFee),
        totalBeforeKeys: amount(monthlyRent + securityDeposit + utilityDeposit + stampingFee),
        earnestDeposit: amount(monthlyRent), commission: '', startDate: row.leaseStart || this.todayDate,
        tenantDate: row.leaseStart || this.todayDate,
        landlordDate: row.leaseStart || this.todayDate,
        tenantWitnessName: '', tenantWitnessIdentity: '', tenantWitnessDate: row.leaseStart || this.todayDate,
        landlordWitnessName: '', landlordWitnessIdentity: '', landlordWitnessDate: row.leaseStart || this.todayDate
      };
      const fields = type === 'tenancy-agreement' ? {
        ...common, leaseId: row.leaseId || '', unitNo: row.unitNo || '', agreementDate: this.todayDate,
        landlordAddress: row.ownerAddress || '', tenantPhone: row.phone || '',
        tenantAddress: row.tenantAddress || '', tenantEmail: row.email || '',
        leaseStart: row.leaseStart || '', leaseEnd: row.leaseEnd || '',
        termYears: years, paymentDay: row.paymentDay || 1,
         use: 'For Residential use only', bankName: '', bankAccount: '', bankBranch: '', renewalOption: ''
      } : common;
      this.templateForm = { templateType: type, signingMode, fields };
      this.templateError = '';
      this.$refs.templateDialog?.showModal();
    },
    async generateTemplate() {
      this.templateBusy = true;
      this.templateError = '';
      try {
        const mode = this.templateForm.signingMode;
        let fields = { ...this.templateForm.fields };
        const mandateId = this.selectedRow?.rentalMandateId || this.selectedRow?.mandateId;
        if (this.templateForm.templateType === 'tenancy-agreement' && mandateId) {
          const handover = await fetchAdminPropertyHandover(mandateId).catch(() => null);
          fields = { ...fields, handoverDate: handover?.handoverDate || fields.leaseStart || '', electricityMeter: handover?.electricityMeter || '', waterMeter: handover?.waterMeter || '', attendedByName: '', attendedByDesignation: '' };
        }
        const result = await generateAdminContractTemplate(this.templateForm.templateType, fields);
        if (mode) {
          const file = new File([result.blob], result.filename, { type: 'application/pdf' });
          await uploadAdminGeneratedLeaseContract(this.selectedRow.leaseId, file);
          await this.loadData();
          this.closeDialog('templateDialog');
          this.page.showToast(this.$t('tenancy.contractReadyForSigning'));
          this.openLeaseSigningDialog();
        } else {
          const url = URL.createObjectURL(result.blob);
          const link = document.createElement('a'); link.href = url; link.download = result.filename; link.style.display = 'none';
          document.body.appendChild(link); link.click(); link.remove();
          setTimeout(() => URL.revokeObjectURL(url), 1000);
          this.closeDialog('templateDialog');
          this.page.showToast(this.templateForm.templateType === 'tenancy-agreement'
            ? this.$t('tenancy.tenancyTemplateGenerated') : this.$t('tenancy.templateGenerated'));
        }
      } catch (e) {
        this.templateError = e.message || this.$t('tenancy.templateGenerateFailed');
      } finally { this.templateBusy = false; }
    },
    validContract(file) { const types = ['application/pdf', 'image/jpeg', 'image/png']; if (!types.includes(file.type) || file.size > 10 * 1024 * 1024) { this.page.showToast(this.$t('tenancy.unsupportedContract')); return false; } return true; },
    selectRow(row) { this.selectedId = row?.tenantId || null; this.selectedLeaseId = row?.leaseId || null; this.selectedInvoiceId = row?.invoiceId || null; },
    openTenantDetails(row) { this.selectRow(row); this.$nextTick(() => { const dialog = this.$refs.tenancyDetailDialog; if (!dialog || dialog.open) return; dialog.showModal(); this.$nextTick(() => dialog.querySelector('.tenancy-actions button:not(:disabled)')?.focus()); }); },
    async openInvoiceDetails(row) { if (!row?.leaseId) return; this.selectRow(row); this.invoiceDetailTitle = `${row.tenantName || this.$t('tenancy.tenantFallback')} · ${row.projectName || ''} ${row.unitNo || ''}`.trim(); this.invoiceDetails = []; this.invoiceDetailsError = ''; this.invoiceDetailsLoading = true; this.$refs.invoiceDetailsDialog?.showModal(); try { this.invoiceDetails = await fetchAdminLeaseRentInvoices(row.leaseId); } catch (e) { this.invoiceDetailsError = e.message || this.$t('tenancy.rentDetailsLoadFailed'); } finally { this.invoiceDetailsLoading = false; } },
    statusParam(v) { const statuses = { paid: 'rentPaid', partial: 'rentPartial', unpaid: 'rentUnpaid', overdue: 'rentOverdue' }; return Object.keys(statuses).find(status => this.$t(`tenancy.${statuses[status]}`) === v) || ''; },
    contractStatusLabel(v) { return this.$t(`tenancy.${({ missing: 'contractMissing', uploaded: 'contractUploadedPending', pending: 'contractSigning', signed: 'contractSigned' })[v] || 'contractMissing'}`); },
    rentLabel(v) { return this.$t(`tenancy.${({ paid: 'rentPaid', partial: 'rentPartial', unpaid: 'rentUnpaid', overdue: 'rentOverdue', no_invoice: 'rentNotBilled' })[v] || 'rentNotBilled'}`); },
    rentClass(v) { return v === 'paid' ? 'green' : v === 'overdue' ? 'red' : v === 'partial' || v === 'unpaid' ? 'orange' : 'gray'; },
    displayDate(value, fallback = '—') { return formatDate(value, fallback); }, monthLabel(value) { return formatMonth(value); }, money(v) { return `RM ${Number(v || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`; }, initials(name) { return String(name || '').split(/\s+/).slice(0, 2).map(p => p[0]).join('').toUpperCase() || this.$t('ui.tenantIcon'); }
  }
};
</script>

<style scoped>
/* Hallmark · pre-emit critique: P5 H5 E5 S5 R5 V5 */
/* Hallmark · component: tenant-detail-dialog-toolbar · genre: modern-minimal · theme: existing CCPS
 * states: default · hover · focus · active · disabled · loading · error · success
 * contrast: pass (40–41) · responsive: pass (34,49,50–57)
 */
.admin-tenancy-workspace{
  --tenancy-web-color-paper:#fff;
  --tenancy-web-color-surface:#f7fafb;
  --tenancy-web-color-surface-muted:#eef5f6;
  --tenancy-web-color-ink:#14324b;
  --tenancy-web-color-ink-strong:#0d2f4a;
  --tenancy-web-color-muted:#61778a;
  --tenancy-web-color-rule:#d9e4eb;
  --tenancy-web-color-rule-strong:#adc3cf;
  --tenancy-web-color-hover:#edf8f8;
  --tenancy-web-color-accent:#087f87;
  --tenancy-web-color-accent-ink:#fff;
  --tenancy-web-color-accent-soft:#e0f3f3;
  --tenancy-web-color-focus:rgba(8,127,135,.24);
  --tenancy-web-color-error:#c7353d;
  --tenancy-web-color-success:#16824d;
  --tenancy-web-color-shadow:rgba(6,35,48,.22);
  --tenancy-web-font-body:"HarmonyOS Sans SC","Noto Sans SC","Microsoft YaHei","PingFang SC",sans-serif;
  --tenancy-web-space-2xs:.25rem;
  --tenancy-web-space-xs:.5rem;
  --tenancy-web-space-sm:.75rem;
  --tenancy-web-space-md:1rem;
  --tenancy-web-space-lg:1.5rem;
  --tenancy-web-space-xl:2rem;
  --tenancy-web-text-caption:.75rem;
  --tenancy-web-text-body:.875rem;
  --tenancy-web-text-heading:1.125rem;
  --tenancy-web-radius-card:.875rem;
  --tenancy-web-radius-control:.5rem;
  --tenancy-web-radius-pill:999px;
  --tenancy-web-control-height:2.75rem;
  --tenancy-web-ease-out:cubic-bezier(.16,1,.3,1);
  --tenancy-web-duration-micro:120ms;
  --tenancy-web-duration-short:220ms;
  --tenancy-web-duration-reduced:150ms;
  grid-template-columns:minmax(0,1fr);
}
.admin-tenancy-workspace .tenancy-table-wrap{overflow-x:hidden}
.tenancy-table-row{cursor:pointer;transition:background-color var(--tenancy-web-duration-micro) var(--tenancy-web-ease-out)}
@media(hover:hover) and (pointer:fine){.tenancy-table-row:hover{background:#f5fbfb}}
.tenancy-table-row:active{background:#e4f4f3}
.tenancy-table-row.selected{background:#eaf7f6}
.tenancy-table-wrap th,.tenancy-table-wrap td{box-sizing:border-box;overflow:hidden;text-overflow:ellipsis}
.tenancy-table-wrap td>strong,.tenancy-table-wrap td>small{overflow:hidden;text-overflow:ellipsis}
.tenancy-lease-period{display:block;color:var(--tenancy-web-color-ink-strong);font-size:1rem;font-weight:800;line-height:1.35;white-space:nowrap}.tenancy-lease-code{display:block;margin-top:var(--tenancy-web-space-2xs);color:var(--tenancy-web-color-muted);font-size:var(--tenancy-web-text-caption);font-weight:400;line-height:1.35;white-space:nowrap}
.tenancy-table-wrap th:nth-child(1),.tenancy-table-wrap td:nth-child(1){width:14%}
.tenancy-table-wrap th:nth-child(2),.tenancy-table-wrap td:nth-child(2){width:14%}
.tenancy-table-wrap th:nth-child(3),.tenancy-table-wrap td:nth-child(3){width:17%}
.tenancy-table-wrap th:nth-child(4),.tenancy-table-wrap td:nth-child(4){width:7%}
.tenancy-table-wrap th:nth-child(5),.tenancy-table-wrap td:nth-child(5){width:6%}
.tenancy-table-wrap th:nth-child(6),.tenancy-table-wrap td:nth-child(6){width:7%}
.tenancy-table-wrap th:nth-child(7),.tenancy-table-wrap td:nth-child(7){width:9%}
.tenancy-table-wrap th:nth-child(8),.tenancy-table-wrap td:nth-child(8){width:8%}
.tenancy-table-wrap th:nth-child(9),.tenancy-table-wrap td:nth-child(9){width:7%}
.tenancy-table-wrap th:nth-child(10),.tenancy-table-wrap td:nth-child(10){width:5%}
.tenancy-table-wrap th:nth-child(11),.tenancy-table-wrap td:nth-child(11){width:6%}
.tenancy-row-action-cell,.tenancy-action-column{text-align:center}.tenancy-table-wrap td.tenancy-row-action-cell{overflow:visible}
.tenancy-row-action{box-sizing:border-box;display:inline-grid;place-items:center;width:100%;min-height:var(--tenancy-web-control-height);overflow:hidden;padding:0 var(--tenancy-web-space-xs);border:1px solid var(--tenancy-web-color-rule-strong);border-radius:var(--tenancy-web-radius-control);background:var(--tenancy-web-color-paper);color:var(--tenancy-web-color-accent);font:inherit;font-weight:700;text-overflow:ellipsis;white-space:nowrap;cursor:pointer;transition:background-color var(--tenancy-web-duration-micro) var(--tenancy-web-ease-out),color var(--tenancy-web-duration-micro) var(--tenancy-web-ease-out),transform var(--tenancy-web-duration-micro) var(--tenancy-web-ease-out)}
@media(hover:hover) and (pointer:fine){.tenancy-row-action:hover{border-color:var(--tenancy-web-color-accent);background:var(--tenancy-web-color-accent-soft)}}
.tenancy-row-action:focus-visible{outline:3px solid var(--tenancy-web-color-focus);outline-offset:2px}
.tenancy-row-action:active{transform:translateY(1px)}
.tenancy-row-action:disabled,.tenancy-row-action[data-state="loading"]{cursor:not-allowed;opacity:.55}
.tenancy-row-action[data-state="error"]{border-color:var(--tenancy-web-color-error);color:var(--tenancy-web-color-error)}
.tenancy-row-action[data-state="success"]{border-color:var(--tenancy-web-color-success);color:var(--tenancy-web-color-success)}
.tenancy-detail-dialog{position:fixed;inset:0;box-sizing:border-box;width:min(1000px,calc(100% - var(--tenancy-web-space-xl)));height:fit-content;max-height:min(86dvh,780px);margin:auto;padding:0;border:1px solid var(--tenancy-web-color-rule-strong);border-radius:var(--tenancy-web-radius-card);background:var(--tenancy-web-color-paper);box-shadow:0 1.5rem 4rem var(--tenancy-web-color-shadow);overflow:hidden;color:var(--tenancy-web-color-ink);font-family:var(--tenancy-web-font-body)}
.tenancy-detail-dialog[open]{animation:tenancy-detail-enter var(--tenancy-web-duration-short) var(--tenancy-web-ease-out)}
.tenancy-detail-dialog::backdrop{background:color-mix(in oklch,var(--tenancy-web-color-ink-strong) 58%,transparent);animation:tenancy-detail-backdrop var(--tenancy-web-duration-short) var(--tenancy-web-ease-out)}
.tenancy-detail-card{box-sizing:border-box;display:grid;grid-template-rows:auto auto minmax(0,1fr);max-height:min(86dvh,780px);padding:0;overflow:hidden;background:var(--tenancy-web-color-paper)}
.tenancy-detail-card-head{z-index:1;display:grid;grid-template-columns:minmax(0,1fr) auto;align-items:center;gap:var(--tenancy-web-space-md);padding:var(--tenancy-web-space-lg);border-bottom:1px solid var(--tenancy-web-color-rule);background:var(--tenancy-web-color-paper)}
.tenancy-detail-card-head .tenancy-profile{margin:0}
.tenancy-profile-copy{display:grid;gap:var(--tenancy-web-space-2xs);min-width:0}
.tenancy-profile-copy h3,.tenancy-profile-copy p,.tenancy-profile-copy small{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.tenancy-profile-copy small{color:var(--tenancy-web-color-muted);font-size:var(--tenancy-web-text-caption)}
.tenancy-detail-close{display:grid;place-items:center;width:var(--tenancy-web-control-height);height:var(--tenancy-web-control-height);padding:0;border:1px solid var(--tenancy-web-color-rule);border-radius:var(--tenancy-web-radius-control);background:var(--tenancy-web-color-surface);color:var(--tenancy-web-color-muted);font:inherit;font-size:1.5rem;line-height:1;cursor:pointer;transition:background-color var(--tenancy-web-duration-micro) var(--tenancy-web-ease-out),color var(--tenancy-web-duration-micro) var(--tenancy-web-ease-out),transform var(--tenancy-web-duration-micro) var(--tenancy-web-ease-out)}
@media(hover:hover) and (pointer:fine){.tenancy-detail-close:hover{background:var(--tenancy-web-color-hover);color:var(--tenancy-web-color-ink-strong)}}
.tenancy-detail-close:focus-visible{outline:3px solid var(--tenancy-web-color-focus);outline-offset:2px}
.tenancy-detail-close:active{transform:translateY(1px)}
.tenancy-detail-close:disabled{cursor:not-allowed;opacity:.45}
.tenancy-action-toolbar{display:grid;grid-template-columns:minmax(8rem,auto) minmax(0,1fr);align-items:start;gap:var(--tenancy-web-space-md);padding:var(--tenancy-web-space-sm) var(--tenancy-web-space-lg);border-bottom:1px solid var(--tenancy-web-color-rule);background:var(--tenancy-web-color-surface)}
.tenancy-action-toolbar-head{display:grid;gap:var(--tenancy-web-space-2xs);min-width:0;padding-block:var(--tenancy-web-space-2xs)}
.tenancy-action-toolbar-head strong{color:var(--tenancy-web-color-ink-strong);font-size:var(--tenancy-web-text-body)}
.tenancy-action-toolbar-head span{overflow:hidden;color:var(--tenancy-web-color-muted);font-size:var(--tenancy-web-text-caption);text-overflow:ellipsis;white-space:nowrap}
.tenancy-detail-card .tenancy-actions{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:var(--tenancy-web-space-xs);margin:0}
.tenancy-detail-card .tenancy-actions button{box-sizing:border-box;min-width:0;min-height:var(--tenancy-web-control-height);overflow:hidden;padding-inline:var(--tenancy-web-space-sm);border:1px solid var(--tenancy-web-color-rule-strong);border-radius:var(--tenancy-web-radius-control);background:var(--tenancy-web-color-paper);color:var(--tenancy-web-color-ink);font:inherit;font-weight:700;text-overflow:ellipsis;white-space:nowrap;cursor:pointer;transition:background-color var(--tenancy-web-duration-micro) var(--tenancy-web-ease-out),color var(--tenancy-web-duration-micro) var(--tenancy-web-ease-out),transform var(--tenancy-web-duration-micro) var(--tenancy-web-ease-out)}
.tenancy-detail-body{min-height:0;overflow-x:hidden;overflow-y:auto}
.tenancy-detail-main{min-width:0;padding:var(--tenancy-web-space-lg)}
.tenancy-detail-summary{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));margin-bottom:var(--tenancy-web-space-lg);border-radius:var(--tenancy-web-radius-control);background:var(--tenancy-web-color-surface-muted);font-variant-numeric:tabular-nums}
.tenancy-detail-summary>div{display:grid;gap:var(--tenancy-web-space-2xs);min-width:0;padding:var(--tenancy-web-space-md)}
.tenancy-detail-summary>div+div{border-inline-start:1px solid var(--tenancy-web-color-rule)}
.tenancy-detail-summary span{color:var(--tenancy-web-color-muted);font-size:var(--tenancy-web-text-caption)}
.tenancy-detail-summary strong{overflow:hidden;color:var(--tenancy-web-color-ink-strong);font-size:var(--tenancy-web-text-heading);text-overflow:ellipsis;white-space:nowrap}
.tenancy-detail-summary strong.money-red{color:var(--tenancy-web-color-error)}
.tenancy-policy-row{display:grid;grid-template-columns:var(--tenancy-web-space-2xs) minmax(0,1fr);align-items:stretch;gap:var(--tenancy-web-space-sm);margin-bottom:var(--tenancy-web-space-xs);padding-block:var(--tenancy-web-space-sm);border-block:1px solid var(--tenancy-web-color-rule);color:var(--tenancy-web-color-muted)}
.tenancy-policy-marker{border-radius:var(--tenancy-web-radius-pill);background:var(--tenancy-web-color-accent)}
.tenancy-policy-copy{display:flex;flex-wrap:wrap;gap:var(--tenancy-web-space-2xs) var(--tenancy-web-space-xs);min-width:0;font-size:var(--tenancy-web-text-caption);line-height:1.5}
.tenancy-policy-copy b{color:var(--tenancy-web-color-ink-strong)}
.tenancy-policy-row.active .tenancy-policy-marker{background:var(--tenancy-web-color-error)}
.tenancy-policy-row.active .tenancy-policy-copy b{color:var(--tenancy-web-color-error)}
.tenancy-detail-sections{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));column-gap:var(--tenancy-web-space-lg)}
.tenancy-detail-section{min-width:0;padding:var(--tenancy-web-space-md) 0}
.tenancy-detail-section .kv{grid-template-columns:minmax(88px,.7fr) minmax(0,1.3fr)}
.tenancy-detail-section .kv b{min-width:0;overflow-wrap:anywhere;text-align:end}
.tenancy-detail-lease-section{grid-column:1/-1;display:grid;grid-template-columns:repeat(2,minmax(0,1fr));column-gap:var(--tenancy-web-space-lg)}
.tenancy-detail-lease-section h4{grid-column:1/-1}
.tenancy-detail-lease-section .kv:nth-last-child(1){grid-column:1/-1}
.tenancy-invoice-footer{display:grid;grid-template-columns:minmax(0,1fr) auto;align-items:center;gap:var(--tenancy-web-space-sm);margin-top:var(--tenancy-web-space-sm);padding-top:var(--tenancy-web-space-sm);border-top:1px solid var(--tenancy-web-color-rule)}
.tenancy-invoice-footer .progress{min-width:0;margin:0}
.tenancy-invoice-link{display:inline-grid;place-items:center;min-height:var(--tenancy-web-control-height);padding-inline:var(--tenancy-web-space-sm);border:1px solid var(--tenancy-web-color-rule-strong);border-radius:var(--tenancy-web-radius-control);background:var(--tenancy-web-color-paper);color:var(--tenancy-web-color-accent);font:inherit;font-weight:700;white-space:nowrap;cursor:pointer;transition:background-color var(--tenancy-web-duration-micro) var(--tenancy-web-ease-out),color var(--tenancy-web-duration-micro) var(--tenancy-web-ease-out),transform var(--tenancy-web-duration-micro) var(--tenancy-web-ease-out)}
@media(hover:hover) and (pointer:fine){.tenancy-detail-card .tenancy-actions button:not(:disabled):hover{border-color:var(--tenancy-web-color-accent);background:var(--tenancy-web-color-accent-soft);color:var(--tenancy-web-color-ink-strong)}.tenancy-detail-card .tenancy-actions .tenancy-action-primary:not(:disabled):hover{border-color:var(--tenancy-web-color-ink-strong);background:var(--tenancy-web-color-ink-strong);color:var(--tenancy-web-color-accent-ink)}.tenancy-detail-card .tenancy-actions .danger-action:not(:disabled):hover{border-color:var(--tenancy-web-color-error);background:var(--tenancy-web-color-surface-muted);color:var(--tenancy-web-color-error)}}
.tenancy-invoice-link:focus-visible{outline:3px solid var(--tenancy-web-color-focus);outline-offset:2px}
.tenancy-invoice-link:active{transform:translateY(1px)}
.tenancy-invoice-link:disabled,.tenancy-invoice-link[data-state="loading"]{cursor:not-allowed;opacity:.55}
.tenancy-invoice-link[data-state="error"]{border-color:var(--tenancy-web-color-error);color:var(--tenancy-web-color-error)}
.tenancy-invoice-link[data-state="success"]{border-color:var(--tenancy-web-color-success);color:var(--tenancy-web-color-success)}
@media(hover:hover) and (pointer:fine){.tenancy-invoice-link:not(:disabled):hover{border-color:var(--tenancy-web-color-accent);background:var(--tenancy-web-color-accent-soft)}}
.tenancy-detail-card .tenancy-actions button:focus-visible{outline:3px solid var(--tenancy-web-color-focus);outline-offset:2px}
.tenancy-detail-card .tenancy-actions button:active{transform:translateY(1px)}
.tenancy-detail-card .tenancy-actions button[data-state="loading"]{cursor:not-allowed;opacity:.55}
.tenancy-detail-card .tenancy-actions button[data-state="error"]{border-color:var(--tenancy-web-color-error);color:var(--tenancy-web-color-error)}
.tenancy-detail-card .tenancy-actions button[data-state="success"]{border-color:var(--tenancy-web-color-success);color:var(--tenancy-web-color-success)}
@keyframes tenancy-detail-enter{from{opacity:0;transform:scale(.98)}to{opacity:1;transform:scale(1)}}
@keyframes tenancy-detail-backdrop{from{opacity:0}to{opacity:1}}
@media(max-width:75rem){.tenancy-table-wrap th:nth-child(4),.tenancy-table-wrap td:nth-child(4),.tenancy-table-wrap th:nth-child(8),.tenancy-table-wrap td:nth-child(8){display:none}}
@media(max-width:60rem){.tenancy-table-wrap th:nth-child(5),.tenancy-table-wrap td:nth-child(5),.tenancy-table-wrap th:nth-child(6),.tenancy-table-wrap td:nth-child(6),.tenancy-table-wrap th:nth-child(9),.tenancy-table-wrap td:nth-child(9){display:none}.tenancy-action-toolbar{grid-template-columns:1fr}.tenancy-action-toolbar-head{grid-template-columns:auto minmax(0,1fr);align-items:baseline;gap:var(--tenancy-web-space-xs)}.tenancy-detail-card .tenancy-actions{grid-template-columns:repeat(3,minmax(0,1fr))}.tenancy-detail-main{padding:var(--tenancy-web-space-md)}}
@media(max-width:48rem){.tenancy-table-wrap th:nth-child(2),.tenancy-table-wrap td:nth-child(2),.tenancy-table-wrap th:nth-child(3),.tenancy-table-wrap td:nth-child(3),.tenancy-table-wrap th:nth-child(7),.tenancy-table-wrap td:nth-child(7){display:none}.tenancy-table-wrap th:nth-child(1),.tenancy-table-wrap td:nth-child(1){width:50%}.tenancy-table-wrap th:nth-child(10),.tenancy-table-wrap td:nth-child(10){width:20%}.tenancy-table-wrap th:nth-child(11),.tenancy-table-wrap td:nth-child(11){width:30%}.tenancy-detail-dialog{width:calc(100% - var(--tenancy-web-space-md));max-height:calc(100dvh - var(--tenancy-web-space-md))}.tenancy-detail-card{max-height:calc(100dvh - var(--tenancy-web-space-md))}.tenancy-detail-card-head,.tenancy-action-toolbar{padding:var(--tenancy-web-space-md)}.tenancy-profile{grid-template-columns:44px minmax(0,1fr)}.tenancy-profile .tag{grid-column:2;justify-self:start}.tenancy-detail-sections{grid-template-columns:1fr}.tenancy-detail-lease-section{grid-column:auto;grid-template-columns:1fr}.tenancy-detail-lease-section h4,.tenancy-detail-lease-section .kv:nth-last-child(1){grid-column:auto}.tenancy-detail-close{width:var(--tenancy-web-control-height);height:var(--tenancy-web-control-height)}}
@media(max-width:40rem){.tenancy-detail-card .tenancy-actions{grid-template-columns:repeat(2,minmax(0,1fr))}}
@media(max-width:30rem){.tenancy-detail-summary{grid-template-columns:1fr}.tenancy-detail-summary>div+div{border-inline-start:0;border-top:1px solid var(--tenancy-web-color-rule)}.tenancy-invoice-footer{grid-template-columns:1fr}.tenancy-invoice-link{width:100%}}
@media(prefers-reduced-motion:reduce){.tenancy-detail-dialog[open],.tenancy-detail-dialog::backdrop{animation-duration:var(--tenancy-web-duration-reduced);animation-name:tenancy-detail-backdrop}.tenancy-table-row,.tenancy-row-action,.tenancy-detail-close,.tenancy-detail-card .tenancy-actions button,.tenancy-invoice-link{transition-duration:0ms}}
.lease-change-note,.lease-transfer-warning{grid-column:1/-1;display:grid;gap:5px;padding:11px 13px;border:1px solid #d9e3ed;border-radius:8px;background:#f6f9fc;color:#53667b;font-size:12px}.lease-change-note b{color:#12375f}.lease-transfer-warning{border-color:#efd28b;background:#fff8e7;color:#78570a}.lease-transfer-warning b{color:#8b5d00}.tenancy-actions .tenancy-action-primary{border-color:var(--tenancy-web-color-accent);background:var(--tenancy-web-color-accent);color:var(--tenancy-web-color-accent-ink)}.tenancy-actions button:disabled{opacity:.5;cursor:not-allowed}.tenancy-actions .danger-action{border-color:var(--tenancy-web-color-error);color:var(--tenancy-web-color-error);background:var(--tenancy-web-color-paper)}
.contract-template-dialog{width:min(760px,96vw)}
.renewal-lease-hint{grid-column:1/-1;margin:0;padding:10px 12px;border:1px solid #cfe0ef;border-radius:8px;background:#f3f8fc;color:#45617c;font-size:12px;line-height:1.6}
.renewal-deposit-hint{display:block;margin-top:5px;color:#6d8296;font-size:11px;line-height:1.5}
.renewal-period-history{display:grid;gap:7px;padding:12px;border:1px solid #d8e5ec;border-radius:10px;background:#f9fcfd}.renewal-period-item{display:grid;grid-template-columns:70px minmax(190px,1fr) 130px auto;align-items:center;gap:10px;padding:8px 10px;border-radius:8px;background:#fff;font-size:12px}.renewal-period-item button{justify-self:end;padding:5px 9px}.renewal-period-item em{justify-self:end;color:#8494a5;font-style:normal}
.tenancy-table-wrap table{width:100%;min-width:0;table-layout:fixed;font-variant-numeric:tabular-nums}.tenancy-deposit{color:#0b7f78;white-space:nowrap}
.tenancy-signing-links{display:grid;gap:12px;min-width:min(820px,82vw);padding:16px}.tenancy-signing-summary{display:grid;grid-template-columns:30px minmax(0,1fr);align-items:center;gap:10px;padding:12px 14px;border:1px solid #a9dbde;border-radius:9px;background:#effafa}.tenancy-signing-summary>span{display:grid;place-items:center;width:30px;height:30px;border-radius:50%;background:#08747c;color:#fff;font-weight:800}.tenancy-signing-summary>div{display:grid;gap:3px}.tenancy-signing-summary strong{color:#08747c}.tenancy-signing-summary small{color:#31556b}.tenancy-signing-links article{display:grid;grid-template-columns:minmax(150px,.7fr) minmax(0,1.3fr);align-items:center;gap:10px;padding:12px;border:1px solid #d7e5e8;border-radius:8px}.tenancy-signing-links article>div{display:grid;gap:3px;min-width:0}.tenancy-signing-links article>div strong,.tenancy-signing-links article>div small{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.tenancy-signing-links article>div small{color:#60798a}.tenancy-signing-links article input{box-sizing:border-box;width:100%;height:44px;border:1px solid #c9dce2;outline:2px solid transparent;outline-offset:1px;border-radius:7px;background:#f8fbfc;padding:0 9px}.tenancy-signing-links article input:focus-visible{outline-color:#12375f}.tenancy-signing-links article section{grid-column:1/-1;display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:8px}.tenancy-signing-links article button,.tenancy-signing-links article a{display:inline-grid;place-items:center;box-sizing:border-box;min-height:44px;border:1px solid #0b8f96;border-radius:7px;background:#fff;padding:0 11px;color:#08747c;font:inherit;font-weight:700;text-align:center;text-decoration:none;white-space:nowrap;cursor:pointer}.tenancy-signing-links article section button:first-child{background:#08747c;color:#fff}.tenancy-signing-links article button:focus-visible,.tenancy-signing-links article a:focus-visible{outline:3px solid #12375f;outline-offset:2px;box-shadow:0 0 0 2px #fff}.tenancy-signing-links article button:active,.tenancy-signing-links article a:active{background:#dff1f1}.tenancy-signing-links article button:disabled{cursor:not-allowed;opacity:.55}.tenancy-signing-links article button[data-state="success"]{border-color:#169451;background:#eaf8ef;color:#116e3e}@media(hover:hover){.tenancy-signing-links article input:hover{background:#f2f8fa}.tenancy-signing-links article button:hover:not(:disabled),.tenancy-signing-links article a:hover{background:#e9f7f7}.tenancy-signing-links article section button:first-child:hover:not(:disabled){background:#05676d}}@media(max-width:800px){.tenancy-signing-links{min-width:0}.tenancy-signing-links article{grid-template-columns:1fr}.tenancy-signing-links article section{grid-template-columns:1fr 1fr}}@media(max-width:430px){.tenancy-signing-links article section{grid-template-columns:1fr}}
.current-unpaid-dialog{width:min(1080px,96vw)}.current-unpaid-summary{display:flex;align-items:center;justify-content:space-between;padding:12px 14px;border:1px solid #f0d4d4;border-radius:9px;background:#fff7f7;color:#8d3b3b}.current-unpaid-summary strong{font-size:18px}.current-unpaid-table-wrap{max-height:58vh;border:1px solid #dce7ed;border-radius:9px}.current-unpaid-table-wrap table{min-width:900px}.current-unpaid-table-wrap td strong,.current-unpaid-table-wrap td small{display:block}.current-unpaid-table-wrap td small{margin-top:3px;color:#8192a0}
.tenancy-signing-links article{grid-template-columns:minmax(150px,.6fr) minmax(0,1.15fr) minmax(190px,.85fr)}.tenancy-signing-links article>label{display:grid;gap:5px;min-width:0;color:#60798a;font-size:10px;font-weight:700}@media(max-width:800px){.tenancy-signing-links article{grid-template-columns:1fr}}
</style>
