import { matchLocalizedSource } from './helpers/localizedSource.mjs';
import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
const source = readFileSync(new URL('../src/components/AdminPropertyProcessWorkspace.vue', import.meta.url), 'utf8');

test('current rental workbench keeps all seven journey steps visible without pagination', () => {
  matchLocalizedSource(source, /v-for="\(step, index\) in journeySteps"/);
  matchLocalizedSource(source, /rentalWorkbench\.stages/);
  for (const key of ['propertySetup', 'mandateAuthorization', 'tenantSetup', 'leaseSetup', 'billingOperations', 'maintenanceOperations', 'leaseClosure']) assert.ok(source.includes(key), key);
  assert.doesNotMatch(source, /paginateProcessSteps/);
  assert.doesNotMatch(source, /process-pagination/);
});
