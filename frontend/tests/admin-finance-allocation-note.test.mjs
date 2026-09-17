import { matchLocalizedSource } from './helpers/localizedSource.mjs';
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
    matchLocalizedSource(source, /FinanceAllocationNoteEditor/);
    matchLocalizedSource(source, /:record="selectedRow"/);
  }
});

test('rent single and batch confirmation both submit allocation notes', () => {
  const source = read('../src/components/AdminRentCollectionWorkspace.vue');
  matchLocalizedSource(source, /form\.allocationNote/);
  matchLocalizedSource(source, /form\.reuseAllocationNote/);
  matchLocalizedSource(source, /batchForm\.allocationNote/);
  matchLocalizedSource(source, /batchForm\.reuseAllocationNote/);
  assert.doesNotMatch(source, /reuseAllocationNote:false/);
});

test('allocation note editor calls the universal finance endpoint', () => {
  const editor = read('../src/components/FinanceAllocationNoteEditor.vue');
  const api = read('../src/services/propertyApi.js');
  matchLocalizedSource(editor, /收支备注/);
  matchLocalizedSource(editor, /暂无备注记录/);
  matchLocalizedSource(editor, /white-space:nowrap/);
  matchLocalizedSource(editor, /@media\(min-width:26\.25rem\)/);
  matchLocalizedSource(editor, /reuseEnabled: true/);
  matchLocalizedSource(editor, /this\.reuseEnabled = true/);
  assert.doesNotMatch(editor, />分担备注</);
  matchLocalizedSource(api, /updateAdminFinanceAllocationNote/);
  matchLocalizedSource(api, /reviews\/\$\{financeRecordId\}\/allocation-note/);
});

test('property cashflow note reuse is selected by default', () => {
  const source = read('../src/components/AdminPropertyDetailWorkspace.vue');
  assert.doesNotMatch(source, /reuseAllocationNote:false/);
  assert.doesNotMatch(source, /reuse:false/);
  matchLocalizedSource(source, /reuseAllocationNote:true/);
  matchLocalizedSource(source, /reuse:true/);
});
