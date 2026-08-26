import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const workspace = new URL('../src/components/AdminTenancyWorkspace.vue', import.meta.url);

test('租客与租金提供两个月宽限期提示和手动解约操作', async () => {
  const source = await readFile(workspace, 'utf8');

  assert.match(source, /closeAdminLease/);
  assert.match(source, /@click="openManualClose"/);
  assert.match(source, /ref="closeLeaseDialog"/);
  assert.match(source, /value="normal_expiry"/);
  assert.match(source, /value="early_termination"/);
  assert.match(source, /plusCalendarMonths\(this\.selectedRow\?\.leaseEnd, 2\)/);
  assert.match(source, /gracePeriodActiveHint/);
  assert.match(source, /await closeAdminLease\(this\.selectedRow\.leaseId/);
});
