import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync(new URL('../src/components/AdminReminderWorkspace.vue', import.meta.url), 'utf8');

test('lease expiry reminder keeps WhatsApp and targets business staff', () => {
  assert.match(source, /value === 'lease_expiry' \? 'business'/);
  assert.match(source, /\['rent_due', 'lease_expiry'\]\.includes\(value\)/);
  assert.match(source, /业务人员/);
});

test('system-managed reminder rules stay hidden from the user rule-management list', () => {
  assert.match(source, /visibleRules\(\)\s*\{\s*return this\.rules\.filter\(rule => !rule\.systemManaged\);\s*\}/);
  assert.match(source, /\{\{ visibleRules\.length \}\}/);
  assert.doesNotMatch(source, /automatic-rule-chip|automatic-rule-notice|系统自动规则/);
  assert.match(source, /if \(rule\.systemManaged\) return/);
});
