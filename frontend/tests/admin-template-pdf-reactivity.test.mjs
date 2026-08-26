import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { reactive, markRaw } from 'vue';
import { getDocument } from 'pdfjs-dist/legacy/build/pdf.mjs';

const componentSource = readFileSync(
  new URL('../src/components/AdminRentalSigningWorkspace.vue', import.meta.url),
  'utf8',
);

test('keeps the PDF.js document outside Vue reactive proxies', async () => {
  assert.match(componentSource, /import \{ markRaw \} from 'vue';/);
  assert.match(componentSource, /this\.templatePdf = markRaw\(await getDocument\([\s\S]*?\)\.promise\);/);

  const bytes = new Uint8Array(readFileSync(
    new URL('../../backend/src/main/resources/contract-templates/ccps-pma-v1.pdf', import.meta.url),
  ));
  const pdf = markRaw(await getDocument({ data: bytes }).promise);
  const state = reactive({ pdf });

  try {
    const page = await state.pdf.getPage(1);
    assert.equal(page.pageNumber, 1);
  } finally {
    await pdf.destroy();
  }
});

test('allows a template field to move to any page in the active PDF', () => {
  assert.match(componentSource, /templateFieldPage/);
  assert.match(componentSource, /@change="changeTemplateFieldPage"/);
  assert.match(componentSource, /async changeTemplateFieldPage\(event\)/);
  assert.match(componentSource, /this\.templateSelectedField\.page = targetPage/);
  assert.match(componentSource, /await this\.changeTemplatePage\(targetPage, false\)/);
});
