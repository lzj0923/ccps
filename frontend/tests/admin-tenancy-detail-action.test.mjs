import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

test('租客列表可點選資料列，並提供明確的查看詳情操作', () => {
  const source = readFileSync(fileURLToPath(new URL('../src/components/AdminTenancyWorkspace.vue', import.meta.url)), 'utf8');
  const adminTheme = readFileSync(fileURLToPath(new URL('../src/admin-theme.css', import.meta.url)), 'utf8');

  assert.match(source, /<tr v-for="row in rows"[^>]*@click="openTenantDetails\(row\)"/, '資料列必須保留整列點選入口');
  assert.match(source, /<th class="tenancy-action-column">\{\{ \$t\('legacy\.t_f3ea6d345e2a'\) \}\}<\/th>/, '列表必須增加操作欄');
  assert.match(source, /class="tenancy-row-action"[^>]*@click\.stop="openTenantDetails\(row\)"[^>]*>\{\{ \$t\('tenancy\.viewDetails'\) \}\}/, '每筆資料必須提供可由鍵盤操作的查看詳情按鈕');
  assert.match(source, /<td colspan="11" class="admin-owner-empty">/, '空資料列必須覆蓋新增後的全部欄位');
  assert.match(source, /\.admin-tenancy-workspace \.tenancy-table-wrap\{overflow-x:hidden\}/, '租客列表不得出現橫向滾動條');
  assert.match(source, /\.tenancy-table-wrap table\{width:100%;min-width:0;table-layout:fixed/, '表格必須固定適配容器寬度');
  assert.doesNotMatch(source, /\.tenancy-table-wrap table\{min-width:1180px\}/, '不得保留強制撐寬表格的舊樣式');
  assert.match(source, /<dialog ref="tenancyDetailDialog" class="tenancy-detail-dialog"/, '租客詳情必須改為原生彈出卡片');
  assert.match(source, /closeDialog\('tenancyDetailDialog'\)/, '詳情卡片必須提供關閉入口');
  assert.doesNotMatch(source, /<aside class="panel detail-panel tenancy-detail-panel">/, '頁面右側不應再常駐租客詳情');
  assert.match(source, /class="tenancy-invoice-link"[^>]*@click="openInvoiceDetails\(selectedRow\)"/, '詳情卡片必須保留月份租金明細入口');
  assert.match(source, /ref="invoiceDetailsDialog"/, '月份租金明細必須使用獨立對話框顯示');
  assert.match(adminTheme, /\.admin-shell \.admin-tenancy-workspace\{[^}]*grid-template-columns:minmax\(0,1fr\)!important/, '移除右側常駐詳情後，租客列表必須橫向撐滿工作區');
  assert.doesNotMatch(adminTheme, /\.admin-shell \.admin-tenancy-workspace\{[^}]*grid-template-columns:minmax\(0,1fr\) minmax\(280px,32%\)!important/, '不得保留舊的兩欄寬度限制');
});

test('租客詳情使用摘要、輕量提示與橫向操作工具列呈現', () => {
  const source = readFileSync(fileURLToPath(new URL('../src/components/AdminTenancyWorkspace.vue', import.meta.url)), 'utf8');

  assert.match(source, /class="tenancy-detail-summary"/, '詳情頂部必須提供租金摘要');
  assert.match(source, /class="tenancy-detail-body"/, '詳情內容必須有清楚的主體版面');
  assert.match(source, /<section[^>]*class="tenancy-action-toolbar"/, '租約操作必須集中在橫向操作工具列');
  assert.match(source, /class="tenancy-policy-row"/, '寬限期說明必須改為輕量資訊列');
  assert.match(source, /class="tenancy-invoice-footer"/, '月份明細入口必須與收款進度整合');
  assert.match(source, /class="tenancy-detail-sections"/, '租客、租約與收款資料必須分組排列');
  assert.doesNotMatch(source, /<aside class="tenancy-action-panel">/, '詳情卡片不得保留整塊右側操作欄');
  assert.match(source, /\.tenancy-detail-body\{[^}]*overflow-x:hidden;overflow-y:auto/, '詳情資料區必須只允許垂直捲動');
  assert.match(source, /\.tenancy-action-toolbar\{[^}]*grid-template-columns:minmax\(8rem,auto\) minmax\(0,1fr\)/, '桌面版操作工具列必須使用緊湊橫向版面');
  assert.match(source, /\.tenancy-detail-card \.tenancy-actions\{grid-template-columns:repeat\(3,minmax\(0,1fr\)\)/, '平板版操作工具列必須保持三欄，避免過度拉高');
  assert.match(source, /@media\(max-width:40rem\)\{\.tenancy-detail-card \.tenancy-actions\{grid-template-columns:repeat\(2,minmax\(0,1fr\)\)\}\}/, '手機版操作工具列必須使用兩欄且不得橫向滾動');
  assert.match(source, /\.tenancy-invoice-link\{[^}]*border:1px solid var\(--tenancy-web-color-rule-strong\)/, '月份明細入口必須使用網頁端次級按鈕樣式');
});

test('租客詳情卡片不再顯示合約生成與發送通知操作', () => {
  const source = readFileSync(fileURLToPath(new URL('../src/components/AdminTenancyWorkspace.vue', import.meta.url)), 'utf8');
  const start = source.indexOf('<dialog ref="tenancyDetailDialog"');
  const end = source.indexOf('<dialog ref="currentUnpaidDialog"', start);
  const detail = source.slice(start, end);

  assert.ok(start >= 0 && end > start, '必須能定位租客詳情卡片');
  assert.doesNotMatch(detail, /@click="openLeaseSigning"/, '不應顯示發起簽署');
  assert.doesNotMatch(detail, /openTemplateGenerator\('otr'\)/, '不應顯示生成 OTR 出價函');
  assert.doesNotMatch(detail, /openTemplateGenerator\('tenancy-agreement'\)/, '不應顯示生成租賃合約');
  assert.doesNotMatch(detail, /@click="chooseContract"|ref="contractInput"/, '不應顯示上傳或更換租約');
  assert.doesNotMatch(detail, /@click="sendReminder"|legacy\.t_0bdaef716172/, '不應顯示發送通知');
  assert.doesNotMatch(detail, /@click="viewContract"|@click="downloadContract"/, '不應顯示查看或下載租約');
  assert.doesNotMatch(source, /async viewContract\(|async downloadContract\(|async openContract\(/, '不得保留詳情卡片專用的租約查看下載程式');
});

test('租客列表將租金到期日標示為繳費日', () => {
  const source = readFileSync(fileURLToPath(new URL('../src/components/AdminTenancyWorkspace.vue', import.meta.url)), 'utf8');
  const messages = readFileSync(fileURLToPath(new URL('../src/i18n/legacy.generated.js', import.meta.url)), 'utf8');

  assert.match(source, /\$t\('legacy\.t_53ebb183604b'\)/, '租客列表必須使用繳費日多語言標題');
  assert.match(messages, /"t_53ebb183604b": "繳費日"/, '繁體中文標題必須為繳費日');
});

test('租客列表的選中與懸停狀態使用桌面端淺色背景', () => {
  const source = readFileSync(fileURLToPath(new URL('../src/components/AdminTenancyWorkspace.vue', import.meta.url)), 'utf8');

  assert.match(source, /\.tenancy-table-row\.selected\{background:#eaf7f6\}/, '選中資料列必須使用可讀的淺色背景');
  assert.match(source, /\.tenancy-table-row:hover\{background:#f5fbfb\}/, '懸停資料列必須使用可讀的淺色背景');
  assert.match(source, /\.tenancy-table-row:active\{background:#e4f4f3\}/, '按下狀態必須保持文字對比');
  assert.doesNotMatch(source, /\.tenancy-table-row(?:\.selected|:active)\{background:var\(--color-native-accent-soft\)\}/, '桌面列表不得誤用移動端深色強調背景');
});

test('租客列表优先放大显示租期并将系统租约编号移到下方', () => {
  const source = readFileSync(fileURLToPath(new URL('../src/components/AdminTenancyWorkspace.vue', import.meta.url)), 'utf8');

  assert.match(source, /<td class="tenancy-lease-cell"><strong class="tenancy-lease-period">\{\{ row\.leaseStart[\s\S]*?<\/strong><small class="tenancy-lease-code">\{\{ row\.leaseNo/, '租期必须显示在租约编号上方');
  assert.match(source, /\.tenancy-lease-period\{[^}]*font-size:1rem;[^}]*font-weight:800/, '租期必须作为主要信息放大加粗');
  assert.match(source, /\.tenancy-lease-code\{[^}]*font-size:var\(--tenancy-web-text-caption\);[^}]*font-weight:400/, '系统租约编号必须作为次要信息缩小显示');
});
