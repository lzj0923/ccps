import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const read = path => readFileSync(new URL(path, import.meta.url), 'utf8');

test('all finance detail dialogs expose the same allocation note editor', () => {
  for (const file of [
    '../src/components/AdminFinanceWorkspace.vue',
    '../src/components/AdminReserveFinanceWorkspace.vue',
    '../src/components/AdminExpenseFinanceWorkspace.vue',
    '../src/components/AdminRentFinanceWorkspace.vue'
  ]) {
    const source = read(file);
    assert.match(source, /FinanceAllocationNoteEditor/);
    assert.match(source, /:record="selectedRow"/);
  }
});

test('rent single and batch confirmation both submit allocation notes', () => {
  const source = read('../src/components/AdminRentCollectionWorkspace.vue');
  assert.match(source, /form\.allocationNote/);
  assert.match(source, /form\.reuseAllocationNote/);
  assert.match(source, /batchForm\.allocationNote/);
  assert.match(source, /batchForm\.reuseAllocationNote/);
  assert.doesNotMatch(source, /reuseAllocationNote:false/);
});

test('allocation note editor calls the universal finance endpoint', () => {
  const editor = read('../src/components/FinanceAllocationNoteEditor.vue');
  const api = read('../src/services/propertyApi.js');
  assert.match(editor, /收支备注/);
  assert.match(editor, /暂无备注记录/);
  assert.match(editor, /white-space:nowrap/);
  assert.match(editor, /@media\(min-width:26\.25rem\)/);
  assert.match(editor, /reuseEnabled: true/);
  assert.match(editor, /this\.reuseEnabled = true/);
  assert.doesNotMatch(editor, />分担备注</);
  assert.match(api, /updateAdminFinanceAllocationNote/);
  assert.match(api, /reviews\/\$\{financeRecordId\}\/allocation-note/);
});

test('property cashflow note reuse is selected by default', () => {
  const source = read('../src/components/AdminPropertyDetailWorkspace.vue');
  assert.doesNotMatch(source, /reuseAllocationNote:false/);
  assert.doesNotMatch(source, /reuse:false/);
  assert.match(source, /reuseAllocationNote:true/);
  assert.match(source, /reuse:true/);
});
