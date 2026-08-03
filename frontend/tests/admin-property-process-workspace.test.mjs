import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync(new URL('../src/components/AdminPropertyProcessWorkspace.vue', import.meta.url), 'utf8');

test('process center uses the current rental workbench model', () => {
  assert.match(source, /buildRentalWorkbench/);
  assert.match(source, /fetchAdminPropertyPhotos/);
  assert.match(source, /fetchAdminRentalMandateDocuments/);
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
  assert.match(source, /generateAdminContractTemplate/);
  assert.match(source, /startAdminMandateDocumentSignature/);
  assert.match(source, /reviewAdminRentalMandate/);
  assert.match(source, /createAdminTenant/);
  assert.match(source, /createAdminLease/);
  assert.match(source, /closeAdminLease/);
  assert.match(source, /createAdminLeaseFirstInvoice/);
  assert.match(source, /create_first_invoice/);
  assert.match(source, /'create_first_invoice', 'confirm_first_receipt'/);
  assert.match(source, /createAdminPropertyContractRecord/);
  assert.match(source, /confirmAdminRentCollection/);
  assert.match(source, /saveAdminPropertyWorkspaceBasic/);
  assert.match(source, /saveAdminPropertyHandover/);
  assert.match(source, /complete_handover/);
  assert.match(source, /complete_property_data/);
  assert.match(source, /conditionSummary/);
  assert.match(source, /keyCount/);
  assert.match(source, /completed: true/);
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

test('generated files are scoped to the selected stage and support current rental downloads', () => {
  assert.match(source, /filterGeneratedFilesByStage/);
  assert.match(source, /generatedFilesStage\?\.key/);
  assert.match(source, /downloadAdminPropertyContractRecord/);
  assert.match(source, /fetchAdminLeaseContract/);
  assert.match(source, /fetchAdminRentReceipt/);
  assert.match(source, /fetchAdminRentProof/);
});

test('normalizes lease-scoped invoice IDs before evaluating the current rental', () => {
  assert.match(source, /invoiceId:\s*item\.invoiceId/);
  assert.match(source, /leaseId:\s*leaseId/);
});

test('confirms the current invoice using the API invoiceId', () => {
  assert.match(source, /const invoiceId = invoice\?\.id \|\| invoice\?\.invoiceId/);
  assert.match(source, /confirmAdminRentCollection\(invoiceId/);
});

test('keeps rent collection outside the four-stage rental workflow', () => {
  assert.match(source, /rental-operations-panel/);
  assert.match(source, /rentalWorkbench\.dailyOperations/);
  assert.match(source, /openOperationsAction/);
  assert.match(source, /create_first_invoice/);
  assert.match(source, /confirm_first_receipt/);
});

test('provides a lease closure action followed by a move-out handover report', () => {
  assert.match(source, /leaseClosure/);
  assert.match(source, /close_lease/);
  assert.match(source, /complete_move_out_handover/);
  assert.match(source, /handoverType[,}]/);
  assert.match(source, /generateHandoverReport\('move_out'\)/);
  assert.match(source, /leaseClosure\?\.leaseId/);
});

test('loads the current mandate handover completion record into the rental workbench', () => {
  assert.match(source, /fetchAdminPropertyHandover/);
  assert.match(source, /handover:\s*this\.workspaceRelated\.handover/);
  assert.match(source, /const handover = currentMandate/);
});

test('allows selecting an existing tenant before creating a new tenant', () => {
  assert.match(source, /fetchAdminTenancyOptions/);
  assert.match(source, /existingTenantId/);
  assert.match(source, /workspaceRelated\.tenants/);
  assert.match(source, /this\.actionForm\.existingTenantId/);
  assert.match(source, /use_existing_tenant/);
});

test('process center shows four current rental stages without pagination', () => {
  assert.match(source, /v-for="stage in rentalWorkbench\.stages"/);
  assert.match(source, /rentalWorkbench\.stages/);
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
  assert.match(source, /handoverCategoryOptions: HANDOVER_CATEGORIES/);
  assert.match(source, /chooseHandoverChecklistCategory\('鑰匙 Keys'\)/);
  assert.match(source, /chooseHandoverChecklistCategory\('主浴室 Master Bathroom'\)/);
  assert.match(source, /\.rental-checklist-category-menu\{[^}]*min-height:160px/);
});

test('process center keeps the workflow list inside the panel padding', () => {
  assert.match(source, /\.process-center-main\{[^}]*padding:20px/);
});

test('property picker list expands through the picker horizontal padding', () => {
    assert.match(source, /\.process-property-picker\{[^}]*grid-template-rows:auto auto auto/);
  assert.match(source, /\.process-property-picker\{[^}]*align-self:start/);
  assert.match(source, /\.process-property-picker\{[^}]*height:auto/);
  assert.match(source, /\.process-picker-head\{[^}]*margin:0 16px/);
  assert.match(source, /\.process-property-picker>input\{[^}]*margin:0 16px/);
  assert.match(source, /\.process-property-list\{[^}]*width:100%/);
  assert.match(source, /\.process-property-list\{[^}]*height:auto/);
  assert.match(source, /\.process-property-list\{[^}]*max-height:calc\(100vh - 290px\)/);
});

test('keeps the four-stage workbench visible inside the viewport', () => {
  assert.match(source, /\.process-center-layout\{[^}]*height:calc\(100vh - 190px\)/);
  assert.match(source, /\.process-center-layout\{[^}]*min-height:0/);
  assert.match(source, /\.process-center-main\{[^}]*height:100%/);
  assert.match(source, /\.process-center-main\{[^}]*overflow-y:auto/);
  assert.doesNotMatch(source, /process-pagination/);
});

test('keeps process content away from the page and panel edges', () => {
  assert.match(source, /\.property-process-center\{[^}]*padding:16px 20px 28px/);
  assert.match(source, /\.process-center-main\{[^}]*padding:20px/);
  assert.match(source, /\.process-property-list\{[^}]*padding:0 16px/);
});

test('gives workflow cards comfortable outer and inner spacing', () => {
  assert.match(source, /\.process-center-main\{[^}]*padding:20px(?! 0)/);
  assert.match(source, /\.rental-current-task\{[^}]*margin:18px 0/);
  assert.match(source, /\.rental-stage-list\{[^}]*gap:12px/);
  assert.match(source, /\.rental-stage-card\{[^}]*padding:16px 18px/);
});

test('uses unit id for the selected property workspace and detail route', () => {
  assert.match(source, /fetchAdminPropertyWorkspaceById\(property\.unitId \|\| property\.id \|\| property\.ownerUnitId\)/);
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

test('renames the workspace and replaces history navigation with inline generated files', () => {
  assert.match(source, /viewGeneratedFiles/);
  assert.match(source, /generatedFilesOpen/);
  assert.match(source, /downloadGeneratedFile/);
  assert.doesNotMatch(source, /class="history-link"/);
  assert.doesNotMatch(source, /openHistory\(stage\)/);
});

test('de-emphasizes generated files as a secondary stage action', () => {
  assert.match(source, /\.rental-stage-actions \.generated-files-link\{[^}]*background:#fff/);
  assert.match(source, /\.rental-stage-actions \.generated-files-link\{[^}]*border:1px solid/);
  assert.match(source, /\.rental-stage-actions \.generated-files-link\{[^}]*color:#547083/);
});

test('saves newly selected photos back to property photos even before a lease exists', () => {
  assert.match(source, /const photoItems = this\.actionForm\.handoverPhotoItems/);
  assert.match(source, /createAdminPropertyPhoto\(ownerId, ownerUnitId/);
  assert.match(source, /leaseId \? 'after' : null/);
  assert.doesNotMatch(source, /if \(leaseId\) await Promise\.all\(photos\.map/);
});

test('refreshes the current mandate documents before resending authorization signing', () => {
  assert.match(source, /refreshAuthorizationState/);
  assert.match(source, /await this\.refreshAuthorizationState\(\)/);
  assert.match(source, /isAuthorizationSignedDocument\(item\)/);
  assert.doesNotMatch(source, /currentTask\?\.key !== 'mandateAuthorization'/);
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

test('completes monthly invoice, payment, charge, and work-order actions inside operations center', () => {
  assert.match(source, /openOperationsForm\(operationsInvoice \? 'receipt' : 'invoice'\)/);
  assert.match(source, /createAdminLeaseFirstInvoice/);
  assert.match(source, /confirmAdminRentCollection/);
  assert.match(source, /createAdminPropertyOperationsCharge/);
  assert.match(source, /createAdminPropertyOperationsWorkOrder/);
  assert.match(source, /leaseId, requestedAt/);
});
