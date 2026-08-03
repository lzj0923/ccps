import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync(
  new URL('../src/components/AdminRentalMandateWorkspace.vue', import.meta.url),
  'utf8',
);

test('rental mandates use the backend page response and shared pager', () => {
  assert.match(source, /import AdminListPager from ['"]\.\/AdminListPager\.vue['"]/);
  assert.match(source, /<AdminListPager/);
  assert.match(source, /page:\s*this\.pageNumber/);
  assert.match(source, /pageSize:\s*this\.pageSize/);
  assert.match(source, /totalRows\s*=\s*Number\(data\.page\?\.totalRows/);
});

test('rental mandate search and status filters reset to the first page', () => {
  assert.match(source, /@keyup\.enter="resetAndLoad"/);
  assert.match(source, /@change="resetAndLoad"/);
  assert.match(source, /resetAndLoad\(\)\s*\{\s*this\.pageNumber\s*=\s*1;/);
});
