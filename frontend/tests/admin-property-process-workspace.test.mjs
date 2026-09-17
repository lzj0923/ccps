import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync(new URL('../src/components/AdminPropertyProcessWorkspace.vue', import.meta.url), 'utf8');
const adminThemeSource = readFileSync(new URL('../src/admin-theme.css', import.meta.url), 'utf8');
const workbenchSource = readFileSync(new URL('../src/utils/rentalCycleWorkbench.js', import.meta.url), 'utf8');
const i18nSource = readFileSync(new URL('../src/i18n/index.js', import.meta.url), 'utf8');

test('does not expose move-in handover as a standalone rental-process step', () => {
  assert.doesNotMatch(workbenchSource, /moveInCollection|complete_move_in_handover/);
  assert.doesNotMatch(source, /moveInCollection|complete_move_in_handover/);
});

test('keeps handover reports optional and outside the system rental gate', () => {
  assert.match(source, /class="rental-action-wide rental-handover-report-launcher"/);
  assert.doesNotMatch(source, /openSigningHandover|saveAdminPropertyHandover/);
  assert.doesNotMatch(workbenchSource, /missingItems,[\s\S]{0,120}\['handover'\]/);
});

test('process center uses the current rental workbench model', () => {
  assert.match(source, /buildRentalWorkbench/);
  assert.match(source, /fetchAdminPropertyPhotos/);
  assert.match(source, /fetchAdminLeaseRentInvoices/);
  assert.match(source, /fetchAdminLeasePayments/);
  assert.match(source, /workspaceRelated/);
  assert.match(source, /rentalWorkbench/);
  assert.match(source, /currentTask/);
  assert.match(source, /stage\.blockingReasonKey/);
  assert.match(source, /stage\.ownerRole/);
  assert.match(source, /navigate\(`/);
  assert.match(source, /target\.type === 'property'/);
  assert.match(source, /target\.type === 'rentalMandate'/);
  assert.match(source, /target\.type === 'tenancy'/);
  assert.match(source, /rental-action-panel/);
  assert.match(source, /submitAction/);
  assert.match(source, /createAdminRentalMandate/);
  assert.doesNotMatch(source, /reviewAdminRentalMandate/);
  assert.doesNotMatch(source, /activeAction === 'review_mandate'/);
  assert.match(source, /createAdminTenant/);
  assert.match(source, /createAdminLease/);
  assert.match(source, /closeAdminLease/);
  assert.match(source, /createAdminLeaseFirstInvoice/);
  assert.match(source, /create_first_invoice/);
  assert.doesNotMatch(source, /confirm_first_receipt/);
  assert.match(source, /confirmAdminRentCollection/);
  assert.match(source, /saveAdminPropertyWorkspaceBasic/);
  assert.doesNotMatch(source, /saveAdminPropertyHandover/);
  assert.match(source, /complete_handover/);
  assert.match(source, /complete_property_data/);
  assert.match(source, /conditionSummary/);
  assert.match(source, /keyCount/);
  assert.match(source, /createAdminPropertyHandoverChecklistItem/);
  assert.match(source, /updateAdminPropertyHandoverChecklistItem/);
  assert.match(source, /createAdminPropertyPhoto/);
  assert.match(source, /createAdminPropertyHandoverReport/);
  assert.match(source, /handoverChecklist/);
  assert.match(source, /handoverPhotoFiles/);
  assert.match(source, /downloadAdminPropertyHandoverReport/);
  assert.match(source, /inject: \['page'\]/);
  assert.match(source, /canPerformStageAction/);
  assert.match(source, /roleActionHint/);
  assert.match(source, /:disabled="!canPerformStageAction/);
});

test('keeps rental attachment browsing outside the process center', () => {
  assert.doesNotMatch(source, /viewGeneratedFiles/);
  assert.doesNotMatch(source, /generatedFilesOpen/);
  assert.doesNotMatch(source, /downloadGeneratedFile/);
});

test('normalizes lease-scoped invoice IDs before evaluating the current rental', () => {
  assert.match(source, /invoiceId:\s*item\.invoiceId/);
  assert.match(source, /leaseId:\s*leaseId/);
});

test('confirms collection from the specific unpaid invoice row instead of a global first-receipt action', () => {
  assert.doesNotMatch(source, /confirm_first_receipt/);
  assert.match(source, /v-if="!operationsInvoicePaid"[^>]*@click="openOperationsForm\('receipt'\)"/);
  assert.match(source, /confirmAdminRentCollection/);
});

test('keeps rent collection outside the system rental workflow', () => {
  assert.match(source, /rental-operations-panel/);
  assert.match(source, /rentalWorkbench\.dailyOperations/);
  assert.match(source, /openOperationsAction/);
  assert.match(source, /create_first_invoice/);
  assert.doesNotMatch(source, /confirm_first_receipt/);
});

test('prefills the owner management fee from property settings when creating a rental mandate', () => {
  assert.match(source, /const profile = this\.workspace\?\.profile \|\| \{\};/);
  assert.match(source, /managementFee: Number\(mandate\.managementFee \?\? profile\.managementFeeAmount \?\? 0\)/);
  assert.match(source, /commissionPercent: Number\(mandate\.commissionPercent \?\? profile\.managementFeePercent \?\? 0\)/);
  assert.match(source, /processCenter\.actionFields\.ownerManagementFeeHint/);
  assert.match(i18nSource, /ownerManagementFeeHint:\s*\['代管服务费由业主承担/);
});

test('provides a lease closure action followed by a move-out handover report', () => {
  assert.match(source, /leaseClosure/);
  assert.match(source, /close_lease/);
  assert.match(source, /complete_move_out_handover/);
  assert.match(source, /handoverType[,}]/);
  assert.match(source, /generateHandoverReport\('move_out'\)/);
  assert.match(source, /leaseClosure\?\.leaseId/);
});

test('keeps move-out handover report generation available after a report already exists', () => {
  assert.doesNotMatch(source, /leaseClosure\.leaseId && !rentalWorkbench\.leaseClosure\.handoverReportReady/);
  assert.match(source, /handoverReportReady\s*\?\s*\$t\('rentalFiles\.regenerateHandoverReport'\)/);
});

test('does not load a move-in completion record to gate rental operations', () => {
  assert.doesNotMatch(source, /\bfetchAdminPropertyHandover\b/);
  assert.doesNotMatch(source, /handover:\s*this\.workspaceRelated\.handover/);
});

test('allows selecting an existing tenant before creating a new tenant', () => {
  assert.match(source, /fetchAdminTenancyOptions/);
  assert.match(source, /existingTenantId/);
  assert.match(source, /workspaceRelated\.tenants/);
  assert.match(source, /this\.actionForm\.existingTenantId/);
  assert.match(source, /use_existing_tenant/);
});

test('process center switches one detail page from the selected journey step', () => {
  assert.match(source, /@click="selectJourneyStep\(step\)"/);
  assert.match(source, /selectedJourneyStep\?\.key === step\.key/);
  assert.match(source, /selectedJourneyStep\.key === 'propertySetup'/);
  assert.match(source, /selectedJourneyStep\.key === 'billingOperations'/);
  assert.match(source, /selectedJourneyStep\.key === 'leaseClosure'/);
  assert.doesNotMatch(source, /v-for="stage in rentalWorkbench\.stages"/);
  assert.doesNotMatch(source, /paginateProcessSteps/);
  assert.doesNotMatch(source, /process-pagination/);
});

test('property preparation exposes the required checklist and photo upload controls', () => {
  const handoverOnlyBlock = source.match(/<template v-if="activeAction === 'complete_handover' && rentalWorkbench\.currentMandate">([\s\S]*?)<\/template>/)?.[1] || '';
  assert.doesNotMatch(handoverOnlyBlock, /handoverPhotos/);
  assert.doesNotMatch(handoverOnlyBlock, /handoverChecklist/);
});

test('keeps the handover checklist compact and full width', () => {
  assert.match(source, /\.rental-action-wide\{[^}]*grid-column:1\/-1/);
  assert.match(source, /\.rental-checklist-row\{[^}]*grid-template-columns:minmax\(0,1\.5fr\) 100px minmax\(140px,1fr\) 30px/);
  assert.match(source, /\.rental-handover-checklist\{[^}]*max-height:260px/);
});

test('opens the handover checklist in an optional edit modal', () => {
  assert.match(source, /checklistModalOpen/);
  assert.match(source, /rental-checklist-launcher/);
  assert.match(source, /@click="checklistModalOpen = true"/);
  assert.match(source, /v-if="checklistModalOpen"/);
  assert.match(source, /editChecklist/);
});

test('makes checklist editing explicit with separate quantity, notes, add and delete actions', () => {
  assert.match(source, /item\.notes/);
  assert.match(source, /checklistNotes/);
  assert.match(source, /removeHandoverChecklistItem/);
  assert.match(source, /deleteAdminPropertyHandoverChecklistItem/);
  assert.match(source, /rental-checklist-add-panel/);
  assert.match(source, /rental-checklist-delete/);
});

test('lets users choose the checklist section and keeps the edit footer visible', () => {
  assert.match(source, /newChecklistCategory/);
  assert.match(source, /checklistCategory/);
  assert.match(source, /v-model="actionForm\.newChecklistCategory"/);
  assert.match(source, /grid-template-rows:auto minmax\(0,1fr\) auto auto/);
  assert.match(source, /\.rental-checklist-modal-list\{[^}]*overflow-y:auto/);
});

test('uses a stable checklist category catalog and labels the add form fields', () => {
  assert.match(source, /const HANDOVER_CATEGORIES = \[/);
  assert.match(source, /newChecklistCategory: '客廳 Living Room'/);
  assert.match(source, /checklistItem/);
  assert.match(source, /rental-checklist-add-controls/);
});

test('renders the checklist category picker outside native select clipping', () => {
  assert.match(source, /rental-checklist-category-picker/);
  assert.match(source, /checklistCategoryOpen/);
  assert.match(source, /chooseHandoverChecklistCategory/);
  assert.match(source, /\.rental-checklist-modal-card\{[^}]*overflow:visible/);
});

test('ports the category menu outside the checklist scroll container', () => {
  assert.match(source, /<Teleport to="body">/);
  assert.match(source, /toggleHandoverChecklistCategory/);
  assert.match(source, /checklistCategoryMenuStyle/);
  assert.match(source, /\.rental-checklist-category-menu\{[^}]*position:fixed/);
});

test('keeps the checklist category trigger as a direct button interaction', () => {
  assert.match(source, /<div class="rental-checklist-category-field">/);
  assert.match(source, /type="button" class="rental-checklist-category-trigger"[^>]*@pointerdown\.prevent\.stop="toggleHandoverChecklistCategory"/);
});

test('keeps the category menu options available independently of checklist rows', () => {
  assert.match(source, /handoverCategoryOptions\(\).*HANDOVER_CATEGORIES\.map/);
  assert.match(source, /@click="chooseHandoverChecklistCategory\(category\.value\)"/);
  assert.match(source, /v-for="category in handoverCategoryOptions"/);
  assert.match(source, /\.rental-checklist-category-menu\{[^}]*min-height:160px/);
});

test('process center keeps the workflow list inside the panel padding', () => {
  assert.match(source, /\.process-center-main\{[^}]*padding:16px/);
});

test('uses the left context rail for selecting one of multiple properties', () => {
  assert.match(source, /<div class="process-shell">/);
  assert.match(source, /<aside class="process-context-rail">/);
  assert.match(source, /v-if="properties\.length > 1" v-model\.trim="search"/);
  assert.match(source, /v-for="property in filteredProperties"/);
  assert.match(source, /@click="selectProperty\(property\)"/);
  assert.match(source, /\.process-shell\{[^}]*grid-template-columns:1fr/);
  assert.match(source, /class="process-start-button process-rail-start"/);
  assert.doesNotMatch(source, /<div class="process-center-count">/);
});

test('process property picker can filter by project from a dropdown', () => {
  assert.match(source, /process-project-filter/);
  assert.match(source, /\.process-project-filter\{[^}]*margin:0/);
  assert.match(source, /\.process-project-filter select\{[^}]*width:100%/);
  assert.match(source, /v-model="selectedProjectName"/);
  assert.match(source, /v-for="project in projectOptions"/);
  assert.match(source, /selectedProjectName/);
});

test('uses the page scrollbar instead of a nested workbench scrollbar', () => {
  assert.match(source, /\.process-center-layout\{[^}]*height:auto/);
  assert.match(source, /\.process-center-layout\{[^}]*min-height:0/);
  assert.match(source, /\.process-center-main\{[^}]*height:auto/);
  assert.match(source, /\.process-center-main\{[^}]*overflow:visible/);
  assert.doesNotMatch(source, /process-pagination/);
});

test('opens the selected journey step at its exact editable record', () => {
  assert.match(source, /class="process-step-edit-button"/);
  assert.match(source, /editSelectedJourneyStep/);
  assert.match(source, /type: 'property', tab: 'basic', action: 'edit'/);
  assert.match(source, /type: 'rentalMandate', action: 'edit'/);
  assert.match(source, /type: 'tenantDirectory', action: 'edit', tenantId/);
  assert.match(source, /tab: 'rentalManagement', rentalTab: 'lease', leaseId/);
  assert.match(source, /openOperationsCenter\('billing'\)/);
  assert.match(source, /openOperationsCenter\('maintenance'\)/);
  assert.match(source, /openOperationsCenter\('lease'\)/);
  assert.match(source, /from: 'rental-process'/);
});

test('removes the duplicate outer scrollbar from the process context rail', () => {
  assert.match(adminThemeSource, /\.admin-shell \.process-context-rail\{[^}]*max-height:none;[^}]*overflow:visible/);
  assert.match(source, /\.process-property-list\{[^}]*overflow-y:auto/);
});

test('keeps process content away from the page and panel edges', () => {
  assert.match(source, /\.property-process-center\{[^}]*padding:14px 20px 24px/);
  assert.match(source, /\.process-center-main\{[^}]*padding:16px/);
  assert.match(source, /\.process-property-list\{[^}]*padding:0 16px/);
});

test('keeps the current task and workflow cards compact', () => {
  assert.match(source, /\.rental-current-task\{[^}]*margin:12px 0 14px/);
  assert.match(source, /\.rental-current-task\{[^}]*padding:12px 14px/);
  assert.match(source, /\.rental-stage-list\{[^}]*gap:8px/);
  assert.match(source, /\.rental-stage-card\{[^}]*padding:11px 14px/);
});

test('keeps the admin content width inside the flex layout on narrow viewports', () => {
  assert.match(adminThemeSource, /\.admin-shell \.main\{[^}]*width:auto;max-width:none;[^}]*flex:1/);
  assert.match(source, /\.property-process-center\{[^}]*width:100%;max-width:100%;min-width:0/);
  assert.match(source, /@media\(min-width:1450px\)\{\.process-shell\{grid-template-columns:minmax\(280px,290px\) minmax\(0,1fr\)\}/);
  assert.match(source, /container-type:inline-size/);
  assert.match(source, /@container \(max-width:1000px\)\{\.process-shell\{grid-template-columns:1fr\}/);
  assert.match(source, /\.process-workspace\{[^}]*min-width:0;max-width:100%;overflow-x:hidden/);
});

test('uses unit id for the selected property workspace and detail route', () => {
  assert.match(source, /const unitId = property\.unitId \|\| property\.id \|\| property\.ownerUnitId/);
  assert.match(source, /fetchAdminPropertyWorkspaceById\(unitId\)/);
  assert.match(source, /this\.selectedProperty = \{ \.\.\.this\.selectedProperty, \.\.\.this\.workspace\.property \}/);
  assert.match(source, /const propertyId = this\.selectedProperty\?\.unitId \|\| this\.selectedProperty\?\.id \|\| ownerUnitId/);
  assert.match(source, /`\/admin\/properties\/\$\{encodeURIComponent\(propertyId\)\}/);
});

test('uses the existing handover report flow instead of a generic attachment upload', () => {
  assert.match(source, /createAdminPropertyHandoverReport/);
  assert.match(source, /downloadAdminPropertyHandoverReport/);
  assert.match(source, /generateHandoverReport/);
  assert.match(source, /mandateId: this\.rentalWorkbench\.currentMandate/);
  assert.match(source, /rental-handover-report-launcher/);
  assert.doesNotMatch(source, /actionFields\.handoverAttachments/);
});

test('allows the handover report to be generated without photos', () => {
  assert.match(source, /:disabled="actionBusy \|\| !actionForm\.handoverChecklist\?\.length"/);
  assert.doesNotMatch(source, /missingItems\.photos/);
});

test('uses backend-supported metadata for handover photos', () => {
  assert.match(source, /category: 'other'/);
  assert.match(source, /rentalStage: leaseId \? 'after' : null/);
  assert.doesNotMatch(source, /rentalStage: 'handover'/);
});

test('assigns report photos to a PDF section and supports reusing property photos', () => {
  assert.match(source, /HANDOVER_PHOTO_SECTIONS/);
  assert.match(source, /handoverPhotoSection/);
  assert.match(source, /reusableHandoverPhotoIds/);
  assert.match(source, /adminPropertyPhotoContentUrl/);
  assert.match(source, /photoSelection\.photoMeta/);
});

test('supports multiple new handover photos with per-photo module metadata', () => {
  assert.match(source, /handoverPhotos[^\n]*multiple/);
  assert.match(source, /handoverPhotoItems/);
  assert.match(source, /photo\.section/);
  assert.match(source, /photoMeta: photos\.map\(item => \(\{ section: item\.section/);
});

test('shows reusable property photos with thumbnails and their module', () => {
  assert.match(source, /adminPropertyPhotoContentUrl\([^)]*photo\.id/);
  assert.match(source, /rental-handover-reuse-thumb/);
  assert.match(source, /rental-handover-reuse-section/);
  assert.match(source, /adminPropertyPhotoContentUrl/);
});

test('loads reusable thumbnails through authenticated blob previews', () => {
  assert.match(source, /async loadReusablePhotoPreview/);
  assert.match(source, /URL\.createObjectURL\(blob\)/);
  assert.match(source, /:src="photo\.previewUrl \|\| reusablePhotoUrl\(photo\)"/);
  assert.match(source, /@error="loadReusablePhotoPreview\(photo\)"/);
});

test('selected stage page exposes its workflow action without attachment shortcuts', () => {
  assert.match(source, /selectedSystemStage\?\.primaryAction/);
  assert.match(source, /@click="openStage\(selectedSystemStage\)"/);
  assert.doesNotMatch(source, /generated-files-link/);
  assert.doesNotMatch(source, /class="history-link"/);
});

test('starts a complete rental journey from project, owner, and property setup', () => {
  assert.match(source, /legacy\.t_e46922c4e850/);
  assert.match(source, /process-journey-track/);
  assert.match(source, /openSetupWizard/);
  assert.match(source, /createAdminProject/);
  assert.match(source, /createAdminOwner/);
  assert.match(source, /createAdminOwnerProperty/);
  assert.match(source, /setupStep === 1/);
  assert.match(source, /legacy\.t_2be6c56a71a9/);
});

test('rejects a duplicate project code in the first setup step instead of the final submission', () => {
  assert.match(source, /checkAdminProjectCodeAvailability/);
  assert.match(source, /@input="scheduleSetupProjectCodeCheck"/);
  assert.match(source, /async checkSetupProjectCode/);
  assert.match(source, /const available = await this\.checkSetupProjectCode\(\)/);
  assert.match(source, /setupProjectCodeStatus === 'duplicate'/);
});

test('new rental flow only accepts handed-over properties', () => {
  assert.doesNotMatch(source, /<option value="PRE_HANDOVER">未交房<\/option>/);
  assert.doesNotMatch(source, /setupPropertyForm\.assetStage === 'PRE_HANDOVER'/);
  assert.match(source, /setupPropertyForm:\s*\{[^}]*assetStage:\s*'OPERATING'/);
  assert.match(source, /v-model="setupPropertyForm\.actualHandoverDate" type="date" required/);
  assert.match(source, /if \(!this\.setupPropertyForm\.actualHandoverDate\)/);
});

test('journey navigation combines project owner property and handover into one preparation step', () => {
  assert.doesNotMatch(source, /key:\s*'projectRecord'/);
  assert.doesNotMatch(source, /key:\s*'ownerRecord'/);
  assert.doesNotMatch(source, /key:\s*'propertyRecord'/);
  assert.match(source, /item\.key === 'propertySetup' \? this\.\$t\('processCenter\.journeyPropertySetup'\)/);
  assert.match(source, /selectedJourneyStep\.key === 'propertySetup'/);
  assert.match(source, /legacy\.t_cf545c9c1bf9/);
  assert.match(source, /legacy\.t_a39a3f21f732/);
  assert.match(source, /legacy\.t_ec0053796f17/);
  assert.match(source, /legacy\.t_3a6e5d931db1/);
  assert.match(source, /isRentalControlEligible\(property\).*=== 'OPERATING'/);
  assert.match(source, /legacy\.t_8371a53c870b/);
});

test('journey navigation splits later work into tenant lease billing and maintenance steps', () => {
  assert.match(source, /key:\s*'tenantSetup'/);
  assert.match(source, /key:\s*'leaseSetup'/);
  assert.match(source, /key:\s*'billingOperations'/);
  assert.match(source, /key:\s*'maintenanceOperations'/);
  assert.match(source, /openOperationsCenter\('billing'\)/);
  assert.match(source, /openOperationsCenter\('maintenance'\)/);
  assert.match(source, /grid-template-columns:repeat\(7,minmax\(112px,1fr\)\)/);
});

test('scopes a shared rental workflow to one room while allowing multiple room leases', () => {
  assert.match(source, /fetchAdminRentalSpaces/);
  assert.match(source, /class="process-rental-space-switcher"/);
  assert.match(source, /v-for="space in selectableRentalSpaces"/);
  assert.match(source, /selectRentalSpace\(space\)/);
  assert.match(source, /leases\.filter\(lease => String\(lease\.rentalSpaceId\) === String\(this\.selectedRentalSpaceId\)\)/);
  assert.match(source, /scopedLeases\.filter\(lease => String\(lease\.status \|\| ''\)\.toLowerCase\(\) === 'active'\)/);
  assert.match(source, /v-model\.number="actionForm\.rentalSpaceId"/);
  assert.match(source, /:disabled="Boolean\(space\.currentLeaseId\)"/);
  assert.match(source, /rentalSpaceId: Number\(this\.actionForm\.rentalSpaceId\)/);
  assert.match(source, /Promise\.all\(leaseIds\.map\(leaseId =>\s+safe\(fetchAdminLeaseRentInvoices/);
  assert.match(source, /operationsSelectedLeaseId = this\.rentalWorkbench\.currentLease/);
});

test('keeps deposit management outside the rental journey', () => {
  assert.doesNotMatch(source, /key:\s*'depositManagement'/);
  assert.doesNotMatch(source, /openOperationsCenter\('deposit'\)/);
  assert.doesNotMatch(source, /operationsTab === 'deposit'/);
  assert.doesNotMatch(source, /createAdminTenantDepositTransaction/);
  assert.doesNotMatch(source, /operationsDepositTransactions/);
});

test('renders the journey overview as compact status tiles with one current-stage summary', () => {
  assert.match(source, /journeyCompletedCount/);
  assert.match(source, /journeyCurrentStep/);
  assert.match(source, /\$lt\(journeyCurrentStep\.title\)/);
  assert.match(source, /step\.status === 'completed' \? '✓' : index \+ 1/);
  assert.match(source, /\.process-journey-track button\{[^}]*border:1px solid/);
  assert.match(source, /\.process-journey-track button\.active\{[^}]*border-color:#0b8f96/);
});

test('exposes system stages through rental operations and lease closure', () => {
  assert.match(source, /open_operations_center/);
  assert.match(source, /close_lease/);
  assert.match(source, /openOperationsCenter/);
  assert.match(source, /submitCloseLease/);
});

test('saves newly selected photos back to property photos even before a lease exists', () => {
  assert.match(source, /const photoItems = this\.actionForm\.handoverPhotoItems/);
  assert.match(source, /createAdminPropertyPhoto\(ownerId, ownerUnitId/);
  assert.match(source, /leaseId \? 'after' : null/);
  assert.doesNotMatch(source, /if \(leaseId\) await Promise\.all\(photos\.map/);
});

test('keeps the system process independent from all signing documents', () => {
  assert.doesNotMatch(source, /fetchAdminRentalMandateDocuments/);
  assert.doesNotMatch(source, /mandateDocuments/);
  assert.doesNotMatch(source, /RENTAL_SIGNING_ACTIONS/);
  assert.doesNotMatch(source, /\/admin\/rental-signing/);
  assert.doesNotMatch(source, /generateAdminContractTemplate/);
  assert.doesNotMatch(source, /startAdminMandateDocumentSignature/);
  assert.doesNotMatch(source, /createAdminPropertyContractRecord/);
  assert.doesNotMatch(source, /startAdminLeaseSignature/);
});

test('keeps lease dates inside the current rental mandate and explains the limit', () => {
  assert.match(source, /rentalLeaseTermError/);
  assert.match(source, /leaseTermErrors/);
  assert.match(source, /:max="rentalWorkbench\.currentMandate\?\.endDate/);
  assert.match(source, /leaseTermHint/);
});

test('opens the daily operations workbench with a lease-scoped monthly snapshot', () => {
  assert.match(source, /rental-operations-open-button/);
  assert.match(source, /fetchAdminPropertyOperations/);
  assert.match(source, /operationsMonth\}-01/);
  assert.match(source, /operationsWorkspace\?\.lease/);
  assert.doesNotMatch(source, /operationsWorkspace\?\.billing \|\| this\.rentalWorkbench\.dailyOperations\.currentInvoice/);
});

test('creates invoices, confirms an unpaid invoice row, and manages charges and work orders inside operations center', () => {
  assert.match(source, /v-if="!operationsInvoice"[^>]*@click="openOperationsForm\('invoice'\)"/);
  assert.match(source, /createAdminLeaseFirstInvoice/);
  assert.match(source, /confirmAdminRentCollection/);
  assert.match(source, /createAdminPropertyOperationsCharge/);
  assert.match(source, /createAdminPropertyOperationsWorkOrder/);
  assert.match(source, /leaseId, requestedAt/);
});

test('daily operations center covers rent, expenses, expense review, maintenance, and lease work', () => {
  assert.match(source, /operationsTab === 'billing'/);
  assert.match(source, /operationsTab === 'expenses'/);
  assert.match(source, /operationsTab === 'expenseReview'/);
  assert.match(source, /operationsTab === 'maintenance'/);
  assert.match(source, /operationsTab === 'lease'/);
  assert.match(source, /createAdminExpense/);
  assert.match(source, /updateAdminExpense/);
  assert.match(source, /deleteAdminExpense/);
  assert.match(source, /confirmAdminFinanceReview/);
  assert.match(source, /rejectAdminFinanceReview/);
  assert.match(source, /sendAdminRentReminder/);
  assert.match(source, /completeAdminMaintenance/);
});

test('daily operations scopes expenses by the physical unit id instead of the owner-unit id', () => {
  assert.match(source, /operationsPhysicalUnitId\(\) \{ return Number\(this\.selectedProperty\?\.unitId/);
  assert.match(source, /filter\(item => Number\(item\.unitId\) === unitId\)/);
  assert.match(source, /const payload = \{ unitId, \.\.\.this\.operationsForm/);
  assert.doesNotMatch(source, /const ownerUnitId = Number\(this\.selectedProperty\?\.ownerUnitId\); this\.operationsExpenses/);
});
