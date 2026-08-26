import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const workspace = readFileSync(new URL('../src/components/AdminRentFinanceWorkspace.vue', import.meta.url), 'utf8');

test('rent finance history removes SQL sync and the fixed detail card', () => {
  assert.doesNotMatch(workspace, /待同步 SQL|syncStatus|<aside/);
  assert.match(workspace, /admin-rent-finance-workspace[\s\S]*\.admin-rent-finance-workspace\{display:block/);
});

test('row operation dialog contains all rent finance actions', () => {
  assert.match(workspace, /@click\.stop="openActions\(row\)"/);
  for (const label of ['下载收据', '下载发票', '上传付款凭证', '前往租客与租金', '退回待确认']) {
    assert.match(workspace, new RegExp(label));
  }
});

test('rent finance history supports selected batch documents and transactional reopen', () => {
  assert.match(workspace, /v-model="selectedIds"/);
  assert.match(workspace, /togglePageSelection/);
  assert.match(workspace, /batchDownload\('invoice'\)/);
  assert.match(workspace, /batchDownload\('receipt'\)/);
  assert.match(workspace, /batchReopenAdminFinanceReviews\(ids, this\.batchReopenNote\)/);
  assert.match(workspace, /任意一笔校验失败时整批不会提交/);
});
