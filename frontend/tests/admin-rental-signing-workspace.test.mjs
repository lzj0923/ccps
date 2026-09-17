import { matchLocalizedSource } from './helpers/localizedSource.mjs';
import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync(new URL('../src/components/AdminRentalSigningWorkspace.vue', import.meta.url), 'utf8');
const router = readFileSync(new URL('../src/router.js', import.meta.url), 'utf8');
const navigation = readFileSync(new URL('../src/composables/dashboardViewModel.js', import.meta.url), 'utf8');
const adminPage = readFileSync(new URL('../src/pages/AdminPage.vue', import.meta.url), 'utf8');
const nginx = readFileSync(new URL('../../docker/frontend/nginx.conf', import.meta.url), 'utf8');
const i18nSource = readFileSync(new URL('../src/i18n/index.js', import.meta.url), 'utf8');

test('rental signing is exposed as a separate admin function', () => {
  matchLocalizedSource(router, /path: '\/admin\/rental-signing'/);
  matchLocalizedSource(router, /moduleId: 'adminRentalSigning'/);
  matchLocalizedSource(navigation, /'adminProcess', 'adminRentalSigning'/);
  matchLocalizedSource(adminPage, /AdminRentalSigningWorkspace/);
});

test('property picker can filter by project from a dropdown', () => {
  matchLocalizedSource(source, /rental-signing-project-filter/);
  matchLocalizedSource(source, /v-model="selectedProjectName"/);
  matchLocalizedSource(source, /v-for="project in projectOptions"/);
  matchLocalizedSource(source, /selectedProjectName/);
  matchLocalizedSource(source, /property\.projectName/);
});

test('rental signing owns document generation and signature actions', () => {
  matchLocalizedSource(source, /fetchAdminRentalMandateDocuments/);
  matchLocalizedSource(source, /fetchAdminPropertyAttachments/);
  matchLocalizedSource(source, /fetchAdminPropertyHandoverReports/);
  matchLocalizedSource(source, /workspace\?\.contracts/);
  matchLocalizedSource(source, /generateAdminContractTemplate/);
  matchLocalizedSource(source, /uploadAdminRentalMandateDocument/);
  matchLocalizedSource(source, /startAdminMandateDocumentSignature/);
  matchLocalizedSource(source, /uploadAdminGeneratedLeaseContract/);
  matchLocalizedSource(source, /startAdminLeaseSignature/);
  matchLocalizedSource(source, /key: 'pma'/);
  matchLocalizedSource(source, /property-management-agreement/);
  matchLocalizedSource(source, /management-authorization/);
  matchLocalizedSource(source, /fetchAdminMandateSignatureParticipants/);
  matchLocalizedSource(source, /startAdminMandateSignaturePackage/);
  matchLocalizedSource(source, /signerRole: 'company'/);
  matchLocalizedSource(source, /signerRole: 'customer_service'/);
  matchLocalizedSource(source, /replaceAdminContractTemplate/);
  matchLocalizedSource(source, /key: 'authorization'/);
  matchLocalizedSource(source, /key: 'otr'/);
  matchLocalizedSource(source, /key: 'lease'/);
});

