import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
const source = readFileSync(new URL('../src/components/AdminPropertyProcessWorkspace.vue', import.meta.url), 'utf8');

test('current rental workbench keeps all four related stages visible without pagination', () => {
  assert.match(source, /v-for="stage in rentalWorkbench\.stages"/);
  assert.match(source, /rentalWorkbench\.stages/);
  assert.doesNotMatch(source, /paginateProcessSteps/);
  assert.doesNotMatch(source, /process-pagination/);
});
