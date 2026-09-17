import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, statSync } from 'node:fs';
import { parse, compileTemplate } from '@vue/compiler-sfc';
const read = path => readFileSync(new URL(path, import.meta.url), 'utf8');
const source = read('../src/components/owner-mobile/OwnerEditorialBanner.vue');
const { descriptor } = parse(source);
const component = new Function('cityImage', 'livingImage', 'architectureImage', descriptor.script.content.replace(/^import .*;$/gm, '').replace('export default', 'return'))('city.webp', 'living.webp', 'architecture.webp');
test('editorial imagery compiles, stays local and reserves image dimensions', () => {
  assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename: 'Banner.vue', id: 'banner' }).errors, []);
  assert.doesNotMatch(source, /https?:\/\//);
  for (const attribute of ['alt=""', ':width=', ':height=', 'decoding="async"', 'v-if="!failed"', '@error="failed = true"']) assert.ok(source.includes(attribute));
  assert.match(source, /aspect-ratio/);
  const app = read('../src/components/owner-mobile/OwnerMobileApp.vue');
  assert.match(app, /components: \{[^}]*OwnerEditorialBanner,/);
  assert.equal((app.match(/<OwnerEditorialBanner/g) || []).length, 3);
});
test('Kuala Lumpur only appears for MY; other countries use non-location imagery', () => {
  for (const [country, kind, expected] of [['MY','city',true],['TH','city',false],['MY','living',false],['SG','city',false]]) {
    const isCity = component.computed.isCity.call({ country, kind });
    assert.equal(isCity, expected);
    assert.equal(component.computed.imageUrl.call({ isCity }), expected ? 'city.webp' : 'living.webp');
  }
  const state = { failed: true }; component.watch.imageUrl.call(state); assert.equal(state.failed, false);
  assert.equal(component.computed.imageUrl.call({ kind: 'architecture', isCity: false }), 'architecture.webp');
});
test('WebP assets remain small and separate from real property photos', () => {
  let bytes = 0;
  for (const name of ['kuala-lumpur', 'living-room', 'architecture']) {
    const url = new URL(`../src/assets/owner/${name}.webp`, import.meta.url);
    const file = readFileSync(url);
    assert.equal(file.toString('ascii', 8, 12), 'WEBP');
    bytes += statSync(url).size;
  }
  assert.ok(bytes < 320 * 1024);
  const cover = read('../src/components/owner-mobile/OwnerPropertyCover.vue');
  assert.doesNotMatch(cover, /living-room|kuala-lumpur|OwnerEditorialBanner/);
  assert.match(cover, /fetchOwnerDocumentFile/);
  assert.doesNotMatch(descriptor.template.content, /<small|imagery\.illustration/);
  assert.doesNotMatch(read('../src/i18n/ownerApp.js'), /非房产实拍|非房產實拍|not a property photo/);
});
test('brand masthead preserves logo and empty illustrations keep meaningful guidance', () => {
  const masthead = read('../src/components/owner-mobile/OwnerBrandMasthead.vue');
  const empty = read('../src/components/owner-mobile/EmptyState.vue');
  for (const text of [masthead, empty]) {
    const sfc = parse(text).descriptor;
    assert.deepEqual(compileTemplate({ source: sfc.template.content, filename: 'Imagery.vue', id: 'imagery' }).errors, []);
    assert.doesNotMatch(text, /https?:\/\//);
  }
  assert.match(masthead, /import logo from '\.\.\/\.\.\/\.\.\/ccps-logo.png'/);
  assert.match(masthead, /height:auto; object-fit:contain/);
  assert.match(masthead, /alt="CCPS 家慶佳業"/);
  assert.match(empty, /owner-empty-art" aria-hidden="true"/);
  assert.match(empty, /\{\{ title \}\}[\s\S]*\{\{ text \}\}/);
  assert.match(empty, /:is="iconComponent"/);
  assert.match(empty, /\.is-compact \.owner-empty-art/);
  assert.doesNotMatch(empty, /animation:|@click|<button/);
});
