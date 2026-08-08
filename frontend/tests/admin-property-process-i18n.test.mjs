import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync(new URL('../src/i18n/index.js', import.meta.url), 'utf8');
const keys = ['propertySetup', 'mandateAuthorization', 'leasingSigning', 'moveInCollection', 'rentalOperations', 'leaseClosure'];

test('process center stage labels describe system state instead of signing files', () => {
  for (const key of keys) assert.match(source, new RegExp(`${key}: \\['[^']+', '[^']+', '[^']+'\\]`));
  assert.match(source, /本次出租/);
  assert.match(source, /mandateAuthorization: \['出租委托'/);
  assert.match(source, /leasingSigning: \['租客与租约'/);
  assert.match(source, /入住交接/);
});
