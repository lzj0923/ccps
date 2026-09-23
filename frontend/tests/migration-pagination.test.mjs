import test from 'node:test';
import assert from 'node:assert/strict';
import { fetchAllPages } from '../src/utils/fetchAllPages.js';
import { readFileSync } from 'node:fs';
test('rental workspaces use the complete inventory without changing eligibility filters', () => {
  for (const name of ['AdminPropertyProcessWorkspace', 'AdminRentalSigningWorkspace']) {
    const source = readFileSync(new URL(`../src/components/${name}.vue`, import.meta.url), 'utf8');
    assert.match(source, /fetchAllPages\(fetchAdminProperties\)/);
    assert.doesNotMatch(source, /fetchAdminProperties\(\{ page: 1, pageSize: (200|500) \}\)/);
  }
});
test('does not lose properties after backend caps page size to 100', async () => {
  const calls = [];
  const rows = await fetchAllPages(async ({ page, pageSize }) => {
    calls.push(page); assert.equal(pageSize, 100);
    return { rows: Array.from({ length: 100 }, (_, i) => ({ id: (page-1)*100+i })), page: { totalPages: 5 } };
  });
  assert.equal(rows.length, 500);
  assert.equal(new Set(rows.map(row => row.id)).size, 500);
  assert.deepEqual(calls, [1,2,3,4,5]);
});
test('read failure never silently becomes an empty inventory', async () => {
  await assert.rejects(fetchAllPages(async () => { throw new Error('offline'); }), /offline/);
});
