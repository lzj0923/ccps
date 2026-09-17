import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync(new URL('../src/components/AdminPropertyDetailWorkspace.vue', import.meta.url), 'utf8');
const signingSource = readFileSync(new URL('../src/components/AdminRentalSigningWorkspace.vue', import.meta.url), 'utf8');
const rentalSpaceSource = readFileSync(new URL('../src/components/AdminRentalSpaceManager.vue', import.meta.url), 'utf8');

test('opens property basic details in edit mode from the rental process', () => {
  assert.match(source, /tab==='basic'&&params\.get\('action'\)==='edit'\)this\.startBasicEdit\(\)/);
});

test('stores multiple management bank accounts below the building management fee', () => {
  assert.match(source, /legacy\.t_ddde7b074a02/);
  assert.match(source, /managementBankAccounts/);
  assert.match(source, /addManagementBankAccount/);
  assert.match(source, /removeManagementBankAccount/);
  assert.match(source, /<section class="wide management-bank-accounts">/);
  assert.doesNotMatch(source, /service\.key === 'building-management'[\s\S]*management-bank-accounts/);
  assert.match(source, /normalizeManagementBankAccounts/);
  assert.match(source, /const managementBankAccounts = normalizeManagementBankAccounts/);
  assert.match(source, /profilePayload[\s\S]*managementBankAccounts/);
  assert.doesNotMatch(source, /t_34c8e1d0af1d/);
  assert.match(signingSource, /pmaBankAccounts/);
  assert.match(signingSource, /pmaForm\.bankAccountId/);
  assert.match(signingSource, /applyPmaBankAccount/);
});

test('property bank account supports transfer limit and overseas transfer fee', () => {
  assert.match(source, /v-model\.number="bankAccountForm\.transferLimit"/);
  assert.match(source, /v-model="bankAccountForm\.overseasBank"/);
  assert.match(source, /v-if="bankAccountForm\.overseasBank"/);
  assert.match(source, /v-model\.number="bankAccountForm\.overseasTransferFee"/);
  assert.match(source, /legacy\.t_648f677839a0/);
  assert.match(source, /legacy\.t_229bc69dbcfe/);
});

test('property handover checklist can be enabled or disabled directly with a checkbox', () => {
  assert.match(source, /class="handover-checklist-toggle"/);
  assert.match(source, /@change="toggleHandoverChecklistItem\(item,\$event\.target\.checked\)"/);
  assert.match(source, /async toggleHandoverChecklistItem\(item,enabled\)/);
  assert.match(source, /updateAdminPropertyHandoverChecklistItem\(this\.property\.ownerId,this\.property\.ownerUnitId,item\.id/);
});

test('occupied whole-unit space opens the exact lease and can end it without leaving the property', () => {
  assert.match(rentalSpaceSource, /space\.spaceType === 'whole_unit' && space\.currentLeaseId/);
  assert.match(rentalSpaceSource, /this\.\$emit\('edit-lease',space\.currentLeaseId\)/);
  assert.match(source, /@edit-lease="openRentalSpaceLease"/);
  assert.match(source, /this\.leaseOptions\.find\(item=>String\(item\.leaseId\)===String\(leaseId\)\)/);
  assert.match(source, /await closeAdminLease\(this\.selectedRental\.leaseId/);
  assert.match(source, /await this\.\$refs\.rentalSpaceManager\?\.load\(\)/);
});
