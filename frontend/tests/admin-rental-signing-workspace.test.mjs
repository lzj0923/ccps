import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync(new URL('../src/components/AdminRentalSigningWorkspace.vue', import.meta.url), 'utf8');
const router = readFileSync(new URL('../src/router.js', import.meta.url), 'utf8');
const navigation = readFileSync(new URL('../src/composables/dashboardViewModel.js', import.meta.url), 'utf8');
const adminPage = readFileSync(new URL('../src/pages/AdminPage.vue', import.meta.url), 'utf8');
const nginx = readFileSync(new URL('../../docker/frontend/nginx.conf', import.meta.url), 'utf8');

test('rental signing is exposed as a separate admin function', () => {
  assert.match(router, /path: '\/admin\/rental-signing'/);
  assert.match(router, /moduleId: 'adminRentalSigning'/);
  assert.match(navigation, /'adminProcess', 'adminRentalSigning'/);
  assert.match(adminPage, /AdminRentalSigningWorkspace/);
});

test('property picker can filter by project from a dropdown', () => {
  assert.match(source, /rental-signing-project-filter/);
  assert.match(source, /v-model="selectedProjectName"/);
  assert.match(source, /v-for="project in projectOptions"/);
  assert.match(source, /selectedProjectName/);
  assert.match(source, /property\.projectName/);
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
  assert.match(source, /fetchAdminMandateSignatureParticipants/);
  assert.match(source, /startAdminMandateSignaturePackage/);
  assert.match(source, /signerRole: 'company'/);
  assert.match(source, /signerRole: 'customer_service'/);
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

test('uses the six fields required by the latest management authorization letter', () => {
  for (const field of ['projectName', 'unitNo', 'landlordName', 'landlordIdentity', 'ownerEmail', 'agreementDate']) {
    assert.match(source, new RegExp(`authorizationForm\\.${field}`));
  }
  assert.match(source, /const AUTHORIZATION_REQUIRED_FIELDS = \{ projectName: '建案名称', unitNo: '单位号码'/);
  assert.doesNotMatch(source, /authorizationForm\.managementOffice/);
  assert.doesNotMatch(source, /authorizationForm\.unitNoOrAddress/);
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

test('validates and completes rental appointment details before every generation', () => {
  assert.match(source, /rentalAppointmentFormOpen/);
  assert.match(source, /openRentalAppointmentForm\(\)/);
  assert.match(source, /@submit\.prevent="generateRentalAppointment"/);
  assert.match(source, /fetchAdminRentalAppointmentDetails/);
  assert.match(source, /saveAdminRentalAppointmentDetails/);
  for (const field of ['caseNo', 'propertyAddress', 'earnestDeposit', 'earnestDepositWords',
    'commissionPercent', 'commissionMonths', 'sstPercent', 'commissionAmount', 'agencyFeeTotal',
    'startDate', 'endDate', 'landlordName', 'landlordIdentity', 'landlordAddress', 'landlordDate',
    'witnessName', 'witnessIdentity', 'witnessAddress', 'witnessDate']) {
    assert.match(source, new RegExp(`rentalAppointmentForm\\.${field}`));
  }
  assert.match(source, /rentalAppointmentMissingFields/);
  assert.match(source, /:disabled="actionBusy \|\| rentalAppointmentMissingFields\.length > 0"/);
});

test('prefills and automatically writes PMA owner and property bank details back to master records', () => {
  assert.match(source, /fetchAdminOwner/);
  assert.match(source, /updateAdminOwner/);
  assert.match(source, /fetchAdminPropertyBankAccounts/);
  assert.match(source, /createAdminPropertyBankAccount/);
  assert.match(source, /updateAdminPropertyBankAccount/);
  assert.match(source, /async loadPmaBankAccount\(\)/);
  assert.match(source, /bankPayeeName: account\.paymentName/);
  assert.match(source, /bankName: account\.itemName/);
  assert.match(source, /bankAddress: account\.bankAddress/);
  assert.match(source, /bankBranchCode: account\.branchCode/);
  assert.match(source, /bankAccountNo: account\.accountNo/);
  assert.match(source, /bankSwiftCode: account\.swiftCode/);
  assert.match(source, /ownerAddress: owner\?\.mailingAddress/);
  assert.match(source, /await this\.syncPmaOwnerProfile\(\)/);
  assert.match(source, /await this\.syncPmaBankAccount\(\)/);
  assert.doesNotMatch(source, /pmaSyncBankAccount/);
});

test('rental signing also provides generated file downloads', () => {
  assert.match(source, /downloadAdminRentalMandateDocument/);
  assert.match(source, /downloadAdminPropertyContractRecord/);
  assert.match(source, /downloadAdminPropertyHandoverReport/);
  assert.match(source, /downloadAdminPropertyAttachment/);
});

test('downloads linked lease contracts through the tenancy contract endpoint', () => {
  assert.match(source, /fetchAdminLeaseContract/);
  assert.match(source, /contractType: contract\.contractType/);
  assert.match(source, /leaseId: contract\.leaseId/);
  assert.match(source, /file\.contractType === 'L_LEASE'.*fetchAdminLeaseContract\(file\.leaseId \|\| file\.id, true\)/s);
});

test('serves pdf worker mjs files with a JavaScript MIME type', () => {
  assert.match(nginx, /location ~\* \\.mjs\$/);
  assert.match(nginx, /default_type application\/javascript/);
});

test('rental signing keeps picker and filters inside their columns', () => {
  assert.match(source, /\.rental-signing-property-picker>input\{box-sizing:border-box;width:calc\(100% - 32px\)/);
  assert.match(source, /\.rental-files-toolbar input,\.rental-files-toolbar select\{box-sizing:border-box;width:100%;min-width:0/);
  assert.match(source, /\.rental-signing-main\{[^}]*overflow-x:hidden/);
});

test('treats signing files as independent actions', () => {
  assert.match(source, /key: 'otr'.*status: this\.otrSigned \? 'complete' : otrPending \? 'pending'/);
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

test('allows mandate attachments to be regenerated at any signing stage', () => {
  assert.match(source, /v-if="task\.canRegenerate"/);
  assert.match(source, /@click="regenerateTask\(task\)"/);
  assert.match(source, /canRegenerate: !readOnly && Boolean\(this\.pmaDocument\)/);
  assert.match(source, /canRegenerate: !readOnly && Boolean\(this\.rentalAppointmentDocument\)/);
  assert.match(source, /canRegenerate: !readOnly && Boolean\(this\.authorizationDocument\)/);
  assert.match(source, /pmaSigned\(\) \{ return String\(this\.pmaDocument\?\.signatureStatus \|\| ''\)\.toLowerCase\(\) === 'signed'; \}/);
  assert.match(source, /canRegenerate: !readOnly && Boolean\(this\.otrContract\)/);
  assert.match(source, /canRegenerate: !readOnly && Boolean\(this\.leaseContract\)/);
  assert.doesNotMatch(source, /leaseSigningStarted/);
  assert.doesNotMatch(source, /\['pending', 'sent', 'viewed', 'signed'\]\.includes\(this\.leaseSignatureStatus\)/);
});

test('区分重新生成与修改资料，重新生成清空资料而修改保留原值', () => {
  assert.match(source, /@click="editTask\(task\)"/);
  assert.match(source, /@click="regenerateTask\(task\)"/);
  assert.match(source, /editTask\(task\)/);
  assert.match(source, /regenerateTask\(task\)[\s\S]*openPmaForm\(true\)/);
  assert.match(source, /openRentalAppointmentForm\(true\)/);
  assert.match(source, /openAuthorizationForm\(true\)/);
  assert.match(source, /openOtrForm\(true\)/);
  assert.match(source, /openLeaseForm\(true\)/);
  assert.match(source, /reset = false/);
});

test('支持只替换条款并用现有资料生成新版合约', () => {
  assert.match(source, /仅更新条款/);
  assert.match(source, /updateClausesTask\(task\)/);
  assert.match(source, /generatePma\(\{ preserveMasterData: true \}\)/);
  assert.match(source, /generateAuthorization\(\{ preserveMasterData: true \}\)/);
  assert.match(source, /if \(!options\.preserveMasterData\)/);
  assert.match(source, /旧版合约已保留/);
});

test('captures every multi-party signer once and allows editable restarts at every stage', () => {
  assert.match(source, /v-for="\(signer, index\) in signerPackageForm"/);
  assert.match(source, /async openSignerPackage\(type\)/);
  assert.match(source, /fetchAdminMandateSignatureParticipants\(this\.currentMandate\.id, document\.id\)/);
  assert.match(source, /startAdminMandateSignaturePackage\(this\.currentMandate\.id, document\.id/);
  assert.match(source, /fetchAdminLeaseSignatureParticipants\(leaseId\)/);
  assert.match(source, /startAdminLeaseSignaturePackage\(leaseId, payload\)/);
  assert.match(source, /\['pma', 'otr', 'lease'\]\.includes\(this\.signingPanel\)/);
  assert.match(source, /signerRole: 'owner'.*signerRole: 'tenant'/s);
  assert.match(source, /signers: this\.signerPackageForm\.map/);
  assert.match(source, /action: !readOnly && !noMandate, actionLabel: this\.pmaDocument/);
  assert.match(source, /action: !readOnly && !noMandate, actionLabel: this\.otrMandateDocument/);
  assert.match(source, /action: !readOnly && !noLease/);
  assert.match(source, /restartSigning/);
});

test('fills the tenancy agreement with the available owner and tenant details', () => {
  assert.match(source, /tenancyAgreementFields\(lease/);
  assert.match(source, /agreementDate: lease\.startDate/);
  assert.match(source, /landlordAddress: base\.ownerAddress/);
  assert.match(source, /tenantIdentity: lease\.tenantIdentity/);
  assert.match(source, /tenantPhone: lease\.tenantPhone/);
  assert.match(source, /tenantEmail: lease\.tenantEmail/);
  assert.match(source, /bankAccount: base\.bankAccountNo/);
});

test('opens a lease details popup before generation and exposes the blank schedule and meter fields', () => {
  assert.match(source, /leaseFormOpen/);
  assert.match(source, /@submit\.prevent="generateLeaseDraft"/);
  assert.match(source, /leaseMissingFields/);
  for (const field of ['paymentMode', 'renewalOption', 'specialConditions', 'electricityMeter', 'waterMeter', 'gasMeter']) {
    assert.match(source, new RegExp(`leaseForm\\.${field}`));
  }
  assert.match(source, /openLeaseForm\(\)/);
  assert.match(source, /Check Out.*签约时保持空白/s);
});

test('supports owner-only online signing for a termination letter with complete source fields', () => {
  assert.match(source, /key: 'terminationLetter'/);
  assert.match(source, /generateAdminContractTemplate\('termination-letter'/);
  assert.match(source, /'termination_letter_draft'/);
  assert.match(source, /terminationLetterFormOpen/);
  for (const field of ['agreementDate', 'landlordName', 'projectName', 'unitNo', 'authorizedAgentName',
    'authorizedAgentIdentity', 'authorizedAgentPhone', 'authorizedAgentEmail', 'bankName',
    'bankPayeeName', 'bankAccountNo', 'bankSwiftCode', 'bankAddress']) {
    assert.match(source, new RegExp(`terminationLetterForm\\.${field}`));
  }
  assert.match(source, /startAdminMandateDocumentSignature\(this\.currentMandate\.id, this\.terminationLetterDocument\.id/);
  assert.match(source, /signerRole: 'owner'/);
  assert.match(source, /terminationLetterSigning/);
});

test('supports owner-only rental remittance authorization with complete bank fields', () => {
  assert.match(source, /key: 'rentalRemittance'/);
  assert.match(source, /generateAdminContractTemplate\('rental-remittance'/);
  assert.match(source, /'rental_remittance_draft'/);
  assert.match(source, /rentalRemittanceFormOpen/);
  assert.match(source, /v-model="rentalRemittanceForm\.bankAccountId"/);
  assert.match(source, /v-for="account in pmaBankAccounts"/);
  assert.match(source, /applyRentalRemittanceBankAccount/);
  for (const field of ['agreementDate', 'landlordName', 'landlordIdentity', 'ownerEmail', 'unitNo',
    'bankPayeeName', 'bankName', 'bankAccountNo', 'bankSwiftCode', 'bankBranchCode', 'bankAddress']) {
    assert.match(source, new RegExp(`rentalRemittanceForm\\.${field}`));
  }
  assert.match(source, /startAdminMandateDocumentSignature\(this\.currentMandate\.id, this\.rentalRemittanceDocument\.id/);
  assert.match(source, /signerRole: 'owner'/);
  assert.match(source, /rentalRemittanceSigning/);
});

test('template designer presents signature fields as movable signature boxes', () => {
  assert.match(source, /templateSignatureField/);
  assert.match(source, /startsWith\('signature\.'\)/);
  assert.match(source, /templateSignatureWidth/);
  assert.match(source, /templateSignatureHeight/);
});

test('keeps rental remittance data when regenerating and labels its category in Chinese', () => {
  assert.match(source, /if \(task\.key === 'rentalRemittance'\) \{ await this\.openRentalRemittanceForm\(false\); return; \}/);
  assert.match(source, /const previous = this\.rentalRemittanceForm \|\| emptyRentalRemittanceForm\(\)/);
  assert.match(source, /agreementDate: previous\.agreementDate \|\| fields\.agreementDate/);
  assert.match(source, /bankAccountId: previous\.bankAccountId \|\|/);
  const i18n = readFileSync(new URL('../src/i18n/index.js', import.meta.url), 'utf8');
  assert.match(i18n, /remittance: \['租金汇款授权书', '租金匯款授權書', 'Rental Remittance Authorization'\]/);
});

test('termination letter form reads existing owner and bank data even when regenerating', () => {
  assert.match(source, /async openTerminationLetterForm\(reset = false\)[\s\S]*fetchAdminOwner\(this\.selectedProperty\.ownerId\)/);
  assert.match(source, /async openTerminationLetterForm\(reset = false\)[\s\S]*loadPmaBankAccount\(\)/);
  assert.doesNotMatch(source, /async openTerminationLetterForm\(reset = false\)[\s\S]*this\.terminationLetterForm = reset \? emptyTerminationLetterForm\(\)/);
  assert.match(source, /terminationLetterForm = \{[\s\S]*owner\?\.fullName[\s\S]*owner\?\.identityNo[\s\S]*owner\?\.email/);
});
