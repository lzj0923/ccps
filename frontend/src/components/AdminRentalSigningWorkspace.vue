<template>
  <section class="rental-signing-page">
    <header class="rental-signing-header">
      <div><span>{{ $t('rentalFiles.kicker') }}</span><h2>{{ $t('rentalFiles.title') }}</h2><p>{{ $t('rentalFiles.description') }}</p></div>
      <div class="rental-signing-total"><strong>{{ completedSigningSteps }} / 6</strong><small>{{ $t('rentalFiles.signingProgress') }}</small></div>
    </header>

    <div class="rental-signing-layout">
      <aside class="rental-signing-property-picker">
        <div class="rental-signing-picker-head"><strong>{{ $t('rentalFiles.selectProperty') }}</strong><span>{{ properties.length }}</span></div>
        <label class="rental-signing-project-filter"><span>{{ $t('legacy.t_cf545c9c1bf9') }}</span><select v-model="selectedProjectName"><option value="">{{ $t('legacy.t_4502f4a3a4aa') }}</option><option v-for="project in projectOptions" :key="project" :value="project">{{ project }}</option></select></label>
        <input v-model.trim="propertySearch" type="search" :placeholder="$t('rentalFiles.propertySearch')">
        <div v-if="loading" class="rental-signing-state">{{ $t('rentalFiles.loadingProperties') }}</div>
        <div v-else-if="loadError" class="rental-signing-state error"><span>{{ $lt(loadError) }}</span><button type="button" @click="loadProperties">{{ $t('rentalFiles.retry') }}</button></div>
        <div v-else-if="!filteredProperties.length" class="rental-signing-state">{{ $t('rentalFiles.noProperties') }}</div>
        <div v-else class="rental-signing-property-list">
          <button v-for="property in filteredProperties" :key="property.rowKey" type="button" :class="{ active: selectedPropertyKey === property.rowKey }" @click="selectProperty(property)">
            <span class="rental-signing-property-icon">⌂</span><span><strong>{{ property.projectName || $t('rentalFiles.unsetProject') }}</strong><small>{{ property.unitNo || $t('rentalFiles.unsetUnit') }} · {{ property.ownerName || $t('rentalFiles.unsetOwner') }}</small></span><i>›</i>
          </button>
        </div>
      </aside>

      <main class="rental-signing-main">
        <div v-if="workspaceLoading" class="rental-signing-empty">{{ $t('rentalFiles.loadingFiles') }}</div>
        <div v-else-if="workspaceError" class="rental-signing-empty error"><strong>{{ $t('rentalFiles.loadFailed') }}</strong><span>{{ $lt(workspaceError) }}</span><button type="button" @click="loadSigningWorkspace">{{ $t('rentalFiles.retry') }}</button></div>
        <div v-else-if="!selectedProperty" class="rental-signing-empty"><strong>{{ $t('rentalFiles.chooseProperty') }}</strong><span>{{ $t('rentalFiles.choosePropertyHint') }}</span></div>
        <template v-else>
          <div class="rental-signing-property-head">
            <div><span>{{ $t('rentalFiles.currentProperty') }}</span><h3>{{ propertyTitle }}</h3><p>{{ selectedProperty.ownerName || $t('rentalFiles.unsetOwner') }} · {{ currentMandate?.mandateNo || $t('rentalFiles.noMandate') }}</p></div>
            <strong :class="{ historical: rentalHeaderStatusKey !== 'rentalFiles.currentRental' }">{{ $t(rentalHeaderStatusKey) }}</strong>
          </div>

          <section v-if="rentalCycles.length" class="rental-cycle-selector">
            <div><span>{{ $t('rentalFiles.rentalCycle') }}</span><strong>{{ $t(selectedCycleTitleKey) }}</strong><small>{{ $t(selectedCycleIsHistorical ? 'rentalFiles.historyCycleHint' : 'rentalFiles.currentCycleHint') }}</small></div>
            <select v-model="selectedCycleId" @change="selectCycle"><option v-for="cycle in rentalCycles" :key="cycle.id" :value="String(cycle.id)">{{ $lt(cycleOptionLabel(cycle)) }}</option></select>
          </section>
          <section v-if="cycleLeases.length" class="rental-cycle-selector rental-lease-selector">
            <div><span>{{ $t('rentalFiles.rentalSpaceAndLease') }}</span><strong>{{ currentLease?.rentalSpaceName || $t('rentalFiles.wholeProperty') }}</strong><small>{{ $lt(leaseStatusLabel(currentLease)) }} · {{ $t('rentalFiles.coRentalLeaseHint') }}</small></div>
            <select v-model="selectedLeaseId" :disabled="cycleLeases.length === 1" @change="selectLease"><option v-for="lease in cycleLeases" :key="lease.leaseId || lease.id" :value="String(lease.leaseId || lease.id)">{{ $lt(leaseOptionLabel(lease)) }}</option></select>
          </section>
          <div v-if="selectedCycleIsHistorical" class="rental-cycle-readonly">{{ $t('rentalFiles.historyReadOnly') }}</div>

          <section class="rental-signing-actions">
            <div class="rental-signing-section-head"><div><h4>{{ $t('rentalFiles.signingTasks') }}</h4><p>{{ $t('rentalFiles.signingTasksHint') }}</p></div></div>
            <div class="rental-signing-card-grid">
              <article v-for="task in signingTasks" :key="task.key" :class="['rental-signing-card', `is-${task.status}`, { highlighted: task.actions.includes(requestedAction) }]">
                <div class="rental-signing-card-head"><span>{{ task.icon }}</span><div><strong>{{ $lt(task.title) }}</strong><small>{{ $lt(task.description) }}</small></div><em>{{ $lt(task.statusLabel) }}</em></div>
                <dl><div><dt>{{ $t('rentalFiles.document') }}</dt><dd>{{ task.fileName || '—' }}</dd></div><div><dt>{{ $t('rentalFiles.latestStatus') }}</dt><dd>{{ task.detail }}</dd></div></dl>
                <div class="rental-signing-card-actions">
                  <button v-if="task.action" type="button" :disabled="task.disabled || actionBusy" @click="runSigningTask(task)">{{ task.actionLabel }}</button>
                   <button v-if="task.canRegenerate" type="button" class="secondary" :disabled="actionBusy" @click="editTask(task)">{{ $t('legacy.t_644d5f06bdba') }}</button>
                   <button v-if="task.canRegenerate" type="button" class="secondary" :disabled="actionBusy" @click="regenerateTask(task)">{{ $t('rentalFiles.regenerateFile') }}</button>
                   <button v-if="task.templateType && task.canRegenerate" type="button" class="secondary" :disabled="actionBusy" @click="updateClausesTask(task)">{{ $t('legacy.t_1c6495422e78') }}</button>
                   <template v-if="task.templateType">
                    <input :ref="`template-${task.templateType}`" class="rental-template-input" type="file" accept="application/pdf" @change="replaceTemplate(task.templateType, $event)">
                    <button type="button" class="secondary" :disabled="actionBusy" @click="chooseTemplate(task.templateType)">{{ $t('rentalFiles.replaceTemplate') }}</button>
                    <button type="button" class="secondary" :disabled="actionBusy" @click="openTemplateDesigner(task.templateType)">{{ $t('rentalFiles.positionTemplateFields') }}</button>
                    <small>{{ $t('rentalFiles.templateVersion') }} {{ templateVersions[task.templateType]?.version || 'v1' }}</small>
                  </template>
                </div>
              </article>
            </div>
          </section>

          <section v-if="signingPanel" class="rental-signing-panel">
            <header><div><span>{{ $t('rentalFiles.signingPanelKicker') }}</span><h4>{{ signingPanelTitle }}</h4><p>{{ signingPanelHint }}</p></div><button type="button" @click="closeSigningPanel">×</button></header>
            <div v-if="signingPanel.endsWith('Status')" class="rental-signing-status-detail">
              <div><span>{{ $t('rentalFiles.signerName') }}</span><strong>{{ activeSigning.signerName || '—' }}</strong></div>
              <div><span>{{ $t('rentalFiles.signerEmail') }}</span><strong>{{ activeSigning.signerEmail || '—' }}</strong></div>
              <div><span>{{ $t('rentalFiles.requestedAt') }}</span><strong>{{ formatDate(activeSigning.requestedAt) }}</strong></div>
              <div><span>{{ $t('rentalFiles.expiresAt') }}</span><strong>{{ formatDate(activeSigning.expiresAt) }}</strong></div>
            </div>
            <div v-else-if="generatedSigningLinks.length" ref="signingLinkResults" class="rental-signing-link-results" tabindex="-1">
              <div class="rental-signing-link-summary"><span>{{ $t('legacy.t_356a192b7913') }}</span><div><strong>{{ $t('rentalFiles.signingLinksReady') }}</strong><small>{{ $t('rentalFiles.signingLinkDirectHint') }}</small></div></div>
              <article v-for="link in generatedSigningLinks" :key="link.requestId || link.signingUrl">
                <div class="rental-signing-link-person"><strong>{{ $lt(signerRoleLabel(link.signerRole)) }} · {{ link.signerName }}</strong><small>{{ $t('rentalFiles.emailOnlyWhenSending') }}</small></div>
                <div class="rental-signing-link-share">
                  <label><span>{{ $t('rentalFiles.signingLink') }}</span><input :value="link.signingUrl" readonly @focus="$event.target.select()"></label>
                  <div class="rental-signing-link-actions">
                    <button type="button" @click="copySigningLink(link.signingUrl)">{{ $t('rentalFiles.copyLink') }}</button>
                    <button type="button" :disabled="Boolean(signingWhatsAppBusyId) || Boolean(link.whatsappStatus && link.whatsappStatus !== 'ready')" :aria-busy="signingWhatsAppBusyId === link.requestId" @click="sendSigningWhatsApp(link)">{{ $t(signingWhatsAppBusyId === link.requestId ? 'rentalFiles.sendingEmail' : 'rentalFiles.sendWhatsAppDirect') }}</button>
                    <a :href="link.signingUrl" target="_blank" rel="noopener noreferrer">{{ $t('rentalFiles.openSigningLink') }}</a>
                    <AdminOwnerSignatureDispatch v-if="['owner', 'second_owner'].includes(link.signerRole)" :request-id="link.requestId" />
                  </div>
                  <div v-if="link.whatsappNeedsPhone">
                    <label><span>{{ $t('rentalFiles.whatsappCountryCode') }}</span><select v-model="link.deliveryCountry" :disabled="Boolean(signingWhatsAppBusyId)"><option value="">{{ $t('rentalFiles.whatsappChooseCountry') }}</option><option v-for="country in phoneCountries" :key="country.code" :value="country.code">{{ country.dialCode }} · {{ country.label }}</option></select></label>
                    <label><span>{{ $t('rentalFiles.whatsappRecipientPhone') }} · {{ link.signerName }}</span><input v-model.trim="link.deliveryPhone" type="tel" maxlength="40" autocomplete="tel" placeholder="+60123456789" :disabled="Boolean(signingWhatsAppBusyId)"></label>
                  </div>
                  <p v-if="link.whatsappStatus && link.whatsappStatus !== 'ready'" role="status">{{ $t(['delivered', 'read'].includes(link.whatsappStatus) ? 'rentalFiles.whatsappDelivered' : link.whatsappStatus === 'sent' ? 'rentalFiles.whatsappSubmitted' : 'rentalFiles.whatsappCheckResult') }}</p>
                </div>
                <div class="rental-signing-link-email">
                  <label><span>{{ $t('rentalFiles.deliveryEmail') }}</span><input v-model.trim="link.deliveryEmail" type="email" maxlength="190" :placeholder="$t('rentalFiles.deliveryEmailHint')" :disabled="signingEmailBusyId === link.requestId"></label>
                  <button type="button" :disabled="signingEmailBusyId === link.requestId" :aria-busy="signingEmailBusyId === link.requestId" :data-state="signingEmailSentIds.includes(link.requestId) ? 'success' : signingEmailBusyId === link.requestId ? 'loading' : 'default'" @click="sendSigningEmail(link)">{{ signingEmailSentIds.includes(link.requestId) ? $t('rentalFiles.emailSent') : signingEmailBusyId === link.requestId ? $t('rentalFiles.sendingEmail') : $t('rentalFiles.sendByEmail') }}</button>
                </div>
              </article>
              <p v-if="signingDeliveryError" class="rental-signing-delivery-error" role="alert">{{ $lt(signingDeliveryError) }}</p>
              <footer><button type="button" class="primary" @click="closeSigningPanel">{{ $t('rentalFiles.done') }}</button></footer>
            </div>
            <div v-else class="rental-signing-direct-state" :class="{ error: actionError }">
              <strong>{{ actionError ? $t('rentalFiles.actionFailed') : $t('rentalFiles.processing') }}</strong>
              <span>{{ actionError || $t('rentalFiles.signingLinkDirectHint') }}</span>
              <button v-if="actionError" type="button" :disabled="actionBusy" @click="submitSigning">{{ $t('rentalFiles.retry') }}</button>
            </div>
          </section>

          <section class="rental-generated-files">
            <span ref="generatedFilesSection" class="rental-generated-files-anchor" tabindex="-1"></span>
            <div class="rental-signing-section-head"><div><h4>{{ $t('rentalFiles.generatedFiles') }}</h4><p>{{ $t('rentalFiles.generatedFilesHint') }}</p></div><span>{{ filteredFiles.length }} {{ $t('rentalFiles.files') }}</span></div>
            <div class="rental-files-toolbar"><input v-model.trim="fileSearch" type="search" :placeholder="$t('rentalFiles.fileSearch')"><select v-model="categoryFilter"><option value="">{{ $t('rentalFiles.allCategories') }}</option><option v-for="category in categories" :key="category" :value="category">{{ $lt(categoryLabel(category)) }}</option></select></div>
            <div v-if="filteredFiles.length" class="rental-files-table-wrap">
              <table class="rental-files-table"><thead><tr><th>{{ $t('rentalFiles.fileName') }}</th><th>{{ $t('rentalFiles.category') }}</th><th>{{ $t('rentalFiles.rentalNo') }}</th><th>{{ $t('rentalFiles.status') }}</th><th>{{ $t('rentalFiles.createdAt') }}</th><th>{{ $t('rentalFiles.action') }}</th></tr></thead><tbody>
                <tr v-for="file in filteredFiles" :key="file.key"><td><div class="rental-file-name"><strong :title="file.name">{{ file.name }}</strong><small>{{ file.reference || '—' }}</small></div></td><td><span class="rental-file-category">{{ $lt(categoryLabel(file.category)) }}</span></td><td><span class="rental-file-mandate">{{ file.mandateNo || '—' }}</span></td><td><span class="rental-file-status" :class="fileStatusClass(file)">{{ $lt(fileStatusLabel(file)) }}</span></td><td><time>{{ formatDate(file.date) }}</time></td><td><button type="button" class="rental-file-download" :disabled="downloadingKey === file.key" @click="downloadFile(file)">{{ downloadingKey === file.key ? $t('rentalFiles.downloading') : $t('rentalFiles.download') }}</button></td></tr>
              </tbody></table>
            </div>
            <div v-else class="rental-signing-empty compact">{{ $t('rentalFiles.noFiles') }}</div>
            <p v-if="actionError && !signingPanel" class="rental-signing-inline-error">{{ $lt(actionError) }}</p>
          </section>

          <section v-if="propertyFiles.length" class="rental-property-files">
            <div class="rental-signing-section-head"><div><h4>{{ $t('rentalFiles.propertyFiles') }}</h4><p>{{ $t('rentalFiles.propertyFilesHint') }}</p></div><span>{{ propertyFiles.length }} {{ $t('rentalFiles.files') }}</span></div>
            <div class="rental-property-file-list"><article v-for="file in propertyFiles" :key="file.key"><div><strong :title="file.name">{{ file.name }}</strong><small>{{ file.reference || '—' }}</small></div><span>{{ formatDate(file.date) }}</span><button type="button" class="rental-file-download" :disabled="downloadingKey === file.key" @click="downloadFile(file)">{{ downloadingKey === file.key ? $t('rentalFiles.downloading') : $t('rentalFiles.download') }}</button></article></div>
          </section>
        </template>
      </main>
    </div>

    <div v-if="otrFormOpen" class="pma-form-backdrop" @pointerdown.self="closeOtrForm">
      <form ref="otrFormRef" class="pma-form-dialog rental-appointment-form-dialog" :class="{ 'is-validated': validatedSupplementForms.otr }" novalidate @submit.prevent="generateOtr">
        <header>
          <div><span>{{ $t('rentalFiles.supplement.dialogs.otr.kicker') }}</span><h3>{{ $t('rentalFiles.supplement.dialogs.otr.title') }}</h3><p>{{ $t('rentalFiles.supplement.dialogs.otr.hint') }}</p></div>
          <button type="button" :aria-label="$t('rentalFiles.supplement.common.close')" @click="closeOtrForm">×</button>
        </header>
        <div class="pma-form-body">
          <div v-if="otrMissingFields.length" class="rental-appointment-missing">
            <strong>{{ $t('rentalFiles.supplement.common.missingCount', { count: otrMissingFields.length }) }}</strong>
            <span>{{ otrMissingFields.join($t('rentalFiles.supplement.common.fieldSeparator')) }}</span>
          </div>
          <section><h4>{{ $t('rentalFiles.supplement.sections.propertyLease') }}</h4><div class="pma-form-grid">
            <label>{{ $t('rentalFiles.supplement.fields.caseNo') }}<input v-model.trim="otrForm.caseNo" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.propertyAddress') }}<input v-model.trim="otrForm.propertyAddress" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.periodYears') }}<input v-model.trim="otrForm.periodYears" inputmode="decimal" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.renewalYears') }}<input v-model.trim="otrForm.renewalYears" inputmode="decimal" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.commencementDate') }}<input v-model="otrForm.commencementDate" type="date" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.otherConditions') }}<input v-model.trim="otrForm.otherConditions" required></label>
          </div></section>
          <section><h4>{{ $t('rentalFiles.supplement.sections.amountsDeposits') }}</h4><div class="pma-form-grid">
            <label>{{ $t('rentalFiles.supplement.fields.advanceRental') }}<input v-model.trim="otrForm.advanceRental" inputmode="decimal" required @input="updateOtrTotal"></label>
            <label>{{ $t('rentalFiles.supplement.fields.securityDepositMonths') }}<input v-model.trim="otrForm.securityDepositMonths" inputmode="decimal" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.securityDeposit') }}<input v-model.trim="otrForm.securityDeposit" inputmode="decimal" required @input="updateOtrTotal"></label>
            <label>{{ $t('rentalFiles.supplement.fields.utilityDepositMonths') }}<input v-model.trim="otrForm.utilityDepositMonths" inputmode="decimal" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.utilityDeposit') }}<input v-model.trim="otrForm.utilityDeposit" inputmode="decimal" required @input="updateOtrTotal"></label>
            <label>{{ $t('rentalFiles.supplement.fields.stampingFee') }}<input v-model.trim="otrForm.stampingFee" inputmode="decimal" required @input="updateOtrTotal"></label>
            <label>{{ $t('rentalFiles.supplement.fields.totalBeforeKeys') }}<input v-model.trim="otrForm.totalBeforeKeys" inputmode="decimal" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.earnestDeposit') }}<input v-model.trim="otrForm.earnestDeposit" inputmode="decimal" required></label>
          </div></section>
          <section><h4>{{ $t('rentalFiles.supplement.sections.parties') }}</h4><div class="pma-form-grid">
            <label>{{ $t('rentalFiles.supplement.fields.tenantName') }}<input v-model.trim="otrForm.tenantName" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.tenantIdentity') }}<input v-model.trim="otrForm.tenantIdentity" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.tenantDate') }}<input v-model="otrForm.tenantDate" type="date" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.landlordName') }}<input v-model.trim="otrForm.landlordName" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.landlordIdentity') }}<input v-model.trim="otrForm.landlordIdentity" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.landlordDate') }}<input v-model="otrForm.landlordDate" type="date" required></label>
          </div></section>
          <section><h4>{{ $t('rentalFiles.supplement.sections.twoWitnesses') }}</h4><div class="pma-form-grid">
            <label>{{ $t('rentalFiles.supplement.fields.tenantWitnessName') }}<input v-model.trim="otrForm.tenantWitnessName" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.tenantWitnessIdentity') }}<input v-model.trim="otrForm.tenantWitnessIdentity" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.tenantWitnessDate') }}<input v-model="otrForm.tenantWitnessDate" type="date" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.ownerWitnessName') }}<input v-model.trim="otrForm.landlordWitnessName" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.ownerWitnessIdentity') }}<input v-model.trim="otrForm.landlordWitnessIdentity" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.ownerWitnessDate') }}<input v-model="otrForm.landlordWitnessDate" type="date" required></label>
          </div></section>
          <section><h4>{{ $t('rentalFiles.supplement.sections.propertyMandate') }}</h4><div class="pma-form-grid">
            <label class="wide">{{ $t('rentalFiles.supplement.fields.depositWords') }}<input v-model.trim="otrForm.earnestDepositWords" :placeholder="$t('rentalFiles.supplement.placeholders.depositWords')" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.appointmentStart') }}<input v-model="otrForm.startDate" type="date" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.appointmentEnd') }}<input v-model="otrForm.endDate" type="date" required></label>
          </div></section>
          <section><h4>{{ $t('rentalFiles.supplement.sections.commissionTax') }}</h4><div class="pma-form-grid">
            <label>{{ $t('rentalFiles.supplement.fields.commissionWords') }}<input v-model.trim="otrForm.commissionWords" :placeholder="$t('rentalFiles.supplement.placeholders.commissionWords')" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.commissionMonths') }}<input v-model.trim="otrForm.commissionMonths" inputmode="decimal" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.sstPercent') }}<input v-model.trim="otrForm.sstPercent" inputmode="decimal" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.commissionAmount') }}<input v-model.trim="otrForm.commissionAmount" inputmode="decimal" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.agencyFeeTotal') }}<input v-model.trim="otrForm.agencyFeeTotal" inputmode="decimal" required></label>
          </div></section>
          <section><h4>{{ $t('rentalFiles.supplement.sections.firstOwner') }}</h4><div class="pma-form-grid">
            <label class="wide">{{ $t('rentalFiles.supplement.fields.address') }}<textarea v-model.trim="otrForm.landlordAddress" rows="2" required></textarea></label>
          </div>
            <label class="pma-bank-sync"><input v-model="otrForm.hasSecondLandlord" type="checkbox"><span><strong>{{ $t('rentalFiles.supplement.fields.hasSecondOwner') }}</strong><small>{{ $t('rentalFiles.supplement.fields.hasSecondOwnerHint') }}</small></span></label>
            <div v-if="otrForm.hasSecondLandlord" class="pma-form-grid rental-appointment-secondary-owner">
              <label>{{ $t('rentalFiles.supplement.fields.secondOwnerName') }}<input v-model.trim="otrForm.secondLandlordName" required></label>
              <label>{{ $t('rentalFiles.supplement.fields.identityPassport') }}<input v-model.trim="otrForm.secondLandlordIdentity" required></label>
              <label class="wide">{{ $t('rentalFiles.supplement.fields.address') }}<textarea v-model.trim="otrForm.secondLandlordAddress" rows="2" required></textarea></label>
              <label>{{ $t('rentalFiles.supplement.fields.signingDate') }}<input v-model="otrForm.secondLandlordDate" type="date" required></label>
            </div>
          </section>
          <section><h4>{{ $t('rentalFiles.supplement.sections.witness') }}</h4><div class="pma-form-grid">
            <label class="wide">{{ $t('rentalFiles.supplement.fields.address') }}<textarea v-model.trim="otrForm.witnessAddress" rows="2" required></textarea></label>
          </div></section>
        </div>
        <p v-if="actionError" class="pma-form-error">{{ $lt(actionError) }}</p>
        <footer><button type="button" class="secondary" @click="closeOtrForm">{{ $t('rentalFiles.supplement.common.cancel') }}</button><button type="submit" class="primary" :disabled="actionBusy">{{ actionBusy ? $t('rentalFiles.supplement.common.processing') : $t('rentalFiles.supplement.dialogs.otr.submit') }}</button></footer>
      </form>
    </div>

    <div v-if="rentalAppointmentFormOpen" class="pma-form-backdrop" @pointerdown.self="closeRentalAppointmentForm">
      <form ref="rentalAppointmentFormRef" class="pma-form-dialog rental-appointment-form-dialog" :class="{ 'is-validated': validatedSupplementForms.rentalAppointment }" novalidate @submit.prevent="generateRentalAppointment">
        <header>
          <div><span>{{ $t('rentalFiles.supplement.dialogs.appointment.kicker') }}</span><h3>{{ $t('rentalFiles.supplement.dialogs.appointment.title') }}</h3><p>{{ $t('rentalFiles.supplement.dialogs.appointment.hint') }}</p></div>
          <button type="button" :aria-label="$t('rentalFiles.supplement.common.close')" @click="closeRentalAppointmentForm">×</button>
        </header>
        <div class="pma-form-body">
          <div v-if="rentalAppointmentMissingFields.length" class="rental-appointment-missing">
            <strong>{{ $t('rentalFiles.supplement.common.missingCount', { count: rentalAppointmentMissingFields.length }) }}</strong>
            <span>{{ rentalAppointmentMissingFields.join($t('rentalFiles.supplement.common.fieldSeparator')) }}</span>
          </div>
          <section>
            <h4>{{ $t('rentalFiles.supplement.common.signerSection') }}</h4>
            <div class="pma-form-grid"><label class="wide">{{ $t('rentalFiles.supplement.common.ownerSigner') }}<input v-model.trim="rentalAppointmentForm.ownerSignerName" required></label></div>
            <p class="pma-profile-sync-hint">{{ $t('rentalFiles.supplement.common.signerHint') }}</p>
          </section>
          <section>
            <h4>{{ $t('rentalFiles.supplement.sections.propertyMandate') }}</h4>
            <div class="pma-form-grid">
              <label>{{ $t('rentalFiles.supplement.fields.appointmentNo') }}<input v-model.trim="rentalAppointmentForm.caseNo" required></label>
              <label>{{ $t('rentalFiles.supplement.fields.depositAmount') }}<input v-model.trim="rentalAppointmentForm.earnestDeposit" inputmode="decimal" required></label>
              <label class="wide">{{ $t('rentalFiles.supplement.fields.propertyAddress') }}<textarea v-model.trim="rentalAppointmentForm.propertyAddress" rows="2" required></textarea></label>
              <label class="wide">{{ $t('rentalFiles.supplement.fields.depositWords') }}<input v-model.trim="rentalAppointmentForm.earnestDepositWords" :placeholder="$t('rentalFiles.supplement.placeholders.depositWords')" required></label>
              <label>{{ $t('rentalFiles.supplement.fields.appointmentStart') }}<input v-model="rentalAppointmentForm.startDate" type="date" required></label>
              <label>{{ $t('rentalFiles.supplement.fields.appointmentEnd') }}<input v-model="rentalAppointmentForm.endDate" type="date" required></label>
            </div>
          </section>
          <section>
            <h4>{{ $t('rentalFiles.supplement.sections.commissionTax') }}</h4>
            <div class="pma-form-grid">
              <label>{{ $t('rentalFiles.supplement.fields.commissionWords') }}<input v-model.trim="rentalAppointmentForm.commissionWords" :placeholder="$t('rentalFiles.supplement.placeholders.commissionWords')" required></label>
              <label>{{ $t('rentalFiles.supplement.fields.commissionMonths') }}<input v-model.trim="rentalAppointmentForm.commissionMonths" inputmode="decimal" required></label>
              <label>{{ $t('rentalFiles.supplement.fields.sstPercent') }}<input v-model.trim="rentalAppointmentForm.sstPercent" inputmode="decimal" required></label>
              <label>{{ $t('rentalFiles.supplement.fields.commissionAmount') }}<input v-model.trim="rentalAppointmentForm.commissionAmount" inputmode="decimal" required></label>
              <label>{{ $t('rentalFiles.supplement.fields.agencyFeeTotal') }}<input v-model.trim="rentalAppointmentForm.agencyFeeTotal" inputmode="decimal" required></label>
            </div>
          </section>
          <section>
            <h4>{{ $t('rentalFiles.supplement.sections.firstOwner') }}</h4>
            <div class="pma-form-grid">
              <label>{{ $t('rentalFiles.supplement.fields.name') }}<input v-model.trim="rentalAppointmentForm.landlordName" required></label>
              <label>{{ $t('rentalFiles.supplement.fields.identityPassport') }}<input v-model.trim="rentalAppointmentForm.landlordIdentity" required></label>
              <label class="wide">{{ $t('rentalFiles.supplement.fields.address') }}<textarea v-model.trim="rentalAppointmentForm.landlordAddress" rows="2" required></textarea></label>
              <label>{{ $t('rentalFiles.supplement.fields.signingDate') }}<input v-model="rentalAppointmentForm.landlordDate" type="date" required></label>
            </div>
            <label class="pma-bank-sync"><input v-model="rentalAppointmentForm.hasSecondLandlord" type="checkbox"><span><strong>{{ $t('rentalFiles.supplement.fields.hasSecondOwner') }}</strong><small>{{ $t('rentalFiles.supplement.fields.hasSecondOwnerHint') }}</small></span></label>
            <div v-if="rentalAppointmentForm.hasSecondLandlord" class="pma-form-grid rental-appointment-secondary-owner">
              <label>{{ $t('rentalFiles.supplement.fields.secondOwnerName') }}<input v-model.trim="rentalAppointmentForm.secondLandlordName" required></label>
              <label>{{ $t('rentalFiles.supplement.fields.identityPassport') }}<input v-model.trim="rentalAppointmentForm.secondLandlordIdentity" required></label>
              <label class="wide">{{ $t('rentalFiles.supplement.fields.address') }}<textarea v-model.trim="rentalAppointmentForm.secondLandlordAddress" rows="2" required></textarea></label>
              <label>{{ $t('rentalFiles.supplement.fields.signingDate') }}<input v-model="rentalAppointmentForm.secondLandlordDate" type="date" required></label>
            </div>
          </section>
          <section>
            <h4>{{ $t('rentalFiles.supplement.sections.witness') }}</h4>
            <div class="pma-form-grid">
              <label>{{ $t('rentalFiles.supplement.fields.name') }}<input v-model.trim="rentalAppointmentForm.witnessName" required></label>
              <label>{{ $t('rentalFiles.supplement.fields.identityPassport') }}<input v-model.trim="rentalAppointmentForm.witnessIdentity" required></label>
              <label class="wide">{{ $t('rentalFiles.supplement.fields.address') }}<textarea v-model.trim="rentalAppointmentForm.witnessAddress" rows="2" required></textarea></label>
              <label>{{ $t('rentalFiles.supplement.fields.witnessDate') }}<input v-model="rentalAppointmentForm.witnessDate" type="date" required></label>
            </div>
          </section>
        </div>
        <p v-if="actionError" class="pma-form-error">{{ $lt(actionError) }}</p>
        <footer><button type="button" class="secondary" @click="closeRentalAppointmentForm">{{ $t('rentalFiles.supplement.common.cancel') }}</button><button type="submit" class="primary" :disabled="actionBusy">{{ actionBusy ? $t('rentalFiles.supplement.common.processing') : $t('rentalFiles.supplement.common.saveGenerate') }}</button></footer>
      </form>
    </div>

    <div v-if="authorizationFormOpen" class="pma-form-backdrop" @pointerdown.self="closeAuthorizationForm">
      <form ref="authorizationFormRef" class="pma-form-dialog authorization-form-dialog" :class="{ 'is-validated': validatedSupplementForms.authorization }" novalidate @submit.prevent="generateAuthorization">
        <header>
          <div><span>{{ $t('rentalFiles.supplement.dialogs.authorization.kicker') }}</span><h3>{{ $t('rentalFiles.supplement.dialogs.authorization.title') }}</h3><p>{{ $t('rentalFiles.supplement.dialogs.authorization.hint') }}</p></div>
          <button type="button" class="pma-form-close" :aria-label="$t('rentalFiles.supplement.common.close')" @click="closeAuthorizationForm">×</button>
        </header>
        <div class="pma-form-body">
          <section>
            <h4>{{ $t('rentalFiles.supplement.common.signerSection') }}</h4>
            <div class="pma-form-grid">
              <label class="wide">{{ $t('rentalFiles.supplement.common.ownerSigner') }}<input v-model.trim="authorizationForm.ownerSignerName" required></label>
            </div>
            <p class="pma-profile-sync-hint">{{ $t('rentalFiles.supplement.common.signerHint') }}</p>
          </section>
          <section>
            <h4>{{ $t('rentalFiles.supplement.sections.property') }}</h4>
            <div class="pma-form-grid">
              <label>{{ $t('rentalFiles.supplement.fields.projectName') }}<input v-model.trim="authorizationForm.projectName" required></label>
              <label>{{ $t('rentalFiles.supplement.fields.unitNo') }}<input v-model.trim="authorizationForm.unitNo" required></label>
            </div>
          </section>
          <section>
            <h4>{{ $t('rentalFiles.supplement.sections.ownerDates') }}</h4>
            <div class="pma-form-grid">
              <label>{{ $t('rentalFiles.supplement.fields.landlordName') }}<input v-model.trim="authorizationForm.landlordName" required></label>
              <label>{{ $t('rentalFiles.supplement.fields.passportNo') }}<input v-model.trim="authorizationForm.landlordIdentity" required></label>
              <label class="wide">{{ $t('rentalFiles.supplement.fields.ownerEmail') }}<input v-model.trim="authorizationForm.ownerEmail" required></label>
              <label>{{ $t('rentalFiles.supplement.fields.authorizationDate') }}<input v-model="authorizationForm.agreementDate" type="date" required></label>
            </div>
          </section>
        </div>
        <p v-if="authorizationMissingFields.length" class="pma-form-error">{{ $t('rentalFiles.supplement.common.missingBeforeGenerate', { fields: authorizationMissingFields.join($t('rentalFiles.supplement.common.fieldSeparator')) }) }}</p>
        <p v-else-if="actionError" class="pma-form-error">{{ $lt(actionError) }}</p>
        <footer><button type="button" class="secondary" @click="closeAuthorizationForm">{{ $t('rentalFiles.supplement.common.cancel') }}</button><button type="submit" class="primary" :disabled="actionBusy">{{ actionBusy ? $t('rentalFiles.supplement.common.processing') : $t('rentalFiles.supplement.common.confirmGenerate') }}</button></footer>
      </form>
    </div>

    <div v-if="terminationLetterFormOpen" class="pma-form-backdrop" @pointerdown.self="closeTerminationLetterForm">
      <form ref="terminationLetterFormRef" class="pma-form-dialog termination-letter-form-dialog" :class="{ 'is-validated': validatedSupplementForms.terminationLetter }" novalidate @submit.prevent="generateTerminationLetter">
        <header>
          <div><span>{{ $t('rentalFiles.supplement.dialogs.termination.kicker') }}</span><h3>{{ $t('rentalFiles.supplement.dialogs.termination.title') }}</h3><p>{{ $t('rentalFiles.supplement.dialogs.termination.hint') }}</p></div>
          <button type="button" class="pma-form-close" :aria-label="$t('rentalFiles.supplement.common.close')" @click="closeTerminationLetterForm">×</button>
        </header>
        <div class="pma-form-body">
          <div v-if="terminationLetterMissingFields.length" class="rental-appointment-missing"><strong>{{ $t('rentalFiles.supplement.common.missingCount', { count: terminationLetterMissingFields.length }) }}</strong><span>{{ terminationLetterMissingFields.join($t('rentalFiles.supplement.common.fieldSeparator')) }}</span></div>
          <section><h4>{{ $t('rentalFiles.supplement.common.signerSection') }}</h4><div class="pma-form-grid">
            <label>{{ $t('rentalFiles.supplement.common.ownerSigner') }}<input v-model.trim="terminationLetterForm.ownerSignerName" required></label>
            <label>{{ $t('rentalFiles.pmaCompanySigner') }}<input v-model.trim="terminationLetterForm.companySignerName" required></label>
          </div><p class="pma-profile-sync-hint">{{ $t('rentalFiles.supplement.common.signerHint') }}</p></section>
          <section><h4>{{ $t('rentalFiles.supplement.sections.letterProperty') }}</h4><div class="pma-form-grid">
            <label>{{ $t('rentalFiles.supplement.fields.letterDate') }}<input v-model="terminationLetterForm.agreementDate" type="date" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.ownerName') }}<input v-model.trim="terminationLetterForm.landlordName" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.landlordIdentity') }}<input v-model.trim="terminationLetterForm.landlordIdentity" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.projectName') }}<input v-model.trim="terminationLetterForm.projectName" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.unitNo') }}<input v-model.trim="terminationLetterForm.unitNo" required></label>
          </div></section>
          <section><h4>{{ $t('rentalFiles.supplement.sections.agent') }}</h4><div class="pma-form-grid">
            <label>{{ $t('rentalFiles.supplement.fields.agentName') }}<input v-model.trim="terminationLetterForm.authorizedAgentName" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.agentIdentity') }}<input v-model.trim="terminationLetterForm.authorizedAgentIdentity" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.agentPhone') }}<input v-model.trim="terminationLetterForm.authorizedAgentPhone" inputmode="tel" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.agentEmail') }}<input v-model.trim="terminationLetterForm.authorizedAgentEmail" required></label>
          </div></section>
          <section><h4>{{ $t('rentalFiles.supplement.sections.ownerBank') }}</h4><div class="pma-form-grid">
            <label>{{ $t('rentalFiles.supplement.fields.bankName') }}<input v-model.trim="terminationLetterForm.bankName" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.payeeName') }}<input v-model.trim="terminationLetterForm.bankPayeeName" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.bankAccount') }}<input v-model.trim="terminationLetterForm.bankAccountNo" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.swiftCode') }}<input v-model.trim="terminationLetterForm.bankSwiftCode" required></label>
            <label class="wide">{{ $t('rentalFiles.supplement.fields.bankAddress') }}<textarea v-model.trim="terminationLetterForm.bankAddress" rows="2" required></textarea></label>
          </div></section>
        </div>
        <p v-if="actionError" class="pma-form-error">{{ $lt(actionError) }}</p>
        <footer><button type="button" class="secondary" @click="closeTerminationLetterForm">{{ $t('rentalFiles.supplement.common.cancel') }}</button><button type="submit" class="primary" :disabled="actionBusy">{{ actionBusy ? $t('rentalFiles.supplement.common.processing') : $t('rentalFiles.supplement.common.confirmGenerate') }}</button></footer>
      </form>
    </div>

    <div v-if="rentalRemittanceFormOpen" class="pma-form-backdrop" @pointerdown.self="closeRentalRemittanceForm">
      <form ref="rentalRemittanceFormRef" class="pma-form-dialog rental-appointment-form-dialog" :class="{ 'is-validated': validatedSupplementForms.rentalRemittance }" novalidate @submit.prevent="generateRentalRemittance">
        <header>
          <div><span>{{ $t('rentalFiles.supplement.dialogs.remittance.kicker') }}</span><h3>{{ $t('rentalFiles.supplement.dialogs.remittance.title') }}</h3><p>{{ $t('rentalFiles.supplement.dialogs.remittance.hint') }}</p></div>
          <button type="button" class="pma-form-close" :aria-label="$t('rentalFiles.supplement.common.close')" @click="closeRentalRemittanceForm">×</button>
        </header>
        <div class="pma-form-body">
          <div v-if="rentalRemittanceMissingFields.length" class="rental-appointment-missing"><strong>{{ $t('rentalFiles.supplement.common.missingCount', { count: rentalRemittanceMissingFields.length }) }}</strong><span>{{ rentalRemittanceMissingFields.join($t('rentalFiles.supplement.common.fieldSeparator')) }}</span></div>
          <section><h4>{{ $t('rentalFiles.supplement.common.signerSection') }}</h4><div class="pma-form-grid"><label class="wide">{{ $t('rentalFiles.supplement.common.ownerSigner') }}<input v-model.trim="rentalRemittanceForm.ownerSignerName" required></label></div><p class="pma-profile-sync-hint">{{ $t('rentalFiles.supplement.common.signerHint') }}</p></section>
          <section><h4>{{ $t('rentalFiles.supplement.sections.letterOwner') }}</h4><div class="pma-form-grid">
            <label>{{ $t('rentalFiles.supplement.fields.letterDate') }}<input v-model="rentalRemittanceForm.agreementDate" type="date" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.ownerName') }}<input v-model.trim="rentalRemittanceForm.landlordName" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.passportNo') }}<input v-model.trim="rentalRemittanceForm.landlordIdentity" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.unitNo') }}<input v-model.trim="rentalRemittanceForm.unitNo" required></label>
          </div></section>
          <section><h4>{{ $t('rentalFiles.supplement.sections.receivingBank') }}</h4><div class="pma-form-grid">
            <label class="wide">{{ $t('rentalFiles.supplement.common.ownerBankAccount') }}<select v-model="rentalRemittanceForm.bankAccountId" @change="applyRentalRemittanceBankAccount"><option value="">{{ $t('rentalFiles.supplement.common.selectOwnerBankAccount') }}</option><option v-for="account in pmaBankAccounts" :key="account.id" :value="String(account.id)">{{ account.itemName }} · {{ account.paymentName }} · {{ account.accountNo }}</option></select></label>
            <label>{{ $t('rentalFiles.supplement.fields.authorizedPayee') }}<input v-model.trim="rentalRemittanceForm.bankPayeeName" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.bankName') }}<input v-model.trim="rentalRemittanceForm.bankName" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.bankAccount') }}<input v-model.trim="rentalRemittanceForm.bankAccountNo" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.swiftCode') }}<input v-model.trim="rentalRemittanceForm.bankSwiftCode" required></label>
            <label>{{ $t('rentalFiles.supplement.fields.branchCode') }}<input v-model.trim="rentalRemittanceForm.bankBranchCode" required></label>
            <label class="wide">{{ $t('rentalFiles.supplement.fields.bankAddress') }}<textarea v-model.trim="rentalRemittanceForm.bankAddress" rows="2" required></textarea></label>
          </div></section>
        </div>
        <p v-if="actionError" class="pma-form-error">{{ $lt(actionError) }}</p>
        <footer><button type="button" class="secondary" @click="closeRentalRemittanceForm">{{ $t('rentalFiles.supplement.common.cancel') }}</button><button type="submit" class="primary" :disabled="actionBusy">{{ actionBusy ? $t('rentalFiles.supplement.common.processing') : $t('rentalFiles.supplement.common.confirmGenerate') }}</button></footer>
      </form>
    </div>

    <div v-if="pmaFormOpen" class="pma-form-backdrop" @pointerdown.self="closePmaForm">
      <form ref="pmaFormRef" class="pma-form-dialog" :class="{ 'is-validated': validatedSupplementForms.pma }" novalidate @submit.prevent="generatePma">
        <header>
          <div><span>{{ $t('rentalFiles.pmaFormKicker') }}</span><h3>{{ $t('rentalFiles.pmaFormTitle') }}</h3><p>{{ $t('rentalFiles.pmaFormHint') }}</p></div>
          <button type="button" :aria-label="$t('rentalFiles.cancel')" @click="closePmaForm">×</button>
        </header>
        <div class="pma-form-body">
          <section>
            <h4>{{ $t('rentalFiles.pmaSignerSection') }}</h4>
            <div class="pma-form-grid">
              <label>{{ $t('rentalFiles.pmaOwnerSigner') }}<input v-model.trim="pmaForm.ownerSignerName" required></label>
              <label>{{ $t('rentalFiles.pmaCompanySigner') }}<input v-model.trim="pmaForm.companySignerName" required></label>
              <label class="wide">{{ $t('rentalFiles.pmaCustomerServiceSigner') }}<input v-model.trim="pmaForm.customerServiceSignerName" required></label>
            </div>
            <p class="pma-profile-sync-hint">{{ $t('rentalFiles.pmaSignerHint') }}</p>
          </section>
          <section>
            <h4>{{ $t('rentalFiles.pmaOwnerSection') }}</h4>
            <div class="pma-form-grid">
              <label>{{ $t('rentalFiles.pmaOwnerName') }}<input v-model.trim="pmaForm.landlordName" required></label>
              <label>{{ $t('rentalFiles.pmaOwnerIdentity') }}<input v-model.trim="pmaForm.landlordIdentity" required></label>
              <label class="wide">{{ $t('rentalFiles.pmaPropertyAddress') }}<textarea v-model.trim="pmaForm.propertyAddress" rows="2" required></textarea></label>
              <label class="wide">{{ $t('rentalFiles.pmaOwnerAddress') }}<textarea v-model.trim="pmaForm.ownerAddress" rows="2" required></textarea></label>
              <label>{{ $t('rentalFiles.pmaOwnerEmail') }}<input v-model.trim="pmaForm.ownerEmail" required></label>
              <label>{{ $t('rentalFiles.pmaOwnerPhone') }}<input v-model.trim="pmaForm.ownerPhone" inputmode="tel" required></label>
            </div>
          </section>
          <section>
            <h4>{{ $t('rentalFiles.pmaAgreementSection') }}</h4>
            <div class="pma-form-grid dates">
              <label>{{ $t('rentalFiles.pmaAgreementDate') }}<input v-model="pmaForm.agreementDate" type="date" required></label>
              <label>{{ $t('rentalFiles.pmaStartDate') }}<input v-model="pmaForm.startDate" type="date" required></label>
              <label>{{ $t('rentalFiles.pmaEndDate') }}<input v-model="pmaForm.endDate" type="date" required></label>
            </div>
          </section>
          <section>
            <h4>{{ $t('rentalFiles.pmaBankSection') }}</h4>
            <div class="pma-form-grid">
              <label class="wide">{{ $t('rentalFiles.supplement.common.ownerBankAccount') }}<select v-model="pmaForm.bankAccountId" @change="applyPmaBankAccount"><option value="">{{ $t('rentalFiles.supplement.common.selectOwnerBankAccount') }}</option><option v-for="account in pmaBankAccounts" :key="account.id" :value="String(account.id)">{{ account.itemName }} · {{ account.paymentName }} · {{ account.accountNo }}</option></select></label>
              <label>{{ $t('rentalFiles.pmaPayeeName') }}<input v-model.trim="pmaForm.bankPayeeName" required></label>
              <label>{{ $t('rentalFiles.pmaBankName') }}<input v-model.trim="pmaForm.bankName" required></label>
              <label class="wide">{{ $t('rentalFiles.pmaBankAddress') }}<textarea v-model.trim="pmaForm.bankAddress" rows="2" required></textarea></label>
              <label>{{ $t('rentalFiles.pmaBranchCode') }}<input v-model.trim="pmaForm.bankBranchCode" required></label>
              <label>{{ $t('rentalFiles.pmaAccountNo') }}<input v-model.trim="pmaForm.bankAccountNo" required></label>
              <label>{{ $t('rentalFiles.pmaSwiftCode') }}<input v-model.trim="pmaForm.bankSwiftCode" required></label>
            </div>
            <p class="pma-profile-sync-hint">{{ $t('rentalFiles.pmaProfileSyncHint') }}</p>
          </section>
        </div>
        <p v-if="pmaMissingFields.length" class="pma-form-error">{{ $t('rentalFiles.supplement.common.missingBeforeGenerate', { fields: pmaMissingFields.join($t('rentalFiles.supplement.common.fieldSeparator')) }) }}</p>
        <p v-else-if="actionError" class="pma-form-error">{{ $lt(actionError) }}</p>
        <footer><button type="button" class="secondary" @click="closePmaForm">{{ $t('rentalFiles.cancel') }}</button><button type="submit" class="primary" :disabled="actionBusy">{{ actionBusy ? $t('rentalFiles.processing') : $t('rentalFiles.confirmGeneratePma') }}</button></footer>
      </form>
    </div>

    <div v-if="leaseFormOpen" class="pma-form-backdrop" @pointerdown.self="closeLeaseForm">
      <form ref="leaseFormRef" class="pma-form-dialog rental-appointment-form-dialog" :class="{ 'is-validated': validatedSupplementForms.lease }" novalidate @submit.prevent="generateLeaseDraft">
        <header>
          <div><span>{{ $t('rentalFiles.supplement.dialogs.lease.kicker') }}</span><h3>{{ $t('rentalFiles.supplement.dialogs.lease.title') }}</h3><p>{{ $t('rentalFiles.supplement.dialogs.lease.hint') }}</p></div>
          <button type="button" :aria-label="$t('rentalFiles.supplement.common.close')" @click="closeLeaseForm">×</button>
        </header>
        <div class="pma-form-body">
          <div v-if="leaseMissingFields.length" class="rental-appointment-missing">
            <strong>{{ $t('rentalFiles.supplement.common.missingCount', { count: leaseMissingFields.length }) }}</strong>
            <span>{{ leaseMissingFields.join($t('rentalFiles.supplement.common.fieldSeparator')) }}</span>
          </div>
          <section class="lease-property-photo-section">
            <div class="lease-photo-picker-head">
              <h4>{{ $t('rentalFiles.supplement.sections.propertyPhotos') }}</h4>
              <span>{{ $t('rentalFiles.supplement.propertyPhotos.selectedCount', { count: leaseSelectedPhotoIds.length }) }}</span>
            </div>
            <div v-if="leasePropertyPhotos.length" class="lease-photo-picker-grid">
              <label v-for="photo in leasePropertyPhotos" :key="photo.id" :class="{ selected: leaseSelectedPhotoIds.includes(String(photo.id)) }" class="lease-photo-choice">
                <input v-model="leaseSelectedPhotoIds" type="checkbox" :value="String(photo.id)">
                <img :src="leasePropertyPhotoUrl(photo)" :alt="photo.originalName || $t('rentalFiles.supplement.sections.propertyPhotos')">
                <span>{{ photo.originalName }}</span>
              </label>
            </div>
            <p v-else class="lease-photo-empty">{{ $t('rentalFiles.supplement.propertyPhotos.empty') }}</p>
            <div class="lease-photo-picker-actions">
              <button type="button" :disabled="!leasePropertyPhotos.length" @click="selectAllLeasePhotos">{{ $t('rentalFiles.supplement.propertyPhotos.selectAll') }}</button>
              <button type="button" :disabled="!leaseSelectedPhotoIds.length" @click="leaseSelectedPhotoIds = []">{{ $t('rentalFiles.supplement.propertyPhotos.clearSelection') }}</button>
            </div>
            <label class="lease-photo-upload">
              <span>{{ $t('rentalFiles.supplement.propertyPhotos.uploadLabel') }}</span>
              <input type="file" accept="image/jpeg,image/png" multiple @change="setLeasePhotoFiles">
              <small>{{ $t('rentalFiles.supplement.propertyPhotos.selectedFiles', { count: leasePhotoFiles.length }) }}</small>
            </label>
            <div v-if="leasePhotoFiles.length" class="lease-photo-upload-list">
              <article v-for="(file, index) in leasePhotoFiles" :key="`${file.name}-${file.size}-${file.lastModified}-${index}`">
                <div>
                  <strong>{{ file.name }}</strong>
                  <small>{{ formatLeasePhotoFileSize(file.size) }}</small>
                </div>
                <button type="button" @click="removeLeasePhotoFile(file)">{{ $t('rentalFiles.supplement.propertyPhotos.removeFile') }}</button>
              </article>
            </div>
            <p class="pma-profile-sync-hint">{{ $t('rentalFiles.supplement.propertyPhotos.hint') }}</p>
          </section>
          <section class="lease-handover-checklist-section">
            <div class="lease-checklist-head">
              <div>
                <h4>{{ $t('rentalFiles.supplement.sections.handoverChecklist') }}</h4>
                <p>{{ $t('rentalFiles.supplement.handoverChecklist.hint') }}</p>
              </div>
              <strong>{{ $t('rentalFiles.supplement.handoverChecklist.selectedCount', { count: leaseSelectedChecklistIds.length }) }}</strong>
            </div>
            <div v-if="leaseChecklistGroups.length" class="lease-checklist-groups">
              <fieldset v-for="group in leaseChecklistGroups" :key="group.category">
                <legend>{{ group.category }}</legend>
                <label v-for="item in group.items" :key="item.id" :class="{ selected: leaseSelectedChecklistIds.includes(String(item.id)) }">
                  <input v-model="leaseSelectedChecklistIds" type="checkbox" :value="String(item.id)">
                  <span>{{ item.itemName }}</span>
                  <small>{{ item.defaultQuantity || '1' }}</small>
                </label>
              </fieldset>
            </div>
            <p v-else class="lease-photo-empty">{{ $t('rentalFiles.supplement.handoverChecklist.empty') }}</p>
            <div class="lease-photo-picker-actions">
              <button type="button" :disabled="!leaseHandoverChecklist.length" @click="selectAllLeaseChecklist">{{ $t('rentalFiles.supplement.handoverChecklist.selectAll') }}</button>
              <button type="button" :disabled="!leaseSelectedChecklistIds.length" @click="leaseSelectedChecklistIds = []">{{ $t('rentalFiles.supplement.handoverChecklist.clearSelection') }}</button>
            </div>
          </section>
          <section>
            <h4>{{ $t('rentalFiles.supplement.common.signerSection') }}</h4>
            <div class="pma-form-grid">
              <label>{{ $t('rentalFiles.supplement.common.ownerSigner') }}<input v-model.trim="leaseForm.ownerSignerName" required></label>
              <label>{{ $t('rentalFiles.supplement.fields.ownerWitnessSigner') }}<input v-model.trim="leaseForm.ownerWitnessSignerName" required></label>
              <label>{{ $t('rentalFiles.supplement.fields.tenantSigner') }}<input v-model.trim="leaseForm.tenantSignerName" required></label>
              <label>{{ $t('rentalFiles.supplement.fields.tenantWitnessSigner') }}<input v-model.trim="leaseForm.tenantWitnessSignerName" required></label>
            </div>
            <p class="pma-profile-sync-hint">{{ $t('rentalFiles.supplement.common.leaseSignerHint') }}</p>
          </section>
          <section>
            <h4>{{ $t('rentalFiles.supplement.sections.contractPartyPayment') }}</h4>
            <div class="pma-form-grid">
              <label class="wide">{{ $t('rentalFiles.supplement.fields.ownerAddress') }}<textarea v-model.trim="leaseForm.landlordAddress" rows="2" required></textarea></label>
              <label>{{ $t('rentalFiles.supplement.fields.tenantIdentity') }}<input v-model.trim="leaseForm.tenantIdentity" required></label>
              <label>{{ $t('rentalFiles.supplement.fields.tenantPhone') }}<input v-model.trim="leaseForm.tenantPhone" inputmode="tel" required></label>
              <label>{{ $t('rentalFiles.supplement.fields.tenantEmail') }}<input v-model.trim="leaseForm.tenantEmail" required></label>
              <label class="wide">{{ $t('rentalFiles.supplement.fields.tenantAddress') }}<textarea v-model.trim="leaseForm.tenantAddress" rows="2" required></textarea></label>
              <label>{{ $t('rentalFiles.supplement.fields.receivingBankName') }}<input v-model.trim="leaseForm.bankName" :required="leasePaymentNeedsBank"></label>
              <label>{{ $t('rentalFiles.supplement.fields.receivingBankAccount') }}<input v-model.trim="leaseForm.bankAccount" :required="leasePaymentNeedsBank"></label>
              <label class="wide">{{ $t('rentalFiles.supplement.fields.bankBranchAddress') }}<input v-model.trim="leaseForm.bankBranch" :required="leasePaymentNeedsBank"></label>
            </div>
          </section>
          <section>
            <h4>{{ $t('rentalFiles.supplement.sections.paymentTerms') }}</h4>
            <div class="pma-form-grid">
              <label>{{ $t('rentalFiles.supplement.fields.paymentMode') }}<input v-model.trim="leaseForm.paymentMode" required :placeholder="$t('rentalFiles.supplement.placeholders.paymentMode')"></label>
              <label>{{ $t('rentalFiles.supplement.fields.renewalOption') }}<textarea v-model.trim="leaseForm.renewalOption" rows="2" required :placeholder="$t('rentalFiles.supplement.placeholders.renewalOption')"></textarea></label>
              <label class="wide">{{ $t('rentalFiles.supplement.fields.specialConditions') }}<textarea v-model.trim="leaseForm.specialConditions" rows="3" required :placeholder="$t('rentalFiles.supplement.placeholders.specialConditions')"></textarea></label>
            </div>
          </section>
          <section>
            <h4>{{ $t('rentalFiles.supplement.sections.meters') }}</h4>
            <div class="pma-form-grid">
              <label>{{ $t('rentalFiles.supplement.fields.electricityMeter') }}<input v-model.trim="leaseForm.electricityMeter" required></label>
              <label>{{ $t('rentalFiles.supplement.fields.electricityMeterDate') }}<input v-model="leaseForm.electricityMeterDate" type="date"></label>
              <label>{{ $t('rentalFiles.supplement.fields.waterMeter') }}<input v-model.trim="leaseForm.waterMeter" required></label>
              <label>{{ $t('rentalFiles.supplement.fields.waterMeterDate') }}<input v-model="leaseForm.waterMeterDate" type="date"></label>
              <label>{{ $t('rentalFiles.supplement.fields.gasMeter') }}<input v-model.trim="leaseForm.gasMeter"></label>
              <label>{{ $t('rentalFiles.supplement.fields.gasMeterDate') }}<input v-model="leaseForm.gasMeterDate" type="date"></label>
              <label>{{ $t('rentalFiles.supplement.fields.coolingMeter') }}<input v-model.trim="leaseForm.districtCoolingMeter"></label>
              <label>{{ $t('rentalFiles.supplement.fields.coolingMeterDate') }}<input v-model="leaseForm.districtCoolingMeterDate" type="date"></label>
              <label>{{ $t('rentalFiles.supplement.fields.otherMeter') }}<input v-model.trim="leaseForm.otherMeter"></label>
              <label>{{ $t('rentalFiles.supplement.fields.otherMeterDate') }}<input v-model="leaseForm.otherMeterDate" type="date"></label>
            </div>
            <p class="pma-profile-sync-hint">{{ $t('rentalFiles.supplement.leaseCheckOutHint') }}</p>
          </section>
        </div>
        <p v-if="leaseMissingFields.length" class="pma-form-error">{{ $t('rentalFiles.supplement.common.missingBeforeGenerate', { fields: leaseMissingFields.join($t('rentalFiles.supplement.common.fieldSeparator')) }) }}</p>
        <p v-else-if="actionError" class="pma-form-error">{{ $lt(actionError) }}</p>
        <footer><button type="button" class="secondary" @click="closeLeaseForm">{{ $t('rentalFiles.supplement.common.cancel') }}</button><button type="submit" class="primary" :disabled="actionBusy">{{ actionBusy ? $t('rentalFiles.supplement.common.processing') : $t('rentalFiles.supplement.common.saveGenerateLease') }}</button></footer>
      </form>
    </div>

    <div v-if="handoverReportFormOpen" class="pma-form-backdrop" @pointerdown.self="closeHandoverReportForm">
      <form class="pma-form-dialog rental-handover-report-dialog" @submit.prevent="generateHandoverReport">
        <header>
          <div><span>{{ $t('rentalFiles.handoverReportKicker') }}</span><h3>{{ $t('rentalFiles.handoverReportTitle') }}</h3><p>{{ $t('rentalFiles.handoverReportFormHint') }}</p></div>
          <button type="button" :aria-label="$t('rentalFiles.cancel')" @click="closeHandoverReportForm">×</button>
        </header>
        <div class="pma-form-body">
          <section>
            <h4>{{ $t('rentalFiles.handoverReportBasic') }}</h4>
            <div class="pma-form-grid">
              <label>{{ $t('rentalFiles.handoverReportDate') }}<input v-model="handoverReportForm.reportDate" type="date" required></label>
              <label>{{ $t('rentalFiles.handoverReportUnitType') }}<input v-model.trim="handoverReportForm.unitType" required></label>
              <label>{{ $t('rentalFiles.handoverReportFrom') }}<input v-model.trim="handoverReportForm.handoverFrom" required></label>
              <label>{{ $t('rentalFiles.handoverReportTo') }}<input v-model.trim="handoverReportForm.handoverTo" required></label>
              <label class="wide">{{ $t('rentalFiles.handoverReportRemarks') }}<textarea v-model.trim="handoverReportForm.remarks" rows="2"></textarea></label>
            </div>
          </section>
          <section class="lease-handover-checklist-section">
            <div class="lease-checklist-head">
              <div><h4>{{ $t('rentalFiles.supplement.sections.handoverChecklist') }}</h4><p>{{ $t('rentalFiles.handoverReportChecklistHint') }}</p></div>
              <strong>{{ $t('rentalFiles.supplement.handoverChecklist.selectedCount', { count: handoverSelectedChecklistIds.length }) }}</strong>
            </div>
            <div v-if="handoverChecklistGroups.length" class="lease-checklist-groups">
              <fieldset v-for="group in handoverChecklistGroups" :key="group.category">
                <legend>{{ group.category }}</legend>
                <label v-for="item in group.items" :key="item.id" :class="{ selected: handoverSelectedChecklistIds.includes(String(item.id)) }">
                  <input v-model="handoverSelectedChecklistIds" type="checkbox" :value="String(item.id)">
                  <span>{{ item.itemName }}</span><small>{{ item.defaultQuantity || '1' }}</small>
                </label>
              </fieldset>
            </div>
            <p v-else class="lease-photo-empty">{{ $t('rentalFiles.supplement.handoverChecklist.empty') }}</p>
            <div class="lease-photo-picker-actions">
              <button type="button" :disabled="!handoverChecklist.length" @click="selectAllHandoverChecklist">{{ $t('rentalFiles.supplement.handoverChecklist.selectAll') }}</button>
              <button type="button" :disabled="!handoverSelectedChecklistIds.length" @click="handoverSelectedChecklistIds = []">{{ $t('rentalFiles.supplement.handoverChecklist.clearSelection') }}</button>
            </div>
          </section>
          <section class="handover-owner-defects-section">
            <div class="handover-owner-defects-head">
              <div><h4>{{ $t('rentalFiles.handoverTenantDefects') }}</h4><p>{{ $t('rentalFiles.handoverTenantDefectsHint') }}</p></div>
              <button type="button" @click="addHandoverIssue('tenantIssues')">{{ $t('rentalFiles.addHandoverTenantDefect') }}</button>
            </div>
            <p v-if="!handoverReportForm.tenantIssues?.length" class="lease-photo-empty">{{ $t('rentalFiles.noHandoverTenantDefects') }}</p>
            <div v-else class="handover-owner-defect-list">
              <article v-for="(issue, index) in handoverReportForm.tenantIssues" :key="issue.key" class="is-single">
                <strong>{{ $t('rentalFiles.handoverDefectItem', { number: index + 1 }) }}</strong>
                <label>{{ $t('rentalFiles.handoverRepairMaintenance') }}<textarea v-model.trim="issue.name" rows="2" :placeholder="$t('rentalFiles.handoverTenantDefectPlaceholder')"></textarea></label>
                <button type="button" class="handover-owner-defect-remove" @click="removeHandoverIssue('tenantIssues', index)">{{ $t('rentalFiles.removeHandoverOwnerDefect') }}</button>
              </article>
            </div>
          </section>
          <section class="handover-owner-defects-section">
            <div class="handover-owner-defects-head">
              <div><h4>{{ $t('rentalFiles.handoverOwnerDefects') }}</h4><p>{{ $t('rentalFiles.handoverOwnerDefectsHint') }}</p></div>
              <button type="button" @click="addHandoverIssue('ownerIssues')">{{ $t('rentalFiles.addHandoverOwnerDefect') }}</button>
            </div>
            <p v-if="!handoverReportForm.ownerIssues?.length" class="lease-photo-empty">{{ $t('rentalFiles.noHandoverOwnerDefects') }}</p>
            <div v-else class="handover-owner-defect-list">
              <article v-for="(issue, index) in handoverReportForm.ownerIssues" :key="issue.key">
                <strong>{{ $t('rentalFiles.handoverDefectItem', { number: index + 1 }) }}</strong>
                <label>{{ $t('rentalFiles.handoverDefectProblem') }}<textarea v-model.trim="issue.name" rows="2" :placeholder="$t('rentalFiles.handoverDefectProblemPlaceholder')"></textarea></label>
                <label>{{ $t('rentalFiles.handoverDefectRecommendation') }}<textarea v-model.trim="issue.recommendation" rows="2" :placeholder="$t('rentalFiles.handoverDefectRecommendationPlaceholder')"></textarea></label>
                <button type="button" class="handover-owner-defect-remove" @click="removeHandoverIssue('ownerIssues', index)">{{ $t('rentalFiles.removeHandoverOwnerDefect') }}</button>
              </article>
            </div>
          </section>
          <section class="lease-property-photo-section">
            <div class="lease-photo-picker-head"><h4>{{ $t('rentalFiles.handoverReportPhotos') }}</h4><span>{{ $t('rentalFiles.supplement.propertyPhotos.selectedCount', { count: handoverSelectedPhotoIds.length }) }}</span></div>
            <div v-if="handoverPropertyPhotos.length" class="lease-photo-picker-grid">
              <label v-for="photo in handoverPropertyPhotos" :key="photo.id" :class="{ selected: handoverSelectedPhotoIds.includes(String(photo.id)) }" class="lease-photo-choice handover-photo-choice">
                <input v-model="handoverSelectedPhotoIds" type="checkbox" :value="String(photo.id)">
                <img :src="handoverPropertyPhotoUrl(photo)" :alt="photo.originalName || $t('rentalFiles.handoverReportPhotos')">
                <span>{{ photo.originalName }}</span>
                <select v-model="handoverPhotoSectionById[String(photo.id)]" @click.stop>
                  <option v-for="section in handoverPhotoSections" :key="section" :value="section">{{ section }}</option>
                </select>
                <textarea v-model.trim="handoverPhotoCaptionById[String(photo.id)]" rows="2" :placeholder="$t('rentalFiles.handoverPhotoCaptionPlaceholder')" @click.stop></textarea>
              </label>
            </div>
            <p v-else class="lease-photo-empty">{{ $t('rentalFiles.supplement.propertyPhotos.empty') }}</p>
            <div class="lease-photo-picker-actions">
              <button type="button" :disabled="!handoverPropertyPhotos.length" @click="selectAllHandoverPhotos">{{ $t('rentalFiles.supplement.propertyPhotos.selectAll') }}</button>
              <button type="button" :disabled="!handoverSelectedPhotoIds.length" @click="handoverSelectedPhotoIds = []">{{ $t('rentalFiles.supplement.propertyPhotos.clearSelection') }}</button>
            </div>
            <label class="lease-photo-upload"><span>{{ $t('rentalFiles.handoverReportAddPhotos') }}</span><input type="file" accept="image/jpeg,image/png" multiple @change="setHandoverPhotoFiles"><small>{{ $t('rentalFiles.supplement.propertyPhotos.selectedFiles', { count: handoverPhotoFiles.length }) }}</small></label>
            <div v-if="handoverPhotoFiles.length" class="lease-photo-upload-list">
              <article v-for="entry in handoverPhotoFiles" :key="entry.key">
                <div><strong>{{ entry.file.name }}</strong><small>{{ formatLeasePhotoFileSize(entry.file.size) }}</small></div>
                <select v-model="entry.section"><option v-for="section in handoverPhotoSections" :key="section" :value="section">{{ section }}</option></select>
                <textarea v-model.trim="entry.caption" rows="2" :placeholder="$t('rentalFiles.handoverPhotoCaptionPlaceholder')"></textarea>
                <button type="button" @click="removeHandoverPhotoFile(entry)">{{ $t('rentalFiles.supplement.propertyPhotos.removeFile') }}</button>
              </article>
            </div>
            <p class="pma-profile-sync-hint">{{ $t('rentalFiles.handoverReportPhotoHint') }}</p>
          </section>
        </div>
        <p v-if="handoverReportError" class="pma-form-error">{{ $lt(handoverReportError) }}</p>
        <footer><button type="button" class="secondary" @click="closeHandoverReportForm">{{ $t('rentalFiles.cancel') }}</button><button type="submit" class="primary" :disabled="handoverReportSaving">{{ handoverReportSaving ? $t('rentalFiles.processing') : $t('rentalFiles.generateHandoverReport') }}</button></footer>
      </form>
    </div>

    <div v-if="financeDocumentOpen" class="finance-document-backdrop" @pointerdown.self="closeFinanceDocumentPanel">
      <section class="finance-document-dialog" role="dialog" aria-modal="true" :aria-label="$t('rentalFiles.financeDocumentPanelTitle')">
        <header>
          <div><span>{{ $t('rentalFiles.financeDocumentKicker') }}</span><h3>{{ $t('rentalFiles.financeDocumentPanelTitle') }}</h3><p>{{ $t('rentalFiles.financeDocumentPanelHint') }}</p></div>
          <button type="button" :aria-label="$t('rentalFiles.cancel')" @click="closeFinanceDocumentPanel">×</button>
        </header>
        <div class="finance-document-context">
          <div><small>{{ $t('rentalFiles.financeDocumentProperty') }}</small><strong>{{ propertyTitle }}</strong></div>
          <div><small>{{ $t('rentalFiles.financeDocumentCycle') }}</small><strong>{{ currentMandate?.mandateNo || '—' }}</strong></div>
          <div class="finance-document-type">
            <small>{{ $t('rentalFiles.financeDocumentKind') }}</small>
            <div><button type="button" :class="{ active: financeDocumentType === 'invoice' }" @click="financeDocumentType = 'invoice'">{{ $t('rentalFiles.financeDocumentInvoice') }}</button><button type="button" :class="{ active: financeDocumentType === 'receipt' }" @click="financeDocumentType = 'receipt'">{{ $t('rentalFiles.financeDocumentReceipt') }}</button></div>
          </div>
        </div>
        <div class="finance-document-body">
          <div v-if="financeDocumentLoading" class="finance-document-state">{{ $t('rentalFiles.financeDocumentLoading') }}</div>
          <div v-else-if="financeDocumentError" class="finance-document-state error"><span>{{ $lt(financeDocumentError) }}</span><button type="button" @click="loadFinanceDocumentRows">{{ $t('rentalFiles.retry') }}</button></div>
          <div v-else-if="!financeDocumentRows.length" class="finance-document-state">{{ $t('rentalFiles.financeDocumentEmpty') }}</div>
          <template v-else>
            <label class="finance-document-select-all"><input type="checkbox" :checked="financeDocumentAllSelected" @change="toggleAllFinanceDocuments"> <span>{{ $t('rentalFiles.financeDocumentSelectAll') }}</span><em>{{ financeDocumentRows.length }} {{ $t('rentalFiles.financeDocumentRecords') }}</em></label>
            <div class="finance-document-list">
              <label v-for="row in financeDocumentRows" :key="`${row.recordType}-${row.id}`" :class="{ selected: financeDocumentSelectedIds.includes(row.id) }">
                <input v-model="financeDocumentSelectedIds" type="checkbox" :value="row.id">
                <span class="finance-document-record-icon">{{ financeRecordDirection(row) }}</span>
                <span class="finance-document-record-main"><strong>{{ financeRecordDescription(row) }}</strong><small>{{ row.transactionNo || '—' }} · {{ $lt(financeRecordTypeLabel(row)) }}</small></span>
                <span class="finance-document-record-meta"><strong>{{ financeMoney(row) }}</strong><small>{{ formatDate(row.transactionDate || row.dueDate) }}</small></span>
                <em :class="`is-${String(row.confirmationStatus || 'pending').toLowerCase()}`">{{ $lt(financeStatusLabel(row)) }}</em>
              </label>
            </div>
          </template>
        </div>
        <p v-if="financeDocumentActionError" class="finance-document-error">{{ $lt(financeDocumentActionError) }}</p>
        <footer><span>{{ $t('rentalFiles.financeDocumentSelectedCount', { count: financeDocumentSelectedIds.length }) }}</span><div><button type="button" class="secondary" @click="closeFinanceDocumentPanel">{{ $t('rentalFiles.cancel') }}</button><button type="button" class="primary" :disabled="!financeDocumentSelectedIds.length || financeDocumentBusy" @click="generateFinanceDocuments">{{ financeDocumentBusy ? $t('rentalFiles.processing') : $t(financeDocumentType === 'invoice' ? 'rentalFiles.financeDocumentGenerateInvoice' : 'rentalFiles.financeDocumentGenerateReceipt') }}</button></div></footer>
      </section>
    </div>

    <div v-if="templateDesignerOpen" class="template-designer-backdrop">
      <section class="template-designer-dialog" role="dialog" aria-modal="true" :aria-label="$t('rentalFiles.templateDesignerTitle')">
        <header>
          <div><span>{{ $t('rentalFiles.templateDesignerKicker') }}</span><h3>{{ $t('rentalFiles.templateDesignerTitle') }}</h3><p>{{ $t('rentalFiles.templateDesignerHint') }}</p></div>
          <button type="button" :aria-label="$t('rentalFiles.cancel')" @click="closeTemplateDesigner">×</button>
        </header>
        <div v-if="templateDesignerLoading" class="template-designer-state">{{ $t('rentalFiles.templateDesignerLoading') }}</div>
        <div v-else-if="templateDesignerError" class="template-designer-state error"><span>{{ $lt(templateDesignerError) }}</span><button type="button" @click="loadTemplateDesigner">{{ $t('rentalFiles.retry') }}</button></div>
        <template v-else>
          <div class="template-designer-toolbar">
            <div><strong>{{ templateDesignerName }}</strong><small>{{ $t('rentalFiles.templateVersion') }} {{ templateLayout?.version }}</small></div>
            <nav><button type="button" :disabled="templatePage <= 1" @click="changeTemplatePage(templatePage - 1)">‹</button><span>{{ $t('rentalFiles.templatePage', { current: templatePage, total: templateLayout?.pages || 0 }) }}</span><button type="button" :disabled="templatePage >= (templateLayout?.pages || 1)" @click="changeTemplatePage(templatePage + 1)">›</button></nav>
          </div>
          <div class="template-designer-body">
            <div class="template-canvas-scroll">
              <div class="template-canvas-wrap" :style="templateCanvasStyle">
                <canvas ref="templateCanvas"></canvas>
                <div ref="templateOverlay" class="template-field-overlay">
                  <button v-for="field in templatePageFields" :key="field.id" type="button" :class="['template-field-marker', { active: field.id === templateSelectedFieldId, templateSignatureField: isTemplateSignatureField(field) }]" :style="templateFieldStyle(field)" @pointerdown="beginTemplateFieldDrag(field, $event)" @click.stop="selectTemplateField(field)"><span>{{ $lt(field.label) }}</span></button>
                </div>
              </div>
            </div>
            <aside class="template-field-panel">
              <div><span>{{ $t('rentalFiles.templatePageFields') }}</span><strong>{{ templatePageFields.length }}</strong></div>
               <button v-for="field in templatePageFields" :key="`list-${field.id}`" type="button" :class="{ active: field.id === templateSelectedFieldId }" @click="selectTemplateField(field)"><strong>{{ $lt(field.label) }}</strong><small v-if="isTemplateSignatureField(field)">{{ $t('legacy.t_5c266f4c6b20') }}{{ templateSignatureRole(field) }} · {{ $t('rentalFiles.templateCoordinates', { x: field.x.toFixed(1), y: field.y.toFixed(1) }) }}</small><small v-else>{{ $t('rentalFiles.templateCoordinates', { x: field.x.toFixed(1), y: field.y.toFixed(1) }) }}</small></button>
              <p v-if="!templatePageFields.length">{{ $t('rentalFiles.templateNoFieldsOnPage') }}</p>
              <section v-if="templateSelectedField" class="template-field-settings">
                <h4>{{ $lt(templateSelectedField.label) }}</h4>
                <div><label class="template-field-page">{{ $t('rentalFiles.templateFieldPage') }}<select :value="templateSelectedField.page" @change="changeTemplateFieldPage"><option v-for="page in templatePages" :key="`field-page-${page}`" :value="page">{{ $t('rentalFiles.templatePageOption', { page }) }}</option></select></label></div>
                <div><label>{{ $t('rentalFiles.templateCoordinateX') }}<input v-model.number="templateSelectedField.x" type="number" min="0" step="0.5"></label><label>{{ $t('rentalFiles.templateCoordinateY') }}<input v-model.number="templateSelectedField.y" type="number" min="0" step="0.5"></label></div>
                 <div v-if="isTemplateSignatureField(templateSelectedField)"><label>{{ $t('legacy.t_3bfe7177ce8d') }}<input v-model.number="templateSelectedField.maxWidth" class="templateSignatureWidth" type="number" min="20" step="1"></label><label>{{ $t('legacy.t_6d2665fa5669') }}<input v-model.number="templateSelectedField.lineHeight" class="templateSignatureHeight" type="number" min="8" max="40" step="1"></label></div>
                 <div v-else><label>{{ $t('rentalFiles.templateFontSize') }}<input v-model.number="templateSelectedField.fontSize" type="number" min="6" max="30" step="0.5"></label><label>{{ $t('rentalFiles.templateFieldWidth') }}<input v-model.number="templateSelectedField.maxWidth" type="number" min="20" step="1"></label></div>
                <small>{{ $t('rentalFiles.templateMovePageHint') }}</small>
                <small>{{ $t('rentalFiles.templateCoordinateHint') }}</small>
              </section>
            </aside>
          </div>
        </template>
        <p v-if="templateDesignerActionError" class="template-designer-error">{{ $lt(templateDesignerActionError) }}</p>
        <footer><span>{{ $t('rentalFiles.templateSaveHint') }}</span><div><button type="button" class="secondary" @click="closeTemplateDesigner">{{ $t('rentalFiles.cancel') }}</button><button type="button" class="primary" :disabled="templateDesignerSaving || templateDesignerLoading" @click="saveTemplateDesigner">{{ templateDesignerSaving ? $t('rentalFiles.processing') : $t('rentalFiles.saveTemplatePositions') }}</button></div></footer>
      </section>
    </div>

  </section>
