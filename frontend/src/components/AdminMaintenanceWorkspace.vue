<template>
  <section class="content-grid admin-maintenance-workspace">
    <div class="panel table-panel maintenance-list-panel">
      <div class="panel-head">
        <div><h2>收支與維修列表</h2><span>{{ filteredRows.length }} records · 資料庫即時資料</span></div>
        <div class="maintenance-tabs">
          <button type="button" :class="{ active: activeTab === 'expense' }" @click="selectTab('expense')">支出記錄</button>
          <button type="button" :class="{ active: activeTab === 'maintenance' }" @click="selectTab('maintenance')">維修工單</button>
        </div>
      </div>

      <div v-if="loading" class="admin-owner-state">正在載入資料庫資料…</div>
      <div v-else-if="errorMessage" class="admin-owner-state error"><strong>收支與維修資料載入失敗</strong><span>{{ errorMessage }}</span><button @click="loadData">重新載入</button></div>
      <div v-else class="table-wrap maintenance-table-wrap">
        <table v-if="activeTab === 'expense'">
          <thead><tr><th>日期</th><th>建案／單位</th><th>支出類別</th><th>說明</th><th>金額</th><th>預備金扣除</th><th>付款狀態</th><th>附件</th></tr></thead>
          <tbody>
            <tr v-if="!filteredRows.length"><td colspan="8" class="admin-owner-empty">目前沒有符合條件的支出記錄</td></tr>
            <tr v-for="row in filteredRows" :key="`expense-${row.id}`" :class="{ selected: selectedKey === rowKey(row) }" @click="selectRow(row)">
              <td>{{ row.occurredOn || '—' }}</td>
              <td><strong>{{ row.projectName }}</strong><small>{{ row.unitNo }}</small></td>
              <td><span class="maintenance-category">{{ categoryLabel(row.category) }}</span></td>
              <td class="maintenance-description">{{ row.description }}</td>
              <td><strong>RM {{ money(row.amount) }}</strong></td>
              <td :class="{ 'money-gold': Number(row.reserveDeductedAmount) > 0 }">RM {{ money(row.reserveDeductedAmount) }}</td>
              <td><span class="tag" :class="statusClass(paymentLabel(row.paymentStatus))">{{ paymentLabel(row.paymentStatus) }}</span></td>
              <td>{{ Number(row.attachmentCount || 0) }} 份</td>
            </tr>
          </tbody>
        </table>

        <table v-else>
          <thead><tr><th>工單編號</th><th>建案／單位</th><th>維修類別</th><th>事項</th><th>報修時間</th><th>費用</th><th>處理狀態</th><th>附件</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-if="!filteredRows.length"><td colspan="9" class="admin-owner-empty">目前沒有符合條件的維修工單</td></tr>
            <tr v-for="row in filteredRows" :key="`maintenance-${row.id}`" :class="{ selected: selectedKey === rowKey(row) }" @click="selectRow(row)">
              <td><strong>{{ row.workOrderNo }}</strong></td>
              <td><strong>{{ row.projectName }}</strong><small>{{ row.unitNo }}</small></td>
              <td><span class="maintenance-category">{{ categoryLabel(row.category) }}</span></td>
              <td class="maintenance-description">{{ row.title }}</td>
              <td>{{ dateTime(row.requestedAt) }}</td>
              <td><strong>RM {{ money(row.amount) }}</strong></td>
              <td><span class="tag" :class="statusClass(maintenanceStatusLabel(row.status))">{{ maintenanceStatusLabel(row.status) }}</span></td>
              <td>{{ Number(row.attachmentCount || 0) }} 份</td>
              <td><button v-if="!['completed','cancelled'].includes(row.status)" type="button" class="maintenance-handle-btn" @click.stop="openHandling(row)">處理</button><span v-else>—</span></td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="pager"><span>共 {{ filteredRows.length }} records</span><div><button disabled>&lt;</button><button class="active">1</button><button disabled>&gt;</button></div></div>
    </div>

    <aside class="panel detail-panel maintenance-detail-panel">
      <div v-if="selectedRow" class="detail-card">
        <div class="profile">
          <div class="big-avatar">{{ activeTab === 'expense' ? '支' : '修' }}</div>
          <div><h3>{{ detailTitle }}</h3><p>{{ selectedRow.projectName }} · {{ selectedRow.unitNo }}</p></div>
          <span class="tag" :class="statusClass(detailStatus)">{{ detailStatus }}</span>
        </div>
        <div class="detail-section">
          <h4><span class="num">1</span>基本資料</h4>
          <div class="kv"><span>{{ activeTab === 'expense' ? '發生日' : '報修時間' }}</span><b>{{ activeTab === 'expense' ? selectedRow.occurredOn : dateTime(selectedRow.requestedAt) }}</b></div>
          <div class="kv"><span>類別</span><b>{{ categoryLabel(selectedRow.category) }}</b></div>
          <div v-if="selectedRow.workOrderNo" class="kv"><span>工單編號</span><b>{{ selectedRow.workOrderNo }}</b></div>
          <div class="kv"><span>附件</span><b>{{ Number(selectedRow.attachmentCount || 0) }} 份</b></div>
        </div>
        <div class="detail-section">
          <h4><span class="num">2</span>金額資訊</h4>
          <div class="kv"><span>記錄金額</span><b>RM {{ money(detailAmount) }}</b></div>
          <div class="kv"><span>預備金扣除</span><b>RM {{ money(selectedRow.reserveDeductedAmount) }}</b></div>
          <div v-if="activeTab === 'expense'" class="kv"><span>付款方式</span><b>{{ paymentMethodLabel(selectedRow.paymentMethod) }}</b></div>
        </div>
        <div class="detail-section">
          <h4><span class="num">3</span>{{ activeTab === 'expense' ? '支出說明' : '維修詳情' }}</h4>
          <p class="maintenance-detail-copy">{{ detailCopy }}</p>
          <div v-if="maintenanceDetail" class="kv"><span>服務商</span><b>{{ maintenanceDetail.vendorName || '尚未指派' }}</b></div>
          <div v-if="maintenanceDetail" class="kv"><span>預估費用</span><b>RM {{ money(maintenanceDetail.estimatedAmount) }}</b></div>
          <div v-if="maintenanceDetail" class="kv"><span>實際費用</span><b>RM {{ money(maintenanceDetail.actualAmount) }}</b></div>
        </div>
      </div>
      <div v-else class="admin-owner-empty">目前沒有可查看的資料</div>
    </aside>

    <dialog ref="expenseCreateDialog" class="modal maintenance-create-dialog">
      <form method="dialog" @submit.prevent="submitExpense">
        <div class="modal-head"><div><h3>新增支出</h3><small>建立支出記錄，並依選擇從預備金扣款或記錄付款狀態</small></div><button type="button" class="icon-close" @click="closeExpenseCreate">×</button></div>
        <div v-if="optionsLoading" class="admin-owner-state">正在讀取單位與預備金資料…</div>
        <div v-else class="maintenance-create-body">
          <label class="wide">建案／單位<select v-model.number="expenseForm.unitId" required><option disabled value="">請選擇單位</option><option v-for="unit in options.units" :key="unit.unitId" :value="unit.unitId">{{ unit.projectName }} · {{ unit.unitNo }}（{{ unit.ownerName }}）</option></select></label>
          <label>支出類別<select v-model="expenseForm.category" required><option value="utilities">水電費</option><option value="management">管理費</option><option value="cleaning">清潔費</option><option value="maintenance">維修費</option><option value="other">其他支出</option></select></label>
          <label>支出日期<input v-model="expenseForm.occurredOn" type="date" required></label>
          <label>金額（RM）<input v-model.number="expenseForm.amount" type="number" min="0.01" step="0.01" required></label>
          <label>結算方式<select v-model="expenseForm.settlementMethod" required><option value="reserve" :disabled="!expenseReserveAvailable">從業主預備金扣除</option><option value="direct_payment">已直接支付</option><option value="unpaid">尚未付款</option></select></label>
          <section v-if="selectedExpenseUnit" class="create-unit-summary wide">
            <span>業主：<b>{{ selectedExpenseUnit.ownerName }}</b></span><span>目前預備金：<b>RM {{ money(selectedExpenseUnit.reserveBalance) }}</b></span><span :class="{ shortage: expenseShortage > 0 }">扣款後：<b>{{ expenseShortage > 0 ? `不足 RM ${money(expenseShortage)}` : `RM ${money(expenseReserveAfter)}` }}</b></span>
          </section>
          <label class="wide">支出說明<textarea v-model.trim="expenseForm.description" maxlength="500" rows="4" required placeholder="例如：2026 年 7 月公共區域水電費"></textarea></label>
          <p v-if="expenseCreateError" class="admin-property-error wide">{{ expenseCreateError }}</p>
        </div>
        <menu><button type="button" @click="closeExpenseCreate">取消</button><button class="primary-btn" :disabled="expenseSaving || optionsLoading">{{ expenseSaving ? '儲存中…' : '建立支出' }}</button></menu>
      </form>
    </dialog>

    <dialog ref="maintenanceCreateDialog" class="modal maintenance-create-dialog">
      <form method="dialog" @submit.prevent="submitMaintenanceCreate">
        <div class="modal-head"><div><h3>新增維修</h3><small>先建立待處理工單；完工時再上傳前後照片並結算費用</small></div><button type="button" class="icon-close" @click="closeMaintenanceCreate">×</button></div>
        <div v-if="optionsLoading" class="admin-owner-state">正在讀取單位與服務商資料…</div>
        <div v-else class="maintenance-create-body">
          <label class="wide">建案／單位<select v-model.number="maintenanceCreateForm.unitId" required><option disabled value="">請選擇單位</option><option v-for="unit in options.units" :key="unit.unitId" :value="unit.unitId">{{ unit.projectName }} · {{ unit.unitNo }}（{{ unit.ownerName }}）</option></select></label>
          <label>維修類別<select v-model="maintenanceCreateForm.category" required><option value="plumbing">管道維修</option><option value="air_conditioning">冷氣維修</option><option value="electrical">電器維修</option><option value="painting">油漆修繕</option><option value="other">其他維修</option></select></label>
          <label>服務商<select v-model="maintenanceCreateForm.vendorId"><option :value="null">尚未指派</option><option v-for="vendor in options.vendors" :key="vendor.id" :value="vendor.id">{{ vendor.name }}{{ vendor.contactName ? ` · ${vendor.contactName}` : '' }}</option></select></label>
          <label>報修時間<input v-model="maintenanceCreateForm.requestedAt" type="datetime-local" required></label>
          <label>預估金額（RM）<input v-model.number="maintenanceCreateForm.estimatedAmount" type="number" min="0.01" step="0.01" required></label>
          <label class="wide">維修事項<input v-model.trim="maintenanceCreateForm.title" maxlength="180" required placeholder="例如：廚房水管漏水維修"></label>
          <label class="wide">問題說明<textarea v-model.trim="maintenanceCreateForm.description" maxlength="1000" rows="4" placeholder="填寫故障位置、現況與需注意事項"></textarea></label>
          <p v-if="maintenanceCreateError" class="admin-property-error wide">{{ maintenanceCreateError }}</p>
        </div>
        <menu><button type="button" @click="closeMaintenanceCreate">取消</button><button class="primary-btn" :disabled="maintenanceCreateSaving || optionsLoading">{{ maintenanceCreateSaving ? '建立中…' : '建立維修工單' }}</button></menu>
      </form>
    </dialog>

    <dialog ref="handlingDialog" class="modal maintenance-handling-dialog">
      <form method="dialog" @submit.prevent="submitHandling">
        <div class="modal-head"><div><h3>處理維修工單</h3><small>{{ handlingRow?.workOrderNo }} · {{ handlingRow?.projectName }} {{ handlingRow?.unitNo }}</small></div><button type="button" class="icon-close" @click="closeHandling">×</button></div>
        <div v-if="handlingLoading" class="admin-owner-state">正在讀取預備金與附件資料…</div>
        <div v-else class="maintenance-handling-body">
          <section class="handling-reserve-summary">
            <div><span>目前預備金</span><strong>RM {{ money(handlingInfo.reserveBalance) }}</strong></div>
            <div><span>已扣維修費</span><strong>RM {{ money(handlingInfo.reserveDeductedAmount) }}</strong></div>
            <div><span>本次實際費用</span><strong>RM {{ money(handlingForm.actualAmount) }}</strong></div>
            <div :class="{ shortage: reserveShortage > 0 }"><span>扣款後／不足</span><strong>{{ reserveShortage > 0 ? `不足 RM ${money(reserveShortage)}` : `RM ${money(reserveAfter)}` }}</strong></div>
          </section>

          <label>實際維修金額（RM）<input v-model.number="handlingForm.actualAmount" type="number" min="0.01" step="0.01" required></label>
          <label>結算方式<select v-model="handlingForm.settlementMethod" required><option value="reserve" :disabled="!handlingInfo.reserveAccountAvailable || reserveShortage > 0">從業主預備金扣除</option><option value="direct_payment">直接支付／業主另付</option></select><small v-if="reserveShortage > 0" class="handling-warning">預備金不足，請選擇直接支付；系統不允許扣成負數。</small></label>

          <div class="handling-photo-grid">
            <label class="handling-photo-field"><span>維修前照片 <b>必須</b></span><input type="file" accept="image/jpeg,image/png" multiple @change="setHandlingFiles('before', $event)"><small>已有 {{ handlingInfo.beforePhotoCount || 0 }} 張；本次選擇 {{ handlingFiles.before.length }} 張</small></label>
            <label class="handling-photo-field"><span>維修後照片 <b>必須</b></span><input type="file" accept="image/jpeg,image/png" multiple @change="setHandlingFiles('after', $event)"><small>已有 {{ handlingInfo.afterPhotoCount || 0 }} 張；本次選擇 {{ handlingFiles.after.length }} 張</small></label>
          </div>

          <label class="handling-note">完成說明<textarea v-model.trim="handlingForm.completionNote" maxlength="500" rows="4" required placeholder="填寫維修內容、驗收結果與需要留存的說明"></textarea></label>
          <p v-if="handlingError" class="admin-property-error wide">{{ handlingError }}</p>
        </div>
        <menu><button type="button" @click="closeHandling">取消</button><button class="primary-btn" :disabled="handlingSaving || handlingLoading">{{ handlingSaving ? '處理中…' : '確認完成工單' }}</button></menu>
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
    return { activeTab: 'expense', response: { summary: {}, properties: [], expenses: [], maintenance: [] }, loading: false, errorMessage: '', selectedKey: '', maintenanceDetail: null, requestSerial: 0, options: { units: [], vendors: [] }, optionsLoading: false, expenseForm: this.emptyExpenseForm(), expenseSaving: false, expenseCreateError: '', maintenanceCreateForm: this.emptyMaintenanceForm(), maintenanceCreateSaving: false, maintenanceCreateError: '', handlingRow: null, handlingInfo: {}, handlingForm: { actualAmount: 0, settlementMethod: 'reserve', completionNote: '' }, handlingFiles: { before: [], after: [] }, handlingLoading: false, handlingSaving: false, handlingError: '' };
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
    selectedRow() { return this.filteredRows.find(row => this.rowKey(row) === this.selectedKey) || this.filteredRows[0] || null; },
    detailTitle() { return this.activeTab === 'expense' ? this.selectedRow?.description || '支出記錄' : this.selectedRow?.title || '維修工單'; },
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
    'page.projectFilter'() { this.ensureSelection(); },
    'page.statusFilter'() { this.ensureSelection(); },
    'page.moduleSearch'() { this.ensureSelection(); },
    'page.dateStart'() { this.loadData(); },
    'page.dateEnd'() { this.loadData(); },
    'page.adminExpenseCreateNonce'() { this.openExpenseCreate(); },
    'page.adminMaintenanceCreateNonce'() { this.openMaintenanceCreate(); }
  },
  mounted() { this.loadData(); },
  methods: {
    today() { return new Date().toLocaleDateString('en-CA'); },
    localDateTime() { const date = new Date(); date.setMinutes(date.getMinutes() - date.getTimezoneOffset()); return date.toISOString().slice(0, 16); },
    emptyExpenseForm() { return { unitId: '', category: 'utilities', description: '', amount: null, occurredOn: this.today(), settlementMethod: 'reserve' }; },
    emptyMaintenanceForm() { return { unitId: '', vendorId: null, category: 'plumbing', title: '', description: '', requestedAt: this.localDateTime(), estimatedAmount: null }; },
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
      const trend = value => `${Number(value || 0) >= 0 ? '↑' : '↓'} ${Math.abs(Number(value || 0)).toFixed(1)}% 較上月`;
      const month = new Date().toISOString().slice(0, 7);
      const categoryAmount = category => (this.response.expenses || [])
        .filter(item => item.category === category && String(item.occurredOn || '').startsWith(month))
        .reduce((total, item) => total + Number(item.amount || 0), 0);
      return [
        { label: '本月總支出', value: `RM ${this.money(summary.monthlyExpense)}`, delta: trend(summary.expenseChangePercent), trend: Number(summary.expenseChangePercent) <= 0 ? 'up' : 'down' },
        { label: '支出變化', value: `${Math.abs(Number(summary.expenseChangePercent || 0)).toFixed(1)}%`, delta: '與上月相比', trend: Number(summary.expenseChangePercent) <= 0 ? 'up' : 'down' },
        { label: '本月維修支出', value: `RM ${this.money(summary.monthlyMaintenanceExpense)}`, delta: trend(summary.maintenanceChangePercent), trend: Number(summary.maintenanceChangePercent) <= 0 ? 'up' : 'down' },
        { label: '本月水電費', value: `RM ${this.money(categoryAmount('utilities'))}`, delta: '資料庫即時統計', trend: 'up' },
        { label: '本月管理費', value: `RM ${this.money(categoryAmount('management'))}`, delta: '資料庫即時統計', trend: 'up' },
        { label: '待處理維修', value: `${summary.pendingMaintenanceCount || 0} 項`, delta: '需要管理員跟進', trend: Number(summary.pendingMaintenanceCount) ? 'down' : 'up' },
        { label: '預備金扣款', value: `RM ${this.money(summary.reserveDeductedAmount)}`, delta: `${summary.reserveDebitCount || 0} 筆扣款`, trend: 'up' }
      ];
    },
    selectTab(tab) { this.activeTab = tab; this.page.statusFilter = '全部狀態'; this.maintenanceDetail = null; this.selectedKey = ''; this.ensureSelection(); },
    ensureSelection() { this.$nextTick(() => { const row = this.filteredRows[0]; this.selectedKey = row ? this.rowKey(row) : ''; if (row) this.loadDetail(row); }); },
    selectRow(row) { this.selectedKey = this.rowKey(row); this.loadDetail(row); },
    rowKey(row) { return `${this.activeTab}-${row.id}`; },
    async loadDetail(row) { const id = this.activeTab === 'maintenance' ? row.id : row.workOrderId; if (!id) { this.maintenanceDetail = null; return; } try { this.maintenanceDetail = await fetchAdminMaintenanceDetail(id); } catch { this.maintenanceDetail = null; } },
    async openHandling(row) {
      this.handlingRow = row; this.handlingLoading = true; this.handlingError = ''; this.handlingFiles = { before: [], after: [] };
      this.handlingForm = { actualAmount: Number(row.amount || 0), settlementMethod: 'reserve', completionNote: '' };
      this.$refs.handlingDialog.showModal();
      try {
        this.handlingInfo = await fetchAdminMaintenanceHandling(row.id);
        if (!this.handlingInfo.reserveAccountAvailable || this.reserveShortage > 0) this.handlingForm.settlementMethod = 'direct_payment';
      } catch (error) { this.handlingError = error.message || '無法讀取工單處理資料'; }
      finally { this.handlingLoading = false; }
    },
    closeHandling() { this.$refs.handlingDialog?.close(); this.handlingRow = null; this.handlingError = ''; },
    setHandlingFiles(type, event) { this.handlingFiles[type] = Array.from(event.target.files || []); },
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
        await completeAdminMaintenance(this.handlingRow.id, this.handlingForm);
        this.closeHandling(); await this.loadData();
      } catch (error) { this.handlingError = error.message || '維修工單處理失敗'; }
      finally { this.handlingSaving = false; }
    },
    categoryLabel(value) { return CATEGORY_MAP[value] || value || '其他'; },
    maintenanceStatusLabel(value) { return STATUS_MAP[value] || value || '待處理'; },
    paymentLabel(value) { return { paid: '已付款', partial: '部分付款', unpaid: '待付款', pending: '待付款' }[value] || value || '待付款'; },
    paymentMethodLabel(value) { return { bank_transfer: '銀行轉帳', online_payment: '線上付款', reserve_account: '預備金扣款', direct_payment: '直接支付', cash: '現金' }[value] || '—'; },
    statusClass(value) { if (['已完成', '已付款'].includes(value)) return 'green'; if (['已取消'].includes(value)) return 'red'; if (['處理中', '部分付款', '已指派', '待驗收'].includes(value)) return 'orange'; return 'gray'; },
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
