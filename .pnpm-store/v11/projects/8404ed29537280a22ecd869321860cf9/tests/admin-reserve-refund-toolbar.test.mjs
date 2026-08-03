import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const read = (relativePath) => readFileSync(fileURLToPath(new URL(relativePath, import.meta.url)), 'utf8');

test('預備金頁面在頂部工具列提供業主預備金返還入口', () => {
  const toolbar = read('../src/components/ModuleToolbar.vue');
  const state = read('../src/composables/dashboardState.js');
  const actions = read('../src/composables/dashboardActions.js');
  const workspace = read('../src/components/AdminReserveWorkspace.vue');

  assert.match(toolbar, /業主預備金返還/, '頂部工具列必須顯示業主預備金返還按鈕');
  assert.match(state, /adminReserveRefundNonce:\s*0/, '頁面狀態必須保留返還操作訊號');
  assert.match(actions, /adminReserveRefundNonce \+= 1/, '點擊頂部按鈕必須發出返還操作訊號');
  assert.match(workspace, /refundNonce\(\) \{ return this\.page\.adminReserveRefundNonce; \}/, '預備金工作區必須接收返還操作訊號');
});