</template>

<script>
import { markRaw } from 'vue';
import { PHONE_COUNTRIES, validatePhone } from '../utils/tenantPhone';
import AdminOwnerSignatureDispatch from './AdminOwnerSignatureDispatch.vue';
import { getDocument, GlobalWorkerOptions } from 'pdfjs-dist';
import pdfWorker from 'pdfjs-dist/build/pdf.worker.min.mjs?url';
import {
  downloadAdminPropertyAttachment,
  downloadAdminPropertyContractRecord,
  downloadAdminPropertyHandoverReport,
  downloadAdminRentalMandateDocument,
  fetchAdminLeaseContract,
  fetchAdminProperties,
  fetchAdminOwner,
  updateAdminOwner,
  fetchAdminPropertyAttachments,
  fetchAdminPropertyHandover,
  fetchAdminPropertyHandoverReports,
  createAdminPropertyHandoverReport,
  fetchAdminPropertyHandoverChecklist,
  updateAdminPropertyHandoverChecklistItem,
  fetchAdminPropertyBankAccounts,
  fetchAdminPropertyPhotos,
  adminPropertyPhotoContentUrl,
  createAdminPropertyPhoto,
  createAdminPropertyBankAccount,
  updateAdminPropertyBankAccount,
  fetchAdminPropertyWorkspaceById,
  fetchAdminFinanceReviews,
  fetchAdminRentalMandateDocuments,
  fetchAdminRentalAppointmentDetails,
  saveAdminRentalAppointmentDetails,
  fetchAdminLeaseAgreementDetails,
  saveAdminLeaseAgreementDetails,
  fetchAdminRentalMandates,
  fetchAdminContractTemplateVersion,
  fetchAdminContractTemplateFile,
  fetchAdminContractTemplateLayout,
  generateAdminContractTemplate,
  replaceAdminContractTemplate,
  saveAdminContractTemplateLayout,
  fetchAdminLeaseSignatureParticipants,
  startAdminLeaseSignaturePackage,
  startAdminMandateDocumentSignature,
  fetchAdminMandateSignatureParticipants,
  startAdminMandateSignaturePackage,
  sendAdminElectronicSignatureInvitation,
  fetchAdminSignatureWhatsApp,
  sendAdminSignatureWhatsApp,
  uploadAdminGeneratedLeaseContract,
  uploadAdminRentalMandateDocument,
  downloadAdminFinanceDocuments,
} from '../services/propertyApi';
import { reportBelongsToMandate, selectCurrentRentalMandate } from '../utils/rentalCycleWorkbench';
import { rentalFileLifecycle } from '../utils/rentalFileStatus';
import { formatDateTime } from '../utils/dateFormat';
import { splitTenantDeposit } from '../utils/tenantDeposit';
import { findFirstEmptyRequiredControl, missingRequiredFields, revealFirstInvalidControl } from '../utils/supplementFormValidation';
import { mergeUniquePhotoFiles, removePhotoFile } from '../utils/photoFiles';

