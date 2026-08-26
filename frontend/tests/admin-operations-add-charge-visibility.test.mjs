import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const source = readFileSync(
  new URL('../src/components/AdminPropertyProcessWorkspace.vue', import.meta.url),
  'utf8',
);

test('运营总览点击添加费用后将内联表单滚动到可见区域', () => {
  assert.match(source, /ref="operationsCenterBody"/);
  assert.match(source, /revealOperationsForm\(\)/);
  assert.match(source, /operationsCenterBody\.scrollTop\s*=\s*0/);
  assert.match(source, /operationsCenterBody\.querySelector\('\.operations-inline-form'\)/);
  assert.match(source, /operationsInlineForm\.querySelector\(/);
});

test('运营内联表单使用独立强调标题层级', () => {
  assert.match(source, /operations-section-heading operations-inline-heading/);
  assert.match(source, /\.operations-inline-heading\{[^}]*background:var\(--admin-soft\)/);
  assert.match(source, /\.operations-inline-heading\{[^}]*box-shadow:inset 0 3px var\(--admin-accent\)/);
  assert.match(source, /\.operations-inline-heading h4\{[^}]*font-size:18px[^}]*font-weight:800/);
});
