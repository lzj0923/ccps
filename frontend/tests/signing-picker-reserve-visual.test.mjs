import assert from 'node:assert/strict';
import { readFileSync, mkdirSync } from 'node:fs';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const require = createRequire(import.meta.url);
let chromium;
try { ({ chromium } = require(process.env.CCPS_PLAYWRIGHT_MODULE || 'playwright')); } catch { /* Optional browser test runtime. */ }
const read = path => readFileSync(new URL(path, import.meta.url), 'utf8');
const signing = read('../src/components/AdminRentalSigningWorkspace.vue');
const reserve = read('../src/components/AdminReserveWorkspace.vue');
const componentCss = source => [...source.matchAll(/<style[^>]*>([\s\S]*?)<\/style>/g)].map(match => match[1]).join('\n');
const globalCss = read('../styles.css') + read('../tokens.css') + read('../src/admin-theme.css');
const vue = read('../node_modules/vue/dist/vue.global.prod.js');
const output = fileURLToPath(new URL('../../project-resources/generated/tmp/signing-reserve-visual/', import.meta.url));

async function mount(browser, source, template, data, width, height) {
  const page = await browser.newPage({ viewport: { width, height } });
  await page.setContent('<style>' + componentCss(source) + globalCss + '</style><div id="app"></div>');
  await page.addScriptTag({ content: vue });
  await page.evaluate(({ template, data }) => {
    window.Vue.createApp({
      template: '<div class="admin-shell"><main class="main">' + template + '</main></div>',
      data: () => data,
      methods: {
        $t: key => ({
          'rentalFiles.selectProperty': '选择房产', 'rentalFiles.propertySearch': '搜索建案、单位或业主',
          'legacy.t_cf545c9c1bf9': '建案', 'legacy.t_4502f4a3a4aa': '全部建案',
          'legacy.t_5e7b60c626a4': 'RM'
        })[key] || key,
        money: value => Number(value).toFixed(2), selectProperty() {}
      }
    }).mount('#app');
  }, { template, data });
  return page;
}

test('signing search stays above scrollable property rows at desktop and short heights', { skip: !chromium }, async () => {
  const browser = await chromium.launch({ channel: process.env.CCPS_BROWSER_CHANNEL || 'chrome', headless: true });
  try {
    const picker = signing.match(/<aside class="rental-signing-property-picker">[\s\S]*?<\/aside>/)[0];
    const properties = Array.from({ length: 30 }, (_, index) => ({ rowKey: String(index), projectName: 'CONLAY', unitNo: String(index + 1), ownerName: 'WANG GE' }));
    for (const [width, height] of [[1517, 745], [1280, 600], [1920, 1080], [390, 844]]) {
      const page = await mount(browser, signing, '<section class="rental-signing-page"><div class="rental-signing-layout">' + picker + '<main class="rental-signing-main">Attachments</main></div></section>', {
        properties, filteredProperties: properties, projectOptions: ['CONLAY'], selectedProjectName: '', propertySearch: '',
        selectedPropertyKey: '0', loading: false, loadError: ''
      }, width, height);
      const geometry = await page.evaluate(() => {
        const search = document.querySelector('input[type=search]');
        const list = document.querySelector('.rental-signing-property-list');
        const rect = search.getBoundingClientRect();
        return { bottom: rect.bottom, height: rect.height, listTop: list.getBoundingClientRect().top, firstTop: list.firstElementChild.getBoundingClientRect().top,
          hit: document.elementFromPoint(rect.left + 15, rect.top + rect.height / 2) === search };
      });
      mkdirSync(output, { recursive: true });
      await page.screenshot({ path: output + '/' + (process.env.CCPS_VISUAL_PHASE || 'current') + '-picker-' + width + 'x' + height + '.png' });
      assert.ok(geometry.listTop >= geometry.bottom + 5, JSON.stringify({ width, height, ...geometry }));
      assert.ok(geometry.firstTop >= geometry.bottom, 'First property must not cover search');
      assert.ok(geometry.height >= 32 && geometry.hit, 'Search must remain visible and clickable');
      await page.locator('input[type=search]').fill('CONLAY');
      await page.locator('.rental-signing-property-list').evaluate(element => { element.scrollTop = 250; });
      assert.equal(await page.locator('input[type=search]').inputValue(), 'CONLAY');
      await page.close();
    }
  } finally { await browser.close(); }
});

test('reserve warning amounts have transparent backgrounds while warning buttons retain theirs', { skip: !chromium }, async () => {
  const browser = await chromium.launch({ channel: process.env.CCPS_BROWSER_CHANNEL || 'chrome', headless: true });
  try {
    const balance = reserve.match(/<td><b :class="[^"]*account\.balanceStatus[^"]*">[\s\S]*?<\/td>/)[0];
    const shortage = reserve.match(/<td><b :class="[^"]*account\.shortageAmount[^"]*">[\s\S]*?<\/td>/)[0];
    const page = await mount(browser, reserve, '<section class="reserve-layout"><div class="panel reserve-list-panel"><table><tbody><tr>' + balance + shortage + '</tr></tbody></table><button class="danger-button">Delete</button></div></section>', {
      account: { balanceStatus: 'low', currentBalance: -200, shortageAmount: 1200 }
    }, 1280, 600);
    const values = await page.locator('td b, button.danger-button').evaluateAll(elements => elements.map(element => {
      const style = getComputedStyle(element); return { background: style.backgroundColor, color: style.color, text: element.textContent };
    }));
    mkdirSync(output, { recursive: true });
    await page.screenshot({ path: output + '/' + (process.env.CCPS_VISUAL_PHASE || 'current') + '-reserve.png' });
    assert.equal(values[0].background, 'rgba(0, 0, 0, 0)', JSON.stringify(values));
    assert.equal(values[1].background, 'rgba(0, 0, 0, 0)');
    assert.equal(values[0].color, values[2].color, 'Amounts keep the warning text color');
    assert.notEqual(values[2].background, 'rgba(0, 0, 0, 0)', 'Do not remove danger button backgrounds globally');
  } finally { await browser.close(); }
});
