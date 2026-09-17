import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const nativeCssUrl = new URL('../src/native-app.css', import.meta.url);
const tokensUrl = new URL('../tokens.css', import.meta.url);
const capacitorConfigUrl = new URL('../capacitor.config.json', import.meta.url);
const indexUrl = new URL('../index.html', import.meta.url);
const webIconUrl = new URL('../public/ccps-favicon-32.png', import.meta.url);
const androidIconUrl = new URL('../android/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png', import.meta.url);
const iosIconUrl = new URL('../ios/App/App/Assets.xcassets/AppIcon.appiconset/AppIcon-512@2x.png', import.meta.url);

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

  assert.match(css, /Hallmark · macrostructure: Ecosystem Index/);
  assert.match(css, /\.owner-app-v2/);
  assert.match(css, /\.owner-stat-hero/);
  assert.match(css, /\.owner-mobile-portal/);
  assert.match(css, /\.owner-mobile-ring/);
  assert.match(css, /\.owner-mobile-property-card/);
  assert.match(css, /@media\(max-width:22\.5rem\)/);
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
    '--color-native-brand',
    '--color-native-brand-yellow',
    '--color-native-surface',
    '--color-native-accent-ink',
    '--color-native-on-accent',
    '--color-native-warm',
    '--color-native-rental-accent',
    '--font-native-display',
    '--font-native-mono',
    '--space-native-md',
    '--text-native-body',
    '--native-control-height',
    '--radius-native-card',
    '--shadow-native-card'
  ]) {
    assert.match(tokens, new RegExp(token));
  }

  assert.match(tokens, /--color-native-canvas:oklch\(96\.5%/);
  assert.match(tokens, /--color-native-brand:#009297/);
  assert.match(tokens, /--color-native-brand-yellow:#EEBE0C/);
  assert.match(tokens, /editorial · macrostructure: Ecosystem Index · theme: CCPS Estate/);
});

test('网页与原生端统一使用带 CCPS 字样的品牌图标', async () => {
  const [index, webIcon, androidIcon, iosIcon] = await Promise.all([
    readFile(indexUrl, 'utf8'),
    readFile(webIconUrl),
    readFile(androidIconUrl),
    readFile(iosIconUrl)
  ]);

  assert.match(index, /<meta name="theme-color" content="#009297"/);
  assert.match(index, /href="\/ccps-favicon-32\.png"/);
  assert.match(index, /href="\/ccps-favicon-192\.png"/);
  assert.match(index, /href="\/ccps-apple-touch-icon-180\.png"/);
  assert.ok(webIcon.length > 500, '网页 favicon 必须为有效品牌图片');
  assert.ok(androidIcon.length > 1_000, 'Android 启动图标必须为有效品牌图片');
  assert.ok(iosIcon.length > 10_000, 'iOS 启动图标必须为有效品牌图片');
});
