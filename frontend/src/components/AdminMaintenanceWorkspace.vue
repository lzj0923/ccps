<template>
  <section class="content-grid admin-maintenance-workspace">
    <div class="panel table-panel maintenance-list-panel">
      <div class="panel-head">
        <div><h2>{{ $t('legacy.t_43337f59c780') }}</h2><span>{{ filteredRows.length }} {{ $t('legacy.t_ab0eac290998') }}</span></div>
        <div class="maintenance-head-actions"><button v-if="lastDeletedRecycleBinId" type="button" class="maintenance-undo-btn" @click="undoLastDelete">撤销删除</button><button type="button" class="maintenance-recycle-btn" @click="openRecycleBin">回收站</button><div class="maintenance-tabs">
          <button type="button" :class="{ active: activeTab === 'expense' }" @click="selectTab('expense')">{{ $t('legacy.t_3361c4c6f1ee') }}</button>
          <button type="button" :class="{ active: activeTab === 'maintenance' }" @click="selectTab('maintenance')">{{ $t('legacy.t_643db1590668') }}</button>
        </div></div>
      </div>

      <div v-if="loading" class="admin-owner-state">{{ $t('legacy.t_62d0616a0881') }}</div>
      <div v-else-if="errorMessage" class="admin-owner-state error"><strong>{{ $t('legacy.t_53afb862b922') }}</strong><span>{{ errorMessage }}</span><button @click="loadData">{{ $t('legacy.t_0c9157b5bfac') }}</button></div>
      <div v-else class="table-wrap maintenance-table-wrap">
        <table v-if="activeTab === 'expense'">
          <thead><tr><th>{{ $t('legacy.t_b6fed9af8313') }}</th><th>{{ $t('legacy.t_114246450ff0') }}</th><th>{{ $t('legacy.t_0cf468db12ee') }}</th><th>{{ $t('legacy.t_9b6c1b038aa5') }}</th><th>{{ $t('legacy.t_380086757011') }}</th><th>{{ $t('legacy.t_5c0ec3674a79') }}</th><th>{{ $t('legacy.t_607b3e1024c4') }}</th><th>{{ $t('legacy.t_99f6fe6c41ad') }}</th><th>{{ $t('legacy.t_f3ea6d345e2a') }}</th></tr></thead>
          <tbody>
            <tr v-if="!filteredRows.length"><td colspan="9" class="admin-owner-empty">{{ $t('legacy.t_3818efa5c5aa') }}</td></tr>
            <tr v-for="row in pagedRows()" :key="`expense-${row.id}`" :class="{ selected: selectedKey === rowKey(row) }" @click="selectRow(row)">
              <td>{{ displayDate(row.occurredOn) }}</td>
              <td><strong>{{ row.projectName }}</strong><small>{{ row.unitNo }}</small></td>
              <td><span class="maintenance-category">{{ categoryLabel(row.category) }}</span></td>
              <td class="maintenance-description">{{ row.description }}</td>
              <td><strong>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(row.amount) }}</strong></td>
              <td :class="{ 'money-gold': Number(row.reserveDeductedAmount) > 0 }">{{ $t('legacy.t_5e7b60c626a4') }} {{ money(row.reserveDeductedAmount) }}</td>
              <td><span class="tag" :class="statusClass(paymentLabel(row.paymentStatus))">{{ paymentLabel(row.paymentStatus) }}</span></td>
              <td>{{ Number(row.attachmentCount || 0) }} {{ $t('legacy.t_aa9f1ad4f91c') }}</td>
              <td class="maintenance-row-actions">
                <div class="maintenance-action-group">
                  <button type="button" class="maintenance-detail-btn" @click.stop="selectRow(row)">{{ $t('legacy.t_0596bf73ba05') }}</button>
                  <button v-if="row.editable" type="button" class="maintenance-edit-btn" @click.stop="openExpenseEdit(row)">{{ $t('projectManagement.edit') }}</button>
                  <button v-if="row.editable" type="button" class="maintenance-delete-btn" @click.stop="removeExpense(row)">{{ $t('projectManagement.delete') }}</button>
                </div>
                <span v-if="!row.editable" class="maintenance-state-pill is-confirmed">财务已确认</span>
              </td>
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
              <td class="maintenance-row-actions">
                <div class="maintenance-action-group">
                  <button type="button" class="maintenance-detail-btn" @click.stop="selectRow(row)">{{ $t('legacy.t_0596bf73ba05') }}</button>
                  <button v-if="maintenanceRowEditable(row)" type="button" class="maintenance-edit-btn" @click.stop="openMaintenanceEdit(row)">{{ $t('projectManagement.edit') }}</button>
                  <button v-if="maintenanceRowEditable(row)" type="button" class="maintenance-delete-btn" @click.stop="removeMaintenance(row)">{{ $t('projectManagement.delete') }}</button>
                  <button v-if="!['completed','cancelled'].includes(row.status) || row.confirmationStatus === 'rejected' || row.paymentStatus === 'voided'" type="button" class="maintenance-handle-btn" @click.stop="openHandling(row)">{{ row.confirmationStatus === 'rejected' || row.paymentStatus === 'voided' ? '处理' : $t('legacy.t_fda275e0bcc3') }}</button>
                </div>
                <span v-if="row.confirmationStatus === 'confirmed' && row.status !== 'cancelled'" class="maintenance-state-pill is-confirmed">财务已确认</span>
                <span v-else-if="row.confirmationStatus === 'rejected'" class="maintenance-state-pill is-rejected">财务已退回</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="pager"><span>{{ $t('legacy.t_3b6ef811b85a') }} {{ filteredRows.length }} {{ $t('legacy.t_86761b63a7bd') }}</span><div class="admin-building-pager"><button :disabled="listPage <= 1" @click="goListPage(listPage - 1)">&lt;</button><button v-for="page in listPages()" :key="page" :class="{ active: page === listPage }" @click="goListPage(page)">{{ page }}</button><button :disabled="listPage >= totalListPages()" @click="goListPage(listPage + 1)">&gt;</button><select v-model.number="listPageSize"><option :value="5">{{ $t('building.recordsPerPage', { count: 5 }) }}</option><option :value="10">{{ $t('building.recordsPerPage', { count: 10 }) }}</option><option :value="20">{{ $t('building.recordsPerPage', { count: 20 }) }}</option><option :value="50">{{ $t('building.recordsPerPage', { count: 50 }) }}</option></select></div></div>
    </div>

    <div v-if="maintenanceDetailOpen && selectedRow" class="maintenance-detail-modal" role="dialog" aria-modal="true" @pointerdown.self="closeMaintenanceDetail">
    <aside class="panel detail-panel maintenance-detail-panel">
      <div class="maintenance-detail-modal-head"><strong>{{ $t('legacy.t_0596bf73ba05') }}</strong><button type="button" class="maintenance-detail-close" :aria-label="$t('ui.close')" @click="closeMaintenanceDetail">×</button></div>
      <div v-if="selectedRow" class="detail-card">
        <div class="profile">
          <div class="big-avatar">{{ activeTab === 'expense' ? $t('legacy.t_18d2086d6a02') : $t('legacy.t_ea97fb39f031') }}</div>
          <div><h3>{{ detailTitle }}</h3><p>{{ selectedRow.projectName }} · {{ selectedRow.unitNo }}</p></div>
          <span class="tag" :class="statusClass(detailStatus)">{{ detailStatus }}</span>
        </div>
        <div class="detail-section">
          <h4><span class="num">{{ $t('legacy.t_356a192b7913') }}</span>{{ $t('legacy.t_c738ebbf02dd') }}</h4>
          <div class="kv"><span>{{ activeTab === 'expense' ? $t('legacy.t_f48697f8b6ec') : $t('legacy.t_dde3113a1be2') }}</span><b>{{ activeTab === 'expense' ? displayDate(selectedRow.occurredOn) : dateTime(selectedRow.requestedAt) }}</b></div>
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
    </div>

    <dialog ref="expenseCreateDialog" class="modal maintenance-create-dialog">
      <form method="dialog" @submit.prevent="submitExpense">
        <div class="modal-head"><div><h3>{{ editingExpenseId ? $t('projectManagement.edit') + $t('legacy.t_3361c4c6f1ee') : $t('legacy.t_16c7d92e55ce') }}</h3><small>{{ editingExpenseId ? '可修改错误资料或付款方式；单位不可更换。' : $t('legacy.t_8ca893baf0d3') }}</small></div><button type="button" class="icon-close" @click="closeExpenseCreate">×</button></div>
        <div v-if="optionsLoading" class="admin-owner-state">{{ $t('legacy.t_c330ff111b3f') }}</div>
        <div v-else class="maintenance-create-body">
          <label class="wide">{{ $t('legacy.t_114246450ff0') }}<select v-model.number="expenseForm.unitId" :disabled="Boolean(editingExpenseId)" required><option disabled value="">{{ $t('legacy.t_06dd9be6b9c7') }}</option><option v-for="unit in options.units" :key="unit.unitId" :value="unit.unitId">{{ unit.projectName }} · {{ unit.unitNo }}（{{ unit.ownerName }}）</option></select></label>
          <label>{{ $t('legacy.t_0cf468db12ee') }}<select v-model="expenseForm.category" required><option value="utilities">{{ $t('legacy.t_39e538e57590') }}</option><option value="management_service_fee">{{ $t('legacy.t_5f6a3d7c1e90') }}</option><option value="land_tax">{{ $t('legacy.t_2e8c9b4a6d71') }}</option><option value="assessment_tax">{{ $t('legacy.t_8b4d1f6a3c20') }}</option><option value="fire_insurance">{{ $t('legacy.t_4c7e2a9d5b13') }}</option><option value="agency_commission">{{ $t('legacy.t_6d1b8f3e4a72') }}</option><option value="cleaning">{{ $t('legacy.t_00e326047628') }}</option><option value="management">{{ $t('legacy.t_a178daac2527') }}</option><option value="maintenance">{{ $t('legacy.t_018bde2b7e24') }}</option><option value="other">{{ $t('legacy.t_c90e8ecbb54d') }}</option></select></label>
          <label>{{ $t('legacy.t_21eebaaf5746') }}<input v-model="expenseForm.occurredOn" type="date" required></label>
           <label>{{ $t('legacy.t_338501824154') }}<input v-model.number="expenseForm.amount" type="number" min="0.01" step="0.01" required></label>
           <label>{{ $t('legacy.t_ab2dbd55b3fb') }}<select v-model="expenseForm.settlementMethod" required><option value="unpaid">{{ $t('legacy.t_9a2e6c4f8b17') }}</option><option value="reserve" :disabled="!expenseReserveAvailable">{{ $t('legacy.t_ea99cc82f746') }}</option><option value="direct_payment" :disabled="!expenseDirectPaymentAllowed">{{ $t('legacy.t_f5d4d7b78adb') }}</option></select><small v-if="selectedExpenseUnit && !expenseDirectPaymentAllowed" class="handling-warning">业主已解约，代付款已停用。</small></label>
           <section class="expense-linked-fields expense-bank-fields wide">
             <header><strong>银行信息</strong><small>先选择付款方类型，再选择该业主或管理层的已有银行账户；选择“新建账户”时才填写完整资料。</small></header>
             <label>付款方类型<select v-model="expenseForm.payerType" required @change="onExpensePayerTypeChange"><option value="owner">业主</option><option value="management">管理层</option><option value="other">其他</option></select></label>
             <label>所属账户<select v-model="expenseForm.accountProfileChoice" :disabled="expenseMasterLoading" required @change="applyExpenseAccountProfileChoice"><option disabled value="">请选择已有账户或新建账户</option><option v-for="option in expenseAccountProfileOptions" :key="option.value" :value="option.value">{{ option.label }}</option><option value="__new__">新建账户</option></select></label>
             <template v-if="expenseForm.accountProfileChoice === '__new__' && expenseForm.payerType === 'owner'">
               <label>银行名称<input v-model.trim="ownerBankForm.itemName" maxlength="120" required></label>
               <label>收款人姓名<input v-model.trim="ownerBankForm.paymentName" maxlength="160" required></label>
               <label>银行账号<input v-model.trim="ownerBankForm.accountNo" maxlength="120" required></label>
               <label>银行／分行代码<input v-model.trim="ownerBankForm.branchCode" maxlength="80"></label>
               <label class="wide">银行地址<textarea v-model.trim="ownerBankForm.bankAddress" rows="3" maxlength="500"></textarea></label>
               <label>SWIFT 代码<input v-model.trim="ownerBankForm.swiftCode" maxlength="80"></label>
               <label>单日转账额度（RM）<input v-model.number="ownerBankForm.transferLimit" type="number" min="0" step="0.01" placeholder="0 表示无限制"><small>填写 0 或留空表示没有额度限制。</small></label>
               <label class="expense-overseas-toggle wide"><input v-model="ownerBankForm.overseasBank" type="checkbox"><span>是否为海外银行</span></label>
               <label v-if="ownerBankForm.overseasBank">海外银行汇款手续费（RM）<input v-model.number="ownerBankForm.overseasTransferFee" type="number" min="0" step="0.01" required></label>
               <label class="wide">备注<textarea v-model.trim="ownerBankForm.remarks" rows="3" maxlength="1000"></textarea></label>
             </template>
             <template v-else-if="expenseForm.accountProfileChoice === '__new__' && expenseForm.payerType === 'management'">
               <label>管理层名称<input v-model.trim="managementBankForm.managementName" maxlength="160" required placeholder="例如：海天公寓管理层"></label>
               <label>费用用途<input v-model.trim="managementBankForm.purpose" maxlength="160" required placeholder="例如：管理费／维修基金"></label>
               <label>银行名称<input v-model.trim="managementBankForm.bankName" maxlength="120" required></label>
               <label>银行账号<input v-model.trim="managementBankForm.accountNo" maxlength="120" required></label>
               <label>收款户名<input v-model.trim="managementBankForm.accountName" maxlength="160" required></label>
               <label>分行／SWIFT<input v-model.trim="managementBankForm.branchOrSwift" maxlength="120"></label>
               <label class="wide">备注<textarea v-model.trim="managementBankForm.remarks" rows="3" maxlength="1000"></textarea></label>
             </template>
             <template v-else-if="expenseForm.accountProfileChoice === '__new__' && expenseForm.payerType === 'other'">
               <label>付款方名称<input v-model.trim="otherPaymentForm.payerName" maxlength="160" required></label>
               <label>银行名称<input v-model.trim="otherPaymentForm.bankName" maxlength="120" required></label>
               <label>支付账户号码<input v-model.trim="otherPaymentForm.paymentAccountNo" maxlength="120" required></label>
             </template>
             <template v-else-if="expenseForm.accountProfileChoice">
               <div class="expense-account-summary"><span>付款方<strong>{{ expenseForm.payerName }}</strong></span><span>银行<strong>{{ expenseForm.bankName }}</strong></span><span>支付账户<strong>{{ expenseForm.paymentAccountNo }}</strong></span></div>
             </template>
             <p v-if="expenseMasterLoading" class="expense-master-note">正在读取房产付款资料…</p>
             <p v-else-if="expenseMasterError" class="admin-property-error">{{ expenseMasterError }}</p>
           </section>
           <section class="expense-linked-fields expense-fee-account-fields wide">
             <header><strong>费用账户号码</strong><small>选择房产已有费用账户，或新建账户类型和号码；新建资料会回写房产基本资料。</small></header>
             <label>费用账户<select v-model="expenseForm.feeAccountChoice" :disabled="expenseMasterLoading" required @change="applyExpenseFeeAccountChoice"><option disabled value="">请选择费用账户</option><option v-for="option in expenseFeeAccountOptions" :key="option.value" :value="option.value">{{ option.label }}</option><option value="__new__">新建费用账户</option></select></label>
             <template v-if="expenseForm.feeAccountChoice === '__new__'">
               <label>费用账户类型<select v-model="expenseForm.feeAccountKey" required><option disabled value="">请选择账户类型</option><option v-for="option in feeAccountTypes" :key="option.key" :value="option.key">{{ option.label }}</option></select></label>
               <label>新费用账户号码<input v-model.trim="expenseForm.feeAccountNo" maxlength="120" required placeholder="请输入费用账户号码"></label>
             </template>
           </section>
          <section v-if="selectedExpenseUnit" class="create-unit-summary wide">
            <span>{{ $t('legacy.t_ef2b2d104853') }}<b>{{ selectedExpenseUnit.ownerName }}</b></span><span>{{ $t('legacy.t_facfa1a1db0d') }}<b>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(selectedExpenseUnit.reserveBalance) }}</b></span><span :class="{ shortage: expenseReserveAfter < 0 }">{{ $t('legacy.t_37c0465513e1') }}<b>RM {{ money(expenseReserveAfter) }}</b></span>
          </section>
          <label class="wide">{{ $t('legacy.t_a52dafecf965') }}<textarea v-model.trim="expenseForm.description" maxlength="500" rows="4" required :placeholder="$t('legacy.t_520b35536c38')"></textarea></label>
          <p v-if="expenseCreateError" class="admin-property-error wide">{{ expenseCreateError }}</p>
        </div>
        <menu><button type="button" @click="closeExpenseCreate">{{ $t('legacy.t_4d0b4688c787') }}</button><button class="primary-btn" :disabled="expenseSaving || optionsLoading">{{ expenseSaving ? $t('legacy.t_8488ea2522af') : editingExpenseId ? '保存修改' : $t('legacy.t_8c962ae7a7fb') }}</button></menu>
      </form>
    </dialog>

    <dialog ref="maintenanceCreateDialog" class="modal maintenance-create-dialog">
      <form method="dialog" @submit.prevent="submitMaintenanceCreate">
        <div class="modal-head"><div><h3>{{ editingMaintenanceId ? $t('projectManagement.edit') + $t('legacy.t_643db1590668') : $t('legacy.t_1c14558442e7') }}</h3><small>{{ editingMaintenanceId ? '修改工单资料与处理状态。' : $t('legacy.t_138f739a4607') }}</small></div><button type="button" class="icon-close" @click="closeMaintenanceCreate">×</button></div>
        <div v-if="optionsLoading" class="admin-owner-state">{{ $t('legacy.t_ff1df444fc44') }}</div>
        <div v-else class="maintenance-create-body">
          <label class="wide">{{ $t('legacy.t_114246450ff0') }}<select v-model.number="maintenanceCreateForm.unitId" :disabled="Boolean(editingMaintenanceId)" required><option disabled value="">{{ $t('legacy.t_06dd9be6b9c7') }}</option><option v-for="unit in options.units" :key="unit.unitId" :value="unit.unitId">{{ unit.projectName }} · {{ unit.unitNo }}（{{ unit.ownerName }}）</option></select></label>
          <label>{{ $t('legacy.t_4e3157d2e547') }}<select v-model="maintenanceCreateForm.category" required><option value="plumbing">{{ $t('legacy.t_925558624144') }}</option><option value="air_conditioning">{{ $t('legacy.t_e7ec651a7c0f') }}</option><option value="electrical">{{ $t('legacy.t_c230e3bc3ab6') }}</option><option value="painting">{{ $t('legacy.t_6a81927031d5') }}</option><option value="other">{{ $t('legacy.t_587c86584e8e') }}</option></select></label>
          <label>{{ $t('legacy.t_2127eb1f484f') }}<select v-model="maintenanceCreateForm.vendorId"><option :value="null">{{ $t('legacy.t_4e4ceafa8ee3') }}</option><option v-for="vendor in options.vendors" :key="vendor.id" :value="vendor.id">{{ vendorOptionLabel(vendor) }}</option></select></label>
           <label>{{ $t('legacy.t_dde3113a1be2') }}<input v-model="maintenanceCreateForm.requestedAt" type="datetime-local" required></label>
           <label>预计金额（RM）<input v-model.number="maintenanceCreateForm.estimatedAmount" type="number" min="0" step="0.01"></label>
           <section class="expense-linked-fields maintenance-bank-fields wide">
             <header><strong>银行信息</strong><small>先选择付款方类型，再选择该业主或管理层的已有银行账户；选择“新建账户”时才填写完整资料。</small></header>
             <label>付款方类型<select v-model="maintenanceCreateForm.payerType" required @change="onMaintenancePayerTypeChange"><option value="owner">业主</option><option value="management">管理层</option><option value="other">其他</option></select></label>
             <label>所属账户<select v-model="maintenanceCreateForm.accountProfileChoice" :disabled="expenseMasterLoading" required @change="applyMaintenanceAccountProfileChoice"><option disabled value="">请选择已有账户或新建账户</option><option v-for="option in maintenanceAccountProfileOptions" :key="option.value" :value="option.value">{{ option.label }}</option><option value="__new__">新建账户</option></select></label>
             <template v-if="maintenanceCreateForm.accountProfileChoice === '__new__' && maintenanceCreateForm.payerType === 'owner'">
               <label>银行名称<input v-model.trim="ownerBankForm.itemName" maxlength="120" required></label>
               <label>收款人姓名<input v-model.trim="ownerBankForm.paymentName" maxlength="160" required></label>
               <label>银行账号<input v-model.trim="ownerBankForm.accountNo" maxlength="120" required></label>
               <label>银行／分行代码<input v-model.trim="ownerBankForm.branchCode" maxlength="80"></label>
               <label class="wide">银行地址<textarea v-model.trim="ownerBankForm.bankAddress" rows="3" maxlength="500"></textarea></label>
               <label>SWIFT 代码<input v-model.trim="ownerBankForm.swiftCode" maxlength="80"></label>
               <label>单日转账额度（RM）<input v-model.number="ownerBankForm.transferLimit" type="number" min="0" step="0.01" placeholder="0 表示无限制"><small>填写 0 或留空表示没有额度限制。</small></label>
               <label class="expense-overseas-toggle wide"><input v-model="ownerBankForm.overseasBank" type="checkbox"><span>是否为海外银行</span></label>
               <label v-if="ownerBankForm.overseasBank">海外银行汇款手续费（RM）<input v-model.number="ownerBankForm.overseasTransferFee" type="number" min="0" step="0.01" required></label>
               <label class="wide">备注<textarea v-model.trim="ownerBankForm.remarks" rows="3" maxlength="1000"></textarea></label>
             </template>
             <template v-else-if="maintenanceCreateForm.accountProfileChoice === '__new__' && maintenanceCreateForm.payerType === 'management'">
               <label>管理层名称<input v-model.trim="managementBankForm.managementName" maxlength="160" required placeholder="例如：海天公寓管理层"></label>
               <label>费用用途<input v-model.trim="managementBankForm.purpose" maxlength="160" required placeholder="例如：管理费／维修基金"></label>
               <label>银行名称<input v-model.trim="managementBankForm.bankName" maxlength="120" required></label>
               <label>银行账号<input v-model.trim="managementBankForm.accountNo" maxlength="120" required></label>
               <label>收款户名<input v-model.trim="managementBankForm.accountName" maxlength="160" required></label>
               <label>分行／SWIFT<input v-model.trim="managementBankForm.branchOrSwift" maxlength="120"></label>
               <label class="wide">备注<textarea v-model.trim="managementBankForm.remarks" rows="3" maxlength="1000"></textarea></label>
             </template>
             <template v-else-if="maintenanceCreateForm.accountProfileChoice === '__new__' && maintenanceCreateForm.payerType === 'other'">
               <label>付款方名称<input v-model.trim="otherPaymentForm.payerName" maxlength="160" required></label>
               <label>银行名称<input v-model.trim="otherPaymentForm.bankName" maxlength="120" required></label>
               <label>支付账户号码<input v-model.trim="otherPaymentForm.paymentAccountNo" maxlength="120" required></label>
             </template>
             <template v-else-if="maintenanceCreateForm.accountProfileChoice">
               <div class="expense-account-summary"><span>付款方<strong>{{ maintenanceCreateForm.payerName }}</strong></span><span>银行<strong>{{ maintenanceCreateForm.bankName }}</strong></span><span>支付账户<strong>{{ maintenanceCreateForm.paymentAccountNo }}</strong></span></div>
             </template>
             <p v-if="expenseMasterLoading" class="expense-master-note">正在读取房产付款资料…</p>
             <p v-else-if="expenseMasterError" class="admin-property-error">{{ expenseMasterError }}</p>
           </section>
           <section class="expense-linked-fields maintenance-fee-account-fields wide">
             <header><strong>费用账户号码</strong><small>选择房产已有费用账户，或新建账户类型和号码；新建资料会回写房产基本资料。</small></header>
             <label>费用账户<select v-model="maintenanceCreateForm.feeAccountChoice" :disabled="expenseMasterLoading" required @change="applyMaintenanceFeeAccountChoice"><option disabled value="">请选择费用账户</option><option v-for="option in maintenanceFeeAccountOptions" :key="option.value" :value="option.value">{{ option.label }}</option><option value="__new__">新建费用账户</option></select></label>
             <template v-if="maintenanceCreateForm.feeAccountChoice === '__new__'">
               <label>费用账户类型<select v-model="maintenanceCreateForm.feeAccountKey" required><option disabled value="">请选择账户类型</option><option v-for="option in feeAccountTypes" :key="option.key" :value="option.key">{{ option.label }}</option></select></label>
               <label>新费用账户号码<input v-model.trim="maintenanceCreateForm.feeAccountNo" maxlength="120" required placeholder="请输入费用账户号码"></label>
             </template>
           </section>
          <label v-if="editingMaintenanceId">工单状态<select v-model="maintenanceCreateForm.status" :disabled="maintenanceCreateForm.status === 'completed'" required><option value="submitted">待处理</option><option value="assigned">已指派</option><option value="in_progress">处理中</option><option value="inspection">待验收</option><option v-if="maintenanceCreateForm.status === 'completed'" value="completed">已完成</option></select></label>
          <label class="wide">{{ $t('legacy.t_38ca8573c24b') }}<input v-model.trim="maintenanceCreateForm.title" maxlength="180" required :placeholder="$t('legacy.t_332ded447fbd')"></label>
          <label class="wide">{{ $t('legacy.t_5586fd550c39') }}<textarea v-model.trim="maintenanceCreateForm.description" maxlength="1000" rows="4" :placeholder="$t('legacy.t_07202ef775ba')"></textarea></label>
          <p v-if="maintenanceCreateError" class="admin-property-error wide">{{ maintenanceCreateError }}</p>
        </div>
        <menu><button type="button" @click="closeMaintenanceCreate">{{ $t('legacy.t_4d0b4688c787') }}</button><button class="primary-btn" :disabled="maintenanceCreateSaving || optionsLoading">{{ maintenanceCreateSaving ? $t('legacy.t_2cd5496ec548') : editingMaintenanceId ? '保存修改' : $t('legacy.t_2a90d4ff462c') }}</button></menu>
      </form>
    </dialog>

    <dialog ref="handlingDialog" class="modal maintenance-handling-dialog">
      <form method="dialog" @submit.prevent="submitHandling">
        <div class="modal-head"><div><h3>{{ $t('legacy.t_3761878742b8') }}</h3><small>{{ handlingRow?.workOrderNo }} · {{ handlingRow?.projectName }} {{ handlingRow?.unitNo }}</small></div><button type="button" class="icon-close" @click="closeHandling">×</button></div>
        <p v-if="handlingRow?.confirmationStatus === 'rejected'" class="maintenance-finance-rejected-note">财务已退回：请核对原有资料，处理完成后将再次提交财务确认。</p>
        <div v-if="handlingLoading" class="admin-owner-state">{{ $t('legacy.t_e32212dc8516') }}</div>
        <div v-else class="maintenance-handling-body">
          <section class="handling-reserve-summary">
            <div><span>{{ $t('legacy.t_82d54c45b8dd') }}</span><strong>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(handlingInfo.reserveBalance) }}</strong></div>
            <div><span>{{ $t('legacy.t_fcd532741253') }}</span><strong>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(handlingInfo.reserveDeductedAmount) }}</strong></div>
            <div><span>{{ $t('legacy.t_3d416cf138b0') }}</span><strong>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(handlingForm.actualAmount) }}</strong></div>
            <div :class="{ shortage: reserveAfter < 0 }"><span>{{ $t('legacy.t_3c9d8e73179c') }}</span><strong>RM {{ money(reserveAfter) }}</strong></div>
          </section>

           <label>{{ $t('legacy.t_3b2b90c8aff2') }}<input v-model.number="handlingForm.actualAmount" type="number" min="0.01" step="0.01" required></label>
           <label>{{ $t('legacy.t_ab2dbd55b3fb') }}<select v-model="handlingForm.settlementMethod" required><option value="reserve" :disabled="!handlingInfo.reserveAccountAvailable">{{ $t('legacy.t_ea99cc82f746') }}</option><option value="direct_payment" :disabled="!handlingDirectPaymentAllowed">{{ $t('legacy.t_3afbca59e929') }}</option></select><small v-if="!handlingDirectPaymentAllowed" class="handling-warning">业主已解约，不能再提交代付款。</small><small v-else-if="handlingForm.settlementMethod === 'reserve' && reserveShortage > 0" class="handling-warning">{{ $t('legacy.t_c1130213b25d') }}</small></label>
           <label>付款方<input v-model.trim="handlingForm.payerName" maxlength="160" placeholder="例如：业主或管理层"></label>
           <label>银行<input v-model.trim="handlingForm.bankName" maxlength="120" placeholder="银行名称"></label>
           <label>支付账户号码<input v-model.trim="handlingForm.paymentAccountNo" maxlength="80" placeholder="收付款账户号码"></label>

          <div class="handling-photo-grid">
            <label class="handling-photo-field"><span>{{ $t('legacy.t_c769e4276274') }} <b v-if="requiredHandlingPhotos">必须</b><em v-else>可选</em></span><input type="file" accept="image/jpeg,image/png,application/pdf" multiple @change="setHandlingFiles('before', $event)"><small>{{ $t('legacy.t_58705ba10b3c') }} {{ handlingInfo.beforePhotoCount || 0 }} {{ $t('legacy.t_e1eeb732c04c') }} {{ handlingFiles.before.length }} {{ $t('legacy.t_6dcb656fb70d') }}</small></label>
            <label class="handling-photo-field"><span>{{ $t('legacy.t_e4a60df0e601') }} <b v-if="requiredHandlingPhotos">必须</b><em v-else>可选</em></span><input type="file" accept="image/jpeg,image/png,application/pdf" multiple @change="setHandlingFiles('after', $event)"><small>{{ $t('legacy.t_58705ba10b3c') }} {{ handlingInfo.afterPhotoCount || 0 }} {{ $t('legacy.t_e1eeb732c04c') }} {{ handlingFiles.after.length }} {{ $t('legacy.t_6dcb656fb70d') }}</small></label>
            <label class="handling-photo-field"><span>{{ $t('legacy.t_addd26b636b6') }}</span><input type="file" accept="image/jpeg,image/png,application/pdf" multiple @change="setHandlingFiles('invoice', $event)"><small>{{ $t('legacy.t_39c9779e3d16') }} {{ handlingFiles.invoice.length }} {{ $t('legacy.t_7d54bc69bbe4') }}</small></label>
            <p class="handling-photo-rule">{{ requiredHandlingPhotos ? '维修金额超过 RM 500，维修前与维修后照片各至少上传 1 份。' : '维修金额不超过 RM 500，维修前与维修后照片可按需要上传。' }} 支持 JPG、PNG、PDF，单类最多 6 份。</p>
          </div>

          <label class="handling-note">{{ $t('legacy.t_51d8d317d95c') }}<textarea v-model.trim="handlingForm.completionNote" maxlength="500" rows="4" required :placeholder="$t('legacy.t_35d405d839f4')"></textarea></label>
          <p v-if="handlingError" class="admin-property-error wide">{{ handlingError }}</p>
        </div>
        <menu><button type="button" @click="closeHandling">{{ $t('legacy.t_4d0b4688c787') }}</button><button class="primary-btn" :disabled="handlingSaving || handlingLoading || (handlingForm.settlementMethod === 'direct_payment' && !handlingDirectPaymentAllowed)">{{ handlingSaving ? $t('legacy.t_1e038f9b55ec') : $t('legacy.t_dd2218bd6344') }}</button></menu>
      </form>
    </dialog>

    <dialog ref="recycleBinDialog" class="modal recycle-bin-dialog">
      <div class="modal-head"><div><h3>回收站</h3><small>已删除的收支与维修记录保留 30 天，逾期自动清理。</small></div><button type="button" class="icon-close" @click="closeRecycleBin">×</button></div>
      <div class="recycle-bin-body">
        <div v-if="recycleBinLoading" class="admin-owner-state">正在读取回收站…</div>
        <div v-else-if="recycleBinError" class="admin-owner-state error"><span>{{ recycleBinError }}</span><button type="button" @click="loadRecycleBin">重试</button></div>
        <div v-else-if="!recycleBin.length" class="admin-owner-empty">回收站目前没有记录</div>
        <ul v-else class="recycle-bin-list">
          <li v-for="item in recycleBin" :key="item.id">
            <div><strong>{{ item.title || item.referenceNo }}</strong><small>{{ item.entityType === 'expense' ? '支出记录' : '维修工单' }} · {{ item.projectName || '—' }} {{ item.unitNo || '' }} · RM {{ money(item.amount) }}</small><small>删除于 {{ dateTime(item.deletedAt) }} · 保留至 {{ dateTime(item.expiresAt) }}</small></div>
            <span><button type="button" class="recycle-restore-btn" @click="restoreRecycleItem(item)">恢复</button><button type="button" class="recycle-purge-btn" @click="purgeRecycleItem(item)">永久移除</button></span>
          </li>
        </ul>
      </div>
      <menu><button type="button" @click="closeRecycleBin">关闭</button></menu>
    </dialog>
  </section>
