import assert from 'node:assert/strict';
import { readFileSync, mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';
import { compileStyle } from '@vue/compiler-sfc';
import test from 'node:test';

const require = createRequire(import.meta.url);
const { chromium } = require(process.env.CCPS_PLAYWRIGHT_MODULE || 'playwright');
const read = path => readFileSync(new URL(path, import.meta.url), 'utf8');
const scope = 'data-v-phone-test';
const output = fileURLToPath(new URL('../../project-resources/generated/tmp/phone-controls-20260907/', import.meta.url));
const variants = [
  ['tenant', 'AdminTenantDirectoryWorkspace', /<label class="phone-field">[\s\S]*?<\/label>/, 'tenant-dialog', 'tenant-form'],
  ['owner', 'AdminOwnersWorkspace', /<label class="wide owner-mobile-field">[\s\S]*?<\/label>/, 'modal admin-owner-dialog', 'form-grid'],
  ['property', 'AdminPropertyDetailWorkspace', /<label class="wide owner-phone-field">[\s\S]*?<\/label>/, 'property-detail-workspace', 'property-basic-form'],
  ['process', 'AdminPropertyProcessWorkspace', /<label class="process-setup-wide">\{\{ \$t\('legacy.t_9416d09a3314'\)[\s\S]*?<\/label>/, 'process-setup-dialog', 'process-setup-grid'],
];
for (const [name, file, pattern, outer, inner] of variants) {
  test(`${name}: country and phone have separate borders, usable widths and no overlap`, async () => {
    const source = read(`../src/components/${file}.vue`);
    const template = source.match(pattern)?.[0];
    assert.ok(template, `${file} phone template`);
    const styles = [...source.matchAll(/<style([^>]*)>([\s\S]*?)<\/style>/g)].map(match => compileStyle({ source: match[2], id: scope, scoped: match[1].includes('scoped') }).code).join('\n');
    const browser = await chromium.launch({ channel: 'chrome', headless: true });
    const failures = [];
    try {
      for (const width of [320, 390, 768, 1280]) for (const label of ['马来西亚', '馬來西亞', 'Malaysia']) {
        const page = await browser.newPage({ viewport: { width, height: 500 } });
        await page.setContent(`<style>${styles}\n${read('../styles.css')}\n${read('../tokens.css')}\n${read('../src/admin-theme.css')}</style><div id="app"></div>`);
        await page.addScriptTag({ content: read('../node_modules/vue/dist/vue.global.prod.js') });
        await page.evaluate(({ template, outer, inner, label, scope }) => {
          const phone = { code: 'MY', dialCode: '+60', example: '1111223344' };
          const field = () => ({ mobileCountry: 'MY', mobileNational: '1111223344' });
          const app = window.Vue.createApp({
            template: `<section class="admin-shell"><section class="${outer}" style="position:static;transform:none;width:100%;max-width:640px;margin:24px auto;padding:16px;box-sizing:border-box"><section class="${inner}">${template}</section></section></section>`,
            data: () => ({ form: { phoneCountry: 'MY', phoneNational: '1111223344' }, ownerForm: field(), basicForm: field(), setupOwnerForm: field(), basicEditing: true, phoneCountries: [phone, {code:'SG',dialCode:'+65'}], selectedPhoneCountry: phone, selectedOwnerPhoneCountry: phone, selectedBasicOwnerPhoneCountry: phone, selectedSetupOwnerPhoneCountry: phone, phoneError: '', ownerPhoneError: '', setupOwnerPhoneError: '' }),
            methods: { $t: () => '电话', $lt: x => x, $regionName: () => label, validatePhone() {}, validateOwnerPhone() {}, validateBasicOwnerPhone() {}, validateSetupOwnerPhone() {} },
          });
          app.mount('#app');
          document.querySelectorAll('#app *').forEach(element => element.setAttribute(scope, ''));
        }, { template, outer, inner, label, scope });
        const geometry = await page.evaluate(() => {
          const entry = document.querySelector('.phone-entry,.owner-phone-entry,.process-phone-entry');
          const select = entry.querySelector('select'), input = entry.querySelector('input');
          const a = select.getBoundingClientRect(), b = input.getBoundingClientRect(), p = entry.getBoundingClientRect();
          const stacked = b.top >= a.bottom;
          return { gap: stacked ? b.top - a.bottom : b.left - a.right, selectWidth:a.width, inputWidth:b.width, overflow:Math.max(a.right,b.right)-p.right, display:getComputedStyle(entry).display, cssGap:getComputedStyle(entry).gap, selectHit: document.elementFromPoint(a.left+10,a.top+a.height/2)===select, inputHit: document.elementFromPoint(b.left+10,b.top+b.height/2)===input };
        });
        mkdirSync(output, {recursive:true});
        if (label === '马来西亚') await page.screenshot({path:`${output}/${process.env.CCPS_VISUAL_PHASE || 'current'}-${name}-${width}.png`});
        if (geometry.gap < 10 || geometry.overflow > 1 || geometry.inputWidth < 120 || !geometry.selectHit || !geometry.inputHit) failures.push({width,label,...geometry});
        await page.locator('input').fill('12345678');
        assert.equal(await page.locator('input').inputValue(),'12345678');
        await page.locator('select').selectOption('SG');
        assert.equal(await page.locator('select').inputValue(),'SG');
        await page.close();
      }
    } finally { await browser.close(); }
    assert.deepEqual(failures, []);
  });
}
