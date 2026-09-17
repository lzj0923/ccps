import assert from 'node:assert/strict';
import test from 'node:test';
import { readFileSync, mkdirSync } from 'node:fs';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';
import { parse, compileStyle } from '@vue/compiler-sfc';
const require = createRequire(import.meta.url);
const { chromium } = require(process.env.CCPS_PLAYWRIGHT_MODULE || 'playwright');
const read = path => readFileSync(new URL(path, import.meta.url), 'utf8');
const parent = parse(read('../src/components/AdminRentalSigningWorkspace.vue')).descriptor;
const child = parse(read('../src/components/AdminOwnerSignatureDispatch.vue')).descriptor;
const article = parent.template.content.match(/<article v-for="link in generatedSigningLinks"[\s\S]*?<\/article>/)[0];
const css = (sfc, id) => sfc.styles.map(style => compileStyle({ source: style.content, id, scoped: style.scoped }).code).join('\n');

test('App dispatch follows link actions, wraps on mobile, and displays recoverable errors beside the control', async () => {
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  try {
    for (const width of [1333, 768, 390]) {
      const page = await browser.newPage({ viewport: { width, height: 800 } });
      await page.setContent('<style>' + css(parent, 'data-v-parent') + css(child, 'data-v-child') + read('../styles.css') + read('../tokens.css') + read('../src/admin-theme.css') + '</style><div id="app"></div>');
      await page.addScriptTag({ content: read('../node_modules/vue/dist/vue.global.prod.js') });
      await page.evaluate(({ article, script, template }) => {
        window.requests = [];
        const child = new Function('fetchAdminSignatureOwnerRecipients', 'dispatchAdminOwnerSignature', script.replace(/^import .*;$/gm, '').replace('export default', 'return'))(
          async id => { window.requests.push(id); throw new Error('服务请求失败（HTTP 404）'); }, async () => { throw new Error('Must not send in layout test'); });
        child.template = template; child.__scopeId = 'data-v-child';
        const app = Vue.createApp({
          __scopeId: 'data-v-parent', components: { AdminOwnerSignatureDispatch: child },
          template: '<div class="admin-shell" style="display:block"><section class="rental-signing-panel"><div class="rental-signing-link-results">' + article + '</div></section></div>',
          data: () => ({ generatedSigningLinks: [{ requestId: 123, signerRole: 'owner', signerName: '测试业主', signingUrl: 'https://example.invalid/sign/test', deliveryEmail: '' }], signingEmailBusyId: null, signingEmailSentIds: [] }),
          methods: { $lt: text => text, signerRoleLabel: () => '屋主', whatsAppSigningUrl: () => '#', copySigningLink() {}, sendSigningEmail() {} }
        });
        app.config.globalProperties.$t = key => ({
          'ownerApp.signing.sendApp': '发送到业主 App', 'ownerApp.signing.loading': '加载中',
          'rentalFiles.copyLink': '复制链接', 'rentalFiles.shareByWhatsApp': 'WhatsApp 分享',
          'rentalFiles.openSigningLink': '打开链接', 'rentalFiles.signingLink': '签署链接',
          'rentalFiles.emailOnlyWhenSending': '邮箱在邮件发送时填写', 'rentalFiles.deliveryEmail': '邮件收件地址',
          'rentalFiles.deliveryEmailHint': '仅发送邮件时需要填写', 'rentalFiles.sendByEmail': '发送邮件'
        })[key] || key;
        app.mount('#app');
      }, { article, script: child.script.content, template: child.template.content });
      const button = page.locator('.owner-signature-dispatch button');
      const bounds = await button.evaluate(button => {
        const rect = button.getBoundingClientRect(), open = document.querySelector('.rental-signing-link-actions a:last-of-type').getBoundingClientRect();
        return { grouped: !!button.closest('.rental-signing-link-actions'), x: rect.x, y: rect.y, right: rect.right, openRight: open.right, openY: open.y };
      });
      assert.ok(bounds.grouped, 'App dispatch must be in the lower link action group');
      if (width >= 768) { assert.ok(Math.abs(bounds.y - bounds.openY) < 2); assert.ok(bounds.x >= bounds.openRight + 7); }
      assert.ok(bounds.right <= width, 'No horizontal overflow');
      await button.click();
      await page.locator('.owner-signature-dispatch [role="alert"]').waitFor();
      assert.equal(await button.isEnabled(), true, 'Retry remains available after failed lookup');
      assert.ok(await page.locator('.rental-signing-link-actions a').first().evaluate(element => element.getBoundingClientRect().height < 60), 'An error must not stretch adjacent link buttons');
      assert.deepEqual(await page.evaluate(() => window.requests), [123]);
      const output = fileURLToPath(new URL('../../project-resources/generated/tmp/owner-signature-dispatch-layout/', import.meta.url));
      mkdirSync(output, { recursive: true });
      await page.screenshot({ path: `${output}/${width}.png`, fullPage: true });
      await page.close();
    }
  } finally { await browser.close(); }
});
