import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

test('renders signer name as the assigned read-only identity', async () => {
  const page = await readFile(new URL('../src/pages/SignaturePage.vue', import.meta.url), 'utf8');

  assert.match(page, /<input\s+:value="signature\.signerName"\s+readonly/);
  assert.doesNotMatch(page, /v-model(?:\.trim)?="(?:form\.)?signerName"/);
});

test('requires witness identity number for PMA and lease witnesses', async () => {
  const page = await readFile(new URL('../src/pages/SignaturePage.vue', import.meta.url), 'utf8');

  assert.match(page, /v-if="isWitness"/);
  assert.match(page, /v-model\.trim="form\.identityNo"/);
  assert.match(page, /\['owner_witness', 'tenant_witness'\]\.includes\(role\)/);
  assert.match(page, /this\.isWitness\s*&&\s*!this\.form\.identityNo/);
});

test('lease package collects owner tenant and both witnesses before sending', async () => {
  const workspace = await readFile(new URL('../src/components/AdminRentalSigningWorkspace.vue', import.meta.url), 'utf8');

  for (const role of ['owner', 'owner_witness', 'tenant', 'tenant_witness']) {
    assert.match(workspace, new RegExp(`signerRole: '${role}'`));
  }
  assert.match(workspace, /fetchAdminPropertyHandover\(this\.currentMandate\.id\)/);
  assert.match(workspace, /landlordAddress:\s*owner\.mailingAddress/);
});
