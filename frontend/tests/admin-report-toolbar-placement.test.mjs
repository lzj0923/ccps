import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const read = (relativePath) => readFileSync(fileURLToPath(new URL(relativePath, import.meta.url)), 'utf8');

test('報表工具列顯示在類型卡片與列表之間', () => {
  const page = read('../src/pages/ReportPage.vue');
  const workspace = read('../src/components/AdminReportWorkspace.vue');

  assert.doesNotMatch(page, /<ModuleToolbar\s*\/>/, '報表頁不可在統計卡片前顯示工具列');
  assert.match(workspace, /import ModuleToolbar from "\.\/ModuleToolbar\.vue";/, '報表工作區必須自行顯示工具列');
  assert.match(workspace, /<div class="definition-strip">[\s\S]*<\/div>\s*<ModuleToolbar\s*\/>\s*<div v-if="loading"/, '工具列必須位於報表類型卡片與列表之間');
});

test('下載檔名包含報表範圍與資料期間', () => {
  const workspace = read('../src/components/AdminReportWorkspace.vue');

  assert.match(workspace, /downloadName\(run\)/, '下載時必須使用描述性檔名');
  assert.match(workspace, /run\.scopeName \|\| run\.projectName \|\| this\.\$t\('reports\.allScope'\)/, '檔名必須包含匯出範圍');
  assert.match(workspace, /run\.dateStart.*run\.dateEnd/, '檔名必須包含資料期間');
});

test('SQL Account 匯出結果不再列入可用報表', () => {
  const mapper = read('../../backend/src/main/java/com/ccps/backend/mapper/AdminReportMapper.java');
  const service = read('../../backend/src/main/java/com/ccps/backend/service/AdminReportService.java');

  assert.doesNotMatch(mapper, /\('SYNC', 'SQL Account 匯出結果', 'sync'/, '預設報表不得再建立 SQL Account 報表');
  assert.match(mapper, /UPDATE report_definitions SET enabled = 0 WHERE report_type = 'sync'/, '既有 SQL Account 報表定義必須停用');
  assert.match(mapper, /FROM report_definitions WHERE enabled = 1 ORDER BY id/, '可用報表只可回傳啟用項目');
  assert.match(service, /mapper\.ensureDefinitions\(\);\s*mapper\.disableSyncDefinition\(\);/, '載入報表前必須停用既有 SQL Account 定義');
});