</template>

<script>
import { completeAdminMaintenance, createAdminExpense, createAdminMaintenance, createAdminPropertyBankAccount, deleteAdminExpense, deleteAdminMaintenance, fetchAdminExpenses, fetchAdminMaintenanceDetail, fetchAdminMaintenanceHandling, fetchAdminMaintenanceOptions, fetchAdminPropertyBankAccounts, fetchAdminPropertyBasicProfile, fetchAdminRecycleBin, purgeAdminRecycleBin, restoreAdminRecycleBin, saveAdminPropertyBasicProfile, updateAdminExpense, updateAdminMaintenance, uploadAdminMaintenancePhotos } from '../services/propertyApi';
import { formatDate, formatDateTime } from '../utils/dateFormat';

const STATUS_MAP = { submitted: '待處理', assigned: '已指派', in_progress: '處理中', inspection: '待驗收', completed: '已完成', cancelled: '已取消' };
const CATEGORY_MAP = { utilities: '水電費', management: '管理費', cleaning: '清潔費', maintenance: '維修費', plumbing: '管道維修', air_conditioning: '冷氣維修', electrical: '電器維修', painting: '油漆修繕', other: '其他支出' };
const FEE_ACCOUNT_TYPES = [
  { key: 'electricity', label: '电费' }, { key: 'water', label: '水费' },
  { key: 'sewerage', label: '排污费' }, { key: 'gas', label: '燃气费' },
  { key: 'withholdingTax', label: '预扣税' }, { key: 'landTax', label: '地税' },
  { key: 'assessmentTax', label: '门牌税' }
];
const managementAccountId = () => globalThis.crypto?.randomUUID?.() || `management-bank-${Date.now()}-${Math.random().toString(16).slice(2)}`;

