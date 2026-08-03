import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync(
  new URL('../src/components/AdminMaintenanceWorkspace.vue', import.meta.url),
  'utf8',
);

test('maintenance pagination state is reactive and page changes use one method', () => {
  assert.match(source, /data\(\)[\s\S]*listPage:\s*1/);
  assert.match(source, /data\(\)[\s\S]*listPageSize:\s*10/);
  assert.match(source, /@click="goListPage\(listPage \+ 1\)"/);
  assert.match(source, /goListPage\(page\)\s*\{/);
});

test('maintenance filters reset pagination to the first page', () => {
  assert.match(source, /'page\.projectFilter'\(\)\s*\{\s*this\.resetListPage\(\)/);
  assert.match(source, /'page\.statusFilter'\(\)\s*\{\s*this\.resetListPage\(\)/);
  assert.match(source, /'page\.moduleSearch'\(\)\s*\{\s*this\.resetListPage\(\)/);
});
