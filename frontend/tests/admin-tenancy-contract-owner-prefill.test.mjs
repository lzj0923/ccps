import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync(new URL('../src/components/AdminTenancyWorkspace.vue', import.meta.url), 'utf8');

test('contract generator prefills the primary owner returned by tenancy query', () => {
  assert.match(source, /landlordName:\s*row\.ownerName\s*\|\|\s*''/);
  assert.match(source, /landlordIdentity:\s*row\.ownerIdentity\s*\|\|\s*''/);
});

test('tenancy agreement generator passes the selected lease for property photos', () => {
  assert.match(source, /\.\.\.common,\s*leaseId:\s*row\.leaseId\s*\|\|\s*''/);
});
