import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

import { localizeTenantApiErrorMessage } from '../src/services/tenantApiErrorMessages.js';

const propertyApiSource = readFileSync(new URL('../src/services/propertyApi.js', import.meta.url), 'utf8');

function loadPropertyApiErrorLocalizer() {
  const block = propertyApiSource.match(/const API_ERROR_MESSAGES[\s\S]*?(?=\nasync function request)/)?.[0] || '';
  return new Function('localizeTenantApiErrorMessage', 'translateLegacyText', `${block.replace('export function', 'function')}; return localizeApiErrorMessage;`)(localizeTenantApiErrorMessage, value => value);
}

test('routes tenancy errors through the tenancy error localizer', () => {
  assert.match(propertyApiSource, /localizeTenantApiErrorMessage\(candidate\)/);
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

test('deduplicates and localizes repeated owner phone validation errors', () => {
  const localizeApiErrorMessage = loadPropertyApiErrorLocalizer();
  assert.equal(
    localizeApiErrorMessage('Owner phone number must use E.164 format : Owner phone number must use E.164 format', 400),
    '业主联系电话格式不正确，请输入带国家区号的号码，例如 +60123456789',
  );
});
