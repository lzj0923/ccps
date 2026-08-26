import assert from 'node:assert/strict';
import test from 'node:test';

import { orderReserveRemarks } from '../src/utils/reserveRemarks.js';

const expected = '1）第一个月不转\n2）最后一个月不转\n3）低于RM300 不转';

test('备用金返还规则按指定优先级排序并重新编号', () => {
  assert.equal(orderReserveRemarks(
    '1）低于RM300不转\n2）最后一个月不转\n3）第一个月不转',
  ), expected);
});

test('同一行的备用金返还规则也能排序', () => {
  assert.equal(orderReserveRemarks(
    '2 最后一个月不转 3 低于 RM 300 不转 1 第一个月不转',
  ), expected);
});

test('其他长期备注保留在返还规则之后', () => {
  assert.equal(orderReserveRemarks(
    '低于RM300不转\n需要财务主管复核\n第一个月不转\n最后一个月不转',
  ), `${expected}\n需要财务主管复核`);
});
