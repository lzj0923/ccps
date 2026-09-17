import { matchLocalizedSource } from './helpers/localizedSource.mjs';
import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

import { calculateTenantDeposit, splitTenantDeposit, TENANT_DEPOSIT_MONTHS } from '../src/utils/tenantDeposit.js';
import { contentDispositionFilename } from '../src/utils/contentDisposition.js';
import { pdfSignatureAnchor, signatureTargetPage } from '../src/utils/signatureTarget.js';

const tenancySource = readFileSync(new URL('../src/components/AdminTenancyWorkspace.vue', import.meta.url), 'utf8');
const processSource = readFileSync(new URL('../src/components/AdminPropertyProcessWorkspace.vue', import.meta.url), 'utf8');
const detailSource = readFileSync(new URL('../src/components/AdminPropertyDetailWorkspace.vue', import.meta.url), 'utf8');
const signingSource = readFileSync(new URL('../src/components/AdminRentalSigningWorkspace.vue', import.meta.url), 'utf8');
const signaturePageSource = readFileSync(new URL('../src/pages/SignaturePage.vue', import.meta.url), 'utf8');
const publicSignatureControllerSource = readFileSync(new URL('../../backend/src/main/java/com/ccps/backend/controller/PublicElectronicSignatureController.java', import.meta.url), 'utf8');

test('tenant deposit is calculated as two and a half months of rent', () => {
  assert.equal(TENANT_DEPOSIT_MONTHS, 2.5);
  assert.equal(calculateTenantDeposit(5500), 13750);
  assert.equal(calculateTenantDeposit('1800.25'), 4500.63);
  assert.equal(calculateTenantDeposit(0), 0);
});

