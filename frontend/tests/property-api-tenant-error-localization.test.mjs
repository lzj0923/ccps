import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

import { localizeTenantApiErrorMessage } from '../src/services/tenantApiErrorMessages.js';

const propertyApiSource = readFileSync(new URL('../src/services/propertyApi.js', import.meta.url), 'utf8');

test('routes tenancy errors through the tenancy error localizer', () => {
  assert.match(propertyApiSource, /localizeTenantApiErrorMessage\(value\)/);
});

test('explains that an existing tenant should be selected when the identity number is duplicated', () => {
  assert.equal(
    localizeTenantApiErrorMessage('Tenant identity number already exists'),
    '该证件号已被现有租客使用，请从“选择已有租客”中选择对应租客。',
  );
});

test('does not treat a shared email address as proof that two tenant records are the same person', () => {
  assert.equal(
    localizeTenantApiErrorMessage('Tenant email already exists'),
    null,
  );
});

test('shows the real owner login conflict instead of a generic operation failure', () => {
  assert.match(propertyApiSource, /\['Owner phone already exists', '该手机号已作为业主登录账号使用，请更换手机号或直接选择已有业主'\]/);
});
