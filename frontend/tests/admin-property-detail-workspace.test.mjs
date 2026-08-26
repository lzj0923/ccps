import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync(new URL('../src/components/AdminPropertyDetailWorkspace.vue', import.meta.url), 'utf8');
const signingSource = readFileSync(new URL('../src/components/AdminRentalSigningWorkspace.vue', import.meta.url), 'utf8');

test('stores multiple management bank accounts below the building management fee', () => {
  assert.match(source, /管理层银行账号/);
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
  assert.match(source, /单日转账额度/);
  assert.match(source, /海外汇款手续费/);
});
