<template>
  <section class="owners-grid admin-owner-workspace" :class="{ 'property-list-workspace': isPropertyMode }">
    <template v-if="isPropertyMode">
      <div class="panel owners-table-panel">
        <div v-if="errorMessage" class="admin-owner-state error" role="alert"><strong>{{ $t('properties.dataLoadFailed') }}</strong><span>{{ $lt(errorMessage) }}</span><button type="button" @click="loadOwners">{{ $t('ui.reload') }}</button></div>
        <div v-else class="table-wrap owners-table-wrap">
          <table>
            <thead><tr><th>{{ $t('properties.projectItem') }}</th><th>{{ $t('properties.unitNo') }}</th><th>{{ $t('ui.owner') }}</th><th>{{ $t('properties.unitType') }}</th><th>{{ $t('properties.handoverStatus') }}</th><th>{{ $t('properties.rentalStatus') }}</th><th>{{ $t('properties.propertyTotal') }}</th><th>{{ $t('properties.paidTotal') }}</th><th>{{ $t('properties.unpaidTotal') }}</th><th>{{ $t('ui.actions') }}</th></tr></thead>
            <tbody>
              <tr v-for="property in filteredProperties" :key="property.rowKey" :class="{ selected: property.rowKey === selectedPropertyKey }" @click="selectProperty(property)">
                <td><strong>{{ property.projectName || $t('properties.unsetProject') }}</strong><small>{{ property.city || '—' }}</small></td>
                <td><strong>{{ property.unitNo || '—' }}</strong><small>{{ [property.building, property.floorNo].filter(Boolean).join(' · ') || '—' }}</small></td>
                <td><span class="avatar">{{ initials(property.ownerName) }}</span>{{ property.ownerName }}<small>{{ property.ownerPhone || $t('properties.unsetMobile') }}</small></td>
                <td>{{ property.unitType || '—' }}<small>{{ property.areaSqm ? `${property.areaSqm} m²` : '—' }}</small></td>
                <td><span class="tag" :class="lifecycleClass(property.assetStage)">{{ $lt(lifecycleLabel(property.assetStage)) }}</span></td>
                <td><span class="tag" :class="rentalStatusClass(property.rentalStatus)">{{ $lt(rentalStatusLabel(property.rentalStatus)) }}</span></td>
                <td>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(property.purchasePrice) }}</td><td>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(property.paidAmount) }}</td>
                <td :class="{ 'money-red': Number(property.remainingAmount || 0) > 0 }">{{ $t('legacy.t_5e7b60c626a4') }} {{ money(property.remainingAmount) }}</td>
                <td class="property-actions-cell"><div class="property-list-actions">
                  <button class="row-actions property-open-action" type="button" :title="$t('properties.enterManagement')" @click.stop="selectProperty(property)"><span>{{ $t('legacy.t_0596bf73ba05') }}</span></button>
                  <button class="row-actions property-delete-action" type="button" :disabled="propertyDeletingId !== null" :title="$t('properties.deleteProperty')" :aria-label="$t('properties.deleteProperty')" :aria-busy="propertyDeletingId === property.unitId" :data-state="propertyDeletingId === property.unitId ? 'loading' : 'default'" @click.stop="removeProperty(property)"><LoaderCircle v-if="propertyDeletingId === property.unitId" class="property-action-spinner" :size="15" aria-hidden="true" /><Trash2 v-else :size="15" aria-hidden="true" /></button>
                </div></td>
              </tr>
              <tr v-if="!loading && !filteredProperties.length"><td colspan="10" class="admin-owner-empty">{{ $t('properties.noMatchingProperties') }}</td></tr>
            </tbody>
          </table>
        </div>
        <div class="owners-pager"><span>{{ $t('ui.records', { count: propertyTotalRows }) }}</span><div class="admin-building-pager"><button :disabled="propertyPageNumber <= 1 || loading" @click="goPropertyPage(propertyPageNumber - 1)">&lt;</button><button v-for="n in propertyVisiblePages" :key="n" :class="{ active: n === propertyPageNumber }" :disabled="loading" @click="goPropertyPage(n)">{{ n }}</button><button :disabled="propertyPageNumber >= propertyTotalPages || loading" @click="goPropertyPage(propertyPageNumber + 1)">&gt;</button><select v-model.number="propertyPageSize" :disabled="loading"><option :value="5">{{ $t('building.recordsPerPage', { count: 5 }) }}</option><option :value="10">{{ $t('building.recordsPerPage', { count: 10 }) }}</option><option :value="20">{{ $t('building.recordsPerPage', { count: 20 }) }}</option><option :value="50">{{ $t('building.recordsPerPage', { count: 50 }) }}</option></select></div></div>
      </div>
      <aside v-if="!isPropertyMode" class="panel owners-detail admin-owner-detail">
        <template v-if="selectedProperty">
          <div class="owners-profile"><div class="big-avatar">{{ $t('legacy.t_510cf918d2af') }}</div><div><h3>{{ selectedProperty.projectName }} · {{ selectedProperty.unitNo }}</h3><p>{{ selectedProperty.ownerName }} {{ $t('legacy.t_c3ed62b96510') }}</p></div><span class="tag" :class="lifecycleClass(selectedProperty.assetStage)">{{ $lt(lifecycleLabel(selectedProperty.assetStage)) }}</span></div>
          <section class="owners-section"><h4><span>{{ $t('legacy.t_356a192b7913') }}</span>{{ $t('legacy.t_bbda2bc362dd') }}</h4><div class="kv"><span>{{ $t('legacy.t_cf545c9c1bf9') }}</span><b>{{ selectedProperty.projectName || '—' }}</b></div><div class="kv"><span>{{ $t('legacy.t_590157b8d4d7') }}</span><b>{{ selectedProperty.city || '—' }}</b></div><div class="kv"><span>{{ $t('legacy.t_7abe31b88b71') }}</span><b>{{ [selectedProperty.building, selectedProperty.floorNo].filter(Boolean).join('／') || '—' }}</b></div><div class="kv"><span>{{ $t('legacy.t_9387cc206f80') }}</span><b>{{ selectedProperty.unitType || '—' }} · {{ selectedProperty.areaSqm ? `${selectedProperty.areaSqm} m²` : '—' }}</b></div><div class="kv"><span>{{ $t('legacy.t_5a6b3e9d88af') }}</span><b>{{ $lt(listingStatusLabel(selectedProperty.listingStatus)) }}</b></div></section>
          <section class="owners-section"><h4><span>{{ $t('legacy.t_da4b9237bacc') }}</span>{{ $t('legacy.t_82688930fcc0') }}</h4><div class="kv"><span>{{ $t('legacy.t_1c4f579e884d') }}</span><b>{{ selectedProperty.ownerName }}</b></div><div class="kv"><span>{{ $t('legacy.t_c30e4f203e42') }}</span><b>{{ selectedProperty.ownerPhone || '—' }}</b></div><div class="kv"><span>{{ $t('legacy.t_13127f02a814') }}</span><b>{{ selectedProperty.ownershipPercent ?? 100 }}%</b></div><div class="kv"><span>{{ $t('legacy.t_c31bb6b8e6e4') }}</span><b>{{ selectedProperty.primary ? $t('legacy.t_30160a21b92a') : $t('legacy.t_8bf5c10ad937') }}</b></div></section>
          <section class="owners-section"><h4><span>{{ $t('legacy.t_77de68daecd8') }}</span>{{ $t('legacy.t_f9ba1ae07a6e') }}</h4><div class="kv"><span>{{ $t('legacy.t_bbc3a9494a03') }}</span><b>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(selectedProperty.purchasePrice) }}</b></div><div class="kv"><span>{{ $t('legacy.t_929fe9d67675') }}</span><b>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(selectedProperty.paidAmount) }}</b></div><div class="kv"><span>{{ $t('legacy.t_1e7b168f5733') }}</span><b class="money-red">{{ $t('legacy.t_5e7b60c626a4') }} {{ money(selectedProperty.remainingAmount) }}</b></div><div class="progress"><i :style="{ width: propertyProgress + '%' }"></i></div></section>
          <div class="owner-property-actions property-page-actions"><button type="button" @click="openDetails(selectedProperty)">{{ $t('legacy.t_0596bf73ba05') }}</button><button type="button" class="edit" @click="openEdit(selectedProperty)">{{ $t('legacy.t_cf0180e2cd0f') }}</button></div>
        </template>
        <div v-else class="admin-owner-empty">{{ $t('legacy.t_23accb02a959') }}</div>
      </aside>
    </template>

    <template v-else>
    <div class="panel owners-table-panel">
      <div class="owners-panel-head">
        <div>
          <div class="owners-workspace-tabs" role="tablist" :aria-label="$t('legacy.t_18febdc46e72')">
            <button type="button" :class="{ active: workspaceTab === 'owners' }" @click="workspaceTab = 'owners'">{{ $t('ui.allOwners') }}</button>
            <button v-if="false" type="button" :class="{ active: workspaceTab === 'accounts' }" @click="workspaceTab = 'accounts'">{{ $t('legacy.t_315224062685') }}</button>
          </div>
          <span>{{ $t('ui.records', { count: filteredOwners.length }) }}</span>
        </div>
        <span v-if="loading">{{ $t('ui.loadingFromDatabase') }}</span>
      </div>

      <div v-if="errorMessage" class="admin-owner-state error" role="alert">
        <strong>{{ $t('ui.ownerDataLoadFailed') }}</strong>
        <span>{{ $lt(errorMessage) }}</span>
        <button type="button" @click="loadOwners">{{ $t('ui.reload') }}</button>
      </div>
      <div v-else class="table-wrap owners-table-wrap">
        <table>
          <thead>
            <tr><th>{{ $t('ui.ownerName') }}</th><th>{{ $t('ui.identityNo') }}</th><th>{{ $t('ui.mobile') }}</th><th>{{ $t('ui.mailbox') }}</th><th>{{ $t('ownerStaff.label') }}</th><th>{{ $t('ui.propertyCount') }}</th><th>{{ $t('ui.accountStatus') }}</th><th>{{ $t('ui.actions') }}</th></tr>
          </thead>
          <tbody>
            <tr v-for="owner in pagedOwners" :key="owner.id" :class="{ selected: owner.id === selectedOwnerId }" @click="selectOwner(owner)">
              <td><span class="avatar">{{ initials(owner.fullName) }}</span>{{ owner.fullName }}</td>
              <td>{{ owner.identityNo || '—' }}</td>
              <td>{{ owner.phone || '—' }}</td>
              <td>{{ owner.email || '—' }}</td>
              <td><span class="owner-staff-list-text">{{ responsibleStaffNames(owner) }}</span></td>
              <td><span class="owner-property-count">{{ $t('ui.properties', { count: owner.properties.length }) }}</span></td>
              <td><span class="tag" :class="owner.status === 'active' ? 'green' : 'gray'">{{ owner.status === 'active' ? $t('ui.enabled') : $t('ui.disabled') }}</span></td>
              <td><button class="row-actions owner-row-arrow" type="button" :title="$t('ui.viewProperties')" @click.stop="selectOwner(owner)">{{ $t('legacy.t_0596bf73ba05') }}</button></td>
            </tr>
            <tr v-if="!loading && !filteredOwners.length"><td colspan="8" class="admin-owner-empty">{{ $t('ui.noMatchingOwners') }}</td></tr>
          </tbody>
        </table>
      </div>
      <div class="owners-pager"><span>{{ $t('ui.records', { count: filteredOwners.length }) }}</span><div class="admin-building-pager"><button :disabled="ownerPageNumber <= 1" @click="goOwnerPage(ownerPageNumber - 1)">&lt;</button><button v-for="n in ownerVisiblePages" :key="n" :class="{ active: n === ownerPageNumber }" @click="goOwnerPage(n)">{{ n }}</button><button :disabled="ownerPageNumber >= ownerTotalPages" @click="goOwnerPage(ownerPageNumber + 1)">&gt;</button><select v-model.number="ownerPageSize"><option :value="5">{{ $t('building.recordsPerPage', { count: 5 }) }}</option><option :value="10">{{ $t('building.recordsPerPage', { count: 10 }) }}</option><option :value="20">{{ $t('building.recordsPerPage', { count: 20 }) }}</option><option :value="50">{{ $t('building.recordsPerPage', { count: 50 }) }}</option></select></div></div>
    </div>

    <div v-if="ownerDetailOpen && selectedOwner" class="admin-owner-detail-modal" role="dialog" aria-modal="true" @pointerdown.self="closeOwnerDetail">
      <section class="panel owners-detail admin-owner-detail">
        <div class="admin-owner-detail-modal-head">
          <strong>{{ $t('ui.ownerDetails') }}</strong>
          <button type="button" class="admin-owner-detail-close" :aria-label="$t('ui.close')" @click="closeOwnerDetail">×</button>
        </div>
        <template v-if="selectedOwner">
        <div class="owners-profile">
          <div class="big-avatar">{{ initials(selectedOwner.fullName) }}</div>
          <div><h3>{{ selectedOwner.fullName }}</h3><p>{{ selectedOwner.phone || $t('legacy.t_d04e9f9fe05a') }} {{ $t('legacy.t_b808d4c7ee51') }}</p></div>
          <span class="tag" :class="selectedOwner.status === 'active' ? 'green' : 'gray'">{{ selectedOwner.status === 'active' ? $t('legacy.t_f78d037abccd') : $t('legacy.t_d989e55188c9') }}</span><button type="button" class="owner-edit-btn" @click="openOwnerEdit"><span aria-hidden="true">✎</span> {{ $t('legacy.t_958cde7af4db') }}</button>
        </div>

        <section class="owners-section">
          <h4><span>{{ $t('legacy.t_356a192b7913') }}</span>{{ $t('legacy.t_30d562a1eddb') }}</h4>
          <div class="kv"><span>{{ $t('legacy.t_8fb4b186c803') }}</span><b>{{ selectedOwner.identityNo || '—' }}</b></div>
          <div class="kv"><span>{{ $t('legacy.t_c30e4f203e42') }}</span><b>{{ selectedOwner.phone || '—' }}</b></div>
          <div class="kv"><span>{{ $t('legacy.t_d2fbfa77a8af') }}</span><b>{{ selectedOwner.email || '—' }}</b></div>
        </section>

        <section class="owners-section owner-contact-fields">
          <h4><span>{{ $t('legacy.t_da4b9237bacc') }}</span>{{ $t('legacy.t_b4fc8598b7fb') }}</h4>
          <div class="kv"><span>{{ $t('legacy.t_53a15b735107') }}</span><b>{{ selectedOwner.ownerNo || `#${selectedOwner.id}` }}</b></div>
          <div class="kv"><span>{{ $t('legacy.t_cddd3bbff27d') }}</span><b>{{ selectedOwner.identityNo || '—' }}</b></div>
          <div class="kv"><span>{{ $t('legacy.t_196ac5431d6e') }}</span><b>{{ selectedOwner.passportNo || '—' }}</b></div>
          <div class="kv"><span>{{ $t('legacy.t_45d661f5882e') }}</span><b>{{ selectedOwner.mobilePhone || selectedOwner.phone || '—' }}</b></div>
          <div class="kv"><span>{{ $t('legacy.t_898dcb50fa59') }}</span><b>{{ selectedOwner.homePhone || '—' }}</b></div>
          <div class="kv"><span>{{ $t('legacy.t_89d89b88795a') }}</span><b>{{ selectedOwner.officePhone || '—' }}</b></div>
          <div class="kv"><span>{{ $t('legacy.t_0d01e2e86669') }}</span><b>{{ selectedOwner.email || '—' }}</b></div>
          <div class="kv"><span>{{ $t('ownerStaff.label') }}</span><b>{{ responsibleStaffNames(selectedOwner) }}</b></div>
        </section>

        <section class="owners-section owner-properties-section">
          <h4><span>{{ $t('legacy.t_da4b9237bacc') }}</span>{{ $t('legacy.t_4057b309ece8') }} <b>{{ selectedOwner.properties.length }}</b></h4>
          <article v-for="property in selectedOwner.properties" :key="property.ownerUnitId" class="admin-owner-property-card">
            <div class="owner-property-heading">
              <div><strong>{{ property.unitNo }}</strong><small>{{ property.projectName }}</small></div>
              <span class="tag" :class="lifecycleClass(property.assetStage)">{{ $lt(lifecycleLabel(property.assetStage)) }}</span>
            </div>
            <div class="owner-property-meta"><span>{{ property.unitType || $t('legacy.t_d865d84d2ab9') }}</span><span>{{ property.areaSqm ? `${property.areaSqm} m²` : $t('legacy.t_908f758f9c20') }}</span><b>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(property.purchasePrice) }}</b></div>
            <div v-if="property.assetStage === 'OPERATING'" class="owner-property-services">
              <span v-for="service in property.services" :key="service">{{ $lt(serviceLabel(service)) }}</span>
              <small v-if="!property.services?.length">{{ $t('legacy.t_92fd34a9ca85') }}</small>
            </div>
            <div class="owner-property-actions">
              <button type="button" @click="openDetails(property)">{{ $t('legacy.t_0596bf73ba05') }}</button>
              <button type="button" class="edit" @click="openEdit(property)">{{ $t('legacy.t_c9c77517fe85') }}</button>
            </div>
          </article>
          <p v-if="!selectedOwner.properties.length" class="admin-owner-empty">{{ $t('legacy.t_ba96acc00f99') }}</p>
        </section>

        <section v-if="preHandoverProperties.length" class="owners-section">
          <h4><span>{{ $t('legacy.t_77de68daecd8') }}</span>{{ $t('legacy.t_a39c951e1305') }}</h4>
          <div class="kv"><span>{{ $t('legacy.t_b25928c69902') }}</span><b>{{ $t('legacy.t_5e7b60c626a4') }} {{ money(ownerTotal) }}</b></div>
          <div class="kv"><span>{{ $t('legacy.t_96f608c16cef') }}</span><b class="money-red">{{ $t('legacy.t_5e7b60c626a4') }} {{ money(ownerPending) }}</b></div>
          <div class="progress"><i :style="{ width: ownerProgress + '%' }"></i></div>
        </section>
        </template>
      </section>
    </div>
    </template>

    <dialog ref="ownerDialog" class="modal admin-owner-dialog">
      <form method="dialog" @submit.prevent="saveOwner">
        <div class="modal-head"><h3>{{ ownerEditingId ? $t('legacy.t_56ca7e123ac5') : $t('legacy.t_4bc730395ca1') }}</h3><button class="icon-close" type="button" @click="closeOwnerDialog">×</button></div>
        <div class="form-grid">
          <label class="wide">{{ $t('legacy.t_1c4f579e884d') }}<input v-model.trim="ownerForm.fullName" maxlength="160" required :placeholder="$t('legacy.t_87746aa5b712')"></label>
          <label class="wide owner-mobile-field">{{ $t('legacy.t_45d661f5882e') }}
            <span class="owner-phone-entry">
              <select v-model="ownerForm.mobileCountry" :aria-label="$t('legacy.t_cc725f519afb')" @change="validateOwnerPhone(false)">
                <option v-for="country in phoneCountries" :key="country.code" :value="country.code">{{ $regionName(country.code) }} {{ country.dialCode }}</option>
              </select>
              <input v-model.trim="ownerForm.mobileNational" inputmode="tel" autocomplete="tel-national" maxlength="30" required :placeholder="selectedOwnerPhoneCountry.example" @blur="validateOwnerPhone(true)">
            </span>
            <small>{{ $t('legacy.t_458d10bfc8a2') }}</small>
            <small v-if="ownerPhoneError" class="owner-phone-error">{{ $lt(ownerPhoneError) }}</small>
          </label>
          <label>{{ $t('legacy.t_898dcb50fa59') }}<input v-model.trim="ownerForm.homePhone" maxlength="40"></label>
          <label>{{ $t('legacy.t_89d89b88795a') }}<input v-model.trim="ownerForm.officePhone" maxlength="40"></label>
          <label>{{ $t('legacy.t_d2fbfa77a8af') }}<input v-model.trim="ownerForm.email" type="email" maxlength="190" :placeholder="$t('legacy.t_66f171d88474')"></label>
          <label>{{ $t('legacy.t_cddd3bbff27d') }}<input v-model.trim="ownerForm.identityNo" maxlength="120"></label>
          <label>{{ $t('legacy.t_196ac5431d6e') }}<input v-model.trim="ownerForm.passportNo" maxlength="80"></label>
          <label>{{ $t('legacy.t_f91fbded4c19') }}<select v-model="ownerForm.status"><option value="active">{{ $t('legacy.t_ce6c3dc32674') }}</option><option value="inactive">{{ $t('legacy.t_d989e55188c9') }}</option></select></label>
          <div class="wide owner-staff-form-field">
            <span>{{ $t('ownerStaff.label') }}</span>
            <OwnerStaffMultiSelect v-model="ownerForm.responsibleUserIds" :options="staffOptions" :label="$t('ownerStaff.label')" :placeholder="$t('ownerStaff.select')" :empty-text="$t('ownerStaff.empty')" />
            <small v-if="staffOptionsError" class="owner-phone-error">{{ $lt(staffOptionsError) }}</small>
          </div>
          <p class="admin-owner-form-note wide">{{ $t('legacy.t_86e05c5b59d5') }}</p>
          <p v-if="ownerFormError" class="admin-property-error wide">{{ $lt(ownerFormError) }}</p>
        </div>
        <menu><button type="button" @click="closeOwnerDialog">{{ $t('legacy.t_4d0b4688c787') }}</button><button type="submit" class="primary-btn" :disabled="ownerSaving">{{ ownerSaving ? $t('legacy.t_59d9eae44030') : (ownerEditingId ? $t('legacy.t_60b4ae9082a3') : $t('legacy.t_f2a2753dcacf')) }}</button></menu>
      </form>
    </dialog>

    <dialog ref="propertyCreateDialog" class="modal admin-property-create-dialog">
      <form method="dialog" @submit.prevent="saveNewProperty">
        <div class="modal-head">
          <div><h3>{{ $t('legacy.t_b5e81d15de84') }}</h3><small>{{ $t(propertyCreateStep === 1 ? 'properties.createStepChooseOwner' : 'properties.createStepDetails') }}</small></div>
          <button class="icon-close" type="button" @click="closePropertyCreateDialog">×</button>
        </div>

        <section v-if="propertyCreateStep === 1" class="owner-picker-step">
          <label class="owner-picker-search">{{ $t('legacy.t_28418f110964') }}<input v-model.trim="propertyOwnerSearch" :placeholder="$t('legacy.t_849b42bcd603')"></label>
          <div class="owner-picker-list">
            <button v-for="owner in propertyOwnerOptions" :key="owner.id" type="button" :class="{ selected: propertyOwnerId === owner.id }" @click="propertyOwnerId = owner.id">
              <span class="avatar">{{ initials(owner.fullName) }}</span>
              <span><strong>{{ owner.fullName }}</strong><small>{{ owner.phone || owner.email || $t('legacy.t_f80fc00eb518') }}</small></span>
              <b>{{ owner.properties.length }} {{ $t('legacy.t_5ea7b33be619') }}</b><i>✓</i>
            </button>
            <p v-if="!propertyOwnerOptions.length" class="admin-owner-empty">{{ $t('legacy.t_e83da67e0d0f') }}</p>
          </div>
          <p v-if="propertyCreateError" class="admin-property-error">{{ $lt(propertyCreateError) }}</p>
        </section>

        <div v-else class="form-grid admin-new-property-form">
          <div class="selected-property-owner wide"><span class="avatar">{{ initials(propertyCreateOwner?.fullName) }}</span><div><small>{{ $t('legacy.t_ac10112ddc97') }}</small><strong>{{ propertyCreateOwner?.fullName }}</strong></div><button type="button" @click="propertyCreateStep = 1">{{ $t('legacy.t_e80cbc759694') }}</button></div>
          <label class="wide">{{ $t('legacy.t_f189d51d8d04') }}
            <select v-model="propertyCreateForm.projectId" required @change="selectExistingProject">
              <option value="">{{ $t('properties.projectInputPlaceholder') }}</option>
              <option v-for="project in propertyProjects" :key="project.id" :value="String(project.id)">{{ project.name }}{{ project.city ? ` · ${project.city}` : '' }}</option>
            </select>
          </label>
          <label>{{ $t('legacy.t_5807b077534d') }}<input v-model.trim="propertyCreateForm.building" maxlength="80"></label>
          <label>{{ $t('legacy.t_fdf913aed6c7') }}<input v-model.trim="propertyCreateForm.floorNo" maxlength="20"></label>
          <label>{{ $t('legacy.t_a366f87e463d') }}<input v-model.trim="propertyCreateForm.unitNo" maxlength="40" required></label>
          <label>{{ $t('legacy.t_80b3b9d472c5') }}<input v-model.trim="propertyCreateForm.unitType" maxlength="80"></label>
          <label>{{ $t('legacy.t_3d68565ece0f') }}<input v-model.number="propertyCreateForm.areaSqm" type="number" min="0.01" step="0.01"></label>
          <label>{{ $t('legacy.t_a51685c50e27') }}<input v-model.number="propertyCreateForm.bedroomCount" type="number" min="0" max="50"></label>
          <label>{{ $t('legacy.t_a1feb767c3c4') }}<select v-model="propertyCreateForm.assetStage"><option value="PRE_HANDOVER">{{ $t('legacy.t_8fbf8d463812') }}</option><option value="OPERATING">{{ $t('legacy.t_6464244e6a21') }}</option></select></label>
          <label v-if="propertyCreateForm.assetStage === 'PRE_HANDOVER'">{{ $t('legacy.t_bc44c4426292') }}<input v-model="propertyCreateForm.expectedHandoverDate" type="date"></label>
          <label v-else>{{ $t('legacy.t_5e6ad3f8b374') }}<input v-model="propertyCreateForm.actualHandoverDate" type="date" required></label>
          <fieldset v-if="propertyCreateForm.assetStage === 'OPERATING'" class="property-service-picker wide">
            <legend>{{ $t('legacy.t_bdc0d84fec07') }}</legend>
            <label v-for="service in serviceOptions" :key="service.value"><input v-model="propertyCreateForm.services" type="checkbox" :value="service.value">{{ $lt(service.label) }}</label>
          </fieldset>
          <label>{{ $t('legacy.t_5a6b3e9d88af') }}<select v-model="propertyCreateForm.listingStatus"><option value="available">{{ $t('legacy.t_e91365cf9ed9') }}</option><option value="reserved">{{ $t('legacy.t_ede0de28e966') }}</option><option value="sold">{{ $t('legacy.t_606980e3452f') }}</option><option value="rented">{{ $t('legacy.t_2ba7bdb71038') }}</option><option value="inactive">{{ $t('legacy.t_d989e55188c9') }}</option></select></label>
          <label>{{ $t('legacy.t_3aa05659bf08') }}<input v-model.number="propertyCreateForm.purchasePrice" type="number" min="0" step="0.01" required></label>
          <label>{{ $t('legacy.t_0f79c83ef51a') }}<input v-model.number="propertyCreateForm.ownershipPercent" type="number" min="0.01" max="100" step="0.01" required></label>
          <label>{{ $t('legacy.t_83bf65a6c9e2') }}<input v-model="propertyCreateForm.startDate" type="date"></label>
          <label class="property-primary-check"><input v-model="propertyCreateForm.primary" type="checkbox">{{ $t('legacy.t_c59ab1be53f3') }}</label>
          <p v-if="propertyCreateError" class="admin-property-error wide">{{ $lt(propertyCreateError) }}</p>
        </div>

        <menu v-if="propertyCreateStep === 1"><button type="button" @click="closePropertyCreateDialog">{{ $t('legacy.t_4d0b4688c787') }}</button><button type="button" class="primary-btn" :disabled="!propertyOwnerId" @click="continuePropertyCreate">{{ $t('legacy.t_d0a1c0c58c5d') }}</button></menu>
        <menu v-else><button type="button" @click="propertyCreateStep = 1">{{ $t('legacy.t_75ef1241c0f4') }}</button><button type="submit" class="primary-btn" :disabled="propertyCreating">{{ propertyCreating ? $t('legacy.t_2cd5496ec548') : $t('legacy.t_ea9957f58b51') }}</button></menu>
      </form>
    </dialog>

    <dialog ref="propertyDialog" class="modal admin-property-dialog">
      <form method="dialog" @submit.prevent>
        <div class="modal-head"><h3>{{ dialogMode === 'edit' ? $t('legacy.t_cf0180e2cd0f') : $t('legacy.t_c2119703b8d0') }}</h3><button class="icon-close" type="button" @click="closeDialog">×</button></div>

        <div v-if="dialogLoading" class="admin-owner-state">{{ $t('legacy.t_00f31e8a581d') }}</div>
        <div v-else-if="dialogError && !dialogProperty" class="admin-owner-state error">{{ $lt(dialogError) }}</div>
        <div v-else-if="dialogProperty" class="form-grid admin-property-form">
          <label>{{ $t('legacy.t_f189d51d8d04') }}<input :value="dialogProperty.projectName" disabled></label>
          <label>{{ $t('legacy.t_590157b8d4d7') }}<input :value="dialogProperty.city || '—'" disabled></label>
          <label>{{ $t('legacy.t_5807b077534d') }}<input v-model="editForm.building" :disabled="dialogMode !== 'edit'" maxlength="80"></label>
          <label>{{ $t('legacy.t_fdf913aed6c7') }}<input v-model="editForm.floorNo" :disabled="dialogMode !== 'edit'" maxlength="20"></label>
          <label>{{ $t('legacy.t_a366f87e463d') }}<input v-model.trim="editForm.unitNo" :disabled="dialogMode !== 'edit'" maxlength="40" required></label>
          <label>{{ $t('legacy.t_80b3b9d472c5') }}<input v-model="editForm.unitType" :disabled="dialogMode !== 'edit'" maxlength="80"></label>
          <label>{{ $t('legacy.t_3d68565ece0f') }}<input v-model.number="editForm.areaSqm" :disabled="dialogMode !== 'edit'" type="number" min="0.01" step="0.01"></label>
          <label>{{ $t('legacy.t_a51685c50e27') }}<input v-model.number="editForm.bedroomCount" :disabled="dialogMode !== 'edit'" type="number" min="0" max="50"></label>
          <label>{{ $t('legacy.t_a1feb767c3c4') }} <select v-model="editForm.assetStage" :disabled="dialogMode !== 'edit'">
              <option value="PRE_HANDOVER" :disabled="dialogProperty.assetStage === 'OPERATING'">{{ $t('legacy.t_8fbf8d463812') }}</option>
              <option value="OPERATING">{{ $t('legacy.t_6464244e6a21') }}</option>
            </select>
          </label>
          <label v-if="editForm.assetStage === 'PRE_HANDOVER'">{{ $t('legacy.t_bc44c4426292') }}<input v-model="editForm.expectedHandoverDate" :disabled="dialogMode !== 'edit'" type="date"></label>
          <label v-else>{{ $t('legacy.t_5e6ad3f8b374') }}<input v-model="editForm.actualHandoverDate" :disabled="dialogMode !== 'edit'" type="date" required></label>
          <fieldset v-if="editForm.assetStage === 'OPERATING'" class="property-service-picker wide">
            <legend>{{ $t('legacy.t_bdc0d84fec07') }}</legend>
            <label v-for="service in serviceOptions" :key="service.value"><input v-model="editForm.services" type="checkbox" :value="service.value" :disabled="dialogMode !== 'edit'">{{ $lt(service.label) }}</label>
          </fieldset>
          <label>{{ $t('legacy.t_5a6b3e9d88af') }} <select v-model="editForm.listingStatus" :disabled="dialogMode !== 'edit'">
              <option value="available">{{ $t('legacy.t_e91365cf9ed9') }}</option><option value="reserved">{{ $t('legacy.t_ede0de28e966') }}</option><option value="sold">{{ $t('legacy.t_606980e3452f') }}</option><option value="rented">{{ $t('legacy.t_2ba7bdb71038') }}</option><option value="inactive">{{ $t('legacy.t_d989e55188c9') }}</option>
            </select>
          </label>
          <label>{{ $t('legacy.t_3aa05659bf08') }}<input v-model.number="editForm.purchasePrice" :disabled="dialogMode !== 'edit'" type="number" min="0" step="0.01" required></label>
          <template v-if="editForm.assetStage === 'PRE_HANDOVER'">
            <label>{{ $t('legacy.t_e63769dcd289') }}<input :value="money(dialogProperty.paidAmount)" disabled></label>
            <label>{{ $t('legacy.t_68856e4c6d17') }}<input :value="money(dialogProperty.remainingAmount)" disabled></label>
          </template>
          <p v-if="dialogError" class="admin-property-error wide">{{ $lt(dialogError) }}</p>
        </div>

        <menu>
          <button type="button" @click="closeDialog">{{ $t('legacy.t_ddc05404b0d6') }}</button>
          <button v-if="dialogMode === 'edit'" type="button" class="primary-btn" :disabled="saving" @click="saveProperty">{{ saving ? $t('legacy.t_8488ea2522af') : $t('legacy.t_df08b46f838a') }}</button>
        </menu>
      </form>
    </dialog>
  </section>
