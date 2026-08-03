import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

test('租客列表的查詢明細使用窄欄圖標按鈕', () => {
  const source = readFileSync(fileURLToPath(new URL('../src/components/AdminTenancyWorkspace.vue', import.meta.url)), 'utf8');

  assert.match(source, /class="row-actions tenancy-detail-button"[^>]*aria-label="查詢各月份租金明細"/, '操作按鈕必須保留清楚的無障礙名稱');
  assert.match(source, /class="tenancy-detail-icon"/, '操作按鈕必須提供固定的圖標內容');
  assert.match(source, /\.tenancy-table-wrap th:last-child,\.tenancy-table-wrap td:last-child\{width:58px/, '操作欄必須固定為窄欄');
});

test('租客列表將租金到期日標示為繳費日', () => {
  const source = readFileSync(fileURLToPath(new URL('../src/components/AdminTenancyWorkspace.vue', import.meta.url)), 'utf8');

  assert.match(source, /<th>繳費日<\/th>/, '租客列表必須以繳費日標示租金應繳日期');
});
