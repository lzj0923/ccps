import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const workspace = readFileSync(
  new URL('../src/components/AdminTenancyWorkspace.vue', import.meta.url),
  'utf8',
);
const api = readFileSync(
  new URL('../src/services/propertyApi.js', import.meta.url),
  'utf8',
);

test('tenancy no longer exposes generated test lease contracts', () => {
  assert.doesNotMatch(workspace, /自動生成測試租約|generateTestContract|generateAdminTestLeaseContract/);
  assert.doesNotMatch(api, /test-contract|generateAdminTestLeaseContract/);
});

test('system-generated tenancy agreements use the dedicated generated-contract upload route', () => {
  assert.match(api, /uploadAdminGeneratedLeaseContract/);
  assert.match(api, /\/generated-contract/);
  assert.match(workspace, /uploadAdminGeneratedLeaseContract/);
  assert.match(
    workspace,
    /generateAdminContractTemplate\([\s\S]*?uploadAdminGeneratedLeaseContract\(this\.selectedRow\.leaseId, file\)/,
  );
});

test('tenancy details no longer expose an online-signing action', () => {
  assert.doesNotMatch(workspace, /@click="openLeaseSigning"/);
  assert.doesNotMatch(workspace, /selectedRow\.contractStatus === 'signed' \? \$t\('rentalFiles\.restartSigning'\)/);
});

test('tenancy details expose the attachment lifecycle separately from the lease', () => {
  assert.match(workspace, /contractStatusLabel\(selectedRow\.contractStatus\)/);
  assert.match(workspace, /missing: 'contractMissing', uploaded: 'contractUploadedPending', pending: 'contractSigning', signed: 'contractSigned'/);
});

test('signed signature page previews the signed document inline at the signature position', () => {
  const signaturePage = readFileSync(new URL('../src/pages/SignaturePage.vue', import.meta.url), 'utf8');
  assert.match(signaturePage, /currentDocumentBaseUrl\(\)[^}]*canDownloadSigned[^}]*signed-document/s);
  assert.match(signaturePage, /pdfSignatureAnchor\(this\.previewPage\)/);
  assert.match(signaturePage, /this\.previewPage = signatureTargetPage\(updated\)/);
});

test('public signing no longer asks for or resends a verification code', () => {
  const signaturePage = readFileSync(new URL('../src/pages/SignaturePage.vue', import.meta.url), 'utf8');
  assert.doesNotMatch(signaturePage, /verificationCode/);
  assert.doesNotMatch(signaturePage, /resendPublicSignatureCode/);
  assert.doesNotMatch(signaturePage, /code-row/);
});

test('public signing page shows the contract first and opens signing from a round floating button', () => {
  const signaturePage = readFileSync(new URL('../src/pages/SignaturePage.vue', import.meta.url), 'utf8');
  const viewer = signaturePage.match(/<section v-else-if="signature" class="contract-viewer">[\s\S]*?<\/section>/)?.[0] || '';
  assert.match(viewer, /class="document-preview"/);
  assert.doesNotMatch(viewer, /class="sign-form"/);
  assert.match(signaturePage, /class="signature-fab"[^>]*@click="openSigning"/);
  assert.match(signaturePage, /<dialog v-if="signature\?\.canSign"[^>]*class="sign-dialog"/);
  assert.match(signaturePage, /class="signer-name"[\s\S]*signature\.signerName/);
  assert.match(signaturePage, /openSigning\(\)[^}]*showModal\(\)/);
  assert.match(signaturePage, /\.signature-fab\{[^}]*border-radius:var\(--radius-round\)/);
});

test('tenancy signing generates links before choosing a delivery method', () => {
  assert.match(workspace, /sendLeaseSigningEmail/);
  assert.match(workspace, /whatsAppLeaseSigningUrl/);
  assert.match(workspace, /rentalFiles\.sendByEmail/);
  assert.match(workspace, /rentalFiles\.shareByWhatsApp/);
  assert.doesNotMatch(workspace, /v-model\.trim="signer\.signerEmail"[^>]*required/);
  assert.match(workspace, /v-model\.trim="link\.deliveryEmail"/);
  assert.match(api, /JSON\.stringify\(\{ token, recipientEmail \}\)/);
  assert.doesNotMatch(workspace, /verificationCodeThirtyMinutes/);
});

test('API errors are localized before they reach user-facing prompts', () => {
  assert.match(api, /This contract document is already signed; replace it before starting a new request/);
  assert.match(api, /此合同文件已完成簽署，請先更換文件後再發起新的簽署/);
  assert.match(api, /API request failed/);
  assert.match(api, /服務請求失敗/);
});
