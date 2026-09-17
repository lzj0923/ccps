import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const workspace = readFileSync(new URL('../src/components/AdminTenancyWorkspace.vue', import.meta.url), 'utf8');
const i18n = readFileSync(new URL('../src/i18n/index.js', import.meta.url), 'utf8');
const propertyApi = readFileSync(new URL('../src/services/propertyApi.js', import.meta.url), 'utf8');

test('contract generator dialogs keep localized labels when opened from their owning workflows', () => {
  assert.match(workspace, /\$t\('tenancy\.generateOtrTitle'\)/);
  assert.match(workspace, /\$t\('tenancy\.generateTenancyAgreementTitle'\)/);
  assert.match(workspace, /\$t\('tenancy\.generateDownloadPdf'\)/);
  assert.match(workspace, /workflow === 'otr'[\s\S]*openTemplateGenerator\('otr'\)/);
  assert.match(workspace, /workflow === 'lease-contract'[\s\S]*openTemplateGenerator\('tenancy-agreement', true\)/);
  assert.match(workspace, /setTimeout\(\(\) => URL\.revokeObjectURL\(url\), 1000\)/);
  assert.doesNotMatch(workspace, />生成授权委托书</);
  assert.doesNotMatch(workspace, />生成 OTR 出价函</);
  assert.doesNotMatch(workspace, /generateAuthorizationTitle|templateType === 'authorization'/);
  for (const key of ['generateAuthorization', 'generateOtr', 'generateTenancyAgreement', 'generateDownloadPdf']) {
    assert.match(i18n, new RegExp(`${key}: \\['[^']+', '[^']+', '[^']+'\\]`));
  }
});

test('tenancy agreement downloads with the PDF extension even when the response header is missing or stale', () => {
  assert.match(propertyApi, /const expectedExtension = '\.pdf'/);
  assert.match(propertyApi, /filename = .*expectedExtension/);
  assert.match(propertyApi, /return \{ blob: await response\.blob\(\), filename \};/);
});

test('authorization letter is owned by rental mandate page, not tenants and rent', () => {
  const workspace = readFileSync(new URL('../src/components/AdminTenancyWorkspace.vue', import.meta.url), 'utf8');
  const mandateWorkspace = readFileSync(new URL('../src/components/AdminRentalMandateWorkspace.vue', import.meta.url), 'utf8');
  assert.doesNotMatch(workspace, /\$t\('tenancy\.generateAuthorization'\)/);
  assert.match(mandateWorkspace, /\$t\('tenancy\.generateAuthorization'\)/);
  assert.doesNotMatch(mandateWorkspace, /openAuthorizationGenerator\(item\)/);
  assert.doesNotMatch(mandateWorkspace, /@click="openAuthorizationGenerator\(selected\)"/);
  assert.match(mandateWorkspace, /generateAdminContractTemplate\('authorization'/);
  assert.match(mandateWorkspace, /uploadAdminRentalMandateDocument\(this\.selected\.id, 'authorization_draft'/);
  assert.match(mandateWorkspace, /v-if="authorizationOpen" class="modal-backdrop" @pointerdown\.self="closeAuthorizationDialog"/);
  assert.match(mandateWorkspace, /authorizationOpen:\s*false/);
  assert.match(mandateWorkspace, /closeAuthorizationDialog\(\) \{ this\.authorizationOpen = false/);
  assert.doesNotMatch(mandateWorkspace, /<dialog ref="authorizationDialog"/);
});
