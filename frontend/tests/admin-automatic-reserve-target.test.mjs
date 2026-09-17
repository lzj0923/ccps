import { matchLocalizedSource } from './helpers/localizedSource.mjs';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const workspace = readFileSync(new URL('../src/components/AdminReserveWorkspace.vue', import.meta.url), 'utf8');

test('reserve settings expose automatic and manual target modes', () => {
  matchLocalizedSource(workspace, /v-model="settings\.automaticCalculation"/);
  matchLocalizedSource(workspace, /自动标准仅按该单位大楼管理费 × 2 计算/);
  matchLocalizedSource(workspace, /automaticCalculation: this\.settings\.automaticCalculation/);
  matchLocalizedSource(workspace, /account\.minimumBalanceMode === 'auto' \? '系統自動' : '人工設定'/);
});

test('reserve details explain the calculated target', () => {
  matchLocalizedSource(workspace, /selectedAccount\.calculatedMinimumBalance/);
  matchLocalizedSource(workspace, /自动标准仅为该单位大楼管理费 × 2/);
});
