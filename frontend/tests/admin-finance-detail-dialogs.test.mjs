import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const load = (name) => readFileSync(new URL(`../src/components/${name}`, import.meta.url), 'utf8');
const workspaces = [
  'AdminFinanceWorkspace.vue',
  'AdminRentCollectionWorkspace.vue',
  'AdminRentFinanceWorkspace.vue',
  'AdminReserveFinanceWorkspace.vue',
  'AdminExpenseFinanceWorkspace.vue'
];

test('all finance workspaces use detail buttons and dialogs instead of fixed side cards', () => {
  for (const name of workspaces) {
    const source = load(name);
    assert.match(source, /查看详情/, `${name} should expose a clear detail button`);
    assert.doesNotMatch(source, /<aside/, `${name} should not render a fixed side card`);
    assert.match(source, /<dialog/, `${name} should render details in a dialog`);
  }
});

test('detail dialogs close only from the top-right icon', () => {
  for (const name of ['AdminFinanceWorkspace.vue', 'AdminRentCollectionWorkspace.vue', 'AdminReserveFinanceWorkspace.vue', 'AdminExpenseFinanceWorkspace.vue']) {
    assert.doesNotMatch(load(name), /<menu><button[^>]+@click="closeDetails"/, `${name} should not render a bottom close button`);
  }
  assert.doesNotMatch(load('AdminRentFinanceWorkspace.vue'), /<menu><button[^>]+@click="closeActions"/);
});

test('finance detail presentation uses full width lists and larger controls', () => {
  const theme = readFileSync(new URL('../src/admin-theme.css', import.meta.url), 'utf8');
  assert.match(theme, /\.admin-finance-workspace\{[^}]*grid-template-columns:minmax\(0,1fr\)!important/);
  assert.match(theme, /\.finance-detail-button\{[^}]*font-size:13px/);
  assert.match(theme, /\.finance-record-detail-dialog\{/);
  assert.match(theme, /\.finance-record-detail-dialog>menu[^\{]*\{display:none!important\}/);
});
