import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

import { calculateTenantDeposit, TENANT_DEPOSIT_MONTHS } from '../src/utils/tenantDeposit.js';

const tenancySource = readFileSync(new URL('../src/components/AdminTenancyWorkspace.vue', import.meta.url), 'utf8');
const processSource = readFileSync(new URL('../src/components/AdminPropertyProcessWorkspace.vue', import.meta.url), 'utf8');
const detailSource = readFileSync(new URL('../src/components/AdminPropertyDetailWorkspace.vue', import.meta.url), 'utf8');
const signingSource = readFileSync(new URL('../src/components/AdminRentalSigningWorkspace.vue', import.meta.url), 'utf8');

test('tenant deposit is calculated as two and a half months of rent', () => {
  assert.equal(TENANT_DEPOSIT_MONTHS, 2.5);
  assert.equal(calculateTenantDeposit(5500), 13750);
  assert.equal(calculateTenantDeposit('1800.25'), 4500.63);
  assert.equal(calculateTenantDeposit(0), 0);
});

test('new lease forms derive the deposit instead of requiring manual entry', () => {
  assert.match(tenancySource, /'leaseForm\.monthlyRent'\(value\).*calculateTenantDeposit\(value\)/);
  assert.match(processSource, /'actionForm\.monthlyRent'\(value\).*calculateTenantDeposit\(value\)/);
  assert.match(tenancySource, /depositAmount[^>]*readonly/);
  assert.match(processSource, /depositAmount[^>]*readonly/);
});

test('rental photos can be uploaded without entering a separate title', () => {
  assert.doesNotMatch(detailSource, /v-model\.trim="rentalPhotoForm\.title"[^>]*required/);
  assert.match(detailSource, /rentalPhotoForm\.title\s*\|\|\s*this\.rentalPhotoFile\?\.name/);
});

test('generated OTR and lease signing links remain visible for copying', () => {
  assert.match(signingSource, /generatedSigningLinks/);
  assert.match(signingSource, /copySigningLink/);
  assert.match(signingSource, /verificationCodeThirtyMinutes/);
  assert.match(signingSource, /result\?\.signingLinks/);
});
