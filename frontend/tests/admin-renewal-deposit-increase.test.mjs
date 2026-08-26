import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

test('续约押金字段说明只对增加差额生成财务确认', async () => {
  const workspace = await readFile(new URL('../src/components/AdminTenancyWorkspace.vue', import.meta.url), 'utf8');
  const i18n = await readFile(new URL('../src/i18n/index.js', import.meta.url), 'utf8');

  assert.match(workspace, /renewalDepositIncreaseHint/);
  assert.match(workspace, /current:\s*money\(selectedRow\?\.depositAmount\)/);
  assert.match(i18n, /提高押金时仅将增加的差额送交财务确认/);
  assert.match(i18n, /金额不变不会生成收款/);
  assert.match(i18n, /降低金额也不会自动退款/);
});
