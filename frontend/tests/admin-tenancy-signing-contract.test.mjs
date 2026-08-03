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

test('online signing is unavailable after the current lease document is signed', () => {
  assert.match(workspace, /contractDocumentMimeType === ['"]application\/pdf['"]\s*&&\s*!selectedRow\.contractSigned/);
});

test('tenancy details expose the attachment lifecycle separately from the lease', () => {
  assert.match(workspace, /contractStatusLabel\(selectedRow\.contractStatus\)/);
  assert.match(workspace, /contractStatus !== ['"]signed['"]/);
});

test('signed signature page previews the signed document instead of the original', () => {
  const signaturePage = readFileSync(new URL('../src/pages/SignaturePage.vue', import.meta.url), 'utf8');
  assert.match(signaturePage, /documentUrl\(\)\s*\{[^}]*signature\?\.status\s*===\s*['"]signed['"][^}]*signedDocumentUrl/s);
});

test('API errors are localized before they reach user-facing prompts', () => {
  assert.match(api, /This contract document is already signed; replace it before starting a new request/);
  assert.match(api, /此合同文件已完成簽署，請先更換文件後再發起新的簽署/);
  assert.match(api, /API request failed/);
  assert.match(api, /服務請求失敗/);
});