</template>

<script>
import { LoaderCircle, Trash2 } from '@lucide/vue';
import { createAdminOwner, createAdminOwnerProperty, deleteAdminProperty, fetchAdminOwnerProperty, fetchAdminOwnerStaffOptions, fetchAdminOwnerSummary, fetchAdminOwners, fetchAdminProperties, fetchAdminPropertyProjects, updateAdminOwner, updateAdminOwnerProperty } from '../services/propertyApi';
import { navigate } from '../router';
import { PHONE_COUNTRIES, phoneCountry, splitPhone, validatePhone } from '../utils/tenantPhone';
import OwnerStaffMultiSelect from './OwnerStaffMultiSelect.vue';

export default {
  components: { LoaderCircle, Trash2, OwnerStaffMultiSelect },
  inject: ['page'],
  props: { mode: { type: String, default: 'owners' } },
  data() {
    return {
      workspaceTab: 'owners',
      owners: [], propertyRows: [], loading: false, errorMessage: '', selectedOwnerId: null, selectedPropertyKey: null, propertyDeletingId: null,
      propertyPageNumber: 1, propertyPageSize: 5, propertyTotalRows: 0, propertyTotalPages: 1, propertyRequestSerial: 0,
      ownerPageNumber: 1, ownerPageSize: 5, ownerDetailOpen: false,
      ownerSaving: false, ownerFormError: '', ownerPhoneError: '', ownerEditingId: null, phoneCountries: PHONE_COUNTRIES, staffOptions: [], staffOptionsError: '',
      ownerForm: { ownerNo: '', fullName: '', identityNo: '', phone: '', mobilePhone: '', mobileCountry: 'MY', mobileNational: '', homePhone: '', officePhone: '', passportNo: '', email: '', status: 'active', responsibleUserIds: [] },
      propertyProjects: [], propertyCreateStep: 1, propertyOwnerSearch: '', propertyOwnerId: null,
      propertyCreating: false, propertyCreateError: '',
      serviceOptions: [{ value: 'RENTAL', label: '出租' }, { value: 'RESALE', label: '代售' }, { value: 'MANAGEMENT', label: '代管' }],
      propertyCreateForm: { projectId: '', projectName: '', building: '', floorNo: '', unitNo: '', unitType: '', areaSqm: null, bedroomCount: null, listingStatus: 'available', assetStage: 'PRE_HANDOVER', expectedHandoverDate: '', actualHandoverDate: '', services: [], purchasePrice: 0, ownershipPercent: 100, primary: true, startDate: '' },
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
        const staffText = (owner.responsibleStaff || []).map(staff => `${staff.displayName || ''} ${staff.username || ''}`).join(' ');
        const matchesKeyword = !keyword || `${owner.fullName} ${owner.phone || ''} ${owner.email || ''} ${staffText} ${propertyText}`.toLowerCase().includes(keyword);
        const matchesProject = projectFilter.includes('全部') || owner.properties.some(item => item.projectName === projectFilter);
        const matchesStatus = statusFilter.includes('全部') || (statusFilter === '啟用' ? owner.status === 'active' : statusFilter === '停用' ? owner.status !== 'active' : owner.properties.some(item => this.lifecycleLabel(item.assetStage) === statusFilter));
        return matchesKeyword && matchesProject && matchesStatus;
      });
    },
    pagedOwners() { const start = (this.ownerPageNumber - 1) * this.ownerPageSize; return this.filteredOwners.slice(start, start + this.ownerPageSize); },
    ownerTotalPages() { return Math.max(1, Math.ceil(this.filteredOwners.length / this.ownerPageSize)); },
    ownerVisiblePages() { const count = Math.min(5, this.ownerTotalPages); const start = Math.max(1, Math.min(this.ownerPageNumber - 2, this.ownerTotalPages - count + 1)); return Array.from({ length: count }, (_, index) => start + index); },
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
    ownerProgress() { return this.ownerTotal ? Math.max(0, Math.min(100, Math.round((this.ownerTotal - this.ownerPending) / this.ownerTotal * 100))) : 0; },
    selectedOwnerPhoneCountry() { return phoneCountry(this.ownerForm.mobileCountry); }
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
        if (this.ownerPageNumber > this.ownerTotalPages) this.ownerPageNumber = this.ownerTotalPages;
        if (!this.pagedOwners.some(owner => owner.id === this.selectedOwnerId)) {
          this.selectedOwnerId = this.pagedOwners[0]?.id || null;
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
    ownerPageSize() { if (!this.isPropertyMode) this.resetOwnerPage(); },
    'page.moduleSearch'() { if (this.isPropertyMode) this.resetPropertyPage(); else this.resetOwnerPage(); },
    'page.globalSearch'() { if (this.isPropertyMode) this.resetPropertyPage(); else this.resetOwnerPage(); },
    'page.projectFilter'() { if (this.isPropertyMode) this.resetPropertyPage(); else this.resetOwnerPage(); },
    'page.statusFilter'() { if (this.isPropertyMode) this.resetPropertyPage(); else this.resetOwnerPage(); },
    'propertyCreateForm.assetStage'(stage) {
      if (stage === 'PRE_HANDOVER') { this.propertyCreateForm.actualHandoverDate = ''; this.propertyCreateForm.services = []; }
      else this.propertyCreateForm.expectedHandoverDate = '';
    },
    'editForm.assetStage'(stage) {
      if (stage === 'PRE_HANDOVER') { this.editForm.actualHandoverDate = ''; this.editForm.services = []; }
      else this.editForm.expectedHandoverDate = '';
    }
  },
  async mounted() { this.page.adminOwnerWorkspaceTab = this.workspaceTab; if (this.isPropertyMode) this.page.statusFilter = '全部出租狀態'; await Promise.all([this.loadOwners(), this.isPropertyMode ? Promise.resolve() : this.loadStaffOptions()]); if (this.isPropertyMode) await this.loadProperties(); },
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
          { icon: 'property', label: this.$t('properties.totalProperties'), value: this.$t('ui.properties', { count: allProperties.length }), delta: this.$t('ui.liveDatabaseStatistics'), trend: '' },
          { icon: 'managed', label: this.$t('properties.handedOver'), value: this.$t('ui.properties', { count: allProperties.filter(property => property.assetStage === 'OPERATING').length }), delta: this.$t('properties.availableForManagement'), trend: '' },
          { icon: 'handover', label: this.$t('properties.preHandover'), value: this.$t('ui.properties', { count: allProperties.filter(property => property.assetStage === 'PRE_HANDOVER').length }), delta: this.$t('properties.underConstruction'), trend: '' },
          { icon: 'payment', label: this.$t('properties.outstandingPayments'), value: this.$t('ui.properties', { count: allProperties.filter(property => Number(property.remainingAmount || 0) > 0).length }), delta: this.$t('properties.followUpPayment'), trend: '' }
        ] : [
          { icon: 'people', label: this.$t('ui.totalOwners'), value: this.$t('ui.owners', { count: Number(liveSummary.ownerCount || 0) }), delta: this.$t('ui.liveDatabaseStatistics'), trend: '' },
          { icon: 'active', label: this.$t('ui.activeOwners'), value: this.$t('ui.records', { count: this.owners.filter(owner => owner.status === 'active').length }), delta: this.$t('ui.canAccessOwner'), trend: '' },
          { icon: 'property', label: this.$t('ui.linkedProperties'), value: this.$t('ui.records', { count: this.owners.filter(owner => owner.properties.length).length }), delta: this.$t('ui.ownersWithProperties'), trend: '' },
          { icon: 'unbound', label: this.$t('ui.unlinkedProperties'), value: this.$t('ui.records', { count: this.owners.filter(owner => !owner.properties.length).length }), delta: this.$t('ui.needsPropertyLink'), trend: '' }
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
        this.propertyRows = (response.rows || []).map(item => ({ ...item.property, paymentAccountNumbers: { electricity: item.property.electricityAccountNo || '', water: item.property.waterAccountNo || '', sewerage: item.property.sewerageAccountNo || '', gas: item.property.gasAccountNo || '', withholdingTax: item.property.withholdingTaxAccountNo || '', landTax: item.property.landTaxAccountNo || '', assessmentTax: item.property.assessmentTaxAccountNo || '' }, ownerId: item.ownerId, ownerName: item.ownerName, ownerPhone: item.ownerPhone, ownerEmail: item.ownerEmail, rentalStatus: item.rentalStatus, rowKey: `${item.ownerId}-${item.property.ownerUnitId}` }));
        this.propertyTotalRows = Number(response.page?.totalRows || 0); this.propertyTotalPages = Number(response.page?.totalPages || 1); this.propertyPageNumber = Number(response.page?.page || 1);
        this.selectedPropertyKey = this.propertyRows.some(property => property.rowKey === preferredKey) ? preferredKey : this.propertyRows[0]?.rowKey || null;
        const selected = this.propertyRows.find(property => property.rowKey === this.selectedPropertyKey); if (selected) this.selectedOwnerId = selected.ownerId;
        this.syncPropertyExportRows(this.propertyRows);
      } catch (error) { if (serial !== this.propertyRequestSerial) return; this.propertyRows = []; this.propertyTotalRows = 0; this.errorMessage = error.message || '房產資料載入失敗'; }
      finally { if (serial === this.propertyRequestSerial) this.loading = false; }
    },
    resetPropertyPage() { this.propertyPageNumber = 1; this.loadProperties(); },
    goPropertyPage(page) { if (page < 1 || page > this.propertyTotalPages || page === this.propertyPageNumber) return; this.propertyPageNumber = page; this.loadProperties(); },
    resetOwnerPage() { this.ownerPageNumber = 1; this.$nextTick(() => { this.selectedOwnerId = this.pagedOwners[0]?.id || null; }); },
    goOwnerPage(page) { if (page < 1 || page > this.ownerTotalPages || page === this.ownerPageNumber) return; this.ownerPageNumber = page; this.$nextTick(() => { this.selectedOwnerId = this.pagedOwners[0]?.id || null; }); },
    selectOwner(owner) { this.selectedOwnerId = owner.id; this.ownerDetailOpen = true; },
    closeOwnerDetail() { this.ownerDetailOpen = false; },
    selectProperty(property) { this.selectedPropertyKey = property.rowKey; this.selectedOwnerId = property.ownerId; if (this.isPropertyMode && property?.unitId) navigate(`/admin/properties/${property.unitId}`); },
    async removeProperty(property) {
      if (!property?.unitId || !window.confirm(this.$t('properties.deletePropertyConfirm', { name: `${property.projectName || ''} ${property.unitNo || ''}`.trim() }))) return;
      this.propertyDeletingId = property.unitId;
      try {
        await deleteAdminProperty(property.unitId);
        this.page.showToast(this.$t('properties.propertyDeleted'));
        await this.loadOwners();
        await this.loadProperties();
      } catch (error) {
        this.page.showToast(error?.message || this.$t('properties.propertyDeleteFailed'));
      } finally {
        this.propertyDeletingId = null;
      }
    },
    async loadStaffOptions() {
      this.staffOptionsError = '';
      try { this.staffOptions = await fetchAdminOwnerStaffOptions(); }
      catch (error) { this.staffOptions = []; this.staffOptionsError = error.message || this.$t('ownerStaff.empty'); }
    },
    openCreateOwner() {
      this.ownerEditingId = null;
      this.ownerForm = { ownerNo: '', fullName: '', identityNo: '', phone: '', mobilePhone: '', mobileCountry: 'MY', mobileNational: '', homePhone: '', officePhone: '', passportNo: '', email: '', status: 'active', responsibleUserIds: [] };
      this.ownerFormError = ''; this.ownerPhoneError = '';
      this.$refs.ownerDialog.showModal();
    },
    closeOwnerDialog() { this.$refs.ownerDialog?.close(); },
    openOwnerEdit() {
      const owner = this.selectedOwner;
      if (!owner) return;
      const mobile = splitPhone(owner.mobilePhone || owner.phone || '');
      this.ownerEditingId = owner.id;
      this.ownerForm = { ownerNo: owner.ownerNo || '', fullName: owner.fullName || '', identityNo: owner.identityNo || '', phone: owner.phone || '', mobilePhone: owner.mobilePhone || owner.phone || '', mobileCountry: mobile.country, mobileNational: mobile.nationalNumber, homePhone: owner.homePhone || '', officePhone: owner.officePhone || '', passportNo: owner.passportNo || '', email: owner.email || '', status: owner.status || 'active', responsibleUserIds: (owner.responsibleStaff || []).map(staff => staff.id) };
      this.ownerFormError = ''; this.ownerPhoneError = '';
      this.$refs.ownerDialog.showModal();
    },
    validateOwnerPhone(showError = false) {
      const result = validatePhone(this.ownerForm.mobileNational, this.ownerForm.mobileCountry, true);
      if (showError || result.valid) {
        if (result.reason === 'country_mismatch') this.ownerPhoneError = `号码国家码与所选的${this.selectedOwnerPhoneCountry.label}不一致。`;
        else if (result.reason === 'invalid') this.ownerPhoneError = `请输入有效的${this.selectedOwnerPhoneCountry.label}手机号，例如 ${this.selectedOwnerPhoneCountry.example}。`;
        else if (result.reason === 'required') this.ownerPhoneError = '请填写业主手机号，该号码将作为登录账号。';
        else this.ownerPhoneError = '';
      }
      return result;
    },
    async saveOwner() {
      if (!this.ownerForm.fullName) { this.ownerFormError = '請填寫業主姓名'; return; }
      if (!this.ownerForm.responsibleUserIds.length) { this.ownerFormError = this.$t('ownerStaff.required'); return; }
      const mobile = this.validateOwnerPhone(true);
      if (!mobile.valid) { this.ownerFormError = this.ownerPhoneError; return; }
      this.ownerForm.phone = mobile.e164;
      this.ownerForm.mobilePhone = mobile.e164;
      this.ownerSaving = true; this.ownerFormError = '';
      try {
        const payload = { ...this.ownerForm };
        delete payload.ownerNo;
        delete payload.mobileCountry;
        delete payload.mobileNational;
        const owner = this.ownerEditingId ? await updateAdminOwner(this.ownerEditingId, payload) : await createAdminOwner(payload);
        await this.loadOwners(owner.id);
        this.closeOwnerDialog();
        this.page.showToast(this.ownerEditingId ? '房主資料已更新' : '業主已新增，登入帳號為手機號，初始密碼 123456');
      } catch (error) { this.ownerFormError = error.message || (this.ownerEditingId ? '房主资料更新失败' : '新增业主失败'); }
      finally { this.ownerSaving = false; }
    },
    responsibleStaffNames(owner) { return (owner?.responsibleStaff || []).map(staff => staff.displayName || staff.username).filter(Boolean).join('、') || '—'; },
    emptyPropertyCreateForm() {
      return { projectId: '', projectName: '', building: '', floorNo: '', unitNo: '', unitType: '', areaSqm: null, bedroomCount: null, listingStatus: 'available', assetStage: 'PRE_HANDOVER', expectedHandoverDate: '', actualHandoverDate: '', services: [], purchasePrice: 0, ownershipPercent: 100, primary: true, startDate: new Date().toISOString().slice(0, 10) };
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
    selectExistingProject() {
      const project = this.propertyProjects.find(item => String(item.id) === String(this.propertyCreateForm.projectId));
      this.propertyCreateForm.projectName = project?.name || '';
    },
    async saveNewProperty() {
      if (!this.propertyCreateOwner) { this.propertyCreateStep = 1; this.propertyCreateError = '請先選擇業主'; return; }
      if (!this.propertyCreateForm.projectId || !this.propertyCreateForm.unitNo) { this.propertyCreateError = '請選擇已有建案，並填寫單位編號'; return; }
      if (this.propertyCreateForm.assetStage === 'OPERATING' && !this.propertyCreateForm.actualHandoverDate) { this.propertyCreateError = '已交房房產需要填寫實際交房日期'; return; }
      await this.submitNewProperty();
    },
    async submitNewProperty() {
      this.propertyCreating = true; this.propertyCreateError = '';
      try {
        const payload = { ...this.propertyCreateForm, projectId: Number(this.propertyCreateForm.projectId) };
        await createAdminOwnerProperty(this.propertyCreateOwner.id, payload);
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
    lifecycleLabel(stage) { return ({ PRE_HANDOVER: this.$t('properties.preHandover'), OPERATING: this.$t('properties.handedOver'), DISPOSED: this.$t('ui.disabled') })[stage] || '—'; },
    lifecycleClass(stage) { return stage === 'OPERATING' ? 'green' : stage === 'DISPOSED' ? 'gray' : 'orange'; },
    serviceLabel(service) { return ({ RENTAL: '出租', RESALE: '代售', MANAGEMENT: '代管' })[service] || service; },
    statusLabel(status) { return ({ paid: '正常', paying: '正常', due_soon: '待處理', overdue: '逾期', not_configured: '待設定', not_applicable: '不適用' })[status] || '待設定'; },
    listingStatusLabel(status) { return ({ available: '可用', reserved: '已預留', sold: '已出售', rented: '出租中', inactive: '停用' })[status] || status || '未設定'; },
    rentalStatusParam(value) { return ({ '未交房': 'pre_handover', '已交房待出租': 'pending_rental', '出租中': 'rented', '未啟用出租': 'not_for_rent' })[value] || ''; },
    rentalStatusLabel(value) { return ({ pre_handover: this.$t('properties.preHandover'), pending_rental: this.$t('properties.pendingRental'), rented: this.$t('properties.rented'), not_for_rent: this.$t('properties.notForRent') })[value] || '—'; },
    rentalStatusClass(value) { return value === 'rented' ? 'green' : value === 'pending_rental' ? 'orange' : value === 'pre_handover' ? 'gray' : 'blue'; },
    statusClass(status) { return status === 'paid' || status === 'paying' ? 'green' : status === 'overdue' ? 'red' : 'orange'; },
    syncOwnerExportRows(owners) {
      this.page.adminOwnerExportHeaders = ['業主姓名', '證件號', '手機號', '郵箱', '房產數量', '帳號狀態'];
      this.page.adminOwnerExportRows = owners.map(owner => [owner.fullName, owner.identityNo || '', owner.phone || '', owner.email || '', owner.properties.length, owner.status === 'active' ? '啟用' : '停用']);
    },
    syncPropertyExportRows(properties) {
          this.page.adminOwnerExportHeaders = ['建案／項目', '城市', '棟', '樓層', '單位編號', '業主姓名', '手機號', '房型', '面積（m²）', '房產階段', '單位狀態', '房產總價（RM）', '已繳金額（RM）', '未繳金額（RM）', '持有比例（%）', '主房主', '电费账户号码', '水费账户号码', '排污费账户号码', '瓦斯费账户号码', '预扣税账户号码', '地税账户号码', '门牌税账户号码'];
          this.page.adminOwnerExportRows = properties.map(property => [property.projectName || '', property.city || '', property.building || '', property.floorNo || '', property.unitNo || '', property.ownerName || '', property.ownerPhone || '', property.unitType || '', property.areaSqm ?? '', this.lifecycleLabel(property.assetStage), this.listingStatusLabel(property.listingStatus), Number(property.purchasePrice || 0), Number(property.paidAmount || 0), Number(property.remainingAmount || 0), property.ownershipPercent ?? '', property.primary ? '是' : '否', property.paymentAccountNumbers?.electricity || '', property.paymentAccountNumbers?.water || '', property.paymentAccountNumbers?.sewerage || '', property.paymentAccountNumbers?.gas || '', property.paymentAccountNumbers?.withholdingTax || '', property.paymentAccountNumbers?.landTax || '', property.paymentAccountNumbers?.assessmentTax || '']);
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
<style scoped>
/* Hallmark · pre-emit critique: P5 H5 E5 S5 R5 V4 */
/* Hallmark · component: property row actions · genre: modern-minimal · theme: existing CCPS
 * states: default · hover · focus · active · disabled · loading · error/success via existing toast
 * contrast: pass
 */
.property-list-actions{--property-action-accent:var(--admin-accent,#087f87);--property-action-accent-dark:var(--admin-accent-dark,#05646c);--property-action-line:var(--admin-line,#d9e4eb);--property-action-paper:#fff;--property-action-soft:#eff9fa;--property-action-danger:#b33a3a;--property-action-danger-line:#e7bcbc;--property-action-danger-soft:#fff3f3;display:inline-flex;align-items:center;justify-content:center;gap:6px;white-space:nowrap}
.property-list-workspace .owners-table-wrap th:last-child,.property-list-workspace .owners-table-wrap td:last-child{width:120px!important;min-width:120px!important;padding-inline:7px!important}
.property-list-workspace .owners-table-wrap .property-actions-cell{overflow:visible!important;text-overflow:clip!important}
.property-list-workspace .owners-table-wrap .property-list-actions .row-actions{display:inline-flex;box-sizing:border-box;align-items:center;justify-content:center;height:34px!important;margin:0;padding:0;border-radius:8px;line-height:1;cursor:pointer;transition:transform .12s ease-out,background-color .12s ease-out,border-color .12s ease-out,color .12s ease-out,opacity .12s ease-out}
.property-list-workspace .owners-table-wrap .property-list-actions .property-open-action{width:64px!important;min-width:64px!important;gap:4px;border-color:var(--property-action-line);background:var(--property-action-paper);color:var(--property-action-accent-dark);font-size:12px!important;font-weight:800}
.property-list-workspace .owners-table-wrap .property-list-actions .property-delete-action{width:34px!important;min-width:34px!important;border-color:var(--property-action-danger-line);background:var(--property-action-paper);color:var(--property-action-danger)}
@media(hover:hover) and (pointer:fine){.property-list-actions .property-open-action:hover{border-color:var(--property-action-accent);background:var(--property-action-soft);color:var(--property-action-accent-dark)}.property-list-actions .property-delete-action:hover{border-color:var(--property-action-danger);background:var(--property-action-danger-soft)}}
.property-list-actions .row-actions:focus-visible{outline:2px solid var(--property-action-accent);outline-offset:2px}
.property-list-actions .property-delete-action:focus-visible{outline-color:var(--property-action-danger)}
.property-list-actions .row-actions:active{transform:translateY(1px)}
.property-list-actions .row-actions:disabled{cursor:not-allowed;opacity:.42;transform:none}
.property-list-actions .property-delete-action[data-state='loading']{border-color:var(--property-action-danger-line);background:var(--property-action-danger-soft)}
.property-action-spinner{animation:property-action-spin .8s linear infinite}
@keyframes property-action-spin{to{transform:rotate(360deg)}}
@media(prefers-reduced-motion:reduce){.property-list-actions .row-actions{transition:none}.property-action-spinner{animation-duration:1.6s}}
.owner-mobile-field{display:grid;gap:7px}.owner-phone-entry{display:grid;grid-template-columns:190px minmax(0,1fr);column-gap:14px}.owner-phone-entry>select,.owner-phone-entry>input{box-sizing:border-box;min-width:0;width:100%;margin-top:0!important;border-radius:9px!important}.owner-mobile-field>small{color:#71889f;font-size:11px}.owner-mobile-field .owner-phone-error{color:#c43d45;font-weight:700}@media(max-width:720px){.owner-phone-entry{grid-template-columns:1fr;row-gap:10px}}
</style>
