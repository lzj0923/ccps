import assert from 'node:assert/strict';
import { readFile, stat } from 'node:fs/promises';
import test from 'node:test';
import { decodeMimeFilename } from '../src/utils/mimeFilename.js';

const read = path => readFile(new URL(path, import.meta.url), 'utf8');

test('合同凭证入口优先选择有文件的房产并允许手动切换', async () => {
  const portal = await read('../src/components/owner-mobile/OwnerMobileApp.vue');

  assert.match(portal, /owner-property-switch/);
  assert.match(portal, /ownerApp\.property/);
  assert.match(portal, /@change="switchDetailProperty"/);
  assert.match(portal, /findPropertyWithDocuments/);
  assert.match(portal, /documentMatchesProperty/);
});

test('业主 App 在原生页面内预览 PDF 和图片', async () => {
  const [portal, preview] = await Promise.all([
    read('../src/components/owner-mobile/OwnerMobileApp.vue'),
    read('../src/components/owner-mobile/OwnerDocumentPreview.vue')
  ]);

  assert.match(portal, /<OwnerDocumentPreview/);
  assert.doesNotMatch(portal, /window\.open\(url/);
  assert.match(preview, /pdfjs-dist/);
  assert.match(preview, /markRaw/);
  assert.match(preview, /cMapUrl/);
  assert.match(preview, /cMapPacked/);
  assert.match(preview, /standardFontDataUrl/);
  assert.match(preview, /<canvas/);
  assert.match(preview, /<img/);
  assert.match(preview, /fetchOwnerDocumentFile/);
  assert.match(preview, /showModal/);
});

test('MIME 编码的中文附件名会还原为可读文件名', () => {
  assert.equal(
    decodeMimeFilename('=?UTF-8?Q?=E5=90=95=E5=BF=97=E6=9D=B0-=E5=90=88=E5=90=8C.pdf?='),
    '吕志杰-合同.pdf'
  );
  assert.equal(
    decodeMimeFilename('=?UTF-8?Q?=E5=90=95=E5=BF=97=E6=9D=B0-=E5=90=88=E5=90=8C.pdf'),
    '吕志杰-合同.pdf'
  );
  assert.equal(decodeMimeFilename('tenancy-agreement.pdf'), 'tenancy-agreement.pdf');
});

test('长文件名不会撑宽手机预览窗口', async () => {
  const preview = await read('../src/components/owner-mobile/OwnerDocumentPreview.vue');
  assert.match(preview, /max-width:calc\(100vw - 1rem\)/);
  assert.match(preview, /grid-template-columns:minmax\(0,1fr\)/);
  assert.match(preview, /flex:0 0 2\.75rem/);
  assert.match(preview, /@media\(max-width:30rem\).*max-width:100vw/);
});

test('App 构建包含中文 PDF 所需字形资源', async () => {
  const [cmap, font] = await Promise.all([
    stat(new URL('../public/pdfjs/cmaps/Adobe-GB1-UCS2.bcmap', import.meta.url)),
    stat(new URL('../public/pdfjs/standard_fonts/LiberationSans-Regular.ttf', import.meta.url))
  ]);
  assert.ok(cmap.size > 0);
  assert.ok(font.size > 0);
});
