import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const dashboard = readFileSync(new URL('../src/components/AdminDashboardWorkspace.vue', import.meta.url), 'utf8');
const theme = readFileSync(new URL('../src/admin-theme.css', import.meta.url), 'utf8');

test('工作台上下两排共用同一组桌面列轨道', () => {
  assert.match(
    dashboard,
    /\.workbench-top-grid,\.workbench-bottom-grid\{[^}]*grid-template-columns:minmax\(0,1\.6fr\) minmax\(20rem,1fr\)/,
  );
  assert.doesNotMatch(dashboard, /(?:^|\n)\.workbench-bottom-grid\{grid-template-columns:/);
  assert.match(
    theme,
    /\.admin-dashboard-workspace :is\(\.workbench-top-grid,\.workbench-bottom-grid\)\{grid-template-columns:minmax\(0,1\.6fr\) minmax\(20rem,1fr\)/,
  );
});

test('工作台上下两排在主题断点同时切换为单列', () => {
  assert.match(
    theme,
    /@media \(max-width:80rem\)\{[\s\S]{0,240}\.admin-dashboard-workspace :is\(\.workbench-top-grid,\.workbench-bottom-grid\)\{grid-template-columns:minmax\(0,1fr\)\}/,
  );
});
