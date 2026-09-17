import { matchLocalizedSource } from './helpers/localizedSource.mjs';
import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const workspace = readFileSync(new URL('../src/components/AdminRentFinanceWorkspace.vue', import.meta.url), 'utf8');

test('rent finance history removes SQL sync and the fixed detail card', () => {
  assert.doesNotMatch(workspace, /待同步 SQL|syncStatus|<aside/);
  matchLocalizedSource(workspace, /admin-rent-finance-workspace[\s\S]*\.admin-rent-finance-workspace\{display:block/);
});

test('row operation dialog contains all rent finance actions', () => {
  matchLocalizedSource(workspace, /@click\.stop="openActions\(row\)"/);
  for (const label of ['下载收据', '下载发票', '上传付款凭证', '前往租客与租金', '退回待确认']) {
    matchLocalizedSource(workspace, new RegExp(label));
  }
});

test('rent finance history supports selected batch documents and transactional reopen', () => {
  matchLocalizedSource(workspace, /v-model="selectedIds"/);
  matchLocalizedSource(workspace, /togglePageSelection/);
  matchLocalizedSource(workspace, /batchDownload\('invoice'\)/);
  matchLocalizedSource(workspace, /batchDownload\('receipt'\)/);
  matchLocalizedSource(workspace, /batchReopenAdminFinanceReviews\(ids, this\.batchReopenNote\)/);
  matchLocalizedSource(workspace, /任意一笔校验失败时整批不会提交/);
});
