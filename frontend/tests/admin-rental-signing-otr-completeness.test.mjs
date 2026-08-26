import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync(new URL('../src/components/AdminRentalSigningWorkspace.vue', import.meta.url), 'utf8');

test('OTR is completed in a dedicated form before generation', () => {
  assert.match(source, /v-if="otrFormOpen"/);
  assert.match(source, /const OTR_REQUIRED_FIELDS/);
  assert.match(source, /openOtrForm\(\)/);
  assert.match(source, /tenantWitnessName/);
  assert.match(source, /landlordWitnessName/);
  assert.match(source, /renewalYears/);
  assert.match(source, /generateAdminContractTemplate\('otr',\s*\{\s*\.\.\.this\.templateFields\(\),\s*\.\.\.this\.otrForm/);
});

test('generated OTR stays in progress until all four parties sign', () => {
  assert.match(source, /nextOtrSignerRole\(\)/);
  assert.match(source, /signingOrder === 1\) return 'tenant_witness'/);
  assert.match(source, /signingOrder === 2\) return 'owner'/);
  assert.match(source, /signingOrder === 3\) return 'owner_witness'/);
  assert.match(source, /otrSigned\(\).*signingOrder\) >= 4/s);
  assert.doesNotMatch(source, /otrComplete\(\).*\['otr', 'otr_document'\]/);
  assert.match(source, /this\.otrSigned \? 'complete' : otrPending \? 'pending'/);
  assert.match(source, /rentalFiles\.allSignersInvited/);
  assert.match(source, /this\.signingPanel = type/);
  for (const role of ['tenant', 'tenant_witness', 'owner', 'owner_witness']) {
    assert.match(source, new RegExp(`signerRole: '${role}'`));
  }
  assert.match(source, /startAdminMandateSignaturePackage\(this\.currentMandate\.id, document\.id/);
  assert.match(source, /openSignerPackage\('otr'\)/);
  assert.match(source, /restartSigning/);
});

test('OTR files use their own category instead of rental contract', () => {
  assert.match(source, /CATEGORY_ORDER = \['management', 'appointment', 'authorization', 'otr', 'contract'/);
  assert.match(source, /relation\.startsWith\('otr'\) \? 'otr' : 'authorization'/);
  assert.match(source, /contract\.contractType === 'O_LEASE_RESERVATION' \? 'otr' : 'contract'/);
});