const CATEGORY_ORDER = ['management', 'appointment', 'authorization', 'termination', 'remittance', 'otr', 'contract', 'handover'];
const HANDOVER_PHOTO_SECTIONS = ['鑰匙、通行卡和遙控器照片 / Keys, Access Card & Remote Control', '客廳照片 / Living Room', '飯廳照片 / Dining Room', '廚房照片 / Kitchen', '主臥室照片 / Master Bedroom', '主浴室照片 / Master Bathroom', '瑕疵 (扣租客押金) / Defects (Deduct From Deposit)', '瑕疵 (询问屋主是否要维修) / Defects (Ask Owner If Repairs Are Needed)'];
const COMPLETE_CONTRACT_STATUSES = new Set(['signed', 'completed', 'active', 'approved']);
const EDITABLE_MANDATE_STATUSES = new Set(['draft', 'pending_review', 'active', 'suspended']);
const emptyPmaForm = () => ({ ownerSignerName: '', companySignerName: '', customerServiceSignerName: '', landlordName: '', landlordIdentity: '', propertyAddress: '', ownerAddress: '', ownerEmail: '', ownerPhone: '', agreementDate: '', startDate: '', endDate: '', bankAccountId: '', bankPayeeName: '', bankName: '', bankAddress: '', bankBranchCode: '', bankAccountNo: '', bankSwiftCode: '' });
const PMA_REQUIRED_FIELDS = { ownerSignerName: 'rentalFiles.pmaOwnerSigner', companySignerName: 'rentalFiles.pmaCompanySigner', customerServiceSignerName: 'rentalFiles.pmaCustomerServiceSigner', landlordName: 'rentalFiles.pmaOwnerName', landlordIdentity: 'rentalFiles.pmaOwnerIdentity', propertyAddress: 'rentalFiles.pmaPropertyAddress', ownerAddress: 'rentalFiles.pmaOwnerAddress', ownerEmail: 'rentalFiles.pmaOwnerEmail', ownerPhone: 'rentalFiles.pmaOwnerPhone', agreementDate: 'rentalFiles.pmaAgreementDate', startDate: 'rentalFiles.pmaStartDate', endDate: 'rentalFiles.pmaEndDate', bankPayeeName: 'rentalFiles.pmaPayeeName', bankName: 'rentalFiles.pmaBankName', bankAddress: 'rentalFiles.pmaBankAddress', bankBranchCode: 'rentalFiles.pmaBranchCode', bankAccountNo: 'rentalFiles.pmaAccountNo', bankSwiftCode: 'rentalFiles.pmaSwiftCode' };
const emptyAuthorizationForm = () => ({ ownerSignerName: '', projectName: '', unitNo: '', landlordName: '', landlordIdentity: '', ownerEmail: '', agreementDate: '' });
const emptyRentalAppointmentForm = () => ({ ownerSignerName: '', caseNo: '', propertyAddress: '', earnestDeposit: '', earnestDepositWords: '', commissionWords: '', commissionMonths: '', sstPercent: '', commissionAmount: '', agencyFeeTotal: '', startDate: '', endDate: '', landlordName: '', landlordIdentity: '', landlordAddress: '', landlordDate: '', hasSecondLandlord: false, secondLandlordName: '', secondLandlordIdentity: '', secondLandlordAddress: '', secondLandlordDate: '', witnessName: '', witnessIdentity: '', witnessAddress: '', witnessDate: '' });
const emptyOtrForm = () => ({ caseNo: '', propertyAddress: '', advanceRental: '', securityDepositMonths: '', securityDeposit: '', utilityDepositMonths: '', utilityDeposit: '', stampingFee: '', totalBeforeKeys: '', periodYears: '', renewalYears: '', commencementDate: '', earnestDeposit: '', tenantName: '', tenantIdentity: '', tenantDate: '', landlordName: '', landlordIdentity: '', landlordDate: '', tenantWitnessName: '', tenantWitnessIdentity: '', tenantWitnessDate: '', landlordWitnessName: '', landlordWitnessIdentity: '', landlordWitnessDate: '', otherConditions: '', earnestDepositWords: '', commissionWords: '', commissionMonths: '', sstPercent: '', commissionAmount: '', agencyFeeTotal: '', startDate: '', endDate: '', landlordAddress: '', hasSecondLandlord: false, secondLandlordName: '', secondLandlordIdentity: '', secondLandlordAddress: '', secondLandlordDate: '', witnessName: '', witnessIdentity: '', witnessAddress: '', witnessDate: '' });
const emptyLeaseForm = () => ({ ownerSignerName: '', ownerWitnessSignerName: '', tenantSignerName: '', tenantWitnessSignerName: '', landlordAddress: '', tenantIdentity: '', tenantPhone: '', tenantEmail: '', tenantAddress: '', bankName: '', bankAccount: '', bankBranch: '', paymentMode: 'Bank Transfer', renewalOption: '', specialConditions: '', handoverChecklistIds: '', photoIds: '', electricityMeter: '', electricityMeterDate: '', waterMeter: '', waterMeterDate: '', gasMeter: '', gasMeterDate: '', districtCoolingMeter: '', districtCoolingMeterDate: '', otherMeter: '', otherMeterDate: '' });
const emptyTerminationLetterForm = () => ({ ownerSignerName: '', companySignerName: '', agreementDate: '', landlordName: '', landlordIdentity: '', ownerEmail: '', projectName: '', unitNo: '', authorizedAgentName: '', authorizedAgentIdentity: '', authorizedAgentPhone: '', authorizedAgentEmail: '', bankName: '', bankPayeeName: '', bankAccountNo: '', bankSwiftCode: '', bankAddress: '' });
const emptyRentalRemittanceForm = () => ({ ownerSignerName: '', agreementDate: '', landlordName: '', landlordIdentity: '', ownerEmail: '', unitNo: '', bankAccountId: '', bankPayeeName: '', bankName: '', bankAccountNo: '', bankSwiftCode: '', bankBranchCode: '', bankAddress: '' });
const sf = field => `rentalFiles.supplement.fields.${field}`;
const AUTHORIZATION_REQUIRED_FIELDS = { ownerSignerName: 'rentalFiles.supplement.common.ownerSigner', projectName: sf('projectName'), unitNo: sf('unitNo'), landlordName: sf('landlordName'), landlordIdentity: sf('passportNo'), ownerEmail: sf('ownerEmail'), agreementDate: sf('authorizationDate') };
const TERMINATION_LETTER_REQUIRED_FIELDS = { ownerSignerName: 'rentalFiles.supplement.common.ownerSigner', companySignerName: 'rentalFiles.pmaCompanySigner', agreementDate: sf('letterDate'), landlordName: sf('ownerName'), landlordIdentity: sf('landlordIdentity'), projectName: sf('projectName'), unitNo: sf('unitNo'), authorizedAgentName: sf('agentName'), authorizedAgentIdentity: sf('agentIdentity'), authorizedAgentPhone: sf('agentPhone'), authorizedAgentEmail: sf('agentEmail'), bankName: sf('bankName'), bankPayeeName: sf('payeeName'), bankAccountNo: sf('bankAccount'), bankSwiftCode: sf('swiftCode'), bankAddress: sf('bankAddress') };
const RENTAL_REMITTANCE_REQUIRED_FIELDS = { ownerSignerName: 'rentalFiles.supplement.common.ownerSigner', agreementDate: sf('letterDate'), landlordName: sf('ownerName'), landlordIdentity: sf('passportNo'), unitNo: sf('unitNo'), bankPayeeName: sf('authorizedPayee'), bankName: sf('bankName'), bankAccountNo: sf('bankAccount'), bankSwiftCode: sf('swiftCode'), bankBranchCode: sf('branchCode'), bankAddress: sf('bankAddress') };
const RENTAL_APPOINTMENT_REQUIRED_FIELDS = { ownerSignerName: 'rentalFiles.supplement.common.ownerSigner', caseNo: sf('appointmentNo'), propertyAddress: sf('propertyAddress'), earnestDeposit: sf('depositAmount'), earnestDepositWords: sf('depositWords'), commissionWords: sf('commissionWords'), commissionMonths: sf('commissionMonths'), sstPercent: sf('sstPercent'), commissionAmount: sf('commissionAmount'), agencyFeeTotal: sf('agencyFeeTotal'), startDate: sf('appointmentStart'), endDate: sf('appointmentEnd'), landlordName: sf('name'), landlordIdentity: sf('identityPassport'), landlordAddress: sf('address'), landlordDate: sf('signingDate'), witnessName: sf('name'), witnessIdentity: sf('identityPassport'), witnessAddress: sf('address'), witnessDate: sf('witnessDate') };
const SECOND_LANDLORD_REQUIRED_FIELDS = { secondLandlordName: sf('secondOwnerName'), secondLandlordIdentity: sf('identityPassport'), secondLandlordAddress: sf('address'), secondLandlordDate: sf('signingDate') };
const OTR_REQUIRED_FIELDS = { caseNo: sf('caseNo'), propertyAddress: sf('propertyAddress'), advanceRental: sf('advanceRental'), securityDepositMonths: sf('securityDepositMonths'), securityDeposit: sf('securityDeposit'), utilityDepositMonths: sf('utilityDepositMonths'), utilityDeposit: sf('utilityDeposit'), stampingFee: sf('stampingFee'), totalBeforeKeys: sf('totalBeforeKeys'), periodYears: sf('periodYears'), renewalYears: sf('renewalYears'), commencementDate: sf('commencementDate'), earnestDeposit: sf('earnestDeposit'), tenantName: sf('tenantName'), tenantIdentity: sf('tenantIdentity'), tenantDate: sf('tenantDate'), landlordName: sf('landlordName'), landlordIdentity: sf('landlordIdentity'), landlordDate: sf('landlordDate'), tenantWitnessName: sf('tenantWitnessName'), tenantWitnessIdentity: sf('tenantWitnessIdentity'), tenantWitnessDate: sf('tenantWitnessDate'), landlordWitnessName: sf('ownerWitnessName'), landlordWitnessIdentity: sf('ownerWitnessIdentity'), landlordWitnessDate: sf('ownerWitnessDate'), otherConditions: sf('otherConditions'), earnestDepositWords: sf('depositWords'), commissionWords: sf('commissionWords'), commissionMonths: sf('commissionMonths'), sstPercent: sf('sstPercent'), commissionAmount: sf('commissionAmount'), agencyFeeTotal: sf('agencyFeeTotal'), startDate: sf('appointmentStart'), endDate: sf('appointmentEnd'), landlordAddress: sf('address'), witnessAddress: sf('address') };
const LEASE_REQUIRED_FIELDS = { ownerSignerName: 'rentalFiles.supplement.common.ownerSigner', ownerWitnessSignerName: sf('ownerWitnessSigner'), tenantSignerName: sf('tenantSigner'), tenantWitnessSignerName: sf('tenantWitnessSigner'), landlordAddress: sf('ownerAddress'), tenantIdentity: sf('tenantIdentity'), tenantPhone: sf('tenantPhone'), tenantEmail: sf('tenantEmail'), tenantAddress: sf('tenantAddress'), paymentMode: sf('paymentMode'), renewalOption: sf('renewalOption'), specialConditions: sf('specialConditions'), electricityMeter: sf('electricityMeter'), waterMeter: sf('waterMeter') };
GlobalWorkerOptions.workerSrc = pdfWorker;

