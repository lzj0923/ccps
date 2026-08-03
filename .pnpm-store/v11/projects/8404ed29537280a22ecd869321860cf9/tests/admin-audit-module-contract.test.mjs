import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

test('操作審計模組提供共用後台狀態所需的空白資料', () => {
  const source = readFileSync(fileURLToPath(new URL('../src/data/dashboardData.js', import.meta.url)), 'utf8');

  assert.match(source, /id:\s*"adminAudit"/, '操作審計必須存在於後台模組');
  assert.match(source, /rows\.adminAudit\s*=\s*\[\]/, '操作審計必須提供空白列表，避免共用列表狀態讀取失敗');
  assert.match(source, /headers\.adminAudit\s*=\s*\[/, '操作審計必須提供表格欄位');
  assert.match(source, /adminAudit:\s*\[\[/, '操作審計必須提供功能、流程及提醒摘要');
});
