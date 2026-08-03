import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync(new URL('../src/components/AdminAuditWorkspace.vue', import.meta.url), 'utf8');

test('audit action codes are displayed with Chinese labels', () => {
  assert.match(source, /admin_direct_reserve_topup:\s*['"]新增預備金充值['"]/);
  assert.match(source, /complete_electronic_signature:\s*['"]完成電子簽署['"]/);
  assert.match(source, /start_electronic_signature:\s*['"]發起電子簽署['"]/);
  assert.match(source, /generate_report:\s*['"]產生報表['"]/);
  assert.match(source, /return actionLabels\[action\] \|\| \(action \? `其他操作：/);
});
