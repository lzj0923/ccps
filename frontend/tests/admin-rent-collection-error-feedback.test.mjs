import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const workspace = readFileSync(new URL('../src/components/AdminRentCollectionWorkspace.vue', import.meta.url), 'utf8');
const propertyApi = readFileSync(new URL('../src/services/propertyApi.js', import.meta.url), 'utf8');

test('keeps rent collection errors outside the scrollable form body', () => {
  const errorPosition = workspace.indexOf('ref="confirmError"');
  const bodyPosition = workspace.indexOf('class="rent-confirm-body"');
  assert.ok(errorPosition >= 0, 'the confirmation error must be rendered');
  assert.ok(errorPosition < bodyPosition, 'the confirmation error must stay above the scrollable form body');
  assert.match(workspace, /\.rent-action-error\{margin:12px 22px 0;padding:10px 12px/);
});

test('translates an insufficient tenant deposit balance precisely', () => {
  assert.match(propertyApi, /\['Tenant deposit balance is insufficient', '租客押金余额不足，请减少本次抵扣金额或改用直接收款'\]/);
});
