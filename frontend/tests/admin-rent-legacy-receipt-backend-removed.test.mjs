import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = relativePath => readFileSync(new URL(`../../${relativePath}`, import.meta.url), 'utf8');
const controller = read('backend/src/main/java/com/ccps/backend/controller/AdminTenancyController.java');
const service = read('backend/src/main/java/com/ccps/backend/service/AdminTenancyService.java');
const mapper = read('backend/src/main/java/com/ccps/backend/mapper/AdminTenancyMapper.java');

test('租金专用旧收据后端链路已移除', () => {
  assert.doesNotMatch(controller, /rent-payments\/\{financeRecordId\}\/receipt/);
  assert.doesNotMatch(service, /downloadRentReceipt|RentReceiptRow/);
  assert.doesNotMatch(mapper, /findRentReceipt|class RentReceiptRow/);
});