export default {
  inject: ['page'],
  data() {
    return { activeTab: 'expense', listPage: 1, listPageSize: 5, maintenanceDetailOpen: false, response: { summary: {}, properties: [], expenses: [], maintenance: [] }, loading: false, errorMessage: '', selectedKey: '', maintenanceDetail: null, requestSerial: 0, options: { units: [], vendors: [] }, optionsLoading: false, exportPaymentProfiles: {}, editingExpenseId: null, expenseForm: this.emptyExpenseForm(), ownerBankForm: this.emptyOwnerBankForm(), managementBankForm: this.emptyManagementBankForm(), otherPaymentForm: this.emptyOtherPaymentForm(), expenseSaving: false, expenseCreateError: '', expenseMasterLoading: false, expenseMasterError: '', expenseMasterProfile: {}, expenseOwnerBankAccounts: [], expenseMasterRequestSerial: 0, editingMaintenanceId: null, maintenanceCreateForm: this.emptyMaintenanceForm(), maintenanceCreateSaving: false, maintenanceCreateError: '', handlingRow: null, handlingInfo: {}, handlingForm: { actualAmount: 0, settlementMethod: 'reserve', completionNote: '', payerName: '', bankName: '', paymentAccountNo: '', feeAccountKey: '', feeAccountNo: '' }, handlingFiles: { before: [], after: [], invoice: [] }, handlingLoading: false, handlingSaving: false, handlingError: '', recycleBin: [], recycleBinLoading: false, recycleBinError: '', lastDeletedRecycleBinId: null };
  },
  computed: {
    rows() { return this.activeTab === 'expense' ? this.response.expenses || [] : this.response.maintenance || []; },
    filteredRows() {
      const keyword = String(this.page.moduleSearch || this.page.globalSearch || '').trim().toLowerCase();
      const project = String(this.page.projectFilter || '');
      const status = String(this.page.statusFilter || '');
      return this.rows.filter(row => {
        if (project && !project.includes('全部') && row.projectName !== project) return false;
        if (this.page.adminMaintenanceDistrictFilter && !this.matchesDistrict(row.state || row.city, this.page.adminMaintenanceDistrictFilter)) return false;
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
    reserveAfter() { return Number(this.handlingInfo.reserveBalance || 0) - this.requiredReserveDebit; },
    selectedExpenseUnit() { return this.options.units.find(unit => Number(unit.unitId) === Number(this.expenseForm.unitId)) || null; },
    expenseShortage() { return Math.max(0, Number(this.expenseForm.amount || 0) - Number(this.selectedExpenseUnit?.reserveBalance || 0)); },
    expenseReserveAfter() { return Number(this.selectedExpenseUnit?.reserveBalance || 0) - Number(this.expenseForm.amount || 0); },
    expenseReserveAvailable() { return Boolean(this.selectedExpenseUnit?.reserveAccountId); },
    expenseDirectPaymentAllowed() { return this.selectedExpenseUnit?.directPaymentAllowed !== false; },
    selectedMaintenanceUnit() { return this.options.units.find(unit => Number(unit.unitId) === Number(this.maintenanceCreateForm.unitId)) || null; },
    feeAccountTypes() { return FEE_ACCOUNT_TYPES; },
    expenseManagementAccounts() { return Array.isArray(this.expenseMasterProfile?.managementBankAccounts) ? this.expenseMasterProfile.managementBankAccounts : []; },
    expenseFeeAccountOptions() {
      const numbers = this.expenseMasterProfile?.paymentAccountNumbers || {};
      return FEE_ACCOUNT_TYPES.filter(type => String(numbers[type.key] || '').trim()).map(type => ({ value: type.key, label: `${type.label} · ${String(numbers[type.key]).trim()}` }));
    },
    expenseAccountProfileOptions() {
      if (this.expenseForm.payerType === 'owner') return this.expenseOwnerBankAccounts.map((account, index) => ({ value: `owner:${account.id ?? index}`, label: `${account.paymentName || this.selectedExpenseUnit?.ownerName || '业主'} · ${account.itemName || '未填写银行'} · ${account.accountNo || '未填写账号'}`, payerName: account.paymentName || this.selectedExpenseUnit?.ownerName || '', bankName: account.itemName || '', paymentAccountNo: account.accountNo || '' })).filter(account => account.payerName && account.bankName && account.paymentAccountNo);
      if (this.expenseForm.payerType === 'management') return this.expenseManagementAccounts.map((account, index) => ({ value: `management:${account.id ?? index}`, label: `${account.managementName || account.accountName || '管理层'} · ${account.bankName || '未填写银行'} · ${account.accountNo || '未填写账号'}`, payerName: account.managementName || account.accountName || '', bankName: account.bankName || '', paymentAccountNo: account.accountNo || '' })).filter(account => account.payerName && account.bankName && account.paymentAccountNo);
      return [];
    },
    maintenanceFeeAccountOptions() { return this.expenseFeeAccountOptions; },
    maintenanceAccountProfileOptions() {
      if (this.maintenanceCreateForm.payerType === 'owner') return this.expenseOwnerBankAccounts.map((account, index) => ({ value: `owner:${account.id ?? index}`, label: `${account.paymentName || this.selectedMaintenanceUnit?.ownerName || '业主'} · ${account.itemName || '未填写银行'} · ${account.accountNo || '未填写账号'}`, payerName: account.paymentName || this.selectedMaintenanceUnit?.ownerName || '', bankName: account.itemName || '', paymentAccountNo: account.accountNo || '' })).filter(account => account.payerName && account.bankName && account.paymentAccountNo);
      if (this.maintenanceCreateForm.payerType === 'management') return this.expenseManagementAccounts.map((account, index) => ({ value: `management:${account.id ?? index}`, label: `${account.managementName || account.accountName || '管理层'} · ${account.bankName || '未填写银行'} · ${account.accountNo || '未填写账号'}`, payerName: account.managementName || account.accountName || '', bankName: account.bankName || '', paymentAccountNo: account.accountNo || '' })).filter(account => account.payerName && account.bankName && account.paymentAccountNo);
      return [];
    },
    expenseCreatesMasterAccount() { return this.expenseForm.payerType !== 'other' && this.expenseForm.accountProfileChoice === '__new__'; },
    maintenanceCreatesMasterAccount() { return this.maintenanceCreateForm.payerType !== 'other' && this.maintenanceCreateForm.accountProfileChoice === '__new__'; },
     handlingDirectPaymentAllowed() { return this.handlingInfo?.directPaymentAllowed !== false; },
     requiredHandlingPhotos() { return Number(this.handlingForm.actualAmount || 0) > 500; }
  },
  watch: {
    filteredRows: {
      immediate: true,
      handler() { this.syncExportRows(); }
    },
    'page.projectFilter'() { this.resetListPage(); },
    'page.statusFilter'() { this.resetListPage(); },
    'page.adminMaintenanceDistrictFilter'() { this.resetListPage(); },
    'page.moduleSearch'() { this.resetListPage(); },
    listPageSize() { this.resetListPage(); },
    'page.dateStart'() { this.loadData(); },
    'page.dateEnd'() { this.loadData(); },
    'page.adminExpenseCreateNonce'() { this.openExpenseCreate(); },
    'page.adminMaintenanceCreateNonce'() { this.openMaintenanceCreate(); },
    'expenseForm.unitId'(unitId) {
      if (unitId && this.selectedExpenseUnit) this.loadExpenseMasterData(unitId);
      else if (!unitId) this.resetExpenseMasterData();
    },
    'maintenanceCreateForm.unitId'(unitId) {
      if (unitId && this.selectedMaintenanceUnit) this.loadMaintenanceMasterData(unitId);
      else if (!unitId) this.resetExpenseMasterData();
    }
  },
  mounted() { this.loadData(); },
  methods: {
    syncExportRows() {
      const headers = ['日期', '行政区', '房产／单位', '业主', '租客', '收支类别', '收支说明', '金额', '支付方式', '付款方', '费用账户号码', '支付账户号码', 'Jompay', 'Ref No.'];
      if (this.activeTab === 'maintenance') {
        this.page.adminMaintenanceExportFileName = '维修支付报告.xlsx';
        this.page.adminMaintenanceExportHeaders = headers;
        this.page.adminMaintenanceExportRows = this.filteredRows.map(row => {
          const payment = this.exportPaymentData(row, 'maintenance');
          return [String(row.requestedAt || '').slice(0, 10), payment.region, [row.projectName, row.unitNo].filter(Boolean).join(' / '), payment.ownerName, payment.tenantName, this.categoryLabel(row.category), [row.title, row.description].filter(Boolean).join('：'), Number(row.amount || 0), this.paymentMethodLabel(row.paymentMethod), row.payerName || '', payment.feeAccountNo, row.paymentAccountNo || '', '', row.workOrderNo || ''];
        });
        return;
      }
      this.page.adminMaintenanceExportFileName = '支出支付报告.xlsx';
      this.page.adminMaintenanceExportHeaders = headers;
      this.page.adminMaintenanceExportRows = this.filteredRows.map(row => {
        const payment = this.exportPaymentData(row, 'expense');
        return [row.occurredOn || '', payment.region, [row.projectName, row.unitNo].filter(Boolean).join(' / '), payment.ownerName, payment.tenantName, this.categoryLabel(row.category), row.description || '', Number(row.amount || 0), this.paymentMethodLabel(row.paymentMethod), row.payerName || '', payment.feeAccountNo, row.paymentAccountNo || '', '', row.transactionNo || `EXP-${row.financeRecordId || row.id || ''}`];
      });
    },
    exportPaymentData(row, recordType) {
      const linked = this.exportPaymentProfiles?.[String(row.unitId)] || this.exportPaymentProfiles?.[row.unitId] || {};
      const profile = linked.profile || {};
      const ownerAccounts = Array.isArray(linked.ownerAccounts) ? linked.ownerAccounts : [];
      const managementAccounts = Array.isArray(profile.managementBankAccounts) ? profile.managementBankAccounts : [];
      const normalizedAccount = String(row.paymentAccountNo || '').trim();
      const account = [...ownerAccounts, ...managementAccounts].find(item => String(item.accountNo || '').trim() === normalizedAccount) || {};
      const numbers = profile.paymentAccountNumbers || {};
      const categoryFeeKey = { electricity: 'electricity', water: 'water', sewerage: 'sewerage', gas: 'gas', withholding_tax: 'withholdingTax', land_tax: 'landTax', assessment_tax: 'assessmentTax' }[row.category];
      const feeKey = row.feeAccountKey || categoryFeeKey || '';
      const feeType = FEE_ACCOUNT_TYPES.find(item => item.key === feeKey);
      const availableFees = FEE_ACCOUNT_TYPES.filter(item => String(numbers[item.key] || '').trim());
      const feeAccountNo = feeKey ? String(row.feeAccountNo || numbers[feeKey] || '').trim() : availableFees.map(item => `${item.label}：${String(numbers[item.key]).trim()}`).join('；');
      const recipientName = String(account.paymentName || account.accountName || row.payerName || '').trim();
      const bankName = String(row.bankName || account.itemName || account.bankName || '').trim();
      const bankAccountNo = normalizedAccount || String(account.accountNo || '').trim();
      const branchCode = String(account.branchCode || account.branchOrSwift || '').trim();
      const swiftCode = String(account.swiftCode || account.branchOrSwift || '').trim();
      const missing = [];
      if (!recipientName) missing.push('收款户名');
      if (!bankName) missing.push('银行名称');
      if (!bankAccountNo) missing.push('银行账号');
      if (!feeAccountNo) missing.push('费用账户号码');
      if (account.overseasBank && !swiftCode) missing.push('SWIFT 代码');
      const confirmation = String(row.confirmationStatus || 'pending');
      const paymentStatus = String(row.paymentStatus || 'unpaid');
      const payoutStatus = paymentStatus === 'paid' ? '已付款' : confirmation !== 'confirmed' ? '待财务确认' : missing.length ? '资料待补充' : '可出款';
      return {
        region: [row.state, row.city].filter(Boolean).join('／'), ownerName: linked.ownerName || '', tenantName: linked.tenantName || '',
        feeAccountType: feeType?.label || (availableFees.length ? '房产费用账户' : ''), feeAccountNo,
        recipientName, bankName, bankAccountNo, branchCode, swiftCode,
        bankAddress: account.bankAddress || '', overseasBank: account.overseasBank ? '是' : '否',
        overseasTransferFee: Number(account.overseasTransferFee || 0), transferLimit: Number(account.transferLimit || 0),
        payoutStatus, completeness: missing.length ? `缺少：${missing.join('、')}` : '完整'
      };
    },
    pagedRows() { const size = Number(this.listPageSize || 10); const page = Number(this.listPage || 1); return this.filteredRows.slice((page - 1) * size, page * size); },
    totalListPages() { return Math.max(1, Math.ceil(this.filteredRows.length / Number(this.listPageSize || 10))); },
    listPages() { return Array.from({ length: this.totalListPages() }, (_, index) => index + 1); },
    goListPage(page) {
      const nextPage = Math.min(Math.max(Number(page) || 1, 1), this.totalListPages());
      this.listPage = nextPage;
      this.ensureSelection();
    },
    resetListPage() { this.listPage = 1; this.ensureSelection(); },
    matchesDistrict(city, district) {
      const value = String(city || '').trim().toLowerCase();
      if (!value) return district === 'other';
      if (district === 'kuala_lumpur') return ['kuala lumpur', 'kl', '吉隆坡'].some(item => value === item || value.includes(item));
      if (district === 'johor') return ['johor', '新山', '柔佛'].some(item => value === item || value.includes(item));
      if (district === 'melaka') return ['melaka', 'malacca', '马六甲', '马六甲州'].some(item => value === item || value.includes(item));
      if (district === 'other') return !['kuala lumpur', 'kl', '吉隆坡', 'johor', '新山', '柔佛', 'melaka', 'malacca', '马六甲', '马六甲州'].some(item => value === item || value.includes(item));
      return true;
    },
    today() { return new Date().toLocaleDateString('en-CA'); },
    localDateTime() { const date = new Date(); date.setMinutes(date.getMinutes() - date.getTimezoneOffset()); return date.toISOString().slice(0, 16); },
    emptyExpenseForm() { return { unitId: '', category: 'utilities', description: '', amount: null, occurredOn: this.today(), settlementMethod: 'unpaid', payerType: 'owner', accountProfileChoice: '', payerName: '', feeAccountChoice: '', feeAccountKey: '', feeAccountNo: '', bankName: '', paymentAccountNo: '' }; },
    emptyOwnerBankForm() { return { itemName: '', paymentName: '', accountNo: '', bankAddress: '', branchCode: '', swiftCode: '', transferLimit: 0, overseasBank: false, overseasTransferFee: 0, remarks: '' }; },
    emptyManagementBankForm() { return { managementName: '', purpose: '', bankName: '', accountNo: '', accountName: '', branchOrSwift: '', remarks: '' }; },
    emptyOtherPaymentForm() { return { payerName: '', bankName: '', paymentAccountNo: '' }; },
    emptyMaintenanceForm() { return { unitId: '', vendorId: null, category: 'plumbing', title: '', description: '', requestedAt: this.localDateTime(), estimatedAmount: null, payerType: 'owner', accountProfileChoice: '', payerName: '', feeAccountChoice: '', feeAccountKey: '', feeAccountNo: '', bankName: '', paymentAccountNo: '', status: 'submitted' }; },
    async loadOptions() {
      this.optionsLoading = true;
      try { this.options = await fetchAdminMaintenanceOptions(); }
      catch (error) { throw new Error(error.message || '無法讀取單位與服務商資料'); }
      finally { this.optionsLoading = false; }
    },
    async openExpenseCreate() {
      this.editingExpenseId = null; this.expenseForm = this.emptyExpenseForm(); this.resetNewExpenseAccountForms(); this.resetExpenseMasterData(); this.expenseCreateError = ''; this.$refs.expenseCreateDialog?.showModal();
      try { await this.loadOptions(); } catch (error) { this.expenseCreateError = error.message; }
    },
    async openExpenseEdit(row) {
      this.editingExpenseId = row.id;
      this.resetNewExpenseAccountForms();
      this.expenseForm = { ...this.emptyExpenseForm(), unitId: row.unitId, category: row.category, description: row.description, amount: Number(row.amount || 0), occurredOn: row.occurredOn, settlementMethod: row.paymentMethod === 'reserve_account' || Number(row.reserveDeductedAmount || 0) > 0 ? 'reserve' : row.paymentMethod === 'direct_payment' ? 'direct_payment' : 'unpaid', payerName: row.payerName || '', bankName: row.bankName || '', paymentAccountNo: row.paymentAccountNo || '', feeAccountKey: row.feeAccountKey || '', feeAccountNo: row.feeAccountNo || '' };
      this.expenseCreateError = ''; this.$refs.expenseCreateDialog?.showModal();
      try { await this.loadOptions(); await this.loadExpenseMasterData(row.unitId); } catch (error) { this.expenseCreateError = error.message; }
    },
    closeExpenseCreate() { this.$refs.expenseCreateDialog?.close(); this.expenseCreateError = ''; this.editingExpenseId = null; },
    resetNewExpenseAccountForms() {
      this.ownerBankForm = this.emptyOwnerBankForm();
      this.managementBankForm = this.emptyManagementBankForm();
      this.otherPaymentForm = this.emptyOtherPaymentForm();
    },
    resetExpenseMasterData() {
      this.expenseMasterRequestSerial += 1;
      this.expenseMasterLoading = false;
      this.expenseMasterError = '';
      this.expenseMasterProfile = {};
      this.expenseOwnerBankAccounts = [];
    },
    async loadExpenseMasterData(unitId) {
      await this.loadLinkedMasterData(unitId, 'expense');
    },
    async loadMaintenanceMasterData(unitId) {
      await this.loadLinkedMasterData(unitId, 'maintenance');
    },
    async loadLinkedMasterData(unitId, target) {
      const unit = this.options.units.find(option => Number(option.unitId) === Number(unitId));
      if (!unit?.ownerId || !unit?.ownerUnitId) { this.expenseMasterError = '此单位缺少房产关联资料，无法读取付款账户。'; return; }
      const serial = ++this.expenseMasterRequestSerial;
      this.expenseMasterLoading = true; this.expenseMasterError = '';
      try {
        const [profile, accounts] = await Promise.all([
          fetchAdminPropertyBasicProfile(unit.ownerId, unit.ownerUnitId),
          fetchAdminPropertyBankAccounts(unit.ownerId, unit.ownerUnitId)
        ]);
        if (serial !== this.expenseMasterRequestSerial) return;
        this.expenseMasterProfile = profile || {};
        this.expenseOwnerBankAccounts = Array.isArray(accounts) ? accounts : [];
        if (target === 'maintenance') this.syncMaintenanceChoicesFromValues();
        else this.syncExpenseChoicesFromValues();
      } catch (error) {
        if (serial === this.expenseMasterRequestSerial) this.expenseMasterError = error.message || '房产付款资料读取失败';
      } finally {
        if (serial === this.expenseMasterRequestSerial) this.expenseMasterLoading = false;
      }
    },
    syncExpenseChoicesFromValues() {
      const form = this.expenseForm;
      if (form.payerName || form.bankName || form.paymentAccountNo) {
        const ownerMatch = this.expenseOwnerBankAccounts.find(account => String(account.paymentName || '').trim() === String(form.payerName || '').trim() && String(account.itemName || '').trim() === String(form.bankName || '').trim() && String(account.accountNo || '').trim() === String(form.paymentAccountNo || '').trim());
        const managementMatch = this.expenseManagementAccounts.find(account => [account.managementName, account.accountName].some(value => String(value || '').trim() === String(form.payerName || '').trim()) && String(account.bankName || '').trim() === String(form.bankName || '').trim() && String(account.accountNo || '').trim() === String(form.paymentAccountNo || '').trim());
        if (ownerMatch) form.payerType = 'owner';
        else if (managementMatch) form.payerType = 'management';
        const matched = this.expenseAccountProfileOptions.find(option => option.payerName === form.payerName && option.bankName === form.bankName && option.paymentAccountNo === form.paymentAccountNo);
        form.accountProfileChoice = matched?.value || '__new__';
        if (!matched) {
          if (form.payerType === 'management') this.managementBankForm = { ...this.emptyManagementBankForm(), managementName: form.payerName, bankName: form.bankName, accountNo: form.paymentAccountNo, accountName: form.payerName };
          else this.ownerBankForm = { ...this.emptyOwnerBankForm(), itemName: form.bankName, paymentName: form.payerName, accountNo: form.paymentAccountNo };
        }
      } else if (form.payerType === 'other') form.accountProfileChoice = '__new__';
      const feeMatch = this.expenseFeeAccountOptions.find(option => String(this.expenseMasterProfile?.paymentAccountNumbers?.[option.value] || '').trim() === String(form.feeAccountNo || '').trim());
      if (form.feeAccountNo && feeMatch) { form.feeAccountChoice = feeMatch.value; form.feeAccountKey = feeMatch.value; }
      else if (this.expenseFeeAccountOptions.length === 1) { form.feeAccountChoice = this.expenseFeeAccountOptions[0].value; this.applyExpenseFeeAccountChoice(); }
    },
    onExpensePayerTypeChange() {
      const form = this.expenseForm;
      form.accountProfileChoice = form.payerType === 'other' ? '__new__' : '';
      form.payerName = ''; form.bankName = ''; form.paymentAccountNo = '';
      this.resetNewExpenseAccountForms();
    },
    applyExpenseAccountProfileChoice() {
      const form = this.expenseForm;
      form.payerName = ''; form.bankName = ''; form.paymentAccountNo = '';
      this.resetNewExpenseAccountForms();
      if (form.accountProfileChoice === '__new__') return;
      const selected = this.expenseAccountProfileOptions.find(option => option.value === form.accountProfileChoice);
      if (!selected) return;
      form.payerName = selected.payerName; form.bankName = selected.bankName; form.paymentAccountNo = selected.paymentAccountNo;
    },
    applyExpenseFeeAccountChoice() {
      if (this.expenseForm.feeAccountChoice === '__new__') { this.expenseForm.feeAccountKey = ''; this.expenseForm.feeAccountNo = ''; return; }
      this.expenseForm.feeAccountKey = this.expenseForm.feeAccountChoice;
      this.expenseForm.feeAccountNo = String(this.expenseMasterProfile?.paymentAccountNumbers?.[this.expenseForm.feeAccountChoice] || '').trim();
    },
    syncMaintenanceChoicesFromValues() {
      const form = this.maintenanceCreateForm;
      if (form.payerName || form.bankName || form.paymentAccountNo) {
        const ownerMatch = this.expenseOwnerBankAccounts.find(account => String(account.paymentName || '').trim() === String(form.payerName || '').trim() && String(account.itemName || '').trim() === String(form.bankName || '').trim() && String(account.accountNo || '').trim() === String(form.paymentAccountNo || '').trim());
        const managementMatch = this.expenseManagementAccounts.find(account => [account.managementName, account.accountName].some(value => String(value || '').trim() === String(form.payerName || '').trim()) && String(account.bankName || '').trim() === String(form.bankName || '').trim() && String(account.accountNo || '').trim() === String(form.paymentAccountNo || '').trim());
        if (ownerMatch) form.payerType = 'owner';
        else if (managementMatch) form.payerType = 'management';
        const matched = this.maintenanceAccountProfileOptions.find(option => option.payerName === form.payerName && option.bankName === form.bankName && option.paymentAccountNo === form.paymentAccountNo);
        form.accountProfileChoice = matched?.value || '__new__';
        if (!matched) {
          if (form.payerType === 'management') this.managementBankForm = { ...this.emptyManagementBankForm(), managementName: form.payerName, bankName: form.bankName, accountNo: form.paymentAccountNo, accountName: form.payerName };
          else this.ownerBankForm = { ...this.emptyOwnerBankForm(), itemName: form.bankName, paymentName: form.payerName, accountNo: form.paymentAccountNo };
        }
      } else if (form.payerType === 'other') form.accountProfileChoice = '__new__';
      const feeMatch = this.maintenanceFeeAccountOptions.find(option => String(this.expenseMasterProfile?.paymentAccountNumbers?.[option.value] || '').trim() === String(form.feeAccountNo || '').trim());
      if (form.feeAccountNo && feeMatch) { form.feeAccountChoice = feeMatch.value; form.feeAccountKey = feeMatch.value; }
      else if (this.maintenanceFeeAccountOptions.length === 1) { form.feeAccountChoice = this.maintenanceFeeAccountOptions[0].value; this.applyMaintenanceFeeAccountChoice(); }
    },
    onMaintenancePayerTypeChange() {
      const form = this.maintenanceCreateForm;
      form.accountProfileChoice = form.payerType === 'other' ? '__new__' : '';
      form.payerName = ''; form.bankName = ''; form.paymentAccountNo = '';
      this.resetNewExpenseAccountForms();
    },
    applyMaintenanceAccountProfileChoice() {
      const form = this.maintenanceCreateForm;
      form.payerName = ''; form.bankName = ''; form.paymentAccountNo = '';
      this.resetNewExpenseAccountForms();
      if (form.accountProfileChoice === '__new__') return;
      const selected = this.maintenanceAccountProfileOptions.find(option => option.value === form.accountProfileChoice);
      if (!selected) return;
      form.payerName = selected.payerName; form.bankName = selected.bankName; form.paymentAccountNo = selected.paymentAccountNo;
    },
    applyMaintenanceFeeAccountChoice() {
      const form = this.maintenanceCreateForm;
      if (form.feeAccountChoice === '__new__') { form.feeAccountKey = ''; form.feeAccountNo = ''; return; }
      form.feeAccountKey = form.feeAccountChoice;
      form.feeAccountNo = String(this.expenseMasterProfile?.paymentAccountNumbers?.[form.feeAccountChoice] || '').trim();
    },
    applyNewMaintenanceAccountValues() {
      const form = this.maintenanceCreateForm;
      if (form.accountProfileChoice !== '__new__') return;
      if (form.payerType === 'owner') {
        form.payerName = String(this.ownerBankForm.paymentName || '').trim();
        form.bankName = String(this.ownerBankForm.itemName || '').trim();
        form.paymentAccountNo = String(this.ownerBankForm.accountNo || '').trim();
      } else if (form.payerType === 'management') {
        form.payerName = String(this.managementBankForm.managementName || '').trim();
        form.bankName = String(this.managementBankForm.bankName || '').trim();
        form.paymentAccountNo = String(this.managementBankForm.accountNo || '').trim();
      } else {
        form.payerName = String(this.otherPaymentForm.payerName || '').trim();
        form.bankName = String(this.otherPaymentForm.bankName || '').trim();
        form.paymentAccountNo = String(this.otherPaymentForm.paymentAccountNo || '').trim();
      }
    },
    validateMaintenanceLinkedFields() {
      const form = this.maintenanceCreateForm;
      this.applyNewMaintenanceAccountValues();
      if (!form.payerType || !form.accountProfileChoice) return '请选择付款方类型和所属账户。';
      if (!form.feeAccountChoice) return '请选择费用账户。';
      if (form.feeAccountChoice === '__new__' && (!form.feeAccountKey || !form.feeAccountNo.trim())) return '请填写新费用账户的类型和号码。';
      if (!form.feeAccountKey || !form.feeAccountNo.trim()) return '请选择有效的费用账户。';
      if (form.accountProfileChoice === '__new__' && form.payerType === 'owner') {
        if (!this.ownerBankForm.itemName.trim() || !this.ownerBankForm.paymentName.trim() || !this.ownerBankForm.accountNo.trim()) return '请完整填写业主账户的银行名称、收款人姓名和银行账号。';
        if (Number(this.ownerBankForm.transferLimit || 0) < 0 || Number(this.ownerBankForm.overseasTransferFee || 0) < 0) return '转账额度和海外汇款手续费不能小于 0。';
      }
      if (form.accountProfileChoice === '__new__' && form.payerType === 'management' && (!this.managementBankForm.managementName.trim() || !this.managementBankForm.purpose.trim() || !this.managementBankForm.bankName.trim() || !this.managementBankForm.accountNo.trim() || !this.managementBankForm.accountName.trim())) return '请完整填写管理层名称、费用用途、银行名称、银行账号和收款户名。';
      if (!form.payerName.trim()) return '请填写付款方名称。';
      if (!form.bankName.trim()) return '请填写银行名称。';
      if (!form.paymentAccountNo.trim()) return '请填写支付账户号码。';
      return '';
    },
    applyNewExpenseAccountValues() {
      if (this.expenseForm.accountProfileChoice !== '__new__') return;
      if (this.expenseForm.payerType === 'owner') {
        this.expenseForm.payerName = String(this.ownerBankForm.paymentName || '').trim();
        this.expenseForm.bankName = String(this.ownerBankForm.itemName || '').trim();
        this.expenseForm.paymentAccountNo = String(this.ownerBankForm.accountNo || '').trim();
      } else if (this.expenseForm.payerType === 'management') {
        this.expenseForm.payerName = String(this.managementBankForm.managementName || '').trim();
        this.expenseForm.bankName = String(this.managementBankForm.bankName || '').trim();
        this.expenseForm.paymentAccountNo = String(this.managementBankForm.accountNo || '').trim();
      } else {
        this.expenseForm.payerName = String(this.otherPaymentForm.payerName || '').trim();
        this.expenseForm.bankName = String(this.otherPaymentForm.bankName || '').trim();
        this.expenseForm.paymentAccountNo = String(this.otherPaymentForm.paymentAccountNo || '').trim();
      }
    },
    validateExpenseLinkedFields() {
      const form = this.expenseForm;
      this.applyNewExpenseAccountValues();
      if (!form.payerType || !form.accountProfileChoice) return '请选择付款方类型和所属账户。';
      if (!form.feeAccountChoice) return '请选择费用账户。';
      if (form.feeAccountChoice === '__new__' && (!form.feeAccountKey || !form.feeAccountNo.trim())) return '请填写新费用账户的类型和号码。';
      if (!form.feeAccountKey || !form.feeAccountNo.trim()) return '请选择有效的费用账户。';
      if (form.accountProfileChoice === '__new__' && form.payerType === 'owner') {
        if (!this.ownerBankForm.itemName.trim() || !this.ownerBankForm.paymentName.trim() || !this.ownerBankForm.accountNo.trim()) return '请完整填写业主账户的银行名称、收款人姓名和银行账号。';
        if (Number(this.ownerBankForm.transferLimit || 0) < 0 || Number(this.ownerBankForm.overseasTransferFee || 0) < 0) return '转账额度和海外汇款手续费不能小于 0。';
      }
      if (form.accountProfileChoice === '__new__' && form.payerType === 'management' && (!this.managementBankForm.managementName.trim() || !this.managementBankForm.purpose.trim() || !this.managementBankForm.bankName.trim() || !this.managementBankForm.accountNo.trim() || !this.managementBankForm.accountName.trim())) return '请完整填写管理层名称、费用用途、银行名称、银行账号和收款户名。';
      if (!form.payerName.trim()) return '请填写付款方名称。';
      if (!form.bankName.trim()) return '请填写银行名称。';
      if (!form.paymentAccountNo.trim()) return '请填写支付账户号码。';
      return '';
    },
    expenseRequestPayload() {
      const form = this.expenseForm;
      return { unitId: form.unitId, category: form.category, description: form.description, amount: form.amount, occurredOn: form.occurredOn, settlementMethod: form.settlementMethod, payerName: form.payerName, bankName: form.bankName, paymentAccountNo: form.paymentAccountNo, feeAccountKey: form.feeAccountKey, feeAccountNo: form.feeAccountNo };
    },
    async syncExpenseMasterData() {
      const form = this.expenseForm;
      const unit = this.selectedExpenseUnit;
      if (!unit?.ownerId || !unit?.ownerUnitId) throw new Error('此单位缺少房产关联资料，无法回写付款账户。');
      const [latestProfile, latestOwnerAccounts] = await Promise.all([
        fetchAdminPropertyBasicProfile(unit.ownerId, unit.ownerUnitId),
        fetchAdminPropertyBankAccounts(unit.ownerId, unit.ownerUnitId)
      ]);
      const profile = latestProfile || {};
      let profileChanged = false;
      if (form.feeAccountChoice === '__new__') {
        profile.paymentAccountNumbers = { ...(profile.paymentAccountNumbers || {}), [form.feeAccountKey]: form.feeAccountNo.trim() };
        profileChanged = true;
      }
      if (form.payerType === 'management' && this.expenseCreatesMasterAccount) {
        const accounts = Array.isArray(profile.managementBankAccounts) ? [...profile.managementBankAccounts] : [];
        const newAccount = { ...this.managementBankForm, id: managementAccountId(), managementName: this.managementBankForm.managementName.trim(), purpose: this.managementBankForm.purpose.trim(), bankName: this.managementBankForm.bankName.trim(), accountNo: this.managementBankForm.accountNo.trim(), accountName: this.managementBankForm.accountName.trim(), branchOrSwift: this.managementBankForm.branchOrSwift.trim(), remarks: this.managementBankForm.remarks.trim() || '由新增支出回写' };
        const duplicate = accounts.some(account => String(account.managementName || '').trim() === newAccount.managementName && String(account.bankName || '').trim() === newAccount.bankName && String(account.accountNo || '').trim() === newAccount.accountNo);
        if (!duplicate) {
          accounts.push(newAccount);
          profile.managementBankAccounts = accounts; profileChanged = true;
        }
      }
      if (profileChanged) {
        this.expenseMasterProfile = await saveAdminPropertyBasicProfile(unit.ownerId, unit.ownerUnitId, profile) || profile;
      }
      if (form.payerType === 'owner' && this.expenseCreatesMasterAccount) {
        const accounts = Array.isArray(latestOwnerAccounts) ? latestOwnerAccounts : [];
        const duplicate = accounts.some(account => String(account.paymentName || '').trim() === form.payerName.trim() && String(account.itemName || '').trim() === form.bankName.trim() && String(account.accountNo || '').trim() === form.paymentAccountNo.trim());
        if (!duplicate) {
          const transferLimit = Number(this.ownerBankForm.transferLimit || 0), overseasTransferFee = Number(this.ownerBankForm.overseasTransferFee || 0);
          const created = await createAdminPropertyBankAccount(unit.ownerId, unit.ownerUnitId, { ...this.ownerBankForm, itemName: this.ownerBankForm.itemName.trim(), paymentName: this.ownerBankForm.paymentName.trim(), accountNo: this.ownerBankForm.accountNo.trim(), bankAddress: this.ownerBankForm.bankAddress.trim() || null, branchCode: this.ownerBankForm.branchCode.trim() || null, swiftCode: this.ownerBankForm.swiftCode.trim() || null, transferLimit: transferLimit > 0 ? transferLimit.toFixed(2) : null, overseasBank: Boolean(this.ownerBankForm.overseasBank), overseasTransferFee: this.ownerBankForm.overseasBank ? overseasTransferFee.toFixed(2) : '0.00', remarks: this.ownerBankForm.remarks.trim() || '由新增支出回写' });
          this.expenseOwnerBankAccounts = [...accounts, created];
        }
      }
    },
    async syncMaintenanceMasterData() {
      const form = this.maintenanceCreateForm;
      const unit = this.selectedMaintenanceUnit;
      if (!unit?.ownerId || !unit?.ownerUnitId) throw new Error('此单位缺少房产关联资料，无法回写付款账户。');
      const [latestProfile, latestOwnerAccounts] = await Promise.all([
        fetchAdminPropertyBasicProfile(unit.ownerId, unit.ownerUnitId),
        fetchAdminPropertyBankAccounts(unit.ownerId, unit.ownerUnitId)
      ]);
      const profile = latestProfile || {};
      let profileChanged = false;
      if (form.feeAccountChoice === '__new__') {
        profile.paymentAccountNumbers = { ...(profile.paymentAccountNumbers || {}), [form.feeAccountKey]: form.feeAccountNo.trim() };
        profileChanged = true;
      }
      if (form.payerType === 'management' && this.maintenanceCreatesMasterAccount) {
        const accounts = Array.isArray(profile.managementBankAccounts) ? [...profile.managementBankAccounts] : [];
        const newAccount = { ...this.managementBankForm, id: managementAccountId(), managementName: this.managementBankForm.managementName.trim(), purpose: this.managementBankForm.purpose.trim(), bankName: this.managementBankForm.bankName.trim(), accountNo: this.managementBankForm.accountNo.trim(), accountName: this.managementBankForm.accountName.trim(), branchOrSwift: this.managementBankForm.branchOrSwift.trim(), remarks: this.managementBankForm.remarks.trim() || '由新增维修回写' };
        const duplicate = accounts.some(account => String(account.managementName || '').trim() === newAccount.managementName && String(account.bankName || '').trim() === newAccount.bankName && String(account.accountNo || '').trim() === newAccount.accountNo);
        if (!duplicate) {
          accounts.push(newAccount);
          profile.managementBankAccounts = accounts; profileChanged = true;
        }
      }
      if (profileChanged) this.expenseMasterProfile = await saveAdminPropertyBasicProfile(unit.ownerId, unit.ownerUnitId, profile) || profile;
      if (form.payerType === 'owner' && this.maintenanceCreatesMasterAccount) {
        const accounts = Array.isArray(latestOwnerAccounts) ? latestOwnerAccounts : [];
        const duplicate = accounts.some(account => String(account.paymentName || '').trim() === form.payerName.trim() && String(account.itemName || '').trim() === form.bankName.trim() && String(account.accountNo || '').trim() === form.paymentAccountNo.trim());
        if (!duplicate) {
          const transferLimit = Number(this.ownerBankForm.transferLimit || 0), overseasTransferFee = Number(this.ownerBankForm.overseasTransferFee || 0);
          const created = await createAdminPropertyBankAccount(unit.ownerId, unit.ownerUnitId, { ...this.ownerBankForm, itemName: this.ownerBankForm.itemName.trim(), paymentName: this.ownerBankForm.paymentName.trim(), accountNo: this.ownerBankForm.accountNo.trim(), bankAddress: this.ownerBankForm.bankAddress.trim() || null, branchCode: this.ownerBankForm.branchCode.trim() || null, swiftCode: this.ownerBankForm.swiftCode.trim() || null, transferLimit: transferLimit > 0 ? transferLimit.toFixed(2) : null, overseasBank: Boolean(this.ownerBankForm.overseasBank), overseasTransferFee: this.ownerBankForm.overseasBank ? overseasTransferFee.toFixed(2) : '0.00', remarks: this.ownerBankForm.remarks.trim() || '由新增维修回写' });
          this.expenseOwnerBankAccounts = [...accounts, created];
        }
      }
    },
    async submitExpense() {
      if (this.expenseForm.settlementMethod === 'reserve' && !this.expenseReserveAvailable) { this.expenseCreateError = '此單位沒有可用的預備金帳戶。'; return; }
      if (this.expenseForm.settlementMethod === 'direct_payment' && !this.expenseDirectPaymentAllowed) { this.expenseCreateError = '业主已解约，不能再新增代付款。'; return; }
      const linkedFieldError = this.validateExpenseLinkedFields();
      if (linkedFieldError) { this.expenseCreateError = linkedFieldError; return; }
      this.expenseSaving = true; this.expenseCreateError = '';
      try { const editing = this.editingExpenseId; await this.syncExpenseMasterData(); const payload = this.expenseRequestPayload(); const result = editing ? await updateAdminExpense(editing, payload) : await createAdminExpense(payload); this.closeExpenseCreate(); this.activeTab = 'expense'; await this.loadData(); this.page.showToast?.(editing ? '支出记录已修改，关联付款资料已同步' : `支出 ${result.referenceNo} 已建立，关联付款资料已同步`); }
      catch (error) { this.expenseCreateError = error.message || (this.editingExpenseId ? '修改支出失败' : '新增支出失败'); }
      finally { this.expenseSaving = false; }
    },
    async removeExpense(row) {
      if (!window.confirm(`确定删除支出记录“${row.description}”吗？删除后会保留审计记录。`)) return;
      try { this.lastDeletedRecycleBinId = await deleteAdminExpense(row.id); this.closeMaintenanceDetail(); await this.loadData(); this.page.showToast?.('支出记录已删除，可点击“撤销删除”恢复'); }
      catch (error) { this.page.showToast?.(error.message || '删除支出失败'); }
    },
    async openMaintenanceCreate() {
      this.editingMaintenanceId = null; this.maintenanceCreateForm = this.emptyMaintenanceForm(); this.resetNewExpenseAccountForms(); this.resetExpenseMasterData(); this.maintenanceCreateError = ''; this.$refs.maintenanceCreateDialog?.showModal();
      try { await this.loadOptions(); } catch (error) { this.maintenanceCreateError = error.message; }
    },
    async openMaintenanceEdit(row) {
      this.editingMaintenanceId = row.id;
      this.resetNewExpenseAccountForms(); this.resetExpenseMasterData();
      this.maintenanceCreateForm = { ...this.emptyMaintenanceForm(), unitId: row.unitId, vendorId: row.vendorId || null, category: row.category, title: row.title, description: row.description || '', requestedAt: String(row.requestedAt || '').slice(0, 16), estimatedAmount: row.estimatedAmount == null ? null : Number(row.estimatedAmount), payerName: row.payerName || '', bankName: row.bankName || '', paymentAccountNo: row.paymentAccountNo || '', feeAccountKey: row.feeAccountKey || '', feeAccountNo: row.feeAccountNo || '', status: row.status || 'submitted' };
      this.maintenanceCreateError = ''; this.$refs.maintenanceCreateDialog?.showModal();
      try { await this.loadOptions(); await this.loadMaintenanceMasterData(row.unitId); } catch (error) { this.maintenanceCreateError = error.message; }
    },
    closeMaintenanceCreate() { this.$refs.maintenanceCreateDialog?.close(); this.maintenanceCreateError = ''; this.editingMaintenanceId = null; },
    async submitMaintenanceCreate() {
      const linkedFieldError = this.validateMaintenanceLinkedFields();
      if (linkedFieldError) { this.maintenanceCreateError = linkedFieldError; return; }
      this.maintenanceCreateSaving = true; this.maintenanceCreateError = '';
      try {
        const editing = this.editingMaintenanceId;
        await this.syncMaintenanceMasterData();
        const payload = { vendorId: this.maintenanceCreateForm.vendorId, category: this.maintenanceCreateForm.category, title: this.maintenanceCreateForm.title, description: this.maintenanceCreateForm.description, requestedAt: this.maintenanceCreateForm.requestedAt, estimatedAmount: this.maintenanceCreateForm.estimatedAmount, payerName: this.maintenanceCreateForm.payerName, bankName: this.maintenanceCreateForm.bankName, paymentAccountNo: this.maintenanceCreateForm.paymentAccountNo, feeAccountKey: this.maintenanceCreateForm.feeAccountKey, feeAccountNo: this.maintenanceCreateForm.feeAccountNo, ...(editing ? { status: this.maintenanceCreateForm.status } : { unitId: this.maintenanceCreateForm.unitId }) };
        const result = editing ? await updateAdminMaintenance(editing, payload) : await createAdminMaintenance(payload);
        this.closeMaintenanceCreate(); this.activeTab = 'maintenance'; await this.loadData(); this.page.showToast?.(editing ? '维修工单已修改，关联付款资料已同步' : `维修工单 ${result.referenceNo} 已建立，关联付款资料已同步`);
      }
      catch (error) { this.maintenanceCreateError = error.message || (this.editingMaintenanceId ? '修改维修工单失败' : '新增维修工单失败'); }
      finally { this.maintenanceCreateSaving = false; }
    },
    async removeMaintenance(row) {
      if (!window.confirm(`确定删除维修工单“${row.title}”吗？删除后会保留审计记录。`)) return;
      try { this.lastDeletedRecycleBinId = await deleteAdminMaintenance(row.id); this.closeMaintenanceDetail(); await this.loadData(); this.page.showToast?.('维修工单已删除，可点击“撤销删除”恢复'); }
      catch (error) { this.page.showToast?.(error.message || '删除维修工单失败'); }
    },
    async openRecycleBin() { if (!this.$refs.recycleBinDialog?.open) this.$refs.recycleBinDialog?.showModal(); await this.loadRecycleBin(); },
    closeRecycleBin() { this.$refs.recycleBinDialog?.close(); },
    async loadRecycleBin() {
      this.recycleBinLoading = true; this.recycleBinError = '';
      try { const result = await fetchAdminRecycleBin(); this.recycleBin = result?.items || []; }
      catch (error) { this.recycleBinError = error.message || '回收站读取失败'; }
      finally { this.recycleBinLoading = false; }
    },
    async undoLastDelete() {
      if (!this.lastDeletedRecycleBinId) return;
      try { await restoreAdminRecycleBin(this.lastDeletedRecycleBinId); this.lastDeletedRecycleBinId = null; await this.loadData(); this.page.showToast?.('记录已撤销删除并恢复'); }
      catch (error) { this.page.showToast?.(error.message || '撤销删除失败'); }
    },
    async restoreRecycleItem(item) {
      if (!window.confirm(`确定恢复“${item.title || item.referenceNo}”吗？`)) return;
      try { await restoreAdminRecycleBin(item.id); if (this.lastDeletedRecycleBinId === item.id) this.lastDeletedRecycleBinId = null; await this.loadRecycleBin(); await this.loadData(); this.page.showToast?.('记录已恢复'); }
      catch (error) { this.page.showToast?.(error.message || '恢复记录失败'); }
    },
    async purgeRecycleItem(item) {
      if (!window.confirm(`确定从回收站永久移除“${item.title || item.referenceNo}”吗？原记录将继续保留审计状态。`)) return;
      try { await purgeAdminRecycleBin(item.id); if (this.lastDeletedRecycleBinId === item.id) this.lastDeletedRecycleBinId = null; await this.loadRecycleBin(); this.page.showToast?.('回收站记录已移除'); }
      catch (error) { this.page.showToast?.(error.message || '移除回收站记录失败'); }
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
        this.loadExportPaymentProfiles(serial);
      } catch (error) {
        if (serial !== this.requestSerial) return;
        this.errorMessage = error.message || 'API request failed'; this.page.adminMaintenanceMetrics = null;
      } finally { if (serial === this.requestSerial) this.loading = false; }
    },
    async loadExportPaymentProfiles(serial) {
      try {
        if (!Array.isArray(this.options.units) || !this.options.units.length) this.options = await fetchAdminMaintenanceOptions();
        const unitIds = [...new Set([...(this.response.expenses || []), ...(this.response.maintenance || [])].map(row => Number(row.unitId)).filter(Boolean))];
        const entries = await Promise.all(unitIds.map(async unitId => {
          const unit = this.options.units.find(item => Number(item.unitId) === unitId);
          if (!unit?.ownerId || !unit?.ownerUnitId) return [String(unitId), { ownerName: unit?.ownerName || '', tenantName: unit?.tenantName || '', profile: {}, ownerAccounts: [] }];
          try {
            const [profile, ownerAccounts] = await Promise.all([
              fetchAdminPropertyBasicProfile(unit.ownerId, unit.ownerUnitId),
              fetchAdminPropertyBankAccounts(unit.ownerId, unit.ownerUnitId)
            ]);
            return [String(unitId), { ownerName: unit.ownerName || '', tenantName: unit.tenantName || '', profile: profile || {}, ownerAccounts: Array.isArray(ownerAccounts) ? ownerAccounts : [] }];
          } catch {
            return [String(unitId), { ownerName: unit.ownerName || '', tenantName: unit.tenantName || '', profile: {}, ownerAccounts: [] }];
          }
        }));
        if (serial !== this.requestSerial) return;
        this.exportPaymentProfiles = Object.fromEntries(entries);
        this.syncExportRows();
      } catch {
        if (serial === this.requestSerial) { this.exportPaymentProfiles = {}; this.syncExportRows(); }
      }
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
    selectTab(tab) { this.activeTab = tab; this.listPage = 1; this.page.statusFilter = '全部狀態'; this.maintenanceDetail = null; this.selectedKey = ''; this.maintenanceDetailOpen = false; this.ensureSelection(); },
    ensureSelection() { this.$nextTick(() => { const row = this.pagedRows()[0]; this.selectedKey = row ? this.rowKey(row) : ''; if (row) this.loadDetail(row); else this.maintenanceDetail = null; }); },
    selectRow(row) { this.selectedKey = this.rowKey(row); this.maintenanceDetailOpen = true; this.loadDetail(row); },
    closeMaintenanceDetail() { this.maintenanceDetailOpen = false; },
    rowKey(row) { return `${this.activeTab}-${row.id}`; },
    async loadDetail(row) { const id = this.activeTab === 'maintenance' ? row.id : row.workOrderId; if (!id) { this.maintenanceDetail = null; return; } try { this.maintenanceDetail = await fetchAdminMaintenanceDetail(id); } catch { this.maintenanceDetail = null; } },
    async openHandling(row) {
      this.handlingRow = row; this.handlingLoading = true; this.handlingError = ''; this.handlingFiles = { before: [], after: [], invoice: [] };
      this.handlingForm = { actualAmount: Number(row.amount || 0), settlementMethod: 'reserve', completionNote: '', payerName: row.payerName || '', bankName: row.bankName || '', paymentAccountNo: row.paymentAccountNo || '', feeAccountKey: row.feeAccountKey || '', feeAccountNo: row.feeAccountNo || '' };
      this.$refs.handlingDialog.showModal();
      try {
        const [handlingInfo, detail] = await Promise.all([
          fetchAdminMaintenanceHandling(row.id),
          fetchAdminMaintenanceDetail(row.id)
        ]);
        this.handlingInfo = handlingInfo || {};
        const previousCompletion = (detail?.history || []).filter(item => item.status === 'completed').at(-1);
        this.handlingForm.actualAmount = Number(detail?.actualAmount ?? row.amount ?? 0);
        const previousMethod = detail?.paymentMethod;
        this.handlingForm.settlementMethod = previousMethod === 'direct_payment'
          ? 'direct_payment'
          : this.handlingInfo.reserveAccountAvailable ? 'reserve' : 'direct_payment';
        this.handlingForm.completionNote = previousCompletion?.note || detail?.description || '';
        this.handlingForm.payerName = detail?.payerName || row.payerName || '';
        this.handlingForm.bankName = detail?.bankName || row.bankName || '';
        this.handlingForm.paymentAccountNo = detail?.paymentAccountNo || row.paymentAccountNo || '';
        if (!this.handlingInfo.reserveAccountAvailable) this.handlingForm.settlementMethod = 'direct_payment';
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
      if (this.handlingForm.settlementMethod === 'direct_payment' && !this.handlingDirectPaymentAllowed) { this.handlingError = '业主已解约，不能再提交代付款。'; return; }
      const hasBefore = Number(this.handlingInfo.beforePhotoCount || 0) > 0 || this.handlingFiles.before.length > 0;
      const hasAfter = Number(this.handlingInfo.afterPhotoCount || 0) > 0 || this.handlingFiles.after.length > 0;
      if (this.requiredHandlingPhotos && (!hasBefore || !hasAfter)) { this.handlingError = '维修金额超过 RM 500，维修前与维修后照片各至少需要 1 份。'; return; }
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
    categoryLabel(value) {
      const direct = { service_fee: '服务费', insurance: '保险', tax: '税费', deposit: '租客押金', deposit_refund: '租客押金退款', deposit_forfeiture: '租客押金没收' }[value];
      if (direct) return direct;
      return this.$t({ utilities: 'legacy.t_39e538e57590', management_service_fee: 'legacy.t_5f6a3d7c1e90', land_tax: 'legacy.t_2e8c9b4a6d71', assessment_tax: 'legacy.t_8b4d1f6a3c20', fire_insurance: 'legacy.t_4c7e2a9d5b13', agency_commission: 'legacy.t_6d1b8f3e4a72', management: 'legacy.t_a178daac2527', cleaning: 'legacy.t_00e326047628', maintenance: 'legacy.t_018bde2b7e24', plumbing: 'legacy.t_925558624144', air_conditioning: 'legacy.t_e7ec651a7c0f', electrical: 'legacy.t_c230e3bc3ab6', painting: 'legacy.t_6a81927031d5', other: 'legacy.t_c90e8ecbb54d' }[value] || 'legacy.t_c90e8ecbb54d');
    },
    maintenanceStatusLabel(value) { return this.$t({ submitted: 'legacy.t_3b8dcefe78c2', assigned: 'legacy.t_89f43720c2e8', in_progress: 'legacy.t_1e038f9b55ec', inspection: 'legacy.t_7421844828c2', completed: 'legacy.t_e99b48a29bdf', cancelled: 'legacy.t_a5ffdc95eeb0' }[value] || 'legacy.t_3b8dcefe78c2'); },
    maintenanceRowEditable(row) { return Boolean(row) && row.editable !== false; },
    paymentLabel(value) { return this.$t({ paid: 'legacy.t_b35b40fe4f61', partial: 'legacy.t_a66b74573539', unpaid: 'legacy.t_20825179461a', pending: 'legacy.t_20825179461a' }[value] || 'legacy.t_20825179461a'); },
    confirmationLabel(value) { return { confirmed: '已确认', rejected: '已退回', pending: '待确认' }[value] || '待确认'; },
    paymentMethodLabel(value) { const key = { bank_transfer: 'legacy.t_789957b63e04', online_payment: 'legacy.t_61179c3c479b', reserve_account: 'legacy.t_c1b0ddc685b1', direct_payment: 'legacy.t_164917d2ce3b', cash: 'legacy.t_e3ca5905c270' }[value]; return key ? this.$t(key) : '—'; },
    vendorOptionLabel(vendor) { return vendor.contactName ? `${vendor.name} · ${vendor.contactName}` : vendor.name; },
    statusClass(value) { if (['已付款', 'Paid'].includes(value)) return 'gray'; if (['待付款', 'Unpaid'].includes(value)) return 'red'; if (['已完成', 'Completed'].includes(value)) return 'green'; if (['已取消', 'Canceled', '已取消'].includes(value)) return 'red'; if (['處理中', '部分付款', '已指派', '待驗收', 'Processing…', 'Partially Paid', 'Assigned', 'Pending Inspection', '处理中…', '部分付款', '已指派', '待验收'].includes(value)) return 'orange'; return 'gray'; },
    money(value) { return Number(value || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); },
    displayDate(value) { return formatDate(value); },
    dateTime(value) { return formatDateTime(value); }
  }
};
</script>

<style scoped>
.admin-maintenance-workspace { align-items: stretch; }
.maintenance-list-panel { min-height: 590px; }
.maintenance-tabs { display: flex; padding: 3px; border: 1px solid #dce4ee; border-radius: 8px; background: #f1f5f9; }
.maintenance-head-actions { display: flex; align-items: center; gap: 8px; }
.maintenance-undo-btn,.maintenance-recycle-btn { height: 34px; padding: 0 13px; border: 1px solid #b9cde0; border-radius: 7px; background: #fff; color: #175381; font-size: 12px; font-weight: 800; cursor: pointer; }
.maintenance-district-filter { height: 34px; min-width: 112px; padding: 0 9px; border: 1px solid #b9cde0; border-radius: 7px; background: #fff; color: #175381; font-size: 12px; font-weight: 700; }
.maintenance-undo-btn { border-color: #e0b66d; background: #fff9ed; color: #9b6500; }
.maintenance-recycle-btn:hover,.maintenance-undo-btn:hover { border-color: #0b8f96; background: #eefafa; color: #075e68; }
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
.maintenance-create-dialog { width: min(780px, calc(100vw - 32px)); }
.maintenance-create-body { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; padding: 18px 20px; }
.maintenance-create-body > label { display: grid; gap: 6px; color: #334155; font-size: 11px; font-weight: 800; }
.maintenance-create-body .wide { grid-column: 1 / -1; }
.maintenance-create-body input,.maintenance-create-body select,.maintenance-create-body textarea { width: 100%; border: 1px solid #dbe3ed; border-radius: 7px; background: #fff; color: #17233a; font: inherit; }
.maintenance-create-body input,.maintenance-create-body select { height: 38px; padding: 0 10px; }
.maintenance-create-body textarea { padding: 10px; resize: vertical; }
.expense-linked-fields { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; padding: 14px; border: 1px solid #cadde8; border-radius: 10px; background: #f7fbfc; }
.expense-linked-fields header { grid-column: 1 / -1; display: grid; gap: 3px; padding-bottom: 4px; }
.expense-linked-fields header strong { color: #173f58; font-size: 13px; }
.expense-linked-fields header small,.expense-master-note { margin: 0; color: #718696; font-size: 10px; font-weight: 600; line-height: 1.5; }
.expense-linked-fields > label { display: grid; gap: 6px; color: #334155; font-size: 11px; font-weight: 800; }
.expense-linked-fields input,.expense-linked-fields select,.expense-linked-fields textarea { box-sizing: border-box; width: 100%; padding: 0 10px; border: 1px solid #cfdee7; border-radius: 7px; background: #fff; color: #17233a; font: inherit; }
.expense-linked-fields input,.expense-linked-fields select { height: 38px; }.expense-linked-fields textarea { padding-block: 9px; resize: vertical; }
.expense-linked-fields label small { color: #718696; font-size: 9px; font-weight: 500; }
.expense-linked-fields .expense-overseas-toggle { display: flex; grid-template-columns: none; align-items: center; gap: 9px; min-height: 38px; padding: 8px 10px; border: 1px solid #d7e4ea; border-radius: 7px; background: #fff; }
.expense-linked-fields .expense-overseas-toggle input { width: 16px; height: 16px; padding: 0; }
.expense-linked-fields > p { grid-column: 1 / -1; }
.expense-account-summary { grid-column: 1 / -1; display: grid; grid-template-columns: repeat(3,minmax(0,1fr)); gap: 8px; padding: 10px 12px; border: 1px solid #dbe7ed; border-radius: 8px; background: #fff; }
.expense-account-summary span { display: grid; gap: 3px; min-width: 0; color: #738695; font-size: 10px; }
.expense-account-summary strong { overflow: hidden; color: #173f58; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.create-unit-summary { display: grid; grid-template-columns: repeat(3,1fr); gap: 10px; padding: 12px; border: 1px solid #dce5ef; border-radius: 9px; background: #f7f9fc; color: #6b778b; font-size: 10px; }
.create-unit-summary span { padding-right: 10px; border-right: 1px solid #e1e7ef; }.create-unit-summary span:last-child { border-right: 0; }.create-unit-summary b { color: #102447; }.create-unit-summary .shortage,.create-unit-summary .shortage b { color: #dc3f3f; }
.maintenance-handling-dialog { width: min(720px, calc(100vw - 32px)); }
.maintenance-handling-body { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; padding: 18px 20px; }
.maintenance-handling-body > label { display: grid; gap: 6px; color: #334155; font-size: 11px; font-weight: 800; }
.maintenance-handling-body input,.maintenance-handling-body select,.maintenance-handling-body textarea { width: 100%; border: 1px solid #dbe3ed; border-radius: 7px; background: #fff; color: #17233a; font: inherit; }
.maintenance-handling-body input,.maintenance-handling-body select { height: 36px; padding: 0 10px; }.maintenance-handling-body textarea { padding: 10px; resize: vertical; }
.handling-reserve-summary { grid-column: 1 / -1; display: grid; grid-template-columns: repeat(4,1fr); gap: 8px; padding: 12px; border: 1px solid #dce5ef; border-radius: 9px; background: #f7f9fc; }
.handling-reserve-summary div { display: grid; gap: 5px; padding-right: 8px; border-right: 1px solid #e1e7ef; }.handling-reserve-summary div:last-child { border-right: 0; }.handling-reserve-summary span { color: #748196; font-size: 9px; }.handling-reserve-summary strong { color: #102447; font-size: 13px; }.handling-reserve-summary .shortage strong,.handling-warning { color: #dc3f3f; }
.handling-photo-grid { grid-column: 1 / -1; display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }.handling-photo-field { display: grid; gap: 7px; padding: 12px; border: 1px dashed #bdc9d8; border-radius: 8px; background: #fbfcfe; }.handling-photo-field span { font-size: 11px; font-weight: 900; }.handling-photo-field b { color: #dc3f3f; }.handling-photo-field em { color: #58738a; font-style: normal; font-weight: 700; }.handling-photo-field small { color: #7b8798; font-size: 9px; }.handling-photo-field input { height: auto; padding: 7px; font-size: 10px; }.handling-photo-rule { grid-column: 1 / -1; margin: 0; color: #64748b; font-size: 10px; line-height: 1.5; }
.handling-note { grid-column: 1 / -1; }.handling-warning { font-size: 9px; font-weight: 700; }
.maintenance-finance-rejected-note { margin: 0; padding: 10px 20px; border-bottom: 1px solid #f3d3ce; background: #fff8f6; color: #b4473f; font-size: 12px; line-height: 1.5; }
.admin-maintenance-workspace { grid-template-columns: minmax(0,1fr) !important; align-items: start; }
.maintenance-list-panel { min-height: 0; }
.maintenance-list-panel .panel-head { min-height: 70px; height: auto; padding: 14px 18px; }
.maintenance-list-panel .panel-head h2 { font-size: 19px; }
.maintenance-list-panel .panel-head span { font-size: 14px; }
.maintenance-tabs button { height: 34px; padding-inline: 16px; font-size: 13px; }
.maintenance-table-wrap { min-height: 0 !important; background: #fff; }
.maintenance-table-wrap table { font-size: 14px; }
.maintenance-table-wrap th { height: 44px; padding: 0 11px; font-size: 13px; color: #36546a; background: #f6fafb; }
.maintenance-table-wrap td { height: 54px; padding: 7px 11px; font-size: 13px; line-height: 1.4; }
.maintenance-table-wrap td strong { font-size: 14px; }
.maintenance-table-wrap td small { font-size: 12px; }
.maintenance-description { max-width: 260px; }
.maintenance-category { padding: 5px 9px; font-size: 12px; }
.maintenance-table-wrap .tag { font-size: 13px; }
.maintenance-table-wrap .tag.gray { color: #66727d; background: #f1f3f5; border: 1px solid #d7dde2; }
.maintenance-table-wrap .tag.red { color: #c62828; background: #fff0f0; border: 1px solid #f1b8b8; }
.maintenance-table-wrap .tag.green { color: #16834b; background: #edf9f1; border: 1px solid #b9e5c8; }
.maintenance-table-wrap .tag.orange { color: #a86600; background: #fff7e5; border: 1px solid #f0d18e; }
.maintenance-table-wrap th:last-child,.maintenance-table-wrap td:last-child { width: 330px; min-width: 330px; text-align: right; }
.maintenance-row-actions { padding: 8px 12px !important; white-space: normal; vertical-align: middle; }
.maintenance-action-group { display: flex; align-items: center; justify-content: flex-end; flex-wrap: wrap; gap: 6px; }
.maintenance-row-actions button { flex: 0 0 auto; height: 32px; padding: 0 11px; border-radius: 8px; font-size: 12px; font-weight: 800; line-height: 1; cursor: pointer; transition: background-color .16s ease, border-color .16s ease, color .16s ease, transform .16s ease; }
.maintenance-row-actions button:hover { transform: translateY(-1px); }
.maintenance-row-actions button:active { transform: translateY(0); }
.maintenance-row-actions button:focus-visible { outline: 3px solid rgba(13, 148, 136, .24); outline-offset: 2px; }
.maintenance-detail-btn { border: 1px solid #9bcdd1; background: #f2fbfb; color: #076976; }
.maintenance-detail-btn:hover { border-color: #0b8f96; background: #e2f5f5; }
.maintenance-resubmit-btn { border: 1px solid #e5b34c; background: #fff8e8; color: #8a5a00; }
.maintenance-resubmit-btn:hover { border-color: #c98c13; background: #fff1ce; }
.maintenance-edit-btn { border: 1px solid #b9cde0; background: #f8fbfe; color: #175381; }
.maintenance-edit-btn:hover { border-color: #7fa7c5; background: #eef6fb; }
.maintenance-delete-btn { border: 1px solid #efc1c1; background: #fff8f8; color: #c33a3a; }
.maintenance-delete-btn:hover { border-color: #df8e8e; background: #fff0f0; }
.maintenance-handle-btn { border: 1px solid #0b3768; background: #0b3768; color: #fff; padding-inline: 13px !important; }
.maintenance-handle-btn:hover { border-color: #092c55; background: #092c55; }
.maintenance-state-pill { display: inline-flex; align-items: center; justify-content: center; margin-top: 6px; padding: 4px 9px; border-radius: 999px; font-size: 11px; font-weight: 800; line-height: 1; }
.maintenance-state-pill.is-confirmed { border: 1px solid #d8e0e7; background: #f1f4f7; color: #697787; }
.maintenance-state-pill.is-rejected { border: 1px solid #f2c4c0; background: #fff1ef; color: #c7473f; }
.maintenance-list-panel .pager { height: 62px; padding-inline: 18px; font-size: 14px; }
.maintenance-list-panel .pager select { width: 120px; min-width: 120px; height: 38px; padding: 0 10px; font-size: 14px; }
.recycle-bin-dialog { width: min(760px, calc(100vw - 32px)); }
.recycle-bin-body { min-height: 180px; max-height: min(520px, calc(100vh - 220px)); overflow: auto; padding: 18px 20px; }
.recycle-bin-list { display: grid; gap: 9px; margin: 0; padding: 0; list-style: none; }
.recycle-bin-list li { display: flex; align-items: center; justify-content: space-between; gap: 14px; padding: 12px; border: 1px solid #dce5ed; border-radius: 9px; background: #fbfdff; }
.recycle-bin-list li>div { display: grid; min-width: 0; gap: 4px; }
.recycle-bin-list strong { overflow: hidden; color: #102447; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.recycle-bin-list small { color: #718096; font-size: 11px; }
.recycle-bin-list li>span { display: flex; flex-shrink: 0; gap: 6px; }
.recycle-restore-btn,.recycle-purge-btn { height: 32px; border-radius: 7px; padding: 0 10px; font-size: 11px; font-weight: 800; cursor: pointer; }
.recycle-restore-btn { border: 1px solid #80cbd0; background: #effafa; color: #08717a; }
.recycle-purge-btn { border: 1px solid #e2b7b7; background: #fff; color: #b43a3a; }
@media (max-width: 820px) { .maintenance-head-actions { flex-wrap: wrap; justify-content: flex-end; }.maintenance-table-wrap th:last-child,.maintenance-table-wrap td:last-child { width: auto; min-width: 220px; }.maintenance-action-group { justify-content: flex-start; }.maintenance-row-actions { text-align: left !important; }.recycle-bin-list li { align-items: flex-start; flex-direction: column; }.recycle-bin-list li>span { width: 100%; }.recycle-bin-list li>span button { flex: 1; } }
.maintenance-detail-modal { position: fixed; inset: 0; z-index: 70; display: grid; place-items: center; padding: 24px; background: rgba(6,35,48,.44); backdrop-filter: blur(3px); }
.maintenance-detail-modal .maintenance-detail-panel { width: min(640px,calc(100vw - 48px)); max-height: calc(100vh - 40px); min-height: 0; overflow: auto; padding: 22px 24px 20px; border: 1px solid #d7e2eb; border-radius: 14px; box-shadow: 0 24px 70px rgba(4,28,40,.28); background: #fff; }
.maintenance-detail-modal-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 18px; padding-bottom: 14px; border-bottom: 1px solid #e5edf2; color: #0d1b3e; font-size: 17px; }
.maintenance-detail-close { width: 34px; height: 34px; border: 1px solid #d7e2eb; border-radius: 8px; background: #fff; color: #526679; font-size: 22px; line-height: 1; cursor: pointer; }
.maintenance-detail-close:hover { background: #eef7f8; border-color: #0b8f96; color: #075e68; }
.maintenance-detail-modal .detail-card { padding: 0; }
.maintenance-detail-modal .profile { gap: 16px; padding-bottom: 18px; }
.maintenance-detail-modal .profile h3 { font-size: 21px; }
.maintenance-detail-modal .profile p { font-size: 14px; }
.maintenance-detail-modal .detail-section { padding: 18px 0 17px; }
.maintenance-detail-modal .detail-section h4 { margin-bottom: 14px; font-size: 16px; }
.maintenance-detail-modal .detail-section .kv { grid-template-columns: 120px minmax(0,1fr); margin: 11px 0; font-size: 14px; line-height: 1.5; }
.maintenance-detail-modal .detail-section .kv b { max-width: none; font-size: 14px; }
.maintenance-detail-modal .maintenance-detail-copy { margin-bottom: 14px; font-size: 14px; line-height: 1.7; }
@media (max-width: 820px) { .maintenance-tabs button { padding-inline: 9px; }.maintenance-create-body,.expense-linked-fields { grid-template-columns: 1fr; }.maintenance-create-body .wide { grid-column: auto; }.expense-linked-fields header,.expense-linked-fields > p,.expense-account-summary { grid-column: auto; }.expense-account-summary { grid-template-columns: 1fr; }.create-unit-summary { grid-template-columns: 1fr; }.create-unit-summary span { border-right: 0; } }
@media (max-width: 820px) { .maintenance-detail-modal { padding: 14px; }.maintenance-detail-modal .maintenance-detail-panel { width: min(100%,calc(100vw - 28px)); padding: 18px 16px; }.maintenance-table-wrap { overflow-x: auto; }.maintenance-table-wrap table { min-width: 980px; } }
</style>
