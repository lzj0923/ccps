<template>
  <section class="owners-grid admin-owner-workspace">
    <template v-if="isPropertyMode">
      <div class="panel owners-table-panel">
        <div v-if="errorMessage" class="admin-owner-state error" role="alert"><strong>房產資料載入失敗</strong><span>{{ errorMessage }}</span><button type="button" @click="loadOwners">重新載入</button></div>
        <div v-else class="table-wrap owners-table-wrap">
          <table>
            <thead><tr><th>建案／項目</th><th>單位編號</th><th>業主</th><th>房型</th><th>交房狀態</th><th>出租狀態</th><th>房產總價</th><th>已繳金額</th><th>未繳金額</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="property in filteredProperties" :key="property.rowKey" :class="{ selected: property.rowKey === selectedPropertyKey }" @click="selectProperty(property)">
                <td><strong>{{ property.projectName || '未設定建案' }}</strong><small>{{ property.city || '—' }}</small></td>
                <td><strong>{{ property.unitNo || '—' }}</strong><small>{{ [property.building, property.floorNo].filter(Boolean).join(' · ') || '—' }}</small></td>
                <td><span class="avatar">{{ initials(property.ownerName) }}</span>{{ property.ownerName }}<small>{{ property.ownerPhone || '未設定手機' }}</small></td>
                <td>{{ property.unitType || '—' }}<small>{{ property.areaSqm ? `${property.areaSqm} m²` : '—' }}</small></td>
                <td><span class="tag" :class="lifecycleClass(property.assetStage)">{{ lifecycleLabel(property.assetStage) }}</span></td>
                <td><span class="tag" :class="rentalStatusClass(property.rentalStatus)">{{ rentalStatusLabel(property.rentalStatus) }}</span></td>
                <td>RM {{ money(property.purchasePrice) }}</td><td>RM {{ money(property.paidAmount) }}</td>
                <td :class="{ 'money-red': Number(property.remainingAmount || 0) > 0 }">RM {{ money(property.remainingAmount) }}</td>
                <td><button class="row-actions owner-row-arrow" type="button" title="查看房產" @click.stop="selectProperty(property)">›</button></td>
              </tr>
              <tr v-if="!loading && !filteredProperties.length"><td colspan="10" class="admin-owner-empty">目前沒有符合條件的房產</td></tr>
            </tbody>
          </table>
        </div>
        <div class="owners-pager"><span>共 {{ propertyTotalRows }} records</span><div class="admin-building-pager"><button :disabled="propertyPageNumber <= 1 || loading" @click="goPropertyPage(propertyPageNumber - 1)">&lt;</button><button v-for="n in propertyVisiblePages" :key="n" :class="{ active: n === propertyPageNumber }" :disabled="loading" @click="goPropertyPage(n)">{{ n }}</button><button :disabled="propertyPageNumber >= propertyTotalPages || loading" @click="goPropertyPage(propertyPageNumber + 1)">&gt;</button><select v-model.number="propertyPageSize" :disabled="loading"><option :value="10">10 條/頁</option><option :value="20">20 條/頁</option><option :value="50">50 條/頁</option></select></div></div>
      </div>
      <aside class="panel owners-detail admin-owner-detail">
        <template v-if="selectedProperty">
          <div class="owners-profile"><div class="big-avatar">房</div><div><h3>{{ selectedProperty.projectName }} · {{ selectedProperty.unitNo }}</h3><p>{{ selectedProperty.ownerName }} · 房產資料</p></div><span class="tag" :class="lifecycleClass(selectedProperty.assetStage)">{{ lifecycleLabel(selectedProperty.assetStage) }}</span></div>
          <section class="owners-section"><h4><span>1</span>房產資訊</h4><div class="kv"><span>建案</span><b>{{ selectedProperty.projectName || '—' }}</b></div><div class="kv"><span>城市</span><b>{{ selectedProperty.city || '—' }}</b></div><div class="kv"><span>棟／樓層</span><b>{{ [selectedProperty.building, selectedProperty.floorNo].filter(Boolean).join('／') || '—' }}</b></div><div class="kv"><span>房型／面積</span><b>{{ selectedProperty.unitType || '—' }} · {{ selectedProperty.areaSqm ? `${selectedProperty.areaSqm} m²` : '—' }}</b></div><div class="kv"><span>單位狀態</span><b>{{ listingStatusLabel(selectedProperty.listingStatus) }}</b></div></section>
          <section class="owners-section"><h4><span>2</span>所屬業主</h4><div class="kv"><span>業主姓名</span><b>{{ selectedProperty.ownerName }}</b></div><div class="kv"><span>手機</span><b>{{ selectedProperty.ownerPhone || '—' }}</b></div><div class="kv"><span>持有比例</span><b>{{ selectedProperty.ownershipPercent ?? 100 }}%</b></div><div class="kv"><span>主房主</span><b>{{ selectedProperty.primary ? '是' : '否' }}</b></div></section>
          <section class="owners-section"><h4><span>3</span>房款資料</h4><div class="kv"><span>房產總價</span><b>RM {{ money(selectedProperty.purchasePrice) }}</b></div><div class="kv"><span>已繳金額</span><b>RM {{ money(selectedProperty.paidAmount) }}</b></div><div class="kv"><span>未繳金額</span><b class="money-red">RM {{ money(selectedProperty.remainingAmount) }}</b></div><div class="progress"><i :style="{ width: propertyProgress + '%' }"></i></div></section>
          <div class="owner-property-actions property-page-actions"><button type="button" @click="openDetails(selectedProperty)">查詢詳情</button><button type="button" class="edit" @click="openEdit(selectedProperty)">修改房產</button></div>
        </template>
        <div v-else class="admin-owner-empty">請點擊左側房產查看詳情</div>
      </aside>
    </template>

    <div v-else-if="workspaceTab === 'accounts'" class="panel owners-table-panel owners-account-panel">
      <div class="owners-panel-head owners-workspace-tabs-head">
        <div class="owners-workspace-tabs" role="tablist" aria-label="業主管理功能">
          <button type="button" :class="{ active: workspaceTab === 'owners' }" @click="workspaceTab = 'owners'">全部業主</button>
          <button type="button" :class="{ active: workspaceTab === 'accounts' }" @click="workspaceTab = 'accounts'">帳號管理</button>
        </div>
        <button type="button" class="primary-btn" @click="openAccountCreate">新增帳號</button>
      </div>
      <AdminAccountsWorkspace ref="accountsWorkspace" />
    </div>

    <template v-else>
    <div class="panel owners-table-panel">
      <div class="owners-panel-head">
        <div>
          <div class="owners-workspace-tabs" role="tablist" aria-label="業主管理功能">
            <button type="button" :class="{ active: workspaceTab === 'owners' }" @click="workspaceTab = 'owners'">全部業主</button>
            <button type="button" :class="{ active: workspaceTab === 'accounts' }" @click="workspaceTab = 'accounts'">帳號管理</button>
          </div>
          <span>{{ filteredOwners.length }} records</span>
        </div>
        <span v-if="loading">正在從資料庫載入…</span>
      </div>

      <div v-if="errorMessage" class="admin-owner-state error" role="alert">
        <strong>業主資料載入失敗</strong>
        <span>{{ errorMessage }}</span>
        <button type="button" @click="loadOwners">重新載入</button>
      </div>
      <div v-else class="table-wrap owners-table-wrap">
        <table>
          <thead>
            <tr><th>業主姓名</th><th>證件號</th><th>手機號</th><th>郵箱</th><th>房產數量</th><th>帳號狀態</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="owner in filteredOwners" :key="owner.id" :class="{ selected: owner.id === selectedOwnerId }" @click="selectOwner(owner)">
              <td><span class="avatar">{{ initials(owner.fullName) }}</span>{{ owner.fullName }}</td>
              <td>{{ owner.identityNo || '—' }}</td>
              <td>{{ owner.phone || '—' }}</td>
              <td>{{ owner.email || '—' }}</td>
              <td><span class="owner-property-count">{{ owner.properties.length }} 套</span></td>
              <td><span class="tag" :class="owner.status === 'active' ? 'green' : 'gray'">{{ owner.status === 'active' ? '啟用' : '停用' }}</span></td>
              <td><button class="row-actions owner-row-arrow" type="button" title="查看名下房產" @click.stop="selectOwner(owner)">›</button></td>
            </tr>
            <tr v-if="!loading && !filteredOwners.length"><td colspan="7" class="admin-owner-empty">目前沒有符合條件的業主</td></tr>
          </tbody>
        </table>
      </div>
      <div class="owners-pager"><span>{{ filteredOwners.length }} records</span><div><button disabled>&lt;</button><button class="active">1</button><button disabled>&gt;</button></div></div>
    </div>

    <aside class="panel owners-detail admin-owner-detail">
      <template v-if="selectedOwner">
        <div class="owners-profile">
          <div class="big-avatar">{{ initials(selectedOwner.fullName) }}</div>
          <div><h3>{{ selectedOwner.fullName }}</h3><p>{{ selectedOwner.phone || '未設定手機' }} · 業主資料</p></div>
          <span class="tag" :class="selectedOwner.status === 'active' ? 'green' : 'gray'">{{ selectedOwner.status === 'active' ? '正常' : '停用' }}</span>
        </div>

        <section class="owners-section">
          <h4><span>1</span>業主資訊</h4>
          <div class="kv"><span>證件號</span><b>{{ selectedOwner.identityNo || '—' }}</b></div>
          <div class="kv"><span>手機</span><b>{{ selectedOwner.phone || '—' }}</b></div>
          <div class="kv"><span>郵箱</span><b>{{ selectedOwner.email || '—' }}</b></div>
        </section>

        <section class="owners-section owner-properties-section">
          <h4><span>2</span>名下房產 <b>{{ selectedOwner.properties.length }}</b></h4>
          <article v-for="property in selectedOwner.properties" :key="property.ownerUnitId" class="admin-owner-property-card">
            <div class="owner-property-heading">
              <div><strong>{{ property.unitNo }}</strong><small>{{ property.projectName }}</small></div>
              <span class="tag" :class="lifecycleClass(property.assetStage)">{{ lifecycleLabel(property.assetStage) }}</span>
            </div>
            <div class="owner-property-meta"><span>{{ property.unitType || '房型未設定' }}</span><span>{{ property.areaSqm ? `${property.areaSqm} m²` : '面積未設定' }}</span><b>RM {{ money(property.purchasePrice) }}</b></div>
            <div v-if="property.assetStage === 'OPERATING'" class="owner-property-services">
              <span v-for="service in property.services" :key="service">{{ serviceLabel(service) }}</span>
              <small v-if="!property.services?.length">尚未啟用營運服務</small>
            </div>
            <div class="owner-property-actions">
              <button type="button" @click="openDetails(property)">查詢詳情</button>
              <button type="button" class="edit" @click="openEdit(property)">修改</button>
            </div>
          </article>
          <p v-if="!selectedOwner.properties.length" class="admin-owner-empty">此業主尚未綁定房產</p>
        </section>

        <section v-if="preHandoverProperties.length" class="owners-section">
          <h4><span>3</span>Payment progress</h4>
          <div class="kv"><span>Total</span><b>RM {{ money(ownerTotal) }}</b></div>
          <div class="kv"><span>Pending</span><b class="money-red">RM {{ money(ownerPending) }}</b></div>
          <div class="progress"><i :style="{ width: ownerProgress + '%' }"></i></div>
        </section>
      </template>
      <div v-else class="admin-owner-empty">請點擊左側業主查看名下房產</div>
    </aside>
    </template>

    <dialog ref="ownerDialog" class="modal admin-owner-dialog">
      <form method="dialog" @submit.prevent="saveOwner">
        <div class="modal-head"><h3>新增業主</h3><button class="icon-close" type="button" @click="closeOwnerDialog">×</button></div>
        <div class="form-grid">
          <label class="wide">業主姓名<input v-model.trim="ownerForm.fullName" maxlength="160" required placeholder="請輸入業主姓名"></label>
          <label>手機號<input v-model.trim="ownerForm.phone" maxlength="40" required placeholder="例如：+60 12-345 6789"></label>
          <label>郵箱<input v-model.trim="ownerForm.email" type="email" maxlength="190" placeholder="owner@example.com"></label>
          <label>證件號<input v-model.trim="ownerForm.identityNo" maxlength="120" placeholder="身份證／護照號"></label>
          <label>業主狀態<select v-model="ownerForm.status"><option value="active">啟用</option><option value="inactive">停用</option></select></label>
          <p class="admin-owner-form-note wide">建立後可再使用「新增房產」或房產綁定功能加入名下房產。</p>
          <p v-if="ownerFormError" class="admin-property-error wide">{{ ownerFormError }}</p>
        </div>
        <menu><button type="button" @click="closeOwnerDialog">取消</button><button type="submit" class="primary-btn" :disabled="ownerSaving">{{ ownerSaving ? '新增中…' : '確認新增' }}</button></menu>
      </form>
    </dialog>

    <dialog ref="propertyCreateDialog" class="modal admin-property-create-dialog">
      <form method="dialog" @submit.prevent="saveNewProperty">
        <div class="modal-head">
          <div><h3>新增房產</h3><small>步驟 {{ propertyCreateStep }}／2 · {{ propertyCreateStep === 1 ? '選擇業主' : '建立房產' }}</small></div>
          <button class="icon-close" type="button" @click="closePropertyCreateDialog">×</button>
        </div>

        <section v-if="propertyCreateStep === 1" class="owner-picker-step">
          <label class="owner-picker-search">搜尋業主<input v-model.trim="propertyOwnerSearch" placeholder="輸入姓名、手機或郵箱"></label>
          <div class="owner-picker-list">
            <button v-for="owner in propertyOwnerOptions" :key="owner.id" type="button" :class="{ selected: propertyOwnerId === owner.id }" @click="propertyOwnerId = owner.id">
              <span class="avatar">{{ initials(owner.fullName) }}</span>
              <span><strong>{{ owner.fullName }}</strong><small>{{ owner.phone || owner.email || '未設定聯絡方式' }}</small></span>
              <b>{{ owner.properties.length }} 套房產</b><i>✓</i>
            </button>
            <p v-if="!propertyOwnerOptions.length" class="admin-owner-empty">沒有符合條件的啟用業主</p>
          </div>
          <p v-if="propertyCreateError" class="admin-property-error">{{ propertyCreateError }}</p>
        </section>

        <div v-else class="form-grid admin-new-property-form">
          <div class="selected-property-owner wide"><span class="avatar">{{ initials(propertyCreateOwner?.fullName) }}</span><div><small>房產業主</small><strong>{{ propertyCreateOwner?.fullName }}</strong></div><button type="button" @click="propertyCreateStep = 1">重新選擇</button></div>
          <label class="wide">建案／項目<select v-model.number="propertyCreateForm.projectId" required><option disabled value="">請選擇建案</option><option v-for="project in propertyProjects" :key="project.id" :value="project.id">{{ project.name }}{{ project.city ? ` · ${project.city}` : '' }}</option></select></label>
          <label>棟<input v-model.trim="propertyCreateForm.building" maxlength="80"></label>
          <label>樓層<input v-model.trim="propertyCreateForm.floorNo" maxlength="20"></label>
          <label>單位編號<input v-model.trim="propertyCreateForm.unitNo" maxlength="40" required></label>
          <label>戶型<input v-model.trim="propertyCreateForm.unitType" maxlength="80"></label>
          <label>面積（m²）<input v-model.number="propertyCreateForm.areaSqm" type="number" min="0.01" step="0.01"></label>
          <label>房間數<input v-model.number="propertyCreateForm.bedroomCount" type="number" min="0" max="50"></label>
          <label>房產階段<select v-model="propertyCreateForm.assetStage"><option value="PRE_HANDOVER">未交房</option><option value="OPERATING">已交房／營運中</option></select></label>
          <label v-if="propertyCreateForm.assetStage === 'PRE_HANDOVER'">預計交房日期<input v-model="propertyCreateForm.expectedHandoverDate" type="date"></label>
          <label v-else>實際交房日期<input v-model="propertyCreateForm.actualHandoverDate" type="date" required></label>
          <fieldset v-if="propertyCreateForm.assetStage === 'OPERATING'" class="property-service-picker wide">
            <legend>營運服務（可多選）</legend>
            <label v-for="service in serviceOptions" :key="service.value"><input v-model="propertyCreateForm.services" type="checkbox" :value="service.value">{{ service.label }}</label>
          </fieldset>
          <label>單位狀態<select v-model="propertyCreateForm.listingStatus"><option value="available">可用</option><option value="reserved">已預留</option><option value="sold">已出售</option><option value="rented">出租中</option><option value="inactive">停用</option></select></label>
          <label>房產總價（RM）<input v-model.number="propertyCreateForm.purchasePrice" type="number" min="0" step="0.01" required></label>
          <label>持有比例（%）<input v-model.number="propertyCreateForm.ownershipPercent" type="number" min="0.01" max="100" step="0.01" required></label>
          <label>持有開始日期<input v-model="propertyCreateForm.startDate" type="date"></label>
          <label class="property-primary-check"><input v-model="propertyCreateForm.primary" type="checkbox">設為主房主</label>
          <p v-if="propertyCreateError" class="admin-property-error wide">{{ propertyCreateError }}</p>
        </div>

        <menu v-if="propertyCreateStep === 1"><button type="button" @click="closePropertyCreateDialog">取消</button><button type="button" class="primary-btn" :disabled="!propertyOwnerId" @click="continuePropertyCreate">下一步：建立房產</button></menu>
        <menu v-else><button type="button" @click="propertyCreateStep = 1">上一步</button><button type="submit" class="primary-btn" :disabled="propertyCreating">{{ propertyCreating ? '建立中…' : '確認建立房產' }}</button></menu>
      </form>
    </dialog>

    <dialog ref="propertyDialog" class="modal admin-property-dialog">
      <form method="dialog" @submit.prevent>
        <div class="modal-head"><h3>{{ dialogMode === 'edit' ? '修改房產' : '房產詳情' }}</h3><button class="icon-close" type="button" @click="closeDialog">×</button></div>

        <div v-if="dialogLoading" class="admin-owner-state">正在載入房產資料…</div>
        <div v-else-if="dialogError && !dialogProperty" class="admin-owner-state error">{{ dialogError }}</div>
        <div v-else-if="dialogProperty" class="form-grid admin-property-form">
          <label>建案／項目<input :value="dialogProperty.projectName" disabled></label>
          <label>城市<input :value="dialogProperty.city || '—'" disabled></label>
          <label>棟<input v-model="editForm.building" :disabled="dialogMode !== 'edit'" maxlength="80"></label>
          <label>樓層<input v-model="editForm.floorNo" :disabled="dialogMode !== 'edit'" maxlength="20"></label>
          <label>單位編號<input v-model.trim="editForm.unitNo" :disabled="dialogMode !== 'edit'" maxlength="40" required></label>
          <label>戶型<input v-model="editForm.unitType" :disabled="dialogMode !== 'edit'" maxlength="80"></label>
          <label>面積（m²）<input v-model.number="editForm.areaSqm" :disabled="dialogMode !== 'edit'" type="number" min="0.01" step="0.01"></label>
          <label>房間數<input v-model.number="editForm.bedroomCount" :disabled="dialogMode !== 'edit'" type="number" min="0" max="50"></label>
          <label>房產階段
            <select v-model="editForm.assetStage" :disabled="dialogMode !== 'edit'">
              <option value="PRE_HANDOVER" :disabled="dialogProperty.assetStage === 'OPERATING'">未交房</option>
              <option value="OPERATING">已交房／營運中</option>
            </select>
          </label>
          <label v-if="editForm.assetStage === 'PRE_HANDOVER'">預計交房日期<input v-model="editForm.expectedHandoverDate" :disabled="dialogMode !== 'edit'" type="date"></label>
          <label v-else>實際交房日期<input v-model="editForm.actualHandoverDate" :disabled="dialogMode !== 'edit'" type="date" required></label>
          <fieldset v-if="editForm.assetStage === 'OPERATING'" class="property-service-picker wide">
            <legend>營運服務（可多選）</legend>
            <label v-for="service in serviceOptions" :key="service.value"><input v-model="editForm.services" type="checkbox" :value="service.value" :disabled="dialogMode !== 'edit'">{{ service.label }}</label>
          </fieldset>
          <label>單位狀態
            <select v-model="editForm.listingStatus" :disabled="dialogMode !== 'edit'">
              <option value="available">可用</option><option value="reserved">已預留</option><option value="sold">已出售</option><option value="rented">出租中</option><option value="inactive">停用</option>
            </select>
          </label>
          <label>房產總價（RM）<input v-model.number="editForm.purchasePrice" :disabled="dialogMode !== 'edit'" type="number" min="0" step="0.01" required></label>
          <template v-if="editForm.assetStage === 'PRE_HANDOVER'">
            <label>已繳金額（RM）<input :value="money(dialogProperty.paidAmount)" disabled></label>
            <label>未繳金額（RM）<input :value="money(dialogProperty.remainingAmount)" disabled></label>
          </template>
          <p v-if="dialogError" class="admin-property-error wide">{{ dialogError }}</p>
        </div>

        <menu>
          <button type="button" @click="closeDialog">關閉</button>
          <button v-if="dialogMode === 'edit'" type="button" class="primary-btn" :disabled="saving" @click="saveProperty">{{ saving ? '儲存中…' : '儲存修改' }}</button>
        </menu>
      </form>
    </dialog>
  </section>