test('keeps one combined rental appointment task alongside management authorization and PMA', () => {
  const tasks = source.match(/signingTasks\(\) \{[\s\S]*?\n\s*activeSignerRole\(\)/)?.[0] || '';
  assert.doesNotMatch(tasks, /key: 'rentalAppointment'/);
  matchLocalizedSource(tasks, /key: 'otr'.*rentalFiles\.rentalAppointmentTitle/s);
  matchLocalizedSource(source, /key: 'authorization'/);
  matchLocalizedSource(source, /key: 'pma'/);
  matchLocalizedSource(source, /generateAdminContractTemplate\('otr'/);
  matchLocalizedSource(source, /generateAdminContractTemplate\('management-authorization'/);
  matchLocalizedSource(source, /generateAdminContractTemplate\('property-management-agreement'/);
  matchLocalizedSource(source, /uploadAdminRentalMandateDocument\(mandate\.id, 'otr', file\)/);
  matchLocalizedSource(source, /'management_authorization_draft'/);
  matchLocalizedSource(source, /'property_management_agreement_draft'/);
});

test('uses the document fields and signer required by the latest management authorization letter', () => {
  for (const field of ['ownerSignerName', 'projectName', 'unitNo', 'landlordName', 'landlordIdentity', 'ownerEmail', 'agreementDate']) {
    matchLocalizedSource(source, new RegExp(`authorizationForm\\.${field}`));
  }
  matchLocalizedSource(source, /const AUTHORIZATION_REQUIRED_FIELDS = \{ ownerSignerName: 'rentalFiles\.supplement\.common\.ownerSigner', projectName: sf\('projectName'\), unitNo: sf\('unitNo'\)/);
  assert.doesNotMatch(source, /authorizationForm\.managementOffice/);
  assert.doesNotMatch(source, /authorizationForm\.unitNoOrAddress/);
});

test('requires complete PMA owner, agreement and bank details before generation', () => {
  matchLocalizedSource(source, /pmaFormOpen/);
  for (const field of ['landlordName', 'landlordIdentity', 'propertyAddress', 'startDate', 'endDate',
    'bankPayeeName', 'bankName', 'bankAddress', 'bankBranchCode', 'bankAccountNo', 'bankSwiftCode',
    'ownerAddress', 'ownerEmail', 'ownerPhone']) {
    matchLocalizedSource(source, new RegExp(`pmaForm\\.${field}`));
  }
  matchLocalizedSource(source, /@submit\.prevent="generatePma"/);
});

test('代租管合约在补充资料弹窗填写全部签署人姓名', () => {
  const pmaDialog = source.match(/<div v-if="pmaFormOpen"[\s\S]*?<div v-if="leaseFormOpen"/)?.[0] || '';
  const authorizationDialog = source.match(/<div v-if="authorizationFormOpen"[\s\S]*?<div v-if="terminationLetterFormOpen"/)?.[0] || '';
  assert.ok(pmaDialog, '未找到代租管合约补充资料弹窗');
  for (const field of ['ownerSignerName', 'companySignerName', 'customerServiceSignerName']) {
    matchLocalizedSource(pmaDialog, new RegExp(`pmaForm\\.${field}`));
    matchLocalizedSource(source, new RegExp(`${field}: 'rentalFiles\\.pma`));
  }
  assert.doesNotMatch(authorizationDialog, /pmaForm\.(ownerSignerName|companySignerName|customerServiceSignerName)/);
  matchLocalizedSource(source, /signerRole: 'owner', signerName: this\.pmaForm\.ownerSignerName/);
  matchLocalizedSource(source, /signerRole: 'company', signerName: this\.pmaForm\.companySignerName/);
  matchLocalizedSource(source, /signerRole: 'customer_service', signerName: this\.pmaForm\.customerServiceSignerName/);
  matchLocalizedSource(source, /class="rental-signing-direct-state"/);
  matchLocalizedSource(source, /if \(this\.signerNamesMissing\(type\)\) \{ await this\.openSupplementForm\(type\); return; \}/);
});

test('所有协议都在各自补充资料弹窗填写签署人，链接步骤不再重复输入', () => {
  const dialogs = {
    otr: source.match(/<div v-if="otrFormOpen"[\s\S]*?<div v-if="rentalAppointmentFormOpen"/)?.[0] || '',
    rentalAppointment: source.match(/<div v-if="rentalAppointmentFormOpen"[\s\S]*?<div v-if="authorizationFormOpen"/)?.[0] || '',
    authorization: source.match(/<div v-if="authorizationFormOpen"[\s\S]*?<div v-if="terminationLetterFormOpen"/)?.[0] || '',
    terminationLetter: source.match(/<div v-if="terminationLetterFormOpen"[\s\S]*?<div v-if="rentalRemittanceFormOpen"/)?.[0] || '',
    rentalRemittance: source.match(/<div v-if="rentalRemittanceFormOpen"[\s\S]*?<div v-if="pmaFormOpen"/)?.[0] || '',
    pma: source.match(/<div v-if="pmaFormOpen"[\s\S]*?<div v-if="leaseFormOpen"/)?.[0] || '',
    lease: source.match(/<div v-if="leaseFormOpen"[\s\S]*?<div v-if="financeDocumentOpen"/)?.[0] || '',
  };
  const expectedFields = {
    otr: ['otrForm.tenantName', 'otrForm.tenantWitnessName', 'otrForm.landlordName', 'otrForm.landlordWitnessName'],
    rentalAppointment: ['rentalAppointmentForm.ownerSignerName'],
    authorization: ['authorizationForm.ownerSignerName'],
    terminationLetter: ['terminationLetterForm.ownerSignerName', 'terminationLetterForm.companySignerName'],
    rentalRemittance: ['rentalRemittanceForm.ownerSignerName'],
    pma: ['pmaForm.ownerSignerName', 'pmaForm.companySignerName', 'pmaForm.customerServiceSignerName'],
    lease: ['leaseForm.ownerSignerName', 'leaseForm.ownerWitnessSignerName', 'leaseForm.tenantSignerName', 'leaseForm.tenantWitnessSignerName'],
  };
  for (const [type, fields] of Object.entries(expectedFields)) {
    assert.ok(dialogs[type], `未找到 ${type} 补充资料弹窗`);
    for (const field of fields) matchLocalizedSource(dialogs[type], new RegExp(field.replace('.', '\\.')));
  }

  const signingPanel = source.match(/<section v-if="signingPanel"[\s\S]*?<section class="rental-generated-files">/)?.[0] || '';
  assert.ok(signingPanel, '未找到生成签署链接区域');
  assert.doesNotMatch(signingPanel, /v-model\.trim="signer\.signerName"/);
  assert.doesNotMatch(signingPanel, /v-model\.trim="signerForm\.name"/);
});

test('补充资料生成文件后直接创建签署链接，不再显示开始签署确认步骤', () => {
  const signingPanel = source.match(/<section v-if="signingPanel"[\s\S]*?<section class="rental-generated-files">/)?.[0] || '';
  assert.doesNotMatch(signingPanel, /<form[^>]+@submit\.prevent="submitSigning"/);
  matchLocalizedSource(signingPanel, /class="rental-signing-direct-state"/);
  matchLocalizedSource(source, /async startSigningDirect\(type\)/);
  matchLocalizedSource(source, /if \(!this\.actionError\) await this\.submitSigning\(\);/);
  for (const type of ['pma', 'rentalAppointment', 'authorization', 'terminationLetter', 'rentalRemittance', 'otr', 'lease']) {
    matchLocalizedSource(source, new RegExp(`generated[^}]+startSigningDirect\\('${type}'\\)`, 's'));
  }
});

test('生成签署链接后自动定位并聚焦链接结果区域', () => {
  matchLocalizedSource(source, /ref="signingLinkResults"[^>]*tabindex="-1"/);
  matchLocalizedSource(source, /async revealGeneratedSigningLinks\(\)[\s\S]*?await this\.\$nextTick\(\)[\s\S]*?scrollIntoView\(\{ behavior: 'smooth', block: 'start' \}\)[\s\S]*?focus\(\{ preventScroll: true \}\)/);
  matchLocalizedSource(source, /this\.generatedSigningLinks = links\.map[\s\S]*?await this\.loadSigningWorkspace\(\);\s*await this\.revealGeneratedSigningLinks\(\);/);
});

test('validates and completes rental appointment details before every generation', () => {
  matchLocalizedSource(source, /rentalAppointmentFormOpen/);
  matchLocalizedSource(source, /openRentalAppointmentForm\(\)/);
  matchLocalizedSource(source, /@submit\.prevent="generateRentalAppointment"/);
  matchLocalizedSource(source, /fetchAdminRentalAppointmentDetails/);
  matchLocalizedSource(source, /saveAdminRentalAppointmentDetails/);
  for (const field of ['caseNo', 'propertyAddress', 'earnestDeposit', 'earnestDepositWords',
    'commissionWords', 'commissionMonths', 'sstPercent', 'commissionAmount', 'agencyFeeTotal',
    'startDate', 'endDate', 'landlordName', 'landlordIdentity', 'landlordAddress', 'landlordDate',
    'witnessName', 'witnessIdentity', 'witnessAddress', 'witnessDate']) {
    matchLocalizedSource(source, new RegExp(`rentalAppointmentForm\\.${field}`));
  }
  matchLocalizedSource(source, /rentalAppointmentMissingFields/);
  matchLocalizedSource(source, /validateSupplementForm\('rentalAppointment', 'rentalAppointmentFormRef'\)/);
  matchLocalizedSource(source, /ref="rentalAppointmentFormRef"[\s\S]*?:disabled="actionBusy"/);
});

test('prefills and automatically writes PMA owner and property bank details back to master records', () => {
  matchLocalizedSource(source, /fetchAdminOwner/);
  matchLocalizedSource(source, /updateAdminOwner/);
  matchLocalizedSource(source, /fetchAdminPropertyBankAccounts/);
  matchLocalizedSource(source, /createAdminPropertyBankAccount/);
  matchLocalizedSource(source, /updateAdminPropertyBankAccount/);
  matchLocalizedSource(source, /async loadPmaBankAccount\(\)/);
  matchLocalizedSource(source, /bankPayeeName: account\.paymentName/);
  matchLocalizedSource(source, /bankName: account\.itemName/);
  matchLocalizedSource(source, /bankAddress: account\.bankAddress/);
  matchLocalizedSource(source, /bankBranchCode: account\.branchCode/);
  matchLocalizedSource(source, /bankAccountNo: account\.accountNo/);
  matchLocalizedSource(source, /bankSwiftCode: account\.swiftCode/);
  matchLocalizedSource(source, /ownerAddress: owner\?\.mailingAddress/);
  matchLocalizedSource(source, /await this\.syncPmaOwnerProfile\(\)/);
  matchLocalizedSource(source, /await this\.syncPmaBankAccount\(\)/);
  assert.doesNotMatch(source, /pmaSyncBankAccount/);
});

test('rental signing also provides generated file downloads', () => {
  matchLocalizedSource(source, /downloadAdminRentalMandateDocument/);
  matchLocalizedSource(source, /downloadAdminPropertyContractRecord/);
  matchLocalizedSource(source, /downloadAdminPropertyHandoverReport/);
  matchLocalizedSource(source, /downloadAdminPropertyAttachment/);
});

test('downloads linked lease contracts through the tenancy contract endpoint', () => {
  matchLocalizedSource(source, /fetchAdminLeaseContract/);
  matchLocalizedSource(source, /contractType: contract\.contractType/);
  matchLocalizedSource(source, /leaseId: contract\.leaseId/);
  matchLocalizedSource(source, /file\.contractType === 'L_LEASE'.*fetchAdminLeaseContract\(file\.leaseId \|\| file\.id, true\)/s);
});

test('serves pdf worker mjs files with a JavaScript MIME type', () => {
  matchLocalizedSource(nginx, /location ~\* \\.mjs\$/);
  matchLocalizedSource(nginx, /default_type application\/javascript/);
});

test('rental signing keeps picker and filters inside their columns', () => {
  matchLocalizedSource(source, /\.rental-signing-property-picker>input\{box-sizing:border-box;width:calc\(100% - 32px\)/);
  matchLocalizedSource(source, /\.rental-files-toolbar input,\.rental-files-toolbar select\{box-sizing:border-box;width:100%;min-width:0/);
  matchLocalizedSource(source, /\.rental-signing-main\{[^}]*overflow-x:hidden/);
});

test('treats signing files as independent actions', () => {
  matchLocalizedSource(source, /key: 'otr'.*status: this\.otrSigned \? 'complete' : otrPending \? 'pending'/);
  matchLocalizedSource(source, /uploadAdminRentalMandateDocument\(mandate\.id, 'otr', file\)/);
  assert.doesNotMatch(source, /key: 'otr'.*disabled: noLease/);
  matchLocalizedSource(source, /relation\.includes\('rental_appointment'\) \|\| relation === 'authorization_draft' \? 'appointment'/);
});

test('scopes files by rental cycle and keeps historical cycles read only', () => {
  matchLocalizedSource(source, /selectedCycleId/);
  matchLocalizedSource(source, /selectCurrentRentalMandate/);
  matchLocalizedSource(source, /reportBelongsToMandate/);
  matchLocalizedSource(source, /cycleLeaseIds\.has\(String\(item\.leaseId\)\)/);
  matchLocalizedSource(source, /selectedCycleIsHistorical/);
  matchLocalizedSource(source, /action: !readOnly/);
  matchLocalizedSource(source, /historyReadOnly/);
  matchLocalizedSource(source, /propertyFiles/);
  assert.doesNotMatch(source, /for \(const attachment of this\.propertyAttachments\) files\.push/);
});

test('已结束租约不再显示为当前出租', () => {
  matchLocalizedSource(source, /activeCycleLease\(\) \{ return this\.cycleLeases\.find\(lease => String\(lease\.status \|\| ''\)\.toLowerCase\(\) === 'active'\) \|\| null; \}/);
  matchLocalizedSource(source, /rentalHeaderStatusKey\(\)/);
  matchLocalizedSource(source, /selectedCycleTitleKey\(\)/);
  matchLocalizedSource(source, /this\.activeCycleLease \? 'rentalFiles\.currentRental' : this\.cycleLeases\.length \? 'rentalFiles\.endedRental'/);
  matchLocalizedSource(source, /leaseStatusLabel\(lease\)/);
  matchLocalizedSource(source, /requestedLeaseId: new URLSearchParams\(window\.location\.search\)\.get\('leaseId'\) \|\| ''/);
  matchLocalizedSource(source, /String\(item\.id \|\| item\.leaseId\) === String\(this\.requestedLeaseId\)/);
  matchLocalizedSource(i18nSource, /endedRental:\s*\['租约已结束'/);
  matchLocalizedSource(i18nSource, /endedCycleOption:\s*\['已结束'/);
});

test('allows mandate attachments to be regenerated at any signing stage', () => {
  matchLocalizedSource(source, /v-if="task\.canRegenerate"/);
  matchLocalizedSource(source, /@click="regenerateTask\(task\)"/);
  matchLocalizedSource(source, /canRegenerate: !readOnly && Boolean\(this\.pmaDocument\)/);
  matchLocalizedSource(source, /canRegenerate: !readOnly && Boolean\(this\.authorizationDocument\)/);
  matchLocalizedSource(source, /pmaSigned\(\) \{ return String\(this\.pmaDocument\?\.signatureStatus \|\| ''\)\.toLowerCase\(\) === 'signed'; \}/);
  matchLocalizedSource(source, /canRegenerate: !readOnly && Boolean\(this\.otrContract\)/);
  matchLocalizedSource(source, /canRegenerate: !readOnly && Boolean\(this\.leaseContract\)/);
  assert.doesNotMatch(source, /leaseSigningStarted/);
  assert.doesNotMatch(source, /\['pending', 'sent', 'viewed', 'signed'\]\.includes\(this\.leaseSignatureStatus\)/);
});

test('重新生成与修改资料都读取系统已有资料', () => {
  matchLocalizedSource(source, /@click="editTask\(task\)"/);
  matchLocalizedSource(source, /@click="regenerateTask\(task\)"/);
  matchLocalizedSource(source, /editTask\(task\)/);
  matchLocalizedSource(source, /regenerateTask\(task\)[\s\S]*openPmaForm\(false\)/);
  matchLocalizedSource(source, /openRentalAppointmentForm\(false\)/);
  matchLocalizedSource(source, /openAuthorizationForm\(false\)/);
  matchLocalizedSource(source, /openOtrForm\(false\)/);
  matchLocalizedSource(source, /openLeaseForm\(false\)/);
  assert.doesNotMatch(source, /regenerateTask\(task\)[\s\S]*?editTask\(task\)[\s\S]*openPmaForm\(true\)/);
  matchLocalizedSource(source, /reset = false/);
});

test('支持只替换条款并用现有资料生成新版合约', () => {
  matchLocalizedSource(source, /仅更新条款/);
  matchLocalizedSource(source, /updateClausesTask\(task\)/);
  matchLocalizedSource(source, /generatePma\(\{ preserveMasterData: true \}\)/);
  matchLocalizedSource(source, /generateAuthorization\(\{ preserveMasterData: true \}\)/);
  matchLocalizedSource(source, /if \(!options\.preserveMasterData\)/);
  matchLocalizedSource(source, /旧版合约已保留/);
});

test('uses supplement signers to create or restart multi-party signing directly', () => {
  assert.doesNotMatch(source, /v-for="\(signer, index\) in signerPackageForm"/);
  matchLocalizedSource(source, /async startSigningDirect\(type\)/);
  matchLocalizedSource(source, /async openSignerPackage\(type\)/);
  matchLocalizedSource(source, /fetchAdminMandateSignatureParticipants\(this\.currentMandate\.id, document\.id\)/);
  matchLocalizedSource(source, /startAdminMandateSignaturePackage\(this\.currentMandate\.id, document\.id/);
  matchLocalizedSource(source, /fetchAdminLeaseSignatureParticipants\(leaseId\)/);
  matchLocalizedSource(source, /startAdminLeaseSignaturePackage\(leaseId, payload\)/);
  matchLocalizedSource(source, /\['pma', 'rentalAppointment', 'terminationLetter', 'otr', 'lease'\]\.includes\(this\.signingPanel\)/);
  matchLocalizedSource(source, /signerRole: 'owner'.*signerRole: 'tenant'/s);
  matchLocalizedSource(source, /signers: this\.signerPackageForm\.map/);
  matchLocalizedSource(source, /action: !readOnly && !noMandate, actionLabel: this\.pmaDocument/);
  matchLocalizedSource(source, /action: !readOnly && !noMandate, actionLabel: this\.otrMandateDocument/);
  matchLocalizedSource(source, /action: !readOnly && !noLease/);
  matchLocalizedSource(source, /restartSigning/);
});

test('fills the tenancy agreement with the available owner and tenant details', () => {
  matchLocalizedSource(source, /tenancyAgreementFields\(lease/);
  matchLocalizedSource(source, /agreementDate: lease\.startDate/);
  matchLocalizedSource(source, /landlordAddress: owner\.mailingAddress \|\| owner\.address \|\| base\.ownerAddress/);
  matchLocalizedSource(source, /tenantIdentity: lease\.tenantIdentity/);
  matchLocalizedSource(source, /tenantPhone: lease\.tenantPhone/);
  matchLocalizedSource(source, /tenantEmail: lease\.tenantEmail/);
  matchLocalizedSource(source, /bankAccount: base\.bankAccountNo/);
});

test('opens a lease details popup before generation and exposes the blank schedule and meter fields', () => {
  matchLocalizedSource(source, /leaseFormOpen/);
  matchLocalizedSource(source, /@submit\.prevent="generateLeaseDraft"/);
  matchLocalizedSource(source, /leaseMissingFields/);
  for (const field of ['paymentMode', 'renewalOption', 'specialConditions', 'electricityMeter', 'waterMeter', 'gasMeter']) {
    matchLocalizedSource(source, new RegExp(`leaseForm\\.${field}`));
  }
  matchLocalizedSource(source, /openLeaseForm\(\)/);
  matchLocalizedSource(source, /supplement\.dialogs\.lease\.hint/);
});

test('all supplement dialogs localize copy and reveal the first invalid field on submit', () => {
  const i18n = readFileSync(new URL('../src/i18n/index.js', import.meta.url), 'utf8');
  const forms = {
    otr: 'otrFormRef',
    rentalAppointment: 'rentalAppointmentFormRef',
    authorization: 'authorizationFormRef',
    terminationLetter: 'terminationLetterFormRef',
    rentalRemittance: 'rentalRemittanceFormRef',
    pma: 'pmaFormRef',
    lease: 'leaseFormRef',
  };
  for (const [key, ref] of Object.entries(forms)) {
    matchLocalizedSource(source, new RegExp(`ref="${ref}"[^>]*is-validated[^>]*novalidate`));
    matchLocalizedSource(source, new RegExp(`validateSupplementForm\\('${key}', '${ref}'\\)`));
  }
  matchLocalizedSource(source, /revealFirstInvalidControl\(form\)/);
  matchLocalizedSource(source, /\.pma-form-dialog\.is-validated \.pma-form-grid input:invalid/);
  assert.doesNotMatch(source, /:disabled="actionBusy \|\| (?:otr|rentalAppointment|authorization|terminationLetter|rentalRemittance|pma|lease)MissingFields\.length > 0"/);
  matchLocalizedSource(i18n, /const rentalSupplementLabels = \{/);
  matchLocalizedSource(i18n, /supplement: localizeLabelTree\(rentalSupplementLabels, index\)/);
});

test('all document supplement forms validate only whether required controls are filled', () => {
  const supplementDialogs = source.match(/<div v-if="otrFormOpen"[\s\S]*?<div v-if="financeDocumentOpen"/)?.[0] || '';
  matchLocalizedSource(source, /findFirstEmptyRequiredControl/);
  assert.doesNotMatch(source, /form\.checkValidity\(\)/);
  assert.doesNotMatch(supplementDialogs, /\s(?:pattern|minlength|maxlength|min|max|step)="/);
  assert.doesNotMatch(supplementDialogs, /type="(?:email|number)"/);
  matchLocalizedSource(supplementDialogs, /rentalAppointmentForm\.landlordAddress[^>]+required/);
  matchLocalizedSource(supplementDialogs, /leaseForm\.tenantPhone[^>]+required/);
});

test('owner email is collected only for documents that actually print it', () => {
  const terminationDialog = source.match(/<div v-if="terminationLetterFormOpen"[\s\S]*?<div v-if="rentalRemittanceFormOpen"/)?.[0] || '';
  const remittanceDialog = source.match(/<div v-if="rentalRemittanceFormOpen"[\s\S]*?<div v-if="pmaFormOpen"/)?.[0] || '';
  assert.doesNotMatch(terminationDialog, /terminationLetterForm\.ownerEmail/);
  assert.doesNotMatch(remittanceDialog, /rentalRemittanceForm\.ownerEmail/);
  assert.doesNotMatch(source, /const TERMINATION_LETTER_REQUIRED_FIELDS = \{[^\n]+ownerEmail/);
  assert.doesNotMatch(source, /const RENTAL_REMITTANCE_REQUIRED_FIELDS = \{[^\n]+ownerEmail/);
});

test('requires owner and company online signatures for a termination letter', () => {
  matchLocalizedSource(source, /key: 'terminationLetter'/);
  matchLocalizedSource(source, /generateAdminContractTemplate\('termination-letter'/);
  matchLocalizedSource(source, /'termination_letter_draft'/);
  matchLocalizedSource(source, /terminationLetterFormOpen/);
  for (const field of ['agreementDate', 'landlordName', 'projectName', 'unitNo', 'authorizedAgentName',
    'authorizedAgentIdentity', 'authorizedAgentPhone', 'authorizedAgentEmail', 'bankName',
    'bankPayeeName', 'bankAccountNo', 'bankSwiftCode', 'bankAddress']) {
    matchLocalizedSource(source, new RegExp(`terminationLetterForm\\.${field}`));
  }
  matchLocalizedSource(source, /signerRole: 'owner', signerName: this\.terminationLetterForm\.ownerSignerName/);
  matchLocalizedSource(source, /signerRole: 'company', signerName: this\.terminationLetterForm\.companySignerName/);
  matchLocalizedSource(source, /\['pma', 'rentalAppointment', 'terminationLetter', 'otr', 'lease'\]/);
  assert.doesNotMatch(source, /startAdminMandateDocumentSignature\(this\.currentMandate\.id, this\.terminationLetterDocument\.id/);
  matchLocalizedSource(source, /terminationLetterSigning/);
});

test('supports owner-only rental remittance authorization with complete bank fields', () => {
  matchLocalizedSource(source, /key: 'rentalRemittance'/);
  matchLocalizedSource(source, /generateAdminContractTemplate\('rental-remittance'/);
  matchLocalizedSource(source, /'rental_remittance_draft'/);
  matchLocalizedSource(source, /rentalRemittanceFormOpen/);
  matchLocalizedSource(source, /v-model="rentalRemittanceForm\.bankAccountId"/);
  matchLocalizedSource(source, /v-for="account in pmaBankAccounts"/);
  matchLocalizedSource(source, /applyRentalRemittanceBankAccount/);
  for (const field of ['agreementDate', 'landlordName', 'landlordIdentity', 'unitNo',
    'bankPayeeName', 'bankName', 'bankAccountNo', 'bankSwiftCode', 'bankBranchCode', 'bankAddress']) {
    matchLocalizedSource(source, new RegExp(`rentalRemittanceForm\\.${field}`));
  }
  matchLocalizedSource(source, /startAdminMandateDocumentSignature\(this\.currentMandate\.id, this\.rentalRemittanceDocument\.id/);
  matchLocalizedSource(source, /signerRole: 'owner'/);
  matchLocalizedSource(source, /rentalRemittanceSigning/);
});

test('template designer presents signature fields as movable signature boxes', () => {
  matchLocalizedSource(source, /templateSignatureField/);
  matchLocalizedSource(source, /startsWith\('signature\.'\)/);
  matchLocalizedSource(source, /templateSignatureWidth/);
  matchLocalizedSource(source, /templateSignatureHeight/);
});

test('keeps rental remittance data when regenerating and labels its category in Chinese', () => {
  matchLocalizedSource(source, /if \(task\.key === 'rentalRemittance'\) \{ await this\.openRentalRemittanceForm\(false\); return; \}/);
  matchLocalizedSource(source, /const previous = this\.rentalRemittanceForm \|\| emptyRentalRemittanceForm\(\)/);
  matchLocalizedSource(source, /agreementDate: previous\.agreementDate \|\| fields\.agreementDate/);
  matchLocalizedSource(source, /bankAccountId: previous\.bankAccountId \|\|/);
  const i18n = readFileSync(new URL('../src/i18n/index.js', import.meta.url), 'utf8');
  matchLocalizedSource(i18n, /remittance: \['租金汇款授权书', '租金匯款授權書', 'Rental Remittance Authorization'\]/);
});

test('termination letter form reads existing owner and bank data even when regenerating', () => {
  matchLocalizedSource(source, /async openTerminationLetterForm\(reset = false\)[\s\S]*fetchAdminOwner\(this\.selectedProperty\.ownerId\)/);
  matchLocalizedSource(source, /async openTerminationLetterForm\(reset = false\)[\s\S]*loadPmaBankAccount\(\)/);
  assert.doesNotMatch(source, /async openTerminationLetterForm\(reset = false\)[\s\S]*this\.terminationLetterForm = reset \? emptyTerminationLetterForm\(\)/);
  matchLocalizedSource(source, /terminationLetterForm = \{[\s\S]*owner\?\.fullName[\s\S]*owner\?\.identityNo[\s\S]*owner\?\.email/);
});

test('tenancy agreement selects existing photos and writes new uploads back to the property library', () => {
  matchLocalizedSource(source, /v-model="leaseSelectedPhotoIds" type="checkbox"/);
  matchLocalizedSource(source, /fetchAdminPropertyPhotos/);
  matchLocalizedSource(source, /savedFields\.photoIds/);
  matchLocalizedSource(source, /this\.leaseForm\.photoIds = this\.leaseSelectedPhotoIds\.join\(','\)/);
  matchLocalizedSource(source, /createAdminPropertyPhoto\(property\.ownerId, property\.ownerUnitId/);
  matchLocalizedSource(source, /for \(const photo of \[\.\.\.this\.leasePhotoFiles\]\)/);
  assert.doesNotMatch(source, /Promise\.all\(this\.leasePhotoFiles\.map/);
  assert.doesNotMatch(source, /photoVersionMonth|leasePhotoVersions|leaseNewPhotoVersionMonth/);
  const uploadBlock = source.match(/for \(const photo of \[\.\.\.this\.leasePhotoFiles\]\) \{[\s\S]*?\n\s*\}/)?.[0] || '';
  assert.doesNotMatch(uploadBlock, /title\s*:/);
  matchLocalizedSource(source, /saveAdminLeaseAgreementDetails\(leaseId, this\.leaseForm\)/);
});

test('tenancy agreement can append multiple batches of new photos and remove individual files', () => {
  matchLocalizedSource(source, /mergeUniquePhotoFiles\(this\.leasePhotoFiles, files\)/);
  matchLocalizedSource(source, /event\.target\.value = ''/);
  matchLocalizedSource(source, /v-for="\(file, index\) in leasePhotoFiles"/);
  matchLocalizedSource(source, /@click="removeLeasePhotoFile\(file\)"/);
  matchLocalizedSource(source, /removePhotoFile\(this\.leasePhotoFiles, file\)/);
});

test('tenancy agreement selects handover items, writes enabled state back, and submits selected ids', () => {
  matchLocalizedSource(source, /v-model="leaseSelectedChecklistIds" type="checkbox"/);
  matchLocalizedSource(source, /fetchAdminPropertyHandoverChecklist/);
  matchLocalizedSource(source, /savedFields\.handoverChecklistIds/);
  matchLocalizedSource(source, /this\.leaseForm\.handoverChecklistIds = this\.leaseSelectedChecklistIds\.join\(','\)/);
  matchLocalizedSource(source, /async syncLeaseHandoverChecklist\(\)/);
  matchLocalizedSource(source, /updateAdminPropertyHandoverChecklistItem\(property\.ownerId, property\.ownerUnitId, item\.id/);
  matchLocalizedSource(source, /await this\.syncLeaseHandoverChecklist\(\)/);
});

test('tenancy agreement shows photo and checklist choices at the top of the same form', () => {
  const leaseDialog = source.match(/<div v-if="leaseFormOpen"[\s\S]*?<div v-if="financeDocumentOpen"/)?.[0] || '';
  const photoIndex = leaseDialog.indexOf('lease-property-photo-section');
  const checklistIndex = leaseDialog.indexOf('lease-handover-checklist-section');
  const signerIndex = leaseDialog.indexOf("supplement.common.signerSection");

  assert.ok(photoIndex >= 0 && photoIndex < signerIndex);
  assert.ok(checklistIndex >= 0 && checklistIndex < signerIndex);
  matchLocalizedSource(leaseDialog, /v-model="leaseSelectedPhotoIds" type="checkbox"/);
  matchLocalizedSource(leaseDialog, /v-model="leaseSelectedChecklistIds" type="checkbox"/);
  assert.doesNotMatch(leaseDialog, /leaseFormStep|nextSelectLeaseAttachments/);
});

test('tenancy agreement lets an operator supplement a missing tenant identity', () => {
  matchLocalizedSource(source, /tenantIdentity: ''/);
  matchLocalizedSource(source, /v-model\.trim="leaseForm\.tenantIdentity"[^>]+required/);
  matchLocalizedSource(source, /tenantIdentity: savedFields\.tenantIdentity \|\| previous\.tenantIdentity \|\| fields\.tenantIdentity \|\| ''/);
  matchLocalizedSource(source, /tenantIdentity: sf\('tenantIdentity'\)/);
});
