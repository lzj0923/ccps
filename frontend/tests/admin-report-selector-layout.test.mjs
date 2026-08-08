import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

test('報表類型以可換行卡片呈現且保留選取行為', () => {
  const source = readFileSync(fileURLToPath(new URL('../src/components/AdminReportWorkspace.vue', import.meta.url)), 'utf8');

  assert.match(source, /class="definition-card"/, '每個報表類型必須使用卡片樣式');
  assert.match(source, /:class="\{ active: selectedType === definition\.reportType \}"/, '卡片必須保留既有選取行為');
  assert.match(source, /\.definition-strip\s*\{[\s\S]*grid-template-columns:\s*repeat\(auto-fit, minmax\(190px, 1fr\)\)/, '類型選擇區必須依寬度自動換行');
  assert.doesNotMatch(source, /\.definition-strip\s*\{[\s\S]{0,180}overflow:\s*auto/, '類型選擇區不可出現橫向捲軸');
  assert.match(source, /owner_statement:\s*"主"/, '必須顯示單個業主帳單類型');
  assert.match(source, /tenant_statement:\s*"客"/, '必須顯示單個租客帳單類型');
  assert.match(source, /tenantId:\s*this\.form\.scopeType === "tenant"/, '租客帳單必須提交所選租客');
});
