import assert from 'node:assert/strict';
import { readFileSync, readdirSync, mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';
import test from 'node:test';
import { createServer } from 'vite';

const read = path => readFileSync(new URL(path, import.meta.url), 'utf8');
const server = await createServer({ server: { middlewareMode: true } });
const { i18n, translateLegacyText } = await server.ssrLoadModule('/src/i18n/index.js');
const maintenance = (await server.ssrLoadModule('/src/components/AdminMaintenanceWorkspace.vue')).default;
const processComponent = (await server.ssrLoadModule('/src/components/AdminPropertyProcessWorkspace.vue')).default;
const mandate = (await server.ssrLoadModule('/src/components/AdminRentalMandateWorkspace.vue')).default;
await server.close();

test('literal translation keys used by source files exist in every supported locale', () => {
  const walk = dir => readdirSync(dir, { withFileTypes: true }).flatMap(entry => entry.isDirectory() ? walk(`${dir}/${entry.name}`) : /\.(vue|js)$/.test(entry.name) ? [`${dir}/${entry.name}`] : []);
  const root = fileURLToPath(new URL('../src', import.meta.url));
  const namespaces = new Set(Object.keys(i18n.global.getLocaleMessage('en')));
  const missing = [];
  for (const file of walk(root).filter(file => !file.includes('/i18n/'))) {
    // Exclude HTML attribute boundaries (v-if="properties.length"); these are expressions, not translation literals.
    for (const match of readFileSync(file, 'utf8').matchAll(/(?<!=)['"]([a-zA-Z]+\.[\w.]+)['"]/g)) {
      const key = match[1];
      if (key.endsWith('.') || !namespaces.has(key.split('.')[0])) continue;
      for (const locale of ['zh-CN', 'zh-TW', 'en']) if (!i18n.global.te(key, locale)) missing.push(`${file}: ${locale} ${key}`);
    }
  }
  assert.deepEqual([...new Set(missing)], []);
});

test('all mapped API errors resolve in English instead of falling through to Chinese', () => {
  i18n.global.locale.value = 'en';
  const source = read('../src/services/propertyApi.js').split('const API_ERROR_MESSAGES = new Map([')[1].split(']);')[0];
  const untranslated = [...source.matchAll(/\['([^']*)', '([^']*)'\]/g)]
    .filter(match => /[\u3400-\u9fff]/.test(translateLegacyText(match[2]))).map(match => match[1]);
  assert.deepEqual(untranslated, []);
});

test('operational codes translate without modifying customer-provided values', () => {
  for (const [locale, active, assigned] of [['zh-CN', '启用中', '已指派'], ['zh-TW', '啟用中', '已指派'], ['en', 'Active', 'Assigned']]) {
    i18n.global.locale.value = locale;
    const ctx = { $t: i18n.global.t, $te: i18n.global.te };
    assert.equal(processComponent.methods.mandateStatusLabel.call(ctx, 'active'), active);
    assert.equal(processComponent.methods.workOrderStatusLabel.call(ctx, 'assigned'), assigned);
    assert.equal(processComponent.methods.workOrderStatusLabel.call(ctx, null), '—');
    assert.equal(mandate.methods.documentTypeLabel.call(ctx, 'custom_customer_file'), 'custom_customer_file');
    assert.notEqual(mandate.methods.documentTypeLabel.call(ctx, 'handover_photo'), 'handover_photo');
    for (const value of ['CCPS', 'LEASE-20260903-F1AA5735B791F', 'WANG GE']) assert.equal(translateLegacyText(value), value);
  }
});

test('payment warning colour is independent of translated status text', () => {
  for (const locale of ['zh-CN', 'zh-TW', 'en']) {
    i18n.global.locale.value = locale;
    assert.equal(maintenance.methods.statusClass.call({}, 'unpaid'), 'red');
    assert.equal(maintenance.methods.statusClass.call({}, 'completed'), 'green');
  }
});

const require = createRequire(import.meta.url);
let chromium;
try { ({ chromium } = require(process.env.CCPS_PLAYWRIGHT_MODULE || 'playwright')); } catch { /* Optional local browser. */ }
test('monthly ledger controls remain accessible on short and narrow viewports in three languages', { skip: !chromium }, async () => {
  const source = read('../src/components/AdminMaintenanceWorkspace.vue');
  const template = source.match(/<dialog ref="monthlyCashflowDialog"[\s\S]*?<\/dialog>/)[0];
  const css = [...source.matchAll(/<style[^>]*>([\s\S]*?)<\/style>/g)].map(m => m[1]).join('\n') + read('../styles.css') + read('../tokens.css') + read('../src/admin-theme.css');
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  try {
    for (const locale of ['zh-CN', 'zh-TW', 'en']) for (const width of [320, 375, 414, 768, 1280]) {
      const page = await browser.newPage({ viewport: { width, height: 600 } });
      await page.setContent(`<html lang="${locale}"><style>${css}</style><div id="app"></div></html>`);
      await page.addScriptTag({ content: read('../node_modules/vue/dist/vue.global.prod.js') });
      await page.evaluate(({ template, messages }) => {
        const units = Array.from({ length: 20 }, (_, i) => ({ unitId: i, projectName: 'CONLAY RESIDENCES', unitNo: 'A-01-01', ownerName: 'WANG GE' }));
        Vue.createApp({ template: '<div class="admin-shell">' + template + '</div>',
          data: () => ({ monthlyUnitSearch: '', monthlyUnitsLoading: false, monthlyFilteredUnits: units, monthlySelectedUnitId: 0, monthlySelectedUnit: units[0], monthlyMonth: '2026-09', monthlyCashflowError: '', monthlyCashflowLoading: false, monthlyIncome: 12345678.90, monthlyExpense: 100, monthlyNet: 12345578.90, monthlyCashflowRows: [], monthlySelectedCashflow: null }),
          methods: { $t: (key, params = {}) => String(key.split('.').reduce((v, part) => v?.[part], messages) || key).replace(/\{(\w+)\}/g, (_, name) => params[name] ?? `{${name}}`), money: value => Number(value).toLocaleString('en-MY', { minimumFractionDigits: 2 }), resetMonthlyCashflow() {}, closeMonthlyCashflow() { this.$refs.monthlyCashflowDialog.close(); }, selectMonthlyUnit() {} },
          mounted() { this.$refs.monthlyCashflowDialog.showModal(); }
        }).mount('#app');
      }, { template, messages: i18n.global.getLocaleMessage(locale) });
      const bounds = await page.evaluate(() => {
        const dialog = document.querySelector('dialog'), search = dialog.querySelector('input[type=search]'), close = dialog.querySelector('.icon-close');
        const r = dialog.getBoundingClientRect(), cr = close.getBoundingClientRect();
        return { left: r.left, right: r.right, bottom: r.bottom, height: r.height, scrollHeight: dialog.scrollHeight, clientHeight: dialog.clientHeight, searchHeight: search.getBoundingClientRect().height, closeWidth: cr.width, closeHeight: cr.height };
      });
      assert.ok(bounds.left >= 0 && bounds.right <= width && bounds.bottom <= 600, JSON.stringify({ locale, width, bounds }));
      assert.ok(bounds.searchHeight >= 44 && bounds.closeWidth >= 44 && bounds.closeHeight >= 44, JSON.stringify({ locale, width, bounds }));
      assert.equal(await page.locator('.monthly-ledger-panel').evaluate(el => el.scrollWidth <= el.clientWidth + 1), true, `Ledger overflow: ${locale} ${width}`);
      if (locale === 'en' && [320, 768, 1280].includes(width)) {
        const output = fileURLToPath(new URL('../../project-resources/generated/tmp/ui-locale-refinement/', import.meta.url));
        mkdirSync(output, { recursive: true });
        await page.screenshot({ path: `${output}/monthly-en-${width}.png` });
      }
      await page.locator('input[type=search]').fill('CONLAY');
      await page.locator('.monthly-summary-grid').scrollIntoViewIfNeeded();
      const summaryVisible = await page.locator('.monthly-summary-grid').evaluate(el => {
        const r = el.getBoundingClientRect(), body = document.querySelector('.monthly-cashflow-body').getBoundingClientRect();
        return r.top >= body.top - 1 && r.top < body.bottom;
      });
      assert.equal(summaryVisible, true, `Summary must be reachable by scrolling: ${locale} ${width}`);
      await page.locator('.icon-close').click();
      assert.equal(await page.locator('dialog').evaluate(el => el.open), false);
      await page.close();
    }
  } finally { await browser.close(); }
});
