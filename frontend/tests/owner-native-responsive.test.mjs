import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const nativeCssUrl = new URL('../src/native-app.css', import.meta.url);
const tokensUrl = new URL('../tokens.css', import.meta.url);
const capacitorConfigUrl = new URL('../capacitor.config.json', import.meta.url);

test('原生壳使用 Capacitor 8 安全区变量', async () => {
  const [css, configText] = await Promise.all([
    readFile(nativeCssUrl, 'utf8'),
    readFile(capacitorConfigUrl, 'utf8')
  ]);
  const config = JSON.parse(configText);

  assert.match(css, /--safe-area-inset-top/);
  assert.match(css, /--safe-area-inset-bottom/);
  assert.equal(config.plugins?.SystemBars?.insetsHandling, 'css');
});

test('原生业主端具备统一移动布局覆盖', async () => {
  const css = await readFile(nativeCssUrl, 'utf8');

  assert.match(css, /Hallmark · macrostructure: Workbench/);
  assert.match(css, /\.owner-summary-grid\.lifecycle-summary/);
  assert.match(css, /\.property-toolbar/);
  assert.match(css, /\.lifecycle-property-row/);
  assert.match(css, /\.payment-property-card/);
  assert.match(css, /\.notification-filters/);
  assert.match(css, /\.notification-list article/);
  assert.match(css, /@media \(min-width:40rem\)/);
});

test('移动设计令牌覆盖排版、间距、控件和状态', async () => {
  const tokens = await readFile(tokensUrl, 'utf8');

  for (const token of [
    '--color-native-surface',
    '--color-native-accent-ink',
    '--font-native-display',
    '--space-native-md',
    '--text-native-body',
    '--native-control-height',
    '--radius-native-card',
    '--shadow-native-card'
  ]) {
    assert.match(tokens, new RegExp(token));
  }
});