</template>

<script>
import { createAdminOwner, createAdminOwnerProperty, fetchAdminOwnerProperty, fetchAdminOwnerSummary, fetchAdminOwners, fetchAdminProperties, fetchAdminPropertyProjects, updateAdminOwnerProperty } from '../services/propertyApi';
import AdminAccountsWorkspace from './AdminAccountsWorkspace.vue';

export default {
  inject: ['page'],
  components: { AdminAccountsWorkspace },
  props: { mode: { type: String, default: 'owners' } },
  data() {
    return {
      workspaceTab: 'owners',
      owners: [], propertyRows: [], loading: false, errorMessage: '', selectedOwnerId: null, selectedPropertyKey: null,
      propertyPageNumber: 1, propertyPageSize: 10, propertyTotalRows: 0, propertyTotalPages: 1, propertyRequestSerial: 0,
      ownerSaving: false, ownerFormError: '',
      ownerForm: { fullName: '', identityNo: '', phone: '', email: '', status: 'active' },
      propertyProjects: [], propertyCreateStep: 1, propertyOwnerSearch: '', propertyOwnerId: null,
      propertyCreating: false, propertyCreateError: '',
      serviceOptions: [{ value: 'RENTAL', label: '出租' }, { value: 'RESALE', label: '代售' }, { value: 'MANAGEMENT', label: '代管' }],
      propertyCreateForm: { projectId: '', building: '', floorNo: '', unitNo: '', unitType: '', areaSqm: null, bedroomCount: null, listingStatus: 'available', assetStage: 'PRE_HANDOVER', expectedHandoverDate: '', actualHandoverDate: '', services: [], purchasePrice: 0, ownershipPercent: 100, primary: true, startDate: '' },
      dialogMode: 'details', dialogLoading: false, dialogProperty: null, dialogError: '', saving: false,
      editForm: { building: '', floorNo: '', unitNo: '', unitType: '', areaSqm: null, bedroomCount: null, listingStatus: 'available', assetStage: 'PRE_HANDOVER', expectedHandoverDate: '', actualHandoverDate: '', services: [], purchasePrice: 0 }
    };
  },
  computed: {
    isPropertyMode() { return this.mode === 'properties'; },
    allProperties() {
      if (this.isPropertyMode) return this.propertyRows;
      return this.owners.flatMap(owner => owner.properties.map(property => ({
        ...property, ownerId: owner.id, ownerName: owner.fullName, ownerPhone: owner.phone,
        ownerEmail: owner.email, rowKey: `${owner.id}-${property.ownerUnitId}`
      })));
    },
    filteredProperties() {
      if (this.isPropertyMode) return this.propertyRows;
      const keyword = String(this.page.globalSearch || this.page.moduleSearch || '').trim().toLowerCase();
      const projectFilter = String(this.page.projectFilter || '');
      const statusFilter = String(this.page.statusFilter || '');
      return this.allProperties.filter(property => {
        const text = `${property.projectName || ''} ${property.unitNo || ''} ${property.ownerName || ''} ${property.ownerPhone || ''} ${property.unitType || ''}`.toLowerCase();
        return (!keyword || text.includes(keyword))
          && (projectFilter.includes('全部') || property.projectName === projectFilter)
          && (statusFilter.includes('全部') || this.lifecycleLabel(property.assetStage) === statusFilter);
      });
    },
    selectedProperty() { return this.filteredProperties.find(property => property.rowKey === this.selectedPropertyKey) || this.filteredProperties[0] || null; },
    propertyVisiblePages() { const start = Math.max(1, Math.min(this.propertyPageNumber - 2, this.propertyTotalPages - 4)); return Array.from({ length: Math.min(5, this.propertyTotalPages) }, (_, index) => start + index); },
    propertyProgress() { return !Number(this.selectedProperty?.purchasePrice) ? 0 : Math.max(0, Math.min(100, Math.round(Number(this.selectedProperty.paidAmount || 0) / Number(this.selectedProperty.purchasePrice) * 100))); },
    filteredOwners() {
      const keyword = String(this.page.globalSearch || this.page.moduleSearch || '').trim().toLowerCase();
      const projectFilter = String(this.page.projectFilter || '');
      const statusFilter = String(this.page.statusFilter || '');
      return this.owners.filter(owner => {
        const propertyText = owner.properties.map(item => `${item.projectName} ${item.unitNo}`).join(' ');
        const matchesKeyword = !keyword || `${owner.fullName} ${owner.phone || ''} ${owner.email || ''} ${propertyText}`.toLowerCase().includes(keyword);
        const matchesProject = projectFilter.includes('全部') || owner.properties.some(item => item.projectName === projectFilter);
        const matchesStatus = statusFilter.includes('全部') || (statusFilter === '啟用' ? owner.status === 'active' : statusFilter === '停用' ? owner.status !== 'active' : owner.properties.some(item => this.lifecycleLabel(item.assetStage) === statusFilter));
        return matchesKeyword && matchesProject && matchesStatus;
      });
    },
    selectedOwner() { return this.owners.find(owner => owner.id === this.selectedOwnerId) || null; },
    createRequestNonce() { return this.page.adminOwnerCreateNonce; },
    propertyCreateRequestNonce() { return this.page.adminPropertyCreateNonce; },
    propertyOwnerOptions() {
      const keyword = this.propertyOwnerSearch.toLowerCase();
      return this.owners.filter(owner => owner.status === 'active' && (!keyword || `${owner.fullName} ${owner.phone || ''} ${owner.email || ''}`.toLowerCase().includes(keyword)));
    },
    propertyCreateOwner() { return this.owners.find(owner => owner.id === this.propertyOwnerId) || null; },
    preHandoverProperties() { return (this.selectedOwner?.properties || []).filter(item => item.assetStage === 'PRE_HANDOVER'); },
    ownerTotal() { return this.preHandoverProperties.reduce((sum, item) => sum + Number(item.purchasePrice || 0), 0); },
    ownerPending() { return this.preHandoverProperties.reduce((sum, item) => sum + Number(item.remainingAmount || 0), 0); },
    ownerProgress() { return this.ownerTotal ? Math.max(0, Math.min(100, Math.round((this.ownerTotal - this.ownerPending) / this.ownerTotal * 100))) : 0; }
  },
  watch: {
    workspaceTab(value) {
      this.page.adminOwnerWorkspaceTab = value;
      if (value === 'accounts') {
        this.page.projectFilter = '全部建案';
        this.page.statusFilter = '全部狀態';
      }
    },
    filteredOwners: {
      immediate: true,
      deep: true,
      handler(owners) {
        if (!owners.some(owner => owner.id === this.selectedOwnerId)) {
          this.selectedOwnerId = owners[0]?.id || null;
        }
        if (!this.isPropertyMode) this.syncOwnerExportRows(owners);
      }
    },
    filteredProperties: {
      immediate: true,
      deep: true,
      handler(properties) {
        if (!this.isPropertyMode) return;
        if (!properties.some(property => property.rowKey === this.selectedPropertyKey)) this.selectedPropertyKey = properties[0]?.rowKey || null;
        const selected = properties.find(property => property.rowKey === this.selectedPropertyKey) || properties[0];
        if (selected) this.selectedOwnerId = selected.ownerId;
        this.syncPropertyExportRows(properties);
      }
    },
    createRequestNonce(value, previousValue) {
      if (value > previousValue) this.openCreateOwner();
    },
    propertyCreateRequestNonce(value, previousValue) {
      if (value > previousValue) this.openPropertyCreate();
    },
    propertyPageSize() { if (this.isPropertyMode) this.resetPropertyPage(); },
    'page.moduleSearch'() { if (this.isPropertyMode) this.resetPropertyPage(); },
    'page.globalSearch'() { if (this.isPropertyMode) this.resetPropertyPage(); },
    'page.projectFilter'() { if (this.isPropertyMode) this.resetPropertyPage(); },
    'page.statusFilter'() { if (this.isPropertyMode) this.resetPropertyPage(); },
    'propertyCreateForm.assetStage'(stage) {
      if (stage === 'PRE_HANDOVER') { this.propertyCreateForm.actualHandoverDate = ''; this.propertyCreateForm.services = []; }
      else this.propertyCreateForm.expectedHandoverDate = '';
    },
    'editForm.assetStage'(stage) {
      if (stage === 'PRE_HANDOVER') { this.editForm.actualHandoverDate = ''; this.editForm.services = []; }
      else this.editForm.expectedHandoverDate = '';
    }
  },
  async mounted() { this.page.adminOwnerWorkspaceTab = this.workspaceTab; if (this.isPropertyMode) this.page.statusFilter = '全部出租狀態'; await this.loadOwners(); if (this.isPropertyMode) await this.loadProperties(); },
  methods: {
    openAccountCreate() {
      this.workspaceTab = 'accounts';
      this.$nextTick(() => this.$refs.accountsWorkspace?.openCreate());
    },
    async loadOwners(preferredOwnerId = null) {
      const previousId = preferredOwnerId || this.selectedOwnerId;
      this.loading = true; this.errorMessage = '';
      try {
        const [owners, projects, summary] = await Promise.all([
          fetchAdminOwners(),
          fetchAdminPropertyProjects(),
          fetchAdminOwnerSummary().catch(() => null)
        ]);
        this.owners = owners.map(owner => ({ ...owner, properties: owner.properties || [] }));
        this.propertyProjects = projects || [];
        const allProperties = this.owners.flatMap(owner => owner.properties);
        const fallbackSummary = {
          ownerCount: this.owners.length,
          propertyCount: allProperties.length,
          operatingPropertyCount: allProperties.filter(property => property.assetStage === 'OPERATING').length,
          unpaidOwnerCount: this.owners.filter(owner => owner.properties.some(property => Number(property.remainingAmount || 0) > 0)).length,
          lowReserveOwnerCount: 0
        };
        const liveSummary = summary || fallbackSummary;
        this.page.adminOwnerMetrics = this.isPropertyMode ? [
          { icon: 'property', label: '房產總數', value: `${allProperties.length} 套`, delta: '資料庫即時統計', trend: '' },
          { icon: 'managed', label: '已交房', value: `${allProperties.filter(property => property.assetStage === 'OPERATING').length} 套`, delta: '可進入營運管理', trend: '' },
          { icon: 'handover', label: '未交房', value: `${allProperties.filter(property => property.assetStage === 'PRE_HANDOVER').length} 套`, delta: '仍在建設或交房前', trend: '' },
          { icon: 'payment', label: '尚有未繳', value: `${allProperties.filter(property => Number(property.remainingAmount || 0) > 0).length} 套`, delta: '需繼續跟進房款', trend: '' }
        ] : [
          { icon: 'people', label: '業主總數', value: `${Number(liveSummary.ownerCount || 0)} 位業主`, delta: '資料庫即時統計', trend: '' },
          { icon: 'active', label: '啟用業主', value: `${this.owners.filter(owner => owner.status === 'active').length} 位`, delta: '可登入業主端', trend: '' },
          { icon: 'property', label: '已綁定房產', value: `${this.owners.filter(owner => owner.properties.length).length} 位`, delta: '已有名下房產', trend: '' },
          { icon: 'unbound', label: '未綁定房產', value: `${this.owners.filter(owner => !owner.properties.length).length} 位`, delta: '需要建立或綁定房產', trend: '' }
        ];
        this.page.adminOwnerProjects = [...new Set(this.owners.flatMap(owner => owner.properties.map(property => property.projectName)).filter(Boolean))].sort((a, b) => a.localeCompare(b));
        if (!String(this.page.projectFilter).includes('全部') && !this.page.adminOwnerProjects.includes(this.page.projectFilter)) {
          this.page.projectFilter = '全部建案';
        }
        this.selectedOwnerId = this.owners.some(owner => owner.id === previousId) ? previousId : this.owners[0]?.id || null;
        if (this.isPropertyMode && !this.allProperties.some(property => property.rowKey === this.selectedPropertyKey)) this.selectedPropertyKey = this.allProperties[0]?.rowKey || null;
        if (this.isPropertyMode) {
          const selectedProperty = this.allProperties.find(property => property.rowKey === this.selectedPropertyKey);
          if (selectedProperty) this.selectedOwnerId = selectedProperty.ownerId;
        }
      } catch (error) {
        this.owners = []; this.page.adminOwnerProjects = []; this.selectedOwnerId = null; this.errorMessage = error.message || 'API request failed';
      } finally { this.loading = false; }
    },
    async loadProperties() {
      if (!this.isPropertyMode) return;
      const serial = ++this.propertyRequestSerial; const preferredKey = this.selectedPropertyKey;
      this.loading = true; this.errorMessage = '';
      try {
        const response = await fetchAdminProperties({ page: this.propertyPageNumber, pageSize: this.propertyPageSize, keyword: this.page.globalSearch || this.page.moduleSearch || '', projectName: String(this.page.projectFilter || '').includes('全部') ? '' : this.page.projectFilter, rentalStatus: this.rentalStatusParam(this.page.statusFilter) });
        if (serial !== this.propertyRequestSerial) return;
        this.propertyRows = (response.rows || []).map(item => ({ ...item.property, ownerId: item.ownerId, ownerName: item.ownerName, ownerPhone: item.ownerPhone, ownerEmail: item.ownerEmail, rentalStatus: item.rentalStatus, rowKey: `${item.ownerId}-${item.property.ownerUnitId}` }));
        this.propertyTotalRows = Number(response.page?.totalRows || 0); this.propertyTotalPages = Number(response.page?.totalPages || 1); this.propertyPageNumber = Number(response.page?.page || 1);
        this.selectedPropertyKey = this.propertyRows.some(property => property.rowKey === preferredKey) ? preferredKey : this.propertyRows[0]?.rowKey || null;
        const selected = this.propertyRows.find(property => property.rowKey === this.selectedPropertyKey); if (selected) this.selectedOwnerId = selected.ownerId;
        this.syncPropertyExportRows(this.propertyRows);
      } catch (error) { if (serial !== this.propertyRequestSerial) return; this.propertyRows = []; this.propertyTotalRows = 0; this.errorMessage = error.message || '房產資料載入失敗'; }
      finally { if (serial === this.propertyRequestSerial) this.loading = false; }
    },
    resetPropertyPage() { this.propertyPageNumber = 1; this.loadProperties(); },
    goPropertyPage(page) { if (page < 1 || page > this.propertyTotalPages || page === this.propertyPageNumber) return; this.propertyPageNumber = page; this.loadProperties(); },
    selectOwner(owner) { this.selectedOwnerId = owner.id; },
    selectProperty(property) { this.selectedPropertyKey = property.rowKey; this.selectedOwnerId = property.ownerId; },
    openCreateOwner() {
      this.ownerForm = { fullName: '', identityNo: '', phone: '', email: '', status: 'active' };
      this.ownerFormError = '';
      this.$refs.ownerDialog.showModal();
    },
    closeOwnerDialog() { this.$refs.ownerDialog?.close(); },
    async saveOwner() {
      if (!this.ownerForm.fullName) { this.ownerFormError = '請填寫業主姓名'; return; }
      if (!this.ownerForm.phone) { this.ownerFormError = '請填寫手機號，系統會用它建立登入帳號'; return; }
      this.ownerSaving = true; this.ownerFormError = '';
      try {
        const owner = await createAdminOwner(this.ownerForm);
        await this.loadOwners(owner.id);
        this.closeOwnerDialog();
        this.page.showToast('業主已新增，登入帳號為手機號，初始密碼 123456');
      } catch (error) { this.ownerFormError = error.message || '新增業主失敗'; }
      finally { this.ownerSaving = false; }
    },
    emptyPropertyCreateForm() {
      return { projectId: '', building: '', floorNo: '', unitNo: '', unitType: '', areaSqm: null, bedroomCount: null, listingStatus: 'available', assetStage: 'PRE_HANDOVER', expectedHandoverDate: '', actualHandoverDate: '', services: [], purchasePrice: 0, ownershipPercent: 100, primary: true, startDate: new Date().toISOString().slice(0, 10) };
    },
    openPropertyCreate() {
      this.propertyCreateStep = 1; this.propertyOwnerSearch = ''; this.propertyOwnerId = null;
      this.propertyCreateForm = this.emptyPropertyCreateForm(); this.propertyCreateError = '';
      this.$refs.propertyCreateDialog.showModal();
    },
    closePropertyCreateDialog() { this.$refs.propertyCreateDialog?.close(); },
    continuePropertyCreate() {
      if (!this.propertyCreateOwner) { this.propertyCreateError = '請先選擇業主'; return; }
      this.propertyCreateError = ''; this.propertyCreateStep = 2;
    },
    async saveNewProperty() {
      if (!this.propertyCreateOwner) { this.propertyCreateStep = 1; this.propertyCreateError = '請先選擇業主'; return; }
      if (!this.propertyCreateForm.projectId || !this.propertyCreateForm.unitNo) { this.propertyCreateError = '請選擇建案並填寫單位編號'; return; }
      if (this.propertyCreateForm.assetStage === 'OPERATING' && !this.propertyCreateForm.actualHandoverDate) { this.propertyCreateError = '已交房房產需要填寫實際交房日期'; return; }
      this.propertyCreating = true; this.propertyCreateError = '';
      try {
        await createAdminOwnerProperty(this.propertyCreateOwner.id, this.propertyCreateForm);
        const ownerId = this.propertyCreateOwner.id;
        await this.loadOwners(ownerId);
        if (this.isPropertyMode) await this.loadProperties();
        this.closePropertyCreateDialog();
        this.page.showToast('房產已建立並綁定業主');
      } catch (error) { this.propertyCreateError = error.message || '建立房產失敗'; }
      finally { this.propertyCreating = false; }
    },
    primaryProperty(owner) { return owner?.properties?.[0] || null; },
    initials(name) { return String(name || '').split(/\s+/).filter(Boolean).slice(0, 2).map(part => part[0]).join('').toUpperCase() || 'CC'; },
    money(value) { return Number(value || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); },
    lifecycleLabel(stage) { return ({ PRE_HANDOVER: '未交房', OPERATING: '已交房', DISPOSED: '已處置' })[stage] || '未設定'; },
    lifecycleClass(stage) { return stage === 'OPERATING' ? 'green' : stage === 'DISPOSED' ? 'gray' : 'orange'; },
    serviceLabel(service) { return ({ RENTAL: '出租', RESALE: '代售', MANAGEMENT: '代管' })[service] || service; },
    statusLabel(status) { return ({ paid: '正常', paying: '正常', due_soon: '待處理', overdue: '逾期', not_configured: '待設定', not_applicable: '不適用' })[status] || '待設定'; },
    listingStatusLabel(status) { return ({ available: '可用', reserved: '已預留', sold: '已出售', rented: '出租中', inactive: '停用' })[status] || status || '未設定'; },
    rentalStatusParam(value) { return ({ '未交房': 'pre_handover', '已交房待出租': 'pending_rental', '出租中': 'rented', '未啟用出租': 'not_for_rent' })[value] || ''; },
    rentalStatusLabel(value) { return ({ pre_handover: '未交房', pending_rental: '已交房待出租', rented: '出租中', not_for_rent: '未啟用出租' })[value] || '未設定'; },
    rentalStatusClass(value) { return value === 'rented' ? 'green' : value === 'pending_rental' ? 'orange' : value === 'pre_handover' ? 'gray' : 'blue'; },
    statusClass(status) { return status === 'paid' || status === 'paying' ? 'green' : status === 'overdue' ? 'red' : 'orange'; },
    syncOwnerExportRows(owners) {
      this.page.adminOwnerExportHeaders = ['業主姓名', '證件號', '手機號', '郵箱', '房產數量', '帳號狀態'];
      this.page.adminOwnerExportRows = owners.map(owner => [owner.fullName, owner.identityNo || '', owner.phone || '', owner.email || '', owner.properties.length, owner.status === 'active' ? '啟用' : '停用']);
    },
    syncPropertyExportRows(properties) {
      this.page.adminOwnerExportHeaders = ['建案／項目', '城市', '棟', '樓層', '單位編號', '業主姓名', '手機號', '房型', '面積（m²）', '房產階段', '單位狀態', '房產總價（RM）', '已繳金額（RM）', '未繳金額（RM）', '持有比例（%）', '主房主'];
      this.page.adminOwnerExportRows = properties.map(property => [property.projectName || '', property.city || '', property.building || '', property.floorNo || '', property.unitNo || '', property.ownerName || '', property.ownerPhone || '', property.unitType || '', property.areaSqm ?? '', this.lifecycleLabel(property.assetStage), this.listingStatusLabel(property.listingStatus), Number(property.purchasePrice || 0), Number(property.paidAmount || 0), Number(property.remainingAmount || 0), property.ownershipPercent ?? '', property.primary ? '是' : '否']);
    },
    async openDetails(property) { await this.openPropertyDialog('details', property); },
    async openEdit(property) { await this.openPropertyDialog('edit', property); },
    async openPropertyDialog(mode, property) {
      this.dialogMode = mode; this.dialogLoading = true; this.dialogError = ''; this.dialogProperty = null;
      this.$refs.propertyDialog.showModal();
      try {
        const fresh = await fetchAdminOwnerProperty(this.selectedOwner.id, property.ownerUnitId);
        this.dialogProperty = fresh; this.fillEditForm(fresh);
      } catch (error) { this.dialogError = error.message || '房產資料載入失敗'; }
      finally { this.dialogLoading = false; }
    },
    fillEditForm(property) {
      this.editForm = { building: property.building || '', floorNo: property.floorNo || '', unitNo: property.unitNo || '', unitType: property.unitType || '', areaSqm: property.areaSqm, bedroomCount: property.bedroomCount, listingStatus: property.listingStatus || 'available', assetStage: property.assetStage || 'PRE_HANDOVER', expectedHandoverDate: property.expectedHandoverDate || '', actualHandoverDate: property.actualHandoverDate || '', services: [...(property.services || [])], purchasePrice: Number(property.purchasePrice || 0) };
    },
    closeDialog() { this.$refs.propertyDialog?.close(); },
    async saveProperty() {
      if (!this.dialogProperty || !String(this.editForm.unitNo || '').trim()) { this.dialogError = '請填寫單位編號'; return; }
      if (this.editForm.assetStage === 'OPERATING' && !this.editForm.actualHandoverDate) { this.dialogError = '已交房房產需要填寫實際交房日期'; return; }
      this.saving = true; this.dialogError = '';
      try {
        const updated = await updateAdminOwnerProperty(this.selectedOwner.id, this.dialogProperty.ownerUnitId, this.editForm);
        const owner = this.selectedOwner;
        owner.properties = owner.properties.map(item => item.ownerUnitId === updated.ownerUnitId ? updated : item);
        this.dialogProperty = updated; this.fillEditForm(updated);
        if (this.isPropertyMode) await this.loadProperties();
        this.page.showToast('房產資料已更新'); this.closeDialog();
      } catch (error) { this.dialogError = error.message || '房產修改失敗'; }
      finally { this.saving = false; }
    }
  }
};
</script>
