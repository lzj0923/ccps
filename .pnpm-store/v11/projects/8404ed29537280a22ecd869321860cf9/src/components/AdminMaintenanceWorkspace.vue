<template>
  <section class="content-grid admin-maintenance-workspace">
    <div class="panel table-panel maintenance-list-panel">
      <div class="panel-head">
        <div><h2>{{ $t('legacy.t_43337f59c780') }}</h2><span>{{ filteredRows.length }} {{ $t('legacy.t_ab0eac290998') }}</span></div>
        <div class="maintenance-tabs">
          <button type="button" :class="{ active: activeTab === 'expense' }" @click="selectTab('expense')">{{ $t('legacy.t_3361c4c6f1ee') }}</button>
          <button type="button" :class="{ active: activeTab === 'maintenance' }" @click="selectTab('maintenance')">{{ $t('legacy.t_643db1590668') }}</button>
        </div>
      </div>

      <div v-if="loading" class="admin-owner-state">{{ $t('legacy.t_62d0616a0881') }}</div>
      <div v-else-if="errorMessage" class="admin-owner-state error"><strong>{{ $t('legacy.t_53afb862b922') }}</strong><span>{{ errorMessage }}</span><button @click="loadData">{{ $t('legacy.t_0c9157b5bfac') }}</button></div>
      <div v-else class="table-wrap maintenance-table-wrap">
        <table v-if="activeTab === 'expense'">
          <thead><tr><th>{{ $t('legacy.t_b6fed9af8313') }}</th><th>{{ $t('legacy.t_114246450ff0') }}</th><th>{{ $t('legacy.t_0cf468db12ee') }}</th><th>{{ $t('legacy.t_9b6c1b038aa5') }}</th><th>{{ $t('legacy.t_380086757011') }}</th><th>{{ $t('legacy.t_5c0ec3674a79') }}</th><th>{{ $t('legacy.t_607b3e1024c4') }}</th><th>{{ $t('legacy.t_99f6fe6c41ad') }}</th></tr></thead>
          <tbody>
            <tr v-if="!filteredRows.length"><td colspan="8" class="admin-owner-empty">{{ $t('legacy.t_3818efa5c5aa') }}</td></tr>
            <tr v-for="row in pagedRows()" :key="`expense-${row.id}`" :class="{ selected: selectedKey === rowKey(row) }" @click="selectRow(row)">
              <td>{{ row.occurredOn || '—' }}</td>
              <td><strong>{{ row.projectName }}</strong><small>{{ row.unitNo }}</small></td>
              <td><span class="maintenance-category">{{ categoryLabel(row.category) }}</span></td>
              <td class="maintenance-description">{{ row.description }}</td>
              <td><strong>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(row.amount) }}</strong></td>
              <td :class="{ 'money-gold': Number(row.reserveDeductedAmount) > 0 }">{{ $t('legacy.t_5e7b60c626a4') }} {{ money(row.reserveDeductedAmount) }}</td>
              <td><span class="tag" :class="statusClass(paymentLabel(row.paymentStatus))">{{ paymentLabel(row.paymentStatus) }}</span></td>
              <td>{{ Number(row.attachmentCount || 0) }} {{ $t('legacy.t_aa9f1ad4f91c') }}</td>
            </tr>
          </tbody>
        </table>

        <table v-else>
          <thead><tr><th>{{ $t('legacy.t_64838faf9fb0') }}</th><th>{{ $t('legacy.t_114246450ff0') }}</th><th>{{ $t('legacy.t_4e3157d2e547') }}</th><th>{{ $t('legacy.t_1765119d7672') }}</th><th>{{ $t('legacy.t_dde3113a1be2') }}</th><th>{{ $t('legacy.t_a9ef3e6efac1') }}</th><th>{{ $t('legacy.t_e81e9a4a92a1') }}</th><th>{{ $t('legacy.t_99f6fe6c41ad') }}</th><th>{{ $t('legacy.t_f3ea6d345e2a') }}</th></tr></thead>
          <tbody>
            <tr v-if="!filteredRows.length"><td colspan="9" class="admin-owner-empty">{{ $t('legacy.t_595650fd16ec') }}</td></tr>
            <tr v-for="row in pagedRows()" :key="`maintenance-${row.id}`" :class="{ selected: selectedKey === rowKey(row) }" @click="selectRow(row)">
              <td><strong>{{ row.workOrderNo }}</strong></td>
              <td><strong>{{ row.projectName }}</strong><small>{{ row.unitNo }}</small></td>
              <td><span class="maintenance-category">{{ categoryLabel(row.category) }}</span></td>
              <td class="maintenance-description">{{ row.title }}</td>
              <td>{{ dateTime(row.requestedAt) }}</td>
              <td><strong>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(row.amount) }}</strong></td>
              <td><span class="tag" :class="statusClass(maintenanceStatusLabel(row.status))">{{ maintenanceStatusLabel(row.status) }}</span></td>
              <td>{{ Number(row.attachmentCount || 0) }} {{ $t('legacy.t_aa9f1ad4f91c') }}</td>
              <td><button v-if="!['completed','cancelled'].includes(row.status)" type="button" class="maintenance-handle-btn" @click.stop="openHandling(row)">{{ $t('legacy.t_fda275e0bcc3') }}</button><span v-else>—</span></td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="pager"><span>{{ $t('legacy.t_3b6ef811b85a') }} {{ filteredRows.length }} {{ $t('legacy.t_86761b63a7bd') }}</span><div class="admin-building-pager"><button :disabled="listPage <= 1" @click="goListPage(listPage - 1)">&lt;</button><button v-for="page in listPages()" :key="page" :class="{ active: page === listPage }" @click="goListPage(page)">{{ page }}</button><button :disabled="listPage >= totalListPages()" @click="goListPage(listPage + 1)">&gt;</button><select v-model.number="listPageSize"><option :value="10">{{ $t('legacy.t_fc6da0e815a1') }}</option><option :value="20">{{ $t('legacy.t_93d673672fa5') }}</option><option :value="50">{{ $t('legacy.t_529930b886d2') }}</option></select></div></div>
    </div>

    <aside class="panel detail-panel maintenance-detail-panel">
      <div v-if="selectedRow" class="detail-card">
        <div class="profile">
          <div class="big-avatar">{{ activeTab === 'expense' ? $t('legacy.t_18d2086d6a02') : $t('legacy.t_ea97fb39f031') }}</div>
          <div><h3>{{ detailTitle }}</h3><p>{{ selectedRow.projectName }} · {{ selectedRow.unitNo }}</p></div>
          <span class="tag" :class="statusClass(detailStatus)">{{ detailStatus }}</span>
        </div>
        <div class="detail-section">
          <h4><span class="num">{{ $t('legacy.t_356a192b7913') }}</span>{{ $t('legacy.t_c738ebbf02dd') }}</h4>
          <div class="kv"><span>{{ activeTab === 'expense' ? $t('legacy.t_f48697f8b6ec') : $t('legacy.t_dde3113a1be2') }}</span><b>{{ activeTab === 'expense' ? selectedRow.occurredOn : dateTime(selectedRow.requestedAt) }}</b></div>
          <div class="kv"><span>{{ $t('legacy.t_16ed763edcda') }}</span><b>{{ categoryLabel(selectedRow.category) }}</b></div>
          <div v-if="selectedRow.workOrderNo" class="kv"><span>{{ $t('legacy.t_64838faf9fb0') }}</span><b>{{ selectedRow.workOrderNo }}</b></div>
          <div class="kv"><span>{{ $t('legacy.t_99f6fe6c41ad') }}</span><b>{{ Number(selectedRow.attachmentCount || 0) }} {{ $t('legacy.t_aa9f1ad4f91c') }}</b></div>
        </div>
        <div class="detail-section">
          <h4><span class="num">{{ $t('legacy.t_da4b9237bacc') }}</span>{{ $t('legacy.t_72f0fd083aba') }}</h4>
          <div class="kv"><span>{{ $t('legacy.t_76c755be2660') }}</span><b>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(detailAmount) }}</b></div>
          <div class="kv"><span>{{ $t('legacy.t_5c0ec3674a79') }}</span><b>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(selectedRow.reserveDeductedAmount) }}</b></div>
          <div v-if="activeTab === 'expense'" class="kv"><span>{{ $t('legacy.t_c6b9a8cfdb21') }}</span><b>{{ paymentMethodLabel(selectedRow.paymentMethod) }}</b></div>
        </div>
        <div class="detail-section">
          <h4><span class="num">{{ $t('legacy.t_77de68daecd8') }}</span>{{ activeTab === 'expense' ? $t('legacy.t_a52dafecf965') : $t('legacy.t_9d4b38186bb3') }}</h4>
          <p class="maintenance-detail-copy">{{ detailCopy }}</p>
          <div v-if="maintenanceDetail" class="kv"><span>{{ $t('legacy.t_2127eb1f484f') }}</span><b>{{ maintenanceDetail.vendorName || $t('legacy.t_02c60c3bdc1f') }}</b></div>
          <div v-if="maintenanceDetail" class="kv"><span>{{ $t('legacy.t_b4126221a8db') }}</span><b>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(maintenanceDetail.actualAmount) }}</b></div>
        </div>
      </div>
      <div v-else class="admin-owner-empty">{{ $t('legacy.t_956595e88a11') }}</div>
    </aside>

    <dialog ref="expenseCreateDialog" class="modal maintenance-create-dialog">
      <form method="dialog" @submit.prevent="submitExpense">
        <div class="modal-head"><div><h3>{{ $t('legacy.t_16c7d92e55ce') }}</h3><small>{{ $t('legacy.t_8ca893baf0d3') }}</small></div><button type="button" class="icon-close" @click="closeExpenseCreate">×</button></div>
        <div v-if="optionsLoading" class="admin-owner-state">{{ $t('legacy.t_c330ff111b3f') }}</div>
        <div v-else class="maintenance-create-body">
          <label class="wide">{{ $t('legacy.t_114246450ff0') }}<select v-model.number="expenseForm.unitId" required><option disabled value="">{{ $t('legacy.t_06dd9be6b9c7') }}</option><option v-for="unit in options.units" :key="unit.unitId" :value="unit.unitId">{{ unit.projectName }} · {{ unit.unitNo }}（{{ unit.ownerName }}）</option></select></label>
          <label>{{ $t('legacy.t_0cf468db12ee') }}<select v-model="expenseForm.category" required><option value="utilities">{{ $t('legacy.t_39e538e57590') }}</option><option value="management">{{ $t('legacy.t_a178daac2527') }}</option><option value="cleaning">{{ $t('legacy.t_00e326047628') }}</option><option value="maintenance">{{ $t('legacy.t_018bde2b7e24') }}</option><option value="other">{{ $t('legacy.t_c90e8ecbb54d') }}</option></select></label>
          <label>{{ $t('legacy.t_21eebaaf5746') }}<input v-model="expenseForm.occurredOn" type="date" required></label>
          <label>{{ $t('legacy.t_338501824154') }}<input v-model.number="expenseForm.amount" type="number" min="0.01" step="0.01" required></label>
          <label>{{ $t('legacy.t_ab2dbd55b3fb') }}<select v-model="expenseForm.settlementMethod" required><option value="reserve" :disabled="!expenseReserveAvailable">{{ $t('legacy.t_ea99cc82f746') }}</option><option value="direct_payment">{{ $t('legacy.t_f5d4d7b78adb') }}</option><option value="unpaid">{{ $t('legacy.t_838553a8b2e6') }}</option></select></label>
          <section v-if="selectedExpenseUnit" class="create-unit-summary wide">
            <span>{{ $t('legacy.t_ef2b2d104853') }}<b>{{ selectedExpenseUnit.ownerName }}</b></span><span>{{ $t('legacy.t_facfa1a1db0d') }}<b>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(selectedExpenseUnit.reserveBalance) }}</b></span><span :class="{ shortage: expenseShortage > 0 }">{{ $t('legacy.t_37c0465513e1') }}<b>{{ expenseShortage > 0 ? `不足 RM ${money(expenseShortage)}` : `RM ${money(expenseReserveAfter)}` }}</b></span>
          </section>
          <label class="wide">{{ $t('legacy.t_a52dafecf965') }}<textarea v-model.trim="expenseForm.description" maxlength="500" rows="4" required :placeholder="$t('legacy.t_520b35536c38')"></textarea></label>
          <p v-if="expenseCreateError" class="admin-property-error wide">{{ expenseCreateError }}</p>
        </div>
        <menu><button type="button" @click="closeExpenseCreate">{{ $t('legacy.t_4d0b4688c787') }}</button><button class="primary-btn" :disabled="expenseSaving || optionsLoading">{{ expenseSaving ? $t('legacy.t_8488ea2522af') : $t('legacy.t_8c962ae7a7fb') }}</button></menu>
      </form>
    </dialog>

    <dialog ref="maintenanceCreateDialog" class="modal maintenance-create-dialog">
      <form method="dialog" @submit.prevent="submitMaintenanceCreate">
        <div class="modal-head"><div><h3>{{ $t('legacy.t_1c14558442e7') }}</h3><small>{{ $t('legacy.t_138f739a4607') }}</small></div><button type="button" class="icon-close" @click="closeMaintenanceCreate">×</button></div>
        <div v-if="optionsLoading" class="admin-owner-state">{{ $t('legacy.t_ff1df444fc44') }}</div>
        <div v-else class="maintenance-create-body">
          <label class="wide">{{ $t('legacy.t_114246450ff0') }}<select v-model.number="maintenanceCreateForm.unitId" required><option disabled value="">{{ $t('legacy.t_06dd9be6b9c7') }}</option><option v-for="unit in options.units" :key="unit.unitId" :value="unit.unitId">{{ unit.projectName }} · {{ unit.unitNo }}（{{ unit.ownerName }}）</option></select></label>
          <label>{{ $t('legacy.t_4e3157d2e547') }}<select v-model="maintenanceCreateForm.category" required><option value="plumbing">{{ $t('legacy.t_925558624144') }}</option><option value="air_conditioning">{{ $t('legacy.t_e7ec651a7c0f') }}</option><option value="electrical">{{ $t('legacy.t_c230e3bc3ab6') }}</option><option value="painting">{{ $t('legacy.t_6a81927031d5') }}</option><option value="other">{{ $t('legacy.t_587c86584e8e') }}</option></select></label>
          <label>{{ $t('legacy.t_2127eb1f484f') }}<select v-model="maintenanceCreateForm.vendorId"><option :value="null">{{ $t('legacy.t_4e4ceafa8ee3') }}</option><option v-for="vendor in options.vendors" :key="vendor.id" :value="vendor.id">{{ vendorOptionLabel(vendor) }}</option></select></label>
          <label>{{ $t('legacy.t_dde3113a1be2') }}<input v-model="maintenanceCreateForm.requestedAt" type="datetime-local" required></label>
          <label class="wide">{{ $t('legacy.t_38ca8573c24b') }}<input v-model.trim="maintenanceCreateForm.title" maxlength="180" required :placeholder="$t('legacy.t_332ded447fbd')"></label>
          <label class="wide">{{ $t('legacy.t_5586fd550c39') }}<textarea v-model.trim="maintenanceCreateForm.description" maxlength="1000" rows="4" :placeholder="$t('legacy.t_07202ef775ba')"></textarea></label>
          <p v-if="maintenanceCreateError" class="admin-property-error wide">{{ maintenanceCreateError }}</p>
        </div>
        <menu><button type="button" @click="closeMaintenanceCreate">{{ $t('legacy.t_4d0b4688c787') }}</button><button class="primary-btn" :disabled="maintenanceCreateSaving || optionsLoading">{{ maintenanceCreateSaving ? $t('legacy.t_2cd5496ec548') : $t('legacy.t_2a90d4ff462c') }}</button></menu>
      </form>
    </dialog>

    <dialog ref="handlingDialog" class="modal maintenance-handling-dialog">
      <form method="dialog" @submit.prevent="submitHandling">
        <div class="modal-head"><div><h3>{{ $t('legacy.t_3761878742b8') }}</h3><small>{{ handlingRow?.workOrderNo }} · {{ handlingRow?.projectName }} {{ handlingRow?.unitNo }}</small></div><button type="button" class="icon-close" @click="closeHandling">×</button></div>
        <div v-if="handlingLoading" class="admin-owner-state">{{ $t('legacy.t_e32212dc8516') }}</div>
        <div v-else class="maintenance-handling-body">
          <section class="handling-reserve-summary">
            <div><span>{{ $t('legacy.t_82d54c45b8dd') }}</span><strong>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(handlingInfo.reserveBalance) }}</strong></div>
            <div><span>{{ $t('legacy.t_fcd532741253') }}</span><strong>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(handlingInfo.reserveDeductedAmount) }}</strong></div>
            <div><span>{{ $t('legacy.t_3d416cf138b0') }}</span><strong>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(handlingForm.actualAmount) }}</strong></div>
            <div :class="{ shortage: reserveShortage > 0 }"><span>{{ $t('legacy.t_3c9d8e73179c') }}</span><strong>{{ reserveShortage > 0 ? `不足 RM ${money(reserveShortage)}` : `RM ${money(reserveAfter)}` }}</strong></div>
          </section>

          <label>{{ $t('legacy.t_3b2b90c8aff2') }}<input v-model.number="handlingForm.actualAmount" type="number" min="0.01" step="0.01" required></label>
          <label>{{ $t('legacy.t_ab2dbd55b3fb') }}<select v-model="handlingForm.settlementMethod" required><option value="reserve" :disabled="!handlingInfo.reserveAccountAvailable || reserveShortage > 0">{{ $t('legacy.t_ea99cc82f746') }}</option><option value="direct_payment">{{ $t('legacy.t_3afbca59e929') }}</option></select><small v-if="reserveShortage > 0" class="handling-warning">{{ $t('legacy.t_c1130213b25d') }}</small></label>

          <div class="handling-photo-grid">
            <label class="handling-photo-field"><span>{{ $t('legacy.t_c769e4276274') }} <b>{{ $t('legacy.t_df7b17a1e3d8') }}</b></span><input type="file" accept="image/jpeg,image/png" multiple @change="setHandlingFiles('before', $event)"><small>{{ $t('legacy.t_58705ba10b3c') }} {{ handlingInfo.beforePhotoCount || 0 }} {{ $t('legacy.t_e1eeb732c04c') }} {{ handlingFiles.before.length }} {{ $t('legacy.t_6dcb656fb70d') }}</small></label>
            <label class="handling-photo-field"><span>{{ $t('legacy.t_e4a60df0e601') }} <b>{{ $t('legacy.t_df7b17a1e3d8') }}</b></span><input type="file" accept="image/jpeg,image/png" multiple @change="setHandlingFiles('after', $event)"><small>{{ $t('legacy.t_58705ba10b3c') }} {{ handlingInfo.afterPhotoCount || 0 }} {{ $t('legacy.t_e1eeb732c04c') }} {{ handlingFiles.after.length }} {{ $t('legacy.t_6dcb656fb70d') }}</small></label>
            <label class="handling-photo-field"><span>{{ $t('legacy.t_addd26b636b6') }}</span><input type="file" accept="image/jpeg,image/png,application/pdf" multiple @change="setHandlingFiles('invoice', $event)"><small>{{ $t('legacy.t_39c9779e3d16') }} {{ handlingFiles.invoice.length }} {{ $t('legacy.t_7d54bc69bbe4') }}</small></label>
          </div>

          <label class="handling-note">{{ $t('legacy.t_51d8d317d95c') }}<textarea v-model.trim="handlingForm.completionNote" maxlength="500" rows="4" required :placeholder="$t('legacy.t_35d405d839f4')"></textarea></label>
          <p v-if="handlingError" class="admin-property-error wide">{{ handlingError }}</p>
        </div>
        <menu><button type="button" @click="closeHandling">{{ $t('legacy.t_4d0b4688c787') }}</button><button class="primary-btn" :disabled="handlingSaving || handlingLoading">{{ handlingSaving ? $t('legacy.t_1e038f9b55ec') : $t('legacy.t_dd2218bd6344') }}</button></menu>
      </form>
    </dialog>
  </section>
