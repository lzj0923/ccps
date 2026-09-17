import test from 'node:test';
import assert from 'node:assert/strict';
import { ownerNoticeTabs } from '../src/utils/ownerPortfolio.js';
test('后台已返回全部分类时，移动端只能显示一个全部标签', () => {
  const tabs = ownerNoticeTabs([{ key: 'all', label: '全部通知', count: 9 }, { key: 'reserve', count: 9 }], 9);
  assert.deepEqual(tabs.map(item => item.key), ['all', 'reserve']);
  assert.equal(tabs[0].count, 9);
});
