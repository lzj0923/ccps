import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

test('產生報表彈窗避免欄位與格式選項橫向溢出', () => {
  const source = readFileSync(fileURLToPath(new URL('../src/components/AdminReportWorkspace.vue', import.meta.url)), 'utf8');

  assert.match(source, /\.report-dialog\s*\{[\s\S]*max-height:\s*calc\(100dvh - 32px\)[\s\S]*overflow:\s*auto/, '彈窗必須可在矮螢幕內垂直捲動');
  assert.match(source, /\.dialog-body select,[\s\S]*width:\s*100%;[\s\S]*box-sizing:\s*border-box/, '輸入欄位必須限制在彈窗寬度內');
  assert.match(source, /\.dialog-body fieldset\s*\{[\s\S]*min-width:\s*0/, '格式區塊不可撐開彈窗');
  assert.match(source, /grid-template-columns:\s*repeat\(2, minmax\(0, 1fr\)\)/, '雙欄格式選項必須允許縮小');
});
