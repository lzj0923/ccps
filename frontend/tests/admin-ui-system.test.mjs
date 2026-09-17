import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');
const sidebar = read('../src/components/SidebarNav.vue');
const header = read('../src/components/PageHeader.vue');
const toolbar = read('../src/components/ModuleToolbar.vue');
const projectWorkspace = read('../src/components/AdminProjectManagementWorkspace.vue');
const workspace = read('../src/components/DataWorkspace.vue');
const adminPage = read('../src/pages/AdminPage.vue');
const router = read('../src/router.js');
const messages = read('../src/i18n/index.js');
const tokens = read('../tokens.css');
const theme = read('../src/admin-theme.css');

test('管理端保留地区大屏为默认入口，工作台为第二入口', () => {
  assert.match(router, /path:\s*'\/admin'[\s\S]*?moduleId:\s*'adminSmartDashboard'/);
  assert.match(sidebar, /adminPrimaryModules/);
  assert.match(sidebar, /nav-group-count/);
  assert.match(sidebar, /project-card/);
});

test('管理端公共骨架具备页面上下文、模块标识和无障碍当前页', () => {
  assert.match(header, /topbar-context/);
  assert.match(header, /adminSectionLabel/);
  assert.match(sidebar, /aria-current/);
  assert.match(adminPage, /data-admin-module/);
});

test('筛选和操作在结构上分区，图标统一使用 Lucide', () => {
  assert.match(toolbar, /class="toolbar-query"/);
  assert.match(toolbar, /class="toolbar-actions"/);
  assert.match(toolbar, /import \{ Search \} from '@lucide\/vue'/);
  assert.doesNotMatch(toolbar, /search-mark">⌕/);
});

test('复合搜索框仅由外层容器绘制边框', () => {
  assert.match(theme, /\.admin-shell \.search-box input[^\{]*\{[^}]*border:0!important[^}]*background:transparent!important/s);
  assert.match(projectWorkspace, /import \{ Search \} from '@lucide\/vue'/);
  assert.doesNotMatch(projectWorkspace, /<span>⌕<\/span>/);
});

test('新增的通用界面文案完整接入三语言资源', () => {
  for (const key of ['overview', 'assets', 'rental', 'finance', 'operations', 'system']) {
    assert.equal((messages.match(new RegExp(`${key}:`, 'g')) || []).length >= 3, true, `missing locale variants for navigation.${key}`);
  }
  for (const key of ['listView', 'boardView', 'timelineView', 'moreActions', 'paymentProgress', 'allDistricts']) {
    assert.match(messages, new RegExp(`${key}:`));
    assert.match(`${toolbar}\n${workspace}`, new RegExp(`ui\\.${key}`));
  }
});

test('统一主题使用管理端令牌并覆盖响应式、焦点和减少动画状态', () => {
  for (const token of ['--color-admin-canvas', '--color-admin-sidebar', '--color-admin-accent', '--admin-control-height', '--admin-sidebar-width']) {
    assert.match(tokens, new RegExp(token));
    assert.match(theme, new RegExp(`var\\(${token}\\)`));
  }
  assert.match(theme, /@media \(max-width:820px\)/);
  assert.match(theme, /:focus-visible/);
  assert.match(theme, /prefers-reduced-motion:reduce/);
});

test('管理端弹窗遮罩始终覆盖吸顶顶部栏', () => {
  const navLayer = Number(tokens.match(/--z-admin-nav:\s*(\d+)/)?.[1]);
  const overlayLayer = Number(tokens.match(/--z-admin-overlay:\s*(\d+)/)?.[1]);

  assert.equal(Number.isFinite(navLayer), true);
  assert.equal(Number.isFinite(overlayLayer), true);
  assert.equal(overlayLayer > navLayer, true);
  assert.match(theme, /\.admin-owner-detail-modal\{[^}]*z-index:var\(--z-admin-overlay\)/s);
});
