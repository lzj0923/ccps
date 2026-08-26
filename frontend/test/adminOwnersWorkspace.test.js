import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

test('owner editor uses update copy instead of add copy', async () => {
  const page = await readFile(new URL('../src/components/AdminOwnersWorkspace.vue', import.meta.url), 'utf8');

  assert.match(page, /ownerEditingId\s*\?\s*\$t\('legacy\.t_60b4ae9082a3'\)/);
  assert.match(page, /:\s*\$t\('legacy\.t_f2a2753dcacf'\)/);
  assert.match(page, /ownerEditingId\s*\?\s*'房主资料更新失败'\s*:\s*'新增业主失败'/);
});