export default {
  components: { AdminOwnerSignatureDispatch },
  name: 'AdminRentalSigningWorkspace',
  inject: ['page'],
  data() {
    return {
      phoneCountries: PHONE_COUNTRIES,
      properties: [], mandates: [], selectedProperty: null, selectedPropertyKey: '', selectedCycleId: '', selectedLeaseId: '', propertySearch: '', selectedProjectName: '',
      workspace: null, mandateDocuments: [], propertyAttachments: [], handoverReports: [], files: [],
      fileSearch: '', categoryFilter: '', loading: false, loadError: '', workspaceLoading: false, workspaceError: '',
      actionBusy: false, actionError: '', signingPanel: '', signerForm: { name: '', email: '' }, signerPackageForm: [], signerPackageLoading: false, generatedSigningLinks: [], signingEmailBusyId: null, signingEmailSentIds: [], signingWhatsAppBusyId: null, signingDeliveryError: '', downloadingKey: '',
      pmaFormOpen: false, pmaForm: emptyPmaForm(), pmaBankAccounts: [], pmaBankAccount: null, pmaOwnerProfile: null,
      authorizationFormOpen: false, authorizationForm: emptyAuthorizationForm(),
      terminationLetterFormOpen: false, terminationLetterForm: emptyTerminationLetterForm(),
      rentalRemittanceFormOpen: false, rentalRemittanceForm: emptyRentalRemittanceForm(),
      rentalAppointmentFormOpen: false, rentalAppointmentForm: emptyRentalAppointmentForm(),
      otrFormOpen: false, otrForm: emptyOtrForm(),
      leaseFormOpen: false, leaseForm: emptyLeaseForm(), leaseGenerationStartsSigning: false,
      leaseHandoverChecklist: [], leaseSelectedChecklistIds: [],
      leasePropertyPhotos: [], leaseSelectedPhotoIds: [], leasePhotoFiles: [],
      handoverReportFormOpen: false, handoverReportSaving: false, handoverReportError: '',
      handoverReportForm: { reportDate: '', unitType: '', handoverFrom: '', handoverTo: '', remarks: '', tenantIssues: [], ownerIssues: [] },
      handoverChecklist: [], handoverSelectedChecklistIds: [], handoverPropertyPhotos: [], handoverSelectedPhotoIds: [],
      handoverPhotoFiles: [], handoverPhotoSectionById: {}, handoverPhotoCaptionById: {}, handoverPhotoSections: HANDOVER_PHOTO_SECTIONS,
      validatedSupplementForms: { otr: false, rentalAppointment: false, authorization: false, terminationLetter: false, rentalRemittance: false, pma: false, lease: false },
      financeDocumentOpen: false, financeDocumentRows: [], financeDocumentSelectedIds: [], financeDocumentType: 'invoice',
      financeDocumentLoading: false, financeDocumentBusy: false, financeDocumentError: '', financeDocumentActionError: '',
      templateVersions: {},
      templateDesignerOpen: false, templateDesignerType: '', templateDesignerLoading: false, templateDesignerSaving: false,
      templateDesignerError: '', templateDesignerActionError: '', templateLayout: null, templatePdf: null,
      templatePage: 1, templateScale: 1.15, templateSelectedFieldId: '', templateDrag: null,
      requestedAction: new URLSearchParams(window.location.search).get('action') || '',
      requestedLeaseId: new URLSearchParams(window.location.search).get('leaseId') || '',
    };
  },
  computed: {
    projectOptions() { return [...new Set(this.properties.map(property => String(property.projectName || '').trim()).filter(Boolean))].sort((a, b) => a.localeCompare(b)); },
    filteredProperties() { const query = this.propertySearch.toLowerCase(); return this.properties.filter(property => (!this.selectedProjectName || property.projectName === this.selectedProjectName) && (!query || [property.projectName, property.unitNo, property.ownerName].some(value => String(value || '').toLowerCase().includes(query)))); },
    propertyTitle() { return `${this.selectedProperty?.projectName || this.$t('rentalFiles.unsetProject')} · ${this.selectedProperty?.unitNo || this.$t('rentalFiles.unsetUnit')}`; },
    leasePropertyAddress() {
      const property = this.selectedProperty || {};
      const unit = String(property.unitNo || '').trim();
      const address = String(property.address || property.fullAddress || property.projectName || '').trim();
      if (!unit) return address;
      if (!address) return unit;
      return address.includes(unit) ? address : `${unit}, ${address}`;
    },
    leaseChecklistGroups() {
      const groups = new Map();
      this.leaseHandoverChecklist.forEach(item => {
        const category = item.category || this.$t('rentalFiles.supplement.handoverChecklist.otherCategory');
        if (!groups.has(category)) groups.set(category, []);
        groups.get(category).push(item);
      });
      return Array.from(groups, ([category, items]) => ({ category, items }));
    },
    handoverChecklistGroups() {
      const groups = new Map();
      this.handoverChecklist.forEach(item => {
        const category = item.category || this.$t('rentalFiles.supplement.handoverChecklist.otherCategory');
        if (!groups.has(category)) groups.set(category, []);
        groups.get(category).push(item);
      });
      return Array.from(groups, ([category, items]) => ({ category, items }));
    },
    propertyMandates() { return this.mandates.filter(item => String(item.ownerUnitId) === String(this.selectedProperty?.ownerUnitId)); },
    activeCycle() {
      return selectCurrentRentalMandate({ property: this.selectedProperty || {}, mandates: this.propertyMandates })
        || [...this.propertyMandates]
          .filter(item => EDITABLE_MANDATE_STATUSES.has(String(item.status || '').toLowerCase()))
          .sort((left, right) => String(right.startDate || right.createdAt || '').localeCompare(String(left.startDate || left.createdAt || '')) || Number(right.id || 0) - Number(left.id || 0))[0]
        || null;
    },
    rentalCycles() { return [...this.propertyMandates].sort((left, right) => Number(String(right.id) === String(this.activeCycle?.id)) - Number(String(left.id) === String(this.activeCycle?.id)) || String(right.startDate || right.createdAt || '').localeCompare(String(left.startDate || left.createdAt || '')) || Number(right.id || 0) - Number(left.id || 0)); },
    currentMandate() { return this.rentalCycles.find(item => String(item.id) === String(this.selectedCycleId)) || this.activeCycle || this.rentalCycles[0] || null; },
    selectedCycleIsHistorical() { return Boolean(this.currentMandate && (!this.activeCycle || String(this.currentMandate.id) !== String(this.activeCycle.id) || !EDITABLE_MANDATE_STATUSES.has(String(this.currentMandate.status || '').toLowerCase()))); },
    cycleLeases() { return (this.workspace?.leases || []).filter(lease => String(lease.rentalMandateId || lease.mandateId) === String(this.currentMandate?.id)).sort((left, right) => String(right.startDate || '').localeCompare(String(left.startDate || '')) || Number(right.id || right.leaseId || 0) - Number(left.id || left.leaseId || 0)); },
    activeCycleLease() { return this.cycleLeases.find(lease => String(lease.status || '').toLowerCase() === 'active') || null; },
    currentLease() { return this.cycleLeases.find(lease => String(lease.id || lease.leaseId) === String(this.selectedLeaseId)) || this.cycleLeases.find(lease => String(lease.status || '').toLowerCase() === 'active') || this.cycleLeases[0] || null; },
    rentalHeaderStatusKey() {
      if (!this.currentMandate) return 'rentalFiles.waitingRental';
      if (this.selectedCycleIsHistorical) return 'rentalFiles.historyRental';
      if (this.currentLease) return String(this.currentLease.status || '').toLowerCase() === 'active' ? 'rentalFiles.currentRental' : 'rentalFiles.endedRental';
      return this.activeCycleLease ? 'rentalFiles.currentRental' : this.cycleLeases.length ? 'rentalFiles.endedRental' : 'rentalFiles.waitingLease';
    },
    selectedCycleTitleKey() { return this.selectedCycleIsHistorical ? 'rentalFiles.historyCycleTitle' : this.activeCycleLease ? 'rentalFiles.currentCycleTitle' : this.cycleLeases.length ? 'rentalFiles.endedCycleTitle' : 'rentalFiles.waitingLeaseCycleTitle'; },
    pmaDocument() { return this.mandateDocuments.find(item => String(item.mandateId) === String(this.currentMandate?.id) && String(item.relationType || '').toLowerCase() === 'property_management_agreement_draft') || null; },
    pmaSigned() { return String(this.pmaDocument?.signatureStatus || '').toLowerCase() === 'signed'; },
    pmaSigning() { const document = this.pmaDocument || {}; return { status: this.pmaSigned ? 'signed' : String(document.signatureStatus || 'not_started').toLowerCase(), signerRole: document.signatureSignerRole || '', signingOrder: Number(document.signatureSigningOrder || 0), signerName: document.signatureSignerName || '', signerEmail: document.signatureSignerEmail || '', requestedAt: document.signatureRequestedAt || null, expiresAt: document.signatureExpiresAt || null }; },
    nextPmaSignerRole() { if (!this.pmaDocument || this.pmaSigning.signingOrder < 1) return 'owner'; if (this.pmaSigning.status !== 'signed') return this.pmaSigning.signerRole || 'owner'; if (this.pmaSigning.signingOrder === 1) return 'company'; if (this.pmaSigning.signingOrder === 2) return 'customer_service'; return ''; },
    rentalAppointmentDocument() { return this.mandateDocuments.find(item => String(item.mandateId) === String(this.currentMandate?.id) && ['rental_appointment_draft', 'authorization_draft'].includes(String(item.relationType || '').toLowerCase())) || null; },
    rentalAppointmentSigning() { const document = this.rentalAppointmentDocument || {}; return { status: this.rentalAppointmentSigned ? 'signed' : String(document.signatureStatus || 'not_started').toLowerCase(), signerName: document.signatureSignerName || '', signerEmail: document.signatureSignerEmail || '', requestedAt: document.signatureRequestedAt || null, expiresAt: document.signatureExpiresAt || null }; },
    rentalAppointmentSigned() { return String(this.rentalAppointmentDocument?.signatureStatus || '').toLowerCase() === 'signed'; },
    authorizationSigning() { const document = this.authorizationDocument || {}; const status = this.authorizationSigned ? 'signed' : String(document.signatureStatus || 'not_started').toLowerCase(); return { status, documentId: document.id || null, signerName: document.signatureSignerName || '', signerEmail: document.signatureSignerEmail || '', requestedAt: document.signatureRequestedAt || null, expiresAt: document.signatureExpiresAt || null, signedAt: document.signatureSignedAt || null }; },
    authorizationDocument() { return this.mandateDocuments.find(item => String(item.mandateId) === String(this.currentMandate?.id) && ['management_authorization_draft', 'authorization_draft', 'authorization'].includes(String(item.relationType || '').toLowerCase())) || null; },
    authorizationSigned() { return String(this.authorizationDocument?.signatureStatus || '').toLowerCase() === 'signed'; },
    terminationLetterDocument() { return this.mandateDocuments.find(item => String(item.mandateId) === String(this.currentMandate?.id) && String(item.relationType || '').toLowerCase() === 'termination_letter_draft') || null; },
     terminationLetterSigning() { const document = this.terminationLetterDocument || {}; return { status: this.terminationLetterSigned ? 'signed' : String(document.signatureStatus || 'not_started').toLowerCase(), signerRole: document.signatureSignerRole || '', signingOrder: Number(document.signatureSigningOrder || 0), signerName: document.signatureSignerName || '', signerEmail: document.signatureSignerEmail || '', requestedAt: document.signatureRequestedAt || null, expiresAt: document.signatureExpiresAt || null, signedAt: document.signatureSignedAt || null }; },
     terminationLetterSigned() { const document = this.terminationLetterDocument || {}; return String(document.signatureStatus || '').toLowerCase() === 'signed' && Number(document.signatureSigningOrder || 0) >= 2; },
     rentalRemittanceDocument() { return this.mandateDocuments.find(item => String(item.mandateId) === String(this.currentMandate?.id) && String(item.relationType || '').toLowerCase() === 'rental_remittance_draft') || null; },
     rentalRemittanceSigning() { const document = this.rentalRemittanceDocument || {}; return { status: this.rentalRemittanceSigned ? 'signed' : String(document.signatureStatus || 'not_started').toLowerCase(), signerName: document.signatureSignerName || '', signerEmail: document.signatureSignerEmail || '', requestedAt: document.signatureRequestedAt || null, expiresAt: document.signatureExpiresAt || null, signedAt: document.signatureSignedAt || null }; },
     rentalRemittanceSigned() { return String(this.rentalRemittanceDocument?.signatureStatus || '').toLowerCase() === 'signed'; },
    currentLeaseContracts() { const leaseId = this.currentLease?.id || this.currentLease?.leaseId; return (this.workspace?.contracts || []).filter(item => String(item.leaseId) === String(leaseId)); },
    otrMandateDocument() { return this.mandateDocuments.find(item => String(item.mandateId) === String(this.currentMandate?.id) && ['otr', 'otr_document'].includes(String(item.relationType || item.documentType || '').toLowerCase())) || null; },
    otrContract() {
      const mandateOtr = this.otrMandateDocument;
      if (mandateOtr) return mandateOtr;
      const contracts = this.workspace?.contracts || [];
      const mandateReference = String(this.currentMandate?.mandateNo || this.currentMandate?.id || '');
      return this.currentLeaseContracts.find(item => item.contractType === 'O_LEASE_RESERVATION')
        || contracts.find(item => item.contractType === 'O_LEASE_RESERVATION' && mandateReference && String(item.contractNo || '').includes(mandateReference))
        || null;
    },
    otrSigning() { const document = this.otrMandateDocument || {}; return { status: String(document.signatureStatus || 'not_started').toLowerCase(), signerRole: document.signatureSignerRole || '', signingOrder: Number(document.signatureSigningOrder || 0), signerName: document.signatureSignerName || '', signerEmail: document.signatureSignerEmail || '', requestedAt: document.signatureRequestedAt || null, expiresAt: document.signatureExpiresAt || null }; },
    nextOtrSignerRole() { if (!this.otrMandateDocument || this.otrSigning.signingOrder < 1) return 'tenant'; if (this.otrSigning.status !== 'signed') return this.otrSigning.signerRole || 'tenant'; if (this.otrSigning.signingOrder === 1) return 'tenant_witness'; if (this.otrSigning.signingOrder === 2) return 'owner'; if (this.otrSigning.signingOrder === 3) return 'owner_witness'; return ''; },
    otrSigned() { return Boolean(this.otrMandateDocument && this.otrSigning.status === 'signed' && Number(this.otrSigning.signingOrder) >= 4) || Boolean(!this.otrMandateDocument && this.contractComplete(this.otrContract)); },
    leaseContract() { return this.currentLeaseContracts.find(item => item.contractType === 'L_LEASE') || null; },
    leaseSignatureStatus() { return String(this.currentLease?.signatureStatus || (this.leaseContractComplete ? 'signed' : this.leaseContract ? 'ready_to_sign' : 'not_generated')).toLowerCase(); },
    leaseContractComplete() { return this.contractComplete(this.leaseContract); },
     completedSigningSteps() { return [this.pmaSigned, this.authorizationSigned, this.terminationLetterSigned, this.rentalRemittanceSigned, this.otrSigned, this.leaseContractComplete].filter(Boolean).length; },
    financeDocumentAllSelected() { return Boolean(this.financeDocumentRows.length) && this.financeDocumentSelectedIds.length === this.financeDocumentRows.length; },
    pmaMissingFields() { return missingRequiredFields(this.pmaForm, PMA_REQUIRED_FIELDS).map(item => this.$t(item.labelKey)); },
    rentalAppointmentMissingFields() {
      const required = this.rentalAppointmentForm.hasSecondLandlord ? { ...RENTAL_APPOINTMENT_REQUIRED_FIELDS, ...SECOND_LANDLORD_REQUIRED_FIELDS } : RENTAL_APPOINTMENT_REQUIRED_FIELDS;
      return missingRequiredFields(this.rentalAppointmentForm, required).map(item => this.$t(item.labelKey));
    },
    otrMissingFields() {
      const required = this.otrForm.hasSecondLandlord ? { ...OTR_REQUIRED_FIELDS, ...SECOND_LANDLORD_REQUIRED_FIELDS } : OTR_REQUIRED_FIELDS;
      return missingRequiredFields(this.otrForm, required).map(item => this.$t(item.labelKey));
    },
    leasePaymentNeedsBank() { return /bank|transfer|转账|匯款|汇款/i.test(String(this.leaseForm.paymentMode || '')); },
    leaseMissingFields() {
      const required = this.leasePaymentNeedsBank
        ? { ...LEASE_REQUIRED_FIELDS, bankName: sf('receivingBankName'), bankAccount: sf('receivingBankAccount'), bankBranch: sf('bankBranchAddress') }
        : LEASE_REQUIRED_FIELDS;
      return missingRequiredFields(this.leaseForm, required).map(item => this.$t(item.labelKey));
    },
    authorizationMissingFields() { return missingRequiredFields(this.authorizationForm, AUTHORIZATION_REQUIRED_FIELDS).map(item => this.$t(item.labelKey)); },
     terminationLetterMissingFields() { return missingRequiredFields(this.terminationLetterForm, TERMINATION_LETTER_REQUIRED_FIELDS).map(item => this.$t(item.labelKey)); },
     rentalRemittanceMissingFields() { return missingRequiredFields(this.rentalRemittanceForm, RENTAL_REMITTANCE_REQUIRED_FIELDS).map(item => this.$t(item.labelKey)); },
     templateDesignerName() { return this.templateDesignerType === 'property-management-agreement' ? this.$t('rentalFiles.pmaTitle') : this.templateDesignerType === 'termination-letter' ? '终止通知书' : this.templateDesignerType === 'rental-remittance' ? '租金汇款授权书' : this.$t('rentalFiles.authorizationTitle'); },
    templatePageFields() { return (this.templateLayout?.fields || []).filter(field => Number(field.page) === Number(this.templatePage)); },
    templateSelectedField() { return (this.templateLayout?.fields || []).find(field => field.id === this.templateSelectedFieldId) || null; },
    templatePages() { return Array.from({ length: Number(this.templateLayout?.pages || 0) }, (_, index) => index + 1); },
    templatePageSize() { return this.templateLayout?.pageSizes?.[this.templatePage - 1] || { width: 595, height: 842 }; },
    templateCanvasStyle() { return { width: `${Number(this.templatePageSize.width) * this.templateScale}px`, height: `${Number(this.templatePageSize.height) * this.templateScale}px` }; },
    signingTasks() {
      const noMandate = !this.currentMandate;
      const noLease = !this.currentLease;
      const readOnly = this.selectedCycleIsHistorical;
      const authorizationPending = ['pending', 'sent', 'viewed'].includes(String(this.authorizationSigning.status || '').toLowerCase());
      const pmaPending = ['pending', 'sent', 'viewed'].includes(String(this.pmaSigning.status || '').toLowerCase());
      const otrPending = ['pending', 'sent', 'viewed'].includes(String(this.otrSigning.status || '').toLowerCase());
      const terminationLetterPending = ['pending', 'sent', 'viewed'].includes(String(this.terminationLetterSigning.status || '').toLowerCase());
      const rentalRemittancePending = ['pending', 'sent', 'viewed'].includes(String(this.rentalRemittanceSigning.status || '').toLowerCase());
      const latestHandoverReport = this.files.find(file => file.kind === 'handover');
      const handoverReportCount = this.files.filter(file => file.kind === 'handover').length;
      return [
        { key: 'pma', icon: '管', title: this.$t('rentalFiles.pmaTitle'), description: this.$t('rentalFiles.pmaHint'), fileName: this.pmaDocument?.originalName, actions: ['generate_pma', 'start_pma_signing', 'view_pma_signing_status'], templateType: 'property-management-agreement',
          status: this.pmaSigned ? 'complete' : pmaPending ? 'pending' : noMandate ? 'unavailable' : 'todo', statusLabel: this.pmaSigned ? this.$t('rentalFiles.statusComplete') : pmaPending ? this.$t('rentalFiles.statusPending') : this.$t('rentalFiles.statusNotStarted'),
          detail: noMandate ? this.$t('rentalFiles.createMandateFirst') : this.pmaSigned ? this.$t('rentalFiles.pmaSigned') : pmaPending ? this.$t('rentalFiles.allSignersInvited') : this.pmaDocument ? this.$t('rentalFiles.readyForAllSigners') : this.$t('rentalFiles.notGenerated'),
          action: !readOnly && !noMandate, actionLabel: this.pmaDocument ? (this.packageStarted(this.pmaSigning) ? this.$t('rentalFiles.restartSigning') : this.$t('rentalFiles.startSigningPackage')) : this.$t('rentalFiles.generatePma'), canRegenerate: !readOnly && Boolean(this.pmaDocument), disabled: noMandate || readOnly },
        { key: 'authorization', icon: '授', title: this.$t('rentalFiles.authorizationTitle'), description: this.$t('rentalFiles.authorizationHint'), fileName: this.authorizationDocument?.originalName, actions: ['generate_authorization', 'start_authorization_signing', 'view_signing_status'],
          templateType: 'management-authorization',
         status: this.authorizationSigned ? 'complete' : authorizationPending ? 'pending' : noMandate ? 'unavailable' : 'todo', statusLabel: this.authorizationSigned ? this.$t('rentalFiles.statusComplete') : authorizationPending ? this.$t('rentalFiles.statusPending') : this.$t('rentalFiles.statusNotStarted'),
           detail: noMandate ? this.$t('rentalFiles.createMandateFirst') : this.authorizationSigned ? this.$t('rentalFiles.authorizationSigned') : authorizationPending ? this.$t('rentalFiles.authorizationAwaiting') : this.authorizationDocument ? this.$t('rentalFiles.readyToSign') : this.$t('rentalFiles.notGenerated'),
           action: !readOnly && !noMandate && !this.authorizationSigned, actionLabel: authorizationPending ? this.$t('rentalFiles.viewSigningStatus') : this.authorizationDocument ? this.$t('rentalFiles.startSigning') : this.$t('rentalFiles.generateAuthorization'), canRegenerate: !readOnly && Boolean(this.authorizationDocument), disabled: noMandate || readOnly },
          { key: 'terminationLetter', icon: '终', title: '终止通知书', description: '屋主终止代租管合约并确认结算账户。', fileName: this.terminationLetterDocument?.originalName, actions: ['generate_termination_letter', 'start_termination_letter_signing', 'view_termination_letter_signing_status'], templateType: 'termination-letter',
            status: this.terminationLetterSigned ? 'complete' : terminationLetterPending ? 'pending' : noMandate ? 'unavailable' : 'todo', statusLabel: this.terminationLetterSigned ? this.$t('rentalFiles.statusComplete') : terminationLetterPending ? this.$t('rentalFiles.statusPending') : this.$t('rentalFiles.statusNotStarted'),
            detail: noMandate ? this.$t('rentalFiles.createMandateFirst') : this.terminationLetterSigned ? '业主及公司授权代表已完成终止通知书签署' : terminationLetterPending ? this.$t('rentalFiles.allSignersInvited') : this.terminationLetterDocument ? this.$t('rentalFiles.readyForAllSigners') : this.$t('rentalFiles.notGenerated'),
            action: !readOnly && !noMandate, actionLabel: this.terminationLetterDocument ? (this.packageStarted(this.terminationLetterSigning) ? this.$t('rentalFiles.restartSigning') : this.$t('rentalFiles.startSigningPackage')) : '生成终止通知书', canRegenerate: !readOnly && Boolean(this.terminationLetterDocument), disabled: noMandate || readOnly },
          { key: 'rentalRemittance', icon: '汇', title: '租金汇款授权书', description: '屋主授权 CCPS 将每月租金汇入指定收款银行账户。', fileName: this.rentalRemittanceDocument?.originalName, actions: ['generate_rental_remittance', 'start_rental_remittance_signing', 'view_rental_remittance_signing_status'], templateType: 'rental-remittance',
            status: this.rentalRemittanceSigned ? 'complete' : rentalRemittancePending ? 'pending' : noMandate ? 'unavailable' : 'todo', statusLabel: this.rentalRemittanceSigned ? this.$t('rentalFiles.statusComplete') : rentalRemittancePending ? this.$t('rentalFiles.statusPending') : this.$t('rentalFiles.statusNotStarted'),
            detail: noMandate ? this.$t('rentalFiles.createMandateFirst') : this.rentalRemittanceSigned ? '屋主已完成租金汇款授权书签署' : rentalRemittancePending ? '签署邀请已发出，等待屋主签署' : this.rentalRemittanceDocument ? this.$t('rentalFiles.readyToSign') : this.$t('rentalFiles.notGenerated'),
            action: !readOnly && !noMandate, actionLabel: rentalRemittancePending ? this.$t('rentalFiles.viewSigningStatus') : this.rentalRemittanceDocument ? this.$t('rentalFiles.startSigning') : '生成租金汇款授权书', canRegenerate: !readOnly && Boolean(this.rentalRemittanceDocument), disabled: noMandate || readOnly },
         { key: 'otr', icon: '任', title: this.$t('rentalFiles.rentalAppointmentTitle'), description: this.$t('rentalFiles.rentalAppointmentHint'), fileName: this.displayRentalAppointmentFileName(this.otrContract), actions: ['generate_otr', 'start_otr_signing', 'view_otr_signing_status'], status: this.otrSigned ? 'complete' : otrPending ? 'pending' : noMandate ? 'unavailable' : 'todo', statusLabel: this.otrSigned ? this.$t('rentalFiles.statusComplete') : otrPending ? this.$t('rentalFiles.statusPending') : this.$t('rentalFiles.statusNotStarted'), detail: noMandate ? this.$t('rentalFiles.createMandateFirst') : this.otrSigned ? this.$t('rentalFiles.otrSigned') : otrPending ? this.$t('rentalFiles.allSignersInvited') : this.otrMandateDocument ? this.$t('rentalFiles.readyForAllSigners') : this.$t('rentalFiles.notGenerated'), action: !readOnly && !noMandate, actionLabel: this.otrMandateDocument ? (this.packageStarted(this.otrSigning) ? this.$t('rentalFiles.restartSigning') : this.$t('rentalFiles.startSigningPackage')) : this.$t('rentalFiles.generateOtr'), canRegenerate: !readOnly && Boolean(this.otrContract), disabled: noMandate || readOnly },
        { key: 'lease', icon: '签', title: this.$t('rentalFiles.leaseTitle'), description: this.$t('rentalFiles.leaseHint'), fileName: this.leaseContract?.originalName, actions: ['sign_lease_contract'], status: this.leaseContractComplete ? 'complete' : noLease ? 'unavailable' : this.leaseContract ? 'pending' : 'todo', statusLabel: this.leaseContractComplete ? this.$t('rentalFiles.statusComplete') : this.leaseContract ? this.$t('rentalFiles.statusPending') : this.$t('rentalFiles.statusNotStarted'), detail: noLease ? this.$t('rentalFiles.createLeaseFirst') : this.leaseContractComplete ? this.$t('rentalFiles.leaseSigned') : this.leaseContract ? this.$t('rentalFiles.leaseAwaiting') : this.$t('rentalFiles.notGenerated'), action: !readOnly && !noLease, actionLabel: this.leaseContract ? (['pending', 'signed'].includes(this.leaseSignatureStatus) ? this.$t('rentalFiles.restartSigning') : this.$t('rentalFiles.startSigningPackage')) : this.$t('rentalFiles.generateAndSignLease'), canRegenerate: !readOnly && Boolean(this.leaseContract), disabled: noLease || readOnly },
        { key: 'handoverReport', icon: '交', title: this.$t('rentalFiles.handoverReportTitle'), description: this.$t('rentalFiles.handoverReportHint'), fileName: latestHandoverReport?.name, actions: ['generate_handover_report'], status: latestHandoverReport ? 'complete' : noLease ? 'unavailable' : 'todo', statusLabel: latestHandoverReport ? this.$t('rentalFiles.statusGenerated') : this.$t('rentalFiles.statusNotStarted'), detail: noLease ? this.$t('rentalFiles.createLeaseFirst') : handoverReportCount ? this.$t('rentalFiles.handoverReportCount', { count: handoverReportCount }) : this.$t('rentalFiles.notGenerated'), action: !readOnly && !noLease, actionLabel: latestHandoverReport ? this.$t('rentalFiles.regenerateHandoverReport') : this.$t('rentalFiles.generateHandoverReport'), canRegenerate: false, disabled: noLease || readOnly },
        { key: 'financeDocuments', icon: this.$t('rentalFiles.financeDocumentIcon'), title: this.$t('rentalFiles.financeDocumentTitle'), description: this.$t('rentalFiles.financeDocumentHint'), fileName: this.$t('rentalFiles.financeDocumentOnDemand'), actions: ['generate_finance_document'], status: 'todo', statusLabel: this.$t('rentalFiles.financeDocumentAvailable'), detail: this.$t('rentalFiles.financeDocumentDetail'), action: true, actionLabel: this.$t('rentalFiles.openFinanceDocumentGenerator'), canRegenerate: false, disabled: false },
      ];
    },
    activeSignerRole() { return this.signingPanel === 'pma' ? this.nextPmaSignerRole : this.signingPanel === 'otr' ? this.nextOtrSignerRole : ['rentalAppointment', 'authorization', 'rentalRemittance'].includes(this.signingPanel) ? 'owner' : ''; },
    multiSignerPanel() { return ['pma', 'rentalAppointment', 'terminationLetter', 'otr', 'lease'].includes(this.signingPanel); },
    packageAlreadyStarted() { return this.signingPanel === 'pma' ? this.packageStarted(this.pmaSigning) : this.signingPanel === 'terminationLetter' ? this.packageStarted(this.terminationLetterSigning) : this.signingPanel === 'otr' ? this.packageStarted(this.otrSigning) : this.signingPanel === 'lease' ? ['pending', 'signed'].includes(this.leaseSignatureStatus) : false; },
    activeSigning() { return this.signingPanel.startsWith('pma') ? this.pmaSigning : this.signingPanel.startsWith('rentalAppointment') ? this.rentalAppointmentSigning : this.signingPanel.startsWith('otr') ? this.otrSigning : this.signingPanel.startsWith('terminationLetter') ? this.terminationLetterSigning : this.signingPanel.startsWith('rentalRemittance') ? this.rentalRemittanceSigning : this.authorizationSigning; },
    signingPanelTitle() { return this.signingPanel.endsWith('Status') ? this.$t('rentalFiles.viewSigningStatus') : this.signingPanel === 'pma' ? this.$t('rentalFiles.pmaSigningTitle') : this.signingPanel === 'rentalAppointment' ? this.$t('rentalFiles.rentalAppointmentSigningTitle') : this.signingPanel === 'authorization' ? this.$t('rentalFiles.authorizationSigningTitle') : this.signingPanel === 'terminationLetter' ? '发起终止通知书签署' : this.signingPanel === 'rentalRemittance' ? '发起租金汇款授权书签署' : this.signingPanel === 'otr' ? this.$t('rentalFiles.otrSigningTitle') : this.$t('rentalFiles.leaseSigningTitle'); },
    signingPanelHint() { return this.signingPanel.endsWith('Status') ? this.$t(this.signingPanel.startsWith('otr') ? 'rentalFiles.otrAwaiting' : 'rentalFiles.authorizationAwaiting') : this.multiSignerPanel ? this.$t('rentalFiles.signerPackageHint') : this.$t('rentalFiles.signingPanelHint'); },
    categories() { return CATEGORY_ORDER.filter(category => this.files.some(file => file.category === category)); },
    filteredFiles() { const query = this.fileSearch.toLowerCase(); return this.files.filter(file => (!this.categoryFilter || file.category === this.categoryFilter) && (!query || [file.name, file.reference, file.mandateNo, this.categoryLabel(file.category)].some(value => String(value || '').toLowerCase().includes(query)))); },
    propertyFiles() { const ownerId = Number(this.selectedProperty?.ownerId); const ownerUnitId = Number(this.selectedProperty?.ownerUnitId); return (this.propertyAttachments || []).map(attachment => ({ key: `property-${attachment.id}`, kind: 'property', category: 'property', id: attachment.id, ownerId, ownerUnitId, name: attachment.originalName || attachment.title || this.$t('rentalFiles.unnamedFile'), reference: attachment.title || attachment.remarks, status: attachment.enabled === false ? 'disabled' : 'active', date: attachment.createdAt || attachment.updatedAt })).sort((left, right) => String(right.date || '').localeCompare(String(left.date || ''))); },
  },
  mounted() { this.loadProperties(); this.loadTemplateVersions(); },
  beforeUnmount() { if (this.templateDesignerOpen) this.closeTemplateDesigner(); },
  methods: {
    resetSupplementValidation(key) {
      this.validatedSupplementForms[key] = false;
    },
    validateSupplementForm(key, refName) {
      const form = this.$refs[refName];
      if (!form || !findFirstEmptyRequiredControl(form)) {
        this.resetSupplementValidation(key);
        return true;
      }
      this.validatedSupplementForms[key] = true;
      this.actionError = this.$t('rentalFiles.supplement.common.invalidForm');
      this.$nextTick(() => revealFirstInvalidControl(form));
      return false;
    },
    async loadProperties() {
      this.loading = true; this.loadError = '';
      try {
        const [propertyResponse, mandateResponse] = await Promise.all([fetchAdminProperties({ page: 1, pageSize: 500 }), fetchAdminRentalMandates({ page: 1, pageSize: 500 })]);
        this.mandates = mandateResponse?.rows || [];
        const rentalUnitIds = new Set(this.mandates.map(item => String(item.ownerUnitId)));
        this.properties = (propertyResponse?.rows || []).map(item => ({ ...item.property, ownerId: item.ownerId, ownerName: item.ownerName, rowKey: `${item.ownerId}-${item.property.ownerUnitId}` })).filter(property => rentalUnitIds.has(String(property.ownerUnitId)));
        if (!this.projectOptions.includes(this.selectedProjectName)) this.selectedProjectName = '';
        const requestedUnitId = new URLSearchParams(window.location.search).get('ownerUnitId');
        const selected = this.properties.find(property => String(property.ownerUnitId) === String(requestedUnitId)) || this.properties.find(property => property.rowKey === this.selectedPropertyKey) || this.properties[0];
        if (selected) await this.selectProperty(selected); else { this.selectedProperty = null; this.files = []; }
      } catch (error) { this.loadError = error.message || this.$t('rentalFiles.loadFailed'); }
      finally { this.loading = false; }
    },
    async selectProperty(property) { this.selectedProperty = property; this.selectedPropertyKey = property.rowKey; this.selectedCycleId = ''; this.selectedLeaseId = ''; this.rentalRemittanceForm = emptyRentalRemittanceForm(); this.fileSearch = ''; this.categoryFilter = ''; this.signingPanel = ''; await this.loadSigningWorkspace(); },
    async loadSigningWorkspace() {
      if (!this.selectedProperty) return;
      this.workspaceLoading = true; this.workspaceError = ''; this.actionError = '';
      try {
        const property = this.selectedProperty; const ownerId = Number(property.ownerId); const ownerUnitId = Number(property.ownerUnitId); const unitId = property.unitId || property.id || property.ownerUnitId;
        const safe = (promise, fallback = []) => promise.catch(() => fallback);
        const groupsPromise = Promise.all(
          this.propertyMandates.map(mandate => safe(fetchAdminRentalMandateDocuments(mandate.id))
            .then(documents => documents.map(document => ({ ...document, mandateId: mandate.id, mandateNo: mandate.mandateNo })))),
        );
        const [workspace, documents, attachments, handovers] = await Promise.all([fetchAdminPropertyWorkspaceById(unitId), groupsPromise, safe(fetchAdminPropertyAttachments(ownerId, ownerUnitId)), safe(fetchAdminPropertyHandoverReports(ownerId, ownerUnitId))]);
        this.workspace = workspace || {}; this.mandateDocuments = documents.flat(); this.propertyAttachments = attachments; this.handoverReports = handovers;
        if (!this.rentalCycles.some(cycle => String(cycle.id) === String(this.selectedCycleId))) this.selectedCycleId = String(this.activeCycle?.id || this.rentalCycles[0]?.id || '');
        if (!this.cycleLeases.some(lease => String(lease.id || lease.leaseId) === String(this.selectedLeaseId))) { const lease = this.cycleLeases.find(item => String(item.id || item.leaseId) === String(this.requestedLeaseId)) || this.cycleLeases.find(item => String(item.status || '').toLowerCase() === 'active') || this.cycleLeases[0]; this.selectedLeaseId = String(lease?.id || lease?.leaseId || ''); }
        this.buildFiles();
        const signing = this.authorizationSigning; this.signerForm = { name: signing.signerName || this.currentMandate?.ownerName || property.ownerName || '', email: signing.signerEmail || this.currentMandate?.ownerEmail || '' };
      } catch (error) { this.workspaceError = error.message || this.$t('rentalFiles.loadFailed'); }
      finally { this.workspaceLoading = false; }
    },
    displayRentalAppointmentFileName(fileOrName) {
      const name = typeof fileOrName === 'string'
        ? fileOrName
        : fileOrName?.originalName || fileOrName?.contractNo || fileOrName?.documentNo || '';
      return String(name).replace(/OTR\s*出价函/gi, '租赁委托书').replace(/^otr(?=\.pdf$)/i, '租赁委托书');
    },
    buildFiles() {
      const files = []; const mandate = this.currentMandate;
      if (!mandate) { this.files = []; return; }
      const ownerId = Number(this.selectedProperty.ownerId); const ownerUnitId = Number(this.selectedProperty.ownerUnitId); const mandateId = String(mandate.id); const mandateReference = String(mandate.mandateNo || mandate.id); const cycleLeaseIds = new Set(this.cycleLeases.map(lease => String(lease.id || lease.leaseId)));
      for (const document of this.mandateDocuments.filter(item => String(item.mandateId) === mandateId)) {
        const relation = String(document.relationType || document.documentType || '').toLowerCase();
            const category = relation.includes('property_management_agreement') ? 'management' : relation.includes('rental_appointment') || relation === 'authorization_draft' ? 'appointment' : relation.includes('termination_letter') ? 'termination' : relation.includes('rental_remittance') ? 'remittance' : relation.startsWith('otr') ? 'otr' : 'authorization';
        const name = document.originalName || document.documentNo || this.$t('rentalFiles.unnamedFile');
        files.push({ key: `mandate-${document.mandateId}-${document.id}`, kind: 'mandate', category, id: document.id, mandateId: document.mandateId, mandateNo: document.mandateNo || mandate.mandateNo, name: category === 'otr' ? this.displayRentalAppointmentFileName(name) : name, reference: document.documentNo || document.relationType, documentType: document.documentType, relationType: document.relationType, status: document.signatureStatus || document.status || document.relationType, date: document.createdAt || document.updatedAt });
      }
      for (const contract of (this.workspace?.contracts || []).filter(item => cycleLeaseIds.has(String(item.leaseId)) || (item.contractType === 'O_LEASE_RESERVATION' && mandateReference && String(item.contractNo || '').includes(mandateReference)))) {
        const category = contract.contractType === 'O_LEASE_RESERVATION' ? 'otr' : 'contract';
        const name = contract.originalName || contract.contractNo || this.$t('rentalFiles.contractFile');
        files.push({ key: `contract-${contract.id}`, kind: 'contract', category, id: contract.id, leaseId: contract.leaseId, contractType: contract.contractType, signedFile: contract.contractType === 'L_LEASE' && this.contractComplete(contract), ownerId, ownerUnitId, mandateNo: mandate.mandateNo, name: category === 'otr' ? this.displayRentalAppointmentFileName(name) : name, reference: contract.contractNo || contract.contractType, status: contract.status, date: contract.createdAt || contract.updatedAt || contract.signedDate });
      }
      const mandateStartedAt = mandate.createdAt || mandate.startDate; const nextMandate = this.propertyMandates.filter(item => String(item.id) !== mandateId && String(item.createdAt || item.startDate || '') > String(mandateStartedAt || '')).sort((left, right) => String(left.createdAt || left.startDate || '').localeCompare(String(right.createdAt || right.startDate || '')))[0] || null;
      for (const report of this.handoverReports.filter(item => reportBelongsToMandate(item, mandate, nextMandate))) files.push({ key: `handover-${report.id}`, kind: 'handover', category: 'handover', id: report.id, ownerId, ownerUnitId, mandateNo: mandate.mandateNo, name: report.originalName || report.title || this.$t('rentalFiles.handoverFile'), reference: report.reportNo || report.title, status: report.completed ? 'completed' : report.status, date: report.createdAt || report.reportDate });
      this.files = files.sort((left, right) => String(right.date || '').localeCompare(String(left.date || '')));
    },
    selectCycle() {
      this.rentalRemittanceForm = emptyRentalRemittanceForm(); this.selectedLeaseId = ''; const lease = this.cycleLeases.find(item => String(item.status || '').toLowerCase() === 'active') || this.cycleLeases[0]; this.selectedLeaseId = String(lease?.id || lease?.leaseId || ''); this.fileSearch = ''; this.categoryFilter = ''; this.signingPanel = ''; this.actionError = ''; this.buildFiles();
      const signing = this.authorizationSigning; this.signerForm = { name: signing.signerName || this.currentMandate?.ownerName || this.selectedProperty?.ownerName || '', email: signing.signerEmail || this.currentMandate?.ownerEmail || '' };
    },
    selectLease() { this.fileSearch = ''; this.categoryFilter = ''; this.signingPanel = ''; this.actionError = ''; this.buildFiles(); },
    leaseStatusLabel(lease) { const status = String(lease?.status || '').toLowerCase(); if (status === 'active') return this.$t('rentalFiles.leaseStatusActive'); if (['terminated', 'transferred', 'completed', 'cancelled', 'expired'].includes(status)) return this.$t('rentalFiles.leaseStatusEnded'); return this.$t('rentalFiles.leaseStatusPending'); },
    leaseOptionLabel(lease) { const space = lease.rentalSpaceName || '整套房产'; const tenant = lease.tenantName || '未命名租客'; const number = lease.leaseNo || `LEASE-${lease.leaseId || lease.id}`; return `${space} · ${tenant} · ${number} · ${this.leaseStatusLabel(lease)}`; },
    cycleOptionLabel(cycle) {
      const current = String(cycle.id) === String(this.activeCycle?.id); const leases = (this.workspace?.leases || []).filter(lease => String(lease.rentalMandateId || lease.mandateId) === String(cycle.id)); const hasActiveLease = leases.some(lease => String(lease.status || '').toLowerCase() === 'active'); const type = this.$t(!current ? 'rentalFiles.historyCycleOption' : hasActiveLease ? 'rentalFiles.currentCycleOption' : leases.length ? 'rentalFiles.endedCycleOption' : 'rentalFiles.waitingLeaseCycleOption'); const period = [cycle.startDate, cycle.endDate].filter(Boolean).join(' ~ ');
      return `${type} · ${cycle.mandateNo || `RM-${cycle.id}`}${period ? ` · ${period}` : ''}`;
    },
    contractComplete(contract) { return Boolean(contract && (COMPLETE_CONTRACT_STATUSES.has(String(contract.status || '').toLowerCase()) || contract.signedDate)); },
    packageStarted(signing) { return Number(signing?.signingOrder || 0) > 0 || !['', 'not_started'].includes(String(signing?.status || '').toLowerCase()); },
    async runSigningTask(task) {
      if (task.key === 'financeDocuments') { await this.openFinanceDocumentPanel(); return; }
      if (task.key === 'handoverReport') { await this.openHandoverReportForm(); return; }
      if (this.selectedCycleIsHistorical) return;
      this.actionError = ''; this.generatedSigningLinks = [];
      if (task.key === 'pma') {
        if (this.pmaDocument) { await this.openSignerPackage('pma'); return; }
        await this.openPmaForm(); return;
      }
      if (task.key === 'rentalAppointment') {
        if (['pending', 'sent', 'viewed'].includes(String(this.rentalAppointmentSigning.status || '').toLowerCase())) { this.signingPanel = 'rentalAppointmentStatus'; return; }
        if (this.rentalAppointmentDocument) { await this.openSignerPackage('rentalAppointment'); return; }
        await this.openRentalAppointmentForm(); return;
      }
      if (task.key === 'authorization') {
        if (['pending', 'sent', 'viewed'].includes(String(this.authorizationSigning.status || '').toLowerCase())) { this.signingPanel = 'authorizationStatus'; return; }
        if (this.authorizationDocument) { await this.startSigningDirect('authorization'); return; }
        this.openAuthorizationForm(); return;
      }
      if (task.key === 'terminationLetter') {
        if (this.terminationLetterDocument) { await this.openSignerPackage('terminationLetter'); return; }
        await this.openTerminationLetterForm(); return;
      }
      if (task.key === 'rentalRemittance') {
        if (['pending', 'sent', 'viewed'].includes(String(this.rentalRemittanceSigning.status || '').toLowerCase())) { this.signingPanel = 'rentalRemittanceStatus'; return; }
        if (this.rentalRemittanceDocument) { await this.startSigningDirect('rentalRemittance'); return; }
        await this.openRentalRemittanceForm(); return;
      }
      if (task.key === 'otr') {
        if (this.otrMandateDocument) { await this.openSignerPackage('otr'); return; }
        this.openOtrForm(); return;
      }
      if (task.key === 'lease') {
        if (!this.leaseContract) { this.leaseGenerationStartsSigning = true; await this.openLeaseForm(); return; }
        if (this.leaseContract) await this.openSignerPackage('lease');
      }
    },
    signerNamesMissing(type) {
      const fields = {
        pma: [this.pmaForm.ownerSignerName, this.pmaForm.companySignerName, this.pmaForm.customerServiceSignerName],
        rentalAppointment: [this.rentalAppointmentForm.ownerSignerName, this.rentalAppointmentForm.witnessName,
          ...(this.rentalAppointmentForm.hasSecondLandlord ? [this.rentalAppointmentForm.secondLandlordName] : [])],
        authorization: [this.authorizationForm.ownerSignerName],
        terminationLetter: [this.terminationLetterForm.ownerSignerName, this.terminationLetterForm.companySignerName],
        rentalRemittance: [this.rentalRemittanceForm.ownerSignerName],
        otr: [this.otrForm.tenantName, this.otrForm.tenantWitnessName, this.otrForm.landlordName,
          this.otrForm.landlordWitnessName,
          ...(this.otrForm.hasSecondLandlord ? [this.otrForm.secondLandlordName] : [])],
        lease: [this.leaseForm.ownerSignerName, this.leaseForm.ownerWitnessSignerName, this.leaseForm.tenantSignerName, this.leaseForm.tenantWitnessSignerName],
      }[type] || [];
      return !fields.length || fields.some(value => !String(value || '').trim());
    },
    singleSignerName(type) {
      return {
        rentalAppointment: this.rentalAppointmentForm.ownerSignerName,
        authorization: this.authorizationForm.ownerSignerName,
        rentalRemittance: this.rentalRemittanceForm.ownerSignerName,
      }[type] || '';
    },
    async openSupplementForm(type) {
      if (type === 'pma') await this.openPmaForm(false);
      else if (type === 'rentalAppointment') await this.openRentalAppointmentForm(false);
      else if (type === 'authorization') this.openAuthorizationForm(false);
      else if (type === 'terminationLetter') await this.openTerminationLetterForm(false);
      else if (type === 'rentalRemittance') await this.openRentalRemittanceForm(false);
      else if (type === 'otr') this.openOtrForm(false);
      else if (type === 'lease') await this.openLeaseForm(false);
    },
    signerPackageDefaults(type) {
      if (type === 'pma') {
        return [
          { signerRole: 'owner', signerName: this.pmaForm.ownerSignerName || this.pmaForm.landlordName || this.currentMandate?.ownerName || this.selectedProperty?.ownerName || '', signerEmail: this.currentMandate?.ownerEmail || this.pmaForm.ownerEmail || '' },
          { signerRole: 'company', signerName: this.pmaForm.companySignerName || '', signerEmail: '' },
          { signerRole: 'customer_service', signerName: this.pmaForm.customerServiceSignerName || '', signerEmail: '' },
        ];
      }
      if (type === 'lease') {
        return [
          { signerRole: 'owner', signerName: this.leaseForm.ownerSignerName || '', signerEmail: '' },
          { signerRole: 'owner_witness', signerName: this.leaseForm.ownerWitnessSignerName || '', signerEmail: '' },
          { signerRole: 'tenant', signerName: this.leaseForm.tenantSignerName || '', signerEmail: '' },
          { signerRole: 'tenant_witness', signerName: this.leaseForm.tenantWitnessSignerName || '', signerEmail: '' },
        ];
      }
      if (type === 'rentalAppointment') {
        const signers = [
          { signerRole: 'owner', signerName: this.rentalAppointmentForm.ownerSignerName || this.rentalAppointmentForm.landlordName || '', signerEmail: '' },
        ];
        if (this.rentalAppointmentForm.hasSecondLandlord) signers.push(
          { signerRole: 'second_owner', signerName: this.rentalAppointmentForm.secondLandlordName || '', signerEmail: '' });
        signers.push({ signerRole: 'witness', signerName: this.rentalAppointmentForm.witnessName || '', signerEmail: '' });
        return signers;
      }
      if (type === 'terminationLetter') {
        return [
          { signerRole: 'owner', signerName: this.terminationLetterForm.ownerSignerName || this.terminationLetterForm.landlordName || '', signerEmail: this.terminationLetterForm.ownerEmail || '' },
          { signerRole: 'company', signerName: this.terminationLetterForm.companySignerName || '', signerEmail: '' },
        ];
      }
      if (type === 'otr') {
        const signers = [
        { signerRole: 'tenant', signerName: this.otrForm.tenantName || '', signerEmail: '' },
        { signerRole: 'tenant_witness', signerName: this.otrForm.tenantWitnessName || '', signerEmail: '' },
        { signerRole: 'owner', signerName: this.otrForm.landlordName || '', signerEmail: '' },
        { signerRole: 'owner_witness', signerName: this.otrForm.landlordWitnessName || '', signerEmail: '' },
        ];
        if (this.otrForm.hasSecondLandlord) signers.push(
          { signerRole: 'second_owner', signerName: this.otrForm.secondLandlordName || '', signerEmail: '' });
        return signers;
      }
      return [];
    },
    async openSignerPackage(type) {
      const document = type === 'pma' ? this.pmaDocument : type === 'rentalAppointment' ? this.rentalAppointmentDocument : type === 'terminationLetter' ? this.terminationLetterDocument : type === 'otr' ? this.otrMandateDocument : this.leaseContract;
      const leaseId = this.currentLease?.id || this.currentLease?.leaseId;
      if (type === 'lease' ? !leaseId || !document : !this.currentMandate?.id || !document?.id) return;
      this.signerPackageForm = this.signerPackageDefaults(type); this.generatedSigningLinks = []; this.signingPanel = type;
      this.signerPackageLoading = true; this.actionError = '';
      try {
        const saved = type === 'lease'
          ? await fetchAdminLeaseSignatureParticipants(leaseId)
          : await fetchAdminMandateSignatureParticipants(this.currentMandate.id, document.id);
        const byRole = new Map((saved || []).map(signer => [signer.signerRole, signer]));
        if (['rentalAppointment', 'otr'].includes(type) && byRole.has('second_owner')
          && !this.signerPackageForm.some(signer => signer.signerRole === 'second_owner')) {
          this.signerPackageForm.splice(1, 0, { signerRole: 'second_owner', signerName: '', signerEmail: '' });
        }
        this.signerPackageForm = this.signerPackageForm.map(signer => {
          const previous = byRole.get(signer.signerRole);
          return previous ? { ...signer, signerName: signer.signerName || previous.signerName, signerEmail: previous.signerEmail || signer.signerEmail } : signer;
        });
      } catch (error) {
        this.actionError = error.message || this.$t('rentalFiles.signerPackageLoadFailed');
      } finally {
        this.signerPackageLoading = false;
      }
      if (!this.actionError && this.signerPackageForm.some(signer => !String(signer.signerName || '').trim())) {
        this.signingPanel = ''; await this.openSupplementForm(type); return;
      }
      if (!this.actionError) await this.submitSigning();
    },
    async startSigningDirect(type) {
      if (this.signerNamesMissing(type)) { await this.openSupplementForm(type); return; }
      if (['pma', 'rentalAppointment', 'terminationLetter', 'otr', 'lease'].includes(type)) { await this.openSignerPackage(type); return; }
      this.signerForm = { name: this.singleSignerName(type), email: '' };
      this.generatedSigningLinks = [];
      this.signingPanel = type;
      await this.submitSigning();
    },
    async revealGeneratedSigningLinks() {
      await this.$nextTick();
      const target = Array.isArray(this.$refs.signingLinkResults) ? this.$refs.signingLinkResults[0] : this.$refs.signingLinkResults;
      if (!target) return;
      target.scrollIntoView({ behavior: 'smooth', block: 'start' });
      target.focus({ preventScroll: true });
    },
    async regenerateTask(task) {
      if (this.selectedCycleIsHistorical || !task?.canRegenerate || this.actionBusy) return;
      if (task.key === 'pma') { await this.openPmaForm(false); return; }
      if (task.key === 'rentalAppointment') { await this.openRentalAppointmentForm(false); return; }
      if (task.key === 'authorization') { this.openAuthorizationForm(false); return; }
      if (task.key === 'terminationLetter') { await this.openTerminationLetterForm(false); return; }
      if (task.key === 'rentalRemittance') { await this.openRentalRemittanceForm(false); return; }
      if (task.key === 'otr') { this.openOtrForm(false); return; }
      if (task.key === 'lease') { this.leaseGenerationStartsSigning = false; await this.openLeaseForm(false); }
    },
    async editTask(task) {
      if (this.selectedCycleIsHistorical || !task?.canRegenerate || this.actionBusy) return;
      if (task.key === 'pma') { await this.openPmaForm(false); return; }
      if (task.key === 'rentalAppointment') { await this.openRentalAppointmentForm(false); return; }
      if (task.key === 'authorization') { this.openAuthorizationForm(false); return; }
      if (task.key === 'terminationLetter') { await this.openTerminationLetterForm(false); return; }
      if (task.key === 'rentalRemittance') { await this.openRentalRemittanceForm(false); return; }
      if (task.key === 'otr') { this.openOtrForm(false); return; }
      if (task.key === 'lease') { this.leaseGenerationStartsSigning = false; await this.openLeaseForm(false); }
    },
    async updateClausesTask(task) {
      if (this.selectedCycleIsHistorical || !task?.templateType || !task.canRegenerate || this.actionBusy) return;
      this.actionError = '';
      try {
        // Load the existing master data into the generation model, but do not save
        // any owner/property fields back. Only the currently active PDF template
        // (the contract clauses) is changed; the previous document remains in history.
        if (task.key === 'pma') {
          await this.openPmaForm(false);
          if (this.actionError) return;
          await this.generatePma({ preserveMasterData: true });
        } else if (task.key === 'authorization') {
          this.openAuthorizationForm(false);
          await this.generateAuthorization({ preserveMasterData: true });
        } else if (task.key === 'terminationLetter') {
          await this.openTerminationLetterForm(false);
          if (this.actionError) return;
          await this.generateTerminationLetter({ preserveMasterData: true });
        } else if (task.key === 'rentalRemittance') {
          await this.openRentalRemittanceForm(false);
          if (this.actionError) return;
          await this.generateRentalRemittance({ preserveMasterData: true });
        }
      } catch (error) { this.actionError = error.message || '新版合约生成失败'; }
    },
    templateFields() {
      const mandate = this.currentMandate || {}; const property = this.selectedProperty || {}; const owner = this.pmaOwnerProfile || this.workspace?.owner || {};
      return {
        caseNo: mandate.mandateNo || (mandate.id ? `RM-${mandate.id}` : ''),
        projectName: property.projectName || '', unitNo: property.unitNo || '', propertyAddress: property.address || property.fullAddress || this.propertyTitle,
        landlordName: owner.fullName || mandate.ownerName || property.ownerName || '',
        landlordIdentity: owner.identityNo || owner.passportNo || mandate.ownerIdentityNo || mandate.ownerIdentity || property.ownerIdentityNo || '',
        ownerEmail: owner.email || mandate.ownerEmail || property.ownerEmail || '', ownerPhone: owner.mobilePhone || owner.phone || mandate.ownerPhone || property.ownerPhone || '',
        ownerAddress: owner.mailingAddress || owner.address || mandate.ownerAddress || property.ownerAddress || '', agreementDate: mandate.startDate || new Date().toISOString().slice(0, 10),
        startDate: mandate.startDate || '', endDate: mandate.endDate || '', managementOffice: property.managementOffice || '',
        bankPayeeName: mandate.bankPayeeName || '', bankName: mandate.bankName || '', bankAccountNo: mandate.bankAccountNo || '', bankSwiftCode: mandate.bankSwiftCode || '',
      };
    },
    tenancyAgreementFields(lease, tenantEmail = lease?.tenantEmail || '', handover = {}) {
      const leaseId = lease?.id || lease?.leaseId;
      const base = this.templateFields();
      const owner = this.workspace?.owner || {};
      const deposit = splitTenantDeposit(lease.depositAmount, lease.monthlyRent,
        lease.utilityDeposit ?? lease.utilityDepositAmount);
      return {
        ...base,
        caseNo: lease.leaseNo || `LEASE-${leaseId}`,
        leaseId: String(leaseId),
        projectName: this.selectedProperty.projectName || '',
        unitNo: this.selectedProperty.unitNo || '',
        propertyAddress: this.leasePropertyAddress,
        landlordName: owner.fullName || this.selectedProperty.ownerName || base.landlordName || '',
        landlordIdentity: owner.identityNo || owner.passportNo || this.currentMandate?.ownerIdentityNo || this.currentMandate?.ownerIdentity || this.selectedProperty.ownerIdentityNo || base.landlordIdentity || '',
        landlordAddress: owner.mailingAddress || owner.address || base.ownerAddress || '',
        tenantName: lease.tenantName || '',
        tenantIdentity: lease.tenantIdentity || lease.identityNo || '',
        tenantPhone: lease.tenantPhone || '',
        tenantEmail: lease.tenantEmail || tenantEmail || '',
        monthlyRent: String(lease.monthlyRent ?? ''),
        advanceRental: String(lease.monthlyRent ?? ''),
        securityDeposit: String(deposit.securityDeposit),
        utilityDeposit: String(deposit.utilityDeposit),
        paymentDay: String(lease.paymentDay ?? ''),
        paymentMode: 'Bank Transfer',
        renewalOption: '',
        specialConditions: '',
        use: 'For Residential use only',
        agreementDate: lease.startDate || '',
        leaseStart: lease.startDate || '',
        leaseEnd: lease.endDate || '',
        bankName: base.bankName || '',
        bankAccount: base.bankAccountNo || '',
        bankBranch: this.currentMandate?.bankAddress || '',
        handoverDate: handover.handoverDate || lease.startDate || '',
        electricityMeter: handover.electricityMeter || '',
        electricityMeterDate: handover.electricityMeterDate || handover.handoverDate || '',
        waterMeter: handover.waterMeter || '',
        waterMeterDate: handover.waterMeterDate || handover.handoverDate || '',
        gasMeter: handover.gasMeter || '',
        gasMeterDate: handover.gasMeterDate || handover.handoverDate || '',
        districtCoolingMeter: handover.districtCoolingMeter || '',
        districtCoolingMeterDate: handover.districtCoolingMeterDate || handover.handoverDate || '',
        otherMeter: handover.otherMeter || '',
        otherMeterDate: handover.otherMeterDate || handover.handoverDate || '',
        attendedByName: '',
        attendedByDesignation: '',
      };
    },
    async loadPmaBankAccount() {
      this.pmaBankAccount = null;
      this.pmaBankAccounts = [];
      const property = this.selectedProperty;
      if (!property?.ownerId || !property?.ownerUnitId) return null;
      const accounts = await fetchAdminPropertyBankAccounts(property.ownerId, property.ownerUnitId);
      this.pmaBankAccounts = accounts || [];
      this.pmaBankAccount = this.pmaBankAccounts[0] || null;
      return this.pmaBankAccount;
    },
    applyPmaBankAccount() {
      const account = this.pmaBankAccounts.find(item => String(item.id) === String(this.pmaForm.bankAccountId));
      if (!account) return;
      this.pmaBankAccount = account;
      Object.assign(this.pmaForm, {
        bankPayeeName: account.paymentName || '',
        bankName: account.itemName || '',
        bankAddress: account.bankAddress || '',
        bankBranchCode: account.branchCode || '',
        bankAccountNo: account.accountNo || '',
        bankSwiftCode: account.swiftCode || '',
      });
    },
    async openPmaForm(reset = false) {
      if (!this.currentMandate?.id || this.selectedCycleIsHistorical) return;
      this.actionError = '';
      this.resetSupplementValidation('pma');
      if (reset) {
        this.pmaOwnerProfile = null;
        try { await this.loadPmaBankAccount(); } catch (_) { this.pmaBankAccounts = []; this.pmaBankAccount = null; }
        this.pmaForm = emptyPmaForm();
        this.pmaFormOpen = true;
        return;
      }
      let account;
      let owner;
      try {
        [account, owner] = await Promise.all([
          this.loadPmaBankAccount().then(result => result || {}),
          this.selectedProperty?.ownerId ? fetchAdminOwner(this.selectedProperty.ownerId) : Promise.resolve(null),
        ]);
      }
      catch (error) { this.actionError = error.message || this.$t('rentalFiles.pmaBankLoadFailed'); return; }
      this.pmaOwnerProfile = owner;
      const fields = this.templateFields();
      const previous = this.pmaForm || emptyPmaForm();
      this.pmaForm = reset ? emptyPmaForm() : {
        ...emptyPmaForm(), ...fields,
        ownerSignerName: previous.ownerSignerName || owner?.fullName || fields.landlordName || '',
        companySignerName: previous.companySignerName || this.currentMandate?.responsibleUserName || '',
        customerServiceSignerName: previous.customerServiceSignerName || '',
        landlordName: owner?.fullName || fields.landlordName || '',
        landlordIdentity: owner?.identityNo || owner?.passportNo || fields.landlordIdentity || '',
        ownerAddress: owner?.mailingAddress || fields.ownerAddress || '',
        ownerEmail: owner?.email || fields.ownerEmail || '',
        ownerPhone: owner?.mobilePhone || owner?.phone || fields.ownerPhone || '',
        agreementDate: fields.agreementDate || new Date().toISOString().slice(0, 10),
        bankPayeeName: account.paymentName || fields.bankPayeeName || '',
        bankName: account.itemName || fields.bankName || '',
        bankAddress: account.bankAddress || '',
        bankBranchCode: account.branchCode || '',
        bankAccountNo: account.accountNo || fields.bankAccountNo || '',
        bankSwiftCode: account.swiftCode || fields.bankSwiftCode || '',
        bankAccountId: account.id ? String(account.id) : '',
      };
      this.pmaFormOpen = true;
    },
    closePmaForm() { if (!this.actionBusy) { this.pmaFormOpen = false; this.resetSupplementValidation('pma'); this.actionError = ''; } },
    async openLeaseForm(reset = false) {
      if (!this.currentLease || this.selectedCycleIsHistorical || this.actionBusy) return;
      this.actionError = '';
      this.resetSupplementValidation('lease');
      this.leasePhotoFiles = [];
      if (reset) {
        try { await Promise.all([this.loadLeasePropertyPhotos(), this.loadLeaseHandoverChecklist()]); }
        catch (error) { this.actionError = error.message || this.$t('rentalFiles.actionFailed'); return; }
        this.selectAllLeasePhotos();
        this.selectEnabledLeaseChecklist();
        this.leaseForm = emptyLeaseForm();
        this.leaseFormOpen = true;
        return;
      }
      let handover = {};
      let savedFields = {};
      try {
        const leaseId = this.currentLease?.id || this.currentLease?.leaseId;
        [handover, savedFields] = await Promise.all([
          this.currentMandate?.id ? fetchAdminPropertyHandover(this.currentMandate.id).catch(() => ({})) : Promise.resolve({}),
          leaseId ? fetchAdminLeaseAgreementDetails(leaseId).then(result => result?.fields || result || {}).catch(() => ({})) : Promise.resolve({}),
          this.loadLeasePropertyPhotos(),
          this.loadLeaseHandoverChecklist(),
        ]);
      } catch (error) {
        this.actionError = error.message || '租赁合同资料读取失败';
        return;
      }
      const fields = this.tenancyAgreementFields(this.currentLease, undefined, handover);
      const previous = this.leaseForm || emptyLeaseForm();
      this.leaseForm = {
        ...emptyLeaseForm(),
        ownerSignerName: savedFields.ownerSignerName || previous.ownerSignerName || fields.landlordName || '',
        ownerWitnessSignerName: savedFields.ownerWitnessSignerName || previous.ownerWitnessSignerName || '',
        tenantSignerName: savedFields.tenantSignerName || previous.tenantSignerName || fields.tenantName || '',
        tenantWitnessSignerName: savedFields.tenantWitnessSignerName || previous.tenantWitnessSignerName || '',
        landlordAddress: savedFields.landlordAddress || previous.landlordAddress || fields.landlordAddress || '',
        tenantIdentity: savedFields.tenantIdentity || previous.tenantIdentity || fields.tenantIdentity || '',
        tenantPhone: savedFields.tenantPhone || previous.tenantPhone || fields.tenantPhone || '',
        tenantEmail: savedFields.tenantEmail || previous.tenantEmail || fields.tenantEmail || '',
        tenantAddress: savedFields.tenantAddress || previous.tenantAddress || fields.tenantAddress || '',
        bankName: savedFields.bankName || previous.bankName || fields.bankName || '',
        bankAccount: savedFields.bankAccount || previous.bankAccount || fields.bankAccount || '',
        bankBranch: savedFields.bankBranch || previous.bankBranch || fields.bankBranch || '',
        paymentMode: savedFields.paymentMode || fields.paymentMode || 'Bank Transfer',
        renewalOption: savedFields.renewalOption || fields.renewalOption || '',
        specialConditions: savedFields.specialConditions || fields.specialConditions || '',
        handoverChecklistIds: savedFields.handoverChecklistIds || '',
        photoIds: savedFields.photoIds || '',
        electricityMeter: savedFields.electricityMeter || fields.electricityMeter || '',
        electricityMeterDate: savedFields.electricityMeterDate || fields.electricityMeterDate || fields.handoverDate || '',
        waterMeter: savedFields.waterMeter || fields.waterMeter || '',
        waterMeterDate: savedFields.waterMeterDate || fields.waterMeterDate || fields.handoverDate || '',
        gasMeter: savedFields.gasMeter || fields.gasMeter || '',
        gasMeterDate: savedFields.gasMeterDate || fields.gasMeterDate || fields.handoverDate || '',
        districtCoolingMeter: savedFields.districtCoolingMeter || fields.districtCoolingMeter || '',
        districtCoolingMeterDate: savedFields.districtCoolingMeterDate || fields.districtCoolingMeterDate || fields.handoverDate || '',
        otherMeter: savedFields.otherMeter || fields.otherMeter || '',
        otherMeterDate: savedFields.otherMeterDate || fields.otherMeterDate || fields.handoverDate || '',
      };
      const availablePhotoIds = new Set(this.leasePropertyPhotos.map(photo => String(photo.id)));
      const savedPhotoIds = String(savedFields.photoIds || '').split(',').map(value => value.trim()).filter(value => availablePhotoIds.has(value));
      this.leaseSelectedPhotoIds = Object.prototype.hasOwnProperty.call(savedFields, 'photoIds')
        ? savedPhotoIds
        : this.leasePropertyPhotos.map(photo => String(photo.id));
      const availableChecklistIds = new Set(this.leaseHandoverChecklist.map(item => String(item.id)));
      const savedChecklistIds = String(savedFields.handoverChecklistIds || '').split(',').map(value => value.trim()).filter(value => availableChecklistIds.has(value));
      this.leaseSelectedChecklistIds = Object.prototype.hasOwnProperty.call(savedFields, 'handoverChecklistIds')
        ? savedChecklistIds
        : this.leaseHandoverChecklist.filter(item => item.enabled).map(item => String(item.id));
      this.leaseFormOpen = true;
    },
    async loadLeaseHandoverChecklist() {
      const property = this.selectedProperty;
      if (!property?.ownerId || !property?.ownerUnitId) { this.leaseHandoverChecklist = []; return []; }
      this.leaseHandoverChecklist = await fetchAdminPropertyHandoverChecklist(property.ownerId, property.ownerUnitId) || [];
      return this.leaseHandoverChecklist;
    },
    selectEnabledLeaseChecklist() {
      this.leaseSelectedChecklistIds = this.leaseHandoverChecklist.filter(item => item.enabled).map(item => String(item.id));
    },
    selectAllLeaseChecklist() {
      this.leaseSelectedChecklistIds = this.leaseHandoverChecklist.map(item => String(item.id));
    },
    async syncLeaseHandoverChecklist() {
      const property = this.selectedProperty;
      const selected = new Set(this.leaseSelectedChecklistIds.map(String));
      for (const item of this.leaseHandoverChecklist) {
        const enabled = selected.has(String(item.id));
        if (Boolean(item.enabled) === enabled) continue;
        const saved = await updateAdminPropertyHandoverChecklistItem(property.ownerId, property.ownerUnitId, item.id, {
          category: item.category,
          itemName: item.itemName,
          defaultQuantity: item.defaultQuantity || null,
          notes: item.notes || null,
          sortOrder: Number(item.sortOrder || 0),
          enabled,
        });
        Object.assign(item, saved || { enabled });
      }
    },
    async loadLeasePropertyPhotos() {
      const property = this.selectedProperty;
      if (!property?.ownerId || !property?.ownerUnitId) { this.leasePropertyPhotos = []; return []; }
      this.leasePropertyPhotos = await fetchAdminPropertyPhotos(property.ownerId, property.ownerUnitId);
      return this.leasePropertyPhotos;
    },
    leasePropertyPhotoUrl(photo) {
      const property = this.selectedProperty;
      return adminPropertyPhotoContentUrl(property.ownerId, property.ownerUnitId, photo.id);
    },
    selectAllLeasePhotos() {
      this.leaseSelectedPhotoIds = this.leasePropertyPhotos.map(photo => String(photo.id));
    },
    setLeasePhotoFiles(event) {
      const files = Array.from(event.target.files || []).filter(file => /^image\/(jpeg|png)$/i.test(file.type));
      this.leasePhotoFiles = mergeUniquePhotoFiles(this.leasePhotoFiles, files);
      event.target.value = '';
    },
    removeLeasePhotoFile(file) {
      this.leasePhotoFiles = removePhotoFile(this.leasePhotoFiles, file);
    },
    formatLeasePhotoFileSize(size) {
      const bytes = Math.max(0, Number(size || 0));
      return bytes >= 1024 * 1024 ? `${(bytes / (1024 * 1024)).toFixed(1)} MB` : `${Math.max(1, Math.ceil(bytes / 1024))} KB`;
    },
    closeLeaseForm() { if (!this.actionBusy) { this.leaseFormOpen = false; this.leaseGenerationStartsSigning = false; this.leasePhotoFiles = []; this.resetSupplementValidation('lease'); this.actionError = ''; } },
    async openHandoverReportForm() {
      const property = this.selectedProperty;
      if (!property?.ownerId || !property?.ownerUnitId || !this.currentLease || this.selectedCycleIsHistorical || this.actionBusy) return;
      this.handoverReportError = '';
      this.handoverPhotoFiles = [];
      this.handoverPhotoCaptionById = {};
      try {
        const [checklist, photos] = await Promise.all([
          fetchAdminPropertyHandoverChecklist(property.ownerId, property.ownerUnitId),
          fetchAdminPropertyPhotos(property.ownerId, property.ownerUnitId),
        ]);
        this.handoverChecklist = checklist || [];
        this.handoverSelectedChecklistIds = this.handoverChecklist.filter(item => item.enabled).map(item => String(item.id));
        this.handoverPropertyPhotos = photos || [];
        this.handoverSelectedPhotoIds = this.handoverPropertyPhotos.map(photo => String(photo.id));
        this.handoverPhotoSectionById = Object.fromEntries(this.handoverPropertyPhotos.map(photo => [String(photo.id), this.defaultHandoverPhotoSection(photo)]));
        this.handoverPhotoCaptionById = Object.fromEntries(this.handoverPropertyPhotos.map(photo => [String(photo.id), this.visibleHandoverPhotoCaption(photo.description)]));
        this.handoverReportForm = {
          reportDate: new Date().toISOString().slice(0, 10),
          unitType: property.unitType || '',
          handoverFrom: property.ownerName || this.currentMandate?.ownerName || '',
          handoverTo: this.currentLease?.tenantName || '',
          remarks: '',
          tenantIssues: [],
          ownerIssues: [],
        };
        this.handoverReportFormOpen = true;
      } catch (error) {
        this.actionError = error.message || this.$t('rentalFiles.handoverReportLoadFailed');
      }
    },
    closeHandoverReportForm() {
      if (this.handoverReportSaving) return;
      this.handoverReportFormOpen = false;
      this.handoverPhotoFiles = [];
      this.handoverPhotoCaptionById = {};
      this.handoverReportError = '';
    },
    selectAllHandoverChecklist() { this.handoverSelectedChecklistIds = this.handoverChecklist.map(item => String(item.id)); },
    selectAllHandoverPhotos() { this.handoverSelectedPhotoIds = this.handoverPropertyPhotos.map(photo => String(photo.id)); },
    addHandoverIssue(field) { this.handoverReportForm[field].push({ key: `${field}-${Date.now()}-${this.handoverReportForm[field].length}`, name: '', recommendation: '' }); },
    removeHandoverIssue(field, index) { this.handoverReportForm[field].splice(index, 1); },
    handoverPropertyPhotoUrl(photo) {
      const property = this.selectedProperty;
      return adminPropertyPhotoContentUrl(property.ownerId, property.ownerUnitId, photo.id);
    },
    defaultHandoverPhotoSection(photo) {
      const category = String(photo?.category || '').toLowerCase();
      if (category.includes('exterior')) return HANDOVER_PHOTO_SECTIONS[0];
      if (category.includes('defect')) return HANDOVER_PHOTO_SECTIONS[6];
      if (category.includes('kitchen')) return HANDOVER_PHOTO_SECTIONS[3];
      if (category.includes('bed')) return HANDOVER_PHOTO_SECTIONS[4];
      if (category.includes('bath')) return HANDOVER_PHOTO_SECTIONS[5];
      return HANDOVER_PHOTO_SECTIONS[1];
    },
    visibleHandoverPhotoCaption(value) {
      const caption = String(value || '').trim();
      return /^(从租赁合同补充资料新增|從租賃合約補充資料新增|Added from tenancy agreement details|由附件签约生成交接报告时新增|由附件簽約產生交接報告時新增|Added while generating a handover report from Files & Signing)$/.test(caption) ? '' : caption;
    },
    setHandoverPhotoFiles(event) {
      const existing = new Set(this.handoverPhotoFiles.map(entry => `${entry.file.name}:${entry.file.size}:${entry.file.lastModified}`));
      for (const file of Array.from(event.target.files || [])) {
        if (!/^image\/(jpeg|png)$/i.test(file.type)) continue;
        const key = `${file.name}:${file.size}:${file.lastModified}`;
        if (!existing.has(key)) {
          this.handoverPhotoFiles.push({ key, file, section: HANDOVER_PHOTO_SECTIONS[1], caption: '' });
          existing.add(key);
        }
      }
      event.target.value = '';
    },
    removeHandoverPhotoFile(entry) { this.handoverPhotoFiles = this.handoverPhotoFiles.filter(item => item !== entry); },
    async syncHandoverReportChecklist() {
      const property = this.selectedProperty;
      const selected = new Set(this.handoverSelectedChecklistIds.map(String));
      for (const item of this.handoverChecklist) {
        const enabled = selected.has(String(item.id));
        if (Boolean(item.enabled) === enabled) continue;
        const saved = await updateAdminPropertyHandoverChecklistItem(property.ownerId, property.ownerUnitId, item.id, {
          category: item.category, itemName: item.itemName, defaultQuantity: item.defaultQuantity || null,
          notes: item.notes || null, sortOrder: Number(item.sortOrder || 0), enabled,
        });
        Object.assign(item, saved || { enabled });
      }
    },
    async handoverPropertyPhotoFile(photo) {
      const response = await fetch(this.handoverPropertyPhotoUrl(photo), { credentials: 'include' });
      if (!response.ok) throw new Error(this.$t('rentalFiles.handoverReportPhotoLoadFailed'));
      const blob = await response.blob();
      return new File([blob], photo.originalName || `property-photo-${photo.id}.jpg`, { type: blob.type || photo.mimeType || 'image/jpeg' });
    },
    async revealGeneratedFiles() {
      await this.$nextTick();
      const target = this.$refs.generatedFilesSection;
      target?.scrollIntoView?.({ behavior: 'smooth', block: 'start' });
      target?.focus?.({ preventScroll: true });
    },
    async generateHandoverReport() {
      const property = this.selectedProperty;
      if (!property?.ownerId || !property?.ownerUnitId || this.handoverReportSaving) return;
      const missing = [];
      if (!this.handoverReportForm.reportDate) missing.push(this.$t('rentalFiles.handoverReportDate'));
      if (!this.handoverReportForm.unitType?.trim()) missing.push(this.$t('rentalFiles.handoverReportUnitType'));
      if (!this.handoverReportForm.handoverFrom?.trim()) missing.push(this.$t('rentalFiles.handoverReportFrom'));
      if (!this.handoverReportForm.handoverTo?.trim()) missing.push(this.$t('rentalFiles.handoverReportTo'));
      if (!this.handoverSelectedChecklistIds.length) missing.push(this.$t('rentalFiles.handoverReportChecklistRequired'));
      if (!(this.handoverSelectedPhotoIds.length + this.handoverPhotoFiles.length)) missing.push(this.$t('rentalFiles.handoverReportPhotoRequired'));
      if (missing.length) {
        this.handoverReportError = this.$t('rentalFiles.handoverReportMissing', { fields: missing.join('、') });
        return;
      }
      this.handoverReportSaving = true;
      this.handoverReportError = '';
      try {
        if (this.handoverPhotoFiles.some(entry => entry.file.size > 10 * 1024 * 1024)) throw new Error(this.$t('rentalFiles.supplement.propertyPhotos.fileTooLarge'));
        const selectedChecklist = new Set(this.handoverSelectedChecklistIds.map(String));
        const sections = this.handoverChecklistGroups.map(group => ({
          title: group.category,
          items: group.items.filter(item => selectedChecklist.has(String(item.id))).map(item => ({ name: item.itemName, quantity: item.defaultQuantity || '1', condition: '', remarks: item.notes || '' })),
        })).filter(section => section.items.length);
        const selectedPhotos = this.handoverPropertyPhotos.filter(photo => this.handoverSelectedPhotoIds.includes(String(photo.id)));
        const existingPhotoFiles = await Promise.all(selectedPhotos.map(photo => this.handoverPropertyPhotoFile(photo)));
        const uploadedPhotoFiles = this.handoverPhotoFiles.map(entry => entry.file);
        const reportPhotos = [...existingPhotoFiles, ...uploadedPhotoFiles];
        const photoMeta = [
          ...selectedPhotos.map(photo => ({ section: this.handoverPhotoSectionById[String(photo.id)] || HANDOVER_PHOTO_SECTIONS[1], caption: this.handoverPhotoCaptionById[String(photo.id)] || '' })),
          ...this.handoverPhotoFiles.map(entry => ({ section: entry.section, caption: entry.caption || '' })),
        ];
        await this.syncHandoverReportChecklist();
        for (const entry of this.handoverPhotoFiles) {
          await createAdminPropertyPhoto(property.ownerId, property.ownerUnitId, { category: 'interior', description: this.$t('rentalFiles.handoverReportPhotoArchiveNote'), sortOrder: 0, cover: false }, entry.file);
        }
        const content = {
          leaseId: this.currentLease?.id || this.currentLease?.leaseId || null, handoverType: 'move_in',
          unitType: this.handoverReportForm.unitType, handoverFrom: this.handoverReportForm.handoverFrom,
          handoverTo: this.handoverReportForm.handoverTo, sections,
          tenantIssues: (this.handoverReportForm.tenantIssues || []).map(({ key, ...issue }) => issue).filter(issue => issue.name),
          ownerIssues: (this.handoverReportForm.ownerIssues || []).map(({ key, ...issue }) => issue).filter(issue => issue.name || issue.recommendation),
          remarks: this.handoverReportForm.remarks, photos: [],
        };
        const generatedTitle = `${property.projectName || ''} ${property.unitNo || ''} ${this.$t('rentalFiles.handoverReportTitle')}`.trim();
        const report = await createAdminPropertyHandoverReport(property.ownerId, property.ownerUnitId, {
          title: generatedTitle, reportDate: this.handoverReportForm.reportDate || new Date().toISOString().slice(0, 10),
          remarks: this.handoverReportForm.remarks, completed: true, contentJson: JSON.stringify(content),
        }, null, reportPhotos, photoMeta);
        if (report?.id) {
          const downloaded = await downloadAdminPropertyHandoverReport(property.ownerId, property.ownerUnitId, report.id);
          const url = URL.createObjectURL(downloaded.blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `${generatedTitle}_${report.id}.pdf`;
          document.body.appendChild(link);
          link.click();
          link.remove();
          setTimeout(() => URL.revokeObjectURL(url), 1000);
        }
        this.handoverReportFormOpen = false;
        this.handoverPhotoFiles = [];
        this.handoverPhotoCaptionById = {};
        await this.loadSigningWorkspace();
        await this.revealGeneratedFiles();
        this.page?.showToast?.(this.$t('rentalFiles.handoverReportGenerated'));
      } catch (error) {
        this.handoverReportError = error.message || this.$t('rentalFiles.handoverReportGenerateFailed');
      } finally {
        this.handoverReportSaving = false;
      }
    },
    async openRentalAppointmentForm(reset = false) {
      if (!this.currentMandate?.id || this.selectedCycleIsHistorical || this.actionBusy) return;
      this.actionError = '';
      this.resetSupplementValidation('rentalAppointment');
      try {
        const saved = reset ? null : await fetchAdminRentalAppointmentDetails(this.currentMandate.id);
        const savedFields = saved?.fields || saved || {};
        const fields = this.templateFields();
        const today = new Date().toISOString().slice(0, 10);
        this.rentalAppointmentForm = reset ? emptyRentalAppointmentForm() : {
          ...emptyRentalAppointmentForm(),
          ...savedFields,
          ownerSignerName: savedFields.ownerSignerName || fields.landlordName || '',
          caseNo: fields.caseNo || savedFields.caseNo || '',
          propertyAddress: fields.propertyAddress || savedFields.propertyAddress || '',
          startDate: fields.startDate || savedFields.startDate || '',
          endDate: fields.endDate || savedFields.endDate || '',
          landlordName: fields.landlordName || savedFields.landlordName || '',
          landlordIdentity: fields.landlordIdentity || savedFields.landlordIdentity || '',
          landlordAddress: fields.ownerAddress || fields.propertyAddress || savedFields.landlordAddress || '',
          landlordDate: fields.agreementDate || savedFields.landlordDate || today,
          witnessDate: savedFields.witnessDate || fields.agreementDate || today,
          hasSecondLandlord: savedFields.hasSecondLandlord === true || String(savedFields.hasSecondLandlord || '').toLowerCase() === 'true',
        };
        this.rentalAppointmentFormOpen = true;
      } catch (error) { this.actionError = error.message || '租赁委任书资料读取失败'; }
    },
    closeRentalAppointmentForm() { if (!this.actionBusy) { this.rentalAppointmentFormOpen = false; this.resetSupplementValidation('rentalAppointment'); this.actionError = ''; } },
    openAuthorizationForm(reset = false) {
      if (!this.currentMandate?.id || this.selectedCycleIsHistorical || this.actionBusy) return;
      const fields = this.templateFields();
      this.actionError = '';
      this.resetSupplementValidation('authorization');
      this.authorizationForm = reset ? emptyAuthorizationForm() : {
        ...emptyAuthorizationForm(),
        ownerSignerName: fields.landlordName || '',
        projectName: fields.projectName || '',
        unitNo: fields.unitNo || '',
        landlordName: fields.landlordName || '',
        landlordIdentity: fields.landlordIdentity || '',
        ownerEmail: fields.ownerEmail || '',
        agreementDate: fields.agreementDate || new Date().toISOString().slice(0, 10),
      };
      this.authorizationFormOpen = true;
    },
    closeAuthorizationForm() { if (!this.actionBusy) { this.authorizationFormOpen = false; this.resetSupplementValidation('authorization'); this.actionError = ''; } },
    async openTerminationLetterForm(reset = false) {
      if (!this.currentMandate?.id || this.selectedCycleIsHistorical || this.actionBusy) return;
      this.actionError = '';
      this.resetSupplementValidation('terminationLetter');
      let account = {};
      let owner = null;
      [account, owner] = await Promise.all([
        this.loadPmaBankAccount().then(result => result || {}).catch(() => {
          this.pmaBankAccounts = [];
          this.pmaBankAccount = null;
          return {};
        }),
        this.selectedProperty?.ownerId ? fetchAdminOwner(this.selectedProperty.ownerId).catch(() => null) : Promise.resolve(null),
      ]);
      this.pmaOwnerProfile = owner;
      const fields = this.templateFields();
      account = account || this.pmaBankAccount || this.pmaBankAccounts[0] || {};
      const previous = this.terminationLetterForm || emptyTerminationLetterForm();
      this.terminationLetterForm = {
        ...emptyTerminationLetterForm(),
        ownerSignerName: previous.ownerSignerName || owner?.fullName || fields.landlordName || '',
        companySignerName: previous.companySignerName || this.pmaForm.companySignerName || '',
        agreementDate: fields.agreementDate || new Date().toISOString().slice(0, 10),
        landlordName: owner?.fullName || fields.landlordName || '', landlordIdentity: owner?.identityNo || owner?.passportNo || fields.landlordIdentity || '', ownerEmail: owner?.email || fields.ownerEmail || '',
        projectName: fields.projectName || '', unitNo: fields.unitNo || '',
        bankName: account.itemName || fields.bankName || '', bankPayeeName: account.paymentName || fields.bankPayeeName || '',
        bankAccountNo: account.accountNo || fields.bankAccountNo || '', bankSwiftCode: account.swiftCode || fields.bankSwiftCode || '', bankAddress: account.bankAddress || fields.bankAddress || '',
        authorizedAgentName: this.currentMandate?.responsibleUserName || '',
      };
      this.terminationLetterFormOpen = true;
    },
    closeTerminationLetterForm() { if (!this.actionBusy) { this.terminationLetterFormOpen = false; this.resetSupplementValidation('terminationLetter'); this.actionError = ''; } },
    applyRentalRemittanceBankAccount() {
      const account = this.pmaBankAccounts.find(item => String(item.id) === String(this.rentalRemittanceForm.bankAccountId));
      if (!account) return;
      Object.assign(this.rentalRemittanceForm, {
        bankPayeeName: account.paymentName || '', bankName: account.itemName || '', bankAddress: account.bankAddress || '',
        bankBranchCode: account.branchCode || '', bankAccountNo: account.accountNo || '', bankSwiftCode: account.swiftCode || '',
      });
    },
    async openRentalRemittanceForm(reset = false) {
      if (!this.currentMandate?.id || this.selectedCycleIsHistorical || this.actionBusy) return;
      this.actionError = '';
      this.resetSupplementValidation('rentalRemittance');
      let account = {};
      let owner = null;
      try {
        [account, owner] = await Promise.all([
          this.loadPmaBankAccount().then(result => result || {}).catch(() => { this.pmaBankAccounts = []; return {}; }),
          this.selectedProperty?.ownerId ? fetchAdminOwner(this.selectedProperty.ownerId).catch(() => null) : Promise.resolve(null),
        ]);
      } catch (error) { this.actionError = error.message || '租金汇款资料读取失败'; return; }
      const fields = this.templateFields();
      const selectedAccount = account || this.pmaBankAccounts[0] || {};
      const previous = this.rentalRemittanceForm || emptyRentalRemittanceForm();
      this.rentalRemittanceForm = reset ? emptyRentalRemittanceForm() : {
        ...emptyRentalRemittanceForm(),
        ownerSignerName: previous.ownerSignerName || owner?.fullName || fields.landlordName || '',
        agreementDate: previous.agreementDate || fields.agreementDate || new Date().toISOString().slice(0, 10),
        landlordName: previous.landlordName || owner?.fullName || fields.landlordName || '', landlordIdentity: previous.landlordIdentity || owner?.identityNo || owner?.passportNo || fields.landlordIdentity || '',
        ownerEmail: previous.ownerEmail || owner?.email || fields.ownerEmail || '', unitNo: previous.unitNo || fields.unitNo || '',
        bankAccountId: previous.bankAccountId || (selectedAccount.id ? String(selectedAccount.id) : ''),
        bankPayeeName: previous.bankPayeeName || selectedAccount.paymentName || fields.bankPayeeName || '', bankName: previous.bankName || selectedAccount.itemName || fields.bankName || '',
        bankAccountNo: previous.bankAccountNo || selectedAccount.accountNo || fields.bankAccountNo || '', bankSwiftCode: previous.bankSwiftCode || selectedAccount.swiftCode || fields.bankSwiftCode || '',
        bankBranchCode: previous.bankBranchCode || selectedAccount.branchCode || '', bankAddress: previous.bankAddress || selectedAccount.bankAddress || '',
      };
      this.rentalRemittanceFormOpen = true;
    },
    closeRentalRemittanceForm() { if (!this.actionBusy) { this.rentalRemittanceFormOpen = false; this.resetSupplementValidation('rentalRemittance'); this.actionError = ''; } },
    async openOtrForm(reset = false) {
      const lease = this.currentLease; const mandate = this.currentMandate;
      if (!mandate?.id || this.selectedCycleIsHistorical || this.actionBusy) return;
      const fields = this.templateFields();
      const today = new Date().toISOString().slice(0, 10);
      const startDate = lease?.startDate || mandate.startDate || '';
      const endDate = lease?.endDate || mandate.endDate || '';
      const start = startDate ? new Date(`${startDate}T00:00:00`) : null;
      const end = endDate ? new Date(`${endDate}T00:00:00`) : null;
      const months = start && end && !Number.isNaN(start.getTime()) && !Number.isNaN(end.getTime())
        ? Math.max(1, (end.getFullYear() - start.getFullYear()) * 12 + end.getMonth() - start.getMonth()) : 12;
      const monthlyRent = Number(lease?.monthlyRent || 0);
      const deposit = splitTenantDeposit(lease?.depositAmount, monthlyRent,
        lease?.utilityDeposit ?? lease?.utilityDepositAmount);
      const securityDeposit = deposit.securityDeposit;
      const utilityDeposit = deposit.utilityDeposit;
      const stampingFee = Number(lease?.stampingFee ?? 0);
      let appointmentFields = {};
      if (!reset) {
        appointmentFields = await fetchAdminRentalAppointmentDetails(mandate.id)
          .then(result => result?.fields || result || {}).catch(() => ({}));
      }
      this.actionError = '';
      this.resetSupplementValidation('otr');
      this.otrForm = reset ? emptyOtrForm() : {
        ...emptyOtrForm(),
        caseNo: lease?.leaseNo || fields.caseNo,
        propertyAddress: this.leasePropertyAddress || fields.propertyAddress,
        advanceRental: monthlyRent.toFixed(2),
        securityDepositMonths: monthlyRent > 0 ? String(Number((securityDeposit / monthlyRent).toFixed(2))) : '0',
        securityDeposit: securityDeposit.toFixed(2),
        utilityDepositMonths: monthlyRent > 0 ? String(Number((utilityDeposit / monthlyRent).toFixed(2))) : '0',
        utilityDeposit: utilityDeposit.toFixed(2),
        stampingFee: stampingFee.toFixed(2),
        totalBeforeKeys: (monthlyRent + securityDeposit + utilityDeposit + stampingFee).toFixed(2),
        periodYears: String(Number((months / 12).toFixed(2))),
        renewalYears: '1',
        commencementDate: startDate,
        earnestDeposit: monthlyRent.toFixed(2),
        tenantName: lease?.tenantName || '',
        tenantIdentity: lease?.tenantIdentity || lease?.identityNo || '',
        tenantDate: startDate || today,
        landlordName: fields.landlordName,
        landlordIdentity: fields.landlordIdentity,
        landlordDate: fields.agreementDate || startDate || today,
        tenantWitnessDate: startDate || today,
        landlordWitnessDate: fields.agreementDate || startDate || today,
        otherConditions: 'Nil',
        earnestDepositWords: appointmentFields.earnestDepositWords || '',
        commissionWords: appointmentFields.commissionWords || '',
        commissionMonths: appointmentFields.commissionMonths || '',
        sstPercent: appointmentFields.sstPercent || '',
        commissionAmount: appointmentFields.commissionAmount || '',
        agencyFeeTotal: appointmentFields.agencyFeeTotal || '',
        startDate: appointmentFields.startDate || mandate.startDate || startDate,
        endDate: appointmentFields.endDate || mandate.endDate || endDate,
        landlordAddress: appointmentFields.landlordAddress || fields.ownerAddress || '',
        hasSecondLandlord: Boolean(appointmentFields.hasSecondLandlord)
          || Boolean(appointmentFields.secondLandlordName || appointmentFields.secondLandlordIdentity),
        secondLandlordName: appointmentFields.secondLandlordName || '',
        secondLandlordIdentity: appointmentFields.secondLandlordIdentity || '',
        secondLandlordAddress: appointmentFields.secondLandlordAddress || '',
        secondLandlordDate: appointmentFields.secondLandlordDate || '',
        witnessName: appointmentFields.witnessName || '',
        witnessIdentity: appointmentFields.witnessIdentity || '',
        witnessAddress: appointmentFields.witnessAddress || '',
        witnessDate: appointmentFields.witnessDate || '',
      };
      this.otrFormOpen = true;
    },
    closeOtrForm() { if (!this.actionBusy) { this.otrFormOpen = false; this.resetSupplementValidation('otr'); this.actionError = ''; } },
    updateOtrTotal() {
      this.otrForm.totalBeforeKeys = ['advanceRental', 'securityDeposit', 'utilityDeposit', 'stampingFee']
        .reduce((total, field) => total + Number(this.otrForm[field] || 0), 0).toFixed(2);
    },
    async openFinanceDocumentPanel() {
      this.financeDocumentOpen = true;
      this.financeDocumentSelectedIds = [];
      this.financeDocumentActionError = '';
      await this.loadFinanceDocumentRows();
    },
    closeFinanceDocumentPanel() {
      if (this.financeDocumentBusy) return;
      this.financeDocumentOpen = false;
      this.financeDocumentError = '';
      this.financeDocumentActionError = '';
    },
    async loadFinanceDocumentRows() {
      if (!this.selectedProperty) return;
      this.financeDocumentLoading = true;
      this.financeDocumentError = '';
      this.financeDocumentActionError = '';
      try {
        const projectName = String(this.selectedProperty.projectName || '').trim();
        const unitNo = String(this.selectedProperty.unitNo || '').trim();
        const reviewTypes = ['property', 'expense', 'reserve', 'reserve_refund', 'tenant_deposit'];
        const responses = await Promise.all(reviewTypes.map(type => fetchAdminFinanceReviews({ type, page: 1, pageSize: 100, projectName, keyword: unitNo })));
        const rowsById = new Map();
        responses.flatMap(response => response?.rows || []).forEach(row => {
          if (projectName && String(row.projectName || '').trim() !== projectName) return;
          if (unitNo && String(row.unitNo || '').trim() !== unitNo) return;
          rowsById.set(String(row.id), row);
        });
        this.financeDocumentRows = [...rowsById.values()].sort((left, right) => String(right.transactionDate || right.submittedAt || '').localeCompare(String(left.transactionDate || left.submittedAt || '')) || Number(right.id || 0) - Number(left.id || 0));
        this.financeDocumentSelectedIds = this.financeDocumentSelectedIds.filter(id => this.financeDocumentRows.some(row => String(row.id) === String(id)));
      } catch (error) {
        this.financeDocumentRows = [];
        this.financeDocumentError = error.message || this.$t('rentalFiles.financeDocumentLoadFailed');
      } finally { this.financeDocumentLoading = false; }
    },
    toggleAllFinanceDocuments(event) { this.financeDocumentSelectedIds = event.target.checked ? this.financeDocumentRows.map(row => row.id) : []; },
    financeRecordDirection(row) { return /expense|debit|deduction|refund|payment_out/i.test(String(row.recordType || '')) ? '−' : '+'; },
    financeRecordTypeLabel(row) {
      const type = String(row.recordType || '').toLowerCase();
      const key = type.includes('rent') ? 'financeDocumentTypeRent' : type.includes('expense') || type.includes('maintenance') ? 'financeDocumentTypeExpense' : type.includes('reserve') ? 'financeDocumentTypeReserve' : type.includes('deposit') ? 'financeDocumentTypeDeposit' : type.includes('property') ? 'financeDocumentTypeProperty' : '';
      return key ? this.$t(`rentalFiles.${key}`) : (row.recordType || this.$t('rentalFiles.financeDocumentTypeOther'));
    },
    financeRecordDescription(row) { return row.submissionNote || row.allocationNote || row.reviewNote || row.payerName || this.financeRecordTypeLabel(row); },
    financeStatusLabel(row) {
      const status = String(row.confirmationStatus || '').toLowerCase();
      if (status === 'confirmed') return this.$t('rentalFiles.financeDocumentStatusConfirmed');
      if (status === 'rejected') return this.$t('rentalFiles.financeDocumentStatusRejected');
      return this.$t('rentalFiles.financeDocumentStatusPending');
    },
    financeMoney(row) { return `${row.currency || 'MYR'} ${Number(row.amount || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`; },
    async generateFinanceDocuments() {
      if (!this.financeDocumentSelectedIds.length || this.financeDocumentBusy) return;
      this.financeDocumentBusy = true;
      this.financeDocumentActionError = '';
      try {
        const result = await downloadAdminFinanceDocuments(this.financeDocumentSelectedIds, this.financeDocumentType);
        const url = URL.createObjectURL(result.blob);
        const link = document.createElement('a');
        link.href = url; link.download = result.filename || `${this.financeDocumentType}-documents.zip`;
        document.body.appendChild(link); link.click(); link.remove();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
        this.page?.showToast?.(this.$t('rentalFiles.financeDocumentSuccess'));
      } catch (error) { this.financeDocumentActionError = error.message || this.$t('rentalFiles.financeDocumentDownloadFailed'); }
      finally { this.financeDocumentBusy = false; }
    },
    async generatePma(options = {}) {
      if (!this.currentMandate?.id || this.selectedCycleIsHistorical || this.actionBusy) return;
      if (!this.validateSupplementForm('pma', 'pmaFormRef')) return;
      let generated = false;
      this.actionBusy = true; this.actionError = '';
      try {
        if (!options.preserveMasterData) {
          await this.syncPmaOwnerProfile();
          await this.syncPmaBankAccount();
        }
        const result = await generateAdminContractTemplate('property-management-agreement', { ...this.templateFields(), ...this.pmaForm });
        const file = new File([result.blob], result.filename, { type: 'application/pdf' });
        await uploadAdminRentalMandateDocument(this.currentMandate.id, 'property_management_agreement_draft', file);
        this.pmaFormOpen = false; await this.loadSigningWorkspace(); this.page?.showToast?.(options.preserveMasterData ? '已按最新条款生成新版合约，旧版合约已保留' : this.$t('rentalFiles.pmaGenerated')); generated = true;
      } catch (error) { this.actionError = error.message || this.$t('rentalFiles.actionFailed'); }
      finally { this.actionBusy = false; }
      if (generated && !options.preserveMasterData) await this.startSigningDirect('pma');
    },
    async syncPmaOwnerProfile() {
      const ownerId = this.selectedProperty?.ownerId;
      if (!ownerId) return;
      const owner = this.pmaOwnerProfile || await fetchAdminOwner(ownerId);
      const identity = this.pmaForm.landlordIdentity.trim();
      const identityIsPassport = Boolean(owner?.passportNo && !owner?.identityNo);
      this.pmaOwnerProfile = await updateAdminOwner(ownerId, {
        ownerNo: owner?.ownerNo || null,
        fullName: this.pmaForm.landlordName,
        identityNo: identityIsPassport ? (owner?.identityNo || null) : identity,
        phone: this.pmaForm.ownerPhone,
        mobilePhone: this.pmaForm.ownerPhone,
        homePhone: owner?.homePhone || null,
        officePhone: owner?.officePhone || null,
        passportNo: identityIsPassport ? identity : (owner?.passportNo || null),
        email: this.pmaForm.ownerEmail,
        mailingAddress: this.pmaForm.ownerAddress,
        status: owner?.status || 'active',
      });
    },
    async syncPmaBankAccount() {
      const property = this.selectedProperty;
      if (!property?.ownerId || !property?.ownerUnitId) return;
      const payload = {
        itemName: this.pmaForm.bankName,
        paymentName: this.pmaForm.bankPayeeName,
        accountNo: this.pmaForm.bankAccountNo,
        bankAddress: this.pmaForm.bankAddress,
        branchCode: this.pmaForm.bankBranchCode,
        swiftCode: this.pmaForm.bankSwiftCode,
        remarks: this.pmaBankAccount?.remarks || '',
      };
      if (this.pmaBankAccount?.id) {
        this.pmaBankAccount = await updateAdminPropertyBankAccount(property.ownerId, property.ownerUnitId, this.pmaBankAccount.id, payload);
      } else {
        this.pmaBankAccount = await createAdminPropertyBankAccount(property.ownerId, property.ownerUnitId, payload);
      }
    },
    async generateRentalAppointment() {
      if (!this.currentMandate?.id || this.selectedCycleIsHistorical || this.actionBusy) return;
      if (!this.validateSupplementForm('rentalAppointment', 'rentalAppointmentFormRef')) return;
      let generated = false;
      this.actionBusy = true; this.actionError = '';
      try {
        const mandate = this.currentMandate; const form = { ...this.rentalAppointmentForm, hasSecondLandlord: String(Boolean(this.rentalAppointmentForm.hasSecondLandlord)) };
        await saveAdminRentalAppointmentDetails(mandate.id, form);
        const result = await generateAdminContractTemplate('authorization', { ...this.templateFields(), ...form, commission: form.commissionWords, commencementDate: form.endDate });
        const file = new File([result.blob], result.filename, { type: 'application/pdf' });
        await uploadAdminRentalMandateDocument(mandate.id, 'rental_appointment_draft', file);
        this.rentalAppointmentFormOpen = false; await this.loadSigningWorkspace(); this.page?.showToast?.(this.$t('rentalFiles.rentalAppointmentGenerated')); generated = true;
      } catch (error) { this.actionError = error.message || this.$t('rentalFiles.actionFailed'); }
      finally { this.actionBusy = false; }
      if (generated) await this.startSigningDirect('rentalAppointment');
    },
    async generateAuthorization(options = {}) {
      if (!this.currentMandate?.id || this.selectedCycleIsHistorical || this.actionBusy) return;
      if (!this.validateSupplementForm('authorization', 'authorizationFormRef')) return;
      let generated = false;
      this.actionBusy = true; this.actionError = '';
      try {
        const mandate = this.currentMandate; const result = await generateAdminContractTemplate('management-authorization', { ...this.templateFields(), ...this.authorizationForm });
        const file = new File([result.blob], result.filename, { type: 'application/pdf' }); await uploadAdminRentalMandateDocument(mandate.id, 'management_authorization_draft', file); this.authorizationFormOpen = false; await this.loadSigningWorkspace(); this.page?.showToast?.(options.preserveMasterData ? '已按最新条款生成新版合约，旧版合约已保留' : this.$t('rentalFiles.authorizationGenerated')); generated = true;
      } catch (error) { this.actionError = error.message || this.$t('rentalFiles.actionFailed'); }
      finally { this.actionBusy = false; }
      if (generated && !options.preserveMasterData) await this.startSigningDirect('authorization');
    },
    async generateTerminationLetter(options = {}) {
      if (!this.currentMandate?.id || this.selectedCycleIsHistorical || this.actionBusy) return;
      if (!this.validateSupplementForm('terminationLetter', 'terminationLetterFormRef')) return;
      let generated = false;
      this.actionBusy = true; this.actionError = '';
      try {
        const result = await generateAdminContractTemplate('termination-letter', { ...this.templateFields(), ...this.terminationLetterForm });
        const file = new File([result.blob], result.filename, { type: 'application/pdf' });
        await uploadAdminRentalMandateDocument(this.currentMandate.id, 'termination_letter_draft', file);
        this.terminationLetterFormOpen = false; await this.loadSigningWorkspace();
        this.page?.showToast?.(options.preserveMasterData ? '已按最新条款生成新版终止通知书，旧版文件已保留' : '终止通知书已生成'); generated = true;
      } catch (error) { this.actionError = error.message || this.$t('rentalFiles.actionFailed'); }
      finally { this.actionBusy = false; }
      if (generated && !options.preserveMasterData) await this.startSigningDirect('terminationLetter');
    },
    async generateRentalRemittance(options = {}) {
      if (!this.currentMandate?.id || this.selectedCycleIsHistorical || this.actionBusy) return;
      if (!this.validateSupplementForm('rentalRemittance', 'rentalRemittanceFormRef')) return;
      let generated = false;
      this.actionBusy = true; this.actionError = '';
      try {
        const result = await generateAdminContractTemplate('rental-remittance', { ...this.templateFields(), ...this.rentalRemittanceForm });
        const file = new File([result.blob], result.filename, { type: 'application/pdf' });
        await uploadAdminRentalMandateDocument(this.currentMandate.id, 'rental_remittance_draft', file);
        this.rentalRemittanceFormOpen = false; await this.loadSigningWorkspace();
        this.page?.showToast?.(options.preserveMasterData ? '已按最新模板生成新版租金汇款授权书，旧版文件已保留' : '租金汇款授权书已生成'); generated = true;
      } catch (error) { this.actionError = error.message || this.$t('rentalFiles.actionFailed'); }
      finally { this.actionBusy = false; }
      if (generated && !options.preserveMasterData) await this.startSigningDirect('rentalRemittance');
    },
    async generateOtr() {
      const mandate = this.currentMandate; if (!mandate?.id || this.selectedCycleIsHistorical || this.actionBusy) return;
      if (!this.validateSupplementForm('otr', 'otrFormRef')) return;
      let generated = false;
      this.actionBusy = true; this.actionError = '';
      try {
        const result = await generateAdminContractTemplate('otr', {
          ...this.templateFields(), ...this.otrForm,
          hasSecondLandlord: String(Boolean(this.otrForm.hasSecondLandlord)),
        });
        const file = new File([result.blob], result.filename, { type: 'application/pdf' }); await uploadAdminRentalMandateDocument(mandate.id, 'otr', file); this.otrFormOpen = false; await this.loadSigningWorkspace(); this.page?.showToast?.(this.$t('rentalFiles.otrGenerated')); generated = true;
      } catch (error) { this.actionError = error.message || this.$t('rentalFiles.actionFailed'); }
      finally { this.actionBusy = false; }
      if (generated) await this.startSigningDirect('otr');
    },
    async generateLeaseDraft() {
      const lease = this.currentLease; const leaseId = lease?.id || lease?.leaseId;
      if (!leaseId || this.selectedCycleIsHistorical || this.actionBusy) return;
      if (!this.leaseSelectedChecklistIds.length) {
        this.actionError = this.$t('rentalFiles.leaseChecklistRequired');
        return;
      }
      if (!(this.leaseSelectedPhotoIds.length + this.leasePhotoFiles.length)) {
        this.actionError = this.$t('rentalFiles.leasePhotoRequired');
        return;
      }
      if (!this.validateSupplementForm('lease', 'leaseFormRef')) return;
      this.actionBusy = true; this.actionError = '';
      let generated = false;
      try {
        if (this.leasePhotoFiles.some(file => file.size > 10 * 1024 * 1024)) {
          throw new Error(this.$t('rentalFiles.supplement.propertyPhotos.fileTooLarge'));
        }
        const property = this.selectedProperty;
        const hadNewPhotos = this.leasePhotoFiles.length > 0;
        for (const photo of [...this.leasePhotoFiles]) {
          const saved = await createAdminPropertyPhoto(property.ownerId, property.ownerUnitId, {
            category: 'interior', description: this.$t('rentalFiles.supplement.propertyPhotos.contractUploadNote'),
            sortOrder: 0, cover: false,
          }, photo);
          if (saved?.id) this.leaseSelectedPhotoIds.push(String(saved.id));
          this.leasePhotoFiles = this.leasePhotoFiles.filter(file => file !== photo);
        }
        this.leaseSelectedPhotoIds = [...new Set(this.leaseSelectedPhotoIds)];
        this.leaseForm.photoIds = this.leaseSelectedPhotoIds.join(',');
        this.leaseSelectedChecklistIds = [...new Set(this.leaseSelectedChecklistIds)];
        this.leaseForm.handoverChecklistIds = this.leaseSelectedChecklistIds.join(',');
        await this.syncLeaseHandoverChecklist();
        if (hadNewPhotos) await this.loadLeasePropertyPhotos();
        const handover = this.currentMandate?.id
          ? await fetchAdminPropertyHandover(this.currentMandate.id).catch(() => null)
          : null;
        const fields = this.tenancyAgreementFields(lease, undefined, handover || {});
        await saveAdminLeaseAgreementDetails(leaseId, this.leaseForm);
        const result = await generateAdminContractTemplate('tenancy-agreement', { ...fields, ...this.leaseForm });
        const file = new File([result.blob], result.filename, { type: 'application/pdf' });
        await uploadAdminGeneratedLeaseContract(leaseId, file);
        this.leaseFormOpen = false;
        this.leaseGenerationStartsSigning = false;
        await this.loadSigningWorkspace();
        this.page?.showToast?.(this.$t('rentalFiles.leaseRegenerated')); generated = true;
      } catch (error) { this.actionError = error.message || this.$t('rentalFiles.actionFailed'); }
      finally { this.actionBusy = false; }
      if (generated && this.leaseContract) await this.startSigningDirect('lease');
    },
    async submitSigning() {
      if (this.selectedCycleIsHistorical || this.actionBusy || !this.signingPanel) return;
      this.actionBusy = true; this.actionError = '';
      try {
        let result = null;
        if (this.multiSignerPanel) {
          const payload = {
            signers: this.signerPackageForm.map(signer => ({ signerRole: signer.signerRole, signerName: signer.signerName, signerEmail: signer.signerEmail })),
            expiresInDays: 7,
          };
          if (this.signingPanel === 'lease') {
            const leaseId = this.currentLease?.id || this.currentLease?.leaseId;
            if (!leaseId || !this.leaseContract) throw new Error(this.$t('rentalFiles.notGenerated'));
            result = await startAdminLeaseSignaturePackage(leaseId, payload);
          } else {
            const document = this.signingPanel === 'pma' ? this.pmaDocument : this.signingPanel === 'rentalAppointment' ? this.rentalAppointmentDocument : this.signingPanel === 'terminationLetter' ? this.terminationLetterDocument : this.otrMandateDocument;
            if (!this.currentMandate?.id || !document?.id) throw new Error(this.$t('rentalFiles.notGenerated'));
            result = await startAdminMandateSignaturePackage(this.currentMandate.id, document.id, payload);
          }
        } else if (this.signingPanel === 'rentalAppointment') {
          if (!this.currentMandate?.id || !this.rentalAppointmentDocument?.id) throw new Error(this.$t('rentalFiles.notGenerated'));
          result = await startAdminMandateDocumentSignature(this.currentMandate.id, this.rentalAppointmentDocument.id, { signerName: this.signerForm.name, signerEmail: this.signerForm.email, expiresInDays: 7, signerRole: 'owner' });
        } else if (this.signingPanel === 'authorization') {
          if (!this.currentMandate?.id || !this.authorizationDocument?.id) throw new Error(this.$t('rentalFiles.notGenerated'));
          result = await startAdminMandateDocumentSignature(this.currentMandate.id, this.authorizationDocument.id, { signerName: this.signerForm.name, signerEmail: this.signerForm.email, expiresInDays: 7, signerRole: 'owner' });
        } else if (this.signingPanel === 'rentalRemittance') {
          if (!this.currentMandate?.id || !this.rentalRemittanceDocument?.id) throw new Error(this.$t('rentalFiles.notGenerated'));
          result = await startAdminMandateDocumentSignature(this.currentMandate.id, this.rentalRemittanceDocument.id, { signerName: this.signerForm.name, signerEmail: this.signerForm.email, expiresInDays: 7, signerRole: 'owner' });
        }
        const links = Array.isArray(result?.signingLinks) && result.signingLinks.length
          ? result.signingLinks
          : result?.signingUrl ? [{ ...result, signerRole: this.activeSignerRole || 'owner', signerName: this.signerForm.name, signerEmail: this.signerForm.email }] : [];
        this.generatedSigningLinks = links.map(link => ({ ...link, deliveryEmail: link.signerEmail || '' }));
        this.signingEmailBusyId = null; this.signingEmailSentIds = []; this.signingDeliveryError = '';
        await this.loadSigningWorkspace();
        await this.revealGeneratedSigningLinks();
        this.page?.showToast?.(this.$t('rentalFiles.signingStarted'));
      } catch (error) { this.actionError = error.message || this.$t('rentalFiles.actionFailed'); }
      finally { this.actionBusy = false; }
    },
    async loadTemplateVersions() {
      const types = ['property-management-agreement', 'management-authorization', 'termination-letter', 'rental-remittance'];
      const results = await Promise.all(types.map(type => fetchAdminContractTemplateVersion(type).catch(() => null)));
      this.templateVersions = Object.fromEntries(types.map((type, index) => [type, results[index]]));
    },
    chooseTemplate(type) { const input = this.$refs[`template-${type}`]; (Array.isArray(input) ? input[0] : input)?.click(); },
    async replaceTemplate(type, event) {
      const file = event.target.files?.[0]; event.target.value = ''; if (!file || this.actionBusy) return;
      this.actionBusy = true; this.actionError = '';
      try {
        const version = await replaceAdminContractTemplate(type, file);
        this.templateVersions = { ...this.templateVersions, [type]: version };
        this.page?.showToast?.(this.$t('rentalFiles.templateReplaced'));
        await this.openTemplateDesigner(type);
      }
      catch (error) { this.actionError = error.message || this.$t('rentalFiles.actionFailed'); }
      finally { this.actionBusy = false; }
    },
    async openTemplateDesigner(type) {
      this.templateDesignerType = type;
      this.templateDesignerOpen = true;
      this.templatePage = 1;
      this.templateSelectedFieldId = '';
      await this.loadTemplateDesigner();
    },
    async loadTemplateDesigner() {
      if (!this.templateDesignerType) return;
      this.templateDesignerLoading = true;
      this.templateDesignerError = '';
      this.templateDesignerActionError = '';
      try {
        this.templatePdf?.destroy?.();
        const [blob, layout] = await Promise.all([
          fetchAdminContractTemplateFile(this.templateDesignerType),
          fetchAdminContractTemplateLayout(this.templateDesignerType),
        ]);
        this.templateLayout = JSON.parse(JSON.stringify(layout));
        this.templatePdf = markRaw(await getDocument({ data: await blob.arrayBuffer() }).promise);
        this.templatePage = Math.min(Math.max(1, this.templatePage), this.templatePdf.numPages);
        this.templateSelectedFieldId = this.templatePageFields[0]?.id || '';
        this.templateDesignerLoading = false;
        await this.$nextTick();
        await this.renderTemplatePage();
      } catch (error) {
        this.templateDesignerError = error.message || this.$t('rentalFiles.templateDesignerLoadFailed');
        this.templateDesignerLoading = false;
      }
    },
    async renderTemplatePage() {
      if (!this.templatePdf || !this.$refs.templateCanvas) return;
      const page = await this.templatePdf.getPage(this.templatePage);
      const viewport = page.getViewport({ scale: this.templateScale });
      const canvas = this.$refs.templateCanvas;
      canvas.width = Math.ceil(viewport.width);
      canvas.height = Math.ceil(viewport.height);
      canvas.style.width = `${viewport.width}px`;
      canvas.style.height = `${viewport.height}px`;
      await page.render({ canvasContext: canvas.getContext('2d'), viewport }).promise;
    },
    async changeTemplatePage(page, selectFirstField = true) {
      this.templatePage = Math.min(Math.max(1, Number(page)), Number(this.templateLayout?.pages || 1));
      if (selectFirstField) this.templateSelectedFieldId = this.templatePageFields[0]?.id || '';
      await this.$nextTick();
      await this.renderTemplatePage();
    },
    async changeTemplateFieldPage(event) {
      if (!this.templateSelectedField) return;
      const targetPage = Math.min(Math.max(1, Number(event.target.value)), Number(this.templateLayout?.pages || 1));
      if (targetPage === Number(this.templateSelectedField.page)) return;
      const targetSize = this.templateLayout?.pageSizes?.[targetPage - 1] || { width: 595, height: 842 };
      this.templateSelectedField.page = targetPage;
      this.templateSelectedField.x = Math.round(Math.max(0, (Number(targetSize.width) - Number(this.templateSelectedField.maxWidth || 20)) / 2) * 2) / 2;
      this.templateSelectedField.y = Math.round(Number(targetSize.height) / 2 * 2) / 2;
      await this.changeTemplatePage(targetPage, false);
    },
    selectTemplateField(field) { this.templateSelectedFieldId = field.id; },
    templateFieldStyle(field) {
      const signature = this.isTemplateSignatureField(field);
      return {
        left: `${Number(field.x) * this.templateScale}px`,
        top: `${(Number(this.templatePageSize.height) - Number(field.y)) * this.templateScale}px`,
        width: `${Math.max(44, this.templateSignatureWidth(field) * this.templateScale)}px`,
        ...(signature ? { height: `${Math.max(18, this.templateSignatureHeight(field) * this.templateScale)}px` } : {}),
        fontSize: `${Math.max(9, Number(field.fontSize) * this.templateScale)}px`,
      };
    },
    isTemplateSignatureField(field) { return String(field?.fieldKey || '').startsWith('signature.'); },
    templateSignatureWidth(field) { return Number.isFinite(Number(field?.maxWidth)) ? Number(field.maxWidth) : 120; },
    templateSignatureHeight(field) { return Number.isFinite(Number(field?.lineHeight)) ? Number(field.lineHeight) : 28; },
    templateSignatureRole(field) {
      return { owner: '业主', company: '物业管理公司', customer_service: '客服见证人' }[String(field?.fieldKey || '').replace('signature.', '')] || '签署人';
    },
    beginTemplateFieldDrag(field, event) {
      if (event.button !== 0) return;
      event.preventDefault();
      this.templateSelectedFieldId = field.id;
      const move = pointerEvent => {
        const overlay = this.$refs.templateOverlay;
        if (!overlay) return;
        const bounds = overlay.getBoundingClientRect();
        const x = Math.max(0, Math.min(Number(this.templatePageSize.width), (pointerEvent.clientX - bounds.left) / this.templateScale));
        const y = Math.max(0, Math.min(Number(this.templatePageSize.height), Number(this.templatePageSize.height) - (pointerEvent.clientY - bounds.top) / this.templateScale));
        field.x = Math.round(x * 2) / 2;
        field.y = Math.round(y * 2) / 2;
      };
      const up = () => {
        window.removeEventListener('pointermove', move);
        window.removeEventListener('pointerup', up);
        this.templateDrag = null;
      };
      this.templateDrag = { move, up };
      window.addEventListener('pointermove', move);
      window.addEventListener('pointerup', up, { once: true });
    },
    async saveTemplateDesigner() {
      if (!this.templateLayout || this.templateDesignerSaving) return;
      this.templateDesignerSaving = true;
      this.templateDesignerActionError = '';
      try {
        const saved = await saveAdminContractTemplateLayout(this.templateDesignerType, this.templateLayout);
        this.templateLayout = JSON.parse(JSON.stringify(saved));
        this.page?.showToast?.(this.$t('rentalFiles.templatePositionsSaved'));
        this.closeTemplateDesigner();
      } catch (error) {
        this.templateDesignerActionError = error.message || this.$t('rentalFiles.templateDesignerSaveFailed');
      } finally {
        this.templateDesignerSaving = false;
      }
    },
    closeTemplateDesigner() {
      if (this.templateDrag) {
        window.removeEventListener('pointermove', this.templateDrag.move);
        window.removeEventListener('pointerup', this.templateDrag.up);
      }
      this.templateDrag = null;
      this.templatePdf?.destroy?.();
      this.templatePdf = null;
      this.templateLayout = null;
      this.templateDesignerOpen = false;
      this.templateDesignerType = '';
      this.templateDesignerError = '';
      this.templateDesignerActionError = '';
    },
    signerRoleLabel(role) {
      if (role === 'second_owner') return '第二业主';
      if (role === 'witness') return '见证人';
      const keys = { owner: 'signerRoleOwner', company: 'signerRoleCompany', customer_service: 'signerRoleCustomerService', tenant: 'signerRoleTenant', tenant_witness: 'signerRoleTenantWitness', owner_witness: 'signerRoleOwnerWitness' };
      return this.$t(`rentalFiles.${keys[role] || keys.owner}`);
    },
    async sendSigningEmail(link) { if (!link?.requestId || !link?.signingUrl || this.signingEmailBusyId) return; const email = String(link.deliveryEmail || '').trim(); if (!email) { this.signingDeliveryError = this.$t('rentalFiles.deliveryEmailRequired'); return; } if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) { this.signingDeliveryError = this.$t('rentalFiles.deliveryEmailInvalid'); return; } this.signingEmailBusyId = link.requestId; this.signingDeliveryError = ''; try { await sendAdminElectronicSignatureInvitation(link.requestId, link.signingUrl, email); link.signerEmail = email; if (!this.signingEmailSentIds.includes(link.requestId)) this.signingEmailSentIds = [...this.signingEmailSentIds, link.requestId]; } catch (error) { this.signingDeliveryError = error.message || this.$t('rentalFiles.emailSendFailed'); } finally { this.signingEmailBusyId = null; } },
    async sendSigningWhatsApp(link) {
      if (!link?.requestId || !link?.signingUrl || this.signingWhatsAppBusyId || (link.whatsappStatus && link.whatsappStatus !== 'ready')) return;
      this.signingWhatsAppBusyId = link.requestId; this.signingDeliveryError = '';
      let submitted = false;
      try {
        const current = await fetchAdminSignatureWhatsApp(link.requestId);
        link.whatsappStatus = current.status;
        if (current.status !== 'ready') return;
        if (!current.available) throw new Error(this.$t('rentalFiles.whatsappUnavailable'));
        let phone = String(link.deliveryPhone ?? current.phone ?? '').trim();
        link.deliveryPhone = phone;
        link.deliveryCountry ??= '';
        if (phone && !phone.startsWith('+') && link.deliveryCountry) {
          const checked = validatePhone(phone, link.deliveryCountry, true);
          if (checked.valid) phone = checked.e164;
        }
        if (!/^\+[1-9][0-9]{7,14}$/.test(phone.replace(/[ ()-]/g, ''))) {
          link.whatsappNeedsPhone = true;
          throw new Error(this.$t('rentalFiles.whatsappPhoneRequired'));
        }
        link.deliveryPhone = phone;
        submitted = true;
        const result = await sendAdminSignatureWhatsApp(link.requestId, link.signingUrl, phone);
        link.whatsappStatus = result.status;
      } catch (error) {
        if (submitted) link.whatsappStatus = 'unknown';
        this.signingDeliveryError = error.message || this.$t('rentalFiles.whatsappCheckResult');
      } finally { this.signingWhatsAppBusyId = null; }
    },
    async copySigningLink(url) { if (!url) return; try { await navigator.clipboard.writeText(url); } catch { const input = document.createElement('textarea'); input.value = url; input.style.position = 'fixed'; input.style.opacity = '0'; document.body.appendChild(input); input.select(); document.execCommand('copy'); input.remove(); } this.page?.showToast?.(this.$t('rentalFiles.linkCopied')); },
    closeSigningPanel() { this.signingPanel = ''; this.signerPackageForm = []; this.signerPackageLoading = false; this.generatedSigningLinks = []; this.signingEmailBusyId = null; this.signingEmailSentIds = []; this.signingDeliveryError = ''; this.actionError = ''; },
    categoryLabel(category) { return this.$t(`rentalFiles.categories.${category}`); },
    fileStatusLabel(file) { const lifecycle = rentalFileLifecycle(file); const keys = { generated: 'statusGenerated', signed: 'statusSigned', archived: 'statusStored', pending: 'statusPending', disabled: 'statusDisabled', stored: 'statusStored' }; return this.$t(`rentalFiles.${keys[lifecycle] || keys.stored}`); },
    fileStatusClass(file) { const lifecycle = rentalFileLifecycle(file); return ({ generated: 'ready', signed: 'complete', archived: 'archived', pending: 'pending', disabled: 'disabled', stored: 'archived' })[lifecycle] || 'archived'; },
    formatDate(value) { return formatDateTime(value); },
    async downloadFile(file) {
      this.downloadingKey = file.key; this.actionError = '';
      try { let result; if (file.kind === 'mandate') result = await downloadAdminRentalMandateDocument(file.mandateId, file.id); else if (file.kind === 'contract') result = file.contractType === 'L_LEASE' ? await fetchAdminLeaseContract(file.leaseId || file.id, true) : await downloadAdminPropertyContractRecord(file.ownerId, file.ownerUnitId, file.id); else if (file.kind === 'handover') result = await downloadAdminPropertyHandoverReport(file.ownerId, file.ownerUnitId, file.id); else if (file.kind === 'property') result = await downloadAdminPropertyAttachment(file.ownerId, file.ownerUnitId, file.id); if (!result?.blob) return; const url = URL.createObjectURL(result.blob); const link = document.createElement('a'); link.href = url; link.download = file.name; document.body.appendChild(link); link.click(); link.remove(); setTimeout(() => URL.revokeObjectURL(url), 1000); }
      catch (error) { this.actionError = error.message || this.$t('rentalFiles.downloadFailed'); }
      finally { this.downloadingKey = ''; }
    },
  },
};
</script>

