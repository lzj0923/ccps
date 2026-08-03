import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

test('renders signer name as the assigned read-only identity', async () => {
  const page = await readFile(new URL('../src/pages/SignaturePage.vue', import.meta.url), 'utf8');

  assert.match(page, /簽署人姓名<input\s+:value="signature\.signerName"\s+readonly/);
  assert.doesNotMatch(page, /簽署人姓名<input\s+v-model/);
});
