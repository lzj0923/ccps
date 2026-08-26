import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync(new URL('../src/components/AdminPropertyDetailWorkspace.vue', import.meta.url), 'utf8');

test('房产收支记录可导出当前筛选后的全部数据', () => {
  assert.match(source, /import \{ downloadCsv \} from '\.\.\/utils\/csvExporter';/);
  assert.match(source, /@click="exportCashflows"/);
  assert.match(source, /exportCashflows\(\)\{/);
  assert.match(source, /this\.filteredCashflows\.map\(/, '导出必须使用筛选后的完整结果');
  assert.doesNotMatch(source, /exportCashflows\(\)[\s\S]{0,1000}this\.pagedCashflows/, '导出不得只使用当前分页');
  assert.match(source, /downloadCsv\([^,]+,headers,rows\)/);
  for (const header of ['交易编号', '来源', '金额（RM）', '确认状态', '付款方式', '凭证', '收支备注']) {
    assert.match(source, new RegExp(header));
  }
});
