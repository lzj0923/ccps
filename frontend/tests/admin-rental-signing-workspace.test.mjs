import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync(new URL('../src/components/AdminRentalSigningWorkspace.vue', import.meta.url), 'utf8');
const router = readFileSync(new URL('../src/router.js', import.meta.url), 'utf8');
const navigation = readFileSync(new URL('../src/composables/dashboardViewModel.js', import.meta.url), 'utf8');
const adminPage = readFileSync(new URL('../src/pages/AdminPage.vue', import.meta.url), 'utf8');

test('rental signing is exposed as a separate admin function', () => {
  assert.match(router, /path: '\/admin\/rental-signing'/);
  assert.match(router, /moduleId: 'adminRentalSigning'/);
  assert.match(navigation, /'adminProcess', 'adminRentalSigning'/);
  assert.match(adminPage, /AdminRentalSigningWorkspace/);
});

test('rental signing owns document generation and signature actions', () => {
  assert.match(source, /fetchAdminRentalMandateDocuments/);
  assert.match(source, /fetchAdminPropertyAttachments/);
  assert.match(source, /fetchAdminPropertyHandoverReports/);
  assert.match(source, /workspace\?\.contracts/);
  assert.match(source, /generateAdminContractTemplate/);
  assert.match(source, /uploadAdminRentalMandateDocument/);
  assert.match(source, /startAdminMandateDocumentSignature/);
  assert.match(source, /uploadAdminLeaseContract/);
  assert.match(source, /startAdminLeaseSignature/);
  assert.match(source, /key: 'pma'/);
  assert.match(source, /property-management-agreement/);
  assert.match(source, /management-authorization/);
  assert.match(source, /signerRole: this\.nextPmaSignerRole/);
  assert.match(source, /replaceAdminContractTemplate/);
  assert.match(source, /key: 'authorization'/);
  assert.match(source, /key: 'otr'/);
  assert.match(source, /key: 'lease'/);
});

test('keeps rental appointment, management authorization and PMA as three different files', () => {
  assert.match(source, /key: 'rentalAppointment'/);
  assert.match(source, /key: 'authorization'/);
  assert.match(source, /key: 'pma'/);
  assert.match(source, /generateAdminContractTemplate\('authorization'/);
  assert.match(source, /generateAdminContractTemplate\('management-authorization'/);
  assert.match(source, /generateAdminContractTemplate\('property-management-agreement'/);
  assert.match(source, /'rental_appointment_draft'/);
  assert.match(source, /'management_authorization_draft'/);
  assert.match(source, /'property_management_agreement_draft'/);
});

test('requires complete PMA owner, agreement and bank details before generation', () => {
  assert.match(source, /pmaFormOpen/);
  for (const field of ['landlordName', 'landlordIdentity', 'propertyAddress', 'startDate', 'endDate',
    'bankPayeeName', 'bankName', 'bankAddress', 'bankBranchCode', 'bankAccountNo', 'bankSwiftCode',
    'ownerAddress', 'ownerEmail', 'ownerPhone']) {
    assert.match(source, new RegExp(`pmaForm\\.${field}`));
  }
  assert.match(source, /@submit\.prevent="generatePma"/);
});

test('rental signing also provides generated file downloads', () => {
  assert.match(source, /downloadAdminRentalMandateDocument/);
  assert.match(source, /downloadAdminPropertyContractRecord/);
  assert.match(source, /downloadAdminPropertyHandoverReport/);
  assert.match(source, /downloadAdminPropertyAttachment/);
});

test('rental signing keeps picker and filters inside their columns', () => {
  assert.match(source, /\.rental-signing-property-picker>input\{box-sizing:border-box;width:calc\(100% - 32px\)/);
  assert.match(source, /\.rental-files-toolbar input,\.rental-files-toolbar select\{box-sizing:border-box;width:100%;min-width:0/);
  assert.match(source, /\.rental-signing-main\{[^}]*overflow-x:hidden/);
});

test('treats signing files as independent actions', () => {
  assert.match(source, /key: 'otr'.*status: this\.otrComplete \? 'complete' : noMandate \? 'unavailable' : 'todo'/);
  assert.match(source, /uploadAdminRentalMandateDocument\(mandate\.id, 'otr', file\)/);
  assert.doesNotMatch(source, /key: 'otr'.*disabled: noLease/);
  assert.match(source, /authorization_draft', 'draft', 'ready/);
});

test('scopes files by rental cycle and keeps historical cycles read only', () => {
  assert.match(source, /selectedCycleId/);
  assert.match(source, /selectCurrentRentalMandate/);
  assert.match(source, /reportBelongsToMandate/);
  assert.match(source, /cycleLeaseIds\.has\(String\(item\.leaseId\)\)/);
  assert.match(source, /selectedCycleIsHistorical/);
  assert.match(source, /action: !readOnly/);
  assert.match(source, /historyReadOnly/);
  assert.match(source, /propertyFiles/);
  assert.doesNotMatch(source, /for \(const attachment of this\.propertyAttachments\) files\.push/);
});

test('allows every generated attachment to be regenerated only before signing starts', () => {
  assert.match(source, /v-if="task\.canRegenerate"/);
  assert.match(source, /@click="regenerateTask\(task\)"/);
  assert.match(source, /canRegenerate: !readOnly && Boolean\(this\.pmaDocument\) && !this\.pmaSigned && !pmaPending/);
  assert.match(source, /canRegenerate: !readOnly && Boolean\(this\.rentalAppointmentDocument\) && !this\.rentalAppointmentSigned && !rentalAppointmentPending/);
  assert.match(source, /canRegenerate: !readOnly && Boolean\(this\.authorizationDocument\) && !this\.authorizationSigned && !authorizationPending/);
  assert.match(source, /canRegenerate: !readOnly && Boolean\(this\.otrContract\)/);
  assert.match(source, /canRegenerate: !readOnly && Boolean\(this\.leaseContract\) && !leaseSigningStarted/);
  assert.match(source, /\['pending', 'sent', 'viewed', 'signed'\]\.includes\(this\.leaseSignatureStatus\)/);
});
