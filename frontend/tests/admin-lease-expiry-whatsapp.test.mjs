import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync(new URL('../src/components/AdminReminderWorkspace.vue', import.meta.url), 'utf8');

test('lease expiry reminder keeps WhatsApp and targets business staff', () => {
  assert.match(source, /value === 'lease_expiry' \? 'business'/);
  assert.match(source, /\['rent_due', 'lease_expiry'\]\.includes\(value\)/);
  assert.match(source, /业务人员/);
});

test('lease expiry business reminder is shown as a system automatic rule without manual actions', () => {
  assert.match(source, /rule\.systemManaged/);
  assert.match(source, /系统自动规则/);
  assert.match(source, /后端启动后立即扫描，并按每小时排程自动执行/);
  assert.match(source, /if \(rule\.systemManaged\) return/);
});
