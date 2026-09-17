import assert from 'node:assert/strict';
import test from 'node:test';
import { readFileSync } from 'node:fs';
import { parse, compileTemplate } from '@vue/compiler-sfc';
import { compile } from '@vue/compiler-dom';
import * as Vue from 'vue';
import { renderToString } from '@vue/server-renderer';
import { hasOwnerPaymentRecords } from '../src/utils/ownerPortfolio.js';
import { formatMonth } from '../src/utils/dateFormat.js';

const read = file => readFileSync(new URL(file, import.meta.url), 'utf8');
test('header currency badge separates currency context from profile identity', () => {
  const app = read('../src/components/owner-mobile/OwnerMobileApp.vue');
  const header = app.slice(0, app.indexOf('</header>'));
  assert.match(header, /owner-currency-badge[\s\S]*ownerApp.currency[\s\S]*\{\{ currency \}\}/);
  assert.doesNotMatch(header, /ownerInitial|owner-app-avatar/);
  assert.match(header, /v-model="countryFilter"/);
  assert.match(app, /owner-profile-card[\s\S]*\{\{ ownerInitial \}\}/);
  const css = read('../src/components/owner-mobile/owner-mobile-visual.css');
  assert.match(css, /\.owner-currency-badge\s*\{[^}]*white-space:nowrap/);
  assert.match(css, /\.owner-currency-badge small\s*\{[^}]*font-size:\.75rem/);
});
test('visual refresh preserves property actions, archive query and all mobile modules', () => {
  const app = read('../src/components/owner-mobile/OwnerMobileApp.vue');
  const card = read('../src/components/owner-mobile/OwnerPropertyCards.vue');
  const cover = read('../src/components/owner-mobile/OwnerPropertyCover.vue');
  for (const module of ['myProperties', 'ownerRentalHub', 'ownerProjects', 'ownerNotice']) assert.ok(app.includes(`currentId === '${module}'`));
  for (const target of ['property', 'payment', 'cashflow', 'files']) assert.ok(card.includes(`$emit('open', property, '${target}')`));
  assert.match(cover, /fetchOwnerDocumentFile\(id\)/);
  assert.match(cover, /sequence === this.sequence/);
  assert.match(cover, /URL.revokeObjectURL/);
  assert.match(cover, /ownerApp.coverMissing/);
  assert.doesNotMatch(cover, /https?:\/\//);
  assert.match(app, /owner-portfolio-board[\s\S]*:style="assetRingStyle"/);
  assert.match(app, /owner-home-dashboard/);
  assert.match(app, /owner-rental-board/);
  assert.match(app, /owner-signout[\s\S]*page.handleLogout\(\)/);
  for (const source of [app, card, cover]) {
    const {descriptor} = parse(source);
    assert.deepEqual(compileTemplate({id:'visual-test',source:descriptor.template.content,filename:'Visual.vue'}).errors, []);
  }
});
test('new visual CSS remains native-scoped, uses brand tokens and width-safe image tracks', () => {
  const css = read('../src/components/owner-mobile/owner-mobile-visual.css');
  assert.match(css, /aspect-ratio:16 \/ 9/);
  assert.match(css, /grid-template-columns:var\(--owner-cover-size\) minmax\(0,1fr\) auto/);
  assert.doesNotMatch(css, /https?:\/\/|#[\da-f]{3,8}\b|100vw|transition:all/);
  for (const match of css.replace(/\/\*[\s\S]*?\*\//g, '').matchAll(/([^{}]+)\{[^{}]*\}/g)) {
    assert.ok(match[1].trim().startsWith('html.capacitor-native'), match[1]);
  }
});

test('visual surfaces stay neutral instead of decorative blue panels', () => {
  const css = read('../src/components/owner-mobile/owner-mobile-visual.css');
  assert.match(css, /\.owner-stat-hero\s*\{[^}]*background:var\(--color-native-paper\)/);
  assert.match(css, /--color-native-paper:var\(--color-owner-paper\)/);
  assert.doesNotMatch(css, /background:var\(--color-native-accent-faint\)/);
  assert.match(css, /\.owner-quick-grid button > svg\s*\{[^}]*background:transparent/);
});

test('redesigned property cards render real amounts, stages and payment availability', async () => {
  const { descriptor } = parse(read('../src/components/owner-mobile/OwnerPropertyCards.vue'));
  const script = descriptor.script.content.replace(/^import .*;$/gm, '').replace('export default', 'return');
  const icon = { render: () => Vue.h('svg', { 'aria-hidden': 'true' }) };
  const cover = { props: ['property'], render() { return Vue.h('div', this.property.coverDocumentId ? 'ARCHIVED_COVER' : 'NO_PHOTO'); } };
  const component = new Function('formatMonth', 'Building2', 'ChartNoAxesColumnIncreasing', 'CircleDollarSign', 'Files', 'Landmark', 'OwnerPropertyCover', 'hasOwnerPaymentRecords', script)(formatMonth, icon, icon, icon, icon, icon, cover, hasOwnerPaymentRecords);
  component.render = new Function('Vue', compile(descriptor.template.content, { mode: 'function', prefixIdentifiers: true }).code)(Vue);
  const property = { ownerUnitId: 'test-unit', projectName: 'Test Residence', city: 'Test City', unitNo: 'A-12', areaSqm: 120, currency: 'MYR', purchasePrice: 200000, purchaseDate: '2026-08-01', assetStage: 'PRE_HANDOVER', totalInstallmentCount: 2, paidAmount: 50000, remainingAmount: 150000, coverDocumentId: 9 };
  const render = async (properties, locale = 'en') => {
    const app = Vue.createSSRApp(component, { properties });
    app.config.globalProperties.$t = key => key;
    app.config.globalProperties.$lt = value => value;
    app.config.globalProperties.$i18n = { locale };
    return renderToString(app);
  };
  const before = JSON.stringify(property);
  const html = await render([property]);
  for (const text of ['Test Residence', 'A-12', '120', 'MYR 200,000.00', 'MYR 50,000.00', 'MYR 150,000.00', '25%', 'ownerApp.payment', 'ARCHIVED_COVER']) assert.ok(html.includes(text), text);
  assert.equal(JSON.stringify(property), before, 'Rendering must not mutate a property');
  const operating = await render([{ ...property, assetStage: 'OPERATING', totalInstallmentCount: 0, coverDocumentId: null }], 'zh-CN');
  assert.ok(operating.includes('ownerApp.cashflow'));
  assert.ok(operating.includes('ownerApp.paymentNotArchived'));
  assert.ok(operating.includes('NO_PHOTO'));
  assert.ok(!operating.includes('MYR 50,000.00'));
  assert.ok((await render([])).includes('owner-app-state'));
});

test('mobile refinement keeps readable type, compact shortcuts and honest photo fallbacks', () => {
  const css = read('../src/components/owner-mobile/owner-mobile-visual.css');
  assert.match(css, /--text-native-body:1rem/);
  assert.match(css, /--font-native-display:var\(--font-native-body\)/);
  assert.match(css, /\.owner-quick-grid\s*\{[^}]*grid-template-columns:repeat\(4,minmax\(0,1fr\)\)/);
  assert.match(css, /\.owner-quick-grid button\s*\{[^}]*min-height:4\.5rem/);
  assert.match(css, /\.owner-property-grid-v2 nav\s*\{[^}]*repeat\(3,minmax\(0,1fr\)\)/);
  assert.match(css, /\.owner-archived-cover:not\(\.has-photo\)\s*\{[^}]*min-height:2\.75rem/);
  assert.match(css, /:focus-visible\s*\{[^}]*outline:2px solid var\(--color-native-focus\)/);
  assert.match(css, /owner-bottom-nav__label\s*\{[^}]*white-space:normal/);
  const nav = read('../src/components/OwnerBottomNav.vue');
  assert.match(nav, /id: 'ownerProjects'[^\n]*primary: true/);
  assert.match(nav, /@click="selectModule\(item.id\)"/);
});

test('cover loading keeps latest property image, revokes blobs and settles unavailable states', async () => {
  const source = read('../src/components/owner-mobile/OwnerPropertyCover.vue');
  const script = parse(source).descriptor.script.content.replace(/^import .*;$/gm, '').replace('export default', 'return');
  const requests = new Map();
  const revoked = [];
  const component = new Function('fetchOwnerDocumentFile', 'Building2', 'URL', script)(id => new Promise((resolve,reject) => requests.set(id,{resolve,reject})), {}, {createObjectURL:blob=>`blob:${blob.id}`,revokeObjectURL:url=>revoked.push(url)});
  const vm = {...component.data()};
  vm.clear = component.methods.clear.bind(vm);
  const load = id => component.watch['property.coverDocumentId'].handler.call(vm,id);
  const first = load(1);
  assert.equal(vm.loading,true);
  const second = load(2);
  requests.get(2).resolve({blob:{type:'image/jpeg',id:2}}); await second;
  assert.equal(vm.url,'blob:2'); assert.equal(vm.loading,false);
  requests.get(1).resolve({blob:{type:'image/jpeg',id:1}}); await first;
  assert.equal(vm.url,'blob:2');
  await load(null); assert.deepEqual(revoked,['blob:2']); assert.equal(vm.url,''); assert.equal(vm.loading,false);
  const failed = load(3); requests.get(3).reject(new Error('unavailable')); await failed;
  assert.equal(vm.url,''); assert.equal(vm.loading,false);
  const nonImage = load(4); requests.get(4).resolve({blob:{type:'application/pdf',id:4}}); await nonImage;
  assert.equal(vm.url,''); assert.equal(vm.loading,false);
  const unmounted = load(5); component.beforeUnmount.call(vm); requests.get(5).resolve({blob:{type:'image/jpeg',id:5}}); await unmounted;
  assert.equal(vm.url,'');
});