</template>

<script>
import { completeAdminMaintenance, createAdminExpense, createAdminMaintenance, fetchAdminExpenses, fetchAdminMaintenanceDetail, fetchAdminMaintenanceHandling, fetchAdminMaintenanceOptions, uploadAdminMaintenancePhotos } from '../services/propertyApi';

const STATUS_MAP = { submitted: '待處理', assigned: '已指派', in_progress: '處理中', inspection: '待驗收', completed: '已完成', cancelled: '已取消' };
const CATEGORY_MAP = { utilities: '水電費', management: '管理費', cleaning: '清潔費', maintenance: '維修費', plumbing: '管道維修', air_conditioning: '冷氣維修', electrical: '電器維修', painting: '油漆修繕', other: '其他支出' };

export default {
  inject: ['page'],
  data() {
    return { activeTab: 'expense', listPage: 1, listPageSize: 10, response: { summary: {}, properties: [], expenses: [], maintenance: [] }, loading: false, errorMessage: '', selectedKey: '', maintenanceDetail: null, requestSerial: 0, options: { units: [], vendors: [] }, optionsLoading: false, expenseForm: this.emptyExpenseForm(), expenseSaving: false, expenseCreateError: '', maintenanceCreateForm: this.emptyMaintenanceForm(), maintenanceCreateSaving: false, maintenanceCreateError: '', handlingRow: null, handlingInfo: {}, handlingForm: { actualAmount: 0, settlementMethod: 'reserve', completionNote: '' }, handlingFiles: { before: [], after: [], invoice: [] }, handlingLoading: false, handlingSaving: false, handlingError: '' };
  },
  computed: {
    rows() { return this.activeTab === 'expense' ? this.response.expenses || [] : this.response.maintenance || []; },
    filteredRows() {
      const keyword = String(this.page.moduleSearch || this.page.globalSearch || '').trim().toLowerCase();
      const project = String(this.page.projectFilter || '');
      const status = String(this.page.statusFilter || '');
      return this.rows.filter(row => {
        if (project && !project.includes('全部') && row.projectName !== project) return false;
        if (status && !status.includes('全部')) {
          const label = this.activeTab === 'expense' ? this.paymentLabel(row.paymentStatus) : this.maintenanceStatusLabel(row.status);
          if (label !== status && !(status === '待處理' && ['待處理', '已指派', '處理中', '待驗收'].includes(label))) return false;
        }
        if (!keyword) return true;
        return [row.workOrderNo, row.projectName, row.unitNo, row.category, row.title, row.description].some(value => String(value || '').toLowerCase().includes(keyword));
      });
    },
    selectedRow() { const rows = this.pagedRows(); return rows.find(row => this.rowKey(row) === this.selectedKey) || rows[0] || null; },
    detailTitle() { return this.activeTab === 'expense' ? this.selectedRow?.description || this.$t('legacy.t_3d6f21b83ce4') : this.selectedRow?.title || this.$t('legacy.t_27f65139d152'); },
    detailStatus() { return this.activeTab === 'expense' ? this.paymentLabel(this.selectedRow?.paymentStatus) : this.maintenanceStatusLabel(this.selectedRow?.status); },
    detailAmount() { return this.activeTab === 'expense' ? this.selectedRow?.amount : this.maintenanceDetail?.actualAmount || this.selectedRow?.amount; },
    detailCopy() { return this.maintenanceDetail?.description || this.selectedRow?.description || this.selectedRow?.title || '—'; },
    requiredReserveDebit() { return Math.max(0, Number(this.handlingForm.actualAmount || 0) - Number(this.handlingInfo.reserveDeductedAmount || 0)); },
    reserveShortage() { return Math.max(0, this.requiredReserveDebit - Number(this.handlingInfo.reserveBalance || 0)); },
    reserveAfter() { return Math.max(0, Number(this.handlingInfo.reserveBalance || 0) - this.requiredReserveDebit); },
    selectedExpenseUnit() { return this.options.units.find(unit => Number(unit.unitId) === Number(this.expenseForm.unitId)) || null; },
    expenseShortage() { return Math.max(0, Number(this.expenseForm.amount || 0) - Number(this.selectedExpenseUnit?.reserveBalance || 0)); },
    expenseReserveAfter() { return Math.max(0, Number(this.selectedExpenseUnit?.reserveBalance || 0) - Number(this.expenseForm.amount || 0)); },
    expenseReserveAvailable() { return Boolean(this.selectedExpenseUnit?.reserveAccountId) && this.expenseShortage === 0; }
  },
  watch: {
    'page.projectFilter'() { this.resetListPage(); },
    'page.statusFilter'() { this.resetListPage(); },
    'page.moduleSearch'() { this.resetListPage(); },
    listPageSize() { this.resetListPage(); },
    'page.dateStart'() { this.loadData(); },
    'page.dateEnd'() { this.loadData(); },
    'page.adminExpenseCreateNonce'() { this.openExpenseCreate(); },
    'page.adminMaintenanceCreateNonce'() { this.openMaintenanceCreate(); }
  },
  mounted() { this.loadData(); },
  methods: {
    pagedRows() { const size = Number(this.listPageSize || 10); const page = Number(this.listPage || 1); return this.filteredRows.slice((page - 1) * size, page * size); },
    totalListPages() { return Math.max(1, Math.ceil(this.filteredRows.length / Number(this.listPageSize || 10))); },
    listPages() { return Array.from({ length: this.totalListPages() }, (_, index) => index + 1); },
    goListPage(page) {
      const nextPage = Math.min(Math.max(Number(page) || 1, 1), this.totalListPages());
      this.listPage = nextPage;
      this.ensureSelection();
    },
    resetListPage() { this.listPage = 1; this.ensureSelection(); },
    today() { return new Date().toLocaleDateString('en-CA'); },
    localDateTime() { const date = new Date(); date.setMinutes(date.getMinutes() - date.getTimezoneOffset()); return date.toISOString().slice(0, 16); },
    emptyExpenseForm() { return { unitId: '', category: 'utilities', description: '', amount: null, occurredOn: this.today(), settlementMethod: 'reserve' }; },
    emptyMaintenanceForm() { return { unitId: '', vendorId: null, category: 'plumbing', title: '', description: '', requestedAt: this.localDateTime() }; },
    async loadOptions() {
      this.optionsLoading = true;
      try { this.options = await fetchAdminMaintenanceOptions(); }
      catch (error) { throw new Error(error.message || '無法讀取單位與服務商資料'); }
      finally { this.optionsLoading = false; }
    },
    async openExpenseCreate() {
      this.expenseForm = this.emptyExpenseForm(); this.expenseCreateError = ''; this.$refs.expenseCreateDialog?.showModal();
      try { await this.loadOptions(); } catch (error) { this.expenseCreateError = error.message; }
    },
    closeExpenseCreate() { this.$refs.expenseCreateDialog?.close(); this.expenseCreateError = ''; },
    async submitExpense() {
      if (this.expenseForm.settlementMethod === 'reserve' && !this.expenseReserveAvailable) { this.expenseCreateError = this.expenseShortage > 0 ? '預備金不足，請選擇已直接支付或尚未付款。' : '此單位沒有可用的預備金帳戶。'; return; }
      this.expenseSaving = true; this.expenseCreateError = '';
      try { const result = await createAdminExpense(this.expenseForm); this.closeExpenseCreate(); this.activeTab = 'expense'; await this.loadData(); this.page.showToast?.(`支出 ${result.referenceNo} 已建立`); }
      catch (error) { this.expenseCreateError = error.message || '新增支出失敗'; }
      finally { this.expenseSaving = false; }
    },
    async openMaintenanceCreate() {
      this.maintenanceCreateForm = this.emptyMaintenanceForm(); this.maintenanceCreateError = ''; this.$refs.maintenanceCreateDialog?.showModal();
      try { await this.loadOptions(); } catch (error) { this.maintenanceCreateError = error.message; }
    },
    closeMaintenanceCreate() { this.$refs.maintenanceCreateDialog?.close(); this.maintenanceCreateError = ''; },
    async submitMaintenanceCreate() {
      this.maintenanceCreateSaving = true; this.maintenanceCreateError = '';
      try { const result = await createAdminMaintenance(this.maintenanceCreateForm); this.closeMaintenanceCreate(); this.activeTab = 'maintenance'; await this.loadData(); this.page.showToast?.(`維修工單 ${result.referenceNo} 已建立`); }
      catch (error) { this.maintenanceCreateError = error.message || '新增維修工單失敗'; }
      finally { this.maintenanceCreateSaving = false; }
    },
    async loadData() {
      const serial = ++this.requestSerial; this.loading = true; this.errorMessage = '';
      try {
        const data = await fetchAdminExpenses({ startDate: this.page.dateStart, endDate: this.page.dateEnd });
        if (serial !== this.requestSerial) return;
        this.response = data || { summary: {}, properties: [], expenses: [], maintenance: [] };
        this.page.adminMaintenanceProjects = (this.response.properties || []).map(item => item.name);
        this.page.adminMaintenanceMetrics = this.metrics(this.response.summary || {});
        this.ensureSelection();
      } catch (error) {
        if (serial !== this.requestSerial) return;
        this.errorMessage = error.message || 'API request failed'; this.page.adminMaintenanceMetrics = null;
      } finally { if (serial === this.requestSerial) this.loading = false; }
    },
    metrics(summary) {
      const trend = value => `${Number(value || 0) >= 0 ? '↑' : '↓'} ${Math.abs(Number(value || 0)).toFixed(1)}% ${this.$t('legacy.t_54d33d40ca98')}`;
      const month = new Date().toISOString().slice(0, 7);
      const categoryAmount = category => (this.response.expenses || [])
        .filter(item => item.category === category && String(item.occurredOn || '').startsWith(month))
        .reduce((total, item) => total + Number(item.amount || 0), 0);
      return [
        { label: this.$t('legacy.t_b9ed7e3ca959'), value: `RM ${this.money(summary.monthlyExpense)}`, delta: trend(summary.expenseChangePercent), trend: Number(summary.expenseChangePercent) <= 0 ? 'up' : 'down' },
        { label: this.$t('legacy.t_d9a7334b1f8e'), value: `${Math.abs(Number(summary.expenseChangePercent || 0)).toFixed(1)}%`, delta: this.$t('legacy.t_54d33d40ca98'), trend: Number(summary.expenseChangePercent) <= 0 ? 'up' : 'down' },
        { label: this.$t('legacy.t_ead725c34999'), value: `RM ${this.money(summary.monthlyMaintenanceExpense)}`, delta: trend(summary.maintenanceChangePercent), trend: Number(summary.maintenanceChangePercent) <= 0 ? 'up' : 'down' },
        { label: this.$t('legacy.t_273a08cfddf4'), value: `RM ${this.money(categoryAmount('utilities'))}`, delta: this.$t('ui.liveDatabaseStatistics'), trend: 'up' },
        { label: this.$t('legacy.t_b991af839d9a'), value: `RM ${this.money(categoryAmount('management'))}`, delta: this.$t('ui.liveDatabaseStatistics'), trend: 'up' },
        { label: this.$t('legacy.t_8707473743ab'), value: this.$t('ui.records', { count: summary.pendingMaintenanceCount || 0 }), delta: this.$t('legacy.t_1da8f14794c1'), trend: Number(summary.pendingMaintenanceCount) ? 'down' : 'up' },
        { label: this.$t('legacy.t_c1b0ddc685b1'), value: `RM ${this.money(summary.reserveDeductedAmount)}`, delta: `${summary.reserveDebitCount || 0} ${this.$t('legacy.t_0b0c218f4c5d')}`, trend: 'up' }
      ];
    },
    selectTab(tab) { this.activeTab = tab; this.listPage = 1; this.page.statusFilter = '全部狀態'; this.maintenanceDetail = null; this.selectedKey = ''; this.ensureSelection(); },
    ensureSelection() { this.$nextTick(() => { const row = this.pagedRows()[0]; this.selectedKey = row ? this.rowKey(row) : ''; if (row) this.loadDetail(row); else this.maintenanceDetail = null; }); },
    selectRow(row) { this.selectedKey = this.rowKey(row); this.loadDetail(row); },
    rowKey(row) { return `${this.activeTab}-${row.id}`; },
    async loadDetail(row) { const id = this.activeTab === 'maintenance' ? row.id : row.workOrderId; if (!id) { this.maintenanceDetail = null; return; } try { this.maintenanceDetail = await fetchAdminMaintenanceDetail(id); } catch { this.maintenanceDetail = null; } },
    async openHandling(row) {
      this.handlingRow = row; this.handlingLoading = true; this.handlingError = ''; this.handlingFiles = { before: [], after: [], invoice: [] };
      this.handlingForm = { actualAmount: Number(row.amount || 0), settlementMethod: 'reserve', completionNote: '' };
      this.$refs.handlingDialog.showModal();
      try {
        this.handlingInfo = await fetchAdminMaintenanceHandling(row.id);
        if (!this.handlingInfo.reserveAccountAvailable || this.reserveShortage > 0) this.handlingForm.settlementMethod = 'direct_payment';
      } catch (error) { this.handlingError = error.message || '無法讀取工單處理資料'; }
      finally { this.handlingLoading = false; }
    },
    closeHandling() { this.$refs.handlingDialog?.close(); this.handlingRow = null; this.handlingError = ''; },
    setHandlingFiles(type, event) {
      const selected = Array.from(event.target.files || []);
      const current = this.handlingFiles[type] || [];
      const merged = [...current, ...selected].filter((file, index, files) => files.findIndex(item =>
        item.name === file.name && item.size === file.size && item.lastModified === file.lastModified) === index);
      if (merged.length > 6) {
        this.handlingError = '每類最多選擇 6 份文件';
      } else if (this.handlingError === '每類最多選擇 6 份文件') {
        this.handlingError = '';
      }
      this.handlingFiles[type] = merged.slice(0, 6);
    },
    async submitHandling() {
      if (!this.handlingRow) return;
      const hasBefore = Number(this.handlingInfo.beforePhotoCount || 0) > 0 || this.handlingFiles.before.length > 0;
      const hasAfter = Number(this.handlingInfo.afterPhotoCount || 0) > 0 || this.handlingFiles.after.length > 0;
      if (!hasBefore || !hasAfter) { this.handlingError = '維修前與維修後照片各至少需要一張'; return; }
      if (this.handlingForm.settlementMethod === 'reserve' && this.reserveShortage > 0) { this.handlingError = '預備金不足，請改用直接支付'; return; }
      this.handlingSaving = true; this.handlingError = '';
      try {
        if (this.handlingFiles.before.length) await uploadAdminMaintenancePhotos(this.handlingRow.id, 'before_photo', this.handlingFiles.before);
        if (this.handlingFiles.after.length) await uploadAdminMaintenancePhotos(this.handlingRow.id, 'after_photo', this.handlingFiles.after);
        if (this.handlingFiles.invoice.length) await uploadAdminMaintenancePhotos(this.handlingRow.id, 'invoice', this.handlingFiles.invoice);
        await completeAdminMaintenance(this.handlingRow.id, this.handlingForm);
        this.closeHandling(); await this.loadData();
      } catch (error) { this.handlingError = error.message || '維修工單處理失敗'; }
      finally { this.handlingSaving = false; }
    },
    categoryLabel(value) { return this.$t({ utilities: 'legacy.t_39e538e57590', management: 'legacy.t_a178daac2527', cleaning: 'legacy.t_00e326047628', maintenance: 'legacy.t_018bde2b7e24', plumbing: 'legacy.t_925558624144', air_conditioning: 'legacy.t_e7ec651a7c0f', electrical: 'legacy.t_c230e3bc3ab6', painting: 'legacy.t_6a81927031d5', other: 'legacy.t_c90e8ecbb54d' }[value] || 'legacy.t_c90e8ecbb54d'); },
    maintenanceStatusLabel(value) { return this.$t({ submitted: 'legacy.t_3b8dcefe78c2', assigned: 'legacy.t_89f43720c2e8', in_progress: 'legacy.t_1e038f9b55ec', inspection: 'legacy.t_7421844828c2', completed: 'legacy.t_e99b48a29bdf', cancelled: 'legacy.t_a5ffdc95eeb0' }[value] || 'legacy.t_3b8dcefe78c2'); },
    paymentLabel(value) { return this.$t({ paid: 'legacy.t_b35b40fe4f61', partial: 'legacy.t_a66b74573539', unpaid: 'legacy.t_20825179461a', pending: 'legacy.t_20825179461a' }[value] || 'legacy.t_20825179461a'); },
    paymentMethodLabel(value) { const key = { bank_transfer: 'legacy.t_789957b63e04', online_payment: 'legacy.t_61179c3c479b', reserve_account: 'legacy.t_c1b0ddc685b1', direct_payment: 'legacy.t_164917d2ce3b', cash: 'legacy.t_e3ca5905c270' }[value]; return key ? this.$t(key) : '—'; },
    vendorOptionLabel(vendor) { return vendor.contactName ? `${vendor.name} · ${vendor.contactName}` : vendor.name; },
    statusClass(value) { if (['已完成', '已付款', 'Completed', 'Paid', '已完成', '已付款'].includes(value)) return 'green'; if (['已取消', 'Canceled', '已取消'].includes(value)) return 'red'; if (['處理中', '部分付款', '已指派', '待驗收', 'Processing…', 'Partially Paid', 'Assigned', 'Pending Inspection', '处理中…', '部分付款', '已指派', '待验收'].includes(value)) return 'orange'; return 'gray'; },
    money(value) { return Number(value || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); },
    dateTime(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '—'; }
  }
};
</script>

