import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync(new URL('../src/i18n/index.js', import.meta.url), 'utf8');
const keys = ['preparation', 'mandateAuthorization', 'leasingSigning', 'moveInCollection'];

test('process center provides four translated current-rental stages', () => {
  for (const key of keys) assert.match(source, new RegExp(`${key}: \\['[^']+', '[^']+', '[^']+'\\]`));
  assert.match(source, /本次出租/);
  assert.match(source, /等待业主完成线上授权签署|无法审核：请先生成并完成业主授权委托书线上签署/);
  assert.match(source, /入住交接/);
});