test('agreement splits the stored total deposit instead of charging utility deposit twice', () => {
  assert.deepEqual(splitTenantDeposit(2500, 1000), { securityDeposit: 2000, utilityDeposit: 500 });
  assert.deepEqual(splitTenantDeposit(1000, 10000), { securityDeposit: 0, utilityDeposit: 1000 });
  assert.deepEqual(splitTenantDeposit(3000, 1000, 400), { securityDeposit: 2600, utilityDeposit: 400 });
  matchLocalizedSource(signingSource, /splitTenantDeposit\(lease\.depositAmount, lease\.monthlyRent/);
  matchLocalizedSource(signingSource, /securityDeposit: String\(deposit\.securityDeposit\)/);
  matchLocalizedSource(signingSource, /utilityDeposit: String\(deposit\.utilityDeposit\)/);
});

test('signature page only enables submission after a meaningful drawn path', () => {
  assert.doesNotMatch(signaturePageSource, /startDraw\(event\).*this\.drew = true/);
  matchLocalizedSource(signaturePageSource, /this\.signatureDistance >= 20/);
});

test('signed contract refreshes inline and downloads only from the explicit round save action', () => {
  matchLocalizedSource(signaturePageSource, /class="viewer-fabs"/);
  matchLocalizedSource(signaturePageSource, /class="file-save-fab"/);
  matchLocalizedSource(signaturePageSource, /:src="previewDocumentUrl"/);
  matchLocalizedSource(signaturePageSource, /:key="documentRevision"/);
  matchLocalizedSource(signaturePageSource, /download=false/);
  matchLocalizedSource(signaturePageSource, /download=true/);
  matchLocalizedSource(signaturePageSource, /this\.documentRevision = Date\.now\(\)/);
  assert.doesNotMatch(signaturePageSource, /class="download-link"/);
  matchLocalizedSource(publicSignatureControllerSource, /@RequestParam\(defaultValue = "false"\) boolean download/);
  matchLocalizedSource(publicSignatureControllerSource, /file\(service\.downloadSigned\(token\), download\)/);
});

test('first visit opens page one and only the post-sign refresh jumps to the signature page', () => {
  matchLocalizedSource(signaturePageSource, /previewPage: 1/);
  matchLocalizedSource(signaturePageSource, /pdfSignatureAnchor\(this\.previewPage\)/);
  matchLocalizedSource(signaturePageSource, /this\.signature = updated; this\.previewPage = signatureTargetPage\(updated\); this\.documentRevision = Date\.now\(\)/);
});

test('every signing file opens and refreshes on its own signature page', () => {
  const cases = [
    ['property_management_agreement_draft', 'owner', 7],
    ['property_management_agreement_draft', 'company', 7],
    ['property_management_agreement_draft', 'customer_service', 7],
    ['rental_appointment_draft', 'owner', 1],
    ['rental_appointment_draft', 'second_owner', 1],
    ['rental_appointment_draft', 'witness', 1],
    ['management_authorization_draft', 'owner', 3],
    ['termination_letter_draft', 'company', 2],
    ['rental_remittance_draft', 'owner', 1],
    ['otr', 'tenant_witness', 1],
    ['lease_contract', 'owner', 12],
    ['lease_contract', 'tenant', 12],
    ['lease_contract', 'owner_witness', 12],
    ['lease_contract', 'tenant_witness', 12]
  ];
  for (const [documentKind, signerRole, page] of cases) {
    assert.equal(signatureTargetPage({ documentKind, signerRole }), page);
  }
  assert.equal(signatureTargetPage({ documentKind: 'lease_contract', signerRole: 'tenant', signaturePage: 19 }), 19);
  assert.equal(pdfSignatureAnchor(12), '#page=12&zoom=page-width');
  matchLocalizedSource(signaturePageSource, /pdfSignatureAnchor\(this\.previewPage\)/);
  matchLocalizedSource(signaturePageSource, /this\.documentRevision = Date\.now\(\)/);
});

test('lease supplement form reveals missing contract party or bank fields before generation', () => {
  matchLocalizedSource(signingSource, /v-model\.trim="leaseForm\.tenantAddress"/);
  matchLocalizedSource(signingSource, /v-model\.trim="leaseForm\.landlordAddress"/);
  matchLocalizedSource(signingSource, /v-model\.trim="leaseForm\.bankAccount"/);
  matchLocalizedSource(signingSource, /leasePaymentNeedsBank/);
  matchLocalizedSource(signingSource, /bankBranch: sf\('bankBranchAddress'\)/);
  matchLocalizedSource(signingSource, /validateSupplementForm\('lease', 'leaseFormRef'\)/);
});

test('lease supplement fields are saved and reused instead of being requested again', () => {
  matchLocalizedSource(signingSource, /fetchAdminLeaseAgreementDetails/);
  matchLocalizedSource(signingSource, /saveAdminLeaseAgreementDetails\(leaseId, this\.leaseForm\)/);
  matchLocalizedSource(signingSource, /ownerWitnessSignerName: savedFields\.ownerWitnessSignerName/);
  matchLocalizedSource(signingSource, /tenantWitnessSignerName: savedFields\.tenantWitnessSignerName/);
  matchLocalizedSource(signingSource, /await fetchAdminLeaseSignatureParticipants\(leaseId\)/);
  matchLocalizedSource(signingSource, /const byRole = new Map/);
});

test('rental appointment creates links for every signature box used by the file', () => {
  matchLocalizedSource(signingSource, /\['pma', 'rentalAppointment', 'terminationLetter', 'otr', 'lease'\]/);
  matchLocalizedSource(signingSource, /signerRole: 'second_owner'/);
  matchLocalizedSource(signingSource, /signerRole: 'witness'/);
  matchLocalizedSource(signingSource, /type === 'rentalAppointment' \? this\.rentalAppointmentDocument/);
});

test('new lease forms derive the deposit instead of requiring manual entry', () => {
  matchLocalizedSource(tenancySource, /'leaseForm\.monthlyRent'\(value\).*calculateTenantDeposit\(value\)/);
  matchLocalizedSource(processSource, /'actionForm\.monthlyRent'\(value\).*calculateTenantDeposit\(value\)/);
  matchLocalizedSource(tenancySource, /depositAmount[^>]*readonly/);
  matchLocalizedSource(processSource, /depositAmount[^>]*readonly/);
});

test('rental photos can be uploaded without entering a separate title', () => {
  assert.doesNotMatch(detailSource, /v-model\.trim="rentalPhotoForm\.title"[^>]*required/);
  assert.doesNotMatch(detailSource, /v-model(?:\.trim)?="rentalPhotoForm\.title"/);
  matchLocalizedSource(detailSource, /createAdminPropertyPhoto\(this\.property\.ownerId,this\.property\.ownerUnitId,payload,this\.rentalPhotoFile\)/);
});

test('generated OTR and lease signing links support explicit delivery without verification codes', () => {
  matchLocalizedSource(signingSource, /generatedSigningLinks/);
  matchLocalizedSource(signingSource, /copySigningLink/);
  matchLocalizedSource(signingSource, /sendSigningEmail/);
  matchLocalizedSource(signingSource, /whatsAppSigningUrl/);
  matchLocalizedSource(signingSource, /rentalFiles\.sendByEmail/);
  matchLocalizedSource(signingSource, /rentalFiles\.shareByWhatsApp/);
  assert.doesNotMatch(signingSource, /v-model\.trim="signer\.signerEmail"[^>]*required/);
  assert.doesNotMatch(signingSource, /v-model\.trim="signerForm\.email"[^>]*required/);
  matchLocalizedSource(signingSource, /v-model\.trim="link\.deliveryEmail"/);
  matchLocalizedSource(signingSource, /deliveryEmailRequired/);
  assert.doesNotMatch(signingSource, /verificationCodeThirtyMinutes/);
  matchLocalizedSource(signingSource, /result\?\.signingLinks/);
});

test('generated files prefer the UTF-8 contract name over the generic ASCII fallback', () => {
  const header = 'attachment; filename="tenancy-agreement.pdf"; filename*=UTF-8\'\'%E7%A7%9F%E9%87%91%E6%B1%87%E6%AC%BE%E6%8E%88%E6%9D%83%E4%B9%A6.pdf';
  assert.equal(contentDispositionFilename(header, 'fallback.pdf'), '租金汇款授权书.pdf');
});