<style scoped>
.admin-maintenance-workspace { align-items: stretch; }
.maintenance-list-panel { min-height: 590px; }
.maintenance-tabs { display: flex; padding: 3px; border: 1px solid #dce4ee; border-radius: 8px; background: #f1f5f9; }
.maintenance-tabs button { height: 28px; padding: 0 13px; border: 0; border-radius: 6px; background: transparent; color: #64748b; font-size: 11px; font-weight: 800; cursor: pointer; }
.maintenance-tabs button.active { background: #0b3768; color: #fff; box-shadow: 0 2px 6px rgba(11,55,104,.18); }
.maintenance-table-wrap { min-height: 454px; }
.maintenance-table-wrap td { height: 48px; }
.maintenance-table-wrap td strong { display: block; color: #102447; font-size: 11px; }
.maintenance-table-wrap td small { display: block; margin-top: 3px; color: #8491a3; font-size: 9px; }
.maintenance-description { max-width: 220px; overflow: hidden; text-overflow: ellipsis; }
.maintenance-category { display: inline-flex; padding: 3px 7px; border-radius: 12px; background: #edf4fb; color: #24517f; font-size: 9px; font-weight: 800; }
.maintenance-handle-btn { height: 27px; padding: 0 12px; border: 1px solid #0b3768; border-radius: 6px; background: #0b3768; color: #fff; font-size: 10px; font-weight: 900; cursor: pointer; }
.maintenance-detail-panel { min-height: 590px; }
.maintenance-detail-copy { margin: 0 0 12px; color: #536176; font-size: 11px; line-height: 1.65; }
.money-gold { color: #b7791f; font-weight: 800; }
.maintenance-create-dialog { width: min(700px, calc(100vw - 32px)); }
.maintenance-create-body { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; padding: 18px 20px; }
.maintenance-create-body > label { display: grid; gap: 6px; color: #334155; font-size: 11px; font-weight: 800; }
.maintenance-create-body .wide { grid-column: 1 / -1; }
.maintenance-create-body input,.maintenance-create-body select,.maintenance-create-body textarea { width: 100%; border: 1px solid #dbe3ed; border-radius: 7px; background: #fff; color: #17233a; font: inherit; }
.maintenance-create-body input,.maintenance-create-body select { height: 38px; padding: 0 10px; }
.maintenance-create-body textarea { padding: 10px; resize: vertical; }
.create-unit-summary { display: grid; grid-template-columns: repeat(3,1fr); gap: 10px; padding: 12px; border: 1px solid #dce5ef; border-radius: 9px; background: #f7f9fc; color: #6b778b; font-size: 10px; }
.create-unit-summary span { padding-right: 10px; border-right: 1px solid #e1e7ef; }.create-unit-summary span:last-child { border-right: 0; }.create-unit-summary b { color: #102447; }.create-unit-summary .shortage,.create-unit-summary .shortage b { color: #dc3f3f; }
.maintenance-handling-dialog { width: min(720px, calc(100vw - 32px)); }
.maintenance-handling-body { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; padding: 18px 20px; }
.maintenance-handling-body > label { display: grid; gap: 6px; color: #334155; font-size: 11px; font-weight: 800; }
.maintenance-handling-body input,.maintenance-handling-body select,.maintenance-handling-body textarea { width: 100%; border: 1px solid #dbe3ed; border-radius: 7px; background: #fff; color: #17233a; font: inherit; }
.maintenance-handling-body input,.maintenance-handling-body select { height: 36px; padding: 0 10px; }.maintenance-handling-body textarea { padding: 10px; resize: vertical; }
.handling-reserve-summary { grid-column: 1 / -1; display: grid; grid-template-columns: repeat(4,1fr); gap: 8px; padding: 12px; border: 1px solid #dce5ef; border-radius: 9px; background: #f7f9fc; }
.handling-reserve-summary div { display: grid; gap: 5px; padding-right: 8px; border-right: 1px solid #e1e7ef; }.handling-reserve-summary div:last-child { border-right: 0; }.handling-reserve-summary span { color: #748196; font-size: 9px; }.handling-reserve-summary strong { color: #102447; font-size: 13px; }.handling-reserve-summary .shortage strong,.handling-warning { color: #dc3f3f; }
.handling-photo-grid { grid-column: 1 / -1; display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }.handling-photo-field { display: grid; gap: 7px; padding: 12px; border: 1px dashed #bdc9d8; border-radius: 8px; background: #fbfcfe; }.handling-photo-field span { font-size: 11px; font-weight: 900; }.handling-photo-field b { color: #dc3f3f; }.handling-photo-field small { color: #7b8798; font-size: 9px; }.handling-photo-field input { height: auto; padding: 7px; font-size: 10px; }
.handling-note { grid-column: 1 / -1; }.handling-warning { font-size: 9px; font-weight: 700; }
@media (max-width: 820px) { .maintenance-tabs button { padding-inline: 9px; }.maintenance-create-body { grid-template-columns: 1fr; }.maintenance-create-body .wide { grid-column: auto; }.create-unit-summary { grid-template-columns: 1fr; }.create-unit-summary span { border-right: 0; } }
</style>
