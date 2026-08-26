import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

test('產生報表彈窗避免欄位與格式選項橫向溢出', () => {
  const source = readFileSync(fileURLToPath(new URL('../src/components/AdminReportWorkspace.vue', import.meta.url)), 'utf8');

  assert.match(source, /\.report-dialog form\s*\{[\s\S]*grid-template-rows:\s*auto minmax\(0, 1fr\) auto/, '彈窗頭部與操作區必須固定在內容捲動區外');
  assert.match(source, /\.dialog-body\s*\{[\s\S]*overflow-y:\s*auto/, '矮螢幕必須只捲動彈窗內容');
  assert.match(source, /\.dialog-body select,[\s\S]*width:\s*100%;[\s\S]*box-sizing:\s*border-box/, '輸入欄位必須限制在彈窗寬度內');
  assert.match(source, /\.format-fieldset\s*\{[\s\S]*min-width:\s*0/, '格式區塊不可撐開彈窗');
  assert.match(source, /grid-template-columns:\s*repeat\(2, minmax\(0, 1fr\)\)/, '雙欄格式選項必須允許縮小');
  assert.match(source, /@media \(min-width:\s*42rem\)/, '彈窗必須由手機單欄漸進增強為桌面雙欄');
  assert.doesNotMatch(source, /legacy\.t_3de1af4c1dcb|legacy\.t_d613d88cb2d8/, '格式選項不可重複顯示 XLSX 或 PDF');
});

test('所有報表可選全部、固定區間、某日之前或某日之後', () => {
  const source = readFileSync(fileURLToPath(new URL('../src/components/AdminReportWorkspace.vue', import.meta.url)), 'utf8');
  const request = readFileSync(fileURLToPath(new URL('../../backend/src/main/java/com/ccps/backend/dto/AdminReportGenerateRequest.java', import.meta.url)), 'utf8');
  const service = readFileSync(fileURLToPath(new URL('../../backend/src/main/java/com/ccps/backend/service/AdminReportService.java', import.meta.url)), 'utf8');
  const mapper = readFileSync(fileURLToPath(new URL('../../backend/src/main/java/com/ccps/backend/mapper/AdminReportMapper.java', import.meta.url)), 'utf8');
  const migration = readFileSync(fileURLToPath(new URL('../../database/migrate_report_time_modes.sql', import.meta.url)), 'utf8');

  for (const mode of ['all', 'range', 'before', 'after']) assert.match(source, new RegExp(`value="${mode}"`));
  assert.match(source, /v-model="form\.timeMode"/);
  assert.match(source, /form\.timeMode === 'range'/);
  assert.match(source, /form\.timeMode === 'before'/);
  assert.match(source, /form\.timeMode === 'after'/);
  assert.match(source, /dateStart:\s*\['range', 'after'\]\.includes\(this\.form\.timeMode\) \? this\.form\.dateStart : null/);
  assert.match(source, /dateEnd:\s*\['range', 'before'\]\.includes\(this\.form\.timeMode\) \? this\.form\.dateEnd : null/);
  assert.doesNotMatch(request, /@NotNull LocalDate dateStart/);
  assert.doesNotMatch(request, /@NotNull LocalDate dateEnd/);
  assert.match(service, /request\.dateStart\(\) != null && request\.dateEnd\(\) != null/);
  assert.match(mapper, /<if test="start != null">AND pi\.due_date &gt;= #\{start\}<\/if>/);
  assert.match(mapper, /<if test="end != null">AND pi\.due_date &lt;= #\{end\}<\/if>/);
  assert.match(migration, /MODIFY COLUMN date_start DATE NULL/);
  assert.match(migration, /MODIFY COLUMN date_end DATE NULL/);
});

test('產生報表完成後立即下載', () => {
  const source = readFileSync(fileURLToPath(new URL('../src/components/AdminReportWorkspace.vue', import.meta.url)), 'utf8');
  const i18n = readFileSync(fileURLToPath(new URL('../src/i18n/index.js', import.meta.url)), 'utf8');

  assert.match(source, /const run = await generateAdminReport\(payload\);[\s\S]*await this\.downloadFile\(run\);/);
  assert.match(i18n, /generate: \['产生并下载', '產生並下載', 'Generate and Download'\]/);
});

test('報表時間範圍預設為全部時間', () => {
  const source = readFileSync(fileURLToPath(new URL('../src/components/AdminReportWorkspace.vue', import.meta.url)), 'utf8');

  assert.match(source, /timeMode:\s*["']all["']/);
  assert.match(source, /openDialog\([\s\S]*?timeMode:\s*["']all["']/, '每次開啟彈窗都必須重設為全部時間');
});
