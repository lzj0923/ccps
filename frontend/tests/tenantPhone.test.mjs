import assert from 'node:assert/strict';
import test from 'node:test';

import { splitTenantPhone, validateTenantPhone } from '../src/utils/tenantPhone.js';

test('rejects the previously accepted three-digit phone number', () => {
  const result = validateTenantPhone('100', 'CN', true);
  assert.equal(result.valid, false);
  assert.equal(result.reason, 'invalid');
});

test('normalizes valid Chinese and Malaysian local numbers to E.164', () => {
  assert.equal(validateTenantPhone('138 0013 8000', 'CN', true).e164, '+8613800138000');
  assert.equal(validateTenantPhone('012-345 6789', 'MY', true).e164, '+60123456789');
});

test('rejects an international number that conflicts with the selected country', () => {
  const result = validateTenantPhone('+86 138 0013 8000', 'MY', true);
  assert.equal(result.valid, false);
  assert.equal(result.reason, 'country_mismatch');
});

test('splits an existing E.164 number for editing', () => {
  assert.deepEqual(splitTenantPhone('+8613800138000'), {
    country: 'CN',
    nationalNumber: '13800138000'
  });
});

test('allows an empty phone only when WhatsApp is disabled', () => {
  assert.equal(validateTenantPhone('', 'MY', false).valid, true);
  assert.equal(validateTenantPhone('', 'MY', true).reason, 'required');
});
