import assert from 'node:assert/strict';
import { readFileSync, mkdirSync } from 'node:fs';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';
import test from 'node:test';
import { parse, compileStyle } from '@vue/compiler-sfc';

const require = createRequire(import.meta.url);
const { chromium } = require(process.env.CCPS_PLAYWRIGHT_MODULE || 'playwright');
const read = path => readFileSync(new URL(path, import.meta.url), 'utf8');
const source = read('../src/components/AdminPropertyDetailWorkspace.vue');
const { descriptor } = parse(source);
const scope = 'data-v-checklist-test';
const css = descriptor.styles.map(style => compileStyle({ source: style.content, id: scope, scoped: style.scoped }).code).join('\n');
const globalCss = read('../styles.css') + read('../tokens.css') + read('../src/admin-theme.css');
const table = source.match(/<table class="handover-checklist-table">[\s\S]*?<\/table>/)[0];
const output = fileURLToPath(new URL('../../project-resources/generated/tmp/checklist-actions/', import.meta.url));

test('checklist action labels fit their buttons and remain separately clickable', async () => {
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  try {
    for (const width of [1320, 960, 760, 390]) {
      for (const labels of [['编辑', '删除项目'], ['編輯', '刪除項目'], ['Edit', 'Delete item']]) {
        const page = await browser.newPage({ viewport: { width, height: 600 } });
        await page.setContent('<style>' + css + globalCss + '</style><div id="app"></div>');
        await page.addScriptTag({ content: read('../node_modules/vue/dist/vue.global.prod.js') });
        await page.evaluate(({ table, labels, scope }) => {
          const item = { id: 1, category: '主浴室 Master Bathroom', itemName: '浴缸 / Bathtub', defaultQuantity: 1, enabled: true };
          window.clicks = [];
          const component = {
            __scopeId: scope,
            template: '<div class="handover-checklist-table-wrap">' + table + '</div>',
            data: () => ({ pagedHandoverChecklist: [item], filteredHandoverChecklist: [item], handoverChecklistTogglingIds: [] }),
            methods: {
              $t: key => ({ 'propertyDetail.checklistEdit': labels[0], 'legacy.t_a48f5d05a68f': labels[1],
                'propertyDetail.checklistEnabled': '启用', 'propertyDetail.checklistCategory': '分类',
                'propertyDetail.checklistItemName': '物品名称', 'propertyDetail.checklistDefaultQuantity': '默认数量',
                'propertyDetail.checklistStatus': '状态', 'propertyDetail.checklistActions': '操作' })[key] || key,
              openHandoverChecklistForm: item => window.clicks.push(['edit', item.id]),
              removeHandoverChecklistItem: item => window.clicks.push(['delete', item.id])
            }
          };
          window.Vue.createApp({ components: { Fixture: component }, template: '<div class="admin-shell" style="display:block"><Fixture /></div>' }).mount('#app');
        }, { table, labels, scope });
        const buttons = page.locator('tbody td:last-child button');
        await buttons.first().scrollIntoViewIfNeeded();
        const geometry = await buttons.evaluateAll(elements => elements.map(button => {
          const rect = button.getBoundingClientRect();
          const range = document.createRange(); range.selectNodeContents(button);
          const label = range.getBoundingClientRect();
          const cell = button.closest('td').getBoundingClientRect();
          return { x: rect.x, right: rect.right, y: rect.y, bottom: rect.bottom, width: rect.width,
            labelLeft: label.left, labelRight: label.right, cellRight: cell.right };
        }));
        mkdirSync(output, { recursive: true });
        await page.screenshot({ path: `${output}/${width}-${labels[0]}.png` });
        for (const rect of geometry) {
          assert.ok(rect.labelLeft >= rect.x && rect.labelRight <= rect.right + 1, JSON.stringify({ width, labels, geometry }));
          assert.ok(rect.right <= rect.cellRight, 'Buttons stay within the action cell');
        }
        assert.ok(geometry[1].x >= geometry[0].right + 4 || geometry[1].y >= geometry[0].bottom + 4, 'Buttons have a visible gap');
        await buttons.first().click(); await buttons.last().click();
        assert.deepEqual(await page.evaluate(() => window.clicks), [['edit', 1], ['delete', 1]]);
        await page.close();
      }
    }
  } finally { await browser.close(); }
});
