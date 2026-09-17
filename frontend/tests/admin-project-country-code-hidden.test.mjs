import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = relativePath => readFileSync(new URL(relativePath, import.meta.url), 'utf8');
const projectManagement = read('../src/components/AdminProjectManagementWorkspace.vue');
const buildingPayment = read('../src/components/AdminBuildingPaymentWorkspace.vue');
const i18n = read('../src/i18n/index.js');

test('建案表单隐藏国家代码并继续默认提交 MY', () => {
  assert.doesNotMatch(projectManagement, /projectManagement\.countryCode/);
  assert.doesNotMatch(projectManagement, /v-model\.trim="form\.countryCode"/);
  assert.match(projectManagement, /countryCode:\s*'MY'/);
  assert.match(projectManagement, /countryCode:\s*source\.countryCode\.trim\(\)\.toUpperCase\(\)/);

  assert.doesNotMatch(buildingPayment, /building\.countryCode/);
  assert.doesNotMatch(buildingPayment, /v-model\.trim="projectForm\.countryCode"/);
  assert.match(buildingPayment, /countryCode:\s*'MY'/);
  assert.match(buildingPayment, /countryCode:\s*this\.projectForm\.countryCode\.toUpperCase\(\)/);

  assert.match(i18n, /formHint:\s*\['建案编码必须唯一。',\s*'建案編碼必須唯一。',\s*'Project codes must be unique\.'/);
});