<style scoped>
/* Hallmark · genre: operational workspace · tone: clear and restrained · anchor: teal · fingerprint: selectable photo library */
/* Hallmark · pre-emit critique: P5 H5 E4 S5 R5 V4 · macrostructure: in-form selection library · contrast: pass (40–41) · slop: pass */
/* Hallmark · pre-emit critique: P5 H5 E4 S5 R5 V4 */
/* Hallmark · component: signing-link-delivery · genre: modern-minimal · tone: professional · anchor: teal · fingerprint: generated-link ledger */
/* Hallmark · macrostructure: generated-link-ledger · theme: existing CCPS · contrast: pass (40–41) · responsive: pass (34,49,50–57) */
.rental-signing-page{display:grid;min-width:0;overflow-x:hidden;gap:18px;padding:16px 20px 28px}.rental-signing-header{display:flex;align-items:flex-start;justify-content:space-between;gap:20px;padding:4px 2px}.rental-signing-header>div:first-child{display:grid;gap:6px}.rental-signing-header span,.rental-signing-property-head span,.rental-signing-panel header span{color:#0b8f96;font-size:10px;font-weight:800;letter-spacing:1.4px}.rental-signing-header h2{margin:0;color:#12375f;font-size:24px}.rental-signing-header p{margin:0;color:#6c7f8e;font-size:13px}.rental-signing-total{display:grid;justify-items:end;gap:2px;padding:10px 16px;border:1px solid #d6e5e8;border-radius:10px;background:#f3fafb;color:#0a737b}.rental-signing-total strong{font-size:20px}.rental-signing-total small{font-size:11px}.rental-signing-layout{display:grid;grid-template-columns:minmax(240px,290px) minmax(0,1fr);width:100%;min-width:0;gap:16px;height:calc(100vh - 190px);min-height:0}.rental-signing-property-picker,.rental-signing-main{min-width:0;border:1px solid #dce7ed;border-radius:12px;background:#fff}.rental-signing-property-picker{display:grid;grid-template-rows:auto auto auto minmax(0,1fr);align-self:start;max-height:100%;gap:11px;padding:16px 0;overflow:hidden}.rental-signing-picker-head{display:flex;align-items:center;justify-content:space-between;margin:0 16px;color:#173f5c}.rental-signing-picker-head span{padding:3px 8px;border-radius:999px;background:#edf6f7;color:#08717a;font-size:11px}.rental-signing-property-picker>input{box-sizing:border-box;width:calc(100% - 32px);height:38px;margin:0 16px;border:1px solid #cad8e3;border-radius:7px;padding:0 10px}.rental-signing-property-list{min-width:0;min-height:0;overflow-y:auto;padding:0 16px}.rental-signing-property-list button{display:grid;grid-template-columns:30px minmax(0,1fr) 22px;align-items:center;gap:8px;width:100%;border:1px solid transparent;border-radius:9px;background:#fff;padding:9px;text-align:left;color:#35576d;cursor:pointer;transition:background .18s,border-color .18s}.rental-signing-property-list button:hover{background:#f4f9fa}.rental-signing-property-list button.active{border-color:#72c9ce;background:#eaf9f9;color:#075e68;box-shadow:inset 3px 0 #0b979f}.rental-signing-property-list button>span:nth-child(2){display:grid;gap:3px;min-width:0}.rental-signing-property-list strong,.rental-signing-property-list small{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.rental-signing-property-list strong{font-size:12px}.rental-signing-property-list small{color:#7b8d99;font-size:10px}.rental-signing-property-list i{display:grid;place-items:center;width:20px;height:20px;border-radius:999px;font-style:normal}.rental-signing-property-list button.active i{background:#d5f1f2;color:#06747c}.rental-signing-property-icon{display:grid;place-items:center;width:30px;height:30px;border-radius:8px;background:#eef5f6;color:#19737a}.rental-signing-main{height:100%;box-sizing:border-box;padding:20px;overflow-x:hidden;overflow-y:auto}.rental-signing-property-head{display:flex;align-items:flex-start;justify-content:space-between;gap:16px;padding:4px 0 18px;border-bottom:1px solid #e8eef2}.rental-signing-property-head>div{display:grid;min-width:0;gap:4px}.rental-signing-property-head h3{overflow:hidden;margin:0;color:#12375f;font-size:20px;text-overflow:ellipsis;white-space:nowrap}.rental-signing-property-head p{overflow:hidden;margin:0;color:#718595;font-size:12px;text-overflow:ellipsis;white-space:nowrap}.rental-signing-property-head>strong{flex:0 0 auto;padding:5px 9px;border-radius:999px;background:#edf6f7;color:#08717a;font-size:10px}.rental-signing-actions,.rental-generated-files{min-width:0}.rental-signing-section-head{display:flex;align-items:flex-start;justify-content:space-between;gap:12px;margin:18px 0 12px}.rental-signing-section-head h4{margin:0;color:#173f5c;font-size:15px}.rental-signing-section-head p{margin:3px 0 0;color:#7a8d9b;font-size:11px}.rental-signing-section-head>span{flex:0 0 auto;margin:0;padding:4px 9px;border-radius:999px;background:#f1f6f7;color:#687f8d;font-size:10px}.rental-signing-card-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px}.rental-signing-card{display:grid;align-content:start;gap:13px;min-width:0;padding:15px;border:1px solid #dce7ed;border-radius:11px;background:#fbfdfe}.rental-signing-card.highlighted{box-shadow:0 0 0 3px rgba(11,143,150,.12)}.rental-signing-card.is-complete{border-color:#b9dfc7;background:#f5fcf7}.rental-signing-card.is-pending{border-color:#e5ce88;background:#fffbf1}.rental-signing-card.is-unavailable{opacity:.68}.rental-signing-card-head{display:grid;grid-template-columns:34px minmax(0,1fr) auto;align-items:start;gap:9px}.rental-signing-card-head>span{display:grid;place-items:center;width:32px;height:32px;border-radius:9px;background:#eaf9f9;color:#0b8f96;font-weight:800}.rental-signing-card-head>div{display:grid;gap:4px;min-width:0}.rental-signing-card-head strong{color:#173f5c;font-size:13px}.rental-signing-card-head small{color:#718595;font-size:10px;line-height:1.4}.rental-signing-card-head em{padding:4px 7px;border-radius:999px;background:#edf2f4;color:#718391;font-size:9px;font-style:normal;white-space:nowrap}.is-complete .rental-signing-card-head em{background:#e4f8ed;color:#168047}.is-pending .rental-signing-card-head em{background:#fff0c7;color:#9b6900}.rental-signing-card dl{display:grid;gap:7px;margin:0}.rental-signing-card dl>div{display:grid;gap:3px}.rental-signing-card dt{color:#8a9aa4;font-size:9px}.rental-signing-card dd{overflow:hidden;margin:0;color:#547083;font-size:10px;text-overflow:ellipsis;white-space:nowrap}.rental-signing-card>button{justify-self:start;height:32px;border:1px solid #0b8f96;border-radius:7px;background:#0b8f96;padding:0 11px;color:#fff;font-size:10px}.rental-signing-card>button:disabled{opacity:.55}.rental-signing-panel{display:grid;gap:14px;margin-top:16px;padding:16px 18px;border:1px solid #a8d8db;border-radius:11px;background:#f5fbfb}.rental-signing-panel header{display:flex;align-items:flex-start;justify-content:space-between;gap:12px}.rental-signing-panel header>div{display:grid;gap:4px}.rental-signing-panel h4{margin:0;color:#173f5c}.rental-signing-panel header p{margin:0;color:#718595;font-size:11px}.rental-signing-panel header>button{width:28px;height:28px;border:1px solid #c7dfe2;border-radius:7px;background:#fff;color:#547083;font-size:18px}.rental-signing-form{display:grid;grid-template-columns:1fr 1fr;gap:12px}.rental-signing-form label{display:grid;gap:6px;color:#31556b;font-size:11px;font-weight:700}.rental-signing-form input{box-sizing:border-box;width:100%;min-width:0;height:36px;border:1px solid #c9dce2;border-radius:7px;padding:0 9px}.rental-signing-form p{grid-column:1/-1;margin:0;color:#a13f2d;font-size:11px}.rental-signing-form>div{display:flex;grid-column:1/-1;justify-content:flex-end;gap:8px}.rental-signing-form button{min-height:34px;border-radius:7px;padding:0 13px}.rental-signing-form .secondary{border:1px solid #c7dfe2;background:#fff;color:#31556b}.rental-signing-form .primary{border:1px solid #0b8f96;background:#0b8f96;color:#fff}.rental-signing-status-detail{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:10px}.rental-signing-status-detail>div{display:grid;gap:5px;padding:11px;border:1px solid #dce7ed;border-radius:8px;background:#fff}.rental-signing-status-detail span{color:#718595;font-size:9px}.rental-signing-status-detail strong{overflow-wrap:anywhere;color:#29475f;font-size:11px}.rental-files-toolbar{display:grid;grid-template-columns:minmax(0,1fr) minmax(150px,190px);width:100%;min-width:0;gap:10px;margin-bottom:12px}.rental-files-toolbar input,.rental-files-toolbar select{box-sizing:border-box;width:100%;min-width:0;height:38px;border:1px solid #cad8e3;border-radius:7px;background:#fff;padding:0 10px}.rental-files-table-wrap{max-width:100%;overflow:auto;border:1px solid #dbe7ec;border-radius:10px}.rental-files-table{width:100%;min-width:900px;border-collapse:collapse}.rental-files-table th{padding:10px 11px;background:#f1f7f8;color:#496377;font-size:11px;text-align:left}.rental-files-table td{padding:12px 11px;border-top:1px solid #e4ecef;color:#29475f;font-size:12px}.rental-files-table td:first-child{display:grid;gap:4px;max-width:280px}.rental-files-table td:first-child strong,.rental-files-table td:first-child small{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.rental-files-table td:first-child small{color:#84949f;font-size:10px}.rental-file-category,.rental-file-status{display:inline-flex;padding:4px 8px;border-radius:999px;background:#edf3f6;color:#547083;font-size:10px;white-space:nowrap}.rental-file-status.complete{background:#e4f8ed;color:#168047}.rental-file-status.pending{background:#fff3d5;color:#9b6900}.rental-file-status.disabled{background:#eef1f2;color:#76858e}.rental-file-download{height:30px;border:1px solid #9fd5d8;border-radius:7px;background:#fff;padding:0 11px;color:#08717a}.rental-signing-state,.rental-signing-empty{display:grid;place-items:center;align-content:center;gap:8px;min-height:180px;color:#718595;font-size:12px;text-align:center}.rental-signing-state{min-height:120px;padding:14px}.rental-signing-state.error,.rental-signing-empty.error,.rental-signing-inline-error{color:#c43a3a}.rental-signing-state button,.rental-signing-empty button{border:1px solid #cbdde2;border-radius:7px;background:#fff;padding:7px 11px;color:#176f78}.rental-signing-empty.compact{min-height:150px;border:1px dashed #c9dfe3;border-radius:10px;background:#fbfdfe}.rental-signing-inline-error{margin:10px 0 0;padding:9px 10px;border-radius:7px;background:#fff1f2;font-size:11px}@media(max-width:1200px){.rental-signing-layout{grid-template-columns:250px minmax(0,1fr)}.rental-signing-card-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(max-width:900px){.rental-signing-layout{grid-template-columns:1fr;height:auto}.rental-signing-property-list{max-height:250px}.rental-signing-main{min-height:500px}}@media(max-width:620px){.rental-signing-page{padding:12px}.rental-signing-header,.rental-signing-property-head{flex-direction:column}.rental-signing-total{justify-items:start}.rental-signing-card-grid,.rental-signing-form,.rental-files-toolbar,.rental-signing-status-detail{grid-template-columns:1fr}}
.rental-files-table{table-layout:fixed;border-collapse:separate;border-spacing:0}.rental-files-table th{padding:11px 14px;font-weight:700;white-space:nowrap}.rental-files-table th:first-child{width:24%}.rental-files-table th:nth-child(2){width:15%}.rental-files-table th:nth-child(3){width:22%}.rental-files-table th:nth-child(4){width:14%}.rental-files-table th:nth-child(5){width:16%}.rental-files-table th:last-child{width:90px}.rental-files-table td{height:58px;padding:10px 14px;background:#fff;vertical-align:middle}.rental-files-table td:first-child{display:table-cell;max-width:none}.rental-files-table tbody tr:hover td{background:#f8fcfc}.rental-file-name{display:grid;min-width:0;gap:4px}.rental-file-name strong,.rental-file-name small,.rental-file-mandate{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.rental-file-name strong{color:#173f5c;font-size:12px}.rental-file-name small{color:#84949f;font-size:10px}.rental-file-mandate{color:#355b73}.rental-files-table time{color:#607a8d;white-space:nowrap}.rental-file-category,.rental-file-status{padding:5px 9px;font-weight:600}.rental-file-status.ready{background:#e7f6fb;color:#147b98}.rental-file-download{border-color:#84cbd0;padding:0 12px;font-weight:700;cursor:pointer}.rental-file-download:hover:not(:disabled){background:#eaf9f9}.rental-file-download:disabled{opacity:.55;cursor:default}
.rental-signing-package-form{display:grid;gap:12px}.rental-signing-package-summary{display:grid;gap:4px;padding:11px 12px;border-radius:8px;background:#eaf7f7;color:#31556b}.rental-signing-package-summary strong{font-size:12px}.rental-signing-package-summary span{font-size:10px}.rental-signing-package-form>section{display:grid;grid-template-columns:1fr 1fr;gap:10px;padding:12px;border:1px solid #d5e5e8;border-radius:9px;background:#fff}.rental-signing-package-form>section>header{display:flex;grid-column:1/-1;align-items:center;justify-content:flex-start;gap:8px}.rental-signing-package-form>section>header span{padding:3px 7px;border-radius:999px;background:#edf6f7;color:#08717a;font-size:9px}.rental-signing-package-form>section>header strong{color:#173f5c;font-size:12px}.rental-signing-package-form label{display:grid;gap:6px;color:#31556b;font-size:11px;font-weight:700}.rental-signing-package-form input{box-sizing:border-box;width:100%;min-width:0;height:36px;border:1px solid #c9dce2;border-radius:7px;padding:0 9px}.rental-signing-package-form>p{margin:0;color:#a13f2d;font-size:11px}.rental-signing-package-form>div:last-child{display:flex;justify-content:flex-end;gap:8px}.rental-signing-package-form button{min-height:34px;border-radius:7px;padding:0 13px}.rental-signing-package-form .secondary{border:1px solid #c7dfe2;background:#fff;color:#31556b}.rental-signing-package-form .primary{border:1px solid #0b8f96;background:#0b8f96;color:#fff}@media(max-width:620px){.rental-signing-package-form>section{grid-template-columns:1fr}}
.rental-signing-prefilled-name{display:grid;grid-column:1/-1;gap:4px;padding:10px 11px;border:1px solid #cfe3e6;border-radius:8px;background:#f5fbfb}.rental-signing-prefilled-name span,.rental-signing-prefilled-name small{color:#718895;font-size:10px}.rental-signing-prefilled-name strong{color:#173f5c;font-size:13px}
/* Keep each recipient and their delivery channels in one readable card. */
.rental-signing-link-results{display:grid;gap:16px}
.rental-signing-link-summary{display:flex;align-items:center;gap:12px;padding:14px 16px;border:1px solid #b8dfe1;border-radius:10px;background:#effafa;color:#31556b}
.rental-signing-link-summary>span{display:grid;place-items:center;flex:0 0 30px;height:30px;border-radius:50%;background:#08747c;color:#fff;font-size:14px;font-weight:700}
.rental-signing-link-summary>div{display:grid;gap:4px}
.rental-signing-link-summary strong{color:#08747c;font-size:15px}
.rental-signing-link-summary small{font-size:13px;line-height:1.6}
.rental-signing-link-results article{display:grid;grid-template-columns:minmax(0,1fr);gap:20px;padding:20px;border:1px solid #d7e5e8;border-radius:12px;background:#fff;min-width:0}
.rental-signing-link-person{display:grid;gap:4px;min-width:0}
.rental-signing-link-person strong{color:#203f50;font-size:16px;line-height:1.5;overflow-wrap:anywhere}
.rental-signing-link-person small{color:#617987;font-size:13px;line-height:1.5}
.rental-signing-link-share{display:grid;gap:12px;min-width:0}
.rental-signing-link-results article label{display:grid;gap:8px;min-width:0;color:#526b78;font-size:13px;font-weight:600}
.rental-signing-link-results article input{box-sizing:border-box;width:100%;min-width:0;min-height:44px;border:1px solid #c9dce2;border-radius:8px;background:#fff;padding:10px 12px;color:#31556b;font:inherit;font-size:14px;line-height:1.5}
.rental-signing-link-results article input[readonly]{background:#f5f8f9;font-weight:400;text-overflow:ellipsis}
.rental-signing-link-actions{display:flex;flex-wrap:wrap;align-items:flex-start;gap:8px}
.rental-signing-link-actions button,.rental-signing-link-actions a,.rental-signing-link-email>button,.rental-signing-link-results>footer button{display:inline-flex;align-items:center;justify-content:center;box-sizing:border-box;min-height:44px;border:1px solid #b8d2d6;border-radius:8px;background:#fff;padding:10px 16px;color:#08747c;font:inherit;font-size:14px;font-weight:600;line-height:1.5;text-align:center;text-decoration:none;cursor:pointer}
.rental-signing-link-email{display:flex;align-items:flex-end;flex-wrap:wrap;gap:12px;padding-top:20px;border-top:1px solid #e5edef;min-width:0}
.rental-signing-link-email>label{flex:1 1 240px;max-width:520px}
.rental-signing-link-email>button{background:#f1f8f8}
.rental-signing-link-results :is(button,a,input):focus-visible{outline:3px solid #12375f;outline-offset:3px}
.rental-signing-link-results button:disabled{cursor:not-allowed;opacity:.55}
.rental-signing-link-email button[data-state="success"]{border-color:#a5d3b4;background:#eaf8ef;color:#216442}
.rental-signing-delivery-error{margin:0;padding:12px 16px;border:1px solid #e6aaaa;border-radius:8px;background:#fff3f3;color:#a72929;font-size:13px;line-height:1.6}
.rental-signing-link-results>footer{display:flex;justify-content:flex-end}
.rental-signing-link-results>footer button{background:#08747c;border-color:#08747c;color:#fff;min-width:96px}
@media(hover:hover){.rental-signing-link-actions :is(button,a):hover:not(:disabled),.rental-signing-link-email>button:hover:not(:disabled){background:#e9f7f7;border-color:#08747c}.rental-signing-link-results>footer button:hover{background:#05636b}}
@media(max-width:520px){.rental-signing-link-results article{padding:16px;gap:16px}.rental-signing-link-actions>*{flex:1 1 120px}.rental-signing-link-email>label{flex-basis:100%;max-width:none}.rental-signing-link-email>button{width:100%}.rental-signing-link-results article input{font-size:16px}}

.rental-signing-card-actions{display:flex;align-items:center;flex-wrap:wrap;gap:7px}.rental-signing-card-actions>button{height:32px;border:1px solid #0b8f96;border-radius:7px;background:#0b8f96;padding:0 11px;color:#fff;font-size:10px;cursor:pointer}.rental-signing-card-actions>button.secondary{border-color:#b8d7da;background:#fff;color:#08717a}.rental-signing-card-actions>small{width:100%;color:#80919b;font-size:9px}.rental-template-input{display:none}.rental-signer-role{display:grid;grid-column:1/-1;grid-template-columns:auto 1fr;align-items:center;gap:10px;padding:10px 12px;border:1px solid #d8e7ea;border-radius:8px;background:#fff}.rental-signer-role span{color:#758995;font-size:10px}.rental-signer-role strong{color:#0a737b;font-size:12px}
.pma-form-backdrop{position:fixed;z-index:1200;inset:0;display:grid;place-items:center;padding:24px;background:rgba(13,36,47,.58)}.pma-form-dialog{display:grid;grid-template-rows:auto minmax(0,1fr) auto auto;width:min(900px,calc(100vw - 40px));max-height:calc(100vh - 48px);overflow:hidden;border-radius:14px;background:#fff;box-shadow:0 24px 70px rgba(8,34,47,.28)}.pma-form-dialog>header{display:flex;align-items:flex-start;justify-content:space-between;gap:20px;padding:20px 24px 16px;border-bottom:1px solid #e2ebee}.pma-form-dialog>header>div{display:grid;gap:4px}.pma-form-dialog>header span{color:#0b8f96;font-size:10px;font-weight:800;letter-spacing:1.2px}.pma-form-dialog>header h3{margin:0;color:#173f5c;font-size:20px}.pma-form-dialog>header p{margin:0;color:#718595;font-size:11px}.pma-form-dialog>header button{width:30px;height:30px;border:1px solid #c9dce2;border-radius:8px;background:#fff;color:#47677b;font-size:20px}.pma-form-body{display:grid;gap:18px;padding:18px 24px;overflow-y:auto}.pma-form-body section{display:grid;gap:11px}.pma-form-body h4{margin:0;padding-bottom:8px;border-bottom:1px solid #edf1f3;color:#1b5069;font-size:13px}.pma-form-grid{display:grid;grid-template-columns:1fr 1fr;gap:12px}.pma-form-grid.dates{grid-template-columns:repeat(3,1fr)}.pma-form-grid label{display:grid;gap:6px;color:#31556b;font-size:11px;font-weight:700}.pma-form-grid label.wide{grid-column:1/-1}.pma-form-grid input,.pma-form-grid textarea,.pma-form-grid select{box-sizing:border-box;width:100%;min-width:0;border:1px solid #c8dbe1;border-radius:7px;background:#fff;padding:9px 10px;color:#173f5c;font:inherit}.pma-form-grid input,.pma-form-grid select{height:38px}.pma-form-grid textarea{resize:vertical;line-height:1.45}.pma-form-dialog.is-validated .pma-form-grid input:invalid,.pma-form-dialog.is-validated .pma-form-grid textarea:invalid,.pma-form-dialog.is-validated .pma-form-grid select:invalid{border-color:#d9363e;background:#fff8f8;box-shadow:0 0 0 2px rgba(217,54,62,.14);scroll-margin-block:120px}.pma-form-dialog.is-validated .pma-form-grid input:invalid:focus,.pma-form-dialog.is-validated .pma-form-grid textarea:invalid:focus,.pma-form-dialog.is-validated .pma-form-grid select:invalid:focus{outline:2px solid #d9363e;outline-offset:2px;box-shadow:0 0 0 4px rgba(217,54,62,.14)}.pma-bank-sync{display:flex;align-items:flex-start;gap:10px;padding:11px 12px;border:1px solid #c7e5e6;border-radius:8px;background:#f3fbfb;color:#244f61;cursor:pointer}.pma-bank-sync input{margin-top:2px;accent-color:#0b8f96}.pma-bank-sync span{display:grid;gap:3px}.pma-bank-sync strong{font-size:12px}.pma-bank-sync small{color:#728793;font-size:10px;font-weight:400}.pma-form-error{margin:0 24px;padding:9px 11px;border-radius:7px;background:#fff1f2;color:#bd3333;font-size:11px}.pma-form-dialog>footer{display:flex;justify-content:flex-end;gap:9px;padding:14px 24px 18px;border-top:1px solid #e5edef}.pma-form-dialog>footer button{min-height:38px;border-radius:8px;padding:0 16px}.pma-form-dialog>footer .secondary{border:1px solid #c7dce1;background:#fff;color:#31556b}.pma-form-dialog>footer .primary{border:1px solid #0b8f96;background:#0b8f96;color:#fff}@media(max-width:700px){.pma-form-backdrop{padding:10px}.pma-form-dialog{width:calc(100vw - 20px);max-height:calc(100vh - 20px)}.pma-form-grid,.pma-form-grid.dates{grid-template-columns:1fr}.pma-form-grid label.wide{grid-column:auto}}
.admin-shell .pma-form-dialog.is-validated .pma-form-grid input:invalid,.admin-shell .pma-form-dialog.is-validated .pma-form-grid textarea:invalid,.admin-shell .pma-form-dialog.is-validated .pma-form-grid select:invalid{border-color:#d9363e!important}
.rental-appointment-form-dialog{width:min(960px,calc(100vw - 40px))}.rental-appointment-missing{display:grid;gap:5px;border:1px solid #efbd70;border-radius:9px;background:#fff8e8;padding:11px 13px;color:#885b0a}.rental-appointment-missing strong{font-size:12px}.rental-appointment-missing span{font-size:11px;line-height:1.55}.rental-appointment-secondary-owner{margin-top:2px;border-left:3px solid #66c4c8;padding:12px;background:#f7fbfc}.pma-form-dialog>footer .primary:disabled{cursor:not-allowed;opacity:.48}
.rental-signing-property-head>strong.historical{background:#f1f3f5;color:#687986}.rental-cycle-selector{display:grid;grid-template-columns:minmax(0,1fr) minmax(270px,390px);align-items:center;gap:18px;margin-top:14px;padding:13px 15px;border:1px solid #dce8eb;border-radius:10px;background:#f8fbfc}.rental-cycle-selector>div{display:grid;gap:3px;min-width:0}.rental-cycle-selector span{color:#0b8f96;font-size:9px;font-weight:800;letter-spacing:1px}.rental-cycle-selector strong{color:#173f5c;font-size:13px}.rental-cycle-selector small{color:#758995;font-size:10px}.rental-cycle-selector select{box-sizing:border-box;width:100%;min-width:0;height:38px;border:1px solid #bfd5dc;border-radius:8px;background:#fff;padding:0 10px;color:#294c63}.rental-cycle-readonly{margin-top:10px;padding:9px 12px;border:1px solid #e2e7ea;border-radius:8px;background:#f6f7f8;color:#667985;font-size:11px}.rental-property-files{min-width:0;margin-top:18px;padding-top:1px;border-top:1px solid #edf1f3}.rental-property-file-list{display:grid;border:1px solid #dbe7ec;border-radius:10px;overflow:hidden}.rental-property-file-list article{display:grid;grid-template-columns:minmax(0,1fr) 145px auto;align-items:center;gap:14px;padding:11px 13px;background:#fbfdfe}.rental-property-file-list article+article{border-top:1px solid #e4ecef}.rental-property-file-list article>div{display:grid;min-width:0;gap:3px}.rental-property-file-list strong,.rental-property-file-list small{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.rental-property-file-list strong{color:#29475f;font-size:11px}.rental-property-file-list small,.rental-property-file-list article>span{color:#7d8f9b;font-size:10px}@media(max-width:900px){.rental-cycle-selector{grid-template-columns:1fr}.rental-property-file-list article{grid-template-columns:minmax(0,1fr) auto}.rental-property-file-list article>span{display:none}}@media(max-width:620px){.rental-cycle-selector{padding:12px}.rental-property-file-list article{grid-template-columns:1fr}.rental-property-file-list .rental-file-download{justify-self:start}}
.finance-document-backdrop{position:fixed;z-index:1250;inset:0;display:grid;place-items:center;padding:24px;background:rgba(13,36,47,.6)}.finance-document-dialog{display:grid;grid-template-rows:auto auto minmax(0,1fr) auto auto;width:min(980px,calc(100vw - 40px));max-height:calc(100vh - 48px);overflow:hidden;border-radius:15px;background:#fff;box-shadow:0 24px 72px rgba(8,34,47,.3)}.finance-document-dialog>header{display:flex;align-items:flex-start;justify-content:space-between;gap:20px;padding:20px 24px 16px;border-bottom:1px solid #e2ebee}.finance-document-dialog>header>div{display:grid;gap:4px}.finance-document-dialog>header span{color:#0b8f96;font-size:10px;font-weight:800;letter-spacing:1.2px}.finance-document-dialog>header h3{margin:0;color:#173f5c;font-size:21px}.finance-document-dialog>header p{margin:0;color:#718595;font-size:12px}.finance-document-dialog>header button{width:32px;height:32px;border:1px solid #f0b8b8;border-radius:8px;background:#fff5f5;color:#c73737;font-size:21px;cursor:pointer}.finance-document-context{display:grid;grid-template-columns:1fr 1fr 1.2fr;gap:12px;padding:15px 24px;border-bottom:1px solid #e7eef0;background:#f8fbfc}.finance-document-context>div{display:grid;align-content:start;gap:5px;min-width:0}.finance-document-context small{color:#7a8c98;font-size:10px}.finance-document-context strong{overflow:hidden;color:#173f5c;font-size:13px;text-overflow:ellipsis;white-space:nowrap}.finance-document-type>div{display:grid;grid-template-columns:1fr 1fr;gap:6px}.finance-document-type button{height:34px;border:1px solid #c8dce1;border-radius:7px;background:#fff;color:#31566d;font-weight:700;cursor:pointer}.finance-document-type button.active{border-color:#0b8f96;background:#eaf9f9;color:#08747c;box-shadow:inset 0 0 0 1px #0b8f96}.finance-document-body{min-height:280px;padding:16px 24px;overflow-y:auto}.finance-document-state{display:grid;place-items:center;align-content:center;gap:10px;min-height:260px;border:1px dashed #cbdfe3;border-radius:11px;background:#fbfdfe;color:#718595;font-size:13px}.finance-document-state.error{color:#bd3333}.finance-document-state button{border:1px solid #c7dce1;border-radius:7px;background:#fff;padding:7px 12px;color:#08717a}.finance-document-select-all{display:flex;align-items:center;gap:9px;margin-bottom:10px;padding:10px 12px;border:1px solid #dbe7eb;border-radius:9px;background:#f7fafb;color:#244a62;font-size:12px;font-weight:700}.finance-document-select-all em{margin-left:auto;color:#718595;font-size:10px;font-style:normal;font-weight:500}.finance-document-list{display:grid;gap:8px}.finance-document-list>label{display:grid;grid-template-columns:auto 34px minmax(0,1fr) minmax(140px,auto) auto;align-items:center;gap:11px;padding:12px;border:1px solid #dce7ed;border-radius:10px;background:#fff;cursor:pointer}.finance-document-list>label:hover,.finance-document-list>label.selected{border-color:#80cbd0;background:#f3fbfb}.finance-document-record-icon{display:grid;place-items:center;width:32px;height:32px;border-radius:9px;background:#eaf9f9;color:#07858d;font-size:18px;font-weight:800}.finance-document-record-main,.finance-document-record-meta{display:grid;min-width:0;gap:4px}.finance-document-record-main strong,.finance-document-record-main small{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.finance-document-record-main strong{color:#173f5c;font-size:12px}.finance-document-record-main small,.finance-document-record-meta small{color:#7b8e9b;font-size:10px}.finance-document-record-meta{text-align:right}.finance-document-record-meta strong{color:#143e5b;font-size:12px}.finance-document-list>label>em{padding:4px 8px;border-radius:999px;background:#fff3d5;color:#9b6900;font-size:9px;font-style:normal;white-space:nowrap}.finance-document-list>label>em.is-confirmed{background:#e5f8ed;color:#148047}.finance-document-list>label>em.is-rejected{background:#fff0f0;color:#bd3333}.finance-document-error{margin:0 24px;padding:9px 11px;border-radius:7px;background:#fff1f2;color:#bd3333;font-size:11px}.finance-document-dialog>footer{display:flex;align-items:center;justify-content:space-between;gap:16px;padding:14px 24px 18px;border-top:1px solid #e5edef}.finance-document-dialog>footer>span{color:#657d8c;font-size:11px}.finance-document-dialog>footer>div{display:flex;gap:9px}.finance-document-dialog>footer button{min-height:38px;border-radius:8px;padding:0 16px;font-weight:700}.finance-document-dialog>footer .secondary{border:1px solid #c7dce1;background:#fff;color:#31556b}.finance-document-dialog>footer .primary{border:1px solid #0b8f96;background:#0b8f96;color:#fff}.finance-document-dialog>footer .primary:disabled{opacity:.5;cursor:not-allowed}@media(max-width:700px){.finance-document-backdrop{padding:10px}.finance-document-dialog{width:calc(100vw - 20px);max-height:calc(100vh - 20px)}.finance-document-context{grid-template-columns:1fr}.finance-document-list>label{grid-template-columns:auto 30px minmax(0,1fr)}.finance-document-record-meta,.finance-document-list>label>em{grid-column:3;text-align:left}.finance-document-dialog>footer{align-items:stretch;flex-direction:column}.finance-document-dialog>footer>div{display:grid;grid-template-columns:1fr 1fr}}
.template-designer-backdrop{position:fixed;z-index:1300;inset:0;display:grid;place-items:center;padding:18px;background:rgba(8,29,41,.68)}.template-designer-dialog{display:grid;grid-template-rows:auto auto minmax(0,1fr) auto auto;width:min(1180px,calc(100vw - 32px));height:min(880px,calc(100vh - 36px));overflow:hidden;border-radius:15px;background:#fff;box-shadow:0 28px 80px rgba(5,28,39,.36)}.template-designer-dialog>header{display:flex;align-items:flex-start;justify-content:space-between;gap:18px;padding:18px 22px 14px;border-bottom:1px solid #dfeaec}.template-designer-dialog>header>div{display:grid;gap:3px}.template-designer-dialog>header span{color:#0b8f96;font-size:10px;font-weight:800;letter-spacing:1.2px}.template-designer-dialog>header h3{margin:0;color:#173f5c;font-size:21px}.template-designer-dialog>header p{margin:0;color:#718595;font-size:11px}.template-designer-dialog>header>button{width:32px;height:32px;border:1px solid #efb7b7;border-radius:8px;background:#fff5f5;color:#c73737;font-size:21px}.template-designer-toolbar{display:flex;align-items:center;justify-content:space-between;gap:16px;padding:10px 22px;border-bottom:1px solid #e5edef;background:#f8fbfc}.template-designer-toolbar>div{display:grid;gap:2px;color:#173f5c}.template-designer-toolbar small{color:#7a8d99}.template-designer-toolbar nav{display:flex;align-items:center;gap:9px}.template-designer-toolbar nav button{width:32px;height:30px;border:1px solid #bdd5da;border-radius:7px;background:#fff;color:#08747c}.template-designer-toolbar nav span{min-width:100px;color:#31566d;text-align:center}.template-designer-body{display:grid;grid-template-columns:minmax(0,1fr) 270px;min-height:0;background:#e9eff2}.template-canvas-scroll{min-width:0;overflow:auto;padding:22px}.template-canvas-wrap{position:relative;margin:0 auto;background:#fff;box-shadow:0 5px 24px rgba(20,51,67,.18)}.template-canvas-wrap canvas{display:block}.template-field-overlay{position:absolute;inset:0}.template-field-marker{position:absolute;box-sizing:border-box;min-height:22px;transform:translateY(-100%);overflow:hidden;border:1px solid #0b8f96;border-radius:4px;background:rgba(21,175,183,.18);padding:2px 5px;color:#075d65;text-align:left;white-space:nowrap;cursor:grab;touch-action:none}.template-field-marker:hover,.template-field-marker.active{z-index:2;border-color:#e4a400;background:rgba(255,205,52,.3);box-shadow:0 0 0 2px rgba(228,164,0,.18)}.template-field-marker:active{cursor:grabbing}.template-field-marker span{pointer-events:none}.template-field-panel{display:grid;align-content:start;gap:7px;min-height:0;padding:15px;border-left:1px solid #d4e1e5;background:#fff;overflow-y:auto}.template-field-panel>div:first-child{display:flex;justify-content:space-between;margin-bottom:2px;color:#31566d}.template-field-panel>button{display:grid;gap:3px;width:100%;border:1px solid #d8e6e9;border-radius:8px;background:#fff;padding:9px 10px;color:#31566d;text-align:left}.template-field-panel>button.active{border-color:#72c9ce;background:#eaf9f9;color:#075e68}.template-field-panel>button small{color:#81929c}.template-field-panel>p{color:#81929c;text-align:center}.template-field-settings{display:grid;gap:10px;margin-top:6px;padding-top:14px;border-top:1px solid #e3ecee}.template-field-settings h4{margin:0;color:#173f5c}.template-field-settings>div{display:grid;grid-template-columns:1fr 1fr;gap:8px}.template-field-settings label{display:grid;gap:4px;color:#607988;font-size:10px}.template-field-settings input{box-sizing:border-box;width:100%;height:34px;border:1px solid #c8dbe0;border-radius:6px;padding:0 8px}.template-field-settings>small{color:#7b8e99;line-height:1.5}.template-designer-state{display:grid;grid-row:2/5;place-items:center;align-content:center;gap:10px;color:#718595}.template-designer-state.error{color:#bd3333}.template-designer-state button{border:1px solid #c7dce1;border-radius:7px;background:#fff;padding:7px 12px;color:#08717a}.template-designer-error{margin:0;padding:9px 22px;background:#fff1f2;color:#bd3333}.template-designer-dialog>footer{display:flex;align-items:center;justify-content:space-between;gap:16px;padding:13px 22px 16px;border-top:1px solid #e0eaec}.template-designer-dialog>footer>span{color:#718595;font-size:11px}.template-designer-dialog>footer>div{display:flex;gap:9px}.template-designer-dialog>footer button{min-height:38px;border-radius:8px;padding:0 16px;font-weight:700}.template-designer-dialog>footer .secondary{border:1px solid #c7dce1;background:#fff;color:#31556b}.template-designer-dialog>footer .primary{border:1px solid #0b8f96;background:#0b8f96;color:#fff}.template-designer-dialog>footer .primary:disabled{opacity:.5}@media(max-width:800px){.template-designer-body{grid-template-columns:1fr}.template-field-panel{max-height:240px;border-top:1px solid #d4e1e5;border-left:0}.template-designer-dialog>footer{align-items:stretch;flex-direction:column}}
.template-field-settings select{box-sizing:border-box;width:100%;height:34px;border:1px solid #c8dbe0;border-radius:6px;background:#fff;padding:0 8px;color:#31566d}.template-field-settings select:focus-visible{outline:2px solid #0b8f96;outline-offset:1px}.template-field-settings label.template-field-page{grid-column:1/-1}
.pma-profile-sync-hint{margin:0;padding:10px 12px;border:1px solid #c7e5e6;border-radius:8px;background:#f3fbfb;color:#31556b;font-size:11px;line-height:1.5}
.lease-property-photo-section{min-width:0}.lease-photo-picker-head{display:flex;align-items:center;justify-content:space-between;gap:12px}.lease-photo-picker-head h4{flex:1}.lease-photo-picker-head span{flex:none;color:#617d8c;font-size:11px}.lease-photo-picker-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:10px}.lease-photo-choice{position:relative;display:grid;min-width:0;overflow:hidden;border:1px solid #c8dbe1;border-radius:8px;background:#fff;cursor:pointer}.lease-photo-choice:hover{border-color:#6aaeb4;background:#f8fcfc}.lease-photo-choice:focus-within{outline:2px solid #0b8f96;outline-offset:2px}.lease-photo-choice:active{background:#eef8f8}.lease-photo-choice.selected{border-color:#0b8f96;background:#eef9f9;box-shadow:inset 0 0 0 1px #0b8f96}.lease-photo-choice input{position:absolute;top:8px;right:8px;z-index:1;width:18px;height:18px;margin:0;accent-color:#0b8f96}.lease-photo-choice img{display:block;width:100%;aspect-ratio:4/3;object-fit:cover;background:#eaf2f4}.lease-photo-choice span{min-width:0;overflow:hidden;padding:8px 10px;color:#254e63;font-size:11px;font-weight:700;line-height:1.3;text-overflow:ellipsis;white-space:nowrap}.lease-photo-picker-actions{display:flex;align-items:center;gap:8px}.lease-photo-picker-actions button{min-height:36px;border:1px solid #bcd4da;border-radius:7px;background:#fff;padding:0 12px;color:#1d6671;white-space:nowrap}.lease-photo-picker-actions button:hover:not(:disabled){border-color:#6aaeb4;background:#f3fafa}.lease-photo-picker-actions button:focus-visible{outline:2px solid #0b8f96;outline-offset:2px}.lease-photo-picker-actions button:active:not(:disabled){background:#e5f4f4}.lease-photo-picker-actions button:disabled{cursor:not-allowed;opacity:.55}.lease-photo-empty{margin:0;padding:18px;border:1px dashed #c8dbe1;border-radius:8px;background:#f8fbfc;color:#6d8491;text-align:center}.lease-photo-upload{display:grid;gap:6px;padding:12px;border:1px solid #d7e5e8;border-radius:8px;background:#fbfdfd;color:#31556b;font-size:11px;font-weight:700}.lease-photo-upload input{box-sizing:border-box;width:100%;min-height:40px;border:1px solid #c8dbe1;border-radius:7px;background:#fff;padding:7px;color:#173f5c}.lease-photo-upload input:hover{border-color:#6aaeb4}.lease-photo-upload input:focus-visible{outline:2px solid #0b8f96;outline-offset:2px}.lease-photo-upload input:active{background:#f4fafa}.lease-photo-upload input:disabled{cursor:not-allowed;opacity:.55}.lease-photo-upload small{color:#718595;font-weight:400}@media(max-width:700px){.lease-photo-picker-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(max-width:420px){.lease-photo-picker-head{align-items:flex-start;flex-direction:column}.lease-photo-picker-grid{grid-template-columns:minmax(0,1fr)}}
.lease-photo-upload-list{display:grid;gap:7px}.lease-photo-upload-list article{display:flex;align-items:center;justify-content:space-between;gap:12px;min-width:0;padding:9px 11px;border:1px solid #d7e5e8;border-radius:8px;background:#fff}.lease-photo-upload-list article>div{display:grid;min-width:0;gap:2px}.lease-photo-upload-list strong{overflow:hidden;color:#254e63;font-size:11px;text-overflow:ellipsis;white-space:nowrap}.lease-photo-upload-list small{color:#718595;font-size:10px}.lease-photo-upload-list button{flex:none;min-height:30px;border:1px solid #e3b2b2;border-radius:7px;background:#fff7f7;padding:0 11px;color:#b43333;cursor:pointer}.lease-photo-upload-list button:hover{border-color:#cd7777;background:#fff0f0}.lease-photo-upload-list button:focus-visible{outline:2px solid #b43333;outline-offset:2px}
.handover-owner-defects-section{display:grid;gap:12px}.handover-owner-defects-head{display:flex;align-items:flex-start;justify-content:space-between;gap:16px}.handover-owner-defects-head>div{display:grid;gap:4px}.handover-owner-defects-head h4{margin:0;color:#173f5c}.handover-owner-defects-head p{margin:0;color:#718595;font-size:11px}.handover-owner-defects-head>button{flex:none;min-height:34px;border:1px solid #0b8f96;border-radius:7px;background:#0b8f96;padding:0 13px;color:#fff;font-weight:700}.handover-owner-defect-list{display:grid;gap:9px}.handover-owner-defect-list article{display:grid;grid-template-columns:60px minmax(0,1fr) minmax(0,1fr) auto;align-items:start;gap:10px;padding:12px;border:1px solid #d8e7e9;border-radius:9px;background:#fbfdfd}.handover-owner-defect-list article.is-single{grid-template-columns:60px minmax(0,1fr) auto}.handover-owner-defect-list article>strong{padding-top:25px;color:#08717a;font-size:11px}.handover-owner-defect-list label{display:grid;gap:5px;color:#31556b;font-size:11px;font-weight:700}.handover-owner-defect-list textarea{box-sizing:border-box;width:100%;resize:vertical;border:1px solid #c8dbe1;border-radius:7px;background:#fff;padding:8px 9px;color:#173f5c;font:inherit;font-weight:400}.handover-owner-defect-remove{align-self:end;min-height:34px;border:1px solid #e3b2b2;border-radius:7px;background:#fff7f7;padding:0 11px;color:#b43333}@media(max-width:700px){.handover-owner-defects-head{align-items:stretch;flex-direction:column}.handover-owner-defects-head>button{align-self:flex-start}.handover-owner-defect-list article,.handover-owner-defect-list article.is-single{grid-template-columns:1fr}.handover-owner-defect-list article>strong{padding-top:0}.handover-owner-defect-remove{justify-self:start}}
.rental-file-status.archived{background:#fff3d5;color:#9b6900}
.rental-signing-project-filter{display:grid;gap:5px;margin:0 16px;color:#557082;font-size:11px;font-weight:700}.rental-signing-project-filter select{box-sizing:border-box;width:100%;height:36px;border:1px solid #cad8e3;border-radius:7px;background:#fff;padding:0 10px;color:#29475f;font:inherit;font-weight:400}
.template-field-marker.templateSignatureField{min-height:18px;border:2px dashed #c98300;border-radius:7px;background:rgba(255,199,69,.24);padding:3px 6px;color:#8a5600;font-weight:700}

.rental-signing-direct-state{display:grid;place-items:center;gap:7px;min-height:110px;border:1px dashed #a9d5d8;border-radius:9px;background:#fff;color:#607b89;text-align:center}.rental-signing-direct-state strong{color:#08747c;font-size:13px}.rental-signing-direct-state span{font-size:11px}.rental-signing-direct-state.error{border-color:#e3aaaa;background:#fff7f7;color:#a72929}.rental-signing-direct-state.error strong{color:#a72929}.rental-signing-direct-state button{min-height:36px;border:1px solid #0b8f96;border-radius:7px;background:#0b8f96;padding:0 14px;color:#fff;font-weight:700}
    @media(max-width:760px){.rental-signing-link-results article{grid-template-columns:1fr}}
    .rental-handover-report-dialog{width:min(980px,calc(100vw - 40px))}.handover-photo-choice{padding-bottom:8px}.handover-photo-choice select{box-sizing:border-box;width:calc(100% - 16px);min-width:0;height:32px;margin:0 8px;border:1px solid #c8dbe1;border-radius:6px;background:#fff;padding:0 7px;color:#31556b;font-size:10px}.handover-photo-choice textarea{box-sizing:border-box;width:calc(100% - 16px);min-width:0;margin:7px 8px 0;resize:vertical;border:1px solid #c8dbe1;border-radius:6px;background:#fff;padding:7px;color:#31556b;font:inherit;font-size:10px}.lease-photo-upload-list article>select{flex:1;min-width:160px;height:32px;border:1px solid #c8dbe1;border-radius:6px;background:#fff;padding:0 7px;color:#31556b;font-size:10px}.lease-photo-upload-list article>textarea{box-sizing:border-box;flex:1.2;min-width:190px;resize:vertical;border:1px solid #c8dbe1;border-radius:6px;background:#fff;padding:7px;color:#31556b;font:inherit;font-size:10px}.rental-generated-files-anchor{display:block;height:0;outline:none;scroll-margin-top:18px}.rental-generated-files-anchor:focus{box-shadow:0 0 0 3px rgba(11,143,150,.18)}@media(max-width:700px){.rental-handover-report-dialog{width:calc(100vw - 20px)}.lease-photo-upload-list article{align-items:stretch;flex-direction:column}.lease-photo-upload-list article>select,.lease-photo-upload-list article>textarea{width:100%;min-width:0}}
/* Hallmark · genre: operational workspace · tone: clear and restrained · anchor: teal · fingerprint: property photo and handover selection libraries */
/* Hallmark · pre-emit critique: P5 H5 E4 S5 R5 V4 · macrostructure: grouped in-form selection · contrast: pass · slop: pass */
.lease-handover-checklist-section{min-width:0}.lease-checklist-head{display:flex;align-items:flex-start;justify-content:space-between;gap:16px}.lease-checklist-head>div{display:grid;gap:4px}.lease-checklist-head h4{margin:0}.lease-checklist-head p{max-width:720px;margin:0;color:#6b8290;font-size:11px;line-height:1.55}.lease-checklist-head>strong{flex:none;padding:5px 8px;border:1px solid #bcdfe1;border-radius:7px;background:#f2fbfb;color:#08747c;font-size:11px}.lease-checklist-groups{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px}.lease-checklist-groups fieldset{display:grid;align-content:start;gap:6px;min-width:0;margin:0;padding:11px;border:1px solid #d7e5e8;border-radius:9px;background:#fbfdfd}.lease-checklist-groups legend{padding:0 6px;color:#254e63;font-size:11px;font-weight:800}.lease-checklist-groups label{display:grid;grid-template-columns:18px minmax(0,1fr) auto;align-items:center;gap:8px;min-width:0;padding:8px 9px;border:1px solid transparent;border-radius:7px;color:#3d5d70;font-size:11px;font-weight:500;cursor:pointer}.lease-checklist-groups label:hover{border-color:#b7d9dc;background:#f4fbfb}.lease-checklist-groups label:focus-within{outline:2px solid #0b8f96;outline-offset:1px}.lease-checklist-groups label:active{background:#e9f7f7}.lease-checklist-groups label.selected{border-color:#80cbd0;background:#eef9f9;color:#124f5b}.lease-checklist-groups input{width:16px;height:16px;margin:0;accent-color:#0b8f96}.lease-checklist-groups span{min-width:0;overflow-wrap:anywhere}.lease-checklist-groups small{min-width:24px;color:#718595;text-align:right}.lease-checklist-groups label.selected small{color:#08747c}@media(max-width:700px){.lease-checklist-groups{grid-template-columns:1fr}}@media(max-width:420px){.lease-checklist-head{flex-direction:column}.lease-checklist-head>strong{align-self:flex-start}}
</style>
