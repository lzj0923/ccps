import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const read = path => readFile(new URL(path, import.meta.url), 'utf8');

test('网页入口不加载 App 样式，原生入口按需加载 App 样式', async () => {
  const [main, nativeApp] = await Promise.all([
    read('../src/main.js'),
    read('../src/nativeApp.js')
  ]);

  assert.doesNotMatch(main, /import\s+['"]\.\/native-app\.css['"]/, '网页公共入口不得静态加载 App 样式');
  assert.doesNotMatch(main, /import\s+['"]\.\/admin-theme\.css['"]/, 'App 公共入口不得静态加载后台网页主题');
  assert.match(nativeApp, /await import\(['"]\.\/native-app\.css['"]\)/, '仅原生入口可以加载 App 样式');
  assert.match(main, /if \(!isNativeApp\) await import\(['"]\.\/admin-theme\.css['"]\)/, '仅网页入口可以加载后台网页主题');
  assert.match(main, /await setupNativeApp\(\)/, '挂载页面前必须完成对应端的样式加载');
});

test('业主 App 底部导航只在原生端渲染', async () => {
  const ownerSystem = await read('../src/systems/owner/OwnerSystem.vue');

  assert.match(ownerSystem, /<OwnerBottomNav\s+v-if="nativeMode"\s*\/>/);
  assert.match(ownerSystem, /import \{ isNativeApp \} from '\.\.\/\.\.\/nativeApp'/);
  assert.match(ownerSystem, /nativeMode:\s*isNativeApp/);
});

test('网页租约卡片只使用网页端样式变量', async () => {
  const tenancy = await read('../src/components/AdminTenancyWorkspace.vue');

  assert.match(tenancy, /--tenancy-web-color-paper:/, '网页租约组件必须拥有独立的网页端颜色变量');
  assert.match(tenancy, /--tenancy-web-control-height:/, '网页租约组件必须拥有独立的网页端控件变量');
  assert.doesNotMatch(tenancy, /var\(--(?:color|space|radius|font|text|dur|ease)-native-|var\(--native-/, '网页租约样式不得继续引用 App 变量');
});

test('App 样式始终限定在原生壳作用域内', async () => {
  const nativeCss = await read('../src/native-app.css');

  const unscopedOwnerSelector = nativeCss
    .split(/\r?\n/)
    .map(line => line.trim())
    .filter(line => line && !line.startsWith('/*') && /\.(?:owner-app-v2|owner-mobile-)/.test(line))
    .find(line => !line.includes('html.capacitor-native'));

  assert.equal(unscopedOwnerSelector, undefined, `发现未限定原生壳的 App 选择器：${unscopedOwnerSelector || ''}`);
});
